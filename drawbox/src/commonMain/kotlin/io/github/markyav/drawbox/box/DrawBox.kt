package io.github.markyav.drawbox.box

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import io.github.markyav.drawbox.controller.DrawBoxSubscription
import io.github.markyav.drawbox.controller.DrawController
import io.github.markyav.drawbox.model.PathWrapper
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DrawBox(
    controller: DrawController,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val coroutineScope = rememberCoroutineScope()
    val size = remember { mutableStateOf(1) }
    //val path: StateFlow<ImageBitmap> = remember { controller.getBitmapForDrawbox(DrawBoxSubscription.DynamicUpdate, coroutineScope) }
    val path: StateFlow<List<PathWrapper>> = remember { controller.getBitmapForDrawbox(DrawBoxSubscription.DynamicUpdate, coroutineScope) }

    Box(modifier = modifier) {
        DrawBoxBackground(
            background = controller.background.value,
            modifier = Modifier.fillMaxSize(),
        )
        DrawBoxCanvas(
            pathListWrapper = path,
            alpha = controller.canvasOpacity.value,
            onSizeChanged = controller::connectToDrawBox,
            onTap = controller::insertNewPath,
            onDragStart = controller::insertNewPath,
            onDrag = controller::updateLatestPath,
            onDragEnd = controller::finalizePath,
            modifier = Modifier.fillMaxSize(),
        )
    }
}