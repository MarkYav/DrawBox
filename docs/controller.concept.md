---
ID: C_CTL
Title: Draw Controller
Status: deprecated
Deprecated-reason: Replaced by [C_CTL_v2](draw_controller_v2.concept.md)
Created: 2026-05-27
Updated: 2026-05-27
Depends on: [C_UTL, C_MDL]
Used by: [C_BOX]
---

# C_CTL — Draw Controller

## 01 — Purpose

`DrawController` is the single source of truth for all drawing state. It exposes
a reactive API for consumers (color, strokeWidth, undo/redo) and an internal API
for the `DrawBox` Composable (gesture callbacks, path retrieval, bitmap export).

## 02 — Domain Model

### Connection State Machine

The controller starts `Disconnected`. The `DrawBox` Composable reports its rendered size
via `connectToDrawBox(IntSize)`. If the size is square and positive, the controller
transitions to `Connected(size)`. All path operations silently no-op while disconnected.

```
Disconnected ──[connectToDrawBox(square size)]──► Connected(size: Int)
```

### Path Lifecycle

```
insertNewPath ──► updateLatestPath (0..N times) ──► finalizePath
                                                         │
                                                    drawnPaths list
```

On finalize, a `PathWrapper` is constructed with the current stroke attributes and
appended to `drawnPaths`. The redo stack (`canceledPaths`) is cleared on every new stroke start.

### Subscription Model

Two modes of path emission:
- `DynamicUpdate` — emits on every pointer event (combines `drawnPaths` + `activeDrawingPath`).
- `FinishDrawingUpdate` — emits only when `finalizePath` is called (from `drawnPaths` only).

### Bitmap Export

`getBitmap(size, subscription)` renders all paths (and optionally an opened image) to an
off-screen `ImageBitmap` at the requested pixel size. The subscription controls which paths
are included. Useful for capturing the drawing result without the Compose UI.

## 03 — Mechanisms

**Normalization:** All geometry stored in [0..1] space. Internal scaling happens in `scale()`.  
**Undo/Redo:** Stack-based. `drawnPaths` is the draw stack; `canceledPaths` is the redo stack.
Any new stroke clears the redo stack.  
**Image loading:** `open(image)` resets all paths and sets the background image.

## 04 — What This IS and IS NOT

**IS:** The library's state manager and public API surface (except the Composable itself).  
**IS NOT:** A Composable or a UI component. Currently imports Compose runtime (tracked violation — ISSUE-002).
Future versions will remove this dependency.

## 05 — Changelog

- 2026-05-27 — Initialized from existing codebase via onboard procedure.
