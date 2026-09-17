# NF-23 — URLSession( в фиче: ловит SwiftLint custom rule no_raw_urlsession (сеть — только через Shared)
EXPECT="swiftlint"
TOUCHES="iosApp/Packages/Features/Sources/TodosFeature/BadNetwork.swift"
apply() {
    cat >"$TOUCHES" <<'SWIFT'
import Foundation

func fetchDirectly() -> URLSession {
  URLSession(configuration: .default)
}
SWIFT
}
