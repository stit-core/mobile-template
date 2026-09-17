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
    // Без нужного JDK Gradle скачает его в ~/.gradle/jdks — сборка не зависит от машины разработчика.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// projects.androidApp.core.ui вместо строк: опечатка в имени модуля — ошибка компиляции, а не рантайма.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "mobile-template"

include(":shared")
include(":androidApp:app")
include(":androidApp:core:designsystem")
include(":androidApp:core:ui")
include(":androidApp:core:platform")
include(":androidApp:core:navigation")
include(":androidApp:core:testing")
include(":androidApp:feature:shell")
include(":androidApp:feature:auth")
include(":androidApp:feature:profile")
include(":androidApp:feature:todos")
include(":androidApp:feature:settings")
include(":androidApp:lint")
