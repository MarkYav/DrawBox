---
ID: SP_MDL
Title: Stroke Data Model — Specification
Status: active
Created: 2026-05-27
Updated: 2026-05-27
Concept: [C_MDL](model.concept.md)
Depends on specs: []
Used by specs: [SP_CTL, SP_BOX]
---

# SP_MDL — Stroke Data Model Specification

## 01 — Data Structures

### `PathWrapper`

```kotlin
data class PathWrapper(
    var points: List<Offset>,
    val strokeWidth: Float = 5f,
    val strokeColor: Color,
    val alpha: Float = 1f
)
```

| Field | Type | Default | Constraints |
|-------|------|---------|-------------|
| `points` | `List<Offset>` | — | Normalized [0..1] space. Empty list = invisible stroke. |
| `strokeWidth` | `Float` | `5f` | Normalized [0..1] space. Must be > 0 when rendered. |
| `strokeColor` | `Color` | — | Any valid Compose `Color`. |
| `alpha` | `Float` | `1f` | Range [0.0, 1.0]. 0 = transparent, 1 = opaque. |

**Key invariant:** `points` and `strokeWidth` are in **normalized [0..1] space**.
They must be multiplied by canvas size before rendering. Violation of this invariant produces
incorrectly scaled strokes.

---

## 02 — Contracts

### `SP_MDL_01 — PathWrapper construction`

| | |
|-|-|
| Input | `points` (normalized), `strokeWidth` (normalized), `strokeColor`, `alpha` |
| Output | Immutable `PathWrapper` value |
| Error cases | None — data class, no validation |
| Invariant | `data class` equality and copy semantics apply |

### `SP_MDL_02 — points mutability note`

`points` is declared `var` (not `val`). This is a known anomaly — in practice the list
is always replaced wholesale, never mutated. New code should treat `points` as effectively
immutable and always create a new `PathWrapper` via `.copy()`.

---

## 03 — Constraints

- `PathWrapper` has no methods — it is a pure value type.
- Unused import `SnapshotStateList` should be removed (ISSUE-003 adjacent).
- Applies rules: [[naming]], [[architecture]] (normalization invariant)

---

## 04 — Rollback Strategy

`PathWrapper` is a data class with default values — adding new fields with defaults is non-breaking.
Removing or renaming fields is a breaking change requiring a new spec version.

---

## 05 — Changelog

- 2026-05-27 — Initialized from existing codebase via onboard procedure.
