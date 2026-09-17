# Contributing — работа с шаблоном через Claude Code

**[META]** Для контрибьютора *самого шаблона*: как устроена `.claude`-система, что
enforce'ится детерминированно, как вмешаться. (Потребителю шаблона — `getting-started.md`.)

## Что где лежит и когда грузится

| слой | где | когда грузится | сила |
|---|---|---|---|
| Ядро + роутер | `CLAUDE.md` (≤ 200 строк); `AGENTS.md` — указатель для JetBrains/Xcode-агентов | всегда | advisory |
| Path-scoped правила | `.claude/rules/*.md` (23) с `paths:` | когда matching-файл в контексте | advisory |
| Skills (процедуры) | `.claude/skills/<name>/SKILL.md` (+ `references/`, `state/`) | по `description` или `/<name>` | procedural |
| Официальные Android-скиллы | `.claude/skills/<id>/` — ставит `make setup` (`scripts/tools/android-skills.sh`), gitignored | по `description` | procedural |
| Subagent'ы | `.claude/agents/{explorer,reviewer}.md` | когда основной агент делегирует | role-specific |
| Settings / permissions | `.claude/settings.json` | старт сессии | детерминированно (deny) |
| План | `.claude/plans/roadmap.md` (единый, `## Active` / `## Backlog`) | читает `/smartdev`, `/plan` | процесс |

Progressive disclosure: на холодном старте — имена и описания; тела skills/rules и `references/`
грузятся по необходимости. **Ключ path-scoping — `paths:`** (не `globs:`); правило без `paths:`
грузится всегда — не заводить.

**Парное место правки — список Android-скиллов**: `SKILLS=` в `scripts/tools/android-skills.sh`
и каталоги `.claude/skills/<id>/` в `.gitignore` — менять одним коммитом.

## Цикл одной задачи

```
prompt → /smartdev (make doctor → make status, roadmap, чейнит режим)
       → /plan (planning-first; iOS-атом = Mac-verify)
       → act (RED→GREEN→REFACTOR; сосед задет → /infra-handoff)
       → verify (make check-fast → make check; iOS — make check-mac)
       → review (опц. subagent reviewer)
       → commit (Conventional Commits; хуки)
```

Планирование — дефолт; прямая правка — только truly-atomic и багфиксы (`/triage-failure`).

## Кто что enforce'ит

| правило | механизм | где | стадия гейта |
|---|---|---|---|
| feature ↛ feature (Android) | `stit.android.feature` (`afterEvaluate`) — падение конфигурации | `build-logic/…/AndroidFeatureConventionPlugin.kt` | любой `./gradlew` |
| `commonMain` без `android./androidx./java./javax./platform.`; слои `core < entity < feature`, `feature/<a>` ↛ `feature/<b>`; презентеры — final без generic'ов, со `state`/`dispatch`; `State` — data, `Intent` — sealed; `SharedContainer` без generic'ов | Konsist `ArchitectureTest` | `shared/src/jvmTest/kotlin/tech/stit/mobile/ArchitectureTest.kt` (`jvmTest` получает `inputs.dir("src")`) | `shared jvm tests` |
| feature ↛ feature по импортам; `*ViewModel` только в `feature/*`; `*Screen` в файлах `*Screen.kt` | Konsist `ArchitectureTest` | `androidApp/app/src/test/…/architecture/ArchitectureTest.kt` | `android unit tests` |
| запрещённые импорты (`android.util.Log` — кроме `LogcatSink`, `java.util.Date`, `GlobalScope`, Material 2, Retrofit, `security-crypto`, Compose test v1); `TODO:`/`FIXME:`; сырые числа в композаблах; compose-rules | detekt `ForbiddenImport`, `ForbiddenComment`, `MagicNumber`, `Compose.*` | `config/detekt/detekt.yml` (`buildUponDefaultConfig`, `warningsAsErrors`) | `detekt` |
| формат Kotlin + порядок зависимостей | Spotless (ktlint 1.8, `.editorconfig`) + `checkSortDependencies` | `stit.quality` | `format kotlin` |
| платформенные ошибки, `HardcodedText`, `ResourceName`, проектные детекторы | Android Lint `warningsAsErrors` + `checkDependencies`, `:androidApp:lint` (`StitIssueRegistry`) | `config/lint/lint.xml`, `androidApp/lint/` | `android lint` |
| cinterop/CocoaPods в `shared`; klib реально собран | `scripts/tools/kmp-check.sh` (grep + каталог `shared/build/classes/kotlin/<target>/main/klib`) | `kmp-check.sh` | `kmp` |
| фасад `shared` не изменился без `api-dump` | BCV `apiCheck` (JVM + klib; strict — `-Pstit.klibStrict=true` в `check-mac`) | `stit.kmp.library` | `shared api` |
| визуальные регрессии всех состояний | Roborazzi `verifyRoborazziDevDebug` (эталоны `src/test/screenshots/`) | `stit.roborazzi` | `screenshots` |
| полы покрытия 80 / 70 / 70 | `:shared:koverVerify`, `:androidApp:app:koverVerifyGate` (вариант `gate` = devDebug), `coverage-floor.sh` (llvm-cov) | `stit.kmp.library`, `stit.android.application`, `scripts/tools/coverage-floor.sh` | `coverage kotlin`, `coverage swift` |
| R8 не ломает reflection/Hilt/serialization | `assembleProdRelease` при `GATE_RELEASE=1` (pre-push, `/release`) | `check.sh` | `assembleProdRelease` |
| модели == регенерация; lock == спека; фикстуры парсятся строгими моделями | `scripts/tools/gen-check.sh`, `mock-check.sh` (`FixturesTest`) | `make gen-check`, `make mock-check` | стадии контракта |
| формат Swift; границы пакетов; `print`/Combine/`URLSession`/сырые цвета/`@preconcurrency` без WHY | swift-format `--strict`; SwiftLint custom rules; `Tools/archlint` над `dump-package` | `iosApp/.swift-format`, `iosApp/.swiftlint.yml`, `iosApp/Tools/archlint` | Swift-стадии (`swift-step.sh`; SKIP, пока `iosApp/` нет) |
| гейт умеет краснеть | `make gate-selftest` — каждая `.gate-fixtures/NF-NN/fixture.sh` (`EXPECT`, `TOUCHES`, `apply`) обязана уронить свою стадию; откат из резервной копии | `scripts/tools/gate-selftest.sh` | самотест (NF-01…10 — Android/KMP-каркас) |
| Conventional Commits; секреты подписи не в коммите; формат/detekt staged; `validate-plan` для roadmap; `GATE_RELEASE=1 make check` перед push | git-хуки `commit-msg`, `pre-commit`, `pre-push` (ставит `make setup`; пропуск — `SKIP_PREPUSH=1`, не `--no-verify`) | `scripts/git-hooks/` | **детерминированно** |
| `--no-verify`, force-push, `reset --hard`, `apiDump`/`assembleRelease`/`keytool`/`sdkmanager` напрямую, правка `generated/`/`shared/api/`/`.pbxproj`/`build/`, чтение ключей и `.env.local` | `permissions.deny` | `.claude/settings.json` | **детерминированно** |
| структура плана (атомы с `Verify:`/`Зависит от:`, статусы, чекпоинты) | `scripts/tools/validate-plan.sh` | `pre-commit` при staged `roadmap.md` | детерминированно |
| ссылки в docs/обвязке резолвятся | `scripts/tools/links-check.sh` | `make links-check` | advisory |
| родство с экосистемой | `scripts/tools/kin-diff.sh` | `make kin-diff` | advisory (WARN) |
| соседи задеты → handoff; карта правится в strategy; логика в `shared`; iOS «готово» только после `check-mac` | правила + скиллы (`infra-impact`, `/infra-handoff`, `/plan` Mac-verify) | `.claude/rules`, `.claude/skills` | soft (дисциплина) |

Claude Code хуков (PreToolUse/PostToolUse/Stop) в шаблоне нет — критичные инварианты держат
git-хуки, `permissions.deny` и гейт; soft-guidance — `CLAUDE.md` и rules. Baseline'ов lint/detekt/
SwiftLint нет — фикстуры обязаны краснеть.

## Как добавить

| что | шаги |
|---|---|
| **Модуль Android** | `include(":androidApp:…")` в `settings.gradle.kts` → `build.gradle.kts` с плагином `stit.*` + `namespace` → строка в `docs/reference/architecture.md` → `make check` (Kover-агрегат подхватит листовой модуль сам) |
| **Фича** | запись в карте `mobile/<surface>/` (strategy) → `/scaffold feature` → `/scaffold screen` (Android + iOS) → скриншоты/снапшоты на состояния |
| **Kind stack** | форма в `docs/stack/_templates/<kind>.md` (frontmatter, `## Generates`) → строка в `docs/stack/README.md` → чек-лист в `.claude/skills/stack-deep-dive/references/field-checklists.md` |
| **Правило** | `.claude/rules/<name>.md` ≤ 60 строк, `name`/`description`/`paths:` → строка в таблице выше |
| **Скилл** | `.claude/skills/<name>/SKILL.md` (+ `references/`) — ссылки только на реальные вербы и пути; `make help` — источник вербов |
| **Стадия гейта** | `stage()` + массив `STAGES` в `scripts/tools/check.sh` (дешёвое первым; Swift после Kotlin) → верб в `Makefile` в своей группе `##@` → негативная фикстура `.gate-fixtures/NF-NN/` → `make gate-selftest` красный |
| **Негативная фикстура** | `.gate-fixtures/NF-NN/fixture.sh`: первая строка `# NF-NN — <что ловит>`, `EXPECT` (стадия), `TOUCHES` (пути), `apply()`; нумерация: NF-01…10 каркас, NF-11…16 `shared`, NF-17…19 Android-проверки, NF-20…24 Swift |
| **Верб Makefile** | группа `##@`, комментарий `## …` одной строкой, реализация через `scripts/tools/*.sh` с `OK:`/`FAIL:` (`gradle-step.sh`/`swift-step.sh`), логи в `build/gate-logs/` |

## Обновление toolchain'ов

- Kotlin / AGP / KSP / Hilt / Compose BOM / Gradle — только `gradle/libs.versions.toml` (+ wrapper с
  `distributionSha256Sum`), парами (`kotlin`↔`ksp`↔`hilt`, `agp`↔`androidTools` = AGP + 23), latest
  stable с двухнедельной выдержкой; через `/plan`: `make check` + `make gate-selftest` (эталоны
  Roborazzi могут потребовать `make screenshots-record` — осознанно).
- Xcode / Swift — `.xcode-version`, `.swift-version`, `docs/stack/runtime.md` одним коммитом; Mac-хост
  обновляется первым; дедлайн App Store на iOS 27 SDK — апрель 2027.
- SDK Android — `compileSdk`/`compileSdkMinor`/`targetSdk` в `libs.versions.toml`; `make doctor`
  печатает, какую платформу доставить (`android --no-metrics sdk install …`; агент не качает).

## Course correction

| хочется | как |
|---|---|
| Прервать работу | `Esc` |
| План на одно сообщение | `/plan` |
| Заблокировать путь | `.claude/settings.local.json` → `permissions.deny` |
| После 2 неудачных попыток | `/clear` + переписать запрос; `/triage-failure` |
| Разовый контекст | `@path/to/file` |
| Откатить | `git restore` |

## Troubleshooting

| симптом | куда смотреть |
|---|---|
| Skill не активируется | `description` в frontmatter — конкретнее, «pushy», с триггер-фразами |
| Path-scoped rule не грузится | frontmatter `paths:` (не `globs:`!), glob совпадает с путём файла |
| Subagent ушёл надолго | `tools` в frontmatter — read-only; не давать ему Gradle |
| `CLAUDE.md` игнорируется | длина > 200 строк или конфликт инструкций между секциями |
| Коммит reject'нут | `scripts/git-hooks/commit-msg` — type ∈ {feat,fix,refactor,docs,test,chore,meta,perf,style}, subject ≤ 72 |
| `validate-plan` красный | структура секции плана — `.claude/skills/plan/SKILL.md § 3` |
| `links-check` красный | ссылка на несуществующий файл — чинить ссылку, не создавать заглушку |
| Гейт красный | `/triage-failure`, каталог мобильных провалов; `docs/reference/architecture.md#traps` |
| `make gate-selftest` → `GREEN?` | гейт ослеп: механизм отключён или фикстура перенацелилась на другой путь |
