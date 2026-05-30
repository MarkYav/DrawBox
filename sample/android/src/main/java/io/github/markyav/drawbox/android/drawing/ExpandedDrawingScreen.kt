package io.github.markyav.drawbox.android.drawing

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

@Composable
internal fun ExpandedDrawingScreen(
    drawController: DrawController,
) {
    val canUndo by drawController.canUndo.collectAsState()
    val canRedo by drawController.canRedo.collectAsState()

    Column {
        DrawBox(
            controller = drawController,
            modifier = Modifier
                .aspectRatio(1f)
                .padding(8.dp)
                .border(width = 1.dp, color = Color.Blue)
                .weight(1f, fill = false),
        )
        Row {
            IconButton(onClick = drawController::undo, enabled = canUndo) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "undo")
            }
            IconButton(onClick = drawController::redo, enabled = canRedo) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "redo")
            }
            IconButton(onClick = drawController::reset, enabled = canUndo || canRedo) {
                Icon(imageVector = Icons.Default.Clear, contentDescription = "reset")
            }
        }
    }
}
