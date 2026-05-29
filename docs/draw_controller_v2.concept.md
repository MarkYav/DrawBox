# Draw Controller v2  {#C_CTL_v2}

> **Code:** C_CTL_v2
> **Status:** active
> **Created:** 2026-05-27
> **Updated:** 2026-05-27
> **Author:** Mark Yavorskyi + Claude (design session)
> **Owner:** Library author (Mark Yavorskyi)
> **Complexity:** medium
>
> **Depends on:** [C_ENG](draw_engine.concept.md)
> **Used by:** [C_BOX_v2](draw_box_v2.concept.md)
> **Spike:** [v2_architecture.spike.md](v2_architecture.spike.md)
> **Specification:** [SP_CTL_v2](draw_controller_v2.sp.md) ✓
> **Plan:** [draw_controller_v2.plan.md](draw_controller_v2.plan.md)
> **Supersedes:** [C_CTL](controller.concept.md) (deprecated)
>
> Draw Controller v2 is the bridge between three worlds: the **consumer** who configures
> paint settings and selects tools, the **Compose rendering layer** that handles gesture
> events and displays the result, and the **Draw Engine** that owns all drawing algorithms
> and action history. The controller coordinates these three without owning any of their
> concerns. It exposes an observable, reactive API so that UI controls stay bound to the
> current drawing state.

---

## 1. Philosophy  {#C_CTL_v2_01}

### 1.1. Core Principle  {#C_CTL_v2_01_01}

In v1, the Draw Controller was the drawing engine, the state manager, and the reactive
bridge all at once. Adding a tool required modifying the controller directly. The coordinate
normalization it applied ([0..1]) made flood fill and pixel reading structurally impossible.

In v2, the controller does **none** of the drawing. It delegates every drawing concern to
the Draw Engine. Its only jobs are:

1. **Hold consumer-facing state** — paint color, stroke width, opacity, active tool. These
   are individually observable so UI controls (sliders, pickers, tool buttons) can bind to
   them directly.

2. **Route gesture events** — translate pointer events from screen space to logical space
   and forward them to the engine. The controller knows the screen-to-logical ratio; the
   engine knows only logical coordinates.

3. **Report display changes** — after each engine operation that changes the visual result,
   notify the rendering layer to re-render. The controller owns this invalidation signal;
   the rendering layer pulls a fresh composed image when it fires.

4. **Expose undo/redo state** — forward undo and redo calls to the engine; track
   `canUndo`/`canRedo` as observable state so UI controls enable/disable reactively.

This separation means: adding a new drawing tool requires writing one new DrawTool
implementation and one new DrawAction implementation. The controller is untouched.

### 1.2. Design Constraints  {#C_CTL_v2_01_02}

1. **Logical size is immutable.** Set at construction; the engine is initialized with it.
   The logical coordinate space does not change regardless of screen resize or rotation.
   Stored actions always reference logical coordinates.

2. **No [0..1] normalization.** All stored data lives in logical pixels. The rendering layer
   applies a single scale transform to project logical pixels onto the screen. This is a
   breaking change from C_CTL.

3. **Rectangular canvas supported.** The canvas connection accepts any positive non-zero
   `width × height` pair — not just squares. This is a breaking change from C_CTL.

4. **Background is not owned here.** In v1, the controller held background image/color state.
   In v2, background is a parameter of the `DrawBox` composable. The controller has no
   knowledge of it.

5. **The rendering layer is not this module.** DrawBoxCanvas (C_BOX_v2) is responsible for
   applying the scale transform and drawing the image. The controller only produces the
   composed image on request.

6. **Rollback.** C_CTL_v2 lives in its own package, separate from C_CTL. Removing C_CTL_v2
   and reverting C_BOX_v2 → C_BOX restores v1 behavior (assuming C_ENG is also removed).
   C_CTL is kept in a deprecated state while consumers migrate.

---

## 2. Domain Model  {#C_CTL_v2_02}

### 2.1. Key Entities  {#C_CTL_v2_02_01}

```
┌──────────────────────────────────────────────────────┐
│                     Consumer                          │
│  (library user — sets paint, selects tool, undo/redo) │
└──────────┬──────────────────────────┬─────────────────┘
           │ sets paint state          │ calls undo/redo/reset
           │ sets active tool          │
           ▼                           ▼
┌────────────────────────────────────────────────────────┐
│                  Draw Controller v2                     │
│                                                         │
│  Paint State: color, strokeWidth, opacity               │
│  Active Tool: current DrawTool                          │
│  Canvas Size: screen IntSize (reported by composable)   │
│  Logical Size: IntSize (construction-time, immutable)   │
│                                                         │
│  ┌──────────────────┐   ┌─────────────────────────┐    │
│  │ Coordinate       │   │ Invalidation Signal      │    │
│  │ Mapping          │   │ (notify → re-render)     │    │
│  │ screen→logical   │   │                          │    │
│  └────────┬─────────┘   └──────────────────────────┘    │
└───────────┼─────────────────────────┬──────────────────┘
            │ engine calls            │ getDisplayOutput()
            ▼                         │
┌─────────────────────┐               │ called by composable
│   Draw Engine       │               │ on each re-render
│   (C_ENG)           │◄──────────────┘
│                     │
│  canUndo / canRedo  │──► controller re-exports as observable state
└─────────────────────┘
            ▲
            │ gesture events (screen coords → logical)
            │
┌────────────────────────┐
│  Compose Rendering     │
│  Layer (C_BOX_v2)      │
│  - reports canvas size │
│  - delivers pointer    │
│    events              │
│  - re-renders on       │
│    invalidation signal │
└────────────────────────┘
```

**Paint State** — three independently observable properties: color, stroke width, and opacity.
They persist across tool switches. Consumers bind UI controls (color picker, slider) directly
to these observables. At gesture start, the controller snapshots all three into a `PaintOptions`
and passes it to the engine via a `ToolContext`.

**Active Tool** — the currently selected `DrawTool`. Observable so the UI can highlight the
active tool button. Defaults to `BrushTool`. Switching tool mid-gesture is not supported —
the controller ignores tool changes while a gesture is in progress.

**Canvas Connection** — the screen canvas `IntSize` reported by `DrawBoxCanvas` when its
layout size becomes known (or changes). Until this is reported, gesture events are silently
ignored. On resize, only the scale factor changes; stored actions are unaffected.

**Invalidation Signal** — a lightweight counter-based signal owned by the controller.
Incremented after every engine operation that changes the visual output. The rendering layer
observes this signal and recomposes, then calls `controller.getDisplayOutput()` to obtain
the new image.

**canUndo / canRedo** — derived from the engine's history state. Exposed as observable state
so undo/redo buttons in the consumer's UI enable or disable reactively without polling.

### 2.2. Data Flows  {#C_CTL_v2_02_02}

**Consumer paint/tool setup (between gestures):**
```
Consumer sets color / strokeWidth / opacity
    → updates observable paint state
    → no engine call; next gesture will snapshot the new values

Consumer sets activeTool
    → updates observable tool state
    → no engine call; next gesture will use the new tool
```

**Gesture lifecycle:**
```
DrawBoxCanvas reports pointer DOWN at screen point P
    │
    ▼
controller.onGestureStart(screenPoint=P)
    │
    ├── map P to logical: logicalP = P * logicalSize / screenSize
    ├── snapshot paint state → PaintOptions
    ├── call engine.createToolContext(paintOptions) → ToolContext
    ├── call engine.beginGesture(activeTool, logicalP, context)
    └── emit invalidation signal

DrawBoxCanvas reports pointer MOVE from P to Q
    │
    ▼
controller.onGestureMove(from=P, to=Q)
    │
    ├── map both points to logical space
    ├── call engine.extendGesture(logicalFrom, logicalTo)
    └── emit invalidation signal

DrawBoxCanvas reports pointer UP
    │
    ▼
controller.onGestureEnd()
    │
    ├── call engine.endGesture()
    ├── update canUndo / canRedo
    └── emit invalidation signal
```

**Tap (single-point gesture):**
```
DrawBoxCanvas reports TAP at screen point P
    │
    ▼
controller.onTap(screenPoint=P)
    │
    ├── map P to logical
    ├── snapshot paint state → PaintOptions
    ├── call engine.createToolContext(paintOptions) → ToolContext
    ├── call engine.onTap(activeTool, logicalP, context)
    ├── update canUndo / canRedo
    └── emit invalidation signal
```

**Undo/redo:**
```
Consumer calls controller.undo()
    │
    ├── call engine.undo()
    ├── update canUndo / canRedo
    └── emit invalidation signal
```

**Re-render:**
```
DrawBoxCanvas observes invalidation signal → recomposes
    │
    ▼
Calls controller.getDisplayOutput() → ImageBitmap (logical resolution)
    │
    ▼
Applies canvas.scale(screenW / logicalW, screenH / logicalH)
Draws the ImageBitmap at (0,0)
```

---

## 3. Mechanisms  {#C_CTL_v2_03}

### 3.1. Core Algorithms  {#C_CTL_v2_03_01}

**Coordinate mapping:**
Every screen-space point `(sx, sy)` is mapped to logical space as:
```
lx = sx * logicalSize.width  / screenSize.width
ly = sy * logicalSize.height / screenSize.height
```
This mapping is applied by the controller at every gesture callback before forwarding to
the engine. The engine never sees screen coordinates.

**Tool context snapshot:**
At the start of every gesture (and on tap), the controller constructs a `ToolContext`:
- `paintOptions` — snapshot of the current color, stroke width, and opacity values
- `committedBitmap` — provided by the engine (a read-only view of the committed canvas)
- `logicalSize` — the controller's immutable logical size

This snapshot is passed to the engine, which forwards it to the active tool. The snapshot
is immutable for the duration of the gesture: mid-stroke paint or tool changes do not affect
the in-progress stroke.

**Invalidation and re-render loop:**
The controller does not push rendered images to the composable. Instead:
1. After each engine call that changes visual state, the invalidation signal is incremented.
2. The composable observes the signal. When it changes, it recomposes and pulls a fresh
   `getDisplayOutput()` image from the controller.
3. The controller forwards `getDisplayOutput()` directly to the engine without caching.

This pull model avoids allocating a new bitmap on every state change that doesn't actually
trigger a recomposition.

**canUndo / canRedo tracking:**
After every operation that could change history (endGesture, onTap, undo, redo, reset),
the controller reads `engine.canUndo` and `engine.canRedo` and updates the observable state.

### 3.2. Edge Cases  {#C_CTL_v2_03_02}

**Gesture event before canvas size known:** The controller has not yet received a canvas
size report from `DrawBoxCanvas`. All gesture callbacks are silently ignored — no engine
calls are made.

**Gesture event while disconnected during a gesture:** If the canvas is resized mid-gesture
(window resize, screen rotation), the in-progress gesture is aborted (`endGesture()` is
called with whatever partial state exists). The new scale factor takes effect for the next
gesture.

**Tool switch during gesture:** The active tool observable is updated immediately, but the
engine continues the in-progress gesture with the tool it was started with. The controller
stores a reference to the tool at gesture start and uses it through `endGesture`.

**EyedropperTool (no action):** `engine.onTap` returns with no committed action. The
invalidation signal is still emitted (the composable may not strictly need to redraw, but
correctness is preserved). `canUndo`/`canRedo` are refreshed.

**Export during gesture:** `exportBitmap` is forwarded to the engine. The engine's spec
defines that `exportBitmap` only includes committed actions — the active (in-progress)
gesture is not included. This is expected and correct behavior.

---

## 4. Integration Points  {#C_CTL_v2_04}

### 4.1. Dependencies  {#C_CTL_v2_04_01}

- **[C_ENG](draw_engine.concept.md)** — all drawing operations, action history, undo/redo,
  and bitmap management are delegated to the Draw Engine. The controller is a thin
  coordinator on top of it.
- **No dependency on C_BOX_v2.** The controller does not import or reference the composable.
  Communication is through the controller's public API only.
- **No dependency on C_UTL or C_MDL.** These are used internally by C_ENG; C_CTL_v2 does
  not call them directly.

### 4.2. API Surface  {#C_CTL_v2_04_02}

**Exposed to consumers (library users):**
- Mutable observable: color, stroke width, opacity — individually settable and observable
- Observable: active tool — settable and observable
- Observable: `canUndo`, `canRedo`
- Operations: `undo()`, `redo()`, `reset()`
- Read: `getDisplayOutput()` — returns the composed `ImageBitmap` for rendering
- Read: `exportBitmap(size)` — returns a scaled `ImageBitmap` of committed state

**Exposed to the Compose rendering layer (C_BOX_v2 internal):**
- `onCanvasSizeChanged(size: IntSize)` — reports screen canvas size when it changes
- `onGestureStart(screenPoint)`, `onGestureMove(from, to)`, `onGestureEnd()` — gesture routing
- `onTap(screenPoint)` — tap routing
- `invalidationSignal` — observable counter; composable subscribes to drive recomposition

**NOT exposed:**
- Draw Engine internals — `CanvasManager`, bitmaps, action history
- Individual `DrawAction` or `ToolContext` instances
- Internal logical-to-screen coordinate mapping

---

## Changelog

| Date | Change |
|------|--------|
| 2026-05-27 | Initial version — created from v2 architecture spike |
