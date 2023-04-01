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
- Optimizing rendering (convert drawn PATHes)
- Usage examples

**Planned:**
- Different image rations
- Erase tool
- Filling tool
- Import/export
- Migrate from Compose dependencies in DrawController

## Usage

```kotlin
val controller = remember { DrawController() }
DrawBox(drawController = controller, modifier = Modifier.fillMaxSize())
```

## Download

Using Gradle Kotlin DSL:
```kotlin
implementation("io.github.markyav.drawbox:drawbox:1.0.0")
```

## Examples
I will add example of usage for Android and desktop in the next release.
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