# Architectural Rules

## Layer Boundaries

**must** — Dependency direction is strictly bottom-up: `util` → `model` → `controller` → `box`.
No module may import from a higher layer. `util` and `model` must not import from `controller` or `box`.

**must** — The `box` package is the only package allowed to use Compose UI APIs (Canvas, gestures,
layout, Composable functions). The `controller` package may currently import Compose runtime
(snapshots) but this is a tracked violation (ISSUE-002) targeted for removal.

## KMP Targets

**must** — All library code lives in `commonMain`. No platform-specific source sets exist in
`drawbox/` — all drawing logic must be expressible in common Kotlin + Compose Multiplatform.

**must** — The `sample/` modules are never included in the published artifact. They exist
only to demonstrate usage. Do not add library logic to sample modules.

## Public vs Internal API

**must** — Only `DrawController` and `DrawBox` are public entry points of the library.
All other types (`DrawBoxCanvas`, `DrawBoxBackground` in box package, internal controller methods)
must be `internal` or accessed only via the public API surface.

**should** — Internal Composables (`DrawBoxCanvas`, box-package `DrawBoxBackground`) must
not be called directly by consumers. They are implementation details of `DrawBox`.

## Normalization Invariant

**must** — All stroke points and stroke widths stored in `PathWrapper` are in normalized [0..1]
space. Points are normalized on input (`div(size)`) and scaled on output (`times(size)`).
Never store pixel-space coordinates in `PathWrapper`.

Example: `DrawController.kt:93` (`newPoint.div(it.size.toFloat())`),
`DrawController.kt:147` (`pw.points.map { it.times(size) }`).

## Square Canvas Constraint

**should** — The canvas is square-only: `DrawBoxConnectionState.Connected` stores a single `Int size`.
`connectToDrawBox` silently ignores non-square sizes. Any feature that requires rectangular canvases
must be treated as a breaking architectural change requiring a new concept.
