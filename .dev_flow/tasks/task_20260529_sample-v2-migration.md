# Task: Sample App v2 Migration

**Task ID:** task_20260529_sample-v2-migration
**Created:** 2026-05-29
**Last updated:** 2026-05-29
**Status:** in-progress
**Contributors:** claude-sonnet-4-6

## Current Work Item

**Document:** sample/android + sample/desktop
**Phase:** Implement → Review
**Traceable ID:** PL_BOX_v2 (backlog item)

## Description

*(claude-sonnet-4-6)* Migrate sample apps from v1 to v2 API. Key changes:
- `DrawController()` → `DrawController(logicalSize = IntSize(1000, 1000))`
- Remove `getBitmap`/`DrawBoxSubscription` (no v2 equivalent; preview panels dropped)
- Replace `undoCount`/`redoCount` with `canUndo`/`canRedo`
- Replace `controller.background` with `Modifier.background(...)` on DrawBox
- Replace `controller.canvasOpacity` with `controller.opacity` (per-stroke)
- Remove camera image import (`controller.open` is gone)
- Simplify `DrawingScreen` to remove `bitmapCallback` (second column was already commented out)

## Subtasks

### Subtask: Migrate sample apps
**Author:** claude-sonnet-4-6
**Status:** in-progress
**Goal:** Rewrite 4 sample files to compile and run correctly against v2 API

**Progress:**
- [x] sample/desktop/Main.kt
- [x] sample/android/drawing/DrawingScreen.kt
- [x] sample/android/drawing/ExpandedDrawingScreen.kt
- [x] sample/android/app/MainScreen.kt
- [x] Pre-commit review passed (0 blockers, 2 non-issue warnings)
- [ ] Gate: user approves commit

**Activity:**
- 2026-05-29: All 4 files rewritten; review passed; awaiting commit approval
- 2026-05-29: Task created; API mapping analyzed; proceeding to rewrite

## Coordination Notes

*(none)*

## Blocking Issues

*(none)*

## Relevant Context

| Item | Note | Added by |
|------|------|----------|
| docs/draw_controller_v2.sp.md | V2 public API reference | claude-sonnet-4-6 |
| docs/draw_box_v2.sp.md | V2 DrawBox signature | claude-sonnet-4-6 |

## Shared Activity Log

- 2026-05-29 — Task created (claude-sonnet-4-6)
