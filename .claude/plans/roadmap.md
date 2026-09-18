# Roadmap

Единственный артефакт планирования проекта — этот файл. Подпапок, `<slug>.md`,
архивов и changelog'ов нет: они расходятся с правдой и мешают.

**Planning-first.** Изменение идёт через план — секцию в `## Active` ниже.
Прямая правка без плана допустима только для truly-atomic (опечатка, один
очевидный файл) и багфиксов (→ skill `triage-failure`, RED-first). Отклонение
от плана — осознанное и проговаривается с юзером.

**Жизненный цикл.** `planning → in-progress → blocked? → done`. По `done` секция
задачи удаляется целиком — история остаётся в `git log`. Незакрытый follow-up
переезжает в backlog или новой Active-секцией, а не хвостом в удаляемой.
**Current focus:** — (активных задач нет; следующий шаг — dry-run обвязки из `## Backlog`,
затем фаза 5 на Mac)

## Active

## Backlog

- **Dry-run обвязки (A9 сессии 3, отложен владельцем):** две копии репо в
  `~/.cache/stit-mobile-research/dryrun/{both,android}` + сосед-strategy из
  `docs/reference/strategy-mobile-canon/`; в копиях `/init-service` (стадии 1–5) →
  `/stack-deep-dive` режим A на 14 kinds → `stable` → `/de-template` both и `--platform android`
  → `/scaffold` каждого варианта (включая `system-action`, `widget`, `background-task`); после
  каждого шага `make check` зелёный, `make gate-selftest` красный; расхождения чинятся в скиллах
  шаблона. Бриф — `docs/research/prompts/NEXT-SESSION-4.md`.
- **Mac-verify iOS-половины** (первый `make check-mac`): имена Obj-C-экспорта в `SharedBridge`
  (`Kotlinx_coroutines_coreStateFlow`, `KotlinBoolean`-боксинг suspend-результатов,
  `RouteTodoDetail`), `iosApp.xcodeproj` (локальные пакеты, per-env конфигурации и схемы
  Staging/Prod/Demo, UI-test target, wiring `Shared.framework` для пакетов, `Package.resolved`),
  snapshot-эталоны `__Snapshots__/`, `MemberImportVisibility`-импорты в `Core`/`Features`/`App`.
- **FCM `getToken()`/`onNewToken` deprecated в firebase-messaging 25.1** — перейти на
  `register()`/`onRegistered`, когда API стабилизируется; сейчас `@Suppress("DEPRECATION")`.
- **AppFunctions на устройстве** — прогнать реестр действий (`addTodo`) на Android 16+ с
  ассистентом, который его поддерживает: голосом и автоматизацией; по выходу stable-релиза
  `androidx.appfunctions` перейти на него и снять строку из
  `docs/stack/dependencies.md § Version policy exceptions`.
- **Срок задачи (`dueAt`) в контракте** — у reference-сущности `todo` поля срока нет, поэтому
  виджет показывает «обновлено», а `todo-reminder` планируется на выбранный пользователем момент.
  Добавить `dueAt` в `api/openapi.yaml` (BACKEND-HANDOFF) → `make gen` → сортировка виджета и
  напоминание по сроку.
- **Разделить в порте `WidgetRefresher` «перерисовать» и «опубликовать»** — сейчас
  `refresh(slug)` делает и то и другое, поэтому после системного действия снимок публикуется
  повторно (идемпотентно, но лишний раз); два метода уберут двойную публикацию.
- **Фаза 5 на Mac:** `make check-mac` (M1–M9), snapshot-тесты всех состояний, XCUITest smoke,
  верификация `iosApp.xcodeproj`, подпись, `mac-host.md` по факту. Поверхности вне экранов и demo:
  завести appex-таргет `iosAppWidgets` (`$(WIDGETS_BUNDLE_ID)`, entitlements, Embed) и App Group
  обоим таргетам; убедиться, что `Shared.framework` не попал в расширение; проверить имена
  Obj-C-экспорта новых типов (`BackgroundResult{Notify,RefreshWidget,Rejected}`,
  `CompleteTodoResult*`, `AddTodoResult*`, кейсы enum'ов,
  `TodayTodosWidgetProvider.companion.SLUG`) и concurrency-аннотации; API App Intents
  (локализованные описания и диалоги, извлечение `${applicationName}` — возможно нужен
  `AppShortcuts.xcstrings`); `make snapshot-record` (эталоны `widget-today-todos-*` и demo-вью);
  отдельный риск существующего кода — у старых файлов `Platform` нет `public import SharedBridge`,
  хотя Shared-типы стоят в public-сигнатурах: под `InternalImportsByDefault` не соберётся. До этого
  iOS помечен «не верифицирован на Mac», `/de-template` с `ios` требует `--ios-unverified`.
- **Фаза 7 — экосистема и первый релиз:** `wellknown` на dev-VPS, бэкенд с B-17/B-1…B-3,
  `/release --stop 1` demo-приложения → Gitea Release, `--stop 2` в RuStore и Play internal.
- **Cross-repo P0** (синтез §9, порядок обязателен): corp-канон C-1…C-3 (7-й шаблон, контракт
  artifact-in-release, Vaultwarden `mobile/<org>/<app>`, ntfy `releases-<org>`), бэкенд
  B-1…B-3 (Bearer + refresh JSON), B-12 (`make openapi` + `gen-check`), B-17
  (`GET /api/app/requirements`), инфра I-1 (`wellknown`) и I-6 (URL политики и удаления
  аккаунта), strategy S-1…S-3 (канон слоя `mobile/`), stit-public P-1 (последним). Шаблон
  работает с fallback'ами: `AuthMode.Cookie`, reference-спека, 404 на `requirements`, копия
  канона в `docs/reference/strategy-mobile-canon/`.
- **Спайки, требующие Mac или аккаунтов:** S-1 (SKIE + direct integration), S-10 (buildable
  folders + umbrella `Features`), S-15 (`exportArchive` без fastlane), S-12 (RuStore/Play API на
  throwaway-приложении).
- **Извлечение `mobilekit`/`swiftkit`** — по триггеру «≥ 2 форков в эксплуатации + первый фикс в
  `shared/…/core` или `Packages/Core`, нужный обоим» (08 §1.6).
- **`sharedUi/` (Compose Multiplatform)** — только по триггеру 03 §7.2 (стабильный CMP-iOS без
  замечаний App Review, паритет Liquid Glass).
- **`buildHealth` (dependency-analysis) в гейт** — когда плагин поддержит AGP 9.x (сейчас
  3.19.2 знает до 9.3.1; верб `deps-report` advisory).
- **Baseline Profile + Gradle Managed Devices** — нужны устройство/KVM и ≥ 10 ГБ RAM; шов —
  `androidApp/baselineprofile` в `docs/reference/release.md`.
- **SQLDelight-шов для offline-first проектов** — спайк компиляции на Linux без cinterop;
  до него persistence v1 = in-memory + `FileCache` + `Settings` + `SecureStore`.
- **SKIE для Swift-фасада** — подключить в `stit.kmp.library`, когда SKIE поддержит Kotlin 2.4.20+
  (0.10.14 ограничен 2.4.10); до этого фасад плоский (`kmp-public-api`), sealed → Swift через
  Obj-C export без enum-сахара.
- **Тёплый `make check` ≤ 15 с** — на живом демоне 20 с после волны 3 (6 дешёвых bash/Swift-стадий
  до Gradle + 15 Gradle-вызовов); варианты: объединить дешёвые Gradle-стадии (`spotlessCheck
  checkSortDependencies detekt lintDevDebug`) в один вызов с разбором per-task статусов или принять
  20 с как норму и закрыть строку.