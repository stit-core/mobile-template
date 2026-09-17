# NF-22 — import UIKit в Packages/Pure: ловит swift build на Linux (no such module 'UIKit'), Pure — только Foundation
EXPECT="swift pure build+tests"
TOUCHES="iosApp/Packages/Pure/Sources/AppFoundation/BadUIKit.swift"
apply() {
    cat >"$TOUCHES" <<'SWIFT'
import UIKit

public func screenScale() -> Double {
  Double(UIScreen.main.scale)
}
SWIFT
}
