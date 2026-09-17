package tech.stit.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.int(alias: String): Int = findVersion(alias)
    .orElseThrow { IllegalStateException("нет версии '$alias' в libs.versions.toml") }
    .requiredVersion
    .toInt()

fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> = findLibrary(alias)
    .orElseThrow { IllegalStateException("нет библиотеки '$alias' в libs.versions.toml") }
