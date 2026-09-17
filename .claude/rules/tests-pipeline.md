---
name: tests-pipeline
description: Пирамида тестов — презентер на JVM с фикстурами → Robolectric/Compose test v2 → Roborazzi/snapshot → Maestro (вне gate); фейки, а не моки; те же сценарии mock/; полы покрытия 80/70/70; эталоны пишет стандартное окружение; негативные фикстуры — часть гейта. Грузится при правке тестов, скриншотов, снапшотов, e2e, .gate-fixtures.
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
| `shared` презентеры, use-case'ы | сценарии по состояниям через фикстуры `mock/`; ошибки и offline-ветки | `commonTest`, `runTest`, `StandardTestDispatcher`, Turbine | 80 % строк (`:shared:koverVerify`, `generated/**` исключён) |
| `shared` репозитории, мапперы, `DeepLinkParser`, `AuthPlugin` | table-driven; single-flight refresh под параллельными запросами; reuse → logout | те же + Ktor `MockEngine` | |
| `shared` контракт | `FixturesTest` — все сценарии × сущности парсятся сгенерированными моделями (`make mock-check`) | JVM | — |
| `shared` архитектура | Konsist `ArchitectureTest` (`jvmTest`): чистый `commonMain`, слои `core < entity < feature`, плоский фасад презентеров | Konsist, стадия `shared jvm tests` | — |
| `androidMain` (`KeystoreSecureStore`, `DataStoreSettings`, `LogcatSink`) | под Robolectric (`shared/src/androidHostTest`, `robolectric.properties` sdk=35) | `androidHostTest` | — |
| Android экраны | Robolectric Compose-тест + Roborazzi-скриншот **на каждое состояние** | Compose test v2, Roborazzi (`robolectric.properties` sdk=35) | 70 % (`:androidApp:app:koverVerifyGate`) |
| Android архитектура | Konsist `ArchitectureTest`; тест lint-детектора | Konsist, `lint-tests` | — |
| iOS `Pure` | Swift Testing, параметризованные | `swift test`, `llvm-cov` (`coverage-floor.sh`) | 70 % |
| iOS `Core`/`Features` | Swift Testing + snapshot на каждое состояние (Mac) | `check-mac`, `swift-snapshot-testing` | без пола |
| гейт | негативная фикстура на каждый механизм | `make gate-selftest` | все красные |
| e2e | Maestro-флоу `app-start`, `login`, `todos` (устройство/эмулятор) | `e2e/` | вне gate |

## Инварианты

- **Фейки, не моки**: `Fake<Port>` в `shared/src/commonTest/…/testing/Fakes.kt` (`FakePlatformDeps`,
  `FakeSecureStore`, `FakeClock`, `TestDispatchers`) + `testContainer(scenario)` на `MockTransport`,
  и `androidApp/core/testing`; MockK — только когда фейк невозможен. Мок и фейк одного порта
  вместе не живут.
- **Детерминизм**: фиксированный `Clock`, `DispatcherProvider`, фикстуры `mock/fixtures/<scenario>/`;
  сценарии те же, что в `demo`-сборке (`empty | populated | authenticated | error`).
- **Эталоны** (`src/test/screenshots/`, `__Snapshots__/`) перезаписываются только
  `make screenshots-record` / `make snapshot-record` осознанно и на стандартном окружении
  (JDK/toolchain из `libs.versions.toml`, симулятор из `.test-simulator`). `*_compare.png`
  — сигнал, не мусор.
- **Compose test API v2** (`androidx.compose.ui.test.junit4.v2`); v1-импорты — detekt.
  `isIncludeAndroidResources = true` уже включён плагинами.
- **Тесты читаются как спецификация**: имена по-русски в backticks допустимы, идентификаторы
  — английские; закомментированных и `@Ignore`-тестов нет.
- **Негативная фикстура** `.gate-fixtures/NF-NN/fixture.sh` (`EXPECT`, `TOUCHES`, `apply`) на
  каждый новый механизм гейта; новая стадия — сначала в `stage()` и `STAGES` `check.sh`.
- `make test` — весь набор JVM; `make check-fast` — `shared` jvmTest в цикле «пока пишу».
