package io.github.markyav.drawbox.engine.action

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import io.github.markyav.drawbox.engine.DrawAction

// [SP_ENG_01_07] Records a stroke-erase gesture by its erase region.
// CanvasManager computes which committed actions fall within eraseRect and suppresses them.
// render is a no-op — visual effect comes from suppression during history replay.
class StrokeEraseAction(val eraseRect: Rect) : DrawAction {
    override fun render(canvas: Canvas) {}
    override fun getBounds(): Rect? = null
}
