# DrawBox
DrawBox is a multipurpose tool to draw anything on canvas, written completely on Compose Multiplatform.
This is the first multiplatform drawing library!

## Features

- Cross-platform!
- Customisable stoke size, color and opacity
- Inbuilt Undo and Redo options
- Reset option
- Background with color/image
- Custom opacity of drawing/background
- Different subscriptions (dynamic update/after each drawing)
- Easy Implementations

**Next releases:**
- Erase tool
- Import/export


**Planned:**
- Different image rations
- Filling tool
- Optimizing rendering (convert drawn PATHes)
- Migrate from Compose dependencies in [controller folder](drawbox/src/commonMain/kotlin/io/github/markyav/drawbox/controller)

## Demo

https://user-images.githubusercontent.com/39382424/230722003-e9b91b28-706a-4048-a950-609f0b357151.mp4

## Usage

```kotlin
val controller = remember { DrawController() }
DrawBox(drawController = controller, modifier = Modifier.fillMaxSize())
```

## Download

Using Gradle Kotlin DSL:
```kotlin
implementation("io.github.markyav.drawbox:drawbox:1.2.0")
```

## Examples

You can find example of the library basic usage in the [sample](sample) package:
- [Android](sample/android/src/main/java/io/github/markyav/drawbox/android/MainActivity.kt)
- [Desktop](sample/desktop/src/jvmMain/kotlin/Main.kt)

Also, I will share a link here to another independent project where you can find more sophisticated example!

## Author
This project was created by [Mark Yavorskyi](https://www.linkedin.com/in/mark-yavorskyi/)

## History
I love my work.
The idea of creating this open-source project appeared because I needed a multiplatform (Android + desktop) library for drawing.
I fround several popular libs for Android but there was **ZERO** for using in KMM/KMP.
I still have some aspects to improve and I will be happy if you share your feedback or propose an idea!

Hope you enjoy it! \
Mark

## License

Licensed under the Apache License, Version 2.0, [click here for the full license](LICENSE.txt).
