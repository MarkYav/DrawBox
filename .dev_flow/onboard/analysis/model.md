# Module Analysis — model

**Path:** `drawbox/src/commonMain/kotlin/io/github/markyav/drawbox/model/`  
**Layer:** 0 (leaf — no internal project imports)

## Files

### PathWrapper.kt

**Purpose:** Normalized stroke data transfer object. Carries all the state of a single drawn stroke.

**Entities:**
- `PathWrapper` — `data class`
  - `points: List<Offset>` — stroke points in **normalized [0..1] space** (NOT pixel coordinates).
  - `strokeWidth: Float = 5f` — normalized stroke width (NOT pixel width).
  - `strokeColor: Color` — stroke color.
  - `alpha: Float = 1f` — stroke opacity [0..1].

**Key invariant:** Points and strokeWidth are stored in normalized [0..1] space relative to canvas size.
They must be multiplied by `canvas.size` before rendering. This normalization enables canvas resizing
without data migration.

**Unused import:** `androidx.compose.runtime.snapshots.SnapshotStateList` is imported but not used
in the current code (legacy from an earlier `mutableStateListOf`-based implementation).

**Error handling:** None — pure data class, no validation.

**Integration:** Read by `DrawController` (for scaling), written by `DrawController.finalizePath()`,
rendered by `DrawBoxCanvas` (after controller scales them back to pixel space).
