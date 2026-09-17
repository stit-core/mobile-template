# Промпт сессии 3: волны 3–4 сборки `mobile-template`

Дата составления: 2026-09-17. Для основной сессии Claude Code (Fable) в
`/home/mm/git/stit-core/mobile-template/`. Сессия 2 закрыла волну 2 (A3–A5: `shared/`, обвязка и
docs); эта сессия ведёт волну 3 (B2 ∥ B4) и волну 4 (проверка, dry-run, отчёт) до Definition of Done.
Всё, что не переопределено здесь, берётся из `NEXT-SESSION.md` (оркестрация §2, промпты агентов
§3.4–§3.5, протокол проверки §4, стандарт качества §5, тесты §6, запреты §7, ловушки §8) и
`NEXT-SESSION-2.md` (§3 уроки оркестрации, §4 общий блок состояния для агентов, §5 команды проверки).

Требование владельца неизменно: **красиво, качественно, без замудрёностей — ровно, понятно,
читаемо; тесты очень важны.**

---

## 0. Как пользоваться этим файлом

1. Прочитать: `docs/research/DECISION.md` → `08-template-design.md` §2.1–§2.3, §2.6–§2.8, §4.1–§4.2,
   §4.7 (остальное — по ссылкам из промптов) → `NEXT-SESSION.md` целиком → `NEXT-SESSION-2.md` §3, §4
   (общая часть), §5 → этот файл → `.claude/plans/roadmap.md` (A1–A5 закрыты, A6–A10 открыты).
   Читать большие отчёты кусками ≤ 150 строк (`sed -n`); отчёты 01/02 (3479 и 2061 строка) целиком
   в основную сессию не грузить — агентам они назначены в промптах, основной сессии хватает
   §7/§8/R3–R10/ловушек 01 и §3/§5/§7/ловушек 02 (номера строк — `grep -n '^## \|^### '`).
2. Прочитать глазами обвязку и каркас: `CLAUDE.md` (191 строка), `docs/reference/architecture.md`,
   `Makefile`, `scripts/tools/check.sh`, `.gate-fixtures/NF-11/fixture.sh` (форма), и **код `shared/`**
   (в сессии 2 основная сессия успела прочитать всю обвязку B3, но не код B1 — это долг §4 п. 3):
   `shared/src/commonMain/kotlin/tech/stit/mobile/{SharedContainer,PlatformDeps}.kt`, по 2–3 файла
   из `core/api`, `core/auth`, `core/mock`, `feature/todos`, `feature/shell`, `entity/todo`,
   `scripts/tools/{gen,gen-check,mock-check}.sh`, `shared/api/jvm/shared.api` (фасад, 2612 строк —
   пробежать по именам). Замечания по читаемости — короткой задачей агенту до старта волны 3.
3. Прогнать `NEXT-SESSION-2.md` §5 целиком **включая `GATE_RELEASE=1 make check`** — R8 после волны 2
   не прогонялся (машина была в swap, сессия закрылась по контексту). Это обязательно до запуска B2:
   он гоняет Gradle, а два Gradle одновременно нельзя.
4. Дальше — по `NEXT-SESSION.md` §0 п. 3–5: волна 3 (B2 ∥ B4), волна 4. Промпты агентов —
   `NEXT-SESSION.md` §3.4–§3.5 дословно, блоки «Состояние репозитория на момент запуска» — §4 этого
   файла (готовы к вставке; дополнить цифрами п. 3).

## 1. Состояние репозитория после сессии 2

- **Коммиты** (`git log --oneline`): ресёрч → план → `feat(repo): каркас и Linux-гейт` →
  `meta(plan): чекпоинт волны 1 и промпт сессии 2` → `feat(shared): KMP-ядро — контракт API,
  презентеры, mock и тесты` (7cda56c, агент B1, проверено основной сессией) → `docs(harness): …`
  (агент B3 + сверка с кодом) → `meta(plan): чекпоинт волны 2 и промпт сессии 3`. Дерево чистое,
  ветка `main`, хуки установлены.
- **Гейт на этой машине** (основная сессия, после освобождения памяти): `make doctor` clean
  (0 warn); `make check` → 0 с SKIP-блоком macOS (5 стадий), тёплый **19 с** (со стартом демона),
  6 с на живом демоне до волны 2, холодный после правок B1 — 109 с; `make check-fast` 12 с;
  `make test` 18 с; `make gate-selftest` — **16 из 16 красных** (~2 мин), дерево после без изменений;
  покрытие `shared` **96,7 %** (без `generated/`), Android 100 % (заглушки); `GATE_RELEASE=1` —
  **не гонялся** после волны 2.
- **`shared/` (B1)** — 127 файлов commonMain (34 сгенерированных), 31 тестовый файл, 107 JVM-тестов
  + 4 host-теста:
  - `core/{config,lib,api,auth,deeplink,requirements,push,mock,observability,i18n}`,
    `entity/{user,session,todo,device}` (модель + репозиторий SSOT с `FileCache` + `<Slug>Mapper`),
    11 презентеров в `feature/{shell,requirements,auth,profile,todos,settings,push,demo}`
    (`feature/auth/{Login,Sessions,DeleteAccount}Presenter`, `feature/todos/{TodoList,TodoDetail}Presenter`
    и т. д.), `PlatformDeps`, `SharedContainer`.
  - Фасад (`shared/api/jvm/shared.api`, klib-дамп `shared/api/shared.klib.api`):
    `SharedContainer(platform: PlatformDeps, config: AppConfig)` с членами `config`, `sessionStore`,
    `deepLinkParser`, `requirements`, `pushRegistrar`, `userRepository`, `todoRepository`,
    `sessionRepository`, `deviceRepository`, `mockTransport?`, `demoMode?` и фабриками
    `shellPresenter()`, `requirementsPresenter()`, `loginPresenter()`, `sessionsPresenter()`,
    `deleteAccountPresenter()`, `profilePresenter()`, `todoListPresenter()`, `todoDetailPresenter(id)`,
    `settingsPresenter()`, `pushOptInPresenter()`, `demoPanelPresenter()`. Каждый презентер:
    `state: StateFlow<XState>` (data class), `dispatch(XIntent)` (sealed), `close()`. Ошибки —
    `MessageKey("error.<code>")`. Примеры: `ShellState(auth, online, route, pendingRoute)` /
    `ShellIntent.{Start, OpenLink(url), RouteConsumed}`; `LoginState(email, password, submitting,
    error, signedIn)` / `{EmailChanged, PasswordChanged, Submit}`; `TodoListState(todos, loading,
    error, offline, newTitle)` / `{Start, Refresh, TitleChanged, Create, Toggle, Delete}`;
    `SettingsState(theme, language, notificationsEnabled, version, build, env, links)`;
    `DemoPanelState(available, scenario, latencyMs)`.
  - **Как собирает платформа**: `AppConfig.parse(APP_ENV, API_BASE_URL, API_MODE, AUTH_MODE,
    DEEP_LINK_HOST, DEEP_LINK_SCHEME, SENTRY_DSN)` (ключи = `config/env/*.properties`);
    `PlatformDeps` = `appInfo: AppInfo(version, build, osVersion, deviceModel)`,
    `secureStore` (`KeystoreSecureStore(context)` / `KeychainSecureStore()`), `settings`
    (`DataStoreSettings(context)` / `UserDefaultsSettings()`; по одному экземпляру на процесс),
    `filesDir`, `pushTransports: List<PushTransport>`, `notificationPermission`, `telemetry`
    (`NoopTelemetrySink` / Sentry), `logSink` (`LogcatSink()` / `NSLogSink()`), `locale`,
    `connectivity: ConnectivityMonitor` (`StateFlow<Boolean>`). ViewModel: `init { presenter.dispatch(Start) }`,
    `onCleared { presenter.close() }`; deep link → `ShellIntent.OpenLink(url)`, после перехода —
    `RouteConsumed`.
  - **Mock/demo**: `mock/scenarios.yaml` + `mock/fixtures/{empty,populated,authenticated}/{user,login,
    todos,sessions,devices,requirements}.json`, `error/error.json`; в код попадают задачей
    `:shared:embedFixtures` (`build/generated`, не коммитится). `API_MODE=mock` (flavor `demo`) →
    движок платформы не создаётся, `mockTransport`/`demoMode` ≠ null; `ShellIntent.Start`
    восстанавливает сценарий из `Settings` (дефолт `populated`); login принимает любые непустые
    email/пароль; `authenticated` сеет refresh-токен; `error` → 500 `INTERNAL`; панель —
    `DemoPanelPresenter` (сценарий, задержка).
  - Контракт: `api/openapi.yaml` (дамп бэкенда + Bearer-ветка + `/api/app/requirements` +
    `/api/v1/devices` + todos как `/api/todos`; добавлено необязательное `UserView.version` — кандидат
    BACKEND-HANDOFF B-20) + `api/openapi.lock`; `make gen` (нужны `python3` + PyYAML — `make doctor`
    предупреждает) → после него `make api-dump`; `make gen-check`, `make mock-check`.
  - Осознанные отличия от 08: expect/actual только `currentPlatform` и `ioDispatcher`; `SecureStore`/
    `Settings` — интерфейсы через `PlatformDeps`; время — `kotlin.time.Instant`/`Clock` (без
    kotlinx-datetime); логи — `Logger` + `LogSink` (без Kermit); `CrashReporter` слит в
    `TelemetrySink`; Konsist-тест слоёв `shared` — `shared/src/jvmTest/.../ArchitectureTest.kt`
    (стадия `shared jvm tests`); `KeychainSecureStore` компилируется в klib, на устройстве не
    верифицирован.
- **Обвязка (B3)**: `CLAUDE.md` 191 строка; 23 правила `.claude/rules/*.md` (≤ 57 строк, frontmatter
  `name/description/paths:`; `forward-only.md` побайтно = Go); `.claude/settings.json` (08 §3.3 +
  allow на `validate-plan`/`links-check`/`kin-diff`); 9 скиллов + 17 reference-файлов
  (`stack-deep-dive/state/.gitkeep`); `.claude/agents/{explorer,reviewer}.md`; `docs/{README,
  getting-started,contributing}.md`, `docs/reference/{architecture,ecosystem,release,observability,
  distribution,strategy-mapping,mac-host,kinship}.md`, `docs/reference/strategy-mobile-canon/{README.md,
  _template/×9}`, `docs/stack/README.md` + `_templates/×15`; `README.md`, `LICENSE`, `SECURITY.md`;
  скрипты `scripts/tools/{validate-plan,links-check,kin-diff}.sh` (+ вербы `make validate-plan`,
  `links-check`, `kin-diff`), `status.sh` с секциями Platforms/Mac/Critical/Handoffs/Strategy,
  `pre-commit` с `validate-plan` при staged roadmap. Сверка docs ↔ код `shared` сделана коротким
  агентом после волны (имена презентеров, expect/actual, логгер, порядок стадий гейта).
- **Что вписать в docs после волны 3** (B3 перечислил места; вербы B2/B4 сейчас не документированы
  как существующие): `CLAUDE.md` §Команды и §Инварианты («Токены» — `tokens-check`);
  `docs/reference/architecture.md` §Гейт + таблица enforcement; `docs/contributing.md` таблица
  enforce; `docs/reference/release.md` §Вербы (`release-ios`, `release-publish`);
  `.claude/skills/release/SKILL.md` шаги 3/5/6 + `references/steps.md`; rules `design-tokens`
  (`tokens-check`), `deep-links-and-push` (`deeplinks-check`), `tests-pipeline`/`mock-and-fixtures`
  (`make e2e`); `scaffold/references/{flow,entry-point,screen}.md`; `_templates/{styling,deep-links}.md`
  §Generates; `docs/contributing.md` нумерация фикстур (NF-17…19 Android, NF-20…24 Swift).
- **Машина**: владелец разрешил закрыть python-`voiceinput` (≈ 1,9 ГБ) — он закрыт; `xray`,
  claude-desktop, чужой vite-сервер не трогать. При занятой машине `make doctor` FAIL по RAM < 4 ГБ
  — сначала освободить память (закрыть voiceinput, если снова запущен: `pgrep -af voiceinput`),
  чужие процессы без разрешения не убивать. Во время Gradle swap уходит почти в ноль
  (свободного swap 2–5 ГБ из 24) — один `./gradlew` за раз, `./gradlew --stop` между волнами.

- **К вниманию сессии 3 — расхождения кода `shared` со спекой 08** (нашёл агент сверки; не блокируют,
  решить при чтении кода в §0 п. 2 — либо поправить код, либо зафиксировать как осознанное отличие):
  1. `KeychainSecureStore` живёт в `iosMain` через `platform.Security` (08: `iosMain` — Foundation only,
     Keychain — Swift-реализация из `PlatformDeps`); компилируется в klib, на устройстве не проверен.
  2. Код и спека помечают `UserView.version` как B-19, docs — B-20 (в 07 B-19 = `oasdiff breaking`):
     привести код к B-20 через правку `api/openapi.yaml` (description) → `make gen` → `make api-dump`.
  3. Публичные `suspend`-функции репозиториев и `SessionStore` без `@Throws` — для Swift необработанный
     `ApiError` = crash; решить вместе с B4 (`SharedBridge`) или добавить `@Throws(ApiError::class)`.
  4. `DemoMode`/debug-панель доступны только при `API_MODE=mock` (спека: панель в dev/staging +
     runtime-переключатель по `requirements.flags.demo_login`) — B2 учитывает как есть.
  5. `entity/user`/`UsersApi` нужны `shell`/`auth` — из prune-листа `profile` исключены (docs правлены).
  6. `ApiClient` не шлёт `User-Agent`/`Accept-Language`, `AppApi` не передаёт `store` — дополнить при
     чтении кода (тест на заголовки уже есть в `ApiClientTest`).

## 2. Что осталось (Definition of Done — `NEXT-SESSION.md` §1)

- **Волна 3:** B2 — `androidApp/` с 8 фичами, Nav3, Hilt, `demo`-flavor, `:lint`-детектор, Konsist,
  Roborazzi на все состояния, `i18n/deeplinks/tokens-check`, `design/tokens.json` + генератор
  `Tokens.kt`/`Tokens.swift`, NF-17…19, `app-id.sh`, AppCompat для per-app locales; B4 — `iosApp/`
  Linux-часть (`Packages/Pure` с тестами и покрытием ≥ 70 %, SwiftLint/swift-format, `Tools/archlint`,
  xcconfig/test plans/PrivacyInfo, каркас `Core`/`Features` по канону 02 без компиляции, NF-20…24,
  Swift-стадии в `check.sh`). Коммиты `feat(android): …`, `feat(ios): …`; после волны — сверка docs
  с кодом Android/iOS (короткий агент, как после волны 2) и вписать вербы B2/B4 в docs (п. 1).
- **Волна 4:** протокол §4 целиком, dry-run обвязки на scratch-копии (`/init-service →
  /stack-deep-dive (A) → /de-template` both и `--platform android`; расхождения чинятся в скиллах),
  `roadmap.md` (закрытое удалить, незакрытое — в `## Backlog`), отчёт владельцу.
- **Не входит** (уже в `## Backlog`): фаза 5 на Mac, фаза 7, спайки S-1/S-10/S-15/S-12, cross-repo P0,
  SKIE.

## 3. Уроки сессии 2 (дополняют `NEXT-SESSION-2.md` §3)

- **Агенты без `model`** работают стабильно: B1 — 66 мин, 78 вызовов, 636 K токенов; B3 — 50 мин,
  176 вызовов, 822 K токенов; обрывов API не было. Два агента правили `Makefile`/`check.sh` без
  конфликтов, потому что B3 было явно сказано их не трогать, а B1 правил точечно.
- **Docs, написанные по спецификации до кода, расходятся с кодом** — после каждой волны, где есть
  и код, и docs, запускать короткого агента «сверка docs ↔ код» (промпт-образец — сессия 2: список
  реальных публичных имён из кода → grep по docs/rules/skills → правка по коду, forward-only,
  `links-check`/`validate-plan` зелёные, код не трогать). После волны 3 — обязательно (Android/iOS
  имена в rules `android-ui`, `ios-ui`, `navigation`, `design-tokens`, `strategy-mapping.md`).
- **Фикстуры самотеста перепроверять после каждой волны**: NF-10 позеленела, когда покрытие выросло
  до 96,7 % (удаление одного маленького теста перестало ронять пол) — перенацелена на удаление
  каталогов `commonTest/{feature,entity}` (покрытие падает до 59 %). После B2 то же может случиться
  с NF-09/NF-06 (перестройка токенов) — B2 обязан прогнать все 10 своих фикстур.
- **Контекст основной сессии** — сессия 2 закончилась по контексту, не по работе: не читать все
  109 файлов обвязки самой основной сессии (§4 п. 3 просит выборочно 3–5 файлов на зону; полную
  сверку — агенту), большие отчёты читать по разделам, проверки формулировать скриптами
  (`verify-wave.sh` в scratchpad сессии 2: doctor → check ×2 → check-fast → gate-selftest → stop;
  режим `release` добавляет R8 — команды в `NEXT-SESSION-2.md` §5).
- **Тёплый `make check` ≤ 15 с достигнут** на живом демоне (6 с до волны 2, 19 с со стартом демона
  после) — backlog-строку про объединение стадий можно закрыть, если после волны 3 цифра на живом
  демоне остаётся ≤ 15 с.

## 4. Блоки «Состояние репозитория на момент запуска» для B2 и B4

Общая часть — `NEXT-SESSION-2.md` §4 (тулчейны, env в каждой команде, один Gradle, scratchpad,
roadmap не трогать, агенты не коммитят) с поправками: scratchpad этой сессии (свой путь), машина —
свободно 3–7 ГБ и swap занят, Bash-таймаут ≤ 600000 мс (Gradle в foreground, длинные цепочки по
стадиям, промежуточных «жду завершения» не делать — финальный ответ только по Done), транскрипты
других агентов не читать. Плюс п. 1 этого файла как факты про `shared/` и обвязку.

### 4.1 B2 (`androidApp/`, волна 3, Gradle-агент)

- `git`: ветка `main`, коммиты п. 1; дерево чистое; что зелёное — п. 1. Твоя зона: `androidApp/**`,
  `config/{detekt,lint,env}/**`, `scripts/tools/{i18n-check,deeplinks-check,tokens-check,app-id}.sh`,
  `design/tokens.json`, `.gate-fixtures/NF-01…10` (перепроверить) и NF-17…19, `e2e/android/**`.
  Общие файлы точечно и с отчётом: `Makefile` (группы LOOP/GATE — `build` даёт `app-dev-debug.apk`
  и `app-demo-debug.apk`; `gen-tokens`, `run-android` через `app-id.sh`), `scripts/tools/check.sh`
  (стадии `tokens-check`, `i18n-check`, `deeplinks-check` — в `stage()` и `STAGES`, дешёвые до
  Gradle-стадий), `build-logic/**` (manifestPlaceholders `DEEP_LINK_HOST`/`DEEP_LINK_SCHEME`,
  генератор токенов), `settings.gradle.kts` (репозиторий VK для RuStore SDK с content-фильтром —
  только если резолвится), `gradle/libs.versions.toml`. Перед каждой правкой общего файла —
  перечитать: параллельно B4 точечно правит `Makefile` (группа `##@ SWIFT`) и `check.sh`
  (Swift-стадии).
- Что даёт `shared/` — п. 1 (фасад, `AppConfig.parse`, `PlatformDeps`, VM-лайфцикл, demo). Правила
  B3, на которые ссылаться: `android-ui`, `navigation`, `design-tokens`, `tests-pipeline`,
  `deep-links-and-push`, `mock-and-fixtures`, `env-and-config`, `gradle-build`; скиллы `/scaffold`
  (`references/screen.md` — раскладка `<Key>Route/Screen/ViewModel/NavEntry/Previews`), `/de-template`
  (`references/prune.md` — что должно легко удаляться).
- Каркас, который продолжаешь: модули с `build.gradle.kts` и namespace уже есть; `stit.android.feature`
  сам подключает shared/designsystem/ui/navigation/platform + hilt/lifecycle/nav3 + `core:testing`;
  `core:testing` экспортирует compose-ui-test, junit4, coroutines-test, robolectric, turbine;
  `Theme.kt` — `StitTheme`/`StitSpacing`/`StitSemanticColors`, `LocalStitSpacing`/`LocalStitColors`
  (единственные CompositionLocal в allowlist detekt `CompositionLocalAllowlist` — строка через
  запятую; новые локали добавляй туда); Roborazzi-форма — `@GraphicsMode(NATIVE)`,
  `@Config(qualifiers = "w360dp-h640dp-xhdpi")`, `RoborazziRule(outputDirectoryPath = "src/test/screenshots")`,
  `captureRoboImage("src/test/screenshots/<name>.png")`, verify `verifyRoborazziDevDebug`, запись —
  `make screenshots-record`; `robolectric.properties` sdk=35 (не менять без перезаписи эталонов);
  Konsist `ArchitectureTest` в `app/src/test` (4 правила; расширяй, имена по-русски в бэктиках);
  Kover-агрегат `gate` в `:androidApp:app` (пол 70, исключения Hilt/Preview/NavEntry).
- Фикстуры NF-01…10 ссылаются на конкретные файлы/строки (`Theme.kt` цвет `0xFF1B7F3B` в NF-09,
  `core/ui/.../BadLogging.kt`, `feature/settings/build.gradle.kts` и т. д.) — после реструктуризации
  перепроверь все 10 и поправь `TOUCHES`/`apply`; NF-17…19 (i18n/deeplinks/tokens) — той же формы.
- Токены: `design/tokens.json` (DTCG) → детерминированный `make gen-tokens` пишет
  `androidApp/core/designsystem/.../Tokens.kt` **и** `iosApp/Packages/Pure/Sources/DesignSystemTokens/Tokens.swift`
  (Foundation-only: `public enum Tokens` с вложенными enum по категориям, цвета `UInt32` ARGB
  light/dark, размеры `Double`, без `import SwiftUI`); B4 создаёт пакет `Pure` и таргет
  `DesignSystemTokens`, рассчитывая на этот файл и форму — не переименовывать. `tokens-check` =
  регенерация + `git diff --exit-code` по обоим файлам.
- i18n: `i18n-check.sh` сверяет `values/strings.xml` ↔ `values-ru/` ↔ реестр `MessageKey` из
  shared ↔ `iosApp/**/Localizable.xcstrings` (если есть; отсутствие = SKIP-строка). Per-app locales
  при minSdk 26 — AppCompat (`AppCompatActivity`, `Theme.AppCompat.DayNight.NoActionBar`,
  `AppLocalesMetadataHolderService`, `generateLocaleConfig = true` + `resources.properties`; свой
  `locale_config.xml` не держать — 01 §3.5). Deep links: intent-filter `autoVerify`, host/scheme из
  `config/env`; `deeplinks-check.sh` — манифест ↔ `config/env` ↔ тесты `DeepLinkParser`;
  `docs/stack/deep-links.md` читается, если существует.
- Push/observability SDK: `FcmTransport` (firebase-messaging с google()), `SentryCrashReporter`
  (sentry-android с mavenCentral; пустой DSN = выключено; без `google-services.json` — ручная
  `FirebaseOptions` или явный no-op) — только за интерфейсами `PlatformDeps`; `RuStoreTransport`
  требует репозиторий VK (`artifactory-external.vkpartner.ru`, content-фильтр `ru.rustore.*`) —
  подключать только если резолвится с этой машины, иначе заглушка за тем же интерфейсом с записью в
  финальном ответе (правило 25 минут на тупик).
- Swift-стадии (`format swift`, `swiftlint`, `archlint`, `swift pure tests`, `coverage swift`)
  принадлежат B4, который работает параллельно: их красный цвет во время твоей работы — не твоя
  проблема, не чини; итоговый `make check` сверит основная сессия.
- В финальном ответе: список экранов × состояний со скриншотами, покрытие по модулям, времена
  гейта, правки общих файлов, что не удалось, точные команды с env.

### 4.2 B4 (`iosApp/`, волна 3, без Gradle)

- `git` и что зелёное — п. 1. Твоя зона: `iosApp/**`, `e2e/ios/**`, `scripts/tools/swift-step.sh`,
  `.gate-fixtures/NF-20…24`. Точечно и с отчётом: `Makefile` (группа `##@ SWIFT (Linux-часть)` —
  `swift-lint`, `swift-test` по 08 §4.1; `IOS_SOURCES`/`fmt`/`fmt-check`/`cover-check` уже ждут
  `iosApp/`), `scripts/tools/check.sh` (стадии `swiftlint`, `archlint`, `swift pure build+tests`;
  `format swift` и `coverage swift` уже есть/вызываются). Перечитывать перед правкой — параллельно
  B2 точечно правит те же файлы (Android-стадии). Gradle не запускай; `bash scripts/tools/check.sh
  --only "<swift-стадия>"` — можно.
- Семя — `docs/research/spikes/swift-linux/` (`Package.swift` tools 6.2 с swift-openapi плагином —
  плагин и `ApiClient` **не переносить**: в В2 сеть только через `Shared`; `.swiftlint.yml` — с
  `trailing_comma` выключенным; `.swift-format` — отступ **2 пробела** (`.editorconfig`, 02 §7.5)).
- Как только появится каталог `iosApp/`, Swift-стадии оживают: `swift-step.sh` перестаёт печатать
  SKIP; `Makefile`/`check.sh` уже вызывают `swift format lint --strict --recursive iosApp/App
  iosApp/Packages iosApp/Tools`, `coverage-floor.sh iosApp/Packages/Pure 70`; `mac.sh` ждёт
  `iosApp/iosApp.xcodeproj`, схему `Dev`, планы `Unit`/`Snapshot`/`UI`, `iosApp/.test-simulator`
  (`platform=iOS Simulator,name=iPhone 17,OS=27.0`). Значит **весь** Swift-код, включая `Core`/
  `Features` (не компилируемые на Linux), обязан проходить swift-format lint и SwiftLint.
- `Packages/Pure`: `AppFoundation` (парсинг `AppConfig` из словаря ключей `Info.plist` — те же
  ключи, что `config/env/*.properties`; `Env`; форматтеры; `LogSink` над `os.Logger`) и
  `DesignSystemTokens` (`Tokens.swift` генерирует B2 — см. 4.1; создай каталог и таргет; если файла
  ещё нет — временный с пометкой в финальном ответе; тесты токенов — инварианты: контраст ≥ 4.5:1,
  ненулевые размеры, не конкретные значения). Swift Testing, `swift test --enable-code-coverage`,
  ≥ 70 % (`coverage-floor.sh`).
- `Tools/archlint` — Swift CLI над `swift package dump-package` для `Packages/{Core,Features,Pure,
  TestSupport}`: Features ↛ Features/Persistence/Platform; Core ↛ Features; Pure — только Foundation;
  ацикличность; `swift run --package-path iosApp/Tools/archlint archlint iosApp/Packages`.
- `Config/{Base,Dev,Staging,Prod,Demo}.xcconfig` + `Local.xcconfig.example` (`https:/$()/host`,
  `PRODUCT_BUNDLE_IDENTIFIER = tech.stit.mobile$(BUNDLE_ID_SUFFIX)`, флаги Swift 6 mode +
  approachable concurrency — 02 §5.4); `App/{iOSApp.swift, AppEnvironment.swift, RootView.swift,
  Info.plist, PrivacyInfo.xcprivacy (UserDefaults CA92.1, tracking = false), App.entitlements}`;
  `TestPlans/{Unit,Snapshot,UI}.xctestplan`; `scripts/embed-shared.sh`.
- `iosApp.xcodeproj` — только копией из `Kotlin/KMP-App-Template-Native` (`git clone --depth 1` в
  scratchpad; сети нет — не создавать и сообщить; `.pbxproj` руками сверх замены идентификаторов
  не править).
- `Packages/Core/{DesignSystem,Routing,Persistence,Platform,SharedBridge}`, `Packages/Features/{Shell,
  AuthFeature,ProfileFeature,TodosFeature,SettingsFeature}` + umbrella `Features`, `TestSupport` —
  по канону 02/08 против **реального** фасада shared (п. 1; `shared/api/shared.klib.api`): тонкие
  `@Observable`-адаптеры над презентерами (`state: StateFlow` → `for await` через Obj-C export без
  SKIE: `FlowCollector`-мост в `SharedBridge`), `Router` с типизированным `[Route]`; Swift-реализация
  `PlatformDeps` (`KeychainSecureStore()`, `UserDefaultsSettings()`, `NSLogSink()` из фреймворка +
  Swift `ApnsRegistrar`); snapshot-тесты — заготовки. Всё это — «не верифицировано на Mac».
- Фикстуры NF-20…24 (`EXPECT`/`TOUCHES`/`apply`): `print(` (SwiftLint custom), `import Combine`,
  Feature→Feature импорт (SwiftLint custom + archlint), `URLSession(` вне `SharedBridge`,
  `import UIKit` в `Pure` (`swift build` на Linux) или `@preconcurrency` без комментария — каждая
  красная в своей стадии.
- Правила B3, на которые ссылаться: `ios-ui`, `swift-concurrency`, `xcode-project`, `navigation`,
  `design-tokens`, `tests-pipeline`; `docs/reference/mac-host.md` (B3 написал — не перезаписывать,
  блок для него — в финальном ответе).
- В финальном ответе: что именно требует Mac; правки общих файлов; команды с env.

## 5. Протокол проверки основной сессией

Команды — `NEXT-SESSION-2.md` §5 (env + `make doctor && make check && make check && make check-fast
&& GATE_RELEASE=1 make check && make gate-selftest && git status --short && ./gradlew --stop && free -g`).
`GATE_RELEASE=1` — один раз до волны 3 (долг сессии 2) и один раз после неё. Дальше —
`NEXT-SESSION.md` §4 п. 2–7. Волна 4, dry-run: копия репозитория в scratchpad (`git clone
/home/mm/git/stit-core/mobile-template <scratch>/dryrun`), в копии — по `SKILL.md` скиллов:
`/init-service` стадии 1–5 с reference-значениями (роль архитектора играет основная сессия:
дефолты шаблона), `/stack-deep-dive` режим A на все 13 kinds → `status: stable` (в dry-run
промоушн допустим), `/de-template` both и затем на второй копии `--platform android`; после каждого
шага в копии — `make check` зелёный, `make gate-selftest` красный; `/scaffold` каждого варианта даёт
компилируемый код. Расхождения чинятся в скиллах шаблона, не в копии.
