---
name: android-ui
description: Модули androidApp/ и их допустимые зависимости, тонкий @HiltViewModel над презентером из shared, экран = *Screen(state, onIntent) без VM внутри, <Group>NavEntry/Previews/TestTags, Roborazzi на каждое состояние, Material 3 без Expressive, ресурсы вместо литералов (i18n-check). Terse; «как и почему» — docs/reference/architecture.md. Грузится при правке androidApp/.
paths:
  - "androidApp/**"
---

# androidApp/ — Compose-слой

Матрица зависимостей и раскладка модулей — `docs/reference/architecture.md`. Здесь — что обязан
соблюдать при правке `androidApp/`.

## Модули

- `app` → всё; `feature:<group>` → `shared`, `core:{designsystem,ui,navigation,platform}` (подключает
  `stit.android.feature`); `core:ui` → `shared`, `designsystem`; `core:navigation`/`core:platform` → `shared`;
  `core:designsystem` → ничего; `core:testing` — только в `testImplementation`; `lint` — JVM, никого не знает.
- **Feature не зависит от feature** — падает на конфигурации (`stit.android.feature`, `afterEvaluate`)
  и в Konsist. Общее — в `core:*` или в `shared`; `app` — медиатор (`AppNavigation`).
- Новый модуль = `include(...)` в `settings.gradle.kts` + плагин `stit.*` + строка в
  `docs/reference/architecture.md`. Пакеты — `tech.stit.mobile.android.<module>`.

## Экраны и состояние

- **`<Key>ViewModel` — тонкая обёртка презентера из `shared`** (`@HiltViewModel`, `container.<key>Presenter()`,
  `Start` в `init`, `close()` в `onCleared`); логики нет, максимум платформенный side-effect
  (`SettingsViewModel` → `AppCompatDelegate.setApplicationLocales`). One-off событий из VM нет.
- **Экран — stateless `<Key>Screen(state, onIntent, …колбэки, modifier)` в `<Key>Screen.kt`**; stateful
  `<Key>Route` — `hiltViewModel()` (`androidx.hilt.lifecycle.viewmodel.compose`) + `collectAsStateWithLifecycle()`;
  регистрация — `<Group>NavEntry.kt` (`<group>Entries`, rule `navigation`); `<Group>Previews.kt` — `@Preview`
  на состояние; `<Group>TestTags.kt` — `testTag`-константы.
- **Konsist `ArchitectureTest`** (`app/src/test`, стадия `android unit tests`): feature ↛ feature, core ↛ feature;
  наследники `ViewModel` — `@HiltViewModel`, `*ViewModel`, в `feature/`; `@Composable *Screen` — в `*Screen.kt` без
  параметров `*ViewModel`; `hiltViewModel()` — только в `*Route` и `app/`; `*Route`/`*Entries` — только в `feature/`.
- **Roborazzi на каждое состояние** `loading / empty / populated / error / offline` (+ `_dark`):
  `<Group>ScreenshotTest : ScreenshotTest` (`core:testing`: `capture(name, darkTheme)`, `w360dp-h640dp-xhdpi`, UTC)
  → `src/test/screenshots/<key>_<state>[_dark].png`; перезапись — только `make screenshots-record` осознанно.
  Состояния — из `State` презентера (`<Group>Fixtures` в тестах), не из превью-моков.

## Платформа

- Material 3 (`androidx.compose.material3`); Material 2 и Expressive — запрещены (detekt
  `ForbiddenImport`). Иконки — `StitIcons` (`res/drawable`, Material Symbols), не `material-icons-*`.
- Цвета/размеры/типографика — только `StitTheme` (`core:designsystem`); сырой `Color(0xFF…)`,
  `.dp`/`.sp`-литерал вне designsystem — detekt `MagicNumber` + Lint `StitRawDesignValue` (rule `design-tokens`).
- Строки — только `res/values{,-ru}/strings.xml` с `resourcePrefix` модуля (`core_ui_`, `feature_todos_`);
  `HardcodedText` — ошибка Lint; паритет ключей `values/` == `values-ru/` и `core_ui_error_<code>` для
  `KnownErrorCodes` — стадия `i18n-check` (NF-17); `MessageKey` → строка через `messageText()` (`core:ui`).
- `MainActivity : AppCompatActivity` (per-app locale ниже API 33, хост `BiometricPrompt`), `enableEdgeToEdge()`,
  predictive back — `enableOnBackInvokedCallback` + `NavDisplay(onBack)`; `onBackPressed()`/`BackHandler` не
  используются. Язык — `AppCompatDelegate.setApplicationLocales`, `generateLocaleConfig` + `localeFilters en,ru`.
- Логи — `Logger` из `shared` (`LogcatSink` debug / `SentryLogSink` release), `android.util.Log` запрещён.
  Compose-тесты — `androidx.compose.ui.test.junit4.v2` (v1 — detekt), Robolectric (`robolectric.properties` пинит
  `sdk=35`); `testTag` из `UiTestTags`/`<Group>TestTags` читает и Maestro (`testTagsAsResourceId` на корне `StitApp`).

## Сигналы

- `make test` красный в `ArchitectureTest` → нарушена матрица или именование.
- `make screenshots` даёт `*_compare.png` → изменение намеренное? Тогда `make screenshots-record`.
- `i18n-check` красный → строка добавлена только в `values/` или нет `core_ui_error_<code>`.
