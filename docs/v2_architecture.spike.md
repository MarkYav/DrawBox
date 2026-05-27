# Spike: DrawBox v2 Architecture

> **Status:** concluded
> **Created:** 2026-05-27
> **Updated:** 2026-05-27
> **Author:** Mark Yavorskyi + Claude (design session)
> **Time-box:** 1 session (multiple `/dev-flow ask` exchanges)
>
> **Target concept:** to be created — C_ENG (Draw Engine), C_CTL_v2 (Controller v2), C_BOX_v2 (Box v2)
> **Question(s):**
> 1. How should the coordinate system work to support resize/rotation without per-point normalization?
> 2. How should the tool/action system be structured to remain open for extension?
> 3. How should rendering be organized for live performance and clean undo?

---

## Context

DrawBox v1 is a single-tool (brush-only) library with a normalized [0..1] coordinate system and
full path replay on every frame. v2 needs to support multiple tools (now and indefinitely more in
the future), a rectangular canvas, fast live rendering, and clean undo — without becoming a
monolith that requires touching 3 files to add each new tool.

The key tensions explored in this spike:
- Extensibility vs. simplicity (open interfaces vs. sealed/enum)
- Live rendering performance vs. undo correctness (bitmaps vs. replay)
- Coordinate space (normalization vs. logical space + scale transform)

---

## Exploration Log

### Entry 1 — 2026-05-27 — Coordinate System

**What was researched:**
Current v1 normalizes every point to [0..1] on input and scales back on output. The fork
(CyrusCastle/DrawBox-Enhanced) abandoned normalization entirely and works in pixel space,
breaking on resize. Several alternatives were compared.

**Alternatives considered:**

| # | Approach | Pros | Cons | Verdict |
|---|----------|------|------|---------|
| 1 | [0..1] normalization (v1) | No reference size needed | Per-point multiply every render; flood-fill impossible in [0..1] | Rejected |
| 2 | Pixel space (fork) | Simple, no transforms | Breaks on resize/rotation | Rejected |
| 3 | Developer-set logical size + canvas.scale() | Human-readable coords; one scale transform at render; flood-fill works naturally; resize = change one float | Developer must set dimensions upfront | **Chosen** |
| 4 | First-connected-size as reference | Developer doesn't think about it | Reference size unstable at startup | Rejected |

**Findings:**
- Approach 3: developer sets `DrawController(logicalSize = IntSize(W, H))`. All gesture
  coordinates are mapped to logical space once on input. Rendering applies
  `canvas.scale(screenW / logicalW, screenH / logicalH)` before any draw calls.
  Stored `DrawAction` data always uses logical coordinates.
- On window resize or screen rotation: only the scale factor changes. Stored actions untouched.
- Flood-fill BFS and eyedropper pixel reads work against a bitmap rendered at logical resolution.
- A sensible default: if developer omits `logicalSize`, use the first connected `IntSize` as
  the reference (document this limitation clearly).

**Key constraint:** The logical size is set once and must not change. If it changes,
all stored actions would be misinterpreted. This is acceptable — treat it as immutable configuration.

---

### Entry 2 — 2026-05-27 — Tool and Action System

**What was researched:**
Fork uses `CanvasTool` enum + `DrawActionFactory` switch statement. This is closed — each new
tool requires editing the enum, the factory, and the action sealed interface. As the tool set
grows, this becomes a maintenance burden.

**Findings — tool/action distinction:**
- **DrawTool** = gesture strategy. Stateless. Receives gesture events + `ToolContext`, returns
  an updated `DrawAction`. The "pencil in your hand."
- **DrawAction** = the completed (or in-progress) drawing operation. Carries its own rendering
  logic (`render(canvas)`). The "mark on the paper."
- These must be **open interfaces**, not sealed/enum, so consumers can add custom tools without
  forking the library.

**DrawTool interface:**
```kotlin
interface DrawTool {
    fun onDragStart(point: Offset, context: ToolContext): DrawAction?
    fun onDrag(from: Offset, to: Offset, current: DrawAction?, context: ToolContext): DrawAction?
    fun onDragEnd(current: DrawAction?, context: ToolContext): DrawAction?
    fun onTap(point: Offset, context: ToolContext): DrawAction?
}
```

**DrawAction interface:**
```kotlin
interface DrawAction {
    fun render(canvas: Canvas)
    fun getBounds(): Rect? = null  // optional hint for optimization
}
```

**ToolContext (one general type for all tools):**
```kotlin
data class ToolContext(
    val paintOptions: PaintOptions,        // snapshot of current color/width/opacity
    val currentBitmap: ImageBitmap,        // for fill/eyedropper: read pixels
    val logicalSize: IntSize               // canvas dimensions in logical space
)
```
`ToolContext` is a snapshot captured at gesture start. It is immutable for the life of the gesture,
even if the user changes paint settings mid-stroke.

**PaintOptions (lives on DrawController, not on tools):**
```kotlin
data class PaintOptions(
    val color: Color,
    val strokeWidth: Float,
    val opacity: Float,
) {
    fun createPaint(): Paint { ... }
}
```
`color`, `strokeWidth`, `opacity` are `MutableStateFlow` on `DrawController`. They persist
across tool switches. At gesture start, they are snapshot-copied into `ToolContext`.

**Built-in tools (library ships these):**
- `BrushTool` → produces `PathAction`
- `PixelEraserTool` → produces `PathAction` with `BlendMode.Clear`
- `StrokeEraserTool` → on tap/drag, removes matching `DrawAction`s from history (controller op)
- `FillTool` → BFS flood fill → produces `FillAction`
- `EyedropperTool` → reads pixel from `currentBitmap`, updates `DrawController.color`, produces no action
- `ShapeTool(type)` → produces `ShapeAction` (Line / Rect / Oval)

**Open question resolved:** `StrokeEraserTool` is different from other tools — it mutates the
action history rather than adding a new action. This requires a callback from the tool back to
`CanvasManager`. Solution: `ToolContext` can optionally include a `removeActions: (DrawAction) -> Unit`
callback, or `StrokeEraserTool` is implemented as a special internal case that controller handles.
**Decision: keep StrokeEraserTool as an internal controller operation triggered via a flag on
the DrawAction — revisit in spec.**

---

### Entry 3 — 2026-05-27 — Rendering and Undo Model

**What was researched:**
v1 replays all `PathWrapper`s on every frame — O(n) per render. The fork maintains one internal
canvas and renders incrementally. The goal is: live preview at 60fps AND clean O(1) undo.

**Three-bitmap model (chosen):**

```
checkpointBitmap  ← all actions[0..checkpointIndex], private in CanvasManager
currentBitmap     ← all actions[0..currentIndex], private in CanvasManager
activeBitmap      ← current in-progress stroke only, private in CanvasManager
```

**Invariants:**
- `checkpointBitmap` = `currentBitmap` from exactly `undoDepth` commits ago
- `currentBitmap` only updates when a stroke is **committed** (`onDragEnd` / `onTap`)
- `activeBitmap` updates on every `onDrag` event — only the new segment is drawn (incremental)
- All three bitmaps are private to `CanvasManager`; the consumer never sees them

**Live preview rendering (in DrawBoxCanvas):**
```
canvas.scale(screenSize / logicalSize)
drawImage(currentBitmap)   // stable — no per-frame recompute
drawImage(activeBitmap)    // in-progress stroke — incrementally updated
```

**On commit (finalizePath):**
1. Apply path simplification (Douglas-Peucker) to collected points → create final `DrawAction`
2. Render final `DrawAction` onto `currentBitmap`
3. Clear `activeBitmap`
4. Append `DrawAction` to `drawnActions` list
5. Advance `checkpointBitmap` if `currentIndex - checkpointIndex >= undoDepth`

**Two-snapshot undo model:**
- Keep exactly TWO rendered bitmaps: `currentBitmap` (now) and `checkpointBitmap` (N ago)
- `undoDepth: Int` is configurable (default: 20)
- `undo()`: decrement `currentIndex`, redraw `currentBitmap` from checkpoint + replay forward (max N actions)
- Undo past checkpoint: full replay from scratch (rare, acceptable cost)
- `checkpointBitmap` is always private — never exposed

**Why not update currentBitmap live?**
If we bake in-progress strokes into `currentBitmap`, undo during a stroke (edge case) or undo
after commit would require rolling back a partially-baked bitmap. The three-bitmap separation
keeps `currentBitmap` at clean committed boundaries.

---

### Entry 4 — 2026-05-27 — Background and Canvas Shape

**Background — open Composable lambda:**
Replace the sealed `DrawBoxBackground` interface with a `@Composable () -> Unit` parameter on
`DrawBox`. The controller no longer owns the background.

```kotlin
@Composable
fun DrawBox(
    controller: DrawController,
    modifier: Modifier = Modifier.fillMaxSize(),
    background: @Composable () -> Unit = {},
)
```

Built-in convenience wrappers (optional utilities, not forced):
```kotlin
fun solidBackground(color: Color, alpha: Float = 1f): @Composable () -> Unit
fun imageBackground(bitmap: ImageBitmap, alpha: Float = 1f): @Composable () -> Unit
```

This is fully backwards-compatible in spirit — the old `ColourBackground` and `ImageBackground`
cases are just one-liners now. `DrawBoxBackground` sealed interface is deprecated.

**Canvas shape — rectangular only:**
Replace `DrawBoxConnectionState.Connected(size: Int)` with `Connected(width: Int, height: Int)`.
The square constraint is removed. `connectToDrawBox` accepts any positive non-zero IntSize.
Non-square canvases are fully supported.

---

## Alternatives Considered (Summary)

| Decision | Rejected alternatives |
|----------|-----------------------|
| Logical size + scale transform | [0..1] normalization, raw pixel space |
| Open `DrawTool` / `DrawAction` interfaces | `CanvasTool` enum, sealed `DrawAction` interface |
| Three bitmaps (checkpoint / current / active) | Replay-all per frame, one baked canvas, update-current live |
| Two snapshots only (checkpoint + current) | Snapshot every N actions |
| `@Composable` background lambda | Sealed `DrawBoxBackground` (keep as controller state) |
| Rectangular canvas (`IntSize`) | Square-only (`Int`), arbitrary shape (too complex) |

---

## Conclusion

**Verdict:** All questions answered. Architecture is decided. Ready for concept phase.

**Key constraints discovered:**
1. Logical size is set once at `DrawController` construction. It is immutable configuration.
2. All three bitmaps (`checkpoint`, `current`, `active`) are private to `CanvasManager`. Never leaked.
3. `ToolContext` is a snapshot — it does not reflect live changes to `DrawController` state mid-gesture.
4. `DrawTool` and `DrawAction` are open interfaces. Built-in tools are shipped in the library but
   consumers can implement their own without touching library internals.
5. `PaintOptions` lives on `DrawController`, not on tools. Tools snapshot it via `ToolContext`.
6. `background` is removed from `DrawController`. It is a parameter on `DrawBox` Composable only.
7. `StrokeEraserTool` needs special treatment — its mechanism needs to be decided in the spec
   (most likely: removes matching actions from `drawnActions` and triggers a full redraw).

**Recommendations for the concept phase:**

Create **three new/updated concepts** (all as v2 or new):

| Concept | Covers | Supersedes |
|---------|--------|------------|
| C_ENG — Draw Engine | DrawTool, DrawAction, ToolContext, PaintOptions, CanvasManager | none (new) |
| C_CTL_v2 — Draw Controller v2 | DrawController public API, coordinate mapping, undo model | C_CTL (deprecated) |
| C_BOX_v2 — DrawBox Composable v2 | DrawBox with background lambda, DrawBoxCanvas rendering | C_BOX (deprecated) |

Existing modules `C_UTL` and `C_MDL` are deprecated as standalone concepts:
- `util/StateFlowUtil` — keep as-is (no changes needed)
- `util/Util.createPath` — still used for bezier path in `PathAction`
- `model/PathWrapper` — replaced by `DrawAction` interface; deprecate

**Artifacts to discard:**
- None (this spike is a discussion document only; no prototype code was written)
- The spike itself can be kept as historical reference or deleted after concepts are approved
