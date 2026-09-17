---
name: xcode-project
description: iosApp.xcodeproj руками не правится; новые таргеты — Package.swift + umbrella Features; настройки per env — xcconfig (ловушка https:/$()/host); три test plans; симулятор фиксирован в .test-simulator; .xcode-version — источник истины. Грузится при правке .pbxproj, Package.swift, .xcconfig, .xctestplan, .test-simulator.
paths:
  - "iosApp/**/*.pbxproj"
  - "iosApp/**/Package.swift"
  - "iosApp/**/*.xcconfig"
  - "iosApp/**/*.xctestplan"
  - "iosApp/.test-simulator"
---

# Xcode-проект

## Инварианты

- **`iosApp/iosApp.xcodeproj` руками не правится** (`Edit`/`Write` на `*.pbxproj` — в
  `permissions.deny`). Имя проекта постоянно; product name, bundle id, team, deployment
  target — в `iosApp/Config/*.xcconfig`; `/de-template` меняет только xcconfig.
- **Buildable folders + один `.xcodeproj`**: пакеты `Packages/{Pure,Core,Features,TestSupport}`
  — SwiftPM local. Новая фича = таргет в `Packages/Features/Package.swift` + реэкспорт в
  umbrella-продукте `Features`; проект не трогается. Tuist/XcodeGen — нет.
- **xcconfig per env**: `Base` + `Dev|Staging|Prod|Demo` (+ gitignored `Local.xcconfig`,
  образец — `Local.xcconfig.example`). Значения уходят в `Info.plist` через `$(VAR)` →
  `AppFoundation.AppConfig`. URL пишется как `https:/$()/host` — `//` в xcconfig это
  комментарий.
- **Схемы = flavors Android** (`Dev`, `Staging`, `Prod`, `Demo`); `Demo` — `API_MODE=mock`.
- **Три test plans**: `TestPlans/{Unit,Snapshot,UI}.xctestplan`; `check-mac` гоняет `Unit` +
  `Snapshot`, `UI` — по `MAC_UI=1`.
- **Симулятор фиксирован** в `iosApp/.test-simulator` (`platform=iOS Simulator,name=…`);
  snapshot-эталоны валидны только для него.
- **`.xcode-version` (27.0) и `.swift-version` (6.4) — источник истины**: `mac.sh doctor`
  сверяет Xcode на хосте; смена версии — через план и `docs/stack/runtime.md`.
- **`Package.swift`**: порядок именованных аргументов фиксирован (`products` раньше
  `dependencies`); `swiftLanguageModes: [.v6]`; `-warnings-as-errors`; зависимости пакета —
  запись в `docs/stack/dependencies.md` + privacy manifest (rule `privacy-and-law`).
- **`PrivacyInfo.xcprivacy`** обязателен в `App/`; `UserDefaults` — required-reason API.
- **Entitlements** (`applinks:`, push) — только через `/scaffold entry-point` и запись в
  `docs/stack/deep-links.md`/`push.md`; `NSAllowsArbitraryLoads` — нет.

## Верификация

Всё, что здесь описано, проверяется на Mac: `make mac-doctor` → `make check-mac`
(`docs/reference/mac-host.md`). На Linux — только `Package.swift` через `Tools/archlint`
(`swift package dump-package`).
