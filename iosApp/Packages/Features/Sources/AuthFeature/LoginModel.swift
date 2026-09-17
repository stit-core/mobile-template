public import SharedBridge

public final class LoginModel: PresenterModel<LoginState, LoginIntent> {
  public init(presenter: LoginPresenter) {
    super.init(stateFlow: presenter.state, dispatch: presenter.dispatch(intent:), close: presenter.close)
  }

  public func emailChanged(_ value: String) { dispatch(LoginIntentEmailChanged(value: value)) }
  public func passwordChanged(_ value: String) { dispatch(LoginIntentPasswordChanged(value: value)) }
  public func submit() { dispatch(LoginIntentSubmit.shared) }
}
