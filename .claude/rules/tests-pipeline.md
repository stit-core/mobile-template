---
name: tests-pipeline
description: Пирамида тестов — презентер на JVM с фикстурами → Robolectric/Compose test v2 → Roborazzi/snapshot → Maestro (make e2e, вне gate); носители вне экранов (действие, фоновая задача, снимок виджета) тестируются в commonTest, состояния виджета — эталонами; фейки, а не моки; те же сценарии mock/; полы покрытия 80/70/70; эталоны пишет стандартное окружение; паритетные стадии и негативные фикстуры NF-01…27 — часть гейта. Грузится при правке тестов, скриншотов, снапшотов, e2e, .gate-fixtures.
paths:
  - "**/src/*Test*/**"
  - "**/commonTest/**"
  - "**/androidHostTest/**"
  - "iosApp/**/Tests/**"
  - "**/__Snapshots__/**"
  - "**/screenshots/**"
  - "e2e/**"
  - ".gate-fixtures/**"
---

# Тесты

Тесты очень важны: пишутся вместе с кодом (RED → GREEN там, где есть логика). Багфикс —
skill `/triage-failure` (падающий тест по той же причине, что и баг → фикс → один коммит).

## Пирамида и что обязано иметь тест

| слой | тест | инструменты | пол |
|---|---|---|---|
| `shared` презентеры | сценарии по состояниям через фикстуры `mock/` (`<Group>PresentersTest`); ошибки и offline-ветки | `commonTest`, `testContainer(scenario)`, `runTest`, `StandardTestDispatcher`, Turbine | 80 % строк (`:shared:koverVerify`, `generated/**` исключён) |
| `shared` репозитории, мапперы, `DeepLinkParser`, `AuthPlugin` | table-driven; single-flight refresh под параллельными запросами; reuse → logout; каждый путь deep link (`deeplinks-check`) | те же + Ktor `MockEngine` | |
| `shared` контракт | `FixturesTest` — все сценарии × сущности парсятся сгенерированными моделями (`make mock-check`) | JVM | — |
| `shared` архитектура | Konsist `ArchitectureTest` (`jvmTest`): чистый `commonMain`, слои `core < entity < feature`, плоский фасад презентеров | Konsist, стадия `shared jvm tests` | — |
| `androidMain` (`KeystoreSecureStore`, `DataStoreSettings`, `LogcatSink`) | под Robolectric (`shared/src/androidHostTest`, `robolectric.properties` sdk=35) | `androidHostTest` | — |
| Android экраны | `<Key>ScreenTest` (Compose test v2) + `<Group>ScreenshotTest : ScreenshotTest` **на каждое состояние** → `src/test/screenshots/<key>_<state>[_dark].png`; `DemoFlowTest` (`app`, Hilt + Robolectric) — demo-сборка от входа до задачи | Robolectric (`sdk=35`), Roborazzi, `core:testing` (`FakePlatformDeps`, `MainDispatcherRule`) | 70 % (`:androidApp:app:koverVerifyGate`) |
| Android архитектура | Konsist `ArchitectureTest` (`app`); `DesignSystemDetectorTest` (`lint-tests`) | стадия `android unit tests` | — |
| `shared` носители вне экранов | `<Group>ActionsTest` (успех · повтор · `SignedOut` · offline · ошибка), `<Group>TasksTest` (результат · каждая причина `Rejected` · offline; расписание через `FakeBackgroundScheduler.scheduled`), `<Group>WidgetsTest` (по строке на каждое состояние `states` карты); образцы — `Todos{Actions,Tasks,Widgets}Test` | `commonTest`, `testContainer(scenario)` | входит в 80 % `shared` |
| Android вне экранов | `<Slug>Widget{,Action}Test` и `WidgetRefreshTest` + Roborazzi на состояния снимка (`compose()` → `RemoteViews` → `Bitmap` → `captureRoboImage`, `@GraphicsMode(NATIVE)`, UTC) → `src/test/screenshots/widget_<slug>_<state>[_dark].png`; Robolectric на планировщик и доставку (`AndroidBackgroundSchedulerTest`, `BackgroundDeliveryTest` на `work-testing`) и на обработчики каналов (`NotificationActionReceiverTest`, `AddTodoActionHandlerTest`); сервис реестра AppFunctions на JVM не тестируется — Robolectric пинит `sdk=35`, поэтому в нём только делегирование | `:androidApp:widget`, `core:platform` | 70 % (агрегат) |
| ресурсы и паритет | `tokens-check` (регенерация `Tokens.kt`/`Tokens.swift`), `i18n-check` (`values/` == `values-ru/`, `core_ui_error_<code>`; xcstrings — WARN), `deeplinks-check` (манифест ↔ env ↔ `DeepLinkParserTest` ↔ `deep-links.md`), `capability-check` (реестр `background.md` ↔ исполнители и идентификаторы, состояния виджетов ↔ эталоны) | `scripts/tools/*-check.sh` | — |
| iOS `Pure` + `Tools/archlint` | Swift Testing (`TokensTests`, `AppConfigTests`, …; `ArchRulesTests`) | `swift test`, `coverage-floor.sh` (llvm-cov по всем `*Tests.so`) | 70 % |
| iOS `Core`/`Features` | Swift Testing на `Scenario.container` + `waitUntil` (`<Key>ModelTests`); `SnapshotTests` — `Snapshots.assertScreen` light + dark на состояние (Mac) | `check-mac` (планы `Unit`, `Snapshot`), `swift-snapshot-testing` | без пола |
| гейт | негативная фикстура на каждый механизм: NF-01…10 каркас, NF-11…16 `shared`, NF-17…19 паритет, NF-20…24 Swift, NF-25…27 поверхности вне экранов | `make gate-selftest` | все красные |
| e2e | Maestro `e2e/android/{app-start,login,todos}.yaml`, `e2e/ios/{app-start,login}.yaml` против demo-сборки | `make e2e` (`P=android` дефолт; iOS — Mac) | вне gate |

## Инварианты

- **Фейки, не моки**: `Fake<Port>` в `shared/src/commonTest/…/testing/Fakes.kt` (`FakePlatformDeps`,
  `FakeSecureStore`, `FakeClock`, `FakeBackgroundScheduler`, `TestDispatchers`) + `testContainer(scenario)` на `MockTransport`;
  `androidApp/core/testing/Fakes.kt` и `iosApp/Packages/TestSupport` — та же форма; MockK — только
  когда фейк невозможен. Мок и фейк одного порта вместе не живут.
- **Детерминизм**: фиксированный `Clock`, `DispatcherProvider`, UTC в `ScreenshotTest`, фикстуры
  `mock/fixtures/<scenario>/`; сценарии те же, что в `demo`-сборке (`empty | populated | authenticated | error`).
- **Эталоны** (`src/test/screenshots/`, `__Snapshots__/`) перезаписываются только
  `make screenshots-record` / `make snapshot-record` осознанно и на стандартном окружении
  (JDK/toolchain из `libs.versions.toml`, симулятор из `.test-simulator`). `*_compare.png` — сигнал, не мусор.
- **Compose test API v2** (`androidx.compose.ui.test.junit4.v2`); v1-импорты — detekt.
  `isIncludeAndroidResources = true` уже включён плагинами.
- **Тесты читаются как спецификация**: имена по-русски в backticks допустимы, идентификаторы —
  английские; закомментированных и `@Ignore`-тестов нет.
- **Негативная фикстура** `.gate-fixtures/NF-NN/fixture.sh` (первая строка `# NF-NN — <что ловит>`,
  `EXPECT`, `TOUCHES`, `apply`) на каждый новый механизм гейта; новая стадия — сначала в `stage()` и
  `STAGES` `check.sh`; `make gate-selftest F=NF-NN` — одна фикстура.
- `make test` — JVM-набор + Swift `Pure`; `make check-fast` — Swift-формат, SwiftLint, Kotlin-формат,
  detekt, `shared` jvmTest в цикле «пока пишу».
