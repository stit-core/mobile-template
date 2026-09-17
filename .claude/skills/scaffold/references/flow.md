# scaffold › flow — навигационный сценарий

Читает строку `flows.md` карты (`slug · name · триггер · шаги (screen-keys) · выход · ветки
ошибок · status`). Флоу с сетью обязан иметь ветку ошибки; `app-start` и `login` — обязательные
флоу карты.

## Вопросы до начала (≤ 2)

1. Все `screens` шагов существуют (иначе сначала `/scaffold screen`)?
2. Какие данные нужны сценарию (`populated`, `authenticated`) — есть ли фикстуры?

## Раскладка

```
shared/src/commonTest/kotlin/<pkg>/feature/<Group>PresentersTest.kt   # сценарный тест презентеров: цепочка интентов → состояния/Route
                                                                       # образцы: ShellPresenterTest — app-start, AuthPresentersTest — login, TodoPresentersTest — todos
androidApp/app/src/test/kotlin/<pkg>/android/DemoFlowTest.kt           # Robolectric + Hilt: demo-сборка от старта до экрана на фикстурах mock/ (образец)
e2e/android/<slug>.yaml                                                # Maestro против demo-сборки (appId tech.stit.mobile.demo.debug), сценарий populated
e2e/ios/<slug>.yaml                                                    # то же для схемы Demo (appId tech.stit.mobile.demo); гоняется с Mac
```

Идентификаторы элементов: Android — `Modifier.testTag` из `UiTestTags`/`<Group>TestTags`
(`login_email`, `todo_list`; `testTagsAsResourceId` на корне `StitApp` превращает их в
resource-id); iOS — `accessibilityIdentifier` (`login.submit`, `todos.screen`). Общие шаги —
через `runFlow:` (`todos.yaml` → `login.yaml`).

## Шаги

1. RED: сценарный тест в `commonTest` — цепочка презентеров по шагам карты (например,
   `login → shell → todoList`), ветка ошибки из записи (сценарий `error`).
2. Maestro-флоу `e2e/android/<slug>.yaml` и `e2e/ios/<slug>.yaml` — только по существующим
   идентификаторам; платформенные различия (permission-диалоги, шторки) — внутри своего файла.
3. Прогон вне гейта: `make e2e` (`P=android` дефолт — `installDemoDebug` + `maestro test`;
   `P=ios` — на Mac). Без эмулятора (нет KVM) — только сценарный тест.
4. `make check` (сценарный тест — часть `shared jvm tests`, `DemoFlowTest` — `android unit tests`).

## Anti-patterns

- ❌ Флоу без ветки ошибки при сетевых шагах.
- ❌ Логика сценария в Maestro (условия, циклы) — это презентеры и `shell`.
- ❌ Maestro в гейте — только `commonTest`-сценарий и `DemoFlowTest`.
