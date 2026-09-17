package tech.stit.mobile.android.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withAnnotationNamed
import com.lemonappdev.konsist.api.ext.list.withParentClassNamed
import com.lemonappdev.konsist.api.verify.assertEmpty
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

// Инварианты, которые не ловит ни компилятор, ни Gradle-граф (08 §2.6): падают в «android unit tests».
class ArchitectureTest {
    private val androidApp = Konsist.scopeFromDirectory("androidApp")
    private val sharedCommon = Konsist.scopeFromDirectory("shared/src/commonMain")

    @Test
    fun `feature-модуль не импортирует другой feature-модуль`() {
        androidApp.files
            .filter { it.path.contains("/androidApp/feature/") }
            .assertTrue { file ->
                val own = Regex("/androidApp/feature/([^/]+)/").find(file.path)?.groupValues?.get(1)
                file.imports.none { import ->
                    import.name.startsWith(FEATURE_PACKAGE) && !import.name.startsWith("$FEATURE_PACKAGE$own.")
                }
            }
    }

    @Test
    fun `commonMain не знает про Android и JVM`() {
        sharedCommon.files
            .flatMap { it.imports }
            .filter { import -> PLATFORM_PACKAGES.any { import.name.startsWith(it) } }
            .assertEmpty()
    }

    @Test
    fun `наследники ViewModel называются XxxViewModel и живут в feature-модулях`() {
        androidApp.classes()
            .withParentClassNamed("ViewModel")
            .assertTrue { it.name.endsWith("ViewModel") && it.path.contains("/androidApp/feature/") }
    }

    @Test
    fun `Composable-экраны лежат в файлах XxxScreen`() {
        androidApp.functions()
            .withAnnotationNamed("Composable")
            .filter { it.name.endsWith("Screen") }
            .assertTrue { it.containingFile.name.endsWith("Screen") }
    }

    private companion object {
        const val FEATURE_PACKAGE = "tech.stit.mobile.android.feature."
        val PLATFORM_PACKAGES = listOf("android.", "androidx.", "java.", "javax.")
    }
}
