# Module Analysis — box

**Path:** `drawbox/src/commonMain/kotlin/io/github/markyav/drawbox/box/`  
**Layer:** 2 (depends on controller, model, util)

## Files

### DrawBox.kt — public entry point

`@Composable fun DrawBox(controller: DrawController, modifier: Modifier)`

- Collects `background` and `canvasOpacity` from controller as Compose state.
- Remembers two StateFlows from controller (DynamicUpdate subscription + image).
- Renders `DrawBoxBackground` (internal) beneath `DrawBoxCanvas` (internal) inside a `Box`.
- Default modifier: `Modifier.fillMaxSize()`.

**Integration:** This is the only public Composable in the library. Consumers create a
`DrawController` and pass it here.

---

### DrawBoxCanvas.kt — internal gesture + render canvas

`@Composable fun DrawBoxCanvas(pathListWrapper, openedImage, alpha, onSizeChanged, onTap, onDragStart, onDrag, onDragEnd, modifier)`

**Gesture wiring:**
- `detectTapGestures(onTap)` — single tap → `controller.onTap`.
- `detectDragGestures(onDragStart, onDrag, onDragEnd, onDragCancel=onDragEnd)` — drag → insert/update/finalize.
- Drag position uses `change.position` (absolute), ignoring `dragAmount` (delta).
- `clipToBounds()` prevents drawing outside canvas bounds.
- `alpha(alpha)` applies canvas-level opacity.
- `onSizeChanged(onSizeChanged)` triggers controller connection on layout.

**Rendering:**
1. Draws opened image if present (center-cropped via `srcOffset`/`srcSize`/`dstSize`).
2. Iterates all `PathWrapper`s and draws each via `createPath(points)` with Stroke style.

---

### DrawBoxBackground.kt (box package) — internal background renderer

`@Composable fun DrawBoxBackground(background: DrawBoxBackground, modifier: Modifier)`

- `NoBackground` → renders nothing.
- `ColourBackground` → `Box` with `.background(color).alpha(alpha)`.
- `ImageBackground` → `Image(bitmap, alpha)` composable.

**Note:** This is a different file from `controller/DrawBoxBackground.kt` (the sealed interface).
Same name, different package — a potential source of confusion.
