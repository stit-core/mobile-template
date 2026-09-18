import Foundation
import Testing

@testable import AppFoundation

@Suite("AppStrings")
struct AppStringsTests {
  @Test("ключа нет в каталоге — виден сам ключ, а не пустая строка")
  func fallsBackToKey() {
    #expect(AppStrings.text("action.add-todo.added", bundle: .main) == "action.add-todo.added")
  }

  @Test("аргумент подставляется в %@")
  func formatsArgument() {
    #expect(AppStrings.text("Добавил: %@", bundle: .main, arguments: ["Купить молоко"]) == "Добавил: Купить молоко")
  }

  @Test("несколько аргументов идут по порядку")
  func formatsSeveral() {
    #expect(AppStrings.text("%@ → %@", bundle: .main, arguments: ["до", "после"]) == "до → после")
  }

  @Test("без аргументов шаблон не форматируется")
  func keepsTemplate() {
    #expect(AppStrings.text("100%", bundle: .main) == "100%")
  }
}
