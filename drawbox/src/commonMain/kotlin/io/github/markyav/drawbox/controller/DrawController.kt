package io.github.markyav.drawbox.controller

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import io.github.markyav.drawbox.engine.DrawEngine
import io.github.markyav.drawbox.engine.DrawTool
import io.github.markyav.drawbox.engine.PaintOptions
import io.github.markyav.drawbox.engine.ToolContext
import io.github.markyav.drawbox.engine.tool.BrushTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// [SP_CTL_v2_01_01] Public facade: coordinates paint state, gesture routing, and undo/redo.
// All operations must be called from the main thread.
class DrawController(
    val logicalSize: IntSize,
    private val undoDepth: Int = 20,
    defaultTool: DrawTool = BrushTool,
) {
    init {
        require(logicalSize.width > 0 && logicalSize.height > 0) {
            "logicalSize dimensions must be > 0"
        }
        require(undoDepth >= 1) { "undoDepth must be >= 1, was $undoDepth" }
    }

    // Engine — created once at construction, never replaced. [SP_CTL_v2_01_01]
    private val engine = DrawEngine(logicalSize, undoDepth)

    // Connection state: null = Disconnected, non-null = Connected(screenSize). [SP_CTL_v2_01_02]
    private var screenSize: IntSize? = null

    // Gesture state: null = Idle, non-null = Gesturing (tool + context locked at gesture start). [SP_CTL_v2_01_03]
    private var gestureActiveTool: DrawTool? = null
    private var gestureContext: ToolContext? = null

    // --- Mutable observable paint state [SP_CTL_v2_01_01] ---

    /** Current stroke color. */
    val color: MutableStateFlow<Color> = MutableStateFlow(Color.Black)

    /** Current stroke width in logical pixels. Values <= 0 are treated as 0.1 at draw time. */
    val strokeWidth: MutableStateFlow<Float> = MutableStateFlow(4f)

    /** Current stroke opacity in [0.0, 1.0]. Out-of-range values are clamped at draw time. */
    val opacity: MutableStateFlow<Float> = MutableStateFlow(1f)

    /** Currently selected drawing tool. Changes during a gesture take effect after gesture ends. */
    val activeTool: MutableStateFlow<DrawTool> = MutableStateFlow(defaultTool)

    // --- Read-only observable history state [SP_CTL_v2_01_01] ---

    private val _canUndo = MutableStateFlow(false)
    /** True when there is at least one committed action to undo. */
    val canUndo: StateFlow<Boolean> = _canUndo

    private val _canRedo = MutableStateFlow(false)
    /** True when there is at least one undone action to redo. */
    val canRedo: StateFlow<Boolean> = _canRedo

    // Monotonically increasing counter; DrawBoxCanvas observes this to know when to re-render.
    // [SP_CTL_v2_04_03]
    private val _invalidationTick = MutableStateFlow(0)
    internal val invalidationTick: StateFlow<Int> = _invalidationTick

    // --- Private helpers ---

    // [SP_CTL_v2_03] strokeWidth/opacity are clamped here, not at the StateFlow write site,
    // to comply with the style rule against getter/setter wrapping on MutableStateFlow.
    private fun buildPaintOptions() = PaintOptions(
        color = color.value,
        strokeWidth = strokeWidth.value.coerceAtLeast(0.1f),
        opacity = opacity.value.coerceIn(0f, 1f),
    )

    // [SP_CTL_v2_04_04] Always called before invalidate() so observers see consistent state.
    private fun refreshUndoRedo() {
        _canUndo.value = engine.canUndo
        _canRedo.value = engine.canRedo
    }

    // [SP_CTL_v2_04_03]
    private fun invalidate() {
        _invalidationTick.value++
    }

    // [SP_CTL_v2_02_09]
    private fun mapToLogical(screenPoint: Offset): Offset {
        val s = checkNotNull(screenSize) { "mapToLogical called while disconnected" }
        return Offset(
            x = screenPoint.x * logicalSize.width.toFloat() / s.width.toFloat(),
            y = screenPoint.y * logicalSize.height.toFloat() / s.height.toFloat(),
        )
    }

    // Finalizes the in-progress gesture (committing whatever partial action exists).
    // Called when a canvas resize arrives mid-gesture. [SP_CTL_v2_02_01]
    private fun abortGesture() {
        engine.endGesture()
        gestureActiveTool = null
        gestureContext = null
        refreshUndoRedo()
        invalidate()
    }

    // --- Public contracts ---

    /** Called by DrawBoxCanvas when its layout size changes. [SP_CTL_v2_02_01] */
    fun onCanvasSizeChanged(size: IntSize) {
        if (size.width <= 0 || size.height <= 0) return
        if (gestureActiveTool != null) abortGesture()
        screenSize = size
    }

    /** Called by DrawBoxCanvas on pointer-down. [SP_CTL_v2_02_02] */
    fun onGestureStart(screenPoint: Offset) {
        if (screenSize == null || gestureActiveTool != null) return
        val tool = activeTool.value
        val context = engine.createToolContext(buildPaintOptions())
        engine.beginGesture(tool, mapToLogical(screenPoint), context)
        gestureActiveTool = tool
        gestureContext = context
        invalidate()
    }

    /** Called by DrawBoxCanvas on each pointer-move event. [SP_CTL_v2_02_03] */
    fun onGestureMove(from: Offset, to: Offset) {
        if (gestureActiveTool == null) return
        engine.extendGesture(mapToLogical(from), mapToLogical(to))
        invalidate()
    }

    /** Called by DrawBoxCanvas on pointer-up or pointer-cancel. [SP_CTL_v2_02_04] */
    fun onGestureEnd() {
        if (gestureActiveTool == null) return
        engine.endGesture()
        gestureActiveTool = null
        gestureContext = null
        refreshUndoRedo()
        invalidate()
    }

    /** Called by DrawBoxCanvas on a single-point tap gesture. [SP_CTL_v2_02_05] */
    fun onTap(screenPoint: Offset) {
        if (screenSize == null || gestureActiveTool != null) return
        val tool = activeTool.value
        val context = engine.createToolContext(buildPaintOptions())
        engine.onTap(tool, mapToLogical(screenPoint), context)
        refreshUndoRedo()
        invalidate()
    }

    /** Undoes the last committed action. No-op during an active gesture. [SP_CTL_v2_02_06] */
    fun undo() {
        if (gestureActiveTool != null) return
        engine.undo()
        refreshUndoRedo()
        invalidate()
    }

    /** Redoes the last undone action. No-op during an active gesture. [SP_CTL_v2_02_07] */
    fun redo() {
        if (gestureActiveTool != null) return
        engine.redo()
        refreshUndoRedo()
        invalidate()
    }

    /** Clears all committed actions and resets the canvas. No-op during an active gesture. [SP_CTL_v2_02_08] */
    fun reset() {
        if (gestureActiveTool != null) return
        engine.reset()
        refreshUndoRedo()
        invalidate()
    }

    /** Returns the composed display image (committed + active canvas). [SP_CTL_v2_02_10] */
    fun getDisplayOutput(): ImageBitmap = engine.getDisplayOutput()

    /** Exports the committed drawing at the given pixel size. [SP_CTL_v2_02_11] */
    fun exportBitmap(size: IntSize): ImageBitmap {
        require(size.width > 0 && size.height > 0) {
            "export size dimensions must be > 0"
        }
        return engine.exportBitmap(size)
    }
}
