package io.github.markyav.drawbox.engine.action

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import io.github.markyav.drawbox.engine.DrawAction
import io.github.markyav.drawbox.util.createPath

// [SP_ENG_01_07] Smooth bezier stroke through a list of points.
// Used by BrushTool (SrcOver) and PixelEraserTool (Clear).
//
// The pointsList is intentionally an ArrayList — BrushTool mutates it in place during a gesture
// so that each drag event appends O(1) instead of copying the entire list. At onGestureEnd, a
// new immutable PathAction is created from the simplified points.
class PathAction internal constructor(
    internal val pointsList: ArrayList<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val opacity: Float,
    val blendMode: BlendMode,
) : DrawAction {

    val points: List<Offset> get() = pointsList

    private fun makePaint() = Paint().apply {
        color = this@PathAction.color.copy(alpha = this@PathAction.color.alpha * this@PathAction.opacity)
        strokeWidth = this@PathAction.strokeWidth
        strokeCap = StrokeCap.Round
        strokeJoin = StrokeJoin.Round
        style = PaintingStyle.Stroke
        blendMode = this@PathAction.blendMode
        isAntiAlias = true
    }

    // [SP_ENG_01_07] Full stroke render — used on commit and history replay.
    override fun render(canvas: Canvas) {
        when {
            pointsList.isEmpty() -> return
            pointsList.size == 1 -> {
                // Single point → draw a filled circle (dot).
                canvas.drawCircle(pointsList[0], strokeWidth / 2f, makePaint().apply { style = PaintingStyle.Fill })
            }
            else -> canvas.drawPath(createPath(pointsList), makePaint())
        }
    }

    // [SP_ENG_01_07] O(1) incremental render — draws only the newest segment.
    // The active canvas is NOT cleared before this call (engine accumulates).
    override fun renderLastSegment(canvas: Canvas) {
        if (pointsList.size < 2) {
            render(canvas)
            return
        }
        canvas.drawLine(pointsList[pointsList.size - 2], pointsList[pointsList.size - 1], makePaint())
    }

    override fun getBounds(): Rect? {
        if (pointsList.isEmpty()) return Rect.Zero
        val expand = strokeWidth / 2f
        return Rect(
            left = pointsList.minOf { it.x } - expand,
            top = pointsList.minOf { it.y } - expand,
            right = pointsList.maxOf { it.x } + expand,
            bottom = pointsList.maxOf { it.y } + expand,
        )
    }
}
