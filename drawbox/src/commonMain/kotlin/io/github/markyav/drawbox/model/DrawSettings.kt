package io.github.markyav.drawbox.model

data class DrawSettings(
    val tool: DrawTool = DrawTool.Brush,
    val color: Long = 0xFFFF0000,
    val strokeWidth: Float = 10f,
    val fillTolerance: Float = 32f
)
