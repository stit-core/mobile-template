public import SharedBridge

public final class DemoPanelModel: PresenterModel<DemoPanelState, DemoPanelIntent> {
  public init(presenter: DemoPanelPresenter) {
    super.init(stateFlow: presenter.state, dispatch: presenter.dispatch(intent:), close: presenter.close)
  }

  public func start() { dispatch(DemoPanelIntentStart.shared) }
  public func select(_ scenario: MockScenario) { dispatch(DemoPanelIntentSelectScenario(scenario: scenario)) }
  public func setLatency(_ latencyMs: Int64) { dispatch(DemoPanelIntentSetLatency(latencyMs: latencyMs)) }
}
