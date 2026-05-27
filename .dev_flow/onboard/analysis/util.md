# Module Analysis — util

**Path:** `drawbox/src/commonMain/kotlin/io/github/markyav/drawbox/util/`  
**Layer:** 0 (leaf — no internal project imports)

## Files

### StateFlowUtil.kt

**Purpose:** Provides `StateFlow` derivation helpers missing from the official coroutines API.

**Entities:**
- `DerivedStateFlow<T>` — class implementing `StateFlow<T>` via a getter lambda + underlying flow.
  Fields: `getValue: () -> T`, `flow: Flow<T>`.
  Workaround for `kotlinx.coroutines#2631` until official implementation ships.
- `mapState` — extension on `StateFlow<T1>` → `StateFlow<R>` via transform lambda.
- `combineStates` — top-level function combining two `StateFlow`s into one via transform.

**Contracts:**
- `mapState(transform)` — synchronously derives current value; propagates new emissions.
- `combineStates(flow, flow2, transform)` — synchronously derives from both current values;
  propagates combined emissions from either source.
- `DerivedStateFlow.collect` — uses `@InternalCoroutinesApi`; collector sees `distinctUntilChanged` values.

**Invariants:**
- `DerivedStateFlow.value` is computed fresh on every access (no caching).
- `replayCache` always returns a single-element list containing the current value.

**Error handling:** None — delegates to underlying coroutines machinery.

---

### Util.kt

**Purpose:** Compose drawing utilities — bezier path construction and list helpers.

**Entities / Contracts:**
- `createPath(points: List<Offset>): Path`
  - Returns empty `Path` if `points.size <= 1`.
  - Uses quadratic Bézier interpolation through midpoints for smooth curves.
  - First segment is a lineTo(midpoint); subsequent segments are quadraticBezierTo.
  - Final segment lineTo last point.
- `calculateMidpoint(start: Offset, end: Offset): Offset` — private; arithmetic midpoint.
- `MutableList<E>.addNotNull(element: E?): Boolean` — adds only if non-null; returns false if null.

**Invariants:**
- `createPath` always returns a non-null `Path` object (may be empty).

**Error handling:** None — pure functions.
