package io.github.markyav.drawbox.engine.tool

import androidx.compose.ui.geometry.Offset
import io.github.markyav.drawbox.engine.DrawAction
import io.github.markyav.drawbox.engine.DrawTool
import io.github.markyav.drawbox.engine.ToolContext
import io.github.markyav.drawbox.engine.action.ShapeAction
import io.github.markyav.drawbox.engine.action.ShapeType

// [SP_ENG_01_08] Geometric shape drawn from gesture start to gesture end.
// clearsActiveCanvasOnDrag = true so each drag replaces the previous preview shape.
class ShapeTool(val shapeType: ShapeType) : DrawTool {

    override val clearsActiveCanvasOnDrag = true

    override fun onGestureStart(point: Offset, context: ToolContext): DrawAction {
        val opts = context.paintOptions
        return ShapeAction(shapeType, point, point, opts.color, opts.strokeWidth, opts.opacity)
    }

    override fun onGestureDrag(from: Offset, to: Offset, current: DrawAction?, context: ToolContext): DrawAction {
        val action = current as? ShapeAction ?: return onGestureStart(to, context)
        return ShapeAction(action.shapeType, action.start, to, action.color, action.strokeWidth, action.opacity)
    }

    override fun onGestureEnd(current: DrawAction?, context: ToolContext): DrawAction? = current as? ShapeAction

    // Degenerate zero-size shape on tap — treated as no-op.
    override fun onTap(point: Offset, context: ToolContext): DrawAction? = null
}
