---
name: ios-ui
description: SwiftUI-слой iosApp/ — View рисует State презентера из shared, @Observable-адаптер без бизнес-логики, Features не импортируют друг друга и Platform/Persistence, строки из Localizable.xcstrings, Liquid Glass системными компонентами, snapshot на каждое состояние. Terse; «как и почему» — docs/reference/architecture.md. Грузится при правке .swift в iosApp/.
paths:
  - "iosApp/**/*.swift"
---

# iosApp/ — SwiftUI-слой

Пакеты и матрица зависимостей — `docs/reference/architecture.md`. Здесь — что обязан
соблюдать при правке Swift-кода. Всё, что импортирует `Shared`/SwiftUI, компилируется только
на Mac (`make check-mac`); на Linux собирается и тестируется только `Packages/Pure`.

## Инварианты

- **View — только представление.** Логика состояний экрана живёт в `shared`
  (`<X>Presenter`); Swift-модель экрана — тонкий `@Observable` адаптер: подписка на
  `state`, `dispatch(intent)`. Никаких сетевых вызовов, правил и хранения в View/Model.
- **`Features/<X>` не импортируют друг друга** и не импортируют `Core/{Persistence,Platform}`
  — реализации `PlatformDeps` собирает только `App/AppEnvironment.swift`. `Core/*` не
  импортирует `Features`. Ловят SwiftLint custom rules + `Tools/archlint`.
- **Сеть — только через `Shared`**: `URLSession(` вне `Core/SharedBridge` запрещён.
- **Строки — `Localizable.xcstrings`** (ru/en, plural-вариации); литералы в View — нет.
- **Токены — `DesignSystem.Theme`** (значения — `Pure/DesignSystemTokens`); `Color(red:…)`,
  `.font(.system(size:))`, фиксированные отступы вне токенов — запрещены.
- **Liquid Glass — системными компонентами**; `.glassEffect()` только на функциональных
  элементах; кастомный фон системных баров — нет.
- **Навигация — `NavigationStack(path: [Route])`** через `Core/Routing`; `Route` приходит из
  `Shared` (rule `navigation`). `sheet`/`fullScreenCover` не стекируются.
- **a11y**: `accessibilityIdentifier` на интерактивных элементах (XCUITest), Dynamic Type
  через `.font(.body)`/`relativeTo:`, touch-target ≥ 44 pt.
- **Snapshot-тест (`swift-snapshot-testing`) на каждое состояние экрана**; эталоны —
  `__Snapshots__/`, перезапись — только `make snapshot-record` на симуляторе из
  `iosApp/.test-simulator`.
- **Логи — `AppFoundation.log` (`os.Logger`)**; `print`/`debugPrint`/`NSLog` — SwiftLint.
- `try!`, force unwrap — только в тестах; `import Combine` — запрещён (rule `swift-concurrency`).
- Новая фича = таргет в `Packages/Features/Package.swift` + реэкспорт в umbrella `Features`;
  `.pbxproj` не трогается (rule `xcode-project`).

## Сигналы

- `no such module 'SwiftUI'` на Linux → таргет не в `Pure`; проверяется на Mac.
- Изменился фасад `shared` → Swift-адаптеры правятся атомом «Mac-verify» (skill `/plan`).
