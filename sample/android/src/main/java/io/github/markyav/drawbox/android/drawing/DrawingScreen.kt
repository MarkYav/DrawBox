package io.github.markyav.drawbox.android.drawing

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import io.github.markyav.drawbox.controller.DrawBoxBackground
import io.github.markyav.drawbox.controller.DrawBoxSubscription
import io.github.markyav.drawbox.controller.DrawController
import kotlinx.coroutines.flow.collectLatest

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun DrawingScreen(
    bitmapCallback: (ImageBitmap) -> Unit,
) {
    val drawController = remember { DrawController() }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            drawController.open(bitmap.asImageBitmap())
        }
    }

    LaunchedEffect(Unit) {
        drawController.color.value = Color.Blue
        drawController.background.value = DrawBoxBackground.ColourBackground(color = Color.Red, alpha = 0.15f)
        drawController.canvasOpacity.value = 0.5f
        drawController.getBitmap(500, DrawBoxSubscription.FinishDrawingUpdate)
            .collectLatest {
                Log.i("TAG_aaa", "DrawingScreen: emitted!")
                bitmapCallback(it)
            }
    }

    Column {
        IconButton(onClick = {
            takePictureLauncher.launch()
        }) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
        ExpandedDrawingScreen(drawController)
    }

}