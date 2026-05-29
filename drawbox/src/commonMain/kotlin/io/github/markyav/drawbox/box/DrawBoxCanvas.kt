package io.github.markyav.drawbox.box

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import io.github.markyav.drawbox.controller.DrawController

// [SP_BOX_v2_01_02] Internal composable — owns size reporting, gesture wiring, and bitmap rendering.
@Composable
internal fun DrawBoxCanvas(
    controller: DrawController,
    modifier: Modifier = Modifier,
) {
    val tick by controller.invalidationTick.collectAsState()
    val bitmap = remember(tick) { controller.getDisplayOutput() } // [SP_BOX_v2_02_07]

    Canvas(
        modifier = modifier
            .clipToBounds()  // [SP_BOX_v2_03_02] applied before gesture detectors
            .onSizeChanged { controller.onCanvasSizeChanged(it) }
            .pointerInput(Unit) {
                // Single gesture scope resolves tap-vs-drag via slop check. [SP_BOX_v2_02_03–06]
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val drag = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    if (drag == null) {
                        // Slop not reached — treat as tap. [SP_BOX_v2_02_03]
                        waitForUpOrCancellation()?.also { controller.onTap(down.position) }
                    } else {
                        // Slop reached — drag gesture. [SP_BOX_v2_02_04–06]
                        controller.onGestureStart(down.position)
                        drag.consume()
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == drag.id } ?: break
                                change.consume()
                                if (!change.pressed) break
                                controller.onGestureMove(change.previousPosition, change.position)
                            }
                        } finally {
                            // Covers both normal end and system cancellation. [SP_BOX_v2_02_06]
                            controller.onGestureEnd()
                        }
                    }
                }
            }
    ) {
        drawImage(bitmap)
    }
}
