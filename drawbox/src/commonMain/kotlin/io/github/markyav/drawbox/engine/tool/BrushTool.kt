package io.github.markyav.drawbox.engine.tool

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import io.github.markyav.drawbox.engine.DrawAction
import io.github.markyav.drawbox.engine.DrawTool
import io.github.markyav.drawbox.engine.ToolContext
import io.github.markyav.drawbox.engine.action.PathAction
import io.github.markyav.drawbox.engine.util.PathSimplifier

// [SP_ENG_01_08] Smooth bezier brush stroke.
// Points accumulate inside the live PathAction's ArrayList to avoid per-drag allocation.
// onGestureEnd produces a new immutable PathAction with simplified points.
object BrushTool : DrawTool {

    override val clearsActiveCanvasOnDrag = false

    override fun onGestureStart(point: Offset, context: ToolContext): DrawAction {
        val opts = context.paintOptions
        return PathAction(arrayListOf(point), opts.color, opts.strokeWidth, opts.opacity, BlendMode.SrcOver)
    }

    override fun onGestureDrag(from: Offset, to: Offset, current: DrawAction?, context: ToolContext): DrawAction {
        val action = current as? PathAction ?: return onGestureStart(to, context)
        action.pointsList.add(to)  // mutate in place — same object returned each drag event
        return action
    }

    override fun onGestureEnd(current: DrawAction?, context: ToolContext): DrawAction? {
        val action = current as? PathAction ?: return null
        val simplified = PathSimplifier.simplify(action.points)
        return PathAction(ArrayList(simplified), action.color, action.strokeWidth, action.opacity, action.blendMode)
    }

    override fun onTap(point: Offset, context: ToolContext): DrawAction {
        val opts = context.paintOptions
        return PathAction(arrayListOf(point), opts.color, opts.strokeWidth, opts.opacity, BlendMode.SrcOver)
    }
}
