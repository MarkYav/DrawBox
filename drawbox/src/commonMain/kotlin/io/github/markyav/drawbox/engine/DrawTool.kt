package io.github.markyav.drawbox.engine

import androidx.compose.ui.geometry.Offset

// [SP_ENG_01_04] Open interface — stateless gesture strategy.
// Implementations must hold no mutable state between gesture sessions.
interface DrawTool {
    // When true, the engine clears the active canvas before each renderLastSegment call.
    // Use for tools whose visual replaces rather than accumulates (e.g., ShapeTool).
    val clearsActiveCanvasOnDrag: Boolean get() = false

    fun onGestureStart(point: Offset, context: ToolContext): DrawAction?
    fun onGestureDrag(from: Offset, to: Offset, current: DrawAction?, context: ToolContext): DrawAction?
    fun onGestureEnd(current: DrawAction?, context: ToolContext): DrawAction?
    fun onTap(point: Offset, context: ToolContext): DrawAction?
}
