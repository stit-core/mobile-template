pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

plugins {
    // Toolchain auto-provisioning: на машине без нужного JDK Gradle скачает его
    // в ~/.gradle/jdks — шаблон не зависит от того, что стоит у разработчика.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Typesafe project accessors: projects.core.model вместо project(":core:model")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "android-gate"

include(":app")
include(":core:model")
include(":core:data")
include(":core:designsystem")
include(":core:testing")
include(":feature:todos")
include(":shared")
