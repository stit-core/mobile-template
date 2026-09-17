package tech.stit.convention

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

// Отчёт гейта считается по варианту devDebug: «total»-вариант Kover тянул бы тесты всех
// восьми вариантов (4 flavor × 2 buildType) на каждый прогон.
internal const val GATE_VARIANT = "gate"

internal fun Project.configureKoverGateVariant() {
    extensions.configure<KoverProjectExtension> {
        currentProject {
            createVariant(GATE_VARIANT) { add("devDebug") }
        }
    }
}
