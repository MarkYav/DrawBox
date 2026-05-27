package io.github.markyav.drawbox.engine

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas

// [SP_ENG_01_03] Open interface — every drawing operation renders itself.
interface DrawAction {
    // Renders the complete action onto canvas. Must be idempotent.
    fun render(canvas: Canvas)

    // Renders only the newest segment added since the last renderLastSegment call.
    // Default: delegates to render. Override for O(1) incremental rendering (e.g., PathAction).
    fun renderLastSegment(canvas: Canvas) = render(canvas)

    // Bounding rect in logical pixels; null means "unknown / covers entire canvas".
    fun getBounds(): Rect?
}
