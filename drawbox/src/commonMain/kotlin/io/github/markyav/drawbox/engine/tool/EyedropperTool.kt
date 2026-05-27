package io.github.markyav.drawbox.engine.tool

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import io.github.markyav.drawbox.engine.DrawAction
import io.github.markyav.drawbox.engine.DrawTool
import io.github.markyav.drawbox.engine.ToolContext

// [SP_ENG_01_08] Reads a pixel color from the committed canvas and reports it via callback.
// Produces no DrawAction — pure side-effect tool.
// onColorPicked is provided at construction so the tool remains stateless across gestures.
class EyedropperTool(val onColorPicked: (Color) -> Unit) : DrawTool {

    override val clearsActiveCanvasOnDrag = false

    override fun onGestureStart(point: Offset, context: ToolContext): DrawAction? = null
    override fun onGestureDrag(from: Offset, to: Offset, current: DrawAction?, context: ToolContext): DrawAction? = null
    override fun onGestureEnd(current: DrawAction?, context: ToolContext): DrawAction? = null

    override fun onTap(point: Offset, context: ToolContext): DrawAction? {
        val bitmap = context.committedBitmap
        val x = point.x.toInt().coerceIn(0, bitmap.width - 1)
        val y = point.y.toInt().coerceIn(0, bitmap.height - 1)
        onColorPicked(bitmap.toPixelMap()[x, y])
        return null
    }
}
