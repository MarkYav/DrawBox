package io.github.markyav.drawbox.engine

import androidx.compose.ui.graphics.Color

// [SP_ENG_01_01] Immutable snapshot of the user's paint settings, captured at gesture start.
// Regular class (not data class) so that opacity clamping can be enforced at every construction path.
class PaintOptions(
    val color: Color,
    val strokeWidth: Float,
    opacity: Float,
) {
    // Clamped at every construction path — satisfies SP_ENG_04 invariant.
    val opacity: Float = opacity.coerceIn(0f, 1f)

    init {
        require(strokeWidth > 0f) { "strokeWidth must be > 0, was $strokeWidth" }
    }
}
