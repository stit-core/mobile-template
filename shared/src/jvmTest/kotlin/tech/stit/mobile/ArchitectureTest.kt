package tech.stit.mobile

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.ext.list.withParentInterfaceNamed
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
    fun `раннеры фоновых задач — final классы Task в feature`() {
        common.classes()
            .withParentInterfaceNamed("BackgroundRunner")
            .filterNot { runner ->
                runner.name.endsWith("Task") &&
                    "/feature/" in runner.path &&
                    !runner.hasOpenModifier &&
                    !runner.hasAbstractModifier &&
                    runner.typeParameters.isEmpty() &&
                    runner.hasFunctionWithName("run")
            }
            .assertEmpty()
    }

    @Test
    fun `системные действия — final классы Action в feature с run и internal-конструктором`() {
        common.classes()
            .withNameEndingWith("Action")
            .filterNot { action ->
                "/feature/" in action.path &&
                    !action.hasOpenModifier &&
                    !action.hasAbstractModifier &&
                    action.typeParameters.isEmpty() &&
                    action.hasFunctionWithName("run") &&
                    action.primaryConstructor?.hasInternalModifier == true
            }
            .assertEmpty()
    }

    @Test
    fun `результат действия — sealed с вариантом SignedOut, сессия наружу не отдаётся`() {
        common.classes()
            .withNameEndingWith("Result")
            .filter { "/feature/" in it.path }
            .assertTrue { result ->
                result.hasSealedModifier && result.hasObjectWithName("SignedOut", includeNested = true)
            }
    }

    @Test
    fun `контракт фона — только sealed, data и enum`() {
        common.classes()
            .filter { "/core/background/" in it.path }
            .filterNot { it.hasSealedModifier || it.hasDataModifier || it.hasEnumModifier }
            .assertEmpty()
    }

    @Test
    fun `словарь состояний виджета — enum в core-widget`() {
        common.classes()
            .filter { "/core/widget/" in it.path }
            .filterNot { it.hasEnumModifier }
            .assertEmpty()
    }

    @Test
    fun `снимок виджета — data class в feature, без generic'ов`() {
        common.classes()
            .withNameEndingWith("WidgetSnapshot")
            .filterNot { snapshot -> snapshot.hasDataModifier && "/feature/" in snapshot.path && snapshot.typeParameters.isEmpty() }
            .assertEmpty()
    }

    @Test
    fun `провайдеры виджетов — final классы в feature со snapshot и publish`() {
        common.classes()
            .withNameEndingWith("WidgetProvider")
            .filterNot { provider ->
                "/feature/" in provider.path &&
                    !provider.hasOpenModifier &&
                    !provider.hasAbstractModifier &&
                    provider.typeParameters.isEmpty() &&
                    provider.hasFunctionWithName("snapshot") &&
                    provider.hasFunctionWithName("publish")
            }
            .assertEmpty()
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
