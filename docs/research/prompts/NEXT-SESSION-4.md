# Промпт сессии 4: dry-run обвязки и хвосты волны 4 `mobile-template`

Дата составления: 2026-09-17. Для основной сессии Claude Code в
`/home/mm/git/stit-core/mobile-template/`. Сессия 3 закрыла раздел 0, волну 3 (B2 ∥ B4) и сверку docs;
владелец отложил dry-run обвязки (A9) и решил, что агентов дальше запускать на Opus (`model: opus`
в `Agent`). Всё, что не переопределено здесь, — `NEXT-SESSION.md` (§2 оркестрация, §4 протокол
проверки, §5 стандарт качества, §6 тесты, §7 запреты, §8 ловушки), `NEXT-SESSION-2.md` §3–§5,
`NEXT-SESSION-3.md` §1, §3 (уроки).

Требование владельца неизменно: **красиво, качественно, без замудрёностей — ровно, понятно,
читаемо; тесты очень важны.**

## 0. Как пользоваться этим файлом

1. Прочитать `docs/research/DECISION.md`, `NEXT-SESSION.md` §4–§8, этот файл, `.claude/plans/roadmap.md`
   (`## Active` пуст, всё незакрытое — `## Backlog`), `docs/getting-started.md`, `docs/stack/README.md`,
   `SKILL.md` скиллов `init-service`, `stack-deep-dive` (+ `references/draft-stable.md`), `de-template`
   (+ `references/{prune,platform}.md`), `scaffold` (+ `references/*.md`).
2. Прогнать `NEXT-SESSION-2.md` §5 (env + `make doctor && make check && make check-fast &&
   GATE_RELEASE=1 make check && make gate-selftest`) — baseline на этой машине.
3. Запустить агента dry-run (§3) на Opus; после него — сверка правок скиллов, коммит `docs(skills): …`
   или `fix(skills): …`, обновление roadmap, отчёт владельцу.

## 1. Состояние репозитория после сессии 3

- Коммиты сессии 3 (после `5734cb0`): `refactor(shared): @Throws для Swift, заголовки
  User-Agent/Accept-Language, B-20` → `feat(shared): ConnectivityState` → `feat(android): Compose-приложение
  — 8 reference-фич, Nav3, Hilt, demo, токены, гейт` → `feat(ios): SwiftUI-каркас — Pure, Core, Features,
  archlint, Swift-стадии гейта` → `docs(harness): …` (сверка docs ↔ код Android/iOS) → `meta(plan): …`.
- Гейт (протокол §5 после волны 3, эта машина): `make doctor` clean; `make check` → 0 с SKIP-блоком
  macOS, холодный 158 с, тёплый на живом демоне 20 с (33 строки, 21 стадия); `make check-fast` 6 с;
  `GATE_RELEASE=1 make check` (R8) 96 с; `make gate-selftest` — 24 из 24 красных за 104 с, дерево после
  без изменений. Покрытие: `shared` 96,7 %, Android 82,6 % (пол 70), Swift `Pure` 90 % (пол 70).
- `androidApp/`: `core:{designsystem,ui,navigation,platform,testing}`, `feature:{shell,auth,profile,
  todos,settings}`, `lint` (`DesignSystemDetector`); 64 Roborazzi-эталона; Konsist на 7 правил;
  `DemoFlowTest` на фикстурах `mock/`; `design/tokens.json` → `scripts/tools/gen-tokens.py` →
  `Tokens.kt`/`Tokens.swift`; стадии `tokens-check`/`i18n-check`/`deeplinks-check`; фикстуры NF-17…19.
- `iosApp/`: `Packages/Pure` (Linux: build+test+coverage), `Core` (DesignSystem, Routing per tab,
  Persistence, Platform, SharedBridge), `Features` (+ umbrella), `TestSupport`, `App`, `Config/*.xcconfig`,
  `TestPlans`, `Tools/archlint`, `.swiftlint.yml` с custom-правилами; стадии `swiftlint`/`archlint`/
  `swift pure build+tests`/`coverage swift`; фикстуры NF-20…24. Всё, что импортирует Shared/SwiftUI,
  не верифицировано на Mac (список — `## Backlog` roadmap, «Mac-verify»).
- Машина: тулчейны и env — `NEXT-SESSION-2.md` §4; один Gradle за раз; `voiceinput` закрывать при
  нехватке RAM (разрешено владельцем); watchdog-скрипт сессии 3 — в scratchpad, не в репо.

## 2. Уроки сессии 3

- Deny-правила `settings.json` на `api/openapi.yaml` действуют и на `sed -i` из Bash у агентов —
  правки reference-части спеки делает основная сессия.
- Kotlin/Native требует `CancellationException` в `@Throws` у suspend-функций (JVM молчит).
- `Agent` без `model` (Fable) — стабильно: B2 97 мин / 156 вызовов / 907 K токенов, B4 54 мин /
  99 вызовов / 847 K; параллельная пара «Gradle-агент + без-Gradle-агент» не конфликтует, если
  общие файлы (`Makefile`, `check.sh`) правятся точечно и перечитываются.
- Сверка docs ↔ код после каждой волны с кодом — обязательна (имена файлов designsystem, вербы,
  стадии, фикстуры расходились со спекой).
- Доработки агента через `SendMessage` тому же id дешевле нового агента (B4: Router per tab,
  `ConnectivityState`, импорты — 4 минуты).

## 3. Бриф агента dry-run (A9) — запускать на Opus

Ты — Fable-агент dry-run обвязки репозитория `mobile-template` экосистемы stit (волна 4, атом A9). Цель — доказать, что путь потребителя шаблона `/init-service → /stack-deep-dive → /de-template` (и `/scaffold`) проходим по написанным скиллам, и найти каждое расхождение «скилл говорит X, дерево требует Y». Работай автономно; спорное фиксируй в финальном ответе.

## Рамка
- Шаблон: `/home/mm/git/stit-core/mobile-template/` (HEAD — чистый, коммиты волны 3). **В шаблоне ты правишь только `.claude/skills/**`, `docs/getting-started.md`, `docs/stack/_templates/**`, `docs/stack/README.md`, `docs/reference/strategy-mobile-canon/**` — то, что описывает процедуру и обнаружило расхождение**; код/гейт/Makefile шаблона не трогай (нашёл дефект кода — в финальный ответ). `.claude/plans/roadmap.md` шаблона не трогай. `git` в шаблоне — только `status`/`diff`; коммитит основная сессия.
- Копии: `/home/mm/.cache/stit-mobile-research/dryrun/` — `git clone /home/mm/git/stit-core/mobile-template both` и `... android` (scratchpad — tmpfs, туда `build/` не влезет). В копиях можно всё (это одноразовые деревья), включая коммиты. В конце — `./gradlew --stop`; копии не удаляй (основная сессия посмотрит и удалит).
- Сосед `../strategy` для копий: создай `/home/mm/.cache/stit-mobile-research/dryrun/strategy/docs/projects/_template/mobile/` копией `docs/reference/strategy-mobile-canon/` из шаблона и `docs/projects/demo/mobile/app/` из `_template/*.md` с reference-значениями шаблона (surface `app`, записи entities/screens/flows/features/shell/entry-points/permissions/shared-api/shared-lib — те, что перечислены в `docs/reference/strategy-mapping.md § Reference-соответствия`, `status: stable`); `../backend` и `../infra` отсутствуют — скиллы обязаны печатать next-step, а не падать.
- Один Gradle за раз (ты сейчас единственный Gradle-процесс на машине), Gradle в foreground, Bash-таймаут ≤ 600000 мс, длинные цепочки — по стадиям. Env в каждом вызове Bash:
  ```
  export JAVA_HOME=/home/mm/.cache/stit-mobile-research/jdk/jdk-21.0.12.1+1
  export ANDROID_HOME=/home/mm/.cache/stit-mobile-research/android-sdk
  export SWIFT_TOOLCHAIN=/home/mm/.cache/stit-mobile-research/swift/swift-6.4.0-RELEASE-debian13/usr/bin
  export PATH="$SWIFT_TOOLCHAIN:/home/mm/.cache/stit-mobile-research/swiftlint:$JAVA_HOME/bin:$PATH"
  export OPENAPI_GENERATOR_JAR=/home/mm/.cache/stit-mobile-research/openapi-generator/openapi-generator-cli-7.25.0.jar
  ```
- Скиллы построены на `AskUserQuestion`; в dry-run роли архитектора и юзера играешь ты сам — не вызывай `AskUserQuestion`, а отвечай дефолтами ниже и следуй процедуре `SKILL.md` дословно (читай `SKILL.md` + `references/` каждого скилла перед шагом). Промоушн `draft → stable` в dry-run допустим без подтверждения.
- Не тратить > 25 минут на тупик; зафиксировать и обойти. Транскрипты других агентов не читать. Scratchpad для заметок: `/home/mm/.cache/tmp/claude-1000/-home-mm-git-stit-core-mobile-template/70f4bebe-906c-4950-8316-698784f62e96/scratchpad/dryrun/`.

## Дефолты архитектора (reference-значения)
project-slug `demo`, имя «Demo», applicationId `tech.example.demo` (+ `.dev`/`.staging`/`.demo`), bundle id `tech.example.demo`, `platforms: [android, ios]` (копия `both`) / `[android]` (копия `android`), `preset: companion`, `auth.mode: bearer`, схема deep link `demo://`, языки `ru` (default), `en`; хосты API `api.dev.example.com` / `api.staging.example.com` / `api.example.com`, deep-link-хосты `app.{dev,staging}.example.com` / `app.example.com`; `SENTRY_DSN` пусто (выключено); `PUSH_PROVIDERS` android `rustore,fcm`, ios `apns`; `ANALYTICS=none`; владелец store-аккаунтов — заказчик; остальное — дефолты шаблона из `docs/stack/_templates/*.md`. Отпечатки/ID, которых нет, — плейсхолдеры формы `<…>` только там, где шаблон их допускает; иначе — reference-значения из шаблона.

## Шаги (в каждой копии; расхождения чинятся в скиллах ШАБЛОНА, затем шаг повторяется в копии)
1. Копия `both`: `make doctor` → `make check` (зелёный, baseline) → `make gate-selftest` (все красные).
2. `/init-service` стадии 1–5 по `.claude/skills/init-service/SKILL.md` (service.md, config.md черновики; стадия 3 — ветка с соседом-strategy, которого ты создал; стадия 5 — handoff-блоки печатаются, не пишутся соседям).
3. `/stack-deep-dive` режим A на все 13 обязательных kinds по волнам W1…W8: `cp _templates/<kind>.md` → заполнение дефолтами → чек-лист `draft → stable` (`references/draft-stable.md`, 6 пунктов, cross-kind-инварианты 1–7 из `docs/stack/README.md`) → `status: stable`; `state/` без blocker. Для копии `android` — iOS-секций в kinds нет (не «пустые», а отсутствуют).
4. `/de-template` по `.claude/skills/de-template/SKILL.md`: Gate → Phase A (A.1 пакет `tech.stit.mobile` → `tech.example.demo` со всеми точками из списка A.1; A.2 README; A.3 конфиги env/xcconfig; A.4 обрезка платформы — только в копии `android` по `references/platform.md`; A.5 strip меты) → Phase B (prune `profile`/`todos` по `references/prune.md`, docs) → Verify (`make check` → 0, `make gate-selftest` → все оставшиеся красные, `git grep -n 'tech\.stit\.mobile'` пуст вне `docs/reference/kinship.md`, `make kin-diff` advisory). В копии `both` — флаг `--ios-unverified` (Mac нет).
5. `/scaffold` в копии `both` — по одному варианту каждого вида по stable-записям созданной карты (`entity`, `feature`, `screen`, `flow`, `entry-point`, `permission`, `api`) с новым slug (например, `note`/`NoteList`) — компилируемый код и `make check` зелёный после каждого; `stack`/`rule`/`skill` из `references/harness.md` — по одному.
6. Копия `android`: шаги 1–4 с `--platform android` (после A.4 Swift-стадий и группы MAC в `Makefile`/`check.sh` нет, `make check` без SKIP-блока, `gate-selftest` — только оставшиеся фикстуры).
7. После каждого шага в копиях — `make check` зелёный, `make gate-selftest` красный. Всё, что скилл не смог выразить точной командой или что расходится с деревом, — правка в скилле шаблона (точечно, forward-only) + повтор шага.

## Финальный ответ (≤ 350 слов)
Таблица расхождений «шаг · что говорит скилл · что потребовало дерево · правка в шаблоне (файл:строки)»; результат `make check`/`gate-selftest` в обеих копиях после каждого этапа (цифры); что не удалось и почему; дефекты кода шаблона (не чинил); точные команды с env.
