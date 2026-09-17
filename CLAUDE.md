# mobile-template — гайд для агентов

Шаблон мобильного приложения: KMP-ядро `shared/` (Kotlin 2.4, Ktor 3.6,
kotlinx-serialization, презентеры на `StateFlow`) + нативный UI — `androidApp/`
(AGP 9.4, Compose BOM 2026.09, Material 3, Navigation 3, Hilt) и `iosApp/`
(Xcode 27, Swift 6.4, SwiftUI, `@Observable`, SwiftPM local packages).
Гейт: Spotless/ktlint + detekt + Android Lint + Robolectric/Roborazzi + Kover +
Konsist; swift-format + SwiftLint + archlint + `swift test` чистых пакетов; паритет
токенов/строк/deep links; macOS-стадии — по SSH (`make check-mac`).

Этот файл — **ядро (always-on) + роутер**. Детали — в `docs/`, контекстные
правила — в `.claude/rules/` (path-scoped), процедуры — в `.claude/skills/`.
Карта документации — `docs/README.md`.

## Где это в экосистеме

- **Роль:** мобильный клиент проекта `<org>` (surface `type: mobile` в
  `<org>/strategy`); один репо на обе платформы; STOP — подписанные артефакты
  в Gitea Release на теге (`docs/reference/release.md`).
- **Зависит от:** `<org>/backend` (Bearer-auth, `api/openapi.yaml`,
  `/api/app/requirements`, `/api/v1/devices`); `<org>/infra` (сервис `wellknown`
  для deep links, `SENTRY_DSN`); `<org>/strategy` (карта `mobile/<surface>/`);
  corp-контур — Gitea (Release), Vaultwarden (`mobile/<org>/<app>`),
  ntfy (`releases-<org>`), GlitchTip/Sentry.
- **Используется:** магазинами (RuStore, Google Play, App Store) и пользователями.
  В оркестрацию `<org>/infra` не входит — образа и compose-сервиса у него нет.
- Полная карта — `docs/reference/ecosystem.md`.

## Точка входа

- **`/smartdev`** — cockpit сессии: `make doctor` → `make status` (ветка,
  платформы, Mac-хост, дрейф API-спеки, активный план, гейты) + переключение
  в режим. Запускай в начале / после `/clear`.
- **Потребителю шаблона** — `docs/getting-started.md`: форк → `/init-service` →
  проектирование (`/stack-deep-dive` здесь; карта `mobile/<surface>/` — в
  `<org>/strategy`) → `/de-template` → свой проект.
- **Planning-first.** Содержательное изменение — через секцию в
  `.claude/plans/roadmap.md` (skill `/plan`). Прямая правка — только
  truly-atomic и багфиксы (`/triage-failure`, RED-first).

## Проектирование первично, код вторичен

Карта приложения проектируется в соседнем `<org>/strategy`: слой
`docs/projects/<slug>/mobile/<surface>/` — 9 kinds (entities · screens · flows ·
features · shell · entry-points · permissions · shared-api · shared-lib); там её
ведёт `/mobile-deep-dive`. Этот репо её **читает** (cross-repo, read-only):
`/scaffold` и `/de-template` разворачивают stable-записи в код и сами выводят
пути (`shared/…/feature/<group>/<Slug>Presenter.kt`, `androidApp/feature/<group>/`,
`iosApp/Packages/Features/Sources/<Group>Feature/`) из kind + slug. Артефакты
платформы — локальные `docs/stack/` (13 kinds + 2 условных). Код пишется по
артефактам, не наоборот. Reference-фичи `demo`, `shell`, `requirements`, `auth`,
`profile`, `todos`, `settings`, `push` — учебные образцы
(`docs/reference/architecture.md#reference`); `profile`/`todos` удаляет `/de-template`.

Резолв соседей — sibling-checkout: поля `project-slug` / `strategy-path`
(дефолт `../strategy`) / `backend-path` (дефолт `../backend`) / `surface` /
`platforms` / `preset` в `docs/stack/service.md`. Соседа нет — скиллы печатают
next-step, не падают.

## Команды

Единый интерфейс — `make` (`make help` — группы). Шаги печатают `OK:`/`FAIL:`
одной строкой, подробности — `build/gate-logs/<step>.log`.

| команда | что делает |
|---|---|
| `make doctor` | preflight: JDK 21, Android SDK 37, Swift 6.4, RAM ≥ 6 ГБ, Mac-хост (если задан) |
| `make fmt` | автоформат Kotlin (Spotless/ktlint + порядок зависимостей) + Swift (swift-format) — единственные автофиксеры |
| `make test` | JVM-тесты: `shared` (jvmTest, androidHostTest), Android (Robolectric, Konsist), lint-детекторы, Swift `Pure` |
| `make screenshots` / `screenshots-record` | Roborazzi verify / перезапись эталонов (осознанно) |
| `make kmp-check` | `shared` → klib под iOS-таргеты + проверка каталога (SKIPPED ≠ OK) + запрет cinterop |
| `make api-check` / `api-dump` | сверка публичного API `shared` с дампами / обновление дампов (решение в плане) |
| `make gen` / `gen-check` / `mock-check` | модели из `api/openapi.yaml` (+ `openapi.lock`; нужны `OPENAPI_GENERATOR_JAR`, `python3` + PyYAML; затем `make api-dump`) / проверка дрейфа / фикстуры ↔ схема |
| `make gen-tokens` / `tokens-check` | `design/tokens.json` → `Tokens.kt` + `Tokens.swift` (коммитятся) / паритет: регенерация во temp == дерево |
| `make i18n-check` / `deeplinks-check` | `values/` == `values-ru/` + `error.<code>` → `core_ui_error_*` (iOS xcstrings — WARN) / манифест ↔ `config/env` ↔ `DeepLinkParserTest` ↔ `deep-links.md` |
| `make swift-lint` / `swift-test` | SwiftLint `--strict` + archlint (граф `Package.swift`) / `swift test` `Pure` + пол 70 % |
| `make check` | **gate** (Linux-полный): Swift-стадии → паритет (tokens/i18n/deeplinks) → контракт → detekt/Lint/тесты/Kover → kmp/api → assembleDevDebug; порядок — `docs/reference/architecture.md#гейт` |
| `make check-fast` | Swift-формат + SwiftLint + Kotlin-формат + detekt + `shared` jvmTest — цикл «пока пишу» |
| `make check-mac` | macOS-стадии по SSH (`MAC_HOST`): link framework, iosSimulatorArm64Test, xcodebuild, планы Unit + Snapshot |
| `make check-full` | `check` + `check-mac`; обязателен перед `/release` |
| `make gate-selftest` | негативные фикстуры `.gate-fixtures/`: гейт обязан краснеть |
| `make build` / `run-android` / `run-ios` / `e2e` | dev+demo debug APK / devDebug на эмулятор (id — `scripts/tools/app-id.sh`) / (Mac) симулятор / Maestro против demo-сборки (`P=android\|ios`) |
| `make release-preflight` / `release-android` | см. `docs/reference/release.md`; оркеструет `/release` |
| `make status` | сводка для `/smartdev` |

Перед claim «готово» / push — **`make check`** (его же гоняет `pre-push`, с
`GATE_RELEASE=1`). «Готово по iOS» — только после `make check-mac`: Linux-гейт
явно печатает, какие стадии не выполнил.

## Карта: где что

| Тема | Где |
|---|---|
| Архитектура `shared/` ↔ `androidApp/` ↔ `iosApp/`, матрица зависимостей | `docs/reference/architecture.md` + rules `kmp-shared`, `android-ui`, `ios-ui` |
| Фасад Kotlin → Swift (плоский, `apiDump`) | rule `kmp-public-api` |
| Навигация и deep links | rules `navigation`, `deep-links-and-push` |
| Экосистема (шаблоны, контракты, Gitea Release как STOP) | `docs/reference/ecosystem.md` |
| Релиз, версии, магазины, Mac-хост | `docs/reference/release.md`, `distribution.md`, `mac-host.md` |
| Observability (Sentry/GlitchTip, `Logger`/`LogSink`, `os.Logger`) | `docs/reference/observability.md` + rule `observability` |
| Тесты (JVM, Robolectric, Roborazzi, Swift Testing, snapshot, Maestro) | rule `tests-pipeline` |
| Токены дизайна (`design/tokens.json`, паритет платформ) | rule `design-tokens` |
| env, флейворы, xcconfig, секреты, сессия | rules `env-and-config`, `signing-and-secrets`, `auth-and-session` |
| API-контракт, кодоген, mock-режим | rules `api-contract`, `mock-and-fixtures` |
| Магазины и право | rules `store-compliance`, `privacy-and-law` |
| Карта приложения (9 kinds, cross-repo) | `../strategy/docs/projects/<slug>/mobile/<surface>/` (копия канона — `docs/reference/strategy-mobile-canon/`); маппинг в код — `docs/reference/strategy-mapping.md` |
| Stack-артефакты (13 + 2 kinds) | `docs/stack/README.md` |
| Устройство `.claude`-системы, кто что enforce'ит | `docs/contributing.md` |
| Forward-only стиль | rule `forward-only` |

## Инварианты (что в 2026 иначе, чем агент «помнит»)

- **AGP 9.4 со встроенным Kotlin**: без `org.jetbrains.kotlin.android`, без
  kapt (только KSP), `kotlin { compilerOptions }`, `targetSdk` задаётся явно;
  convention-плагины `stit.*` (`build-logic/`) — единственное место конфигурации
  Android/KMP; версии и SDK-уровни — только `gradle/libs.versions.toml`
- **Navigation 3**: `NavKey` + `rememberNavBackStack` + `NavDisplay`; без
  `NavHost`/`NavController`
- **Compose test API v2** (`androidx.compose.ui.test.junit4.v2`); Material 3
  без Expressive; `hiltViewModel` из `androidx.hilt.lifecycle.viewmodel.compose`
- **`shared/` — чистый KMP**: platform API в `commonMain` нет; cinterop и
  CocoaPods нет; `expect/actual` — только `currentPlatform`/`ioDispatcher`/
  `platformHttpEngine`; `iosMain` — Foundation + Security (Keychain); остальное —
  Swift-реализации интерфейсов `PlatformDeps`; логика тестируется на JVM (`jvmTest`)
- **Фасад для Swift плоский**: final-классы, data/sealed, `suspend`/`StateFlow`,
  без generic'ов в публичных сигнатурах; `apiDump` (JVM + klib) коммитится
- **Swift 6 mode + approachable concurrency**: `MainActor` по умолчанию,
  предупреждения = ошибки, без Combine/TCA; `@preconcurrency` — только с
  комментарием-причиной
- **Liquid Glass / iOS 27 SDK**: системные компоненты; `PrivacyInfo.xcprivacy`
  обязателен
- **Один автофиксер на язык**: Spotless (ktlint 1.8) и swift-format;
  detekt-formatting и SwiftFormat не подключаются; baseline'ов lint/detekt нет
- **Токены**: `design/tokens.json` → `make gen-tokens` → `Tokens.kt`/`Tokens.swift`
  (коммитятся, `tokens-check` сверяет); в UI — только `StitTheme`/`theme`; сырой `0xFF…`
  — detekt `MagicNumber` + Lint `StitRawDesignValue`, в Swift — SwiftLint
- **Секреты**: токены только в `SecureStore` (Keystore/Keychain), access — в
  памяти; `security-crypto` запрещён; ключи подписи — в Vaultwarden, не в репо
- **Сеть — только через `shared`**: `URLSession`/`OkHttp` напрямую в фичах запрещены
- **Комментарии** — только WHY (rule `comments`); **forward-only** (rule `forward-only`)

## Git workflow

- **Conventional Commits**: `feat|fix|refactor|docs|test|chore|meta|perf|style`,
  `<type>(<scope>)?: <subject>`, subject ≤ 72
- Push в `main` разрешён (прямой или через PR); линейная история
- Хуки (`scripts/git-hooks/`, ставятся `make setup`): `commit-msg` — формат;
  `pre-commit` — секреты подписи, Spotless/detekt по staged, `validate-plan`
  для roadmap; `pre-push` — `GATE_RELEASE=1 make check` (пропуск —
  `SKIP_PREPUSH=1`, не `--no-verify`)
- Теги `vX.Y.Z` — только через `/release` (`versionCode` / `CFBundleVersion`
  выводятся из тега)

## Границы

### Делать всегда
- Перед claim «готово» — `make check`; «готово по iOS» — `make check-mac`
- Изменение — через план (skill `/plan`)
- Новая логика — в `shared/` (презентер + тест на JVM), UI — тонкий;
  cross-feature — через `entity`/`core`
- Новый endpoint/поле — сначала `api/openapi.yaml` (`make gen` → `make api-dump`);
  бэкенд отстаёт — BACKEND-HANDOFF
- При правке `docs/stack/*.md` — следить за `status: stable` и `## Generates`;
  карта `mobile/` правится только в `<org>/strategy`
- Правка deep-link-хостов, push-провайдеров, `SENTRY_DSN`, min-version —
  INFRA-HANDOFF (rule `infra-impact`, skill `/infra-handoff`)

### Сначала спросить
- Новая зависимость / SDK (запись в `docs/stack/dependencies.md` +
  `compliance.md` — privacy manifest)
- Изменение версий Kotlin / AGP / Compose BOM / Xcode (`.xcode-version`)
- Правка permissions в `AndroidManifest.xml`, usage-descriptions в `Info.plist`,
  entitlements
- Удаление reference-фич вручную (это работа `/de-template`)
- Любой `--no-verify`; любой `make release-*`; `make screenshots-record`

### Не делать никогда
- `--no-verify` без явного разрешения
- Коммитить `*.jks`, `*.p12`, `*.p8`, `*.mobileprovision`, `local.properties`,
  `Local.xcconfig`, `.env.local`
- Править `*.pbxproj` руками (product name, bundle id, team — в xcconfig;
  таргеты — в `Package.swift`)
- Править `build/`, `core/api/generated/`, `shared/api/*.api`, `generated/Tokens.kt`,
  `Tokens.swift` руками (только `make gen` / `make api-dump` / `make gen-tokens`)
- `android sdk install` / `sdkmanager` и `./gradlew bundleRelease` «по пути»
- Считать зелёный Linux-гейт доказательством работоспособности iOS-UI

## Forward-only

Любой текст в репо описывает **только текущее состояние** — не «было/стало».
История — в `git log` + Conventional Commits. Полное правило —
`.claude/rules/forward-only.md`.

## Авто-память

Запись в файловую авто-память запрещена глобально. Не предлагай «сохранить в
память». Долгосрочное знание — в репо: `CLAUDE.md`, `.claude/rules/`, `state/`
каждого скилла.
