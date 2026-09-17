import org.gradle.api.tasks.PathSensitivity

plugins {
    alias(libs.plugins.stit.kmp.library)
}

kotlin {
    android {
        namespace = "tech.stit.mobile.shared"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.core)
            // MockTransport — production-код: demo-сборка для App Review ходит в него, а не в сеть.
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.okio)
        }
        commonTest {
            // advanceUntilIdle и StandardTestDispatcher — стандарт тестов презентеров, без opt-in каждый файл шумит.
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.okio.fakefilesystem)
            implementation(libs.turbine)
        }
        androidMain.dependencies {
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.androidx.test.ext.junit)
            implementation(libs.junit4)
            implementation(libs.robolectric)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        jvmTest.dependencies {
            implementation(libs.junit4)
            implementation(libs.konsist)
        }
    }
}

// Фикстуры mock/ — один источник для тестов, demo-сборки и скриншотов; в commonMain они попадают
// Kotlin-строками, чтобы общий код не зависел от файловой системы платформ.
val embedFixtures = tasks.register("embedFixtures") {
    val fixturesDir = rootProject.layout.projectDirectory.dir("mock/fixtures")
    val outputDir = layout.buildDirectory.dir("generated/fixtures/commonMain/kotlin")
    inputs.dir(fixturesDir).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(outputDir)
    doLast {
        val root = fixturesDir.asFile
        val entries = root.walkTopDown()
            .filter { it.isFile && it.extension == "json" && it.parentFile.parentFile == root }
            .sortedBy { it.relativeTo(root).path }
            .joinToString("\n") { file ->
                val key = "${file.parentFile.name}/${file.nameWithoutExtension}"
                val json = file.readText().trimEnd().replace("$", "\${'$'}")
                "        \"$key\" to \"\"\"\n$json\n\"\"\","
            }
        val target = outputDir.get().file("tech/stit/mobile/core/mock/Fixtures.kt").asFile
        target.parentFile.mkdirs()
        target.writeText(
            """
            |package tech.stit.mobile.core.mock
            |
            |// Сгенерировано задачей :shared:embedFixtures из mock/fixtures — руками не правится.
            |internal object Fixtures {
            |    val files: Map<String, String> = mapOf(
            |$entries
            |    )
            |}
            |
            """.trimMargin(),
        )
    }
}
kotlin.sourceSets.commonMain { kotlin.srcDir(embedFixtures) }

// Konsist читает исходники всех source set'ов с диска: без этого input jvmTest остаётся UP-TO-DATE после нарушения в iosMain.
tasks.named<Test>("jvmTest") {
    inputs.dir("src").withPathSensitivity(PathSensitivity.RELATIVE)
}
