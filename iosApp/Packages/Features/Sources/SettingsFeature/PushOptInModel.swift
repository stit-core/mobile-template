public import SharedBridge

public final class PushOptInModel: PresenterModel<PushOptInState, PushOptInIntent> {
  public init(presenter: PushOptInPresenter) {
    super.init(stateFlow: presenter.state, dispatch: presenter.dispatch(intent:), close: presenter.close)
  }

  public func start() { dispatch(PushOptInIntentStart.shared) }
  public func accept() { dispatch(PushOptInIntentAccept.shared) }
  public func decline() { dispatch(PushOptInIntentDecline.shared) }
}
