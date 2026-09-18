public import Foundation
public import SharedBridge

public final class ShellModel: PresenterModel<ShellState, ShellIntent> {
  public init(presenter: ShellPresenter) {
    super.init(stateFlow: presenter.state, dispatch: presenter.dispatch(intent:), close: presenter.close)
  }

  public func start() { dispatch(ShellIntentStart.shared) }
  public func open(url: URL) { dispatch(ShellIntentOpenLink(url: url.absoluteString)) }
  public func routeConsumed() { dispatch(ShellIntentRouteConsumed.shared) }
  public func leaveDemo() { dispatch(ShellIntentLeaveDemo.shared) }
}
