import AppFoundation
import DesignSystem
import SharedBridge
public import SwiftUI

public struct TodoDetailView: View {
  @Environment(\.theme) private var theme
  @Environment(\.dismiss) private var dismiss
  private let model: TodoDetailModel

  public init(model: TodoDetailModel) {
    self.model = model
  }

  public var body: some View {
    let state = model.state
    Group {
      if state.loading, state.todo == nil {
        LoadingStateView()
      } else if let error = state.error, state.todo == nil {
        ErrorStateView(messageKey: error.localizationKey, onRetry: model.start)
      } else if let todo = state.todo {
        details(todo, state: state)
      }
    }
    .navigationTitle(Text("todos.detail.title", bundle: .module))
    .task { model.start() }
    .onChange(of: state.deleted) { _, deleted in
      if deleted { dismiss() }
    }
    .accessibilityIdentifier("todo_detail.screen")
  }

  private func details(_ todo: Todo, state: TodoDetailState) -> some View {
    VStack(alignment: .leading, spacing: theme.metrics.spaceMd) {
      if state.offline { OfflineBanner() }
      Text(todo.title).font(theme.typography.headingLg)
      Toggle(isOn: Binding(get: { todo.done }, set: { _ in model.toggle() })) {
        Text("todos.detail.done", bundle: .module)
      }
      .disabled(state.offline)
      .accessibilityIdentifier("todo_detail.done")
      LabeledContent {
        Text(AppFormat.dateTime(todo.createdAt.date))
      } label: {
        Text("todos.detail.created", bundle: .module)
      }
      LabeledContent {
        Text(AppFormat.dateTime(todo.updatedAt.date))
      } label: {
        Text("todos.detail.updated", bundle: .module)
      }
      if let error = state.error { ErrorText(messageKey: error.localizationKey) }
      Spacer()
      Button(role: .destructive, action: model.delete) { Text("todos.detail.delete", bundle: .module) }
        .buttonStyle(PrimaryButtonStyle())
        .disabled(state.offline)
        .accessibilityIdentifier("todo_detail.delete")
    }
    .padding(theme.metrics.spaceLg)
  }
}
