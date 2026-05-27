package io.github.markyav.drawbox.engine.action

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import io.github.markyav.drawbox.engine.DrawAction

// [SP_ENG_01_07] Geometric shape parameterized by type and two bounding points.
class ShapeAction(
    val shapeType: ShapeType,
    val start: Offset,
    val end: Offset,
    val color: Color,
    val strokeWidth: Float,
    val opacity: Float,
) : DrawAction {

    private fun makePaint() = Paint().apply {
        color = this@ShapeAction.color.copy(alpha = this@ShapeAction.color.alpha * this@ShapeAction.opacity)
        strokeWidth = this@ShapeAction.strokeWidth
        strokeCap = StrokeCap.Round
        strokeJoin = StrokeJoin.Round
        style = PaintingStyle.Stroke
        isAntiAlias = true
    }

    override fun render(canvas: Canvas) {
        val paint = makePaint()
        val left = minOf(start.x, end.x)
        val top = minOf(start.y, end.y)
        val right = maxOf(start.x, end.x)
        val bottom = maxOf(start.y, end.y)
        val rect = Rect(left, top, right, bottom)
        when (shapeType) {
            ShapeType.Line -> canvas.drawLine(start, end, paint)
            ShapeType.Rectangle -> canvas.drawRect(rect, paint)
            ShapeType.Oval -> canvas.drawOval(rect, paint)
        }
    }

    // ShapeTool sets clearsActiveCanvasOnDrag = true so the engine clears before each call.
    // renderLastSegment delegates to render since the shape always redraws fully.
    override fun renderLastSegment(canvas: Canvas) = render(canvas)

    override fun getBounds(): Rect? {
        val expand = strokeWidth / 2f
        return Rect(
            left = minOf(start.x, end.x) - expand,
            top = minOf(start.y, end.y) - expand,
            right = maxOf(start.x, end.x) + expand,
            bottom = maxOf(start.y, end.y) + expand,
        )
    }
}
