# Module Analysis — controller

**Path:** `drawbox/src/commonMain/kotlin/io/github/markyav/drawbox/controller/`  
**Layer:** 1 (depends on model, util)

## Files

### DrawBoxConnectionState.kt

Sealed interface: `Disconnected` | `Connected(size: Int, alpha: Float = 1f)`.
- `size` = canvas side length in pixels (square-only).
- `alpha` = unused (dead field — see ISSUE-001).

### DrawBoxBackground.kt (controller)

Sealed interface describing the background behind the drawing canvas:
- `NoBackground` — transparent / nothing.
- `ColourBackground(color: Color, alpha: Float = 1f)` — solid colour fill.
- `ImageBackground(bitmap: ImageBitmap, alpha: Float = 1f)` — bitmap fill.

### DrawBoxSubscription.kt

Sealed interface — consumer subscription mode:
- `DynamicUpdate` — emits on every pointer move (real-time preview).
- `FinishDrawingUpdate` — emits only when a stroke is finalized.

### OpenedImage.kt

Sealed interface representing the background bitmap opened for drawing-over:
- `None` — no image loaded.
- `Image(image, dstSize, srcOffset, srcSize)` — center-crop math baked into defaults:
  - `srcOffset` = negative half-delta to center the smaller dimension.
  - `srcSize` = square crop of `min(width, height)`.

### DrawController.kt

**Public API (consumer-facing):**
- `canvasOpacity: MutableStateFlow<Float>` — opacity of entire canvas layer [0..1].
- `opacity: MutableStateFlow<Float>` — stroke opacity [0..1].
- `strokeWidth: MutableStateFlow<Float>` — stroke width in raw pixels (normalized before storage).
- `color: MutableStateFlow<Color>` — stroke color.
- `background: MutableStateFlow<DrawBoxBackground>` — background configuration.
- `openedImage: MutableStateFlow<ImageBitmap?>` — currently loaded image.
- `undoCount: StateFlow<Int>` — number of undoable strokes.
- `redoCount: StateFlow<Int>` — number of redoable strokes.
- `undo()` — moves last drawn path to canceledPaths.
- `redo()` — moves last canceled path back to drawnPaths.
- `reset()` — clears drawnPaths + canceledPaths.
- `open(image: ImageBitmap)` — calls reset() then sets openedImage.
- `getDrawPath(subscription): StateFlow<List<PathWrapper>>` — returns normalized paths.
- `getBitmap(size: Int, subscription): StateFlow<ImageBitmap>` — off-screen render to bitmap.

**Internal API (DrawBox-only):**
- `connectToDrawBox(size: IntSize)` — validates square, sets Connected state.
- `updateLatestPath(newPoint: Offset)` — appends normalized point to active path.
- `insertNewPath(newPoint: Offset)` — starts new active path; clears redo stack.
- `finalizePath()` — commits active path to drawnPaths as PathWrapper.
- `onTap(newPoint: Offset)` — insertNewPath + finalizePath in one gesture.
- `getPathWrappersForDrawbox(subscription)` — scaled paths for canvas rendering.
- `getOpenImageForDrawbox(size: Int?)` — scaled image for canvas rendering.

**State machine:**
```
Disconnected --[connectToDrawBox(square)]--► Connected(size)
```
Only `Connected` state allows path operations. Non-square sizes keep it Disconnected.

**Normalization invariant:**
- Points stored in [0..1]: `newPoint.div(connectedState.size.toFloat())`.
- Rendering: `scale(size.toFloat())` multiplies all points and strokeWidth back.

**Issues found:**
- ISSUE-002: Compose runtime import (`androidx.compose.runtime.*`) — planned removal.
- ISSUE-003: Commented-out code in `insertNewPath` (lines 102–107).
