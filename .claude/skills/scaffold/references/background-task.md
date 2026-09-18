# scaffold › background-task — работа без пользователя на экране

Читает строку `background-tasks.md` карты (`slug · trigger (time|periodic|connectivity|
remote-event|user-started) · when · timing (exact|window|opportunistic|continuous) · runs ·
entities · conditions · result (notification:<slug>|widget:<slug>|fresh-data) · fallback ·
permissions · platforms · situation`). Фичи из `runs` и адресаты `result` существуют.

## Вопросы до начала (≤ 3)

1. `timing` → какой механизм по таблице `docs/stack/background.md`? `exact` означает точный
   **показ уведомления**, не точное исполнение кода; подтверждено?
2. Задача одна на приложение (`ID`, строка в `backgroundTasks`) или на конкретную запись
   (`ID_PREFIX` + id записи в ключе, планируется по действию пользователя)?
3. `result: notification:<slug>` — категория есть в `push.md` и есть парная запись
   `entry-points` `push` с `trigger: background:<slug>` (локальное уведомление, без бэкенда)?
   Новое разрешение (точные будильники, микрофон) — строка в `compliance.md` и
   `/scaffold permission`.

## Раскладка

Ядро `core/background` уже на месте — новая задача добавляет только свой раннер и строку в стек.

```
shared/…/core/background/BackgroundScheduler.kt   # interface BackgroundScheduler { fun schedule(task: BackgroundTask); fun cancel(id: String) } (не suspend) + interface BackgroundRunner { suspend fun run(): BackgroundResult }
shared/…/core/background/BackgroundTask.kt        # data class BackgroundTask(id, trigger, timing, conditions, periodMinutes, at) + enum BackgroundTrigger/BackgroundTiming/BackgroundCondition — зеркало enum'ов карты
shared/…/core/background/BackgroundResult.kt      # sealed: Notify(category, titleKey, bodyKey, args, deepLink, notificationId) | RefreshWidget(slug) | FreshData | Rejected(BackgroundRejection{Offline,SignedOut,NothingToDo,Failed})
shared/…/feature/<group>/<Slug>Task.kt            # internal class <Slug>Task(deps) : BackgroundRunner — оркестрация фич из runs; companion: const val ID = "<slug>" и val TASK (задача на запись — SLUG + ID_PREFIX = "<slug>:" и fun task(id, at))
shared/…/SharedContainer.kt                       # + строка в реестре backgroundTasks и ветка backgroundRunner(id); задача на конкретную запись — фабрика вида todoReminderTask(id, at)
shared/src/commonTest/…/feature/<Group>TasksTest.kt        # результат · каждая ветка Rejected · offline; расписание — через FakeBackgroundScheduler (образец — TodosTasksTest)
shared/src/commonTest/…/testing/Fakes.kt                   # FakeBackgroundScheduler (поле scheduled) уже есть
androidApp/core/platform/…/background/AndroidBackgroundScheduler.kt  # timing → WorkManager (Constraints из conditions) | точный будильник; Continuous планировщику не отдаётся
androidApp/core/platform/…/background/BackgroundTaskWorker.kt        # CoroutineWorker: inputData[task_id] → backgroundRunner(id).run() → deliver(); граф — Hilt EntryPoint, не @HiltWorker
androidApp/core/platform/…/background/BackgroundAlarmReceiver.kt     # будильник → тот же воркер
androidApp/core/platform/…/background/BackgroundDelivery.kt          # Notify → PushMessage/PushNotifier, Rejected(Failed) → Result.retry()
androidApp/core/platform/src/main/AndroidManifest.xml                # <receiver> и <uses-permission> по `permissions` записи (SCHEDULE_EXACT_ALARM; foreground service c типом при continuous)
androidApp/app/…/StitApplication.kt                                  # реестр планируется на старте: container.backgroundTasks.forEach(platform.background::schedule)
iosApp/Packages/Core/Sources/Platform/Background/{BgTaskScheduler,LocalNotifier,BackgroundDelivery}.swift  # механизм по timing: exact — локальное уведомление, periodic/opportunistic — app refresh, иначе processing; continuous и remote-event не планируются
iosApp/App/{AppEnvironment,AppDelegate}.swift, iosApp/App/Info.plist    # registerBackgroundTasks() на старте; BGTaskSchedulerPermittedIdentifiers (<bundle>.<slug>) — только задачам, которые ведёт BGTaskScheduler; UIBackgroundModes
docs/stack/background.md                                                # + строка задачи: триггер, timing, механизм per платформа, идентификаторы
```

Ключ — единственный: `const val ID = "<slug>"` (задача на конкретную запись — `SLUG` +
`ID_PREFIX = "<slug>:"`); `capability-check` принимает обе формы, но новый код пишется так.

## Шаги

1. Строка в `background.md` (+ `compliance.md` и `/scaffold permission` при новом разрешении).
2. RED: `<Group>TasksTest` — результат, каждая ветка `Rejected`, offline; затем `<Slug>Task` и
   строка в `backgroundTasks`/`backgroundRunner(id)`; `make api-dump` (реестр — часть фасада).
3. Android: механизм по `timing` уже в `AndroidBackgroundScheduler` — нужны только разрешения
   манифеста и, при `Notify`, ключи текста в `PushMessage.TEXTS`; Robolectric-тест планирования.
4. `result: notification:<slug>` → `/scaffold entry-point` (`push`, `trigger: background:<slug>`);
   `result: widget:<slug>` → обновление виджета в `:androidApp:widget`.
5. iOS (при `platforms ∋ ios`): планировщик, локальные уведомления, ключи `Info.plist` — атом
   «Mac-verify». `make check`.

## Anti-patterns

- ❌ Бизнес-правило в воркере: задача только запускает фичу из `runs`.
- ❌ Исключение из `run()`: отказ возвращается как `Rejected(<причина>)` — по `Failed` платформа
  просит повтор, по остальным причинам считает работу сделанной.
- ❌ `timing: exact` для исполнения кода (точен только показ уведомления); интервалы в минутах
   в артефактах карты (в `BackgroundTask.periodMinutes` они законны — это уже механизм).
- ❌ `continuous` без постоянного индикатора и без остановки пользователем; FGS вместо
  WorkManager под задачу, которой не нужен видимый ход.
- ❌ Задача без `fallback` — приложение обещает то, чего система не гарантирует.
- ❌ Серверная периодика (рассылки, расчёты) — это воркер `<org>/backend`, не устройство.
- ❌ Загрузка при открытом экране, названная фоном — это состояние `loading`.
