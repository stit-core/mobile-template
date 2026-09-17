package tech.stit.mobile

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertEmpty
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

// Инварианты shared (08 §2.6, правила kmp-shared и kmp-public-api): слои, чистый commonMain, плоский фасад для Swift.
class ArchitectureTest {
    private val common = Konsist.scopeFromDirectory("shared/src/commonMain")

    @Test
    fun `commonMain не импортирует platform API`() {
        common.files
            .flatMap { it.imports }
            .filter { import -> PLATFORM_PACKAGES.any { import.name.startsWith(it) } }
            .assertEmpty()
    }

    @Test
    fun `core не импортирует entity и feature`() {
        common.files
            .filter { "/core/" in it.path }
            .flatMap { it.imports }
            .filter { it.name.startsWith("$PKG.entity.") || it.name.startsWith("$PKG.feature.") }
            .assertEmpty()
    }

    @Test
    fun `entity не импортирует feature`() {
        common.files
            .filter { "/entity/" in it.path }
            .flatMap { it.imports }
            .filter { it.name.startsWith("$PKG.feature.") }
            .assertEmpty()
    }

    @Test
    fun `feature не импортирует другую feature`() {
        common.files
            .filter { "/feature/" in it.path }
            .assertTrue { file ->
                val own = Regex("/feature/([^/]+)/").find(file.path)?.groupValues?.get(1)
                file.imports.none { it.name.startsWith("$PKG.feature.") && !it.name.startsWith("$PKG.feature.$own.") }
            }
    }

    @Test
    fun `презентеры — final классы без generic'ов, со state и dispatch`() {
        common.classes()
            .withNameEndingWith("Presenter")
            .assertTrue { presenter ->
                !presenter.hasOpenModifier &&
                    !presenter.hasAbstractModifier &&
                    presenter.typeParameters.isEmpty() &&
                    presenter.publicFunctions().none { it.typeParameters.isNotEmpty() } &&
                    presenter.hasPropertyWithName("state") &&
                    presenter.hasFunctionWithName("dispatch")
            }
    }

    @Test
    fun `State — data class, Intent — sealed`() {
        val featureClasses = common.classes().filter { "/feature/" in it.path }
        featureClasses.withNameEndingWith("State").assertTrue { it.hasDataModifier }
        featureClasses.withNameEndingWith("Intent").assertTrue { it.hasSealedModifier }
    }

    @Test
    fun `SharedContainer без generic'ов в публичных сигнатурах`() {
        common.classes()
            .filter { it.name == "SharedContainer" }
            .assertTrue { container ->
                container.typeParameters.isEmpty() &&
                    container.publicFunctions().none { it.typeParameters.isNotEmpty() }
            }
    }

    private fun KoClassDeclaration.publicFunctions() = functions(includeNested = false).filter { it.hasPublicOrDefaultModifier }

    private companion object {
        const val PKG = "tech.stit.mobile"
        val PLATFORM_PACKAGES = listOf("android.", "androidx.", "java.", "javax.", "platform.")
    }
}
