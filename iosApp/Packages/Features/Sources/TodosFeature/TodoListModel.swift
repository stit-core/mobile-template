public import SharedBridge

public final class TodoListModel: PresenterModel<TodoListState, TodoListIntent> {
  public init(presenter: TodoListPresenter) {
    super.init(stateFlow: presenter.state, dispatch: presenter.dispatch(intent:), close: presenter.close)
  }

  public func start() { dispatch(TodoListIntentStart.shared) }
  public func refresh() { dispatch(TodoListIntentRefresh.shared) }
  public func titleChanged(_ value: String) { dispatch(TodoListIntentTitleChanged(value: value)) }
  public func create() { dispatch(TodoListIntentCreate.shared) }
  public func toggle(id: String) { dispatch(TodoListIntentToggle(id: id)) }
  public func delete(id: String) { dispatch(TodoListIntentDelete(id: id)) }
}
