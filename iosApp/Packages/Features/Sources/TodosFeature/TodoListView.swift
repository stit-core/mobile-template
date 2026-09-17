import DesignSystem
import Routing
import SharedBridge
public import SwiftUI

public struct TodoListView: View {
  @Environment(\.theme) private var theme
  private let model: TodoListModel

  public init(model: TodoListModel) {
    self.model = model
  }

  public var body: some View {
    let state = model.state
    VStack(spacing: 0) {
      if state.offline { OfflineBanner() }
      composer(state)
      content(state)
    }
    .navigationTitle(Text("todos.list.title", bundle: .module))
    .task { model.start() }
    .accessibilityIdentifier("todos.screen")
  }

  private func composer(_ state: TodoListState) -> some View {
    HStack(spacing: theme.metrics.spaceSm) {
      TextField(
        String(localized: "todos.list.new_placeholder", bundle: .module),
        text: Binding(get: { state.newTitle }, set: model.titleChanged)
      )
      .textFieldStyle(.roundedBorder)
      .onSubmit(model.create)
      .accessibilityIdentifier("todos.new_title")
      Button(action: model.create) {
        Image(systemName: "plus.circle.fill").font(theme.typography.headingLg)
      }
      .disabled(!state.canCreate || state.offline)
      .accessibilityLabel(Text("todos.list.add", bundle: .module))
      .accessibilityIdentifier("todos.add")
    }
    .padding(theme.metrics.spaceMd)
  }

  @ViewBuilder
  private func content(_ state: TodoListState) -> some View {
    if state.loading, state.todos.isEmpty {
      LoadingStateView()
    } else if let error = state.error, state.todos.isEmpty {
      ErrorStateView(messageKey: error.localizationKey, onRetry: model.refresh)
    } else if state.todos.isEmpty {
      EmptyStateView(Text("todos.list.empty", bundle: .module), systemImage: "checklist")
    } else {
      List {
        if let error = state.error { ErrorText(messageKey: error.localizationKey) }
        ForEach(state.todos, id: \.id) { todo in
          NavigationLink(value: AppRoute.todoDetail(id: todo.id)) {
            TodoRow(todo: todo) { model.toggle(id: todo.id) }
          }
          .swipeActions {
            Button(role: .destructive) {
              model.delete(id: todo.id)
            } label: {
              Text("todos.detail.delete", bundle: .module)
            }
            .disabled(state.offline)
          }
          .accessibilityIdentifier("todos.row.\(todo.id)")
        }
      }
      .listStyle(.plain)
      .refreshable { model.refresh() }
    }
  }
}

struct TodoRow: View {
  @Environment(\.theme) private var theme
  let todo: Todo
  let toggle: () -> Void

  var body: some View {
    HStack(spacing: theme.metrics.spaceSm) {
      Button(action: toggle) {
        Image(systemName: todo.done ? "checkmark.circle.fill" : "circle")
          .foregroundStyle(todo.done ? theme.colors.success : theme.colors.muted)
      }
      .buttonStyle(.plain)
      .accessibilityIdentifier("todos.toggle.\(todo.id)")
      Text(todo.title)
        .font(theme.typography.bodyMd)
        .strikethrough(todo.done)
        .foregroundStyle(todo.done ? theme.colors.muted : theme.colors.appFg)
    }
  }
}
