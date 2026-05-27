package io.github.markyav.drawbox.engine.tool

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntSize
import io.github.markyav.drawbox.engine.DrawAction
import io.github.markyav.drawbox.engine.DrawTool
import io.github.markyav.drawbox.engine.ToolContext
import io.github.markyav.drawbox.engine.action.FillAction
import kotlin.math.abs

// [SP_ENG_01_08] Flood fill — BFS from tap point, replaces contiguous same-colored region.
// onTap is the only active callback; all gesture callbacks return null.
object FillTool : DrawTool {

    private const val TOLERANCE = 8f / 255f  // per-channel color similarity threshold

    override val clearsActiveCanvasOnDrag = false

    override fun onGestureStart(point: Offset, context: ToolContext): DrawAction? = null
    override fun onGestureDrag(from: Offset, to: Offset, current: DrawAction?, context: ToolContext): DrawAction? = null
    override fun onGestureEnd(current: DrawAction?, context: ToolContext): DrawAction? = null

    override fun onTap(point: Offset, context: ToolContext): DrawAction {
        val filled = floodFill(context.committedBitmap, point, context.paintOptions.color, context.logicalSize)
        return FillAction(filled)
    }

    private fun floodFill(
        source: ImageBitmap,
        seed: Offset,
        fillColor: Color,
        logicalSize: IntSize,
    ): ImageBitmap {
        val w = logicalSize.width
        val h = logicalSize.height
        val pixelMap = source.toPixelMap()
        val seedX = seed.x.toInt().coerceIn(0, w - 1)
        val seedY = seed.y.toInt().coerceIn(0, h - 1)
        val targetColor = pixelMap[seedX, seedY]

        val result = ImageBitmap(w, h, ImageBitmapConfig.Argb8888)
        if (colorSimilar(targetColor, fillColor)) return result  // already filled — return empty

        val canvas = Canvas(result)
        val paint = Paint().apply {
            color = fillColor
            style = PaintingStyle.Fill
        }

        val visited = Array(w) { BooleanArray(h) }
        val queue = ArrayDeque<Int>()  // encoded as y*w + x

        fun enqueue(x: Int, y: Int) {
            if (x !in 0 until w || y !in 0 until h || visited[x][y]) return
            if (colorSimilar(pixelMap[x, y], targetColor)) {
                visited[x][y] = true
                queue.add(y * w + x)
            }
        }

        visited[seedX][seedY] = true
        queue.add(seedY * w + seedX)

        while (queue.isNotEmpty()) {
            val code = queue.removeFirst()
            val x = code % w
            val y = code / w
            canvas.drawRect(x.toFloat(), y.toFloat(), (x + 1).toFloat(), (y + 1).toFloat(), paint)
            enqueue(x - 1, y)
            enqueue(x + 1, y)
            enqueue(x, y - 1)
            enqueue(x, y + 1)
        }

        return result
    }

    private fun colorSimilar(a: Color, b: Color): Boolean =
        abs(a.red - b.red) <= TOLERANCE &&
        abs(a.green - b.green) <= TOLERANCE &&
        abs(a.blue - b.blue) <= TOLERANCE &&
        abs(a.alpha - b.alpha) <= TOLERANCE
}
