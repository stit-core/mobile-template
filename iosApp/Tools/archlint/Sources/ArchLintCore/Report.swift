public enum Report {
  public static func lines(for graph: ArchGraph, violations: [Violation]) -> [String] {
    if violations.isEmpty {
      return ["OK: archlint — модулей \(graph.modules.count), рёбер \(graph.edges.count)"]
    }
    return ["FAIL: archlint — нарушений \(violations.count)"] + violations.map { "  \($0)" }
  }

  // Mermaid-граф для docs: `archlint <Packages> --graph`.
  public static func mermaid(_ graph: ArchGraph) -> String {
    let lines = graph.edges.map { "  \(id($0.from)) --> \(id($0.to))" }
    return (["graph LR"] + lines).joined(separator: "\n")
  }

  private static func id(_ module: Module) -> String {
    "\(module.package)_\(module.target)[\"\(module)\"]"
  }
}
