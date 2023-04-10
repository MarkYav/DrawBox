package io.github.markyav.drawbox.box

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.markyav.drawbox.controller.DrawBoxSubscription
import io.github.markyav.drawbox.controller.DrawController
import io.github.markyav.drawbox.model.PathWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DrawBox(
    controller: DrawController,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val path: StateFlow<List<PathWrapper>> = remember {
        controller.getPathWrappersForDrawbox(DrawBoxSubscription.DynamicUpdate)
    }

    Box(modifier = modifier) {
        DrawBoxBackground(
            background = controller.background.value,
            modifier = Modifier.fillMaxSize(),
        )
        DrawBoxCanvas(
            pathListWrapper = path,
            alpha = controller.canvasOpacity.value,
            onSizeChanged = controller::connectToDrawBox,
            onTap = controller::onTap,
            onDragStart = controller::insertNewPath,
            onDrag = controller::updateLatestPath,
            onDragEnd = controller::finalizePath,
            modifier = Modifier.fillMaxSize(),
        )
    }
}