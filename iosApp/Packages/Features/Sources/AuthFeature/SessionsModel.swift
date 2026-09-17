public import SharedBridge

public final class SessionsModel: PresenterModel<SessionsState, SessionsIntent> {
  public init(presenter: SessionsPresenter) {
    super.init(stateFlow: presenter.state, dispatch: presenter.dispatch(intent:), close: presenter.close)
  }

  public func refresh() { dispatch(SessionsIntentRefresh.shared) }
  public func revoke(jti: String) { dispatch(SessionsIntentRevokeSession(jti: jti)) }
  public func revokeAll() { dispatch(SessionsIntentRevokeAll.shared) }
  public func unregister(deviceId: String) { dispatch(SessionsIntentUnregisterDevice(id: deviceId)) }
}
