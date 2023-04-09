package io.github.markyav.drawbox.model

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class PathWrapper(
    var points: List<Offset>,
    val strokeWidth: Float = 5f,
    val strokeColor: Color,
    val alpha: Float = 1f
)