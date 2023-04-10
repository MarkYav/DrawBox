package io.github.markyav.drawbox.android.drawing.item

enum class SketchDrawingClass(
    val colorMeaning: String,
    val color: String,
) {
    Skin(colorMeaning = "Skin", color = "#7fd4ff"),
    BrowLeft(colorMeaning = "Left brow", color = "#ffff7f"),
    BrowRight(colorMeaning = "Right brow", color = "#ffff7f"),
    EyeLeft(colorMeaning = "Left eye", color = "#ffffaa"),
    EyeRight(colorMeaning = "Right eye", color = "#ffffaa"),
    NoseRight(colorMeaning = "Right nose", color = "#f09df0"),
    NoseLeft(colorMeaning = "Left nose", color = "#ffd4ff"),
    Mouth(colorMeaning = "Mouth", color = "#59405c"),
    UpperLip(colorMeaning = "Upper lip", color = "#ed6663"),
    LowLip(colorMeaning = "Low lip", color = "#b53b65"),
    EarLeft(colorMeaning = "Left ear", color = "#00ff55"),
    EarRight(colorMeaning = "Right ear", color = "#00ff55"),
    Earring(colorMeaning = "Earring", color = "#00ffaa"),
    EyeGlass(colorMeaning = "Glasses", color = "#ffffaa"),
    Neck(colorMeaning = "Neck", color = "#7faaff"),
    NeckRing(colorMeaning = "Neck ring", color = "#5500ff"),
    Cloth(colorMeaning = "Cloth", color = "#ffaa7f"),
    Hair(colorMeaning = "Hair", color = "#d47fff"),
    Hat(colorMeaning = "Hat", color = "#00aaff"),
}

fun getSketchDrawingClassList(): List<SketchDrawingClass> {
    return SketchDrawingClass.values().toList()
}
