package tech.stit.mobile.feature.todos

import kotlinx.serialization.Serializable
import tech.stit.mobile.core.widget.WidgetState
import kotlin.time.Instant

// Срез для виджета «ближайшие задачи»: только показываемое — снимок лежит в общем каталоге, куда смотрит и расширение iOS.
@Serializable
data class TodayTodosWidgetSnapshot(
    val state: WidgetState,
    val items: List<TodayTodosWidgetItem>,
    // Возраст снимка — единственное основание для stale: виджет и расширение не знают, ходил ли кто-то в сеть.
    val generatedAt: Instant,
    // Тап уходит тем же https-адресом, что и deep link: роутер у ссылки и у виджета один.
    val tapUrl: String,
)

@Serializable
data class TodayTodosWidgetItem(
    // Кнопка строки адресует конкретную задачу — без её id действие вне приложения не выполнить.
    val id: String,
    val title: String,
    val updatedAt: Instant,
    val done: Boolean,
)
