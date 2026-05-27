group = Library.group
version = Library.version

plugins {
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.androidKmpLibrary).apply(false)
    alias(libs.plugins.mavenPublish).apply(false)
    alias(libs.plugins.composeMultiplatform).apply(false)
    alias(libs.plugins.composeCompiler).apply(false)
    alias(libs.plugins.buildConfig).apply(false)
    alias(libs.plugins.kotlinJvm).apply(false)
    alias(libs.plugins.androidApplication).apply(false)
}