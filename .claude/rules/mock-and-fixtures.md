---
name: mock-and-fixtures
description: Одна фикстурная база mock/ для тестов, demo-сборки, скриншотов, DemoFlowTest и Maestro (make e2e); сценарии empty|populated|authenticated|error; MockTransport — production-код (demo-режим для App Review); фикстуры валидируются схемой спеки (mock-check); demo не ходит в сеть (тест). Грузится при правке mock/, core/mock, demo.properties, Demo.xcconfig, e2e/.
paths:
  - "mock/**"
  - "shared/**/core/mock/**"
  - "config/env/demo.properties"
  - "iosApp/Config/Demo.xcconfig"
  - "e2e/**"
---

# Mock-режим и фикстуры

## Инварианты

- **Одна фикстурная база**: `mock/scenarios.yaml` + `mock/fixtures/<scenario>/<entity>.json`
  (формат = ответы API с конвертом `data`). Её читают `commonTest`, `demo`-сборка,
  Roborazzi/snapshot-состояния, `DemoFlowTest` и Maestro-флоу `e2e/{android,ios}`. Второго набора
  фейковых данных нет.
- **Сценарии — `empty | populated | authenticated | error`** (+ проектные); имена те же, что в
  SPA-шаблоне и в `shared-api.md` карты strategy; `error` = все операции 500,
  `authenticated` = seed-сессия.
- **`MockTransport` — production-код** в `shared/…/core/mock` (Ktor `MockEngine`, роутер
  «метод + путь → фикстура/поведение», `MockState` — in-memory состояние: create/patch/delete
  меняют его до `reset(MockScenario)`); `DemoMode` хранит сценарий в `Settings`
  (`app.mock.scenario`) и сеет refresh-токен для `authenticated`. Фикстуры попадают в код объектом
  `Fixtures` из задачи `:shared:embedFixtures` (`build/generated`, не коммитится). Это demo-режим
  для App Review, не тестовая утилита.
- **`demo` не ходит в сеть**: flavor/схема `demo` фиксирует `API_MODE=mock`, хост `.invalid`;
  `MockTransportTest` проверяет, что `SharedContainer` при `mock` ходит в `MockTransport`, а при
  `real` его не создаёт. Runtime-переключатель в prod — только по `requirements.flags.demo_login`.
- **Debug-панель** (`feature/demo/DemoPanelPresenter`: сценарий, `latencyMs`) — только при
  `API_MODE=mock` (`DemoPanelState.available`, иначе `DemoMode == null`): секция `demoSection` в
  `SettingsScreen` (Android), `DemoPanelSection` в `SettingsView` (iOS).
- **Фикстуры валидируются схемой**: `FixturesTest` парсит каждую фикстуру каждого сценария
  строгими сгенерированными моделями (`make mock-check`); фикстура с полем не по схеме —
  красный гейт (NF-16). Новая сущность = фикстуры во всех сценариях (`/scaffold entity`).
- **Данные синтетические**: никаких реальных ПДн, телефонов, адресов; даты — фиксированные
  (детерминизм скриншотов).
- **Те же фейки в тестах**: презентеры — на `MockTransport` со сценарием (`testContainer(scenario)` в
  `commonTest`, `Scenario.container(_:)` в `TestSupport`), не на заглушках репозиториев; экраны — по
  сценариям (`empty`/`populated`/`error` = состояния `State`); `androidApp/app/…/DemoFlowTest.kt`
  (Robolectric + Hilt, `demoConfig()` + `FakePlatformDeps`) проходит demo-сборку от входа до задачи.
- **Maestro — `make e2e`** (`P=android` дефолт, `P=ios` — на Mac): ставит `installDemoDebug` и гонит
  `maestro test e2e/<P>` — вне гейта, сценарий `populated`; `appId` — `tech.stit.mobile.demo.debug` /
  `tech.stit.mobile.demo`; идентификаторы — `testTag` (`testTagsAsResourceId` на корне `StitApp`,
  константы `UiTestTags`/`<Group>TestTags`) и `accessibilityIdentifier`.

## Сигналы

- `make mock-check` красный → фикстура разошлась со спекой (после `make gen` — обнови фикстуру).
- Sentry-событие с сетевым вызовом из `demo` → нарушен инвариант «demo не ходит в сеть».
