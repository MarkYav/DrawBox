package io.github.markyav.drawbox.box

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import io.github.markyav.drawbox.controller.DrawBoxBackground

@Composable
fun DrawBoxBackground(
    background: DrawBoxBackground,
    modifier: Modifier,
) {
    when (background) {
        is DrawBoxBackground.ColourBackground -> Box(
            modifier = modifier
                .alpha(alpha = background.alpha)
                .background(color = background.color)
        )
        is DrawBoxBackground.ImageBackground -> Image(
            bitmap = background.bitmap,
            alpha = background.alpha,
            modifier = modifier,
            contentDescription = null
        )
        is DrawBoxBackground.NoBackground -> { }
    }
}