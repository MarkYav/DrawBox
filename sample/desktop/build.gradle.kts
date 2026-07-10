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
        mainClass = "io.github.markyav.drawbox.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = Library.name
            packageVersion = Library.version
        }
    }
}

dependencies {
    implementation(project(":drawbox"))
    implementation(project(":drawbox-ui"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.material.icons.core)
}
