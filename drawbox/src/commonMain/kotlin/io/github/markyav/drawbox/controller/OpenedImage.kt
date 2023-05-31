package io.github.markyav.drawbox.controller

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

sealed interface OpenedImage {
    object None : OpenedImage
    data class  Image(
        val image: ImageBitmap,
        val dstSize: IntSize,
        val srcOffset: IntOffset = IntOffset(
            x = - (minOf(image.width, image.height) - image.width) / 2,
            y = - (minOf(image.width, image.height) - image.height) / 2,
        ),
        val srcSize: IntSize = IntSize(
            width = minOf(image.width, image.height),
            height = minOf(image.width, image.height),
        ),
    ) : OpenedImage
}


