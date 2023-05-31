package io.github.markyav.drawbox.box

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
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
import io.github.markyav.drawbox.controller.OpenedImage
import io.github.markyav.drawbox.model.PathWrapper
import io.github.markyav.drawbox.util.createPath
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DrawBoxCanvas(
    pathListWrapper: StateFlow<List<PathWrapper>>,
    openedImage: StateFlow<OpenedImage>,
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
    val path by pathListWrapper.collectAsState()
    val image by openedImage.collectAsState()

    Canvas(modifier = modifier
        .onSizeChanged(onSizeChanged)
        .pointerInput(Unit) { detectTapGestures(onTap = onTap) }
        .pointerInput(Unit) { detectDragGestures(onDragStart = onDragStart, onDrag = onDragMapper, onDragEnd = onDragEnd, onDragCancel = onDragEnd) }
        .clipToBounds()
        .alpha(alpha)
    ) {
        (image as? OpenedImage.Image)?.let {
            drawImage(
                image = it.image,
                srcOffset = it.srcOffset,
                srcSize = it.srcSize,
                dstSize = it.dstSize,
            )
        }

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