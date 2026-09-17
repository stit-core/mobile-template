import Foundation

// Каталог для FileCache из Shared: Application Support не виден в Files и не требует UIFileSharingEnabled.
public enum FilesDirectory {
  public static func applicationSupport(fileManager: FileManager = .default) throws -> String {
    let url = try fileManager.url(
      for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
    return url.path
  }
}
