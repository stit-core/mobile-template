---
name: ios-ui
description: SwiftUI-слой iosApp/ — View рисует State презентера из shared через PresenterModel-адаптер без бизнес-логики, Features не импортируют друг друга и Platform/Persistence, строки из Localizable.xcstrings таргета, токены через @Environment(\.theme), Liquid Glass системными компонентами, snapshot на состояние; вне экранов — интенты Core/SystemActions, записи виджетов Core/Widgets и вью WidgetsFeature, фон Core/Platform/Background. Terse; «как и почему» — docs/reference/architecture.md. Грузится при правке .swift в iosApp/.
paths:
  - "iosApp/**/*.swift"
---

# iosApp/ — SwiftUI-слой

Пакеты и матрица зависимостей — `docs/reference/architecture.md`. Всё, что импортирует
`Shared`/SwiftUI, компилируется только на Mac (`make check-mac`); на Linux собираются и
тестируются `Packages/Pure` и `Tools/archlint`, SwiftLint и swift-format идут по всему дереву.

## Инварианты

- **View — только представление.** Логика экрана — `<X>Presenter` в `shared`; Swift-модель —
  `final class <X>Model: PresenterModel<XState, XIntent>` (`Core/SharedBridge/PresenterModel.swift`:
  `state` из `StateFlow`, `dispatch(_:)`, `deinit` закрывает презентер), методы-обёртки интентов
  (`start()`, `refresh()`); никаких сетевых вызовов, правил и хранения в View/Model.
- **`Features/<X>` не импортируют друг друга** и `Core/{Persistence,Platform}` — реализации
  `PlatformDeps` собирает только `App/AppEnvironment.swift` (`IosPlatformDeps`); `Core/*` не
  импортирует `Features`. Ловят SwiftLint `no_feature_import`, `no_platform_import_in_features`
  и `Tools/archlint` (граф `Package.swift`).
- **Вне экранов — `Core/{SystemActions,Widgets}` (только `Pure`, без `Shared`),
  `Core/Platform/{Background,SystemActionsBridge,NotificationActions,WidgetRefresh}`,
  `Features/WidgetsFeature` и appex `iosApp/Widgets/`**; таргет действий зовётся `SystemActions`,
  а не `Intents` (это системный модуль SiriKit). Инварианты — rule `off-screen-surfaces`.
- **Сеть — только через `Shared`**: `URLSession(` вне `Core/SharedBridge` — SwiftLint `no_raw_urlsession`.
- **Строки — `Resources/Localizable.xcstrings` своего таргета** (`defaultLocalization: "ru"`, ru/en),
  `Text("<slug>.<key>", bundle: .module)`; ключи ошибок `error.<code>` — каталог `DesignSystem`
  (`ErrorStateView(messageKey:)`, `ErrorText`); литералы в View — нет.
- **Токены — `@Environment(\.theme)`** (`theme.colors/metrics/typography`, значения —
  `Pure/DesignSystemTokens`); компоненты — `PrimaryButtonStyle`, `.card()`, `LoadingStateView`,
  `EmptyStateView`, `ErrorStateView`, `ErrorText`, `OfflineBanner`. `Color(red:…)`,
  `.font(.system(size:))` — SwiftLint `no_hardcoded_color`/`no_fixed_font_size`.
- **Liquid Glass — системными компонентами** (`TabView`, `NavigationStack`, `Form`, `List`);
  `.glassEffect()` только на функциональных элементах; кастомный фон системных баров — нет.
- **Навигация — `Router` + `NavigationStack(path:)` на таб** (`RootView`), маршруты — `AppRoute`
  (rule `navigation`); модель экрана создаёт `Screen<Model, Content>` (`RootView.swift`) из
  фабрик `AppEnvironment.<x>Model()`; `sheet`/`fullScreenCover` не стекируются.
- **a11y**: `accessibilityIdentifier` точечной нотацией (`login.submit`, `todos.row.<id>`,
  `state.error`) — их читают XCUITest и Maestro `e2e/ios`; Dynamic Type через `theme.typography`
  (системные `Font`), touch-target ≥ 44 pt (`metrics.touchMin`).
- **Тесты**: Swift Testing на `Scenario.container(.populated|.empty|.error|.authenticated)` +
  `waitUntil` (`TestSupport`); snapshot на состояние экрана — `Snapshots.assertScreen(view, named:)`
  (light + dark, `ViewImageConfig.iPhone13`) в `Features/Tests/SnapshotTests`; эталоны
  `__Snapshots__/`, перезапись — только `make snapshot-record` на симуляторе из `iosApp/.test-simulator`.
- **Логи — `AppFoundation.AppLogger` (`OSLogSink` → `os.Logger`)**; `print`/`debugPrint`/`NSLog` —
  SwiftLint `no_print`/`no_nslog`. `try!`, force unwrap — только в тестах (swift-format);
  `import Combine` — `no_combine` (rule `swift-concurrency`).
- Новая фича = имя в `featureNames` (`Packages/Features/Package.swift`: таргет + `<Name>Tests`) +
  `@_exported import` в `Sources/Features/Features.swift`; `.pbxproj` не трогается (rule `xcode-project`).

## Сигналы

- `no such module 'SwiftUI'` на Linux → таргет не в `Pure`; проверяется на Mac.
- Изменился фасад `shared` → Swift-адаптеры правятся атомом «Mac-verify» (skill `/plan`).
