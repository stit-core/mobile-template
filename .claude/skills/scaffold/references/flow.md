# scaffold › flow — навигационный сценарий

Читает строку `flows.md` карты (`slug · name · триггер · шаги (screen-keys) · выход · ветки
ошибок · status`). Флоу с сетью обязан иметь ветку ошибки; `app-start` и `login` — обязательные
флоу карты.

## Вопросы до начала (≤ 2)

1. Все `screens` шагов существуют (иначе сначала `/scaffold screen`)?
2. Какие данные нужны сценарию (`populated`, `authenticated`) — есть ли фикстуры?

## Раскладка

```
e2e/common/<slug>.yaml                        # Maestro-флоу против demo-сборки, сценарий populated; шаги по testTag/accessibilityIdentifier
e2e/android/<slug>.yaml, e2e/ios/<slug>.yaml  # только платформенные различия (permission-диалоги, системные шторки)
shared/src/commonTest/kotlin/<pkg>/feature/<…>/<Slug>FlowTest.kt   # сценарный тест презентеров: последовательность интентов → ожидаемые состояния/Route
```

## Шаги

1. RED: сценарный тест в `commonTest` — цепочка презентеров по шагам карты (например,
   `login → shell → todoList`), ветка ошибки из записи.
2. Maestro-флоу в `e2e/common` (обе платформы), платформенные различия — в `e2e/<platform>`;
   идентификаторы элементов — `Modifier.testTag` / `.accessibilityIdentifier` (уже в экранах).
3. Прогон вне гейта на эмуляторе/симуляторе (устройство нужен; на машине без KVM — только
   сценарный тест).
4. `make check` (сценарный тест — часть `shared jvm tests`).

## Anti-patterns

- ❌ Флоу без ветки ошибки при сетевых шагах.
- ❌ Логика сценария в Maestro (условия, циклы) — это презентеры и `shell`.
- ❌ Maestro в гейте — только `commonTest`-сценарий.
