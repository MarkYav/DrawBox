package io.github.markyav.drawbox.engine.tool

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import io.github.markyav.drawbox.engine.DrawAction
import io.github.markyav.drawbox.engine.DrawTool
import io.github.markyav.drawbox.engine.ToolContext
import io.github.markyav.drawbox.engine.action.StrokeEraseAction

// [SP_ENG_01_08] Stroke-level eraser — removes intersecting committed actions from history.
// No visual feedback during gesture (active canvas stays blank).
// CanvasManager computes intersections and handles suppression on commitStrokeErase.
object StrokeEraserTool : DrawTool {

    private const val ERASE_RADIUS = 20f  // logical pixels radius around gesture path

    override val clearsActiveCanvasOnDrag = false

    override fun onGestureStart(point: Offset, context: ToolContext): DrawAction =
        StrokeEraseInProgress(mutableListOf(point))

    override fun onGestureDrag(from: Offset, to: Offset, current: DrawAction?, context: ToolContext): DrawAction {
        val progress = current as? StrokeEraseInProgress ?: return StrokeEraseInProgress(mutableListOf(from, to))
        progress.points.add(to)
        return progress
    }

    override fun onGestureEnd(current: DrawAction?, context: ToolContext): DrawAction? {
        val progress = current as? StrokeEraseInProgress ?: return null
        return StrokeEraseAction(progress.computeEraseRect(ERASE_RADIUS))
    }

    override fun onTap(point: Offset, context: ToolContext): DrawAction =
        StrokeEraseAction(
            Rect(
                point.x - ERASE_RADIUS, point.y - ERASE_RADIUS,
                point.x + ERASE_RADIUS, point.y + ERASE_RADIUS,
            )
        )
}

// Private gesture accumulator — carries mutable point list across onGestureStart / onGestureDrag calls.
private class StrokeEraseInProgress(val points: MutableList<Offset>) : DrawAction {
    override fun render(canvas: Canvas) {}  // no visual feedback during stroke erase gesture
    override fun getBounds(): Rect? = null

    fun computeEraseRect(radius: Float): Rect {
        if (points.isEmpty()) return Rect.Zero
        return Rect(
            left = points.minOf { it.x } - radius,
            top = points.minOf { it.y } - radius,
            right = points.maxOf { it.x } + radius,
            bottom = points.maxOf { it.y } + radius,
        )
    }
}
