---
id: stack.background
kind: background
name: background
status: draft
---

# background

Платформенная механика фоновой работы на устройстве. Карта (`background-tasks.md` в
`<org>/strategy`) говорит **что и насколько точно** нужно продукту; здесь — **чем это
исполняется** на каждой платформе, какие идентификаторы и разрешения это требует и какие лимиты
ОС нельзя обойти. Серверная периодика (рассылки, расчёты) живёт в `<org>/backend`, не здесь.

## Timing to mechanism

Единственная таблица выбора: строка карты (`trigger` + `timing`) → механизм.

| `trigger` · `timing` | Android | iOS |
|---|---|---|
| `periodic` · `window` \| `opportunistic` | WorkManager (`PeriodicWorkRequest`, `Constraints` из `conditions`) | фоновое обновление приложения (`BGAppRefreshTask`) |
| `connectivity` · `window` \| `opportunistic` | WorkManager (`NetworkType.CONNECTED`/`UNMETERED`) | `BGProcessingTask` (`requiresNetworkConnectivity`) |
| `time` · `exact` | точный будильник (`setExactAndAllowWhileIdle`) → тот же воркер — **ради показа уведомления**; без выданного `SCHEDULE_EXACT_ALARM` планировщик уходит на неточный будильник | локальное уведомление по календарному/интервальному триггеру (`UNUserNotificationCenter`) — исполнения кода к моменту нет |
| `remote-event` · `opportunistic` | data-message → WorkManager (не foreground service) | `content-available: 1` + background mode `remote-notification`; доставка не гарантирована и троттлится |
| `user-started` \| `continuous` | foreground service с типом `dataSync` и реализованным `onTimeout` (лимит 6 ч на 24 ч; исчерпан → старт запрещён) | background mode по случаю (аудио, геолокация) — отдельное решение владельца |

Точным по времени бывает **показ уведомления**, не исполнение кода. `fallback` каждой строки
карты — единственное обещание пользователю на случай, когда система не дала отработать.

## Tasks

Строка на каждую stable-запись `background-tasks.md` карты.

| задача (`slug`) | `trigger` · `timing` | механизм Android · iOS | `result` | разрешения |
|---|---|---|---|---|
| _`refresh-todos`_ | _`periodic` · `window` (условие `network`)_ | _WorkManager · фоновое обновление_ | _`widget:today-todos`_ | _—_ |
| _`todo-reminder`_ | _`time` · `exact`_ | _будильник → воркер · локальное уведомление_ | _`notification:todo-reminder`_ | _`exact-alarm`, `notifications`_ |

## Identifiers

| платформа | форма | где пин |
|---|---|---|
| общий ключ | `slug` записи карты (`refresh-todos`); задача на конкретную запись — `<slug>:<entityId>` (`todo-reminder:<todoId>`). По нему `SharedContainer.backgroundRunner(id)` поднимает исполнителя | `companion` раннера (`ID` / `ID_PREFIX`) |
| Android | тот же ключ: имя уникальной работы WorkManager, `extra` будильника и `inputData["task_id"]` воркера | `AndroidBackgroundScheduler`, `BackgroundTaskWorker.KEY_TASK_ID` |
| iOS | `<bundle-id>.<slug>` | `App/Info.plist` → `BGTaskSchedulerPermittedIdentifiers` |

Идентификаторы `Info.plist` и зарегистрированные в `AppDelegate` совпадают со списком задач —
сверяет стадия `capability-check`.

## Limits and permissions

| что | значение проекта |
|---|---|
| точные будильники | _только под обещанный пользователю момент: `SCHEDULE_EXACT_ALARM` (не `USE_EXACT_ALARM` — его магазины отдают будильникам и календарям); с Android 14 разрешение по умолчанию не выдано, планировщик проверяет его и уходит на неточный будильник_ |
| типы foreground service | _`dataSync` при `continuous`/`user-started`; иначе типов нет_ |
| iOS background modes | _`remote-notification` при `remote-event`; остальные — по решению_ |
| App Group | _`group.<bundle-id>` — общий контейнер снимков виджетов (`service.md`)_ |
| ограничения производителей | энергосбережение и «заморозка» приложений отменяют фон; приложение об этом не спорит, а исполняет `fallback` |

## Generates

- `shared/…/core/background/{BackgroundScheduler,BackgroundTask,BackgroundResult}.kt` (`BackgroundRunner` — рядом со `BackgroundScheduler`), `PlatformDeps.background`, реестр `backgroundTasks` и подъём `backgroundRunner(id)` в `SharedContainer`
- `androidApp/core/platform/…/background/{AndroidBackgroundScheduler,BackgroundTaskWorker,BackgroundAlarmReceiver,BackgroundDelivery}.kt` + `<receiver>` и `<uses-permission>` в `core/platform/src/main/AndroidManifest.xml`; планирование реестра — `androidApp/app/…/StitApplication.kt`
- `iosApp/Packages/Core/Sources/Platform/Background/{BgTaskScheduler,LocalNotifier}.swift`, регистрация в `App/AppDelegate.swift`
- `iosApp/App/Info.plist` — `BGTaskSchedulerPermittedIdentifiers`, `UIBackgroundModes`
- `gradle/libs.versions.toml` — `androidx.work` (`work-runtime-ktx`, `work-testing`); строка в `dependencies.md`
- строки `compliance.md` (точные будильники, тип foreground service, background modes) и локальные категории в `push.md`
- стадия `capability-check`: реестр задач ↔ идентификаторы ↔ тесты

## Открытые вопросы

- _что_
