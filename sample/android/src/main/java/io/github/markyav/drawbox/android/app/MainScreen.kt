package io.github.markyav.drawbox.android.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import io.github.markyav.drawbox.android.drawing.DrawingScreen

@Composable
fun MainScreen() {
    val bitmap = remember { mutableStateOf(ImageBitmap(1, 1)) }

    val bitmapCallback: (ImageBitmap) -> Unit = {
        bitmap.value = it
    }

    Row {
        Column(modifier = Modifier.weight(2f, true)) {
            DrawingScreen(bitmapCallback = bitmapCallback)
        }
        /*Column(modifier = Modifier.weight(1f, true)) {
            Image(
                bitmap = bitmap.value,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(8.dp).border(1.dp, Color.Blue),
            )
        }*/
    }
}