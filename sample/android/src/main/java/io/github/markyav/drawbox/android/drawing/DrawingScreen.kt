package io.github.markyav.drawbox.android.drawing

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import io.github.markyav.drawbox.controller.DrawBoxSubscription
import io.github.markyav.drawbox.controller.DrawController
import kotlinx.coroutines.flow.collectLatest

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun DrawingScreen(
    bitmapCallback: (ImageBitmap) -> Unit,
) {
    val drawController = remember { DrawController() }

    LaunchedEffect(Unit) {
        drawController.color.value = Color.Blue
        drawController.getBitmap(500, DrawBoxSubscription.FinishDrawingUpdate)
            .collectLatest {
                Log.i("TAG_aaa", "DrawingScreen: emitted!")
                bitmapCallback(it)
            }
    }

    ExpandedDrawingScreen(drawController)
}