---
name: triage-failure
description: Red-арм cockpit'а — структурированный разбор красного теста / стадии гейта / сборки / дрейфа (gen-check, api-check, kmp-check, screenshots, coverage). TDD-first для багфиксов (RED→GREEN). smartdev чейнит сюда на ATTENTION; багфикс идёт сюда, а не в plan. Применяй, когда что-то падает или не собирается — красный тест, стадия гейта, сборка, дрейф — юзер приносит лог или стек либо просит разобрать причину.
---

# Skill: triage-failure

Дисциплинированный разбор падения. Цель — найти **корневую причину**, не замазать симптом, и
зафиксировать инвариант тестом.

**Red-арм cockpit'а:** `smartdev` переключает сюда, когда `make status`/`make check` дал
`ATTENTION:` или задача — багфикс. Багфикс идёт сюда, а не в `plan`: ему нужна строгая петля
RED→GREEN→REFACTOR, а не атомы плана.

## Когда применять

- «Тест X падает», «стадия `detekt` красная», «`kmp-check` FAIL», юзер кидает лог/стек.
- `make gate-selftest` показал `GREEN?` (фикстура не покраснела — гейт ослеп).

## Когда не применять

- Юзер просит новую фичу — это `plan` или прямая работа.
- Опечатка, видная с первой строки, — просто чини.

## Категории падений

| Тип | Признаки | Где смотреть |
|---|---|---|
| **Формат** | `FAIL: format kotlin` / `format swift` | `make fmt`, затем diff — единственные автофиксеры |
| **detekt / Lint / SwiftLint / archlint** | `FAIL: detekt`, `android lint`, `swiftlint`, `archlint` | `build/gate-logs/{detekt,android_lint,swiftlint,archlint}.log`; правило и файл в первых строках выжимки |
| **Паритет** | `FAIL: tokens-check` / `i18n-check` / `deeplinks-check` / `capability-check` | `build/gate-logs/{tokens_check,i18n_check,deeplinks_check,capability_check}.log`: `make gen-tokens` / ключ в `values-ru/` или `core_ui_error_<code>` / тест в `DeepLinkParserTest`, плейсхолдер `${DEEP_LINK_HOST}` / ключ задачи, ветка `backgroundRunner(id)`, идентификатор в `BGTaskSchedulerPermittedIdentifiers`, эталон и тест состояния виджета, строка ответа действия в обоих `values` |
| **Build** | `e: file.kt:12:5 Unresolved reference`, `Inconsistent JVM-target`, `Cannot add extension 'kotlin'` | `build/gate-logs/assembleDevDebug.log`; первая `e:` обычно главная |
| **Unit test** | `FAILED` в `shared jvm tests` / `android unit tests` | `FORMAT=detailed make test`; точечно `./gradlew :shared:jvmTest --tests '*XTest*'` |
| **Konsist** | `ArchitectureTest > … FAILED` | нарушена матрица `docs/reference/architecture.md` или именование |
| **Скриншоты** | `screenshots` FAIL, файлы `*_compare.png` | открой compare: изменение намеренное? |
| **Покрытие** | `coverage kotlin` / `coverage swift` FAIL | `make cover` — чего не хватает; тест удалён или логика без теста |
| **KMP** | `kmp cinterop-free` / `klib iosArm64` FAIL | platform API в `commonMain`, cinterop, зависимость без iOS-таргета |
| **Фасад** | `shared api` FAIL (`apiCheck`) | фасад изменился без `make api-dump` (решение в плане?) |
| **Дрейф контракта** | `gen-check` / `mock-check` FAIL | модели правлены руками / lock устарел / фикстура не по схеме |
| **Mac** | `check-mac` FAIL или `mac.last` устарел | `build/gate-logs/<стадия>.log`, `docs/reference/mac-host.md` |

## Шаги

### 1. Воспроизвести минимально

- Какая стадия/команда? Локально или только на Mac? На какой ветке/коммите?
- Гоняй точечно, не весь гейт: `bash scripts/tools/check.sh --only "<стадия>"`,
  `./gradlew :androidApp:feature:todos:testDevDebugUnitTest --tests '*TodosScreenTest*'`,
  `swift test --package-path iosApp/Packages/Pure --filter <Suite>`.

### 2. Прочитать лог стадии

Полный лог — `build/gate-logs/<стадия>.log` (`FORMAT=detailed` — длиннее выжимка, `VERBOSE=1` —
сырой вывод). В Gradle-логах причина часто в середине, после стека, а не в первой строке.

### 3. Сформулировать гипотезу

Одно предложение: «X падает потому что Y» — проверяемое. Примеры:
- «`AuthPluginTest` падает, потому что два параллельных 401 запускают два refresh — нет single-flight».
- «`kmp-check` красный, потому что в `commonMain` появился `java.io.File` из нового маппера».
- «`screenshots` красный, потому что токен `color.action.bg` изменён — эталон устарел намеренно».

### 4. TDD для багфиксов (обязательно)

Если это **баг** (а не формат/дрейф/конфиг):

1. **Напиши падающий тест**, воспроизводящий баг — там, где живёт логика (`commonTest` для
   презентера/парсера/auth; Robolectric для экрана; `androidHostTest` для actual'ов).
   Тест должен падать **по той же причине**, что и баг.
2. Точечный прогон → red. Подтверди, что red — твой.
3. **Только теперь** правь продакшен-код.
4. Точечный прогон → green; `make test` → всё зелёное.
5. Коммит: тест и фикс одним `fix(<scope>): <one-liner>`.

### 5. Не-баг-флоу — каталог мобильных провалов

| Симптом | Причина | Действие |
|---|---|---|
| `OutOfMemoryError: Metaspace` в Lint | `MaxMetaspaceSize < 1g` | `gradle.properties` не трогать; `./gradlew --stop`, освободить RAM |
| `SKIPPED` у `link*Framework*`, каталога klib нет | это Linux — framework только на Mac | логика проверяется `make kmp-check`; UI — `make check-mac` |
| `no such module 'SwiftUI'`/`'UIKit'` на Linux | таргет не в `Packages/Pure` или `import UIKit` в `Pure` (NF-22) | UI-код — в `Core`/`Features`, проверяется на Mac; на Linux — `Pure` + SwiftLint + archlint |
| `InaccessibleObjectException` в Robolectric | нет `--add-opens` | `configureUnitTests`/JVM-args плагина `stit.android.library` |
| `*_compare.png` в дереве | изменился рендер | намеренно → `make screenshots-record`; нет → чини код |
| `Inconsistent JVM-target` | `jvmTarget` ≠ Java | `JvmLibraryConventionPlugin`: JVM 17 (NF-08) |
| `spotlessCheck` diff | формат | `make fmt` |
| `MissingApplicationIcon` и др. Lint | манифест/ресурсы; `warningsAsErrors` | чинить, не baseline (baseline запрещён) |
| `apiCheck failed` | фасад изменился | решение в плане → `make api-dump`, дампы в коммит |
| `gen-check` drift | спека/lock/модели разошлись | `make gen`; несовместимо → BACKEND-HANDOFF |
| `mock-check` / `FixturesTest` | фикстура не по схеме | обнови `mock/fixtures/*` (NF-16) |
| `swift format lint` vs SwiftLint спорят | `trailing_comma` не выключен | `.swiftlint.yml`: `disabled_rules: [trailing_comma]` |
| `xcodebuild: Signing … requires a development team` | Mac-хост без профиля | `CODE_SIGNING_ALLOWED=NO` для build; профили — `docs/reference/mac-host.md` |
| `Hilt: [Dagger/MissingBinding]` | нет биндинга `PlatformDeps` | биндинг в `androidApp/app` (`SharedModule`) |
| K/N download 60+ с на первом `kmp-check` | прогрев `~/.konan` | не ошибка; повторить |
| `RuStore: 4xx на publish` | JWE-токен живёт 900 с, `timestamp ± 60 с` | перевыпустить токен, проверить часы |
| `Cannot add extension with name 'kotlin'` | применён `kotlin-android` | убрать; AGP 9 со встроенным Kotlin |
| `Unresolved reference: hiltViewModel` | старый импорт | `androidx.hilt.lifecycle.viewmodel.compose` |
| `createComposeRule` deprecated | Compose test v1 | `androidx.compose.ui.test.junit4.v2` (detekt ловит) |

### 6. Если корневая причина в чужом коде

- Дефект спеки бэкенда (`$schema`, `data: [array, null]`, нет `ErrorModel.code`) → пост-процессинг в
  `scripts/tools/gen.sh` + BACKEND-HANDOFF, не правка `api/openapi.yaml` руками.
- Баг библиотеки (Roborazzi, Konsist, compose-rules) → не `@Ignore`; workaround с WHY-комментарием
  и строка в `.claude/plans/backlog.md`.

### 7. Когда застрял

Признаки: 2+ попытки не помогли; каждая ломает другое; гипотеза не сужается. Действия:
1. Откатись (`git stash`) к чистому red-state.
2. Сформулируй вслух юзеру: «Думаю X, проверка Y показывает Z, не хватает Q».
3. Делегируй subagent `explorer` — «найди все места, где …».
4. Если всё равно — план через `plan`: research-spike.

### 8. Верификация фикса

Точечный прогон → `make test` → `make check`; для флаки — `--rerun-tasks` × N. Перед коммитом
— `make check`; если тронут `iosApp/**` — `make check-mac`.

## Anti-patterns

- ❌ «Перепиши тест, чтобы он проходил» — сокрытие, не фикс.
- ❌ `@Ignore`/`.disabled` для флаки — корневая причина уйдёт в прод.
- ❌ Baseline для Lint/detekt/SwiftLint — долг вместо фикса.
- ❌ `make screenshots-record` «чтобы позеленело» без просмотра `*_compare.png`.
- ❌ `--no-verify` — заблокирован `permissions.deny`: хуки — часть гейта.
- ❌ Фикс без теста для багфикса. TDD не опциональна для багов.
- ❌ «Заодно» расширить скоуп — багфикс маленький; большое — отдельный план.
