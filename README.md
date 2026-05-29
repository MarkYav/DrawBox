# DrawBox

DrawBox is a multiplatform drawing library built entirely on Compose Multiplatform (Android & Desktop).

## Features

- Cross-platform — Android and Desktop from a single codebase
- Multiple drawing tools: Brush, Shapes, Fill, Pixel Eraser, Stroke Eraser, Eyedropper
- Customizable stroke color, width, and per-stroke opacity
- Undo / Redo / Reset
- Export drawing to `ImageBitmap` at any resolution
- Logical drawing resolution — strokes are recorded at a fixed logical size and rendered correctly at any screen size
- No Compose dependency in the controller — works cleanly in any architecture

## Demo

https://user-images.githubusercontent.com/39382424/230722003-e9b91b28-706a-4048-a950-609f0b357151.mp4

## Usage

```kotlin
val controller = remember { DrawController(logicalSize = IntSize(1000, 1000)) }

DrawBox(
    controller = controller,
    modifier = Modifier.fillMaxSize(),
)
```

### Paint settings

```kotlin
controller.color.value = Color.Red
controller.strokeWidth.value = 8f   // logical pixels
controller.opacity.value = 0.8f     // 0.0–1.0

// Switch drawing tool (BrushTool, ShapeTool, FillTool, PixelEraserTool, StrokeEraserTool, EyedropperTool)
controller.activeTool.value = FillTool
```

### Undo / Redo / Reset

```kotlin
val canUndo by controller.canUndo.collectAsState()
val canRedo by controller.canRedo.collectAsState()

IconButton(onClick = controller::undo, enabled = canUndo) { ... }
IconButton(onClick = controller::redo, enabled = canRedo) { ... }
IconButton(onClick = controller::reset, enabled = canUndo || canRedo) { ... }
```

### Export

```kotlin
val bitmap: ImageBitmap = controller.exportBitmap(IntSize(1920, 1080))
```

### Background

Color background — pass via `modifier`:

```kotlin
DrawBox(
    controller = controller,
    modifier = Modifier
        .fillMaxSize()
        .background(Color.White),
)
```

Image background — wrap in a `Box`:

```kotlin
Box {
    Image(
        painter = painterResource(R.drawable.my_bg),
        contentDescription = null,
        modifier = Modifier.matchParentSize(),
        contentScale = ContentScale.Crop,
    )
    DrawBox(
        controller = controller,
        modifier = Modifier.matchParentSize(),
    )
}
```

## Download

Using Gradle Kotlin DSL:

```kotlin
implementation("io.github.markyav.drawbox:drawbox:2.0.0")
```

## Examples

You can find examples of the library in the [sample](sample) package:
- [Android](sample/android/src/main/java/io/github/markyav/drawbox/android/drawing/DrawingScreen.kt)
- [Desktop](sample/desktop/src/jvmMain/kotlin/Main.kt)

## Planned

- Test suite
- Additional shape types

## Author

This project was created by [Mark Yavorskyi](https://www.linkedin.com/in/mark-yavorskyi/)

## History

I love my work.
The idea of creating this open-source project appeared because I needed a multiplatform (Android + desktop) library for drawing.
I found several popular libs for Android but there was **ZERO** for using in KMM/KMP.
I still have some aspects to improve and I will be happy if you share your feedback or propose an idea!

Hope you enjoy it! \
Mark

## License

Licensed under the Apache License, Version 2.0, [click here for the full license](LICENSE.txt).
