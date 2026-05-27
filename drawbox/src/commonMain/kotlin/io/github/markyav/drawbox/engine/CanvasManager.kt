package io.github.markyav.drawbox.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntSize
import io.github.markyav.drawbox.engine.action.StrokeEraseAction

// [SP_ENG_01_05] Private rendering subsystem.
// Owns three bitmaps (checkpoint, committed, active), the action history, and undo/redo state.
// Never exposed outside the engine package.
internal class CanvasManager(
    private val logicalSize: IntSize,
    private val undoDepth: Int,
) {
    private val w = logicalSize.width.toFloat()
    private val h = logicalSize.height.toFloat()

    // [SP_ENG_01_05] Three private bitmaps.
    private val checkpointBitmap: ImageBitmap = ImageBitmap(logicalSize.width, logicalSize.height, ImageBitmapConfig.Argb8888)
    internal val committedBitmap: ImageBitmap = ImageBitmap(logicalSize.width, logicalSize.height, ImageBitmapConfig.Argb8888)
    internal val activeBitmap: ImageBitmap    = ImageBitmap(logicalSize.width, logicalSize.height, ImageBitmapConfig.Argb8888)

    internal val drawnActions: MutableList<DrawAction> = mutableListOf()
    internal val suppressedIndices: MutableSet<Int>    = mutableSetOf()

    // Maps erase-action index → list of action indices it suppressed.
    // Populated on commitStrokeErase; used to restore suppressed actions on undo.
    private val eraseRemovals: MutableMap<Int, List<Int>> = mutableMapOf()

    internal var currentIndex: Int = -1
        private set
    private var checkpointIndex: Int = -1

    val canUndo: Boolean get() = currentIndex >= 0
    val canRedo: Boolean get() = currentIndex < drawnActions.size - 1

    // [SP_ENG_02_05]
    fun commit(action: DrawAction) {
        discardRedoStack()
        drawnActions.add(action)
        currentIndex++
        Canvas(committedBitmap).also { action.render(it) }
        advanceCheckpointIfNeeded()
    }

    // [SP_ENG_02_06] Stroke erase — computes which actions intersect eraseRect, suppresses them.
    fun commitStrokeErase(action: StrokeEraseAction) {
        discardRedoStack()

        val eraseRect = action.eraseRect
        val fullCanvas = Rect(0f, 0f, w, h)
        val removed = (0..currentIndex).filter { i ->
            if (i in suppressedIndices) return@filter false
            val bounds = drawnActions[i].getBounds() ?: fullCanvas
            bounds.overlaps(eraseRect)
        }

        drawnActions.add(action)
        currentIndex++
        suppressedIndices.addAll(removed)
        eraseRemovals[currentIndex] = removed

        replayFromCheckpoint()
        advanceCheckpointIfNeeded()
    }

    // [SP_ENG_02_07]
    fun undo() {
        if (currentIndex < 0) return
        val undone = drawnActions[currentIndex]
        if (undone is StrokeEraseAction) {
            suppressedIndices.removeAll((eraseRemovals[currentIndex] ?: emptyList()).toSet())
        }
        currentIndex--
        replayFromCheckpoint()
    }

    // [SP_ENG_02_08]
    fun redo() {
        if (currentIndex >= drawnActions.size - 1) return
        currentIndex++
        val redone = drawnActions[currentIndex]
        if (redone is StrokeEraseAction) {
            suppressedIndices.addAll(eraseRemovals[currentIndex] ?: emptyList())
            replayFromCheckpoint()
        } else {
            Canvas(committedBitmap).also { redone.render(it) }
        }
        advanceCheckpointIfNeeded()
    }

    fun reset() {
        drawnActions.clear()
        suppressedIndices.clear()
        eraseRemovals.clear()
        currentIndex = -1
        checkpointIndex = -1
        clearBitmap(checkpointBitmap)
        clearBitmap(committedBitmap)
        clearBitmap(activeBitmap)
    }

    fun clearActiveBitmap() = clearBitmap(activeBitmap)

    // [SP_ENG_02_09] Rebuilds committedBitmap from the checkpoint state.
    private fun replayFromCheckpoint() {
        if (currentIndex >= checkpointIndex) {
            copyBitmap(checkpointBitmap, committedBitmap)
            for (i in (checkpointIndex + 1)..currentIndex) {
                if (i !in suppressedIndices) Canvas(committedBitmap).also { drawnActions[i].render(it) }
            }
        } else {
            clearBitmap(committedBitmap)
            for (i in 0..currentIndex) {
                if (i !in suppressedIndices) Canvas(committedBitmap).also { drawnActions[i].render(it) }
            }
        }
    }

    // [SP_ENG_02_10] Keeps checkpoint exactly undoDepth steps behind currentIndex.
    private fun advanceCheckpointIfNeeded() {
        while (currentIndex - checkpointIndex > undoDepth) {
            checkpointIndex++
            if (checkpointIndex !in suppressedIndices) {
                Canvas(checkpointBitmap).also { drawnActions[checkpointIndex].render(it) }
            }
        }
    }

    // Discards redo stack (actions beyond currentIndex) and cleans up associated metadata.
    private fun discardRedoStack() {
        val redoStart = currentIndex + 1
        for (i in redoStart until drawnActions.size) {
            eraseRemovals.remove(i)
        }
        if (redoStart < drawnActions.size) {
            drawnActions.subList(redoStart, drawnActions.size).clear()
        }
    }

    private fun clearBitmap(bitmap: ImageBitmap) {
        Canvas(bitmap).drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(),
            Paint().apply { blendMode = BlendMode.Clear })
    }

    private fun copyBitmap(src: ImageBitmap, dst: ImageBitmap) {
        clearBitmap(dst)
        Canvas(dst).drawImage(src, Offset.Zero, Paint())
    }
}
