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

**Current focus:** build-mobile-template-v1

## Active

### build-mobile-template-v1 — Сборка mobile-template v1
**Создан:** 2026-09-17 · **Статус:** in-progress

**Контекст.** Решение принято (`docs/research/DECISION.md`): один шаблон `mobile-template` —
KMP-ядро `shared/` + нативный UI `androidApp/` (Compose) и `iosApp/` (SwiftUI). Форма
репозитория — `docs/research/08-template-design.md` (§2 дерево, §3 обвязка, §4 гейт, §5 docs),
эмпирика и семя гейта — `docs/research/06-tooling-agent.md` + `docs/research/spikes/android-gate/`
(прогнан, зелёный). Сессия идёт по `docs/research/prompts/NEXT-SESSION.md`: основная сессия
запускает Fable-агентов волнами (§2.4), после каждой волны гоняет протокол проверки §4 и
коммитит; агенты не коммитят. Это фазы 2, 3, 4, 6 плана синтеза (§12) и Linux-часть фазы 5.
Требование владельца: красиво, качественно, без замудрёностей, читаемо; тесты очень важны.

**Не входит в скоуп.** Фаза 5 на Mac (`check-mac`, snapshot-тесты, `.xcodeproj`-верификация,
подпись); фаза 7 (первый релиз в RuStore/Play, cross-repo PR'ы в соседние репо); спайки S-1,
S-10, S-15 (нужен Mac) и S-12 (нужны аккаунты магазинов). Всё это — в `## Backlog`.

**Открытые вопросы.**
- [x] Q1: собирается ли SKIE 0.10.14 на Linux без cinterop → нет: SKIE 0.10.14 поддерживает Kotlin
  ≤ 2.4.10 и падает на 2.4.20 при конфигурации; SKIE не подключён, фасад для Swift — плоский по
  правилу `kmp-public-api`; подключение — строкой в `## Backlog`.
- [x] Q2: считает ли BCV 0.18.2 `iosArm64` поддерживаемым на Linux при Kotlin 2.4 (S-2) → да:
  `apiDump`/`apiCheck` на Linux ведут оба дампа (`shared/api/jvm/shared.api`, `shared/api/shared.klib.api`);
  строгая klib-валидация — `-Pstit.klibStrict=true` в `check-mac`.

**Гейт (Done).**
- `make doctor`, `make check`, `make check-fast`, `make gate-selftest` — зелёные на этой машине
  (все негативные фикстуры красные); `make check` печатает блок пропущенных macOS-стадий и
  завершается кодом 0.
- Каркас точно по дереву 08 §2.1; 8 reference-фич работают в `androidApp` в `demo`- и
  `dev`-flavor против `MockTransport`.
- Тесты: `:shared:jvmTest` ≥ 80 % строк; Android-модули ≥ 70 %; каждый экран — Robolectric-тест
  и Roborazzi-скриншот на каждое состояние; Konsist; `FixturesTest`; «demo не ходит в сеть»;
  негативные фикстуры NF-01…NF-16 (+ NF-20…24 при iOS-pure).
- Обвязка: `CLAUDE.md` ≤ 200 строк, 23 правила с `paths:`, `settings.json`, 9 скиллов, docs,
  15 stack-форм, канон strategy, `README.md`; dry-run обвязки на scratch-копии в режимах both и
  `--platform android` — после каждого `make check` зелёный, `gate-selftest` красный.
- История — Conventional Commits, дерево чистое, этот файл актуален.

**Атомы.**

#### Phase 1 — волна 1: фундамент
- [x] **A1:** Агент A — каркас репозитория и Linux-гейт на пустом дереве (08 §2.1, §2.3, §4;
  семя — spike `android-gate`): корень Gradle, `build-logic` (11 плагинов), пустой `shared/`,
  `androidApp/*`, `Makefile`, `scripts/tools/*`, `scripts/git-hooks/*`, `.gate-fixtures/NF-01…10`,
  заглушки `AGENTS.md`/`CLAUDE.md`.
  - Verify: `make doctor && make check && make gate-selftest` → check зелёный ≤ 15 с тёплый с
    SKIP-блоком, 10 фикстур красные · Зависит от: —
  - **Done:** `make doctor` OK; `make check` → 0 с SKIP-блоком, тёплый 19–21 с (машина в swap; цель
    ≤ 15 с — перемерить на свободной машине), холодный 104 с; `check-fast` 7 с, `kmp-check` 1 с,
    `gate-selftest` 10/10 красных; `GATE_RELEASE=1 make check` зелёный (R8 1 ч 45 м при zram 100 %);
    покрытие shared 100 % / Android 100 % (заглушки); SKIE не подключён (Q1), BCV klib на Linux (Q2).
- [x] **A2:** Протокол проверки §4 после волны 1 (машина и гейт, структура, читаемость), точечные
  правки, коммит `feat(repo): каркас и Linux-гейт`.
  - Verify: `make check` → 0 и `git status` чистый · Зависит от: A1
  - **Done:** основная сессия: `make doctor` OK (1 warn RAM), `make check` тёплый 21 с после `--stop`
    и 9 с на живом демоне (код 0, SKIP-блок из 5 macOS-стадий), `check-fast` 3 с, `gate-selftest`
    10/10 красных за 29 с, дерево после самотеста без изменений, `./gradlew --stop`; прочитаны все
    файлы каркаса, оба PNG-эталона читаемы; замечания (run-android app id, дубль списка скиллов,
    секции status.sh, themes.xml) — в `docs/research/prompts/NEXT-SESSION-2.md` §1.

#### Phase 2 — волна 2: shared ∥ обвязка
- [ ] **A3:** Агент B1 — `shared/` целиком (08 §2.2–§2.7, 07): core, контракт API + кодоген
  моделей, сущности, презентеры 8 фич, `SharedContainer`, фикстуры `mock/`, тесты, NF-11…16.
  - Verify: `make kmp-check && make test && make cover-check && make gen-check && make mock-check
    && make check` → зелёные, покрытие shared ≥ 80 %, NF-11…16 красные · Зависит от: A2
- [ ] **A4:** Агент B3 (параллельно с A3, без Gradle) — `CLAUDE.md`, 23 правила, `settings.json`,
  9 скиллов, агенты, `AGENTS.md`, `docs/**`, `README.md`, `LICENSE`, `SECURITY.md`.
  - Verify: `wc -l CLAUDE.md` ≤ 200; frontmatter правил с `paths:`; ссылки docs резолвятся
    (скрипт); `make check` не сломан · Зависит от: A2
- [ ] **A5:** Протокол §4 после волны 2 (гейт, покрытие, читаемость, обвязка против кода),
  коммиты `feat(shared): …`, `docs(harness): …`.
  - Verify: `make check` → 0, `make gate-selftest` → NF-01…16 красные, `git status` чистый ·
    Зависит от: A3, A4

#### Phase 3 — волна 3: Android ∥ iOS-pure
- [ ] **A6:** Агент B2 — `androidApp/` (08 §2.3, §2.6, §2.7, 01): core-модули, 8 фич, Nav3, Hilt,
  `demo`-flavor, `:lint`, Konsist, Roborazzi на все состояния, `i18n/deeplinks/tokens-check`.
  - Verify: `make check && GATE_RELEASE=1 make check && make screenshots && make build` → зелёные,
    `app-dev-debug.apk` + `app-demo-debug.apk`, покрытие Android ≥ 70 % · Зависит от: A5
- [ ] **A7:** Агент B4 (параллельно с A6, без Gradle) — `iosApp/` Linux-часть (02, 08 §2.3):
  `Packages/Pure` + тесты, SwiftLint/swift-format, `Tools/archlint`, каркас `Core`/`Features`
  по канону без компиляции, `swift-step.sh`, NF-20…24.
  - Verify: Swift-стадии в `make check` зелёные, `make gate-selftest` → NF-20…24 красные ·
    Зависит от: A5
- [ ] **A8:** Протокол §4 после волны 3 (скриншоты открыть и прочитать, тесты, читаемость),
  коммиты `feat(android): …`, `feat(ios): …`.
  - Verify: `make check` → 0, все NF красные, `git status` чистый · Зависит от: A6, A7

#### Phase 4 — волна 4: проверка и dry-run
- [ ] **A9:** Dry-run обвязки на scratch-копии: `/init-service → /stack-deep-dive (A) →
  /de-template` в режимах both и `--platform android`; расхождения чинятся в скиллах.
  - Verify: после каждого шага `make check` → 0 и `make gate-selftest` → красный · Зависит от: A8
- [ ] **A10:** Итоговый прогон §4 целиком, обновление этого файла (закрытое удаляется,
  незакрытое — в `## Backlog`), отчёт владельцу (что зелёное, цифры гейта, что осталось).
  - Verify: `make doctor && make check && make check-fast && GATE_RELEASE=1 make check &&
    make gate-selftest` → как ожидается, `./gradlew --stop`, `git status` чистый · Зависит от: A9

**Infra impact.** — (cross-repo изменения — строкой в `## Backlog`, отдельными сессиями в
соседних репо).

**Чекпоинты.**
- **2026-09-17 · старт.** `git init -b main`, первый коммит с ресёрчем, план записан.
  Машина: тулчейны в `~/.cache/stit-mobile-research/`, свободной RAM ≈ 3 ГБ (swap занят) —
  Gradle-агенты строго по одному. Следующий шаг: A1.
- **2026-09-17 · волна 1 закрыта.**
  - Закрыто: A1, A2 ✓ (коммит `feat(repo): каркас и Linux-гейт`); Q1, Q2 ✓.
  - Обнаружено: субагент с явным `model: fable` падает на старте (403) — запускать без `model`;
    обрывы API лечатся `SendMessage` тому же агенту (контекст и дерево сохраняются); SKIE 0.10.14
    несовместим с Kotlin 2.4.20; тёплый `make check` 19–21 с из-за swap и 12 вызовов Gradle.
  - Следующий шаг: A3 ∥ A4 (волна 2) по `docs/research/prompts/NEXT-SESSION-2.md` (блоки состояния
    для B1/B3 готовы там же).

## Backlog

- **Фаза 5 на Mac:** `make check-mac` (M1–M9), snapshot-тесты всех состояний, XCUITest smoke,
  верификация `iosApp.xcodeproj`, подпись, `mac-host.md` по факту; до этого iOS помечен
  «не верифицирован на Mac», `/de-template` с `ios` требует `--ios-unverified`.
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
- **Тёплый `make check` ≤ 15 с** — сейчас 19–21 с на занятой машине (12 вызовов Gradle по 1–2 с +
  `spotlessCheck` 5 с); если на свободной машине > 15 с — объединить дешёвые стадии (`spotlessCheck
  checkSortDependencies detekt lintDevDebug`) в один вызов Gradle с разбором per-task статусов.
