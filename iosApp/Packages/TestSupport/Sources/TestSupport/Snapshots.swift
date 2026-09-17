public import SnapshotTesting
public import SwiftUI
import UIKit

// Снапшоты валидны только для симулятора из iosApp/.test-simulator; перезапись — make snapshot-record.
public enum Snapshots {
  public static let device = ViewImageConfig.iPhone13

  public static func assertScreen(
    _ view: some View,
    named name: String,
    fileID: StaticString = #fileID,
    file filePath: StaticString = #filePath,
    testName: String = #function,
    line: UInt = #line,
    column: UInt = #column
  ) {
    for (suffix, style) in [("light", UIUserInterfaceStyle.light), ("dark", .dark)] {
      let host = UIHostingController(rootView: view)
      host.overrideUserInterfaceStyle = style
      assertSnapshot(
        of: host, as: .image(on: device), named: "\(name)-\(suffix)",
        fileID: fileID, file: filePath, testName: testName, line: line, column: column
      )
    }
  }
}
