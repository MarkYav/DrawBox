package io.github.markyav.drawbox.util

import io.github.markyav.drawbox.model.NormPoint
import kotlin.math.sqrt
import kotlin.collections.ArrayDeque

/**
 * A stateless utility object for simplifying and smoothing strokes.
 */
object StrokeProcessor {
    /**
     * Simplifies raw touch points before committing an action.
     * Uses the iterative Ramer-Douglas-Peucker algorithm to remove redundant points.
     * Distance is calculated in pixel coordinates to avoid aspect ratio distortion.
     * 
     * @param points The raw stroke points in normalized coordinates.
     * @param canvasWidth The width of the canvas in pixels.
     * @param canvasHeight The height of the canvas in pixels.
     * @param pixelEpsilon The maximum allowed distance (in pixels) for a point to be discarded.
     */
    fun process(
        points: List<NormPoint>,
        canvasWidth: Float,
        canvasHeight: Float,
        pixelEpsilon: Float = 2.0f
    ): List<NormPoint> {
        if (points.size <= 2) return points

        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.size - 1] = true

        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(Pair(0, points.size - 1))

        while (stack.isNotEmpty()) {
            val (start, end) = stack.removeLast()
            var dmax = 0f
            var index = 0

            for (i in start + 1 until end) {
                val d = perpendicularDistancePixels(
                    points[i], points[start], points[end],
                    canvasWidth, canvasHeight
                )
                if (d > dmax) {
                    index = i
                    dmax = d
                }
            }

            if (dmax > pixelEpsilon) {
                keep[index] = true
                stack.addLast(Pair(start, index))
                stack.addLast(Pair(index, end))
            }
        }

        val result = mutableListOf<NormPoint>()
        for (i in points.indices) {
            if (keep[i]) {
                result.add(points[i])
            }
        }
        return result
    }

    private fun perpendicularDistancePixels(
        pt: NormPoint,
        lineStart: NormPoint,
        lineEnd: NormPoint,
        w: Float,
        h: Float
    ): Float {
        val startX = lineStart.x * w
        val startY = lineStart.y * h
        val endX = lineEnd.x * w
        val endY = lineEnd.y * h
        val pX = pt.x * w
        val pY = pt.y * h

        val dx = endX - startX
        val dy = endY - startY
        
        // If lineStart and lineEnd are the same point, return distance to that point
        if (dx == 0f && dy == 0f) {
            val vx = pX - startX
            val vy = pY - startY
            return sqrt(vx * vx + vy * vy)
        }
        
        val num = kotlin.math.abs(dy * pX - dx * pY + endX * startY - endY * startX)
        val den = sqrt(dx * dx + dy * dy)
        return num / den
    }
}
