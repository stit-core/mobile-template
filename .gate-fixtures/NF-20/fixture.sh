# NF-20 — print( в Swift-коде: ловит SwiftLint custom rule no_print (логи — AppFoundation.AppLogger)
EXPECT="swiftlint"
TOUCHES="iosApp/Packages/Pure/Sources/AppFoundation/BadPrint.swift"
apply() {
    cat >"$TOUCHES" <<'SWIFT'
public func debugDump(_ value: String) {
  print(value)
}
SWIFT
}
