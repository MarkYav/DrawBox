---
ID: C_UTL
Title: Utility Layer
Status: active
Created: 2026-05-27
Updated: 2026-05-27
Depends on: []
Used by: [C_MDL, C_CTL, C_BOX]
---

# C_UTL — Utility Layer

## 01 — Purpose

Provide reusable, stateless helpers that the rest of the library builds on.
Two concerns are covered: reactive state derivation and Compose path geometry.

## 02 — Domain Model

### StateFlow Utilities

The `kotlinx.coroutines` library does not provide a first-class way to derive a `StateFlow`
from another `StateFlow` while keeping both the synchronous `.value` accessor and the reactive
`collect` semantics. `DerivedStateFlow` fills this gap.

Two derivation functions are exposed:
- `mapState` — 1-to-1 transform of a `StateFlow`.
- `combineStates` — 2-to-1 combine of two `StateFlow`s.

Both preserve the invariant that `.value` is always computed synchronously from the source(s).

### Path Geometry

`createPath` converts a list of `Offset` points into a smooth `Path` using quadratic Bézier
interpolation through midpoints. This produces visually smooth strokes from discrete pointer events.

## 03 — Mechanisms

**Bézier interpolation:** Given points P0..Pn, the algorithm:
1. MoveTo P0.
2. For i=1: LineTo midpoint(P0, P1).
3. For i>1: QuadraticBezierTo(P(i-1), midpoint(P(i-1), Pi)).
4. LineTo Pn.

This ensures the curve passes through all midpoints and is tangent at control points.

**DerivedStateFlow:** Wraps a value getter and an underlying Flow. On collect,
it converts the flow to a StateFlow in a child scope using `stateIn`, then collects with
`distinctUntilChanged`. This is a workaround for `kotlinx.coroutines#2631`.

## 04 — What This IS and IS NOT

**IS:** Pure, stateless helper functions and a single adapter class.  
**IS NOT:** A state management system. Does not own or observe any application state.

## 05 — Changelog

- 2026-05-27 — Initialized from existing codebase via onboard procedure.
