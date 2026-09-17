---
name: xcode-project
description: iosApp.xcodeproj руками не правится; новые таргеты — Package.swift + umbrella Features; настройки per env — xcconfig (ловушка https:/$()/host); три test plans; симулятор фиксирован в .test-simulator; .xcode-version — источник истины; wiring локальных пакетов и per-env схем — на Mac. Грузится при правке .pbxproj, Package.swift, .xcconfig, .xctestplan, .test-simulator.
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
  `permissions.deny`). Таргет `iosApp` (product `iosApp.app`), базовая конфигурация —
  `Config/Dev.xcconfig`, `PRODUCT_BUNDLE_IDENTIFIER = $(BUNDLE_ID)`, build phase «Compile Kotlin
  Framework» → `scripts/embed-shared.sh` (`:shared:embedAndSignAppleFrameworkForXcode`,
  `FRAMEWORK_SEARCH_PATHS` → `shared/build/xcode-frameworks`). Product name, bundle id, team,
  deployment target — только в `Config/*.xcconfig`; `/de-template` меняет только xcconfig.
- **Пакеты — SwiftPM local**: `Packages/{Pure,Core,Features,TestSupport}` + `Tools/archlint`.
  Новая фича = имя в `featureNames` (`Packages/Features/Package.swift`) + `@_exported import` в
  umbrella `Features`; проект не трогается. Tuist/XcodeGen — нет.
- **Требует Mac** (первый `make check-mac`, `docs/reference/mac-host.md`): подключение локальных
  пакетов и `Shared.framework` к проекту, per-env конфигурации и схемы `Staging|Prod|Demo`
  (в репо shared-схема — `Dev`; `mac.sh` и `run-ios` работают с ней), UI-test таргет
  `iosAppUITests` (`TestPlans/UI.xctestplan`, `UITests/AppLaunchTests.swift`), `Package.resolved`,
  эталоны `__Snapshots__/`. Результат коммитится.
- **xcconfig per env**: `Base` + `Dev|Staging|Prod|Demo` (+ gitignored `Local.xcconfig`, образец
  `Local.xcconfig.example` — `TEAM_ID`). Ключи = `config/env/*.properties` (`APP_ENV`, `API_BASE_URL`,
  `API_MODE`, `AUTH_MODE`, `DEEP_LINK_HOST`, `DEEP_LINK_SCHEME`, `SENTRY_DSN`, `PUSH_PROVIDERS`) +
  `APS_ENVIRONMENT`, `BUNDLE_ID_SUFFIX`, `APP_DISPLAY_NAME`; значения уходят в `Info.plist` через
  `$(VAR)` → `AppFoundation.AppConfig` (отсутствующий ключ — `AppConfigError.missingKey`). URL пишется
  как `https:/$()/host` — `//` в xcconfig это комментарий.
- **Три test plans** `TestPlans/{Unit,Snapshot,UI}.xctestplan`: `Unit` — тест-таргеты `Pure`, `Core`,
  `Features` (`language: ru`, `codeCoverage`), `Snapshot` — `SnapshotTests` (не параллельно), `UI` —
  `iosAppUITests`; `check-mac` гоняет `Unit` + `Snapshot`, `UI` — по `MAC_UI=1`.
- **Симулятор фиксирован** в `iosApp/.test-simulator` (`platform=iOS Simulator,name=iPhone 17,OS=27.0`);
  snapshot-эталоны валидны только для него.
- **`.xcode-version` (27.0) и `.swift-version` (6.4) — источник истины**: `mac.sh doctor`
  сверяет Xcode на хосте; смена версии — через план и `docs/stack/runtime.md`.
- **`Package.swift`**: `swift-tools-version: 6.2`, `platforms: [.iOS(.v18)]` (`Pure`/`archlint` —
  плюс `.macOS(.v15)` для Linux-сборки), `products` раньше `dependencies`, `defaultLocalization: "ru"`,
  ресурсы `.process("Resources")`; политика строгости — rule `swift-concurrency`. Внешние
  зависимости — `sentry-cocoa` (`Core`), `swift-snapshot-testing` (`TestSupport`); новая — запись в
  `docs/stack/dependencies.md` + privacy manifest (rule `privacy-and-law`).
- **`PrivacyInfo.xcprivacy`** обязателен в `App/` (`UserDefaults` — required-reason `CA92.1`);
  `Info.plist` — `ITSAppUsesNonExemptEncryption=false`, `UIBackgroundModes remote-notification`.
- **Entitlements** (`applinks:$(DEEP_LINK_HOST)`, `aps-environment = $(APS_ENVIRONMENT)`) — только через
  `/scaffold entry-point` и запись в `docs/stack/deep-links.md`/`push.md`; `NSAllowsArbitraryLoads` — нет.
- `ExportOptions.plist` — `method app-store-connect`, `destination export`, `uploadSymbols` (`/release`).

## Верификация

Всё, что здесь описано, проверяется на Mac: `make mac-doctor` → `make check-mac`. На Linux —
`Package.swift` через `Tools/archlint` (`swift package dump-package`), SwiftLint и swift-format.
