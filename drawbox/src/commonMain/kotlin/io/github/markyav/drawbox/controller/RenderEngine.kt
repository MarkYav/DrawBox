package io.github.markyav.drawbox.controller

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import io.github.markyav.drawbox.model.Action
import io.github.markyav.drawbox.model.ActionHistory
import io.github.markyav.drawbox.model.BrushAction
import io.github.markyav.drawbox.model.EraserAction
import io.github.markyav.drawbox.model.FillAction
import io.github.markyav.drawbox.model.RemoveAction

internal object RenderEngine {
    
    fun render(history: ActionHistory, target: ImageBitmap, scale: Float = 1f) {
        clearBitmap(target)
        val removedIds = history.actions.filterIsInstance<RemoveAction>().flatMap { it.removedActionIds }.toSet()
        history.actions.forEach { action ->
            if (action.id !in removedIds && action !is RemoveAction) {
                renderAction(action, target, scale)
            }
        }
    }

    fun renderAction(action: Action, target: ImageBitmap, scale: Float = 1f) {
        val canvas = Canvas(target)
        val width = target.width.toFloat()
        val height = target.height.toFloat()
        
        when (action) {
            is BrushAction -> {
                if (action.points.isEmpty()) return
                val paint = Paint().apply {
                    color = Color(action.color)
                    strokeWidth = action.strokeWidth * scale
                    style = PaintingStyle.Stroke
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                }
                
                val path = Path()
                val first = action.points.first()
                path.moveTo(first.x * width, first.y * height)
                
                // If it's a single point, just draw a point
                if (action.points.size == 1) {
                    canvas.drawCircle(Offset(first.x * width, first.y * height), (action.strokeWidth * scale) / 2, paint.apply { style = PaintingStyle.Fill })
                    return
                }

                for (i in 1 until action.points.size) {
                    val pt = action.points[i]
                    path.lineTo(pt.x * width, pt.y * height)
                }
                canvas.drawPath(path, paint)
            }
            is EraserAction -> {
                if (action.points.isEmpty()) return
                val paint = Paint().apply {
                    strokeWidth = action.strokeWidth * scale
                    style = PaintingStyle.Stroke
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                    blendMode = BlendMode.Clear // Erases pixels
                }
                
                val path = Path()
                val first = action.points.first()
                path.moveTo(first.x * width, first.y * height)
                
                // If it's a single point, just draw a point
                if (action.points.size == 1) {
                    canvas.drawCircle(Offset(first.x * width, first.y * height), action.strokeWidth / 2, paint.apply { style = PaintingStyle.Fill })
                    return
                }

                for (i in 1 until action.points.size) {
                    val pt = action.points[i]
                    path.lineTo(pt.x * width, pt.y * height)
                }
                canvas.drawPath(path, paint)
            }
            is RemoveAction -> {
                // Do nothing when drawing incrementally, processed in render() full rebuild.
            }
            is FillAction -> {
                renderFill(action, target, scale)
            }
        }
    }

    private class Point(val x: Int, val y: Int)

    fun calculateFillSpans(
        target: ImageBitmap,
        startXNorm: Float,
        startYNorm: Float,
        tolerance: Float,
        fillColorInt: Int
    ): List<io.github.markyav.drawbox.model.FillSpan> {
        val width = target.width
        val height = target.height
        val startX = (startXNorm * width).toInt().coerceIn(0, width - 1)
        val startY = (startYNorm * height).toInt().coerceIn(0, height - 1)

        val pixels = IntArray(width * height)
        target.readPixels(pixels)

        val targetColor = pixels[startY * width + startX]

        if (targetColor == fillColorInt) return emptyList()

        val visited = BooleanArray(width * height)
        val spans = mutableListOf<io.github.markyav.drawbox.model.FillSpan>()
        val stack = mutableListOf<Point>()
        stack.add(Point(startX, startY))

        while (stack.isNotEmpty()) {
            val pt = stack.removeLast()
            val cx = pt.x
            val cy = pt.y

            if (visited[cy * width + cx]) continue
            if (colorDistance(pixels[cy * width + cx], targetColor) > tolerance) continue

            var lx = cx
            while (lx > 0 && !visited[cy * width + (lx - 1)] && colorDistance(pixels[cy * width + (lx - 1)], targetColor) <= tolerance) {
                lx--
            }

            var rx = cx
            while (rx < width - 1 && !visited[cy * width + (rx + 1)] && colorDistance(pixels[cy * width + (rx + 1)], targetColor) <= tolerance) {
                rx++
            }

            spans.add(io.github.markyav.drawbox.model.FillSpan(lx, rx, cy))

            for (x in lx..rx) {
                visited[cy * width + x] = true
            }

            if (cy < height - 1) {
                var x = lx
                while (x <= rx) {
                    if (!visited[(cy + 1) * width + x] && colorDistance(pixels[(cy + 1) * width + x], targetColor) <= tolerance) {
                        stack.add(Point(x, cy + 1))
                    }
                    x++
                }
            }

            if (cy > 0) {
                var x = lx
                while (x <= rx) {
                    if (!visited[(cy - 1) * width + x] && colorDistance(pixels[(cy - 1) * width + x], targetColor) <= tolerance) {
                        stack.add(Point(x, cy - 1))
                    }
                    x++
                }
            }
        }
        return spans
    }

    private fun renderFill(action: FillAction, target: ImageBitmap, scale: Float) {
        val canvas = Canvas(target)
        val paint = Paint().apply {
            color = Color(action.color)
            style = PaintingStyle.Fill
        }

        for (span in action.spans) {
            canvas.drawRect(span.lx.toFloat() * scale, span.y.toFloat() * scale, (span.rx.toFloat() + 1f) * scale, (span.y.toFloat() + 1f) * scale, paint)
        }
    }

    private fun colorDistance(c1: Int, c2: Int): Float {
        val r1 = (c1 shr 16) and 0xFF
        val g1 = (c1 shr 8) and 0xFF
        val b1 = c1 and 0xFF
        val a1 = (c1 shr 24) and 0xFF

        val r2 = (c2 shr 16) and 0xFF
        val g2 = (c2 shr 8) and 0xFF
        val b2 = c2 and 0xFF
        val a2 = (c2 shr 24) and 0xFF

        val dr = r1 - r2
        val dg = g1 - g2
        val db = b1 - b2
        val da = a1 - a2
        return kotlin.math.sqrt((dr * dr + dg * dg + db * db + da * da).toFloat())
    }

    fun clearBitmap(target: ImageBitmap) {
        val canvas = Canvas(target)
        val paint = Paint().apply {
            blendMode = BlendMode.Clear
        }
        canvas.drawRect(0f, 0f, target.width.toFloat(), target.height.toFloat(), paint)
    }
}
