package io.github.markyav.drawbox.engine.tool

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import io.github.markyav.drawbox.engine.DrawAction
import io.github.markyav.drawbox.engine.DrawTool
import io.github.markyav.drawbox.engine.ToolContext
import io.github.markyav.drawbox.engine.action.PathAction
import io.github.markyav.drawbox.engine.util.PathSimplifier

// [SP_ENG_01_08] Pixel-level eraser — same as BrushTool but BlendMode.Clear paints transparency.
object PixelEraserTool : DrawTool {

    override val clearsActiveCanvasOnDrag = false

    override fun onGestureStart(point: Offset, context: ToolContext): DrawAction {
        val opts = context.paintOptions
        return PathAction(arrayListOf(point), opts.color, opts.strokeWidth, opts.opacity, BlendMode.Clear)
    }

    override fun onGestureDrag(from: Offset, to: Offset, current: DrawAction?, context: ToolContext): DrawAction {
        val action = current as? PathAction ?: return onGestureStart(to, context)
        action.pointsList.add(to)
        return action
    }

    override fun onGestureEnd(current: DrawAction?, context: ToolContext): DrawAction? {
        val action = current as? PathAction ?: return null
        val simplified = PathSimplifier.simplify(action.points)
        return PathAction(ArrayList(simplified), action.color, action.strokeWidth, action.opacity, action.blendMode)
    }

    override fun onTap(point: Offset, context: ToolContext): DrawAction {
        val opts = context.paintOptions
        return PathAction(arrayListOf(point), opts.color, opts.strokeWidth, opts.opacity, BlendMode.Clear)
    }
}
