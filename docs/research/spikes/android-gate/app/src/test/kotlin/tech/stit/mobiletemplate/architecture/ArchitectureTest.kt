package tech.stit.mobiletemplate.architecture

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withAllAnnotationsOf
import com.lemonappdev.konsist.api.ext.list.withParentClassOf
import com.lemonappdev.konsist.api.verify.assertEmpty
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * Архитектурные инварианты, которые не ловит ни компилятор, ни Gradle-граф:
 * проверяются как обычные JVM-тесты, потому падают в `test`-таске гейта.
 */
class ArchitectureTest {
    @Test
    fun `feature-модуль не импортирует другой feature-модуль`() {
        Konsist
            .scopeFromProject()
            .files
            .filter { it.path.contains("/feature/") }
            .assertTrue { file ->
                val ownFeature = Regex("/feature/([^/]+)/").find(file.path)?.groupValues?.get(1)
                file.imports.none { import ->
                    import.name.startsWith("tech.stit.mobiletemplate.feature.") &&
                        ownFeature != null &&
                        !import.name.startsWith("tech.stit.mobiletemplate.feature.$ownFeature")
                }
            }
    }

    @Test
    fun `core model не знает про Android`() {
        Konsist
            .scopeFromProject()
            .files
            .filter { it.path.contains("/core/model/") }
            .flatMap { it.imports }
            .filter { it.name.startsWith("android.") || it.name.startsWith("androidx.") }
            .assertEmpty()
    }

    @Test
    fun `классы-наследники ViewModel называются XxxViewModel`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withParentClassOf(ViewModel::class)
            .assertTrue { it.name.endsWith("ViewModel") }
    }

    @Test
    fun `Composable-функции экранов лежат в файлах XxxScreen`() {
        Konsist
            .scopeFromProject()
            .functions()
            .withAllAnnotationsOf(Composable::class)
            .filter { it.name.endsWith("Screen") }
            .assertTrue { it.containingFile.name.endsWith("Screen") }
    }

    @Test
    fun `в data-слое нет публичных Hilt-модулей`() {
        Konsist
            .scopeFromProject()
            .classes(includeNested = true)
            .filter { it.name.endsWith("Module") && it.path.contains("/core/data/") }
            .assertTrue { it.hasInternalModifier }
    }
}
