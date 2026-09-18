import AppFoundation
import SwiftUI
import SystemActions
import WidgetKit
import Widgets
import WidgetsFeature

// Точка входа виджетов в ОС. Расширение живёт на снимке из контейнера App Group: ни сети, ни токена, ни Shared внутри —
// поэтому статический Kotlin-фреймворк в его бинарь не линкуется.
@main
struct StitWidgets: WidgetBundle {
  var body: some Widget {
    TodayTodosWidget()
  }
}

struct TodayTodosWidget: Widget {
  var body: some WidgetConfiguration {
    StaticConfiguration(kind: TodayTodosEntry.kind, provider: TodayTodosProvider()) { entry in
      TodayTodosWidgetView(entry: entry, complete: CompleteTodoIntent.init(todoId:))
    }
    .configurationDisplayName(TodayTodosWidgetText.name)
    .description(TodayTodosWidgetText.summary)
    .supportedFamilies([.systemSmall, .systemMedium])
  }
}

// Таймлайн из одной записи: интервалов обновления в коде нет — перерисовку заказывает приложение, когда снимок изменился.
nonisolated struct TodayTodosProvider: TimelineProvider {
  private let store = TodayTodosSnapshotStore()

  func placeholder(in context: Context) -> TodayTodosEntry {
    .placeholder()
  }

  func getSnapshot(in context: Context, completion: @escaping (TodayTodosEntry) -> Void) {
    completion(store.entry())
  }

  func getTimeline(in context: Context, completion: @escaping (Timeline<TodayTodosEntry>) -> Void) {
    let entry = store.entry()
    completion(Timeline(entries: [entry], policy: reload(after: entry)))
  }

  // Свежесть — часть состояния: к порогу устаревания запись перечитывается, иначе stale не покажется никогда.
  // Дальше этого порога перечитывать нечего — снимок обновит приложение или фоновая задача и закажет перерисовку сама.
  private func reload(after entry: TodayTodosEntry) -> TimelineReloadPolicy {
    switch entry.state {
    case .populated, .empty:
      return .after(entry.date.addingTimeInterval(WidgetSnapshotFile.freshFor))
    default:
      return .never
    }
  }
}
