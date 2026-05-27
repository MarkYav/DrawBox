# Draw Engine  {#C_ENG}

> **Code:** C_ENG
> **Status:** active
> **Created:** 2026-05-27
> **Updated:** 2026-05-27
> **Author:** Mark Yavorskyi + Claude (design session)
> **Owner:** Library author (Mark Yavorskyi)
> **Complexity:** high
>
> **Depends on:** [C_UTL](util.concept.md)
> **Used by:** [C_CTL_v2](draw_controller_v2.concept.md), [C_BOX_v2](draw_box_v2.concept.md)
> **Spike:** [v2_architecture.spike.md](v2_architecture.spike.md)
> **Specification:** [SP_ENG](draw_engine.sp.md)
> **Plan:** [draw_engine.plan.md](draw_engine.plan.md)
>
> The Draw Engine is the core of the drawing library. It separates three concerns that were
> entangled in v1: what kind of thing was drawn (Drawing Action), how user gestures produce
> that thing (Drawing Tool), and how the results are stored and rendered efficiently
> (Canvas Manager). Each concern is an open extension point — new tools and new action types
> can be added by anyone, including library consumers, without modifying library internals.

---

## 1. Philosophy  {#C_ENG_01}

### 1.1. Core Principle  {#C_ENG_01_01}

In v1, the library knows about exactly one drawing operation: a brush stroke. Adding a second
operation (eraser, fill, shape) would require modifying the controller, the rendering loop, and
the data model simultaneously. Every new tool multiplies this coupling.

The Draw Engine exists to make the library **open for extension and closed for modification**.
A new drawing operation should be addable by writing two new things: a tool (gesture strategy)
and an action (rendering recipe). Nothing existing should need to change.

This is not premature generalization — the planned tool set (eraser, fill, shapes, eyedropper,
spray) already demonstrates that tool variety is a first-class requirement, not a future
possibility.

### 1.2. Design Constraints  {#C_ENG_01_02}

1. **No sealed types for extension points.** Drawing Actions and Drawing Tools are open
   interfaces. Built-in implementations (Brush, Eraser, Fill, Shape…) are shipped by the
   library, but consumers may add their own without forking or patching.

2. **Tools are stateless across gestures.** A Drawing Tool holds no mutable state between
   gesture sessions. It receives all context it needs at call time. This makes tools safe
   to share, swap, and test in isolation.

3. **Actions are self-rendering.** A Drawing Action carries its own rendering logic. No
   central switch statement maps action types to renderers. Adding a new action type requires
   no changes to the rendering pipeline.

4. **The Canvas Manager is fully private.** Its three internal canvases (checkpoint, committed,
   active) are never exposed outside the engine. Consumers observe only the final rendered
   output via the Draw Controller.

5. **Rollback:** If C_ENG is deprecated, the old controller-owns-everything model from C_CTL
   can be restored. C_ENG lives in its own package — removal is isolated.

---

## 2. Domain Model  {#C_ENG_02}

### 2.1. Key Entities  {#C_ENG_02_01}

```
                 ┌───────────────┐
                 │  Paint State  │  (lives on Controller, not Engine)
                 │ color, width, │
                 │ opacity       │
                 └──────┬────────┘
                        │ snapshot at gesture start
                        ▼
┌──────────────────────────────────────────────────────┐
│                    Tool Context                       │
│  paint options │ committed canvas view │ logical size │
└──────────────────────┬───────────────────────────────┘
                       │ passed to
                       ▼
              ┌─────────────────┐
              │  Drawing Tool   │  (open interface — gesture strategy)
              │                 │  BrushTool, PixelEraserTool,
              │  onGestureStart │  FillTool, ShapeTool, ...
              │  onGestureDrag  │
              │  onGestureEnd   │
              │  onTap          │
              └────────┬────────┘
                       │ produces / updates
                       ▼
              ┌─────────────────┐
              │  Drawing Action │  (open interface — rendering recipe)
              │                 │  PathAction, FillAction,
              │  render(canvas) │  ShapeAction, ...
              │  getBounds()    │
              └────────┬────────┘
                       │ committed to
                       ▼
              ┌─────────────────────────────────────────┐
              │           Canvas Manager                 │
              │  (private — never exposed outside)       │
              │                                          │
              │  action history: [Action, Action, ...]   │
              │  checkpoint canvas  (N actions ago)      │
              │  committed canvas   (all finalized)      │
              │  active canvas      (in-progress stroke) │
              └─────────────────────────────────────────┘
```

**Drawing Action** — a completed or partially-completed drawing operation. It knows everything
needed to reproduce its visual output on any canvas at any time. Its two roles:
- During a gesture: it is the partial result being built up by the tool.
- After commit: it is a permanent, immutable record in the action history (used for undo and export).

**Drawing Tool** — a stateless gesture strategy. It answers the question "given these gesture
events and this context, what drawing action is being created?" Different tools produce
different action types. A tool has no memory between gestures — all its inputs arrive at call time.

**Tool Context** — a read-only snapshot of the environment, captured at the moment a gesture
begins. It gives the tool access to: the current paint settings, a view of the committed canvas
(for tools that need to read pixel colors, like flood fill or eyedropper), and the canvas logical
dimensions. The snapshot is immutable for the duration of the gesture, even if paint settings
change mid-stroke.

**Paint State** — the user's current drawing configuration: color, stroke width, opacity. This
is owned by the Draw Controller (not the engine), because it persists across tool switches and
is set directly by the consumer. The engine receives a snapshot of it via Tool Context.

**Canvas Manager** — the private rendering subsystem. It owns three rendered canvases and the
action history. It handles: applying committed actions to the committed canvas, maintaining the
checkpoint canvas for fast undo, rendering the active canvas incrementally during a gesture,
and providing the final composed view for display.

### 2.2. Data Flows  {#C_ENG_02_02}

**Gesture lifecycle:**

```
1. Consumer touches screen
      │
      ▼ [coordinate mapping — in Controller]
2. Logical point computed from screen coordinates
      │
      ▼ [Tool Context snapshot — in Controller]
3. ToolContext { paintOptions, committedCanvasView, logicalSize }
      │
      ▼ [Drawing Tool]
4. Tool.onGestureStart(point, context) → new partial Drawing Action
      │
      ▼ [each new drag point]
5. Tool.onGestureDrag(from, to, partialAction, context)
      │  → updated partial Drawing Action (grows with each event)
      │  → new segment painted onto active canvas (incremental)
      │
      ▼ [gesture ends]
6. Tool.onGestureEnd(partialAction, context)
      │  → path simplification applied
      │  → final Drawing Action produced
      │
      ▼ [Canvas Manager.commit(action)]
7. Final Drawing Action rendered onto committed canvas
   Committed canvas → checkpoint canvas if undoDepth reached
   Active canvas cleared
   Action appended to history list
```

**Undo lifecycle:**

```
1. undo() requested
      │
      ▼
2. currentIndex decremented
      │
      ├── currentIndex ≥ checkpointIndex?
      │     YES → reset committed canvas to checkpoint canvas copy
      │            replay history[checkpointIndex..currentIndex]
      │
      └── currentIndex < checkpointIndex?
            NO  → reset committed canvas to blank
                   replay history[0..currentIndex]  (full replay — rare)
```

**Display pipeline:**

```
Canvas Manager composes:
  committed canvas   (stable — does not change during gesture)
+ active canvas      (updates incrementally on every drag event)
= display output     (consumed by DrawBoxCanvas for rendering)
```

---

## 3. Mechanisms  {#C_ENG_03}

### 3.1. Core Algorithms  {#C_ENG_03_01}

**Incremental active canvas rendering:**
During a gesture, only the newest segment (from the previous point to the current point) is
drawn onto the active canvas on each drag event. The active canvas is not cleared and redrawn
from scratch — only new data is added. On commit, the active canvas is cleared entirely.

This bounds the per-event rendering cost to O(1) regardless of stroke length.

**Checkpoint canvas advancement:**
After each commit, if the distance between the current action index and the checkpoint index
reaches `undoDepth`, the next committed action is also applied to the checkpoint canvas and
`checkpointIndex` is incremented by one. This keeps the checkpoint exactly `undoDepth` behind
current at all times, with no bulk copy operations.

**Path simplification on commit:**
Before a Drawing Action is stored in the history, redundant intermediate points are removed
using a tolerance-based simplification (e.g., Douglas-Peucker). This reduces history size and
improves export quality. The active canvas rendering (during the gesture) uses the raw point
sequence for smooth live feedback; only the stored action uses the simplified sequence.

**Action-to-canvas rendering:**
When redrawing from history (undo), actions are replayed by calling each action's own render
function in order. No central renderer knows about action types. The Canvas Manager provides
the canvas; the action knows how to draw itself.

**Stroke-level erase:**
The stroke eraser tool is a special case: instead of producing a new Drawing Action, it removes
existing actions from the history that intersect the erased region, then triggers a history
replay to rebuild the committed canvas. This is more expensive than pixel erasing but preserves
the integrity of the action history for undo and export.

### 3.2. Edge Cases  {#C_ENG_03_02}

**Undo past checkpoint:** Replaying from index 0 is O(n). Acceptable because: (a) it only
occurs when the user undoes more than `undoDepth` steps in sequence — uncommon; (b) the visual
result is correct; (c) no data is lost.

**Tool produces no action (e.g. eyedropper):** A tool may return null from any gesture callback.
The Canvas Manager treats null as "no action to commit" — no history entry is added, active
canvas stays unchanged.

**Zero-length gesture (tap on empty canvas):** A tap with a single point is a degenerate stroke.
Tools handle this explicitly via `onTap` — they may produce a dot-action, update state without
producing an action (eyedropper), or do nothing.

**Redo after new action:** Any new committed action after an undo clears the redo stack
(actions beyond `currentIndex` in history are discarded).

**Very long history:** History is unbounded in size (limited only by device memory). Path
simplification reduces per-action point counts. A future layer system or explicit "flatten"
operation can merge history into a single committed canvas snapshot if needed — but this is
out of scope for C_ENG.

---

## 4. Integration Points  {#C_ENG_04}

### 4.1. Dependencies  {#C_ENG_04_01}

- **[C_UTL](util.concept.md)** — the path geometry utility (`createPath`) is used internally
  by the brush action's render function to convert point lists into smooth bezier curves.
  The StateFlow utilities are not used inside C_ENG (reactivity is handled by C_CTL_v2).

- **No dependency on C_CTL_v2 or C_BOX_v2.** The engine has no knowledge of the controller,
  the composable, or the UI. It is a pure drawing subsystem.

### 4.2. API Surface  {#C_ENG_04_02}

The Draw Engine exposes the following to the rest of the system (via C_CTL_v2):

**Open extension points (library consumers may implement):**
- Drawing Tool interface — implement to add a custom gesture strategy.
- Drawing Action interface — implement to add a custom rendering recipe.

**Engine operations (called by C_CTL_v2 only):**
- Begin gesture with a tool and context → returns partial action
- Extend gesture with a new point → updates active canvas, returns updated partial action
- End gesture → commits final action, clears active canvas
- Tap with a tool and context → commits single-point action
- Undo one step
- Redo one step
- Reset all (clear history, clear all canvases)
- Query `canUndo` / `canRedo` state
- Get composed display output (committed + active, for rendering)
- Export action list at a given logical size (for `getBitmap`)

**Built-in tools (shipped by the library, all implement Drawing Tool):**
- Brush — smooth bezier path, supports per-stroke color/width/opacity
- Pixel Eraser — brush-shaped transparent region (clear blend mode)
- Stroke Eraser — removes intersected committed actions from history
- Flood Fill — replaces connected same-colored region with new color
- Eyedropper — reads color from committed canvas; produces no action; updates Paint State
- Shape (Line / Rectangle / Oval) — geometric shape from start to end point

**Built-in actions (all implement Drawing Action):**
- Path Action — smooth bezier stroke through point list
- Fill Action — set of filled points from flood fill BFS
- Shape Action — geometric shape (type + two bounding points)
- Pixel Erase Action — transparent brush stroke (same structure as Path, different blend mode)

---

## Changelog

| Date | Change |
|------|--------|
| 2026-05-27 | Initial draft — created from v2 architecture spike and design session |
