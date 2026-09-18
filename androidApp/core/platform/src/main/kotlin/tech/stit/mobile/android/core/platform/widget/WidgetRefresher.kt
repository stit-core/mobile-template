package tech.stit.mobile.android.core.platform.widget

// Порт перерисовки поверхностей вне экранов: доставка фонового результата знает slug, а не Glance — реализацию
// даёт модуль :androidApp:widget.
interface WidgetRefresher {
    suspend fun refresh(slug: String)
}
