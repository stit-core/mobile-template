import AppFoundation
public import AppIntents
import DesignSystem
public import SwiftUI
import WidgetKit
public import Widgets

// Ближайшие задачи на главном экране: срез приходит записью из App Group, репозиториев и сети виджет не знает.
// Кнопка строки — интент системного действия, поэтому тип интента отдаёт композиционный корень расширения.
public struct TodayTodosWidgetView<CompleteIntent: AppIntent>: View {
  @Environment(\.theme) private var theme
  private let entry: TodayTodosEntry
  private let complete: (String) -> CompleteIntent

  public init(entry: TodayTodosEntry, complete: @escaping (String) -> CompleteIntent) {
    self.entry = entry
    self.complete = complete
  }

  public var body: some View {
    VStack(alignment: .leading, spacing: theme.metrics.spaceXs) {
      Text("widget.today-todos.title", bundle: .module)
        .font(theme.typography.bodyMd.weight(.medium))
        .foregroundStyle(theme.colors.appFg)
      content
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    .containerBackground(theme.colors.appBg, for: .widget)
    .widgetURL(entry.tapUrl)
    .accessibilityIdentifier("widget.today-todos")
  }

  @ViewBuilder
  private var content: some View {
    switch entry.state {
    case .placeholder:
      hint("widget.today-todos.placeholder")
    case .empty:
      hint("widget.today-todos.empty")
    case .signedOut:
      hint("widget.today-todos.signed_out")
    case .populated:
      rows(hint: nil)
    case .stale:
      rows(hint: "widget.today-todos.stale")
    }
  }

  private func hint(_ key: LocalizedStringKey) -> some View {
    Text(key, bundle: .module)
      .font(theme.typography.bodyMd)
      .foregroundStyle(theme.colors.muted)
  }

  @ViewBuilder
  private func rows(hint key: LocalizedStringKey?) -> some View {
    // Предупреждение о свежести — подпись над задачами, а не вместо них: данные всё ещё видны.
    if let key {
      Text(key, bundle: .module)
        .font(theme.typography.bodySm)
        .foregroundStyle(theme.colors.muted)
    }
    ForEach(entry.items) { item in
      row(item)
    }
  }

  private func row(_ item: TodayTodosItem) -> some View {
    HStack(spacing: theme.metrics.spaceSm) {
      VStack(alignment: .leading, spacing: 0) {
        Text(item.title)
          .font(theme.typography.bodySm)
          .strikethrough(item.done)
          .foregroundStyle(item.done ? theme.colors.muted : theme.colors.appFg)
          .lineLimit(1)
        Text(
          AppStrings.text("widget.today-todos.updated", bundle: .module, arguments: [AppFormat.date(item.updatedAt)])
        )
        .font(theme.typography.bodySm)
        .foregroundStyle(theme.colors.muted)
        .lineLimit(1)
      }
      Spacer(minLength: 0)
      trailing(item)
    }
    .padding(.vertical, theme.metrics.spaceXs)
    .accessibilityIdentifier("widget.today-todos.row.\(item.id)")
  }

  @ViewBuilder
  private func trailing(_ item: TodayTodosItem) -> some View {
    if item.done {
      Text("widget.today-todos.status_done", bundle: .module)
        .font(theme.typography.bodySm)
        .foregroundStyle(theme.colors.success)
    } else {
      // Отметка прямо на виджете: приложение не открывается, работу делает интент системного действия.
      Button(intent: complete(item.id)) {
        Text("widget.today-todos.complete", bundle: .module)
          .font(theme.typography.bodySm.weight(.medium))
          .foregroundStyle(theme.colors.appFg)
      }
      .buttonStyle(.plain)
      .accessibilityIdentifier("widget.today-todos.complete.\(item.id)")
    }
  }
}

// Имя и описание виджета в галерее системы: строки живут в каталоге фичи, расширение их только показывает.
public enum TodayTodosWidgetText {
  public static var name: Text { Text("widget.today-todos.name", bundle: .module) }
  public static var summary: Text { Text("widget.today-todos.description", bundle: .module) }
}
