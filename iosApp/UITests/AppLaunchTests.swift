import XCTest

// Smoke XCUITest (план UI, вне Linux-гейта): холодный старт схемы Demo доходит до логина или списка задач.
final class AppLaunchTests: XCTestCase {
  func testColdStartShowsLoginOrTodos() {
    let app = XCUIApplication()
    app.launch()
    let login = app.buttons["login.submit"]
    let todos = app.otherElements["todos.screen"]
    XCTAssertTrue(login.waitForExistence(timeout: 15) || todos.waitForExistence(timeout: 15))
  }
}
