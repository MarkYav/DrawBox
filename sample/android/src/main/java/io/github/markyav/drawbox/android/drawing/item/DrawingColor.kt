package io.github.markyav.drawbox.android.drawing.item

import androidx.compose.ui.graphics.Color

data class DrawingColor(
    val color: Color,
    val className: String,
)

fun SketchDrawingClass.toDrawingColor(): DrawingColor {
    return DrawingColor(
        color = Color(android.graphics.Color.parseColor(this.color)),
        className = this.colorMeaning,
    )
}

fun List<SketchDrawingClass>.toDrawingColorList(): List<DrawingColor> {
    return this.map { it.toDrawingColor() }
}