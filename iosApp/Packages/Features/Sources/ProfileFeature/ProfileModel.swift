public import SharedBridge

public final class ProfileModel: PresenterModel<ProfileState, ProfileIntent> {
  public init(presenter: ProfilePresenter) {
    super.init(stateFlow: presenter.state, dispatch: presenter.dispatch(intent:), close: presenter.close)
  }

  public func start() { dispatch(ProfileIntentStart.shared) }
  public func refresh() { dispatch(ProfileIntentRefresh.shared) }
  public func nameChanged(_ value: String) { dispatch(ProfileIntentNameChanged(value: value)) }
  public func saveName() { dispatch(ProfileIntentSaveName.shared) }
  public func changePassword(old oldPassword: String, new newPassword: String) {
    dispatch(ProfileIntentChangePassword(oldPassword: oldPassword, newPassword: newPassword))
  }
  public func logout() { dispatch(ProfileIntentLogout.shared) }
}
