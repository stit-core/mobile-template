public import Foundation

// Ключ строки приходит из shared в рантайме (push.*, action.*), поэтому каталог таргета читается по значению ключа,
// а не литералом; подстановка — только %@: Swift-строка в String(format:) не совместима с %s из Android-ресурсов.
nonisolated public enum AppStrings {
  public static func text(_ key: String, bundle: Bundle, arguments: [String] = []) -> String {
    let template = bundle.localizedString(forKey: key, value: key, table: nil)
    guard !arguments.isEmpty else { return template }
    return String(format: template, arguments: arguments)
  }
}
