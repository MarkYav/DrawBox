package io.github.markyav.drawbox.engine.util

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.sqrt

// [PL_ENG Phase 3] Ramer-Douglas-Peucker path simplification.
// Removes redundant intermediate points while preserving visual shape.
internal object PathSimplifier {

    fun simplify(points: List<Offset>, tolerance: Float = 1f): List<Offset> {
        if (points.size <= 2) return points

        val start = points.first()
        val end = points.last()

        var maxDist = 0f
        var maxIdx = 0
        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], start, end)
            if (dist > maxDist) {
                maxDist = dist
                maxIdx = i
            }
        }

        return if (maxDist > tolerance) {
            val left = simplify(points.subList(0, maxIdx + 1), tolerance)
            val right = simplify(points.subList(maxIdx, points.size), tolerance)
            left.dropLast(1) + right
        } else {
            listOf(start, end)
        }
    }

    private fun perpendicularDistance(point: Offset, lineStart: Offset, lineEnd: Offset): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val lineLengthSq = dx * dx + dy * dy
        if (lineLengthSq == 0f) return euclidean(point, lineStart)
        val t = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / lineLengthSq
        val tClamped = t.coerceIn(0f, 1f)
        return euclidean(point, Offset(lineStart.x + tClamped * dx, lineStart.y + tClamped * dy))
    }

    private fun euclidean(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}
