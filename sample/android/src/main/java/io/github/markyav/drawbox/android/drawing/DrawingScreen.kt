package io.github.markyav.drawbox.android.drawing

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntSize
import io.github.markyav.drawbox.controller.DrawController

@Composable
fun DrawingScreen() {
    val drawController = remember { DrawController(logicalSize = IntSize(1000, 1000)) }
    Column {
        ExpandedDrawingScreen(drawController)
    }
}
