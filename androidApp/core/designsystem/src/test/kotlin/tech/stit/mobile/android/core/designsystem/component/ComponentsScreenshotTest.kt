package tech.stit.mobile.android.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import org.junit.Test
import tech.stit.mobile.android.core.designsystem.StitIcons
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.testing.ScreenshotTest

// Каталог компонентов: по эталону на компонент в обеих темах, где цвет несёт смысл.
class ComponentsScreenshotTest : ScreenshotTest() {
    @Test
    fun `кнопки — обычная, вторичная, выключенная, загрузка`() = capture("components_buttons") { Buttons() }

    @Test
    fun `кнопки в тёмной теме`() = capture("components_buttons_dark", darkTheme = true) { Buttons() }

    @Test
    fun `поля ввода — обычное, с ошибкой, пароль`() = capture("components_text_fields") { TextFields() }

    @Test
    fun `элементы списка`() = capture("components_list_items") { ListItems() }

    @Test
    fun `бейджи статусов`() = capture("components_badges") { Badges() }

    @Test
    fun `бейджи статусов в тёмной теме`() = capture("components_badges_dark", darkTheme = true) { Badges() }
}

@Composable
private fun Catalog(content: @Composable () -> Unit) {
    Surface { Column(modifier = Modifier.fillMaxWidth().padding(StitTheme.spacing.md)) { content() } }
}

@Composable
private fun Buttons() = Catalog {
    StitButton(text = "Войти", onClick = {}, modifier = Modifier.fillMaxWidth())
    StitButton(text = "Отмена", onClick = {}, modifier = Modifier.fillMaxWidth(), variant = StitButtonVariant.Secondary)
    StitButton(text = "Недоступно", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
    StitButton(text = "Загрузка", onClick = {}, modifier = Modifier.fillMaxWidth(), loading = true)
}

@Composable
private fun TextFields() = Catalog {
    StitTextField(value = "demo@example.com", onValueChange = {}, label = "Email")
    StitTextField(value = "", onValueChange = {}, label = "Имя", error = "Обязательное поле")
    StitTextField(value = "secret", onValueChange = {}, label = "Пароль", secret = true)
}

@Composable
private fun ListItems() = Catalog {
    StitListItem(headline = "Прочитать docs/reference/architecture.md", supporting = "10 сентября", onClick = {})
    StitListItem(
        headline = "Собрать demo-сборку",
        leading = { Icon(painter = painterResource(StitIcons.List), contentDescription = null) },
        trailing = { StitStatusBadge(text = "Готово", tone = StitTone.Success) },
    )
}

@Composable
private fun Badges() = Catalog {
    Row {
        StitStatusBadge(text = "Успех", tone = StitTone.Success)
        StitStatusBadge(text = "Внимание", tone = StitTone.Warning, modifier = Modifier.padding(start = StitTheme.spacing.sm))
        StitStatusBadge(text = "Ошибка", tone = StitTone.Danger, modifier = Modifier.padding(start = StitTheme.spacing.sm))
        StitStatusBadge(text = "Нет сети", tone = StitTone.Muted, modifier = Modifier.padding(start = StitTheme.spacing.sm))
    }
}
