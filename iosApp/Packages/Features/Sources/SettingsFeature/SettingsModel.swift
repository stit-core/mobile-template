public import SharedBridge

public final class SettingsModel: PresenterModel<SettingsState, SettingsIntent> {
  public init(presenter: SettingsPresenter) {
    super.init(stateFlow: presenter.state, dispatch: presenter.dispatch(intent:), close: presenter.close)
  }

  public func start() { dispatch(SettingsIntentStart.shared) }
  public func setTheme(_ theme: ThemeMode) { dispatch(SettingsIntentSetTheme(theme: theme)) }
  public func setNotifications(_ enabled: Bool) { dispatch(SettingsIntentSetNotifications(enabled: enabled)) }
}
