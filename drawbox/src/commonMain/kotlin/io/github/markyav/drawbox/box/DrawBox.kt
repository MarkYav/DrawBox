package io.github.markyav.drawbox.box

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.markyav.drawbox.controller.DrawController

// [SP_BOX_v2_01_01] Public entry point — delegates all rendering and input to DrawBoxCanvas.
@Composable
fun DrawBox(
    controller: DrawController,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    DrawBoxCanvas(
        controller = controller,
        modifier = modifier,
    )
}
