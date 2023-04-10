package io.github.markyav.drawbox.android.drawing

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.markyav.drawbox.android.drawing.item.DrawingColor
import io.github.markyav.drawbox.box.DrawBox
import io.github.markyav.drawbox.controller.DrawBoxSubscription
import io.github.markyav.drawbox.controller.DrawController

@Composable
internal fun ExpandedDrawingScreen(
    drawingColors: List<DrawingColor>,
    drawController: DrawController,
) {
    val coroutineScope = rememberCoroutineScope()
    val bitmap by remember { drawController.getBitmap(500, DrawBoxSubscription.FinishDrawingUpdate, coroutineScope) }.collectAsState()

    Row {
        Image(bitmap = bitmap, modifier = Modifier
            .size(50.dp)
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
                val enableUndo by remember { derivedStateOf { drawController.undoCount > 0 } }
                val enableRedo by remember { derivedStateOf { drawController.redoCount > 0 } }
                IconButton(onClick = drawController::undo, enabled = enableUndo) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "undo")
                }
                IconButton(onClick = drawController::redo, enabled = enableRedo) {
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "redo")
                }
                IconButton(onClick = drawController::reset, enabled = enableUndo || enableRedo) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "reset")
                }
            }
        }
    }
}

/*@AndroidPreviewDevices
@Composable
fun ExpandedDrawingScreenPreview() {
    MobiSketchTheme {
        ExpandedDrawingScreen()
    }
}*/