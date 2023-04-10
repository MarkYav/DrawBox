package io.github.markyav.drawbox.android.drawing

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import io.github.markyav.drawbox.android.drawing.item.DrawingColor
import io.github.markyav.drawbox.android.drawing.item.getSketchDrawingClassList
import io.github.markyav.drawbox.android.drawing.item.toDrawingColorList
import io.github.markyav.drawbox.controller.DrawBoxSubscription
import io.github.markyav.drawbox.controller.DrawController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun DrawingScreen(
    drawingColors: List<DrawingColor> = getSketchDrawingClassList().toDrawingColorList(),
    bitmapCallback: (ImageBitmap) -> Unit,
) {
    val drawController = remember { DrawController() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        drawController.color.value = drawingColors.first().color
        drawController.getBitmap(500, DrawBoxSubscription.FinishDrawingUpdate, coroutineScope)
            .collectLatest {
                Log.i("TAG_aaa", "DrawingScreen: emitted!")
                bitmapCallback(it)
            }
    }

    ExpandedDrawingScreen(drawingColors, drawController)
}