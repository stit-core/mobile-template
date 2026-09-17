# NF-24 — @preconcurrency без комментария-причины: ловит SwiftLint custom rule preconcurrency_needs_why
EXPECT="swiftlint"
TOUCHES="iosApp/Packages/Core/Sources/Platform/BadPreconcurrency.swift"
apply() {
    cat >"$TOUCHES" <<'SWIFT'
@preconcurrency import Sentry

func breadcrumbCount() -> Int {
  0
}
SWIFT
}
