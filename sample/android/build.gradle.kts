plugins {
    id("org.jetbrains.compose")
    id("com.android.application")
    kotlin("android")
}

group = Library.group
version = Library.version

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
    compileSdk = Android.compileSdk
    defaultConfig {
        applicationId = Android.applicationId
        minSdk = Android.minSdk
        targetSdk = Android.targetSdk
        versionCode = Android.versionCode
        versionName = Library.version
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