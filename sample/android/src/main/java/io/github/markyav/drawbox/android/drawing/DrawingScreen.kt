package io.github.markyav.drawbox.android.drawing

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import io.github.markyav.drawbox.controller.DrawController
import io.github.markyav.drawbox.model.ImageMode
import kotlinx.coroutines.flow.collectLatest

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun DrawingScreen(
    bitmapCallback: (ImageBitmap) -> Unit,
) {
    val drawController = remember { DrawController(IntSize(1000, 1000)) }

    LaunchedEffect(Unit) {
        drawController.setColor(Color.Blue.toArgb().toLong())
        drawController.image(ImageMode.Committed)
            .collectLatest {
                bitmapCallback(it)
            }
    }

    Column {
        ExpandedDrawingScreen(drawController)
    }
}