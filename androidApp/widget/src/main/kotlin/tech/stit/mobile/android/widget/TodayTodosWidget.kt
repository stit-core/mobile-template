package tech.stit.mobile.android.widget

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import tech.stit.mobile.android.core.designsystem.generated.Tokens
import tech.stit.mobile.core.widget.WidgetState
import tech.stit.mobile.feature.todos.TodayTodosWidgetItem
import tech.stit.mobile.feature.todos.TodayTodosWidgetSnapshot
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Instant
import kotlin.time.toJavaInstant

// Ближайшие задачи на главном экране: срез приходит снимком из shared, сети и репозиториев виджет не знает.
class TodayTodosWidget(
    private val snapshot: suspend (Context) -> TodayTodosWidgetSnapshot = { it.todayTodosSnapshot() },
) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val current = snapshot(context)
        provideContent { TodayTodosWidgetBody(current) }
    }
}

@Composable
internal fun TodayTodosWidgetBody(snapshot: TodayTodosWidgetSnapshot) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetTokens.appBg)
            .padding(Tokens.Dimensions.spaceMd)
            .clickable(actionStartActivity(openAppIntent(context, snapshot.tapUrl))),
    ) {
        Text(text = context.getString(R.string.widget_today_todos_title), style = WidgetTokens.heading)
        when (snapshot.state) {
            WidgetState.Placeholder -> WidgetHint(R.string.widget_today_todos_placeholder)
            WidgetState.Empty -> WidgetHint(R.string.widget_today_todos_empty)
            WidgetState.SignedOut -> WidgetHint(R.string.widget_today_todos_signed_out)
            WidgetState.Populated -> TodoLines(snapshot.items, hint = null)
            WidgetState.Stale -> TodoLines(snapshot.items, hint = R.string.widget_today_todos_stale)
        }
    }
}

@Composable
private fun WidgetHint(@StringRes text: Int, style: TextStyle = WidgetTokens.hint) {
    Text(
        text = LocalContext.current.getString(text),
        style = style,
        modifier = GlanceModifier.padding(top = Tokens.Dimensions.spaceSm),
    )
}

@Composable
private fun TodoLines(items: List<TodayTodosWidgetItem>, @StringRes hint: Int?) {
    Column(modifier = GlanceModifier.padding(top = Tokens.Dimensions.spaceXs)) {
        // Предупреждение о свежести — подпись над задачами, а не вместо них: данные всё ещё видны.
        hint?.let { WidgetHint(it, WidgetTokens.caption) }
        items.forEach { TodoLine(it) }
    }
}

@Composable
private fun TodoLine(item: TodayTodosWidgetItem) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.padding(vertical = Tokens.Dimensions.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight().padding(end = Tokens.Dimensions.spaceSm)) {
            Text(text = item.title, style = WidgetTokens.title(item.done), maxLines = 1)
            Text(
                text = context.getString(R.string.widget_today_todos_updated, item.updatedAt.toDateText()),
                style = WidgetTokens.caption,
                maxLines = 1,
            )
        }
        if (item.done) {
            Text(text = context.getString(R.string.widget_today_todos_status_done), style = WidgetTokens.status(true), maxLines = 1)
        } else {
            CompleteButton(item.id)
        }
    }
}

// Отметка «выполнено» прямо на виджете: приложение не открывается, задачу закрывает колбэк.
@Composable
private fun CompleteButton(todoId: String) {
    Text(
        text = LocalContext.current.getString(R.string.widget_today_todos_complete),
        style = WidgetTokens.action,
        maxLines = 1,
        modifier = GlanceModifier
            .padding(horizontal = Tokens.Dimensions.spaceSm, vertical = Tokens.Dimensions.spaceXs)
            .clickable(actionRunCallback<TodayTodosWidgetAction>(actionParametersOf(TodayTodosWidgetAction.TODO_ID to todoId))),
    )
}

// Тап уходит https-ссылкой в собственное приложение: маршрут выбирает DeepLinkParser, как для любой ссылки.
private fun openAppIntent(context: Context, url: String): Intent = Intent(Intent.ACTION_VIEW, url.toUri()).setPackage(context.packageName)

private fun Instant.toDateText(): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(toJavaInstant())
