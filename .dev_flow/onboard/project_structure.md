# Project Structure — DrawBox

**Type:** Kotlin Multiplatform (KMP) Compose library  
**Version:** 2.0.0 (develop branch, v2 upgrade from 1.3.1)  
**Targets:** Android, JVM (Desktop)  
**Published to:** Maven Central (`io.github.markyav.drawbox:drawbox`)

```
DrawBox/
├── buildSrc/                          # Build constants
│   └── src/main/kotlin/Library.kt    # Group, artifact, version, author, license metadata
├── gradle/
│   └── libs.versions.toml            # Version catalog: AGP 9.x, Kotlin 2.3.21, Compose 1.11.0
├── build.gradle.kts                   # Root build — applies plugins
├── settings.gradle.kts                # Module includes: drawbox, sample:android, sample:desktop
├── drawbox/                           # ★ Library module (the publishable artifact)
│   └── src/commonMain/kotlin/io/github/markyav/drawbox/
│       ├── util/
│       │   ├── StateFlowUtil.kt       # DerivedStateFlow, mapState, combineStates
│       │   └── Util.kt               # createPath (bezier), calculateMidpoint, addNotNull
│       ├── model/
│       │   └── PathWrapper.kt        # Normalized stroke data model
│       ├── controller/
│       │   ├── DrawController.kt     # Public API: undo/redo/reset/open/getBitmap, internal draw hooks
│       │   ├── DrawBoxConnectionState.kt  # Disconnected | Connected(size)
│       │   ├── DrawBoxBackground.kt  # NoBackground | ColourBackground | ImageBackground
│       │   ├── DrawBoxSubscription.kt     # DynamicUpdate | FinishDrawingUpdate
│       │   └── OpenedImage.kt        # None | Image (with crop math)
│       └── box/
│           ├── DrawBox.kt            # Public @Composable entry point
│           ├── DrawBoxCanvas.kt      # Internal canvas + gesture handling
│           └── DrawBoxBackground.kt  # Internal background renderer
└── sample/
    ├── android/                       # Android sample app
    └── desktop/                       # Desktop (JVM) sample app
```

**Key observations:**
- No test source sets exist anywhere.
- Workspace is a standard Gradle multi-project (not pnpm/Cargo/Go workspaces).
- `buildSrc` holds only build metadata, not Gradle plugins.
- `sample/` is excluded from library publishing.
