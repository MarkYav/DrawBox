package io.github.markyav.drawbox.box

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import io.github.markyav.drawbox.model.PathWrapper
import io.github.markyav.drawbox.util.createPath

@Composable
fun DrawBoxCanvas(
    path: List<PathWrapper>,
    alpha: Float,
    onSizeChanged: (IntSize) -> Unit,
    onTap: (Offset) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier,
) {
    val onDragMapper: (change: PointerInputChange, dragAmount: Offset) -> Unit = remember {
        { change, _ -> onDrag(change.position) }
    }

    Canvas(modifier = modifier
        .onSizeChanged(onSizeChanged)
        .pointerInput(Unit) { detectTapGestures(onTap = onTap) }
        .pointerInput(Unit) { detectDragGestures(onDragStart = onDragStart, onDrag = onDragMapper, onDragEnd = onDragEnd, onDragCancel = onDragEnd) }
        .clipToBounds()
        .alpha(alpha)
    ) {
        path.forEach { pw ->
            drawPath(
                createPath(pw.points),
                color = pw.strokeColor,
                alpha = pw.alpha,
                style = Stroke(
                    width = pw.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}