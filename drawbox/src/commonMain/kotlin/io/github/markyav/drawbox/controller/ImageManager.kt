package io.github.markyav.drawbox.controller

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize

internal class ImageManager {
    var current: ImageBitmap? = null
        private set
    var active: ImageBitmap? = null
        private set
    var checkpoint: ImageBitmap? = null
        private set

    fun allocateBuffers(size: IntSize) {
        current = ImageBitmap(size.width, size.height)
        active = ImageBitmap(size.width, size.height)
        checkpoint = ImageBitmap(size.width, size.height)
    }

    fun clearActive() {
        active?.let { RenderEngine.clearBitmap(it) }
    }
}
