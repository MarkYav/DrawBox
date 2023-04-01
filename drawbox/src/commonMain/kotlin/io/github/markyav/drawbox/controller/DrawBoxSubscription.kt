package io.github.markyav.drawbox.controller

sealed interface DrawBoxSubscription {
    object DynamicUpdate : DrawBoxSubscription
    object FinishDrawingUpdate : DrawBoxSubscription
}