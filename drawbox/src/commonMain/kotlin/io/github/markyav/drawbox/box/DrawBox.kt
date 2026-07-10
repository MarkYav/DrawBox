package io.github.markyav.drawbox.box

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import io.github.markyav.drawbox.controller.DrawController
import io.github.markyav.drawbox.model.ImageMode
import io.github.markyav.drawbox.model.NormPoint

@Composable
fun DrawBox(
    controller: DrawController,
    modifier: Modifier = Modifier,
) {
    val liveImage by controller.image(ImageMode.Live).collectAsState()

    val canvasRatio = controller.canvasSize.width.toFloat() / controller.canvasSize.height.toFloat()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(canvasRatio)
                .pointerInput(controller) {
                    detectTapGestures(
                        onTap = { offset ->
                            controller.onTap(NormPoint(offset.x / size.width.toFloat(), offset.y / size.height.toFloat()))
                        }
                    )
                }
                .pointerInput(controller) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            controller.onDragStart(NormPoint(offset.x / size.width.toFloat(), offset.y / size.height.toFloat()))
                        },
                        onDrag = { change, _ ->
                            val offset = change.position
                            controller.onDrag(NormPoint(offset.x / size.width.toFloat(), offset.y / size.height.toFloat()))
                        },
                        onDragEnd = controller::onDragEnd,
                        onDragCancel = controller::onDragEnd
                    )
                }
        ) {
            drawImage(
                image = liveImage,
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )
        }
    }
}