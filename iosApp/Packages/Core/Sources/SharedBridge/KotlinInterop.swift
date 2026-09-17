public import Foundation
import Shared

extension KotlinInstant {
  public var date: Date {
    Date(timeIntervalSince1970: Double(toEpochMilliseconds()) / 1000)
  }
}

extension MessageKey {
  // Ключ ресурса совпадает с MessageKey.value: error.<code> ищется в каталоге строк DesignSystem.
  public var localizationKey: String { value }
}
