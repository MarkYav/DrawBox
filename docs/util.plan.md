---
ID: PL_UTL
Title: Utility Layer — Implementation Plan
Status: completed
Created: 2026-05-27
Updated: 2026-05-27
Spec: [SP_UTL](util.sp.md)
---

# PL_UTL — Utility Layer Plan

## Technology Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| StateFlow derivation | Custom `DerivedStateFlow` | No official `StateFlow.map → StateFlow` in coroutines yet |
| Path smoothing | Quadratic Bézier through midpoints | Simple, smooth, O(n) per render |
| API style | Top-level extension functions | Consistent with Kotlin stdlib idioms |

## Phases

### Phase 1 — StateFlow utilities [DONE]
- Implement `DerivedStateFlow<T>` wrapping getter + flow.
- Implement `StateFlow.mapState()` extension.
- Implement `combineStates()` top-level function.

### Phase 2 — Drawing utilities [DONE]
- Implement `createPath(points)` with Bézier interpolation.
- Implement `MutableList.addNotNull()` extension.

## Backlog

- Replace `DerivedStateFlow` with official coroutines implementation once
  `kotlinx.coroutines#2631` is resolved.
- `@InternalCoroutinesApi` usage in `DerivedStateFlow.collect` — track coroutines releases.

## Changelog

- 2026-05-27 — Initialized from existing codebase via onboard procedure.
