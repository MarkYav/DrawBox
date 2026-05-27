package io.github.markyav.drawbox.engine

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize

// [SP_ENG_01_02] Immutable snapshot of the drawing environment at gesture start.
// Valid for the lifetime of one gesture only — do not retain across gestures.
data class ToolContext(
    val paintOptions: PaintOptions,
    val committedBitmap: ImageBitmap,
    val logicalSize: IntSize,
)
