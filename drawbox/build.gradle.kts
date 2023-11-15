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
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
        }
    }
}

android {
    namespace = "io.github.markyav.drawbox"
    compileSdk = Android.compileSdk
    defaultConfig {
        minSdk = Android.minSdk
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}