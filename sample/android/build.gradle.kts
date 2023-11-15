plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
}

group = Library.group
version = Library.version

android {
    namespace = "io.github.markyav.drawbox.android"
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

dependencies {
    implementation(project(":drawbox"))
    implementation(libs.androidx.activityCompose)
    implementation(compose.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.coreKtx)
}