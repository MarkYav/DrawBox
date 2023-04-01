package io.github.markyav.drawbox.controller

sealed interface DrawBoxConnectionState {
    object Disconnected : DrawBoxConnectionState
    data class Connected(val size: Int, val alpha: Float = 1f) : DrawBoxConnectionState // it is square
}