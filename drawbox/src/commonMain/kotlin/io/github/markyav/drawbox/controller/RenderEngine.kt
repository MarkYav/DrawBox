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

internal object RenderEngine {
    
    fun render(history: ActionHistory, target: ImageBitmap) {
        clearBitmap(target)
        history.actions.forEach { action ->
            renderAction(action, target)
        }
    }

    fun renderAction(action: Action, target: ImageBitmap) {
        val canvas = Canvas(target)
        val width = target.width.toFloat()
        val height = target.height.toFloat()
        
        when (action) {
            is BrushAction -> {
                if (action.points.isEmpty()) return
                val paint = Paint().apply {
                    color = Color(action.color)
                    strokeWidth = action.strokeWidth
                    style = PaintingStyle.Stroke
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
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
