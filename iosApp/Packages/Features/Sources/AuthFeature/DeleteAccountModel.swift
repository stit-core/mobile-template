public import SharedBridge

public final class DeleteAccountModel: PresenterModel<DeleteAccountState, DeleteAccountIntent> {
  public init(presenter: DeleteAccountPresenter) {
    super.init(stateFlow: presenter.state, dispatch: presenter.dispatch(intent:), close: presenter.close)
  }

  public func confirmChanged(_ confirmed: Bool) { dispatch(DeleteAccountIntentConfirmChanged(confirmed: confirmed)) }
  public func delete() { dispatch(DeleteAccountIntentDelete.shared) }
}
