package io.github.markyav.drawbox.box

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.markyav.drawbox.controller.DrawBoxBackground
import io.github.markyav.drawbox.controller.DrawController
import io.github.markyav.drawbox.model.PathWrapper

@Composable
fun DrawBox(
    controller: DrawController,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val path: List<PathWrapper>? = controller.pathToDrawOnCanvas
    val background: DrawBoxBackground = controller.background
    val canvasAlpha: Float = controller.canvasOpacity

    Box(modifier = modifier) {
        DrawBoxBackground(
            background = background,
            modifier = Modifier.fillMaxSize(),
        )
        DrawBoxCanvas(
            path = path ?: emptyList(),
            alpha = canvasAlpha,
            onSizeChanged = controller::connectToDrawBox,
            onTap = controller::insertNewPath,
            onDragStart = controller::insertNewPath,
            onDrag = controller::updateLatestPath,
            onDragEnd = controller::finalizePath,
            modifier = Modifier.fillMaxSize(),
        )
    }
}