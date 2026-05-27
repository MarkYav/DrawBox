---
ID: PL_CTL
Title: Draw Controller — Implementation Plan
Status: completed
Created: 2026-05-27
Updated: 2026-05-27
Spec: [SP_CTL](controller.sp.md)
---

# PL_CTL — Draw Controller Plan

## Technology Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| State container | `MutableStateFlow` | Reactive, multiplatform, no lifecycle dependency |
| Undo/Redo | Two stacks (`drawnPaths`, `canceledPaths`) | Simple, O(1) undo/redo |
| Normalization | Divide on input, multiply on output | Allows multi-size rendering from same data |
| Bitmap export | Off-screen `Canvas(ImageBitmap)` | Compose-native, no platform-specific APIs |
| Reactive derivation | `mapState` / `combineStates` from SP_UTL | Keeps `.value` synchronous |

## Phases

### Phase 1 — Sealed types [DONE]
- `DrawBoxConnectionState` (Disconnected, Connected)
- `DrawBoxBackground` (NoBackground, ColourBackground, ImageBackground)
- `DrawBoxSubscription` (DynamicUpdate, FinishDrawingUpdate)
- `OpenedImage` (None, Image with center-crop math)

### Phase 2 — DrawController core state [DONE]
- Private: `state`, `drawnPaths`, `activeDrawingPath`, `canceledPaths`
- Public: `openedImage`, `canvasOpacity`, `opacity`, `strokeWidth`, `color`, `background`
- Derived: `undoCount`, `redoCount`

### Phase 3 — Public API [DONE]
- `undo()`, `redo()`, `reset()`, `open(image)`
- `getDrawPath(subscription)`, `getBitmap(size, subscription)`

### Phase 4 — Internal API for DrawBox [DONE]
- `connectToDrawBox(size)`, `insertNewPath(point)`, `updateLatestPath(point)`, `finalizePath()`, `onTap(point)`
- `getPathWrappersForDrawbox(subscription)`, `getOpenImageForDrawbox(size)`

## Backlog

- ISSUE-001: Remove dead `alpha` field from `DrawBoxConnectionState.Connected`.
- ISSUE-002: Remove `androidx.compose.runtime.*` import — migrate snapshot usage.
- ISSUE-003: Remove commented-out code block in `insertNewPath` (lines 102–107).
- Future: Erase tool (requires new path operation type or eraser color strategy).
- Future: Import/export (serialize/deserialize `List<PathWrapper>`).
- Future: Rectangular canvas support (replace `Connected(size: Int)` with `Connected(width, height)`).
- Future: Optimize rendering — convert drawn paths to bitmaps after N strokes.

## Changelog

- 2026-05-27 — Initialized from existing codebase via onboard procedure.
