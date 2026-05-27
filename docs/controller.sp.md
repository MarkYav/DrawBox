---
ID: SP_CTL
Title: Draw Controller — Specification
Status: active
Created: 2026-05-27
Updated: 2026-05-27
Concept: [C_CTL](controller.concept.md)
Depends on specs: [SP_UTL](util.sp.md), [SP_MDL](model.sp.md)
Used by specs: [SP_BOX](box.sp.md)
---

# SP_CTL — Draw Controller Specification

## 01 — Data Structures

### `DrawBoxConnectionState`

```kotlin
sealed interface DrawBoxConnectionState {
    object Disconnected : DrawBoxConnectionState
    data class Connected(val size: Int, val alpha: Float = 1f) : DrawBoxConnectionState
}
```

| Variant | Fields | Notes |
|---------|--------|-------|
| `Disconnected` | none | Initial state; all path ops silently no-op |
| `Connected` | `size: Int` | Square canvas side in pixels; `alpha` is unused (ISSUE-001) |

### `DrawBoxBackground` (controller)

```kotlin
sealed interface DrawBoxBackground {
    object NoBackground : DrawBoxBackground
    data class ColourBackground(val color: Color, val alpha: Float = 1f) : DrawBoxBackground
    data class ImageBackground(val bitmap: ImageBitmap, val alpha: Float = 1f) : DrawBoxBackground
}
```

### `DrawBoxSubscription`

```kotlin
sealed interface DrawBoxSubscription {
    object DynamicUpdate : DrawBoxSubscription      // emits on every pointer move
    object FinishDrawingUpdate : DrawBoxSubscription // emits only on finalizePath
}
```

### `OpenedImage`

```kotlin
sealed interface OpenedImage {
    object None : OpenedImage
    data class Image(
        val image: ImageBitmap,
        val dstSize: IntSize,
        val srcOffset: IntOffset,  // default: center-crop offset
        val srcSize: IntSize,      // default: min(width, height) square crop
    ) : OpenedImage
}
```

Center-crop defaults: `srcOffset.x = -(min(w,h) - w) / 2`, `srcOffset.y = -(min(w,h) - h) / 2`,
`srcSize = IntSize(min(w,h), min(w,h))`.

### `DrawController` — public state properties

| Property | Type | Default | Range / Notes |
|----------|------|---------|---------------|
| `canvasOpacity` | `MutableStateFlow<Float>` | `1f` | [0..1] — entire canvas layer |
| `opacity` | `MutableStateFlow<Float>` | `1f` | [0..1] — per-stroke |
| `strokeWidth` | `MutableStateFlow<Float>` | `10f` | Raw pixels; normalized on storage |
| `color` | `MutableStateFlow<Color>` | `Color.Red` | Any Compose Color |
| `background` | `MutableStateFlow<DrawBoxBackground>` | `NoBackground` | — |
| `openedImage` | `MutableStateFlow<ImageBitmap?>` | `null` | — |
| `undoCount` | `StateFlow<Int>` | `0` | Count of undoable strokes |
| `redoCount` | `StateFlow<Int>` | `0` | Count of redoable strokes |

---

## 02 — Contracts

### `SP_CTL_01 — connectToDrawBox` (internal)

```kotlin
internal fun connectToDrawBox(size: IntSize)
```

| | |
|-|-|
| Input | `IntSize` from Compose layout |
| Output | State transitions to `Connected(size.width)` if `width > 0 && height > 0 && width == height` |
| Error case: non-square | Silent no-op; state remains unchanged |
| Error case: zero size | Silent no-op |

### `SP_CTL_02 — insertNewPath` (internal)

```kotlin
internal fun insertNewPath(newPoint: Offset)
```

| | |
|-|-|
| Input | First pointer position (pixel space) |
| Pre-condition | `activeDrawingPath.value == null` (enforced by `require`) |
| Post-condition | `activeDrawingPath` is `listOf(newPoint / size)` |
| Side effect | `canceledPaths` is cleared |
| Error case: disconnected | Silent no-op |

### `SP_CTL_03 — updateLatestPath` (internal)

```kotlin
internal fun updateLatestPath(newPoint: Offset)
```

| | |
|-|-|
| Input | Current pointer position (pixel space) |
| Pre-condition | `activeDrawingPath.value != null` (enforced by `require`) |
| Post-condition | Normalized point appended to `activeDrawingPath` |
| Error case: disconnected | Silent no-op |

### `SP_CTL_04 — finalizePath` (internal)

```kotlin
internal fun finalizePath()
```

| | |
|-|-|
| Pre-condition | `activeDrawingPath.value != null` (enforced by `require`) |
| Post-condition | `PathWrapper` appended to `drawnPaths`; `activeDrawingPath` set to `null` |
| PathWrapper built with | `points=activeDrawingPath`, `strokeColor=color.value`, `alpha=opacity.value`, `strokeWidth=strokeWidth.value/size` |

### `SP_CTL_05 — onTap` (internal)

```kotlin
internal fun onTap(newPoint: Offset)
```

Equivalent to `insertNewPath(newPoint)` followed immediately by `finalizePath()`.
Results in a single-point stroke (dot).

### `SP_CTL_06 — undo`

```kotlin
fun undo()
```

| | |
|-|-|
| Pre-condition | `drawnPaths` is non-empty |
| Post-condition | Last element moved from `drawnPaths` to `canceledPaths` |
| Error case: empty | Silent no-op |

### `SP_CTL_07 — redo`

```kotlin
fun redo()
```

| | |
|-|-|
| Pre-condition | `canceledPaths` is non-empty |
| Post-condition | Last element moved from `canceledPaths` to `drawnPaths` |
| Error case: empty | Silent no-op |

### `SP_CTL_08 — reset`

```kotlin
fun reset()
```

Clears `drawnPaths` and `canceledPaths`. Does not affect `openedImage`.

### `SP_CTL_09 — open`

```kotlin
fun open(image: ImageBitmap)
```

Calls `reset()` then sets `openedImage.value = image`.

### `SP_CTL_10 — getDrawPath`

```kotlin
fun getDrawPath(subscription: DrawBoxSubscription): StateFlow<List<PathWrapper>>
```

Returns **normalized** paths (not scaled):
- `DynamicUpdate` → `combineStates(drawnPaths, activeDrawingPath)` including active stroke.
- `FinishDrawingUpdate` → `drawnPaths` only.

### `SP_CTL_11 — getBitmap`

```kotlin
fun getBitmap(size: Int, subscription: DrawBoxSubscription): StateFlow<ImageBitmap>
```

Off-screen render: creates `ImageBitmap(size, size)`, draws opened image (center-crop), then
all paths scaled to `size`. Returns as `StateFlow` — reactive, updates on each draw event per subscription.

### `SP_CTL_12 — getPathWrappersForDrawbox` (internal)

```kotlin
internal fun getPathWrappersForDrawbox(subscription: DrawBoxSubscription): StateFlow<List<PathWrapper>>
```

Returns paths **scaled to pixel space** (`paths.scale(connectedSize.toFloat())`).
If disconnected, scale factor = 1 (no-op).

### `SP_CTL_13 — getOpenImageForDrawbox` (internal)

```kotlin
internal fun getOpenImageForDrawbox(size: Int?): StateFlow<OpenedImage>
```

Returns `OpenedImage.Image` with `dstSize = size ?: connectedSize` when image is loaded,
otherwise `OpenedImage.None`.

---

## 03 — State Transitions

```
Disconnected ──[connectToDrawBox(square)]──► Connected(size)
```

Path operation state (only valid when Connected):
```
idle  ──[insertNewPath]──► drawing ──[updateLatestPath*]──► drawing ──[finalizePath]──► idle
  │                                                                                       │
  └──────────────────────────────[onTap]─────────────────────────────────────────────────┘
```

---

## 04 — Constraints

- `require()` guards in `insertNewPath`, `updateLatestPath`, `finalizePath` enforce call order.
  These throw `IllegalArgumentException` on misuse — not expected in normal operation.
- `connectToDrawBox` silently ignores non-square sizes (ISSUE-005).
- `Connected.alpha` field is unused (ISSUE-001).
- Applies rules: [[architecture]] (normalization), [[error-handling]], [[naming]]

---

## 05 — Rollback Strategy

`DrawController` is a class with no persistence — state is in-memory only. Rolling back means
reverting source files and restarting the consumer app. No data migration required.

---

## 06 — Changelog

- 2026-05-27 — Initialized from existing codebase via onboard procedure.
