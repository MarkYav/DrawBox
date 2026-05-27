---
ID: SP_UTL
Title: Utility Layer — Specification
Status: active
Created: 2026-05-27
Updated: 2026-05-27
Concept: [C_UTL](util.concept.md)
Depends on specs: []
Used by specs: [SP_MDL, SP_CTL, SP_BOX]
---

# SP_UTL — Utility Layer Specification

## 01 — Data Structures

### `DerivedStateFlow<T> : StateFlow<T>`

| Field | Type | Description |
|-------|------|-------------|
| `getValue` | `() -> T` | Synchronous value getter |
| `flow` | `Flow<T>` | Source flow for reactive updates |

- `value` — always calls `getValue()` fresh; no caching.
- `replayCache` — always `listOf(value)`.
- `collect` — uses `@InternalCoroutinesApi`; wraps flow with `distinctUntilChanged().stateIn(scope)`.

### `Path` (external — `androidx.compose.ui.graphics.Path`)

Used as output of `createPath`. Not owned by this module.

---

## 02 — Contracts

### `SP_UTL_01 — mapState`

```kotlin
fun <T1, R> StateFlow<T1>.mapState(transform: (T1) -> R): StateFlow<R>
```

| | |
|-|-|
| Input | A `StateFlow<T1>` receiver and a pure transform function |
| Output | A `StateFlow<R>` whose `.value` = `transform(receiver.value)` |
| Error cases | None — delegates failures to underlying coroutines machinery |
| Invariant | `.value` is computed synchronously; never stale relative to source |

### `SP_UTL_02 — combineStates`

```kotlin
fun <T1, T2, R> combineStates(flow: StateFlow<T1>, flow2: StateFlow<T2>, transform: (T1, T2) -> R): StateFlow<R>
```

| | |
|-|-|
| Input | Two source `StateFlow`s and a pure binary transform |
| Output | A `StateFlow<R>` derived from both sources |
| Error cases | None |
| Invariant | `.value` is `transform(flow.value, flow2.value)` computed synchronously |

### `SP_UTL_03 — createPath`

```kotlin
fun createPath(points: List<Offset>): Path
```

| | |
|-|-|
| Input | `List<Offset>` — stroke points (any coordinate space) |
| Output | `Path` object ready for `Canvas.drawPath` |
| Edge case: empty / 1 point | Returns an empty `Path` (no draw calls) |
| Edge case: 2 points | MoveTo P0, LineTo midpoint(P0,P1), LineTo P1 |
| N > 2 points | Quadratic Bézier interpolation through midpoints |
| Invariant | Never returns null; always returns a valid (possibly empty) `Path` |

### `SP_UTL_04 — addNotNull`

```kotlin
fun <E> MutableList<E>.addNotNull(element: E?): Boolean
```

| | |
|-|-|
| Input | Nullable element |
| Output | `true` if added, `false` if null |
| Invariant | List is not modified when `element == null` |

---

## 03 — Constraints

- `DerivedStateFlow.collect` uses `@InternalCoroutinesApi` — acceptable as a temporary workaround
  until `kotlinx.coroutines` provides official `StateFlow.map` returning `StateFlow`.
- All functions are pure / stateless from caller's perspective.
- Applies rules: [[naming]], [[style]]

---

## 04 — Rollback Strategy

These are utility functions with no persistent state — rollback means reverting the source file.
No migration required.

---

## 05 — Changelog

- 2026-05-27 — Initialized from existing codebase via onboard procedure.
