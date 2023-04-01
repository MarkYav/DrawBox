package io.github.markyav.drawbox.controller

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

sealed interface DrawBoxBackground {
    object NoBackground : DrawBoxBackground
    data class ColourBackground(val color: Color, val alpha: Float = 1f) : DrawBoxBackground
    data class ImageBackground(val bitmap: ImageBitmap, val alpha: Float = 1f) : DrawBoxBackground
}