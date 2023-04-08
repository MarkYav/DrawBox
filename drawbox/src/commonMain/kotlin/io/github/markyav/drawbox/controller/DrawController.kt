package io.github.markyav.drawbox.controller

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntSize
import io.github.markyav.drawbox.model.PathWrapper
import io.github.markyav.drawbox.util.createPath
import io.github.markyav.drawbox.util.pop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

/**
 * DrawController interacts with [DrawBox] and it allows you to control the canvas and all the components with it.
 */
class DrawController {
    private var state: DrawBoxConnectionState by mutableStateOf(DrawBoxConnectionState.Disconnected)

    /** A stateful list of [Path] that is drawn on the [Canvas]. */
    private val drawnPaths: SnapshotStateList<PathWrapper> = mutableStateListOf()

    private val completelyDrawnPaths: SnapshotStateList<PathWrapper> = mutableStateListOf()

    /** A stateful list of [Path] that is drawn on the [Canvas] and is scaled for the connected bitmap. */
    internal val pathToDrawOnCanvas: List<PathWrapper>? by derivedStateOf {
        (state as? DrawBoxConnectionState.Connected)?.let {
            drawnPaths.scale(it.size.toFloat())
        }
    }

    /** A stateful list of [Path] that was drawn on the [Canvas] but user retracted his action. */
    private val canceledPaths: SnapshotStateList<PathWrapper> = mutableStateListOf()

    /** An [canvasOpacity] of the [Canvas] in the [DrawBox] */
    var canvasOpacity: Float by mutableStateOf(1f)

    /** An [opacity] of the stroke */
    var opacity: Float by mutableStateOf(1f)

    /** A [strokeWidth] of the stroke */
    var strokeWidth: Float by mutableStateOf(10f)

    /** A [color] of the stroke */
    var color: Color by mutableStateOf(Color.Red)

    /** A [background] of the background of DrawBox */
    var background: DrawBoxBackground by mutableStateOf(DrawBoxBackground.NoBackground)

    /** Indicate how many redos it is possible to do. */
    val undoCount: Int by derivedStateOf { drawnPaths.size }

    /** Indicate how many undos it is possible to do. */
    val redoCount: Int by derivedStateOf { canceledPaths.size }

    /** Executes undo the drawn path if possible. */
    fun undo() {
        if (drawnPaths.isNotEmpty()) {
            canceledPaths.add(drawnPaths.pop())
        }
    }

    /** Executes redo the drawn path if possible. */
    fun redo() {
        if (canceledPaths.isNotEmpty()) {
            drawnPaths.add(canceledPaths.pop())
        }
    }

    /** Clear drawn paths and the bitmap image. */
    fun reset() {
        drawnPaths.clear()
        canceledPaths.clear()
    }

    /** Call this function when user starts drawing a path. */
    internal fun updateLatestPath(newPoint: Offset) {
        (state as? DrawBoxConnectionState.Connected)?.let {
            drawnPaths.last().points.add(newPoint.div(it.size.toFloat()))
        }
    }

    /** When dragging call this function to update the last path. */
    internal fun insertNewPath(newPoint: Offset) {
        (state as? DrawBoxConnectionState.Connected)?.let {
            val pathWrapper = PathWrapper(
                points = mutableStateListOf(newPoint.div(it.size.toFloat())),
                strokeColor = color,
                alpha = opacity,
                strokeWidth = strokeWidth.div(it.size.toFloat()),
            )
            drawnPaths.add(pathWrapper)
            canceledPaths.clear()
        }
    }

    internal fun finalizePath() {
        completelyDrawnPaths.clear()
        completelyDrawnPaths.addAll(drawnPaths)
    }

    /** Call this function to connect to the [DrawBox]. */
    internal fun connectToDrawBox(size: IntSize) {
        if (
            size.width > 0 &&
            size.height > 0 &&
            size.width == size.height //&&
        //state is DrawBoxConnectionState.Disconnected
        ) {
            state = DrawBoxConnectionState.Connected(size = size.width)
        }
    }

    private fun List<PathWrapper>.scale(size: Float): List<PathWrapper> {
        return this.map { pw ->
            val t = pw.points.map { it.times(size) }
            pw.copy(
                points = SnapshotStateList<Offset>().also { it.addAll(t) },
                strokeWidth = pw.strokeWidth * size
            )
        }
    }

    fun getBitmap(size: Int, coroutineScope: CoroutineScope, subscription: DrawBoxSubscription): StateFlow<ImageBitmap> {
        val initialBitmap = ImageBitmap(size, size, ImageBitmapConfig.Argb8888)
        return flow {
            val bitmap = ImageBitmap(size, size, ImageBitmapConfig.Argb8888)
            val canvas = Canvas(bitmap)
            val path = when (subscription) {
                is DrawBoxSubscription.DynamicUpdate -> drawnPaths
                is DrawBoxSubscription.FinishDrawingUpdate -> completelyDrawnPaths
            }
            path.scale(size.toFloat()).forEach { pw ->
                canvas.drawPath(
                    createPath(pw.points),
                    paint = Paint().apply {
                        color = pw.strokeColor
                        alpha = pw.alpha
                        style = PaintingStyle.Stroke
                        strokeCap = StrokeCap.Round
                        strokeJoin = StrokeJoin.Round
                        strokeWidth = pw.strokeWidth
                    }
                )
            }
            emit(bitmap)
        }.stateIn(coroutineScope, started = SharingStarted.Eagerly, initialValue = initialBitmap)
    }
}