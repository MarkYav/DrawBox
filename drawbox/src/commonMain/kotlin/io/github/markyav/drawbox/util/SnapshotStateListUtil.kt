package io.github.markyav.drawbox.util

import androidx.compose.runtime.snapshots.SnapshotStateList

fun <T> SnapshotStateList<T>.pop(): T {
    val len = this.size
    val obj = this.last()
    removeAt(len - 1)
    return obj
}