# Architectural Rules

## Layer Boundaries

**must** — Dependency direction is strictly bottom-up:
`util` (0) → `model` (0, deprecated) → `engine` (1) → `controller` (2) → `box` (3).
No package may import from a higher-numbered layer.

- `engine` may import from `util`. It must not import from `controller` or `box`.
- `controller` may import from `engine` and `util`. It must not import from `box`.
- `box` may import from `controller`, `engine` (for tool/action types), and `util`.

**must** — The `box` package is the only package allowed to use Compose UI APIs (Canvas, gestures,
layout, `@Composable` functions). The `controller` package must NOT import Compose runtime
or Compose UI APIs — state is managed via `StateFlow` only (ISSUE-002 resolved in v2).

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

## Coordinate Space (v2)

**must** — All engine data (DrawAction points, ToolContext logical size) uses logical pixel
coordinates set at `DrawController` construction. No [0..1] normalization. Screen-to-logical
mapping is applied in `DrawController.mapToLogical()` before any engine call.

**must** — `DrawEngine` and all `DrawAction`/`DrawTool` implementations operate exclusively
in logical pixel space. They must never receive or store screen-space coordinates.

*Note: The v1 `PathWrapper` normalization invariant and square-canvas constraint are retired
in v2. `PathWrapper` is deprecated and will be removed in PL_BOX_v2.*
