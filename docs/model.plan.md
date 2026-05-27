---
ID: PL_MDL
Title: Stroke Data Model — Implementation Plan
Status: completed
Created: 2026-05-27
Updated: 2026-05-27
Spec: [SP_MDL](model.sp.md)
---

# PL_MDL — Stroke Data Model Plan

## Technology Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Data type | `data class` | Value semantics, auto-generated equals/copy/toString |
| Point storage | `List<Offset>` | Immutable view; controller always replaces wholesale |
| Coordinate space | Normalized [0..1] | Decouples stored data from canvas pixel size |

## Phases

### Phase 1 — Define PathWrapper data class [DONE]
- `points: List<Offset>` (normalized)
- `strokeWidth: Float` (normalized)
- `strokeColor: Color`
- `alpha: Float`

## Backlog

- Remove unused `SnapshotStateList` import.
- Change `var points` to `val points` — points are never mutated in place.
- Consider adding validation (alpha in [0,1], strokeWidth > 0) if moving to a sealed class.

## Changelog

- 2026-05-27 — Initialized from existing codebase via onboard procedure.
