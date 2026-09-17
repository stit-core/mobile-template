public import SharedBridge

public final class RequirementsModel: PresenterModel<RequirementsState, RequirementsIntent> {
  public init(presenter: RequirementsPresenter) {
    super.init(stateFlow: presenter.state, dispatch: presenter.dispatch(intent:), close: presenter.close)
  }

  public func start() { dispatch(RequirementsIntentStart.shared) }
  public func foreground() { dispatch(RequirementsIntentForeground.shared) }
  public func retry() { dispatch(RequirementsIntentRetry.shared) }
  public func dismissRecommendation() { dispatch(RequirementsIntentDismissRecommendation.shared) }
}
