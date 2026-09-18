---
name: off-screen-surfaces
description: Виджеты системы, фоновые задачи и системные действия — носитель в shared (публичный класс с internal-конструктором, отказ возвращается, а не бросается), платформенная обёртка без логики, снимок вместо репозитория, ключ задачи = slug, состояния виджета = эталоны, ответ действия = ресурс в обоих values; стадия capability-check. Terse; «как и почему» — docs/reference/architecture.md. Грузится при правке core/background, core/widget, *Task/*Action/*Widget*, androidApp/widget, core:platform/{background,action,widget}, iOS SystemActions/Widgets/Background и docs/stack/background.md.
paths:
  - "shared/**/core/background/**"
  - "shared/**/core/widget/**"
  - "shared/**/feature/**/*Task.kt"
  - "shared/**/feature/**/*Action.kt"
  - "shared/**/feature/**/*Widget*.kt"
  - "androidApp/widget/**"
  - "androidApp/core/platform/**/background/**"
  - "androidApp/core/platform/**/action/**"
  - "androidApp/core/platform/**/widget/**"
  - "iosApp/**/SystemActions/**"
  - "iosApp/**/Widgets/**"
  - "iosApp/**/Background/**"
  - "docs/stack/background.md"
---

# Поверхности вне экранов

Три kind'а карты (`widgets`, `background-tasks`, `system-actions`) исполняет система — без экрана
и часто без процесса приложения. Раскладка и «почему» — `docs/reference/architecture.md`
(«Вне экранов»), механика платформ — `docs/stack/background.md`.

## Инварианты

- **Носитель — в `shared/…/feature/<group>/`**: `<X>Task : BackgroundRunner`, `<X>Action`,
  `<X>Widget{Snapshot,Provider}` — публичный класс с `internal`-конструктором, поднимается фабрикой
  `SharedContainer`. Правила — в фиче; обёртка (worker, Glance-виджет, приёмник, интент) только
  зовёт носитель.
- **Отказ возвращается, не бросается**: `BackgroundResult.Rejected(reason)`,
  `<X>Result.{SignedOut, Failed(error)}`. Повтор мутации — отдельная ветка исхода
  (идемпотентность), офлайн — честный `Failed`, сессия наружу не отдаётся.
- **Ключ задачи — её `slug`** (`const val ID = "<slug>"`; задача на конкретную запись — `SLUG` +
  `ID_PREFIX = "<slug>:"`, id записи в ключе). Тот же ключ — имя работы Android,
  `inputData["task_id"]` и `<bundle>.<slug>` в `BGTaskSchedulerPermittedIdentifiers`.
- **Точен только показ уведомления.** `timing: exact` — будильник ради уведомления (`Android`:
  `SCHEDULE_EXACT_ALARM`, без него — неточный); исполнение кода точнее окна не бывает, поэтому у
  каждой задачи есть `fallback`. `continuous` — foreground service с типом и `onTimeout`.
- **Виджет читает снимок, а не репозиторий**: `publish()` пишет `@Serializable` срез через
  `FileCache` в `<filesDir>/cache/widget-<slug>.json` (на iOS — контейнер App Group); снимок
  публикуют `WidgetUpdates` (on-app-use), фоновая задача и сами действия после мутации. `Stale` —
  по возрасту снимка **и** только без сети. ПДн и токенов в снимке нет.
- **Тап виджета — тот же https-адрес** (`tapUrl` снимка → `ACTION_VIEW`/`widgetURL` →
  `DeepLinkParser`); кнопки виджета и уведомления — записи `system-actions`, оба канала идут в один
  `<Slug>ActionHandler`.
- **Ответ пользователю — ресурс**: `core_platform_action_<slug>_*`, `widget_<slug>_*` в обоих
  `values{,-ru}`; имена — с префиксом модуля (Lint `ResourceName`), литералов нет.
- **iOS без `Shared` в расширении**: `Core/SystemActions` и `Core/Widgets` зависят только от
  `Pure`; контейнер приходит протоколом `SystemActionRunner` из `Platform/SystemActionsBridge`;
  вью — `Features/WidgetsFeature` (вне `featureNames`, без `Routing`/`SharedBridge`).
- **Тесты**: каждое состояние `WidgetState` — тест снимка в `commonTest` и эталон Roborazzi
  (`compose()` → `RemoteViews` → `Bitmap`); каждая ветка результата действия и задачи — тест;
  реестр AppFunctions на JVM не тестируется (Robolectric пинит `sdk=35`), в нём только
  делегирование под `@RequiresApi(36)`.

## Сигналы

- `capability-check` красный → ключ, ветка `backgroundRunner(id)`, идентификатор `Info.plist`,
  эталон/тест состояния или строка ответа разошлись с кодом `shared`.
- Виджет держится в `placeholder` → снимок ещё не опубликован.
