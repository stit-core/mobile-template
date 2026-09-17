import SwiftUI

@main
struct StitApp: App {
  @UIApplicationDelegateAdaptor(AppDelegate.self) private var delegate

  var body: some Scene {
    WindowGroup {
      RootView()
        .environment(delegate.environment)
        .onOpenURL { url in delegate.environment.shell.open(url: url) }
    }
  }
}
