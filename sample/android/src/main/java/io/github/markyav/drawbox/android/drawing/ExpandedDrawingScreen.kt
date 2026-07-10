package io.github.markyav.drawbox.android.drawing

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.markyav.drawbox.box.DrawBox
import io.github.markyav.drawbox.controller.DrawController
import io.github.markyav.drawbox.model.ImageMode

@Composable
internal fun ExpandedDrawingScreen(
    drawController: DrawController,
) {
    val bitmap by drawController.image(ImageMode.Committed).collectAsState()

    Column {
        Image(bitmap = bitmap, modifier = Modifier
            .size(250.dp)
            .border(1.dp, Color.Red), contentDescription = null)

        Column(modifier = Modifier.weight(4.5f, false)) {
            Log.i("TAG_aaa", "ExpandedDrawingScreen: $bitmap")
            DrawBox(
                controller = drawController,
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(8.dp)
                    .border(width = 1.dp, color = Color.Blue)
                    .weight(1f, fill = false),
            )
            Row {
                val enableUndo by drawController.canUndo.collectAsState()
                val enableRedo by drawController.canRedo.collectAsState()
                IconButton(onClick = drawController::undo, enabled = enableUndo) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "undo")
                }
                IconButton(onClick = drawController::redo, enabled = enableRedo) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "redo")
                }
                IconButton(onClick = drawController::clear, enabled = enableUndo || enableRedo) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "clear")
                }
            }
        }
    }
}