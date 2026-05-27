# Onboard Issues

## ISSUE-001 — Dead `alpha` field on `DrawBoxConnectionState.Connected`

`Connected(val size: Int, val alpha: Float = 1f)` — `alpha` is never read anywhere in the codebase.
**Impact:** Dead code, misleads future developers.
**Location:** `controller/DrawBoxConnectionState.kt:5`
**Resolution:** Remove `alpha` from `Connected` or document intended use.

---

## ISSUE-002 — Compose runtime imports in controller

`DrawController.kt` imports `androidx.compose.runtime.*` (line 3). The README's **Planned** section
explicitly calls out: "Migrate from Compose dependencies in controller folder."
**Impact:** Controller is coupled to Compose runtime, which makes it harder to test without a Compose host.
**Location:** `controller/DrawController.kt:3`
**Resolution:** Extract Compose-specific logic; track as a backlog item in PL_CTL.

---

## ISSUE-003 — Commented-out dead code in `insertNewPath`

Lines 102–107 in `DrawController.kt` contain a commented-out `PathWrapper` construction block
(the `mutableStateListOf` variant). The live code immediately below creates a list-based replacement.
**Impact:** Confusing; contradicts no-dead-code style.
**Location:** `controller/DrawController.kt:102-107`
**Resolution:** Delete commented-out block.

---

## ISSUE-004 — No test suite

The project has no test source sets and no test runner configuration.
**Impact:** All dev-flow Test phases skip; only manual verification is available.
**Resolution:** Create `commonTest` source set and add at least smoke tests for `DrawController`.

---

## ISSUE-005 — Square-only canvas (known limitation)

`connectToDrawBox` rejects non-square sizes (`width == height` guard, line 136).
`DrawBoxConnectionState.Connected(size: Int)` stores one dimension only.
**Impact:** Library cannot render rectangular canvases — documented limitation.
**Resolution:** Known design constraint; document clearly in spec and consider as future feature.
