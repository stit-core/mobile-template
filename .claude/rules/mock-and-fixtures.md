---
name: mock-and-fixtures
description: Одна фикстурная база mock/ для тестов, demo-сборки, скриншотов и Maestro; сценарии empty|populated|authenticated|error; MockTransport — production-код (demo-режим для App Review); фикстуры валидируются схемой спеки (mock-check); demo не ходит в сеть (тест). Грузится при правке mock/, core/mock, src/demo, Demo.xcconfig, e2e/.
paths:
  - "mock/**"
  - "shared/**/core/mock/**"
  - "androidApp/**/src/demo/**"
  - "iosApp/Config/Demo.xcconfig"
  - "e2e/**"
---

# Mock-режим и фикстуры

## Инварианты

- **Одна фикстурная база**: `mock/scenarios.yaml` + `mock/fixtures/<scenario>/<entity>.json`
  (формат = ответы API с конвертом `data`). Её читают `commonTest`, `demo`-сборка,
  Roborazzi/snapshot-состояния и Maestro-флоу `e2e/common`. Второго набора фейковых данных
  нет.
- **Сценарии — `empty | populated | authenticated | error`** (+ проектные); имена те же, что в
  SPA-шаблоне и в `shared-api.md` карты strategy; `error` = все операции 500,
  `authenticated` = seed-сессия.
- **`MockTransport` — production-код** в `shared/…/core/mock` (Ktor `MockEngine`, роутер
  «метод + путь → фикстура/поведение», `MockState` — in-memory состояние: create/patch/delete
  меняют его до `reset(MockScenario)`); `DemoMode` хранит сценарий в `Settings` и сеет
  refresh-токен для `authenticated`. Фикстуры попадают в код объектом `Fixtures` из задачи
  `:shared:embedFixtures` (`build/generated`, не коммитится). Это demo-режим для App Review,
  не тестовая утилита.
- **`demo` не ходит в сеть**: flavor/схема `demo` фиксирует `API_MODE=mock`, хост `.invalid`;
  `MockTransportTest` проверяет, что `SharedContainer` при `mock` ходит в `MockTransport`, а при
  `real` его не создаёт. Runtime-переключатель в prod — только по `requirements.flags.demo_login`.
- **Debug-панель** (`feature/demo/DemoPanelPresenter`: сценарий, `latencyMs`) — доступна только
  при `API_MODE=mock` (`DemoPanelState.available`, иначе `DemoMode == null`); состояние —
  `Settings` (`app.mock.scenario`).
- **Фикстуры валидируются схемой**: `FixturesTest` парсит каждую фикстуру каждого сценария
  строгими сгенерированными моделями (`make mock-check`); фикстура с полем не по схеме —
  красный гейт (NF-16). Новая сущность = фикстуры во всех сценариях (`/scaffold entity`).
- **Данные синтетические**: никаких реальных ПДн, телефонов, адресов; даты — фиксированные
  (детерминизм скриншотов).
- **Те же фейки в тестах**: презентеры тестируются на `MockTransport` со сценарием, не на
  ручных заглушках репозиториев; скриншоты экранов — по сценариям (`empty`/`populated`/`error`
  = состояния `State`).
- **Maestro (`e2e/`)** — вне гейта, на сценарии `populated` против `demo`-сборки;
  идентификаторы элементов — `testTag`/`accessibilityIdentifier`.

## Сигналы

- `make mock-check` красный → фикстура разошлась со спекой (после `make gen` — обнови фикстуру).
- Sentry-событие с сетевым вызовом из `demo` → нарушен инвариант «demo не ходит в сеть».
