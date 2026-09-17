public import SharedBridge

public final class TodoDetailModel: PresenterModel<TodoDetailState, TodoDetailIntent> {
  public init(presenter: TodoDetailPresenter) {
    super.init(stateFlow: presenter.state, dispatch: presenter.dispatch(intent:), close: presenter.close)
  }

  public func start() { dispatch(TodoDetailIntentStart.shared) }
  public func toggle() { dispatch(TodoDetailIntentToggle.shared) }
  public func delete() { dispatch(TodoDetailIntentDelete.shared) }
}
