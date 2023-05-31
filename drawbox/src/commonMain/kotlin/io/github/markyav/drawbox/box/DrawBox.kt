package io.github.markyav.drawbox.box

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.github.markyav.drawbox.controller.DrawBoxSubscription
import io.github.markyav.drawbox.controller.DrawController
import io.github.markyav.drawbox.controller.OpenedImage
import io.github.markyav.drawbox.model.PathWrapper
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DrawBox(
    controller: DrawController,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val path: StateFlow<List<PathWrapper>> = remember { controller.getPathWrappersForDrawbox(DrawBoxSubscription.DynamicUpdate) }
    val openedImage: StateFlow<OpenedImage> = remember { controller.getOpenImageForDrawbox(null) }
    val background by controller.background.collectAsState()
    val canvasOpacity by controller.canvasOpacity.collectAsState()

    Box(modifier = modifier) {
        DrawBoxBackground(
            background = background,
            modifier = Modifier.fillMaxSize(),
        )
        DrawBoxCanvas(
            pathListWrapper = path,
            openedImage = openedImage,
            alpha = canvasOpacity,
            onSizeChanged = controller::connectToDrawBox,
            onTap = controller::onTap,
            onDragStart = controller::insertNewPath,
            onDrag = controller::updateLatestPath,
            onDragEnd = controller::finalizePath,
            modifier = Modifier.fillMaxSize(),
        )
    }
}