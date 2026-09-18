public import SharedBridge
import WidgetKit

// Снимок пишет shared, перерисовку заказывает WidgetKit: обе половины обновления виджета живут здесь.
// Расширение в этой цепочке не участвует — оно только читает готовый файл из контейнера App Group.
nonisolated public enum WidgetRefresh {
  public static func publish(slug: String, container: SharedContainer) async {
    guard slug == TodayTodosWidgetProvider.companion.SLUG else { return }
    _ = try? await container.todayTodosWidgetProvider().publish()
    WidgetCenter.shared.reloadTimelines(ofKind: slug)
  }

  // on-app-use: пока приложение живо, снимок обновляется вместе с данными, иначе виджет застревает на placeholder.
  public static func publishAll(container: SharedContainer) async {
    await publish(slug: TodayTodosWidgetProvider.companion.SLUG, container: container)
  }
}
