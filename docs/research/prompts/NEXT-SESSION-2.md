# Промпт сессии 2: продолжение сборки `mobile-template` (волны 2–4)

Дата составления: 2026-09-17. Для основной сессии Claude Code (Fable) в
`/home/mm/git/stit-core/mobile-template/`. Сессия 1 закрыла волну 1 (каркас и Linux-гейт, фаза 2
плана); эта сессия ведёт волны 2–4 до Definition of Done. Всё, что не переопределено здесь, берётся
из `NEXT-SESSION.md` — он остаётся источником оркестрации (§2), промптов агентов (§3), протокола
проверки (§4), стандарта качества (§5), тестовой политики (§6), запретов (§7) и ловушек (§8).

Требование владельца неизменно: **красиво, качественно, без замудрёностей — ровно, понятно,
читаемо; тесты очень важны.**

---

## 0. Как пользоваться этим файлом

1. Прочитать: `docs/research/DECISION.md` → `docs/research/08-template-design.md` (целиком) →
   `docs/research/prompts/NEXT-SESSION.md` (целиком) → этот файл → `.claude/plans/roadmap.md`
   (секция `build-mobile-template-v1`: закрытые атомы A1–A2, открытые A3–A10, чекпоинт волны 1).
   Отчёты 06/07/01/02/03 — по ссылкам из промптов агентов.
2. Прочитать каркас глазами — он небольшой: `Makefile`, `scripts/tools/check.sh`,
   `build-logic/convention/src/main/kotlin/*.kt`, `shared/build.gradle.kts`,
   `androidApp/app/build.gradle.kts`, `config/detekt/detekt.yml`, `.gate-fixtures/NF-01/fixture.sh`.
   Это форма, которую агенты волн 2–4 обязаны продолжать, а не переизобретать.
3. Прогнать §5 этого файла (машина и гейт) до запуска агентов — убедиться, что дерево зелёное на
   этой машине сегодня.
4. Дальше — по `NEXT-SESSION.md` §0 п. 3–5: волна 2 (B1 ∥ B3), волна 3 (B2 ∥ B4), волна 4 (проверка,
   dry-run обвязки, отчёт). Промпты агентов — `NEXT-SESSION.md` §3.2–§3.5 дословно, блоки «Состояние
   репозитория на момент запуска» — §4 этого файла (готовы к вставке; дополнить свежими цифрами).

## 1. Состояние репозитория после сессии 1

- **Коммиты** (`git log --oneline`): `docs(research): …` (ресёрч и spike'и), `meta(plan): план сборки`,
  `feat(repo): каркас и Linux-гейт` (волна 1, агент A, проверено основной сессией),
  `meta(plan): чекпоинт волны 1 и промпт сессии 2` (roadmap + этот файл). Дерево чистое, ветка `main`, хуки установлены
  (`git config core.hooksPath scripts/git-hooks`).
- **Дерево** (08 §2.1, фаза 2): корень Gradle (`settings.gradle.kts` с 13 модулями, `build.gradle.kts`
  только `apply false` + `stit.quality`, `gradle.properties` = 06 §8.2 + `HeapDumpPath=build/`,
  `gradle/libs.versions.toml` — все версии и SDK-уровни, wrapper 9.7.1 + sha256); `build-logic/convention`
  — 11 плагинов `stit.*` (≤ 84 строк каждый, паттерн AGP 9 через `ApplicationExtension`/`LibraryExtension`,
  общее — `tech/stit/convention/{AndroidDsl,Kover,Libs}.kt`); `shared/` (androidLibrary + `withHostTest`,
  jvm, iosArm64, iosSimulatorArm64, framework `Shared` static, BCV JVM + klib, дампы в `shared/api/`;
  по одной заглушке `AppEnv`/`Platform` с тестами в commonTest/jvmTest/androidHostTest);
  `androidApp/{app, core/{designsystem,ui,platform,navigation,testing}, feature/{shell,auth,profile,todos,settings}, lint}`
  — минимальные модули, `app` с flavors `env = dev|staging|prod|demo` из `config/env/*.properties` → `BuildConfig`,
  подпись release из `STIT_KEYSTORE_*` (иначе debug-ключ, R8 проверяется без секретов), Konsist
  `ArchitectureTest` (4 правила), `MainActivityTest` на Compose Test v2, `StitTheme` + Roborazzi
  light/dark с эталонами, `StitIssueRegistry` без детекторов; `config/{detekt,lint,env}`;
  `Makefile` (группы LOOP/GATE/KMP/MAC/RELEASE/SETUP/INTEL/META), `scripts/tools/*` (12 скриптов:
  `check.sh` с `--only` и SKIP-блоком, `gradle-step.sh`, `swift-step.sh`, `doctor.sh`, `gate-selftest.sh`,
  `kmp-check.sh`, `coverage-floor.sh`, `mac.sh`, `status.sh`, `version-from-tag.sh`, `android-skills.sh`),
  `scripts/release/{preflight,build-android}.sh`, `scripts/git-hooks/{commit-msg,pre-commit,pre-push}`,
  `.gate-fixtures/NF-01…10/fixture.sh`, `AGENTS.md` (3 строки), заглушка `CLAUDE.md` (5 строк),
  `.xcode-version` 27.0, `.swift-version` 6.4, `local.properties.example`.
- **Гейт на этой машине** (агент A + проверка основной сессии, тёплый `~/.gradle`, машина в swap):
  `make doctor` OK; `make check` → 0 с SKIP-блоком macOS: холодный (чистые `build/`) 104 с, тёплый
  19–21 с (цель ≤ 15 с не достигнута: 12 вызовов Gradle по 1–2 с и `spotlessCheck` 5 с при полностью
  up-to-date дереве; на незанятой машине ожидается ≈ 10 с — измерить в сессии 2 и, если > 15,
  объединить дешёвые стадии в один вызов Gradle); `make check-fast` 7 с; `make kmp-check` 1 с;
  `make test` 3 с; `make build` 1 с; покрытие shared 100 % / Android 100 % (заглушки);
  `make gate-selftest` — 10 из 10 красных, дерево после самотеста без изменений;
  `GATE_RELEASE=1 make check` зелёный, но R8 занял 1 ч 45 м при zram-swap 100 % — на занятой машине
  `GATE_RELEASE=1` гонять только один раз в конце волны.
- **Открытые вопросы плана закрыты:** Q1 — SKIE 0.10.14 поддерживает Kotlin ≤ 2.4.10, с 2.4.20 не
  собирается («SKIE 0.10.14 does not support Kotlin 2.4.20») → SKIE не подключён, фасад для Swift
  плоский по правилу `kmp-public-api`, строка в `## Backlog` (подключить при релизе SKIE с
  поддержкой 2.4.x); Q2 — BCV 0.18.2 на Linux и дампит, и проверяет klib для `iosArm64`/
  `iosSimulatorArm64` (`shared/api/shared.klib.api`), строгий режим включается `-Pstit.klibStrict=true`
  (плагин `stit.kmp.library` читает свойство) — его гоняет `check-mac`.
- **Что из spike перенесено как есть:** конфиг Gradle, `.editorconfig` (+ секции swift/Makefile),
  `Theme.kt`, ресурсы app, `gradle-step/doctor/check` (расширены). **Изменено осознанно:** пакеты
  `tech.stit.mobile` (shared) и `tech.stit.mobile.android.*` (androidApp); flavors во всех
  Android-модулях (единые имена тасков `testDevDebugUnitTest`, `lintDevDebug`); Kover-вариант `gate`
  (= devDebug) с агрегацией в `:androidApp:app`; detekt читает весь `src/` (иначе `commonMain` без
  анализа); `MagicNumber` ловит сырые цвета в композаблах; `sort-dependencies` слит в `fmt` и стадию
  `format kotlin`; Android-скиллы ставятся `make setup` в `.claude/skills/<id>/` (7 каталогов в
  `.gitignore`); AGP доустановил `platforms/android-37.0` рядом с 37.2.
- **Точечные замечания к каркасу** (не блокируют; закрыть по ходу волн): `Makefile run-android`
  хардкодит applicationId `tech.stit.mobile.debug` (B2); список Android-скиллов продублирован в
  `.gitignore` и `scripts/tools/android-skills.sh` — зафиксировать в `docs/contributing.md` как парное
  место правки (B3); `status.sh` печатает секции Branch/Working tree/Gate/Modules/Plan/Recent commits/TODOs
  — секции Platforms/Mac/Critical/Handoffs/Strategy из 08 §3.4 добавляет B3 вместе со скиллом `/smartdev`;
  `themes.xml` — `android:Theme.Material.Light.NoActionBar` (B2 решит вместе с per-app locales).
- **Артефакты для B1 в scratchpad основной сессии** (tmpfs, может очиститься — при отсутствии
  восстановить рецептом 07 §2.1): `research-artifacts/backend-openapi-3.1.yaml` — дамп текущего
  контракта `go-backend-template` (cookie-only, 743 строки, 11 путей `/api/auth/*`, `/api/users*`),
  `openapidump-main.go` — образец дампера. В `go-backend-template` `api/openapi.yaml` ещё нет (B-12
  не сделан), `cmd/openapidump` тоже.

## 2. Что осталось (Definition of Done — `NEXT-SESSION.md` §1)

- **Волна 2:** B1 — `shared/` целиком (core, контракт API + кодоген моделей, сущности, презентеры
  8 фич, `SharedContainer`, `mock/`, тесты ≥ 80 %, NF-11…16); B3 — `CLAUDE.md`, 23 правила,
  `settings.json`, 9 скиллов, агенты, `docs/**`, `README.md`, `LICENSE`, `SECURITY.md`.
  Коммиты `feat(shared): …`, `docs(harness): …`.
- **Волна 3:** B2 — `androidApp/` с 8 фичами, Nav3, Hilt, `demo`-flavor, `:lint`-детектор, Konsist,
  Roborazzi на все состояния, `i18n/deeplinks/tokens-check`; B4 — `iosApp/` Linux-часть (`Packages/Pure`,
  SwiftLint/swift-format, `Tools/archlint`, каркас по канону 02 без компиляции, NF-20…24).
  Коммиты `feat(android): …`, `feat(ios): …`.
- **Волна 4:** протокол §4 целиком, dry-run обвязки на scratch-копии (`/init-service →
  /stack-deep-dive (A) → /de-template` both и `--platform android`), `roadmap.md`, отчёт владельцу.
- **Не входит** (уже в `## Backlog` roadmap): фаза 5 на Mac, фаза 7, спайки S-1/S-10/S-15/S-12,
  cross-repo P0, SKIE (см. §1).

## 3. Уроки оркестрации сессии 1 (обязательны к учёту)

- **Запуск агентов.** `Agent` с явным `model: fable` падает на старте с `403 authentication_failed`.
  Запускать `subagent_type: general-purpose` **без параметра `model`** — субагент наследует модель
  сессии (Fable), это работает.
- **Обрывы API.** Субагент может упасть с `403` или `EAI_AGAIN` посреди работы; его дерево и логи
  остаются на диске, контекст сохраняется. **Возобновлять тем же id через `SendMessage`** («продолжай
  с того места…» + факты: что уже в дереве, что показал `git status`, что в `build/gate-logs/check.last`),
  а не запускать нового агента — новый начнёт с чтения документов заново (≈ 20 минут).
- **Промежуточные отчёты.** Агент, запустивший Gradle в фоне, регулярно «завершается» с текстом «жду
  завершения цепочки» — это interim-результат, агент возобновится сам; вмешиваться не нужно.
- **Один Gradle-агент.** Правило §2.2 подтверждено: демоны Gradle + Kotlin занимают 3–4 ГБ RSS при
  свободных 2–4 ГБ; параллельный Gradle уронит машину в swap. B3 и B4 Gradle не запускают.
- **Наблюдение.** Полезен `Monitor` с редкими событиями (раз в 60 с): содержимое `build/gate-logs/`
  (появление логов стадий = прогресс), `check.last`, RSS демонов, `MemAvailable < 900 MB`, `*.hprof`.
  Транскрипт агента (`tasks/<id>.output`) не читать — переполнит контекст.
- **Время.** Волна 1 заняла ≈ 3,5 часа стены (чтение документов ≈ 20 мин, сборка каркаса ≈ 1,5 ч,
  прогоны гейта и самотеста ≈ 1,5 ч при холодных кешах и занятой машине). Планировать волну 2 как
  ≥ 4 часов, волну 3 — ≥ 5 часов.
- **Форма фикстур.** `.gate-fixtures/NF-NN/fixture.sh` объявляет `EXPECT` (имя стадии `check.sh`),
  `TOUCHES` (пути) и функцию `apply`; `gate-selftest.sh` делает бэкап `TOUCHES`, применяет, гоняет
  `check.sh --only "$EXPECT"`, откатывает из бэкапа (без git — работает и на грязном дереве).
  Новые фикстуры B1 (NF-11…16), B2 (NF-17…19 при необходимости) и B4 (NF-20…24) — той же формы;
  для новой стадии — сначала добавить её в `stage()` в `check.sh`.
- **Стадии гейта.** Новые стадии добавляются в `stage()` и в массив `STAGES` в `check.sh` (B1:
  `gen-check`, `mock-check` при отдельной стадии; B2: `tokens-check`, `i18n-check`, `deeplinks-check`;
  B4: `swiftlint`, `archlint`, `swift pure build+tests`, `coverage swift` через `swift-step.sh` — он
  сейчас честно печатает SKIP, пока нет `iosApp/`). Порядок — дешёвое первым.

## 4. Блоки «Состояние репозитория на момент запуска» для агентов волн 2–3

Вставлять после дословного промпта агента из `NEXT-SESSION.md` §3.x. Общая часть — для всех:

- Тулчейны (не качать, `apt`/`sudo` нет): `JAVA_HOME=/home/mm/.cache/stit-mobile-research/jdk/jdk-21.0.12.1+1`
  (Temurin 21; системный `/usr/bin/java` — только JRE без javac);
  `ANDROID_HOME=/home/mm/.cache/stit-mobile-research/android-sdk` (`platforms/android-36`, `platforms/android-37.2`,
  `build-tools/36.0.0`, `37.0.0`, `platform-tools`, `cmdline-tools/latest/bin/android`); Swift 6.4 —
  `/home/mm/.cache/stit-mobile-research/swift/swift-6.4.0-RELEASE-debian13/usr/bin` (`swift`, `swift-format`);
  SwiftLint 0.65.1 — `/home/mm/.cache/stit-mobile-research/swiftlint/swiftlint`; openapi-generator —
  `/home/mm/.cache/stit-mobile-research/openapi-generator/openapi-generator-cli-7.25.0.jar`; Gradle-кеши `~/.gradle`
  (тёплые), `~/.konan` (Kotlin/Native 2.4.20 prebuilt). Go 1.26 есть в системе (`go version`).
- Каждый вызов Bash — новый shell без сохранённого окружения: экспортируй `JAVA_HOME`, `ANDROID_HOME` и `PATH`
  в каждой команде (или используй gitignored `local.properties` с `sdk.dir`). Пути этой машины в файлах
  репозитория не хардкодить.
- Машина: Debian 13, 20 CPU, 15 ГБ RAM, свободно ≈ 3–4 ГБ, swap занят. Один `./gradlew` за раз, в конце —
  `./gradlew --stop`. Первые прогоны медленнее цифр 06 — это не ошибка.
- Свой scratchpad: `/home/mm/.cache/tmp/claude-1000/-home-mm-git-stit-core-mobile-template/c0b195ef-0f24-44f2-967c-c50d3796ac94/scratchpad/<agent>/`
  (tmpfs ≈ 2,9 ГБ — кеши сборок туда не класть).
- `.claude/plans/roadmap.md` ведёт основная сессия — не трогай. Агенты не коммитят.
- Финальный ответ прочитает основная сессия и прогонит протокол проверки NEXT-SESSION.md §4: перечисли точные
  команды с env, которыми получен зелёный результат, и всё, что правил в общих файлах.

### 4.1 B1 (`shared/`, волна 2, Gradle-агент)

- `git`: ветка `main`, коммиты: ресёрч → план → `feat(repo): каркас и Linux-гейт` (агент A, проверено
  основной сессией). Дерево чистое. Что зелёное — §1 этого файла.
- Твоя зона: `shared/**`, `api/**`, `mock/**`, `config/openapi/**`, `scripts/tools/{gen,gen-check,mock-check}.sh`,
  строки `shared`-зависимостей в `gradle/libs.versions.toml`, `.gate-fixtures/NF-11…16`. Вербы `gen`, `gen-check`,
  `mock-check` в `Makefile` и их стадии в `scripts/tools/check.sh` добавляешь ты (точечно, в существующие
  группы, в стиле соседних строк) — и перечисляешь эти правки в финальном ответе. Параллельно работает
  агент B3 (обвязка и docs: `CLAUDE.md`, `.claude/**`, `docs/**` кроме `docs/research`, `README.md`) — его
  файлы не трогай; Gradle гоняешь только ты.
- Спека: в `go-backend-template` `api/openapi.yaml` ещё нет (B-12 не сделан). Готовый дамп текущего
  контракта бэкенда (cookie-only, 743 строки, OpenAPI 3.1, `openapi-generator validate` чист) лежит в
  `/home/mm/.cache/tmp/claude-1000/-home-mm-git-stit-core-mobile-template/c0b195ef-0f24-44f2-967c-c50d3796ac94/scratchpad/research-artifacts/backend-openapi-3.1.yaml`
  (рядом — 3.0-даунгрейд и `openapidump-main.go` — образец 30-строчного дампера из другого проекта; Go 1.26.6
  есть, `go version`). Reference-спека `api/openapi.yaml` = этот дамп + Bearer-ветка auth (07 §1.3b:
  `X-Client`, `TokenPairBody`, `RefreshInputBody`, `/api/auth/sessions`) + `GET /api/app/requirements`
  (07 §4.3) + `PUT/GET/DELETE /api/v1/devices` (07 §6.1) + todos как reference-ресурс; дефекты B-13…B-15
  (`$schema`, `data: [array, null]`, `ErrorModel.code`) — чинить пост-процессингом в `gen.sh`, а не
  правкой спеки руками, если только это не reference-часть, которой в дампе нет.
- `openapi-generator` вызывается напрямую jar'ом (`java -jar …/openapi-generator-cli-7.25.0.jar`), путь —
  через env `OPENAPI_GENERATOR_JAR` с проверкой в `make doctor`/`gen.sh` и next-step, если не задан.
- BCV на Linux дампит и проверяет klib (Q2 закрыт, §1): при каждом изменении публичного фасада — `make api-dump`
  (обновляет оба дампа в `shared/api/`), `apiCheck` — стадия `shared api` в гейте; строгая klib-валидация —
  только в `check-mac`. Дампы коммитит основная сессия вместе с твоей волной.

### 4.2 B3 (обвязка и docs, волна 2, без Gradle)

- `git`: ветка `main`, коммиты: ресёрч → план → `feat(repo): каркас и Linux-гейт` (агент A, проверено
  основной сессией). Дерево чистое. Что зелёное — §1 этого файла.
- Твоя зона: `CLAUDE.md` (заглушку A заменяешь), `AGENTS.md`, `README.md`, `LICENSE`, `SECURITY.md`,
  `.claude/**` кроме `.claude/plans/roadmap.md`, `docs/**` кроме `docs/research/**`. Параллельно работает
  агент B1 (`shared/**`, `api/**`, `mock/**`, `config/openapi/**`, `scripts/tools/{gen,gen-check,mock-check}.sh`);
  Gradle гоняет только он — ты Gradle не запускай (проверяй `make check` только если это не Gradle-стадии:
  например, свой `scripts/tools/links-check.sh` для docs; полный `make check` прогонит основная сессия).
- Реальные вербы `Makefile` и скрипты `scripts/tools/*` — уже в дереве (агент A): читай `Makefile` и
  `scripts/tools/check.sh` и ссылайся только на существующие имена (в т. ч. `kmp-check`, `cover-check`).
  Вербы, которые появятся у B1 (`gen`, `gen-check`, `mock-check`, `api-dump`, `api-check`) и B2
  (`i18n-check`, `deeplinks-check`, `tokens-check`, `gen-tokens`, `screenshots`) — упоминай по именам из
  NEXT-SESSION.md/08 §4.1 и перечисли в финальном ответе как «зависит от кода B1/B2».
- Образцы формы (read-only): `/home/mm/git/stit-core/go-backend-template/{CLAUDE.md,README.md,SECURITY.md,LICENSE,docs/**,.claude/**,scripts/tools/validate-plan.sh}`,
  `/home/mm/git/stit-core/spa-frontend-template/{CLAUDE.md,README.md,docs/**,.claude/**}`,
  `/home/mm/git/stit-core/telegram-bot-template/CLAUDE.md`,
  `/home/mm/git/stit-core/business-strategy-template/docs/projects/_template/{telegram-bot-flow,fsd,screens}/` и
  `.claude/skills/{fsd-deep-dive,fsd-audit,telegram-bot-flow-deep-dive}/SKILL.md`,
  `/home/mm/git/stit-core/corp-infra-template/docs/ecosystem.md`.
- `/plan` — по родству «идентично» Go-шаблону: `SKILL.md` + `references/questions.md` переносятся
  целиком (с заменой Go-специфики в примерах на мобильную), `scripts/tools/validate-plan.sh` — тоже
  (`scripts/tools/validate-plan.sh` и `links-check.sh` — единственные скрипты в твоей зоне).
  `scripts/git-hooks/pre-commit` (общий файл агента A) сейчас проверяет только секреты, Spotless/detekt и
  swift-format — добавь в него точечно вызов `validate-plan.sh` при изменении `.claude/plans/roadmap.md`
  (форма — `go-backend-template/scripts/git-hooks/pre-commit`) и сообщи об этой правке в финальном ответе.
- Проверь свои ссылки скриптом (`scripts/tools/links-check.sh`: все относительные ссылки `[…](path)` в
  `docs/**`, `README.md`, `CLAUDE.md`, `.claude/**` резолвятся в существующие файлы; ссылки на будущие
  файлы B1/B2 — перечисли в финальном ответе, не оставляй битыми без пометки).

### 4.3 B2 (`androidApp/`, волна 3, Gradle-агент) — дополнить по итогам волны 2

- Что даёт `shared/` (по финальному ответу B1): пакеты, `SharedContainer`, презентеры, `MockTransport`,
  фикстуры `mock/`, публичный API из `shared/api/`. Что даёт B3: имена правил/скиллов, на которые
  ссылаться.
- Точечные замечания к каркасу, которые закрывает B2: `Makefile run-android` хардкодит
  `tech.stit.mobile.debug` — вывести applicationId из одного места (например, `scripts/tools/app-id.sh`
  читает `androidApp/app/build.gradle.kts`/`config/env`); `androidApp/app/src/main/res/values/themes.xml`
  наследует `android:Theme.Material.Light.NoActionBar` — для per-app locales ниже API 33 нужен
  AppCompat (01 §3.5), решить в `settings`; `robolectric.properties` пинит `sdk=35` (WHY в файле) —
  не менять без перезаписи эталонов.
- Ключевые файлы каркаса, которые B2 продолжает: `build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt`
  (зависимости фичи и запрет feature → feature), `AndroidApplicationConventionPlugin.kt` (flavors из
  `config/env`, Kover-вариант `gate` с полом 70), `androidApp/app/src/test/.../ArchitectureTest.kt` (Konsist),
  `androidApp/lint/.../StitIssueRegistry.kt` (реестр без детекторов — добавить `DesignSystemDetector` + тест).

### 4.4 B4 (`iosApp/`, волна 3, без Gradle) — дополнить по итогам волны 2

- Swift 6.4 и SwiftLint — пути в общей части; `scripts/tools/swift-step.sh` и `coverage-floor.sh`
  уже есть (SKIP, пока нет `iosApp/`); `Makefile` уже содержит `IOS_SOURCES`, вербы `fmt`/`fmt-check`
  с Swift-частью и `mac-*`; `scripts/tools/mac.sh` ждёт `iosApp/iosApp.xcodeproj`, схему `Dev`,
  планы `Unit`/`Snapshot`/`UI`, `iosApp/.test-simulator`.
- Спайк `docs/research/spikes/swift-linux/` — `.swift-format`, `.swiftlint.yml`, `Package.swift`,
  Swift Testing (прогнан, зелёный).

## 5. Протокол проверки основной сессией — команды

Окружение (каждый вызов Bash — новый shell):

```bash
export JAVA_HOME=/home/mm/.cache/stit-mobile-research/jdk/jdk-21.0.12.1+1
export ANDROID_HOME=/home/mm/.cache/stit-mobile-research/android-sdk
export SWIFT_TOOLCHAIN=/home/mm/.cache/stit-mobile-research/swift/swift-6.4.0-RELEASE-debian13/usr/bin
export PATH="$SWIFT_TOOLCHAIN:/home/mm/.cache/stit-mobile-research/swiftlint:$JAVA_HOME/bin:$PATH"
export OPENAPI_GENERATOR_JAR=/home/mm/.cache/stit-mobile-research/openapi-generator/openapi-generator-cli-7.25.0.jar
cd /home/mm/git/stit-core/mobile-template
make doctor && make check && make check && make check-fast && GATE_RELEASE=1 make check \
  && make gate-selftest && git status --short && ./gradlew --stop && free -g
```

`GATE_RELEASE=1` (R8) на занятой машине идёт долго (у сессии 1 — 1 ч 45 м при zram 100 %): гонять один
раз в конце каждой волны, а не после каждой правки; остальное — после каждой волны и в конце сессии.

Дальше — `NEXT-SESSION.md` §4 п. 2–7 (структура, читаемость, тесты, обвязка, dry-run, коммит).
Хуки ставятся `make setup` (`core.hooksPath = scripts/git-hooks`): commit-msg проверяет Conventional
Commits, pre-commit — Spotless/detekt по staged, pre-push — `GATE_RELEASE=1 make check`.
