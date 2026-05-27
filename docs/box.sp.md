---
ID: SP_BOX
Title: DrawBox Composable Layer — Specification
Status: active
Created: 2026-05-27
Updated: 2026-05-27
Concept: [C_BOX](box.concept.md)
Depends on specs: [SP_CTL](controller.sp.md), [SP_MDL](model.sp.md), [SP_UTL](util.sp.md)
Used by specs: []
---

# SP_BOX — DrawBox Composable Layer Specification

## 01 — Data Structures

No new data structures. The box layer consumes types from controller and model packages.

---

## 02 — Contracts

### `SP_BOX_01 — DrawBox` (public Composable)

```kotlin
@Composable
fun DrawBox(controller: DrawController, modifier: Modifier = Modifier.fillMaxSize())
```

| | |
|-|-|
| Input | `DrawController` instance, optional `Modifier` |
| Output | Renders a `Box` containing `DrawBoxBackground` + `DrawBoxCanvas` stacked |
| Subscription used | `DynamicUpdate` (real-time stroke preview) |
| Background | Collected from `controller.background` |
| Canvas opacity | Collected from `controller.canvasOpacity` |
| Side effect | `DrawBoxCanvas` calls `controller.connectToDrawBox` on first layout |

### `SP_BOX_02 — DrawBoxCanvas` (internal Composable)

```kotlin
@Composable
internal fun DrawBoxCanvas(
    pathListWrapper: StateFlow<List<PathWrapper>>,
    openedImage: StateFlow<OpenedImage>,
    alpha: Float,
    onSizeChanged: (IntSize) -> Unit,
    onTap: (Offset) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier,
)
```

| Gesture | Callback |
|---------|----------|
| Single tap | `onTap(absolutePosition)` |
| Drag start | `onDragStart(absolutePosition)` |
| Drag move | `onDrag(change.position)` — uses absolute position, not delta |
| Drag end | `onDragEnd()` |
| Drag cancel | `onDragEnd()` (same callback as end) |

Rendering order inside `Canvas { }`:
1. Opened image (if `OpenedImage.Image`) — drawn with src/dst rect crop.
2. All `PathWrapper`s — drawn as `Stroke` with `StrokeCap.Round`, `StrokeJoin.Round`.

Modifiers applied: `onSizeChanged`, `pointerInput(detectTapGestures)`,
`pointerInput(detectDragGestures)`, `clipToBounds`, `alpha(alpha)`.

### `SP_BOX_03 — DrawBoxBackground` (internal Composable — box package)

```kotlin
@Composable
internal fun DrawBoxBackground(background: DrawBoxBackground, modifier: Modifier)
```

| Variant | Renders |
|---------|---------|
| `NoBackground` | Nothing (empty branch) |
| `ColourBackground(color, alpha)` | `Box` with `.background(color).alpha(alpha)` |
| `ImageBackground(bitmap, alpha)` | `Image(bitmap, alpha, ...)` |

---

## 03 — Rendering Pipeline

```
controller.getPathWrappersForDrawbox(DynamicUpdate)  ──► StateFlow<List<PathWrapper>> (scaled)
                                                              │
                                           DrawBoxCanvas.Canvas { path.forEach { drawPath(...) } }
```

The `DrawBoxCanvas` renders from pixel-space paths (already scaled by controller).
It does NOT perform normalization — that is the controller's responsibility.

---

## 04 — Constraints

- `DrawBoxCanvas` and the box-package `DrawBoxBackground` are `internal` — not part of the public API.
- The `DrawBox` composable is the only public surface in this layer.
- Drag position tracking uses `change.position` (absolute), intentionally ignoring `dragAmount` (delta).
  This matches the gesture model used by `DrawController` (absolute pixel positions).
- Applies rules: [[architecture]] (no logic in UI), [[style]] (Compose conventions)

---

## 05 — Rollback Strategy

All state is in `DrawController` (in-memory). Rolling back the box layer requires only
reverting source files — no persistent state to migrate.

---

## 06 — Changelog

- 2026-05-27 — Initialized from existing codebase via onboard procedure.
