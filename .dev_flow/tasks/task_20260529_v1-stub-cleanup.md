# Task: v1 Stub Cleanup

**Task ID:** task_20260529_v1-stub-cleanup
**Created:** 2026-05-29
**Last updated:** 2026-05-29
**Status:** done
**Contributors:** claude-sonnet-4-6

## Current Work Item

**Document:** box/ and controller/ packages
**Phase:** Implement → Review
**Traceable ID:** PL_BOX_v2 (backlog item)

## Description

*(claude-sonnet-4-6)* Delete the 5 v1 stub files that are dead code in v2. These were intentionally
deferred in PL_BOX_v2 until the coordinated cleanup pass. The library module compiles without them;
only the sample apps reference them (will be fixed in Part 2 — sample app update).

Files to delete:
- `box/DrawBoxBackground.kt` — v1 background composable, not called by v2 DrawBox
- `controller/DrawBoxBackground.kt` — v1 background sealed interface
- `controller/DrawBoxConnectionState.kt` — v1 connection state (square-only, ISSUE-001)
- `controller/DrawBoxSubscription.kt` — v1 subscription enum
- `controller/OpenedImage.kt` — v1 image import sealed interface

## Subtasks

### Subtask: Delete v1 stubs
**Author:** claude-sonnet-4-6
**Status:** in-progress
**Goal:** Delete all 5 v1 stub files; verify library module unaffected

**Progress:**
- [x] Delete box/DrawBoxBackground.kt
- [x] Delete controller/DrawBoxBackground.kt
- [x] Delete controller/DrawBoxConnectionState.kt
- [x] Delete controller/DrawBoxSubscription.kt
- [x] Delete controller/OpenedImage.kt
- [x] Pre-commit review passed (0 blockers, 0 warnings)
- [ ] Gate: user approves commit

**Activity:**
- 2026-05-29: All 5 files deleted; pre-commit review passed; awaiting commit approval
- 2026-05-29: Task created; scope confirmed (5 files); sample app breakage is expected

## Coordination Notes

*(none)*

## Blocking Issues

*(none)*

## Relevant Context

| Item | Note | Added by |
|------|------|----------|
| docs/draw_box_v2.plan.md | Backlog item: "Delete v1 box stubs" | claude-sonnet-4-6 |
| .dev_flow/onboard/issues.md | ISSUE-001: dead `alpha` on DrawBoxConnectionState.Connected | claude-sonnet-4-6 |
| sample/android + sample/desktop | Still import these types; will break until Part 2 | claude-sonnet-4-6 |

## Shared Activity Log

- 2026-05-29 — Task created; proceeding to deletion (claude-sonnet-4-6)
