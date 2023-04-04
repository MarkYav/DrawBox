plugins {
    id("org.jetbrains.compose")
    id("com.android.application")
    kotlin("android")
}

group = "io.github.markyav.drawbox"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
}

dependencies {
    implementation(project(":drawbox"))
    implementation("androidx.activity:activity-compose:1.5.0")
    implementation(compose.material)
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.core:core-ktx:1.9.0")
}

android {
    compileSdkVersion(33)
    defaultConfig {
        applicationId = "io.github.markyav.drawbox.android"
        minSdkVersion(24)
        targetSdkVersion(33)
        versionCode = 1
        versionName = "1.0-SNAPSHOT"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}