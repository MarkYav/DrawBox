plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.androidLibrary)
    id("convention-publication")
}

group = Library.group
version = Library.version

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        publishLibraryVariants("release")
    }
    jvm("desktop") {
        jvmToolchain(11)
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
            }
        }
        val androidMain by getting
        val desktopMain by getting
    }
}

android {
    namespace = "io.github.markyav.drawbox"
    compileSdk = Android.compileSdk
    defaultConfig {
        minSdk = Android.minSdk
    }
}