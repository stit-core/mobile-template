import Foundation
import Testing

@testable import SharedBridge

@Suite("AppError")
struct AppErrorTests {
  @Test("ApiError.Http даёт ключ error.<code> и requestId")
  func httpError() {
    let error = ApiErrorHttp(status: 429, code: "RATE_LIMITED", detail: nil, requestId: "req-1", fieldErrors: [:])
    let mapped = AppError(error)
    #expect(mapped.messageKey == MessageKey.companion.forErrorCode(code: "RATE_LIMITED").value)
    #expect(mapped.requestId == "req-1")
  }

  @Test("ApiError.Network — сетевой ключ без requestId")
  func networkError() {
    let mapped = AppError(ApiErrorNetwork(cause: KotlinThrowable(message: "offline")))
    #expect(mapped.messageKey == MessageKey.companion.NetworkError.value)
    #expect(mapped.requestId == nil)
  }

  @Test("любой другой Throwable — error.unknown")
  func unknownThrowable() {
    #expect(AppError(KotlinThrowable(message: "boom")).messageKey == MessageKey.companion.UnknownError.value)
  }

  @Test("NSError без KotlinException — error.unknown")
  func plainNSError() {
    let error = NSError(domain: "test", code: 1)
    #expect(AppError(error).messageKey == MessageKey.companion.UnknownError.value)
  }
}

@Suite("AppRoute ↔ Route")
struct RouteBridgeTests {
  @Test("объекты и деталь маппятся один к одному")
  func mapsRoutes() {
    #expect(AppRoute(RouteHome.shared) == .home)
    #expect(AppRoute(RouteLogin.shared) == .login)
    #expect(AppRoute(RouteProfile.shared) == .profile)
    #expect(AppRoute(RouteSettings.shared) == .settings)
    #expect(AppRoute(RouteTodoList.shared) == .todoList)
    #expect(AppRoute(RouteTodoDetail(id: "42")) == .todoDetail(id: "42"))
  }
}
