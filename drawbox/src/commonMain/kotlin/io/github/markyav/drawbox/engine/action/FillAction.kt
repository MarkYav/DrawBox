package io.github.markyav.drawbox.engine.action

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import io.github.markyav.drawbox.engine.DrawAction

// [SP_ENG_01_07] Result of a flood fill operation, pre-rasterized at commit time.
// filledBitmap has the same dimensions as the logical canvas; only filled pixels are non-transparent.
class FillAction(val filledBitmap: ImageBitmap) : DrawAction {

    override fun render(canvas: Canvas) {
        canvas.drawImage(filledBitmap, Offset.Zero, Paint())
    }

    // Fill is never incremental — renderLastSegment delegates to render.
    override fun renderLastSegment(canvas: Canvas) = render(canvas)

    // Bounding rect is unknown without scanning pixels; return null (treated as full-canvas).
    override fun getBounds(): Rect? = null
}
