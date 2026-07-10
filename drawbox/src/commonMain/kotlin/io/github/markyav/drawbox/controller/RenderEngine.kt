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
        }
    }

    fun clearBitmap(target: ImageBitmap) {
        val canvas = Canvas(target)
        val paint = Paint().apply {
            blendMode = BlendMode.Clear
        }
        canvas.drawRect(0f, 0f, target.width.toFloat(), target.height.toFloat(), paint)
    }
}
