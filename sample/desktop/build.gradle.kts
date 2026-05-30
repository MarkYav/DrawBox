import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

group = Library.group
version = Library.version

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = Library.name
            packageVersion = Library.version
        }
    }
}

dependencies {
    implementation(project(":drawbox"))
    implementation(compose.desktop.currentOs)
    implementation(libs.material.icons.extended)
}
