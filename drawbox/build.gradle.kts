import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.buildConfig)
}

group = Library.group
version = Library.version

kotlin {
    android {
        namespace = "io.github.markyav.drawbox"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
        }
    }
}

//Publishing your Kotlin Multiplatform library to Maven Central
//https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-publish-libraries.html
mavenPublishing {
    publishToMavenCentral()
    coordinates(Library.group, Library.artifact, Library.version)

    pom {
        name = Library.name
        description = Library.description
        url = Library.url

        licenses {
            license {
                name = Library.License.name
                url = Library.License.url
            }
        }

        developers {
            developer {
                id = Library.Author.id
                name = Library.Author.name
                email = Library.Author.email
            }
        }

        scm {
            url = Library.url
        }
    }
    if (project.hasProperty("signing.keyId")) signAllPublications()
}

buildConfig {
    // BuildConfig configuration here.
    // https://github.com/gmazzo/gradle-buildconfig-plugin#usage-in-kts
}
