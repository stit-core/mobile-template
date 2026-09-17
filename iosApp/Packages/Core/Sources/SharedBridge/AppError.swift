import Foundation
import Shared

// Ошибка Kotlin в Swift-ошибку с ключом локализации: ApiError.Http/Network → messageKey, прочее → error.unknown.
public struct AppError: Error, Equatable, Sendable {
  public let messageKey: String
  public let requestId: String?

  public init(_ throwable: KotlinThrowable) {
    if let apiError = throwable as? ApiError {
      messageKey = apiError.messageKey.value
      requestId = (apiError as? ApiErrorHttp)?.requestId
    } else {
      messageKey = MessageKey.companion.UnknownError.value
      requestId = nil
    }
  }

  // suspend-функции Kotlin бросают NSError, где исходный KotlinThrowable лежит в userInfo["KotlinException"].
  public init(_ error: any Error) {
    if let throwable = (error as NSError).userInfo["KotlinException"] as? KotlinThrowable {
      self.init(throwable)
    } else {
      messageKey = MessageKey.companion.UnknownError.value
      requestId = nil
    }
  }
}
