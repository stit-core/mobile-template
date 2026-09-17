import ArchLintCore
import Foundation

// archlint <каталог с пакетами> [--graph]: каждый подкаталог с Package.swift читается через swift package dump-package.
let arguments = CommandLine.arguments.dropFirst()
guard let root = arguments.first(where: { !$0.hasPrefix("--") }) else {
  FileHandle.standardError.write(Data("usage: archlint <packages-dir> [--graph]\n".utf8))
  exit(2)
}
let wantGraph = arguments.contains("--graph")
let fileManager = FileManager.default

func packageDirectories(in root: String) -> [String] {
  let entries = (try? fileManager.contentsOfDirectory(atPath: root)) ?? []
  return entries.sorted().map { "\(root)/\($0)" }.filter { fileManager.fileExists(atPath: "\($0)/Package.swift") }
}

func dumpPackage(at directory: String) throws -> Data {
  let process = Process()
  process.executableURL = URL(fileURLWithPath: "/usr/bin/env")
  process.arguments = ["swift", "package", "dump-package", "--package-path", directory]
  let output = Pipe()
  process.standardOutput = output
  try process.run()
  let data = output.fileHandleForReading.readDataToEndOfFile()
  process.waitUntilExit()
  guard process.terminationStatus == 0 else {
    throw NSError(
      domain: "archlint", code: Int(process.terminationStatus),
      userInfo: [NSLocalizedDescriptionKey: "dump-package \(directory)"])
  }
  return data
}

let directories = packageDirectories(in: root)
guard !directories.isEmpty else {
  FileHandle.standardError.write(Data("FAIL: archlint — в \(root) нет пакетов с Package.swift\n".utf8))
  exit(1)
}

do {
  let manifests = try directories.map { try Manifest.parse(try dumpPackage(at: $0)) }
  let graph = ArchGraph(manifests: manifests)
  if wantGraph {
    print(Report.mermaid(graph))
    exit(0)
  }
  let violations = ArchRules.check(graph)
  for line in Report.lines(for: graph, violations: violations) { print(line) }
  exit(violations.isEmpty ? 0 : 1)
} catch {
  FileHandle.standardError.write(Data("FAIL: archlint — \(error.localizedDescription)\n".utf8))
  exit(1)
}
