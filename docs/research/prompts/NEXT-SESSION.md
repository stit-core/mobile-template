# Промпт следующей сессии: сборка репозитория `mobile-template`

Дата составления: 2026-09-17. Для основной сессии Claude Code (Fable) в
`/home/mm/git/stit-core/mobile-template/`, которая запускает 2–3 Fable-агента на сборку
репозитория и затем сама всё проверяет. Решение, по которому строим, — `../DECISION.md`;
форма репозитория — `../08-template-design.md`; синтез — `../00-synthesis.md`.

Главное требование владельца к результату: **красиво, качественно, без замудрёностей — ровно,
понятно, читаемо; тесты очень важны.** Каждый агент и основная сессия меряют работу этим
требованием, а не объёмом.

---

## 0. Как пользоваться этим файлом (основная сессия)

1. Прочитать целиком и по порядку: `docs/research/DECISION.md` → `docs/research/00-synthesis.md`
   (§1, §8, §12) → `docs/research/08-template-design.md` (целиком — это спецификация формы) →
   `docs/research/06-tooling-agent.md` (§2 листинги spike, §3 измерения, §4 шум, §8 рекомендации,
   §9 ловушки) → этот файл. Остальные отчёты (01, 02, 03, 05, 07) — по ссылкам из 08, по мере
   надобности; агентам они назначены в их промптах.
2. Выполнить шаг 0 (§2.3): `git init`, первый коммит с ресёрчем, `.claude/plans/roadmap.md` с
   фазами. С этого момента репозиторий живёт по правилам экосистемы: planning-first,
   Conventional Commits, forward-only, комментарии только WHY по-русски.
3. Запускать агентов волнами из §2.4 с промптами из §3 (`model: fable`, `subagent_type:
   general-purpose`, `run_in_background: true`; **не больше одного Gradle-агента одновременно**, см.
   §2.2). Каждому агенту передавать промпт дословно, дополнив блок «Состояние репозитория на
   момент запуска» (что уже есть, что зелёное).
4. После каждой волны — протокол проверки §4, затем коммит(ы) от основной сессии. Агенты **не
   коммитят**.
5. В конце сессии — итоговый прогон §4 целиком, обновление `roadmap.md` (закрытые фазы удаляются
   целиком, незакрытое — строкой в `## Backlog`), отчёт владельцу: что зелёное, какие цифры гейта,
   что осталось (iOS-верификация на Mac и т. п.).

## 1. Цель сессии и Definition of Done

Цель — репозиторий `mobile-template` в состоянии, из которого форк может разрабатывать
Android-приложение с общим Kotlin-ядром и (после появления Mac) iOS-приложение. Это фазы 2, 3, 4,
6 плана (синтез §12) и Linux-часть фазы 5.

Done для сессии:

- `make doctor`, `make check`, `make check-fast`, `make gate-selftest` — зелёные на этой машине
  (`gate-selftest` — все негативные фикстуры красные). `make check` печатает явный блок
  пропущенных macOS-стадий и завершается кодом 0.
- Каркас точно по дереву 08 §2.1: `shared/`, `androidApp/{app,core/*,feature/*,lint}`,
  `iosApp/` (Packages/Pure собирается на Linux; остальное — по канону 02, помечено «не
  верифицировано на Mac»), `build-logic/`, `gradle/libs.versions.toml`, `config/`, `api/`,
  `mock/`, `scripts/`, `.gate-fixtures/`, `docs/`, `.claude/`, `Makefile`, `AGENTS.md`.
- 8 reference-фич работают в `androidApp` в `demo`-flavor и в `dev`-flavor против
  `MockTransport`: `demo`, `shell`, `requirements`, `auth` (с удалением аккаунта), `profile`,
  `todos` (с deep link), `settings` (тема + язык), `push` (регистрация).
- Тесты: `:shared:jvmTest` ≥ 80 % строк; Android-модули ≥ 70 %; каждый экран — Robolectric-тест
  и Roborazzi-скриншот на каждое состояние; Konsist-тесты слоёв; `FixturesTest` на все сценарии
  mock; тест «demo не ходит в сеть»; негативные фикстуры NF-01…NF-16.
- Обвязка: `CLAUDE.md` ≤ 200 строк, 23 правила с `paths:`, `settings.json`, 9 скиллов,
  `docs/{README,getting-started,contributing}.md`, `docs/reference/*`, `docs/stack/README.md` +
  15 `_templates`, `docs/reference/strategy-mobile-canon/`, `README.md`.
- Dry-run обвязки на scratch-копии репозитория: `/init-service → /stack-deep-dive (режим A на
  reference-значениях) → /de-template` в режимах both и `--platform android` — после каждого
  `make check` зелёный, `gate-selftest` красный.
- История — Conventional Commits, дерево чистое, `roadmap.md` актуален.

Не входит в сессию (фиксируется в `## Backlog`): фаза 5 на Mac (`check-mac`, snapshot-тесты,
`.xcodeproj`-верификация, подпись), фаза 7 (первый релиз в RuStore/Play, cross-repo PR'ы),
спайки S-1, S-10, S-15 (Mac) и S-12 (аккаунты магазинов).

## 2. Оркестрация

### 2.1 Роли

| роль | кто | что делает |
|---|---|---|
| Основная сессия | Fable (эта сессия) | git, roadmap, запуск агентов, проверка (§4), коммиты, отчёт владельцу |
| Агент A · Фундамент | Fable | каркас репозитория + Linux-гейт на пустом дереве (фаза 2) |
| Агент B1 · `shared/` | Fable | KMP-ядро: core, контракт API, презентеры, фикстуры, тесты (фаза 3) |
| Агент B3 · Обвязка и docs | Fable | `CLAUDE.md`, правила, settings, скиллы, docs, stack-формы, канон strategy (фаза 6) |
| Агент B2 · Android | Fable | `androidApp/`: core, фичи, Nav3, Hilt, скриншоты, Konsist, `:lint`, demo-flavor (фаза 4) |
| Агент B4 · iOS-pure (опционально) | Fable | `iosApp/Packages/Pure`, SwiftLint/swift-format, `Tools/archlint`, каркас `iosApp/` по канону без Xcode (Linux-часть фазы 5) |

### 2.2 Ограничения машины и правила совместной работы

- **Память.** Пара демонов Gradle + Kotlin занимает ≈ 4,8 ГБ RSS (06 §3.4); на машине 15 ГБ и
  посторонняя нагрузка. **Одновременно работает не больше одного агента, который гоняет Gradle**
  (A, B1, B2 — строго по очереди); агенты без Gradle (B3, B4) идут параллельно с Gradle-агентом.
  Каждый Gradle-агент в конце — `./gradlew --stop`; `gradle.properties` — из 06 §8.2
  (`-Xmx2g -XX:MaxMetaspaceSize=1g`, `workers.max=4`, configuration/build cache).
- **Тулчейны уже установлены** (06): Android SDK `~/.cache/stit-mobile-research/android-sdk`, JDK 21
  `~/.cache/stit-mobile-research/jdk/…`, Swift 6.4 `~/.cache/stit-mobile-research/swift/…`,
  SwiftLint `~/.cache/stit-mobile-research/swiftlint/…`, openapi-generator
  `~/.cache/stit-mobile-research/openapi-generator/…`, Gradle-кеши `~/.gradle`, K/N `~/.konan`.
  Ничего не перекачивать и не ставить через `apt`/`sudo`; `make doctor`/`make setup` принимают
  пути через env (`ANDROID_HOME`, `JAVA_HOME`, `SWIFT_TOOLCHAIN`) и печатают next-step, если чего-то
  нет. Точные пути — `06-tooling-agent.md` §2 и приложение.
- **Владение файлами** (чтобы параллельные агенты не сталкивались):

  | агент | пишет только в |
  |---|---|
  | A | всё дерево (один, без параллели) |
  | B1 | `shared/**`, `api/**`, `mock/**`, `config/openapi/**`, `scripts/tools/{gen,gen-check,mock-check}.sh`, строки `shared`-зависимостей в `gradle/libs.versions.toml`, `.gate-fixtures/NF-11…16` |
  | B2 | `androidApp/**`, `config/{detekt,lint,env}/**`, `scripts/tools/{i18n-check,deeplinks-check,tokens-check}.sh`, `design/tokens.json`, `.gate-fixtures/NF-01…10` (если A не создал), `e2e/android/**` |
  | B3 | `CLAUDE.md`, `AGENTS.md`, `README.md`, `.claude/**` (кроме `plans/roadmap.md` — его ведёт основная сессия), `docs/**` (кроме `docs/research/**`) |
  | B4 | `iosApp/**`, `e2e/ios/**`, `scripts/tools/swift-step.sh`, `.gate-fixtures/NF-20…24` |

  Общие файлы (`Makefile`, `scripts/tools/check.sh`, `settings.gradle.kts`, `gradle.properties`,
  `build-logic/**`, `.editorconfig`, `.gitignore`) создаёт A; B1/B2/B4 правят их только
  точечно и **сообщают об этом в финальном ответе** (основная сессия сверяет).
- Агенты пишут только внутри `/home/mm/git/stit-core/mobile-template/` (и в свой scratchpad);
  соседние репо `/home/mm/git/stit-core/*` — read-only. Агенты не коммитят и не запускают
  `git` кроме `git status`/`git diff`.
- Чужие процессы не убивать (`pkill`/`killall` запрещены); свои Gradle-демоны — `./gradlew --stop`.
- Не тратить > 25 минут на один тупик: зафиксировать в финальном ответе, обойти, идти дальше.

### 2.3 Шаг 0 — основная сессия, до агентов

```bash
cd /home/mm/git/stit-core/mobile-template
git init -b main
git add .gitignore docs/research
git commit -m "docs(research): ресёрч мобильного шаблона — синтез, решение, отчёты потоков"
```

Затем создать `.claude/plans/roadmap.md` (форма — `spa-frontend-template/.claude/plans/roadmap.md`
и skill `/plan` Go/SPA): `## Active` с секцией «Сборка mobile-template v1» (шаги = волны §2.4,
гейт = §1 Done), `## Backlog` — фаза 5 на Mac, фаза 7, спайки Mac/аккаунты, cross-repo P0
(синтез §9), извлечение `mobilekit` по триггеру, `sharedUi/` по триггеру, `buildHealth` при
поддержке AGP 9.x, Baseline Profile + GMD, SQLDelight-шов. Коммит `meta(plan): план сборки`.

### 2.4 Волны

| волна | агенты | вход | выход (проверяется основной сессией по §4) |
|---|---|---|---|
| 1 | **A** | пустое дерево + spike `docs/research/spikes/android-gate` | каркас; `make check` зелёный на пустом дереве ≤ 15 с тёплый; `gate-selftest` красный на NF-01…10; коммит `feat(repo): каркас и Linux-гейт` |
| 2 | **B1** ∥ **B3** | каркас | `shared/` полностью с тестами ≥ 80 %; обвязка и docs; коммиты `feat(shared): …`, `docs(harness): …` |
| 3 | **B2** ∥ **B4** (опц.) | shared + обвязка | `androidApp/` с 8 фичами, скриншотами, Konsist, `:lint`, demo; iOS-pure; коммиты `feat(android): …`, `feat(ios): …` |
| 4 | основная сессия (+ короткие агенты на точечные правки) | всё | протокол §4 целиком, dry-run обвязки, итоговый отчёт |

Между волнами основная сессия: читает финальные ответы агентов, гоняет §4, чинит расхождения
(сама или коротким агентом с точным заданием), коммитит.

## 3. Промпты агентов

Общий преамбул (вставлять в начало каждого промпта):

```text
Ты — Fable-агент сборки репозитория `mobile-template` экосистемы stit. Работай автономно, вопросы
владельцу не задавай — принимай решения по `docs/research/DECISION.md` и `08-template-design.md`,
а спорное фиксируй в финальном ответе.

Репозиторий: `/home/mm/git/stit-core/mobile-template/` — пиши только туда (в свою зону владения из
`docs/research/prompts/NEXT-SESSION.md` §2.2) и в свой scratchpad. Соседние репо
`/home/mm/git/stit-core/*` — только чтение. `git` — только `status`/`diff`; коммитит основная
сессия. Тулчейны уже стоят в `~/.cache/stit-mobile-research/` (пути — `06-tooling-agent.md` §2);
ничего не качать заново, `apt`/`sudo` не использовать; Gradle только с `gradle.properties` из
06 §8.2; в конце — `./gradlew --stop`. Чужие процессы не убивать.

Требование владельца: **красиво, качественно, без замудрёностей, ровно, читаемо; тесты очень
важны.** Стандарт качества — NEXT-SESSION.md §5, тестовая политика — §6, чего не делать — §7,
ловушки — §8. Код и доки — forward-only (только текущее состояние), комментарии — только WHY,
по-русски, однострочные; идентификаторы, команды, названия API — на английском. Работай
инкрементально: дерево зелёное после каждого этапа, не «всё сразу в конце». Не тратить
> 25 минут на тупик — зафиксировать и обойти.

Финальный ответ — резюме ≤ 300 слов: что сделано, цифры гейта (времена, покрытие), что менял в
общих файлах, что не удалось и почему, открытые вопросы.
```

### 3.1 Агент A — фундамент (фаза 2)

```text
[преамбул]

## Задача
Собрать каркас репозитория и Linux-гейт на пустом дереве — точно по `08-template-design.md`
§2.1 (дерево), §2.3 (Gradle-проекты и convention-плагины), §4 (гейт целиком: §4.1 словарь вербов,
§4.2 состав `make check`, §4.3 KMP-стадии, §4.5 хуки и обёртки, §4.6 форматеры, §4.7 негативные
фикстуры, §4.9 требования к машине). Семя — spike `docs/research/spikes/android-gate/`
(прогнан, зелёный): `build-logic/`, `gradle/libs.versions.toml`, `gradle.properties`,
`config/detekt/detekt.yml`, `Makefile`, `scripts/tools/{check.sh,gradle-step.sh,doctor.sh}`,
`.editorconfig`, `shared/build.gradle.kts` — переноси и дорабатывай, не переписывай с нуля.
Прочитай перед началом: DECISION.md; 08 §2–§4; 06 §2 (листинги spike), §8 (рекомендации, в т. ч.
§8.2 `gradle.properties`, §8.5 permissions, §8.6 фикстуры, §8.7 решения), §9 (ловушки);
01 §R3 (convention-плагины), §R5 (Spotless/Lint/Kover), §R7 (гейт).

## Что сделать
1. Корень: `settings.gradle.kts` (`pluginManagement { includeBuild("build-logic") }`, foojay-resolver,
   `TYPESAFE_PROJECT_ACCESSORS`, `include(":shared", ":androidApp:app", ":androidApp:core:designsystem",
   ":androidApp:core:ui", ":androidApp:core:platform", ":androidApp:core:navigation",
   ":androidApp:core:testing", ":androidApp:feature:shell", … ":androidApp:lint")` — модули пока
   пустые с минимальным `build.gradle.kts`), `build.gradle.kts` (только `plugins { … apply false }`),
   `gradle.properties` (06 §8.2), `gradle/libs.versions.toml` (версии из spike; SDK-уровни там же),
   `gradle/wrapper` (9.7.1 + `distributionSha256Sum`), `.editorconfig`, `.gitignore` (дополнить
   существующий: `build/`, `.gradle/`, `.kotlin/`, `*.hprof`, `local.properties`,
   `Local.xcconfig`, `DerivedData/`, `*.xcresult`, `iosApp/.build/`, `.env.local`),
   `.xcode-version` (27.0), `.swift-version` (6.4), `local.properties.example`.
2. `build-logic/convention` — плагины `stit.android.application`, `stit.android.library`,
   `stit.android.feature`, `stit.android.compose`, `stit.android.hilt`, `stit.android.lint`,
   `stit.kmp.library`, `stit.jvm.library`, `stit.quality` (Spotless + detekt + Kover), `stit.roborazzi`,
   `stit.konsist` (имена и состав — 08 §2.3; паттерн AGP 9 — 06 §9 L-04: `ApplicationExtension`/
   `LibraryExtension` напрямую, общее — функциями). Каждый плагин ≤ 100 строк, с однострочным
   WHY-комментарием, без дублирования между плагинами.
3. `shared/` — KMP-модуль по 08 §2.2/§2.3: targets `androidLibrary { withHostTest {} }`, `jvm()`,
   `iosArm64()`, `iosSimulatorArm64()`; framework `Shared` (static); source-sets `commonMain/
   commonTest/androidMain/androidHostTest/iosMain/jvmMain/jvmTest`; один класс-заглушка + один
   тест; BCV `apiDump` (`shared/api/`); SKIE подключить, если собирается на Linux без cinterop
   (иначе — зафиксировать и оставить TODO-строку в финальном ответе, не в коде).
4. `androidApp/app` — пустое приложение (Application, MainActivity с `Text("mobile-template")`),
   flavors `env = dev | staging | prod | demo` из `config/env/*.properties` → `BuildConfig`;
   подпись release из env `STIT_KEYSTORE_*` (как в spike). Остальные модули — минимальные.
5. `Makefile` — словарь вербов 08 §4.1 (группы `##@ LOOP / GATE / KMP / MAC / RELEASE / SETUP /
   INTEL / META`, concise-by-default, `FORMAT=detailed`/`VERBOSE=1`), `scripts/tools/*`:
   `check.sh` (стадии с `OK:/FAIL:` и SKIP-блоком macOS в конце, логи в `build/gate-logs/`),
   `gradle-step.sh`, `swift-step.sh` (заглушка, если Swift-часть ещё пуста — честно печатает SKIP),
   `doctor.sh` (JDK, `ANDROID_HOME`, лицензии, RAM ≥ 6 ГБ, `.xcode-version`, Swift-тулчейн —
   WARN/FAIL с next-step), `status.sh`, `gate-selftest.sh` (по фикстурам), `version-from-tag.sh`,
   `coverage-floor.sh`, `mac.sh` (SSH-обёртка с fail-fast при отсутствии `MAC_HOST`),
   `android-skills.sh` (`android skills add`, `--no-metrics`). `make kmp-check` проверяет
   **наличие каталога klib** после `compileKotlinIos*` (06 §8.4).
6. `.gate-fixtures/NF-01…NF-10` (06 §8.6) + `make gate-selftest`: подложить фикстуру → ожидать
   ненулевой код → откатить; все 10 красные.
7. `scripts/git-hooks/{commit-msg,pre-commit,pre-push}` (форма — `go-backend-template/scripts/
   git-hooks/`): commit-msg — Conventional Commits `feat|fix|refactor|docs|test|chore|meta|perf|style`;
   pre-commit — Spotless/detekt по изменённым; pre-push — `make check` (+ `GATE_RELEASE=1`).
   `make setup` их ставит.
8. `AGENTS.md` — три строки-указатель на `CLAUDE.md` (сам `CLAUDE.md` пишет B3; оставь заглушку
   из 5 строк, чтобы `AGENTS.md` не ссылался в пустоту).

## Done
`make doctor` OK; `make check` зелёный ≤ 15 с тёплый и печатает SKIP-блок; `make check-fast`,
`make kmp-check`, `make fmt`, `make test`, `make build`; `make gate-selftest` — 10 из 10 красных;
`./gradlew --stop`. В финальном ответе — времена (холодный/тёплый), список созданных модулей и
плагинов, что из spike перенесено как есть, что изменено и почему.
```

### 3.2 Агент B1 — `shared/` (фаза 3)

```text
[преамбул]

## Задача
Наполнить `shared/` — KMP-ядро по `08-template-design.md` §2.2 (что где), §2.4 (DI и persistence),
§2.5 (сеть, контракт, auth), §2.6 (матрица зависимостей внутри shared), §2.7 (что демонстрируют
reference-фичи) — и контракт API по `07-backend-contract.md` (§1.3b auth-контракт и черновик
спека, §2 спек и кодоген, §2.6 конвенции, §3 mock, §4.2 env-контракт, §6 push, §7 observability).
Стек и решения — DECISION.md §2. Прочитай также 03 §6.3–6.4 (правило плоского фасада, черновики
конфигов) и 06 §5, §8.8 (KMP на Linux, кодоген Kotlin).

## Структура (`shared/src/commonMain/kotlin/tech/stit/mobile/`)
- `core/config` (`AppConfig`, `AppEnv`, `AuthMode`), `core/api` (`ApiClient` на Ktor: базовый URL,
  `X-Request-ID`, `X-Client`, `Idempotency-Key` на мутациях, ретраи по `Retry-After`/429/503,
  `ApiError{code,…}` → типизированные ошибки; `generated/` — модели из спека),
  `core/auth` (`SessionStore`, `AuthPlugin` с single-flight refresh, `AuthMode.Bearer` +
  `AuthMode.Cookie`, `SecureStore` expect/actual), `core/requirements` (`RequirementsClient`,
  флаги, min-version), `core/push` (`PushRegistrar`, `DeviceRegistration`), `core/deeplink`
  (`Route` sealed, `DeepLinkParser: URL → Route?` — чистая функция), `core/mock` (`MockTransport`
  на Ktor `MockEngine`, сценарии из `mock/scenarios.yaml`, фикстуры из `mock/fixtures/`),
  `core/observability` (`TelemetrySink` с `NoopSink`, `Logger`), `core/i18n` (`MessageKey`),
  `core/lib` (`DispatcherProvider` expect/actual, `Clock`, `Result`-хелперы, `FileCache<T>` на okio,
  `Settings` expect/actual).
- `entity/{user,session,todo,device}` — модель + репозиторий (SSOT, in-memory кеш + `FileCache`)
  + DTO-мапперы.
- `feature/{shell,auth,profile,todos,settings,requirements,push,demo}` — презентеры:
  `class <X>Presenter(deps) { val state: StateFlow<State>; fun dispatch(intent: Intent) }`,
  `State` — immutable data class, `Intent` — sealed; use-case'ы только там, где есть логика.
- `SharedContainer(platform: PlatformDeps, config: AppConfig)` — ручной composition root, ленивые
  `val`, фабрики презентеров; **плоский фасад для Swift**: final-классы, без generic'ов в публичных
  сигнатурах, `suspend`/`StateFlow` (правило `kmp-public-api`, 08 §3.2). `apiDump` обновлять.
- `api/openapi.yaml` — reference-спека: снять с `go-backend-template` рецептом 07 §2.1 (30-строчный
  `cmd/openapidump` на копии бэкенда в scratchpad, Go 1.26 есть) **или** написать руками по 07 §1.3b
  (auth Bearer-ветка, `/api/app/requirements`, `/api/v1/devices`, `/api/users/me`, todos как
  reference-ресурс); `openapi-generator validate` чист; заголовок-комментарий «reference до B-12».
  `config/openapi/kotlin-models.yaml` + `scripts/tools/gen.sh` (models-only, kotlinx-serialization,
  `ignoreUnknownKeys`, пост-процессинг B-13…B-15 из 07 §2.2) + `gen-check.sh` (дрейф спека ↔
  моделей ↔ `api/openapi.lock`); сгенерированное коммитится в `core/api/generated/`, руками не
  правится.
- `mock/scenarios.yaml` + `mock/fixtures/<scenario>/<entity>.json` (`empty | populated |
  authenticated | error`), формат = ответы API с конвертом `data`.
- `androidMain`: actual'ы `SecureStore` (Keystore AES-GCM + DataStore), `Settings` (DataStore),
  `DispatcherProvider`, `FileCache` (files dir); `iosMain`: `SecureStore` (Keychain через
  Foundation/Security — `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`, `Synchronizable=false`),
  `Settings` (`NSUserDefaults`), `FileCache`; **без cinterop/CocoaPods**.

## Тесты (`commonTest`, запуск `:shared:jvmTest`; `androidHostTest` для Android-actual'ов)
Каждый презентер — сценарные тесты по состояниям через фикстуры (Turbine, `runTest`,
`StandardTestDispatcher`); каждый репозиторий и маппер — table-driven; `AuthPlugin` — single-flight
refresh под параллельными запросами, reuse → logout; `DeepLinkParser` — таблица URL → Route;
`FixturesTest` — все фикстуры всех сценариев парсятся сгенерированными моделями; `MockTransport`
— «demo не ходит в сеть». Фейки, не моки. Kover ≥ 80 % строк по `shared` (без `generated/`).
Негативные фикстуры NF-11…NF-16 (08 §4.7): `shared/commonMain` с `android.`-импортом; `core/**`
импортирует `feature/**`; публичный класс с generic'ом в сигнатуре фасада; `cinterops {` в
`shared/build.gradle.kts`; правка `generated/` руками (`gen-check` красный); фикстура с лишним
полем при `ignoreUnknownKeys=false` — проверить, что каждая красная.

## Done
`make kmp-check`, `make test`, `make cover-check` (≥ 80 %), `make gen-check`, `make mock-check`,
`make check` — зелёные; `apiDump` закоммичен (основной сессией); NF-11…16 — красные в
`gate-selftest`. В финальном ответе — покрытие по пакетам, список публичного API фасада
(`shared/api/shared.api` кратко), что менял в общих файлах.
```

### 3.3 Агент B3 — обвязка и docs (фаза 6)

```text
[преамбул]

## Задача
Написать агентную обвязку и документацию репозитория по `08-template-design.md` §3 (CLAUDE.md
листинг §3.1, 23 правила §3.2, settings §3.3, 9 скиллов §3.4, AGENTS.md §3.5), §5 (docs, kinds,
kinship), §6 (слой `mobile/` в strategy: копия канона), §7.4–7.7 (форма `/release`). Образцы формы
— **читать и соразмерять, не копировать**: `go-backend-template/{CLAUDE.md,docs/**,.claude/**}`,
`spa-frontend-template/{CLAUDE.md,docs/**,.claude/**}`, `telegram-bot-template/CLAUDE.md`,
`business-strategy-template/docs/projects/_template/{telegram-bot-flow,fsd}/README.md` и
`.claude/skills/{fsd-deep-dive,fsd-audit}/SKILL.md`. Факты для инвариантов — DECISION.md, 01 §R9,
02 §7.9, 04 §7.2, 05 §8.5, 07 §10.3, 06 §8.5–8.7.

## Что написать
- `CLAUDE.md` ≤ 200 строк по листингу 08 §3.1: «Где это в экосистеме», точка входа, «проектирование
  первично», команды `make`, «Карта: где что», **«Инварианты: что в 2026 иначе, чем агент помнит»**,
  git workflow, границы (делать всегда / сначала спросить / никогда), forward-only, авто-память.
- `.claude/rules/*.md` — 23 файла по таблице 08 §3.2, каждый ≤ 60 строк, frontmatter `name /
  description / paths:`; terse-инварианты, ссылка на `docs/reference/*` за «как и почему».
- `.claude/settings.json` — 08 §3.3 (allow/deny из 06 §8.5, `effortLevel: max`, `autoMemoryEnabled:
  false`).
- `.claude/skills/{smartdev,plan,triage-failure,init-service,stack-deep-dive,de-template,scaffold,
  release,infra-handoff}/SKILL.md` (+ `references/` где 08 §3.4 их требует: prune-лист
  `de-template`, варианты `scaffold`, чек-листы `stack-deep-dive`, шаги `release`, формы handoff).
  Скиллы ссылаются на реальные вербы `Makefile` и реальные пути дерева.
- `.claude/agents/{explorer,reviewer}.md` — по образцу Go-шаблона, с мобильными инструментами.
- `AGENTS.md` — указатель на `CLAUDE.md` (заменить заглушку A).
- `docs/README.md`, `docs/getting-started.md` ([META]; **Mac build-host — предусловие iOS-ветки**,
  Android-first путь), `docs/contributing.md`, `docs/reference/{architecture,ecosystem,release,
  observability,distribution,strategy-mapping,mac-host,kinship}.md`, `docs/reference/
  strategy-mobile-canon/` (README + 9 форм — 08 §6.2/§6.4), `docs/stack/README.md` +
  `docs/stack/_templates/<kind>.md` × 15 (08 §5.2, каждая с frontmatter и секцией `Generates`,
  предзаполненными дефолтами шаблона), `README.md` корня (по образцу Go/SPA: место в экосистеме,
  «использовать как шаблон», что внутри, команды, документация, лицензия Apache-2.0),
  `LICENSE`, `SECURITY.md` (скопировать из Go-шаблона).
- Всё forward-only, по-русски, без «было/стало», без changelog'ов; ссылки только на существующие
  файлы (сверяй с деревом; если файл ещё не создан другим агентом — ссылайся на путь из 08 §2.1
  и отметь в финальном ответе).

## Done
`CLAUDE.md` ≤ 200 строк; у всех правил валидный frontmatter с `paths:`; все скиллы имеют `name` и
`description`; `docs/stack/_templates` — 15 файлов с `status: draft` и `Generates`; ссылки внутри
docs резолвятся (проверь скриптом); `make check` не сломан. В финальном ответе — список файлов и
пункты, где текст зависит от кода B1/B2 (чтобы основная сессия сверила).
```

### 3.4 Агент B2 — Android (фаза 4)

```text
[преамбул]

## Задача
Собрать `androidApp/` по `08-template-design.md` §2.1–§2.3 (модули и convention-плагины), §2.6
(матрица зависимостей и enforcement), §2.7 (reference-фичи), §4 (стадии гейта Android) и канону
`01-android-canon.md` (§2 архитектура, §3 модульность/токены/i18n, §R4–R6 detekt/Lint/Konsist,
§7 тесты, §R8 правила, §R10 фичи). Презентеры и `SharedContainer` уже есть в `shared/` (B1) —
UI-слой **тонкий**: `@HiltViewModel` оборачивает презентер, экран рисует `State` и шлёт `Intent`.
Прочитай 06 §2 (Roborazzi/Konsist/Lint в spike), §9 (ловушки).

## Что сделать
- `core:designsystem` — токены (`Color/Type/Spacing/Shape` + `AppTheme` через `CompositionLocal`
  поверх Material 3 1.4; light/dark; dynamic color выключен), `design/tokens.json` (DTCG-имена)
  + Gradle-таск `generateTokens` → `Tokens.kt` (детерминированный); каталог компонентов с
  `@Preview` и Roborazzi-тестом на каждый.
- `core:ui` — общие композаблы состояний (`LoadingState`, `EmptyState`, `ErrorState`,
  `OfflineBanner`, `UpdateRequired`), `core:platform` — Hilt-биндинги `PlatformDeps`, `core:navigation`
  — Navigation 3 (`NavDisplay`, `rememberNavBackStack`, `Route` из shared как `NavKey`, back-обработка),
  `core:testing` — фейки, `MainDispatcherRule`, Roborazzi-хелперы, Hilt-тест-модули.
- `feature/{shell,auth,profile,todos,settings}` — `<X>Route` (stateful, `@HiltViewModel`) +
  `<X>Screen` (stateless, все состояния из `State`), `@Preview` на каждое состояние; `requirements`,
  `push`, `demo` — в `core`/`app` по 08 §2.7. Deep link `todos/<id>` через `intent-filter` +
  `DeepLinkParser` из shared. `settings`: тема + язык (`LocaleConfig`, `setApplicationLocales`).
  `auth`: логин, биометрия как разблокировка `SecureStore`, **удаление аккаунта**.
- `app` — `Application`, `MainActivity`, Hilt-граф (`SharedModule` → `SharedContainer`), flavors
  `dev | staging | prod | demo` (`demo` = `MockTransport` + фикстуры `populated`), `APP_ENV` на экране
  «О приложении», `res/` ru/en (без hardcoded-строк), иконки Material Symbols в `res/drawable`.
- `lint` — свой модуль с `DesignSystemDetector` (сырые `Color(0x…)`/`dp` вне designsystem) + тест
  детектора; Lint `warningsAsErrors`, без baseline.
- `app/src/test/.../ArchitectureTest.kt` (Konsist) — правила 08 §2.6; detekt `ForbiddenImport`
  (`android.util.Log`, `androidx.compose.material.`, `retrofit2.`, `androidx.security.crypto.`,
  `GlobalScope`).
- `scripts/tools/{i18n-check,deeplinks-check,tokens-check}.sh` + строки в `check.sh`;
  `e2e/android/*.yaml` (Maestro, вне гейта); `.gate-fixtures/NF-01…10` — проверить, что красные.

## Тесты
Каждый экран — Robolectric Compose-тест (`androidx.compose.ui.test.junit4.v2`) на каждое состояние
+ Roborazzi-скриншот на каждое состояние (эталоны — `make screenshots-record`, осознанно);
ViewModel-обёртки — тесты только там, где есть логика (маппинг/навигация); Konsist; Lint-детектор;
`FixturesTest` из shared используется в demo-тесте «экран todos в demo показывает данные
фикстуры». Kover ≥ 70 % по `androidApp` (без `generated/`, превью и Hilt-кода).

## Done
`make check` и `GATE_RELEASE=1 make check` зелёные; `make screenshots` зелёный, PNG-эталоны
читаемы (открой 2–3 и опиши, что видно); `make gate-selftest` — NF-01…10 красные; `make build`
даёт `app-dev-debug.apk` и `app-demo-debug.apk`; `./gradlew --stop`. В финальном ответе — времена
гейта, покрытие по модулям, список экранов × состояний, что менял в общих файлах.
```

### 3.5 Агент B4 — iOS-pure (Linux-часть фазы 5; опционально)

```text
[преамбул]

## Задача
Подготовить `iosApp/` в объёме, проверяемом на Linux, по `02-ios-canon.md` (§3 модульность,
§4 тесты, §5 качество/гейт, §7 черновики) и `08-template-design.md` §2.1–§2.3 (пакеты `Pure/Core/
Features/TestSupport`, `SharedBridge`), §4.4 (`check-mac`), §4.6 (swift-format + SwiftLint без
`trailing_comma`). Семя — `docs/research/spikes/swift-linux/` (Package.swift, `.swift-format`,
`.swiftlint.yml`, Swift Testing — прогнано, зелёное). Mac/Xcode нет: всё, что импортирует
SwiftUI/UIKit/Shared, пишется по канону **без компиляции** и помечается в финальном ответе как
«не верифицировано на Mac».

## Что сделать
- `iosApp/Packages/Pure` (`AppFoundation`, `DesignSystemTokens` из `design/tokens.json` тем же
  генератором, что у Android, или отдельным детерминированным скриптом; тесты Swift Testing) —
  `swift build`/`swift test` на Linux зелёные, coverage через `llvm-cov` ≥ 70 %.
- `iosApp/.swiftlint.yml` (02 §7.4: custom rules — запрет `print`, `import Combine`, `URLSession(`
  вне `SharedBridge`, `import <Другая>Feature`), `.swift-format` (02 §7.5), `.test-simulator`,
  `Config/{Base,Dev,Staging,Prod,Demo}.xcconfig` + `Local.xcconfig.example`, `TestPlans/*.xctestplan`.
- `iosApp/Tools/archlint` — Swift CLI над `swift package dump-package`: Features ↛ Features/
  Persistence/Platform; Core ↛ Features; работает на Linux.
- Каркас `iosApp/iosApp.xcodeproj` — **только** копией из `Kotlin/KMP-App-Template-Native`
  (клонировать в scratchpad, перенести `iosApp/`, заменить имена/bundle id через `xcconfig`, не
  править `.pbxproj` руками сверх замен идентификаторов); `App/{iOSApp.swift, AppEnvironment.swift,
  RootView.swift, Info.plist, PrivacyInfo.xcprivacy, App.entitlements}`, `scripts/embed-shared.sh`.
- `Packages/Core/{DesignSystem,Routing,Persistence,Platform,SharedBridge}` и `Packages/Features/*`
  — по канону 02/08: тонкие `@Observable`-адаптеры над презентерами shared, `Router` с
  типизированным путём; snapshot-тесты — заготовки (Mac).
- `scripts/tools/swift-step.sh` + стадия `swift` в `check.sh` (Linux: `swift format lint`, SwiftLint,
  archlint, `swift build`/`test` для `Pure`); `.gate-fixtures/NF-20…24` (02/08: `print(`, `import
  Combine`, Feature→Feature, `URLSession` вне bridge, сырой цвет) — красные.

## Done
Linux-часть зелёная в `make check`; `make gate-selftest` — NF-20…24 красные; README-пометка
«iOS не верифицирован на Mac» в `docs/reference/mac-host.md` (согласовать с B3 — не перезаписывать
его файл, оставить блок в финальном ответе). В финальном ответе — что именно требует Mac.
```

## 4. Протокол проверки основной сессией

После каждой волны и в конце — по порядку, с фиксацией цифр в `roadmap.md`-секции:

1. **Машина и гейт.** `make doctor` → `make check` (записать время тёплого/холодного прогона и
   число строк вывода; SKIP-блок macOS присутствует, код 0) → `make check-fast` →
   `GATE_RELEASE=1 make check` → `make gate-selftest` (все фикстуры красные, дерево после —
   чистое) → `./gradlew --stop`; `free -g` до/после (демоны не остались).
2. **Структура.** Дерево совпадает с 08 §2.1 (расхождения — либо обоснованы в финальном ответе
   агента, либо чинятся); имена модулей/пакетов/классов по канону (`<X>Presenter`, `<X>State`,
   `<X>Intent`, `<X>Route`, `<X>Screen`, `Fake*`); нет пустых модулей-заглушек «на будущее»; нет
   `TODO`/`FIXME` в коде (незакрытое — в `roadmap.md`).
3. **Читаемость (владелец: «красиво, без замудрёностей»).** Выборочно прочитать по 3–5 файлов из
   каждой зоны (`shared/core`, `shared/feature`, `androidApp/feature`, `build-logic`, скрипты) и
   ответить на вопросы: понятно ли за минуту, что делает файл; нет ли лишних абстракций
   (интерфейс с одной реализацией без теста-фейка, generic-обёртки, «фреймворк внутри шаблона»);
   файл ≤ ~300 строк; комментарии только WHY, по-русски; формат единый (`make fmt-check`).
   Всё, что не проходит, — короткая задача агенту с точным адресом.
4. **Тесты.** Покрытие ≥ 80/70; каждый экран имеет тест и скриншот на каждое состояние (открыть
   3–4 PNG эталона и убедиться, что они читаемы и соответствуют состоянию); фейки, не моки;
   `FixturesTest`, `MockTransport`-тест, single-flight refresh, `DeepLinkParser`, Konsist,
   Lint-детектор — присутствуют и зелёные; негативные фикстуры — красные.
5. **Обвязка.** `CLAUDE.md` ≤ 200 строк и не противоречит коду (вербы `make`, пути, версии);
   у правил есть `paths:` и они срабатывают на своих файлах (открыть файл из зоны — правило в
   контексте); скиллы ссылаются на существующие вербы/пути; `settings.json` deny блокирует
   `assembleRelease`/`keytool`/правку `generated/`; docs без битых ссылок (`scripts/tools/
   links-check.sh` или grep), forward-only (нет «было/стало», дат-ремарок, changelog'ов).
6. **Dry-run обвязки** (волна 4): скопировать репозиторий в scratchpad, пройти `/init-service →
   /stack-deep-dive` (режим A, reference-значения) → `/de-template` в режимах both и
   `--platform android`; после каждого — `make check` зелёный, `gate-selftest` красный; `/scaffold`
   каждого варианта даёт компилируемый код. Расхождения — чинить в скиллах, не в копии.
7. **Коммит.** `git status` чистый, Conventional Commits по волнам, трейлер `Co-Authored-By`
   как принято в сессии; `roadmap.md` обновлён.

## 5. Стандарт качества («красиво, ровно, читаемо»)

- **Одна вещь — одно место.** Версии — только `libs.versions.toml`; SDK-уровни — там же; конфиг
  гейта — `config/`; вербы — `Makefile`; правда о структуре — `docs/reference/architecture.md`.
- **Никаких замудрёностей.** Ручной `SharedContainer` вместо DI-фреймворка в shared; презентер =
  `StateFlow<State>` + `dispatch(Intent)` и ничего больше; без generic-«движков», без
  собственных мини-фреймворков навигации/ошибок/логирования, без reflection. Интерфейс
  заводится, когда есть вторая реализация (фейк в тестах — законная вторая).
- **Читаемый файл**: ≤ ~300 строк, одно понятие, имя говорит за содержимое; порядок членов
  единый (публичное сверху); импорты без wildcard; форматирование только автофиксером (`make fmt`).
- **Именование** — единое на всех уровнях: `<X>Presenter/State/Intent` (shared), `<X>Route/
  <X>Screen/<X>ViewModel` (Android), `<X>View/<X>Model` (iOS), `Fake<Port>` (тесты), `Default<Port>`
  (реализации), `stit.*` (convention-плагины), `NF-NN` (фикстуры).
- **Пустых состояний нет**: каждый экран умеет `loading / empty / error / offline` там, где они
  возможны, и это видно в скриншотах.
- **Строки, цвета, размеры** — только через ресурсы и токены; hardcoded — красный Lint.
- **Комментарии** — только WHY, по-русски, одна строка; docstring'ов «что делает» нет.
- **Документация** — короткая, forward-only, с таблицами «где что»; README читается за две
  минуты; ни одной ссылки в пустоту.
- **Вывод инструментов** — concise (`OK:`/`FAIL:` + путь к логу), полные логи в `build/gate-logs/`.

## 6. Тестовая политика (тесты очень важны)

| слой | обязано иметь тест | инструменты | пол покрытия |
|---|---|---|---|
| `shared` презентеры, use-case'ы | сценарии по состояниям через фикстуры; ошибки и offline-ветки | `commonTest`, `runTest`, `StandardTestDispatcher`, Turbine | 80 % строк (`generated/` исключён) |
| `shared` репозитории, мапперы, `DeepLinkParser`, `AuthPlugin` | table-driven; single-flight refresh; reuse → logout | те же + `MockEngine` | |
| `shared` контракт | `FixturesTest` (все сценарии × сущности парсятся); `gen-check` (дрейф спека) | JVM | — |
| Android экраны | Robolectric Compose-тест + Roborazzi-скриншот **на каждое состояние** | Compose Test v2, Roborazzi | 70 % |
| Android архитектура | Konsist (слои, feature ≠ feature, naming, v1-импорты); Lint-детектор с тестом | Konsist, Lint test | — |
| Android platform-actual'ы | `SecureStore`/`Settings`/`FileCache` под Robolectric (`androidHostTest`) | Robolectric | — |
| гейт | `make gate-selftest`: каждая негативная фикстура красная | скрипт | — |
| iOS `Pure` | Swift Testing, параметризованные тесты | `swift test`, `llvm-cov` | 70 % |
| e2e | Maestro-флоу `app-start`, `login`, `todos` (вне гейта, устройство/эмулятор) | Maestro | — |

Правила: тест пишется вместе с кодом (RED → GREEN там, где есть логика); фейки, не моки;
детерминизм (фиксированный `Clock`, `DispatcherProvider`, фикстуры); эталоны скриншотов
перезаписываются только осознанной командой; закомментированных и `@Ignore`-тестов нет; тесты
читаются как спецификация (имена — по-русски в `@DisplayName`/`@Test("…")` допустимы, идентификаторы
— английские).

## 7. Чего не делать

Retrofit, Koin/Metro/kotlin-inject, Room/SQLDelight, Compose Multiplatform, Swift OpenAPI
Generator, TCA/Combine, Tuist/XcodeGen, Periphery, fastlane на Linux, xtool, Firebase (кроме
FCM-транспорта в `push`), `security-crypto`, `androidx.navigation` 2.x, Material 2, Material 3
Expressive, `material-icons-*`, lint/detekt baseline'ы, `detekt-formatting`, SwiftFormat
(nicklockwood), `kotlin-android`/`kapt`, BDUI-шов, аналитика/IAP/соцвход по умолчанию, `TODO` в
коде, «на будущее»-модули, второй способ сделать то же самое, правка `generated/` и `.pbxproj`
руками, `--no-verify`, коммиты агентами, `apt`/`sudo`, скачивание SDK/тулчейнов заново.

## 8. Ловушки, которые уже стоили времени (полные списки — 06 §9, 01 §Ловушки, 02 §Ловушки)

1. AGP 9: `kotlin-android` не применять, `kotlinOptions{}` нет, kapt нет; `CommonExtension` не
   генерик — конфигурировать `ApplicationExtension`/`LibraryExtension`; `compileSdk = 37` +
   `compileSdkMinor = 2`.
2. `MaxMetaspaceSize < 1g` → Android Lint падает с `OutOfMemoryError: Metaspace`.
3. `linkDebugFrameworkIosArm64` на Linux — SKIPPED с кодом 0: проверять каталог klib на диске;
   `iosSimulatorArm64Test` на Linux не запускается; `kotlin.native.ignoreDisabledTargets=true`.
4. `androidLibrary {}` (KMP) — `commonTest` на Android не запускается без `withHostTest {}`.
5. `detekt-formatting` (ktlint 0.50) конфликтует со Spotless (ktlint 1.8.0) — не подключать;
   без `.editorconfig` ktlint валит `@Composable`-имена; compose-rules ≥ 0.5.0 требует
   detekt 2.0-alpha — держать 0.4.28 + 1.23.8.
6. Compose Test API v1 deprecated — только `androidx.compose.ui.test.junit4.v2`; v2 дефолтит
   `StandardTestDispatcher`.
7. `dependency-analysis` 3.19.2 не знает AGP 9.4 — вне гейта.
8. `openapi-generator kotlin`: cookie-security → query-параметр, `$schema` → `dollarSchema` —
   поэтому models-only + пост-процессинг; спека Bearer-ветки генерируема.
9. SwiftLint `trailing_comma` против `swift-format` `TrailingComma` — выключить правило SwiftLint.
10. `*.hprof` после OOM — `-XX:HeapDumpPath=build/` и `.gitignore`.
11. Roborazzi-эталоны зависят от JDK/платформы — toolchain пинится, порог различий подбирается.
12. Никаких baseline'ов lint/detekt в свежем шаблоне — фикстуры должны краснеть.
