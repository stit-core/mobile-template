---
name: android-ui
description: Модули androidApp/ и их допустимые зависимости, тонкий @HiltViewModel над презентером из shared, экран = *Screen(state, onIntent) без VM внутри, превью и Roborazzi на каждое состояние, Material 3 без Expressive, ресурсы вместо литералов. Terse; «как и почему» — docs/reference/architecture.md. Грузится при правке androidApp/.
paths:
  - "androidApp/**"
---

# androidApp/ — Compose-слой

Матрица зависимостей и раскладка модулей — `docs/reference/architecture.md`. Здесь — что обязан
соблюдать при правке `androidApp/`.

## Модули

- `app` → всё; `feature:<x>` → `shared`, `core:{designsystem,ui,navigation,platform}` (зависимости
  подключает `stit.android.feature`); `core:ui`/`core:navigation`/`core:platform` → `shared`;
  `core:designsystem` → ничего; `core:testing` — только в `testImplementation`; `lint` — JVM,
  никого не знает.
- **Feature не зависит от feature** — падает уже на конфигурации (`stit.android.feature`,
  `afterEvaluate`) и в Konsist. Общее — в `core:*` или в `shared`.
- Новый модуль = `include(...)` в `settings.gradle.kts` + плагин `stit.*` + строка в
  `docs/reference/architecture.md`. Пакеты — `tech.stit.mobile.android.<module>`.

## Экраны и состояние

- **`<X>ViewModel` — тонкая обёртка презентера из `shared`** (`@HiltViewModel`, только в
  `feature:*`, имя `*ViewModel` — Konsist). Логики в VM нет: маппинг/навигация максимум.
- **Экран — stateless `<X>Screen(state, onIntent)` в файле `<X>Screen.kt`**, stateful
  обёртка `<X>Route` подключает `hiltViewModel()` из
  `androidx.hilt.lifecycle.viewmodel.compose` и `collectAsStateWithLifecycle()`.
- **`@Preview` + Roborazzi-скриншот на каждое состояние** `loading / empty / populated /
  error / offline`; эталоны — `src/test/screenshots/`, перезапись — только `make
  screenshots-record` осознанно. Состояния — из `State` презентера, не из превью-моков.
- Один поток состояния вниз, интенты вверх; one-off событий из VM нет.

## Платформа

- Material 3 (`androidx.compose.material3`); Material 2 и Expressive — запрещены (detekt
  `ForbiddenImport`). Иконки — `res/drawable` (Material Symbols), не `material-icons-*`.
- Цвета/размеры/типографика — только `StitTheme` (`core:designsystem`); сырой `Color(0xFF…)`
  или `.dp`-литерал в композабле — detekt `MagicNumber` + `:androidApp:lint`.
- Строки — только `res/values*/strings.xml` (ru/en); `HardcodedText` — ошибка Lint.
  Ресурсы модуля с `resourcePrefix` (`core_ui_`, `feature_todos_`).
- Edge-to-edge (`enableEdgeToEdge()`), predictive back (`BackHandler`/`PredictiveBackHandler`),
  `onBackPressed()` не используется. Per-app locales — `setApplicationLocales`.
- Логи — `Logger` из `shared` (`core/observability`), sink — `LogcatSink`; `android.util.Log` запрещён.
- Compose-тесты — `androidx.compose.ui.test.junit4.v2` (v1-импорты — detekt), Robolectric
  (`robolectric.properties` пинит `sdk=35`).

## Сигналы

- `make test` красный в `ArchitectureTest` → нарушена матрица или именование.
- `make screenshots` даёт `*_compare.png` → изменение намеренное? Тогда `make screenshots-record`.
