# 04 — Индустрия мобильной разработки 2025–2026: тренды, устойчивые практики, компании, опросы, AI-агенты

Поток `04-industry`. Дата среза фактов — **2026-09-17**. Пометки у фактов:
**[П]** — проверено по первоисточнику в день среза (URL рядом); **[П~]** — проверено по
вторичному источнику (пресса/разбор) или первоисточник недоступен боту; **[?]** —
предположение/оценка автора; **[03 §x]** / **[01 §x]** / **[02 §x]** / **[05 §x]** —
факт уже разобран в соседнем отчёте, здесь только ссылка.

Зона потока — **индустрия как целое**. Глубокий разбор стеков — `01-android-canon.md`
и `02-ios-canon.md`; шаринг кода (KMP/CMP/Skip/Rust, разворот Shopify, hh.ru) —
`03-shared-strategy.md`; дистрибуция и право РФ — `05-distribution-ru.md`; эмпирика
тулинга на Linux — `06-tooling-agent.md`; контракт с бэкендом, mock-режим, observability,
мост в strategy — `07-backend-contract.md`. Этот отчёт их **дополняет**, не переписывает.

Вопрос потока: как живёт индустрия в 2025–2026, что из трендов реально, что — мода; какие
практики держатся 10+ лет; как строят приложения известные компании (мир и РФ); что
говорят опросы и рынок труда; как AI-агенты меняют мобильную разработку. Итог — **что
зашивать в шаблон, что держать опцией, чего избегать**.

---

## TL;DR

1. **Три вендора сошлись в одной точке** (проверено по I/O 2025/2026, WWDC25/26, KotlinConf
   2025/26): декларативный UI как единственный стандарт — «Compose is our standard… Views now in
   maintenance mode» (I/O 2026), SwiftUI + Liquid Glass обязателен с iOS 27 SDK; агент — штатный
   участник цикла (Android CLI 1.0 + Skills, Xcode 26.3/27 с Claude Agent SDK/Codex/Gemini, MCP и
   **Agent Client Protocol** у Apple и JetBrains одновременно); типизированная навигация
   (Navigation 3 / `NavigationStack`) и UDF — общий знаменатель; тулчейн Android — AGP 9.4 со
   встроенным Kotlin (плагин `kotlin-android` не применяется [01]), iOS — Xcode 27 / iOS 27 /
   Swift 6.4 (вышли 14–15.09.2026 [02]). Ни один вендор не продвигает
   общий UI между платформами; Google даже выпустил Migration Assistant **в** native.
2. **Индустрия качнулась к нативу, а «общая логика» стала нормой**: Shopify (2026-09-10) ушёл с
   React Native «потому что агенты удешевили писать дважды»; Proton (02.2026) переписал почту
   на Rust-ядро ≈ 80 % + SwiftUI/Compose с crash-rate iOS 0.05 %; Netflix, Duolingo, McDonald's,
   Cash App, Wise, Monzo шарят логику на KMP; Avito, Яндекс Карты, X5, Т-Банк — то же в РФ.
   Публичного кейса «весь UI на Compose Multiplatform» у крупной RU-компании не найдено; единственный
   подробный RU-разбор CMP — отказ. Это подтверждает рекомендацию B из [03].
3. **Российская специфика инженерии — BDUI и гигиена SDK**: Avito (Beduin v2: 100+ сценариев
   Android/iOS, KMP-ядро, quality-гейты), Яндекс (DivKit, Еда), Ozon (Composer), Альфа —
   server-driven UI как штатный обход релизного цикла; сами же называют цену (кривая для iOS,
   сложность, расхождения платформ). Для B2B-компаньона — **опция-шов, не база**. Flutter в РФ
   живёт в нишах (Яндекс Про, SDK 2ГИС, KasperskyOS); флагманские B2C-приложения нативные.
4. **Опросы согласны**: Kotlin растёт (SO 2025: 10.8 % vs Swift 5.4 %; «92 % Android-разработчиков
   на Kotlin» — Google), KMP среди респондентов DevEco 7 % → 18 % за год, KMP Survey 2025 — ~70 %
   довольны, ~80 % используют CMP (вендорская выборка). AI используют 84–90 % (SO/DevEco/DORA),
   **«highly trust» — 3.1 %**, DORA 2025: throughput ↑, **stability ↓** — «AI amplifies what's
   already there». Uber: > 70 % PR — от агентов, 3 600 скиллов, 1 000+ MCP-серверов за gateway.
   SO 2026 и DevEco 2026 на 2026-09-17 не опубликованы.
5. **Рынок труда РФ**: hh.ru 2026-09-17 — Android 333 / Kotlin 323 / iOS 243 / Swift 211 вакансий
   против **KMP 9, CMP 5, Flutter 89, RN 46** [03]; Хабр Карьера I п/г 2026 — медианы iOS/Swift
   **326 тыс. ₽**, Android/Kotlin **320 тыс. ₽**, мобильная разработка — самый дорогой сегмент после
   архитекторов. В РФ iOS даёт ~34 % мобильного трафика при ~22–25 % парка — iOS-версия для
   компаньона не «вторая».
6. **Пороги качества стали нормативными**: Play — crash 1.09 % / ANR 0.47 % (28 дней), с 02.2027 —
   бюджеты памяти и **DEX-оптимизация ≥ 25 %**, с 04.2027 — **Zero-Tap Sign-In restoration**
   обязателен для приложений с логином; Xcode 27 Organizer — **Metric Goals** (hang rate, launch,
   battery, storage, hitches), калиброванные по похожим приложениям. Их место — в гейте `/release`,
   не в документации.
7. **Фундамент (16 практик 10+ лет)** совпадает у Google, Apple и живых OSS: UDF + immutable state,
   SSOT/repository, конструкторная DI, модульность с машинной проверкой границ, фейки вместо моков
   (Google: «Prefer fakes to mocks — Strongly recommended»), тесты без устройства + скриншоты,
   feature flags как инвентарь, offline-кеш, a11y/i18n, crash/perf-телеметрия как гейт, CI-гейт,
   trunk-based, code review, типизированный контракт, логика вне UI. Маятник «паттерн года» стоит
   в положении «официальный канон вендора» — редкий момент, когда «как вендор» = «фундаментально».
8. **AI-тулинг асимметричен по хосту**: Android — полностью headless на Linux (Android CLI 1.0,
   skills, Knowledge Base, Kotlin LSP/`kdoc.jar`/klibs MCP, JVM-тесты, Hot Reload MCP); iOS —
   богатейший инструментарий (Xcode MCP/ACP, XcodeBuildMCP под Sentry, Maestro MCP), но **только на
   macOS**. Отсюда архитектурный ответ: максимум логики вне UI, тонкий SwiftUI-слой, удалённая
   Mac-стадия ([03 §6.1], [05 §6.5]). Эмпирика 06 это подтверждает: Android-гейт целиком зелёный
   на Linux (`make check` — 37 с при правке файла, 12 строк вывода, Roborazzi-PNG читаем агентом),
   KMP-klib для `iosArm64` собирается на Linux (но `linkDebugFramework…` молча SKIPPED — ловушка
   гейта), Swift 6.4 на Debian 13 — `swift test` 0.72 с; всё с `SwiftUI`/`UIKit` — только Mac.
9. **Ловушки агентов, о которых говорит сообщество**: `.pbxproj` («never let them touch»), подпись,
   недетерминизм симулятора, галлюцинации версий API (AGP 8 vs 9, старые SwiftUI-модификаторы),
   Gradle-loop friction и «compiles but runs wrong», дрейф снапшотов. Все закрываются обвязкой:
   buildable folders/`Package.swift`, deny на подпись, фиксированный симулятор, `android docs`/
   Apple Document Search/`kdoc.jar`, «не собирать без просьбы» + LSP, `make snapshot-record`.
10. **`AGENTS.md`/`CLAUDE.md` — свойство продовых репо, не шаблонов**: файлы есть у NiA, Element X
    (Android/iOS), DuckDuckGo, Firefox, Wire iOS, WordPress, Nextcloud, Tuist (27 КБ) и даже у
    Telegram-iOS (`CLAUDE.md` 24 КБ с runbook'ами симулятора и правилами многоволнового
    рефакторинга); **нет** — у Bitwarden, Signal, Proton и у всех community-шаблонов
    (architecture-templates, cortinico, KMP-App-Template, KaMPKit). Паттерн: инварианты →
    команды → карта модулей; там, где нет скиллов, файл разрастается до 24–27 КБ. Наш формат
    (ядро ≤ 200 строк + path-scoped rules + скиллы) — структурнее любого найденного.
11. **Community-шаблоны останавливаются на «проект собирается»**: Google `architecture-templates`
    стагнирует (последний коммит 2024-12-12, Navigation 2), `compose-multiplatform-template`
    архивирован; живые (`cortinico`, `KMP-App-Template(-Native)`, KaMPKit) дают каркас + линтеры +
    GH Actions, но без тестовой политики, гейта одной командой, архитектурных тестов, docs-first,
    demo-режима и agent-обвязки. Стартовая точка кода — `KMP-App-Template-Native` + `android create
    empty-activity-agp-9` + Xcode «SwiftUI App»; всё остальное переносится из Go/SPA-шаблонов.
12. **Что зашить / опция / избегать** (полная таблица — §7.1): зашить — Compose, SwiftUI +
    `@Observable` + Swift 6, Nav3/`NavigationStack`, UDF, repository, DI, модульность +
    архитектурные тесты, фейки, JVM/скриншот/Preview-тесты, Swift Testing, coverage-пол, пороги
    Play/Xcode в релизе, R8 ≥ 25 %, adaptive/edge-to-edge/predictive back, Credential Manager +
    Zero-Tap, токен-слой, i18n/a11y, mock/demo-режим, Sentry-совместимый шов, feature-flag шов,
    agent-обвязка + вендорские скиллы; опция — KMP-`sharedUi`/CMP, BDUI/DivKit, on-device AI/App
    Intents/AppFunctions, Kotlin LSP/MCP-инструменты, Xcode/Maestro MCP; избегать — RN/Flutter,
    Rust/gomobile-ядро для профиля, TCA/MVI-фреймворки как база, Rx/Combine/kapt/XML/Fragments,
    SwiftData по умолчанию, Firebase/GA, ручные `.pbxproj`, CocoaPods, Tuist Cloud, GitHub Actions
    как обязательный CI, долгоживущие флаги.
13. **Что устареет за год**: версии стека (кварталы), дедлайны Play 2027/Apple 04.2027 (определённо),
    команды агентных CLI/MCP (высокая), Swift export/Kotlin Toolchain статусы (средняя), владелец
    RuStore (сменился 2026-07-16 — VK продала 100 % менеджменту разработчика), Konsist/Periphery
    (стагнация/коммерциализация). Ответ — пины версий + `/plan` + forward-only + drift-проверка.
14. **Открытые вопросы владельцу** (§8.2): принять ли тезис Shopify как гипотезу; нужен ли
    BDUI-шов; госсектор/«Аврора»; швы под on-device AI; имя agent-файлов и ACP; вендоринг
    скиллов и телеметрия; пороги crash-free ≥ 99.9 % как STOP-2; политика «без платных IDE»;
    лимиты расходов на агентов; Mac.

---

## 1. Векторы вендоров 2025–2026

Три вендора, шесть ключевых событий (Google I/O 2025 и 2026, WWDC 2025 и 2026,
KotlinConf 2025 и 2026) плюс релизные вехи между ними. Ниже — что **реально анонсировано**
(по первоисточникам), а в конце раздела — таблица «в проде уже / обещание».

### 1.1. Google: Android 16 → 17, Compose-first, агентный тулинг

**Google I/O 2025 (2025-05-20)** — пост «16 things to know for Android developers»
[П, https://android-developers.googleblog.com/2025/05/16-things-to-know-for-android-developers-google-io-2025.html]:

| Вектор | Что анонсировано | Статус на 2026-09-17 |
|---|---|---|
| On-device AI | ML Kit GenAI APIs на Gemini Nano (summarize/proofread/rewrite/image description), Firebase AI Logic | в проде; Gemini Nano 4 preview (I/O 2026) |
| Adaptive | «single mobile app» на 500 M устройств (телефоны, складные, планшеты, ChromeOS, авто, XR); Compose Layouts library; **Navigation 3** | Nav3 1.0 stable 2025-11-19 [01 §1]; 580 M+ large-screen устройств (I/O 2026) |
| Дизайн | **Material 3 Expressive** («emotional UX», motion, personalization); Wear OS 6 на M3 Expressive | material3 1.4.0 в Compose BOM 2026.09 [01 §1] |
| Виджеты | Glance 1.2, Live Updates (Progress Style) | в проде |
| KMP | AS-шаблон «KMP shared module», Jetpack-библиотеки с KMP, codelabs миграции | tier-1 KMP у `room/datastore/lifecycle/paging/…` [03 §1] |
| Gemini в Android Studio | Image-to-Code, **Journeys** (e2e-сценарии на естественном языке), Version Upgrade Agent | в проде (Journeys — через `android` CLI, 2026) |
| XR | Android XR SDK DP2 (Samsung Project Moohan, XREAL) | DP4 → Beta core libs (I/O 2026) |
| Android 16 | Live Updates, desktop windowing, connected displays, a11y | stable лето 2025 [П~] |

**Android CLI + Android Skills (2026-04-16)** — пост «Android CLI and skills: Build Android
apps 3x faster using any agent» [П, https://android-developers.googleblog.com/2026/04/build-android-apps-3x-faster-using-any-agent.html]:
Android CLI (preview), репозиторий Android Skills (`SKILL.md`-формат), Android Knowledge Base
(`android docs`), заявленные «−70 % токенов на setup, ×3 быстрее» (методика в посте не
раскрыта). Поддерживаемые агенты названы прямо: «Gemini in Android Studio, Gemini CLI,
Antigravity, or third-party agents like **Claude Code** or Codex». Детали CLI — [01 §6].

**Google I/O 2026 (2026-05-19)** — пост «17 things to know for Android developers»
[П, https://android-developers.googleblog.com/2026/05/17-things-android-developers-google-io.html]
и «Android Studio I/O Edition» [П, https://android-developers.googleblog.com/2026/05/whats-new-android-developer-tools.html]:

| № | Анонс | Формулировка / детали | Что это для нас |
|---|---|---|---|
| 1 | **Android CLI stable** | «programmatic tools that allow any AI agent» (Claude Code, Codex, Antigravity); мост в Android Studio: semantic symbol resolution, analyze files, **render Compose previews** | базовый инструмент агентного цикла (Linux-совместим) |
| 2 | Google AI Studio → native Android | «build native Android apps, simply with a prompt», Compose + Kotlin + recommended patterns, встроенный эмулятор | сигнал: Google считает Compose+Kotlin «языком агентов» |
| 3 | **Android Bench** | LLM-лидерборд на реальных Android-задачах, в т. ч. open-weight Gemma 4 | ориентир при выборе модели |
| 4 | **Migration Assistant** | iOS / React Native / web → native Android «за часы» агентным воркфлоу | прямой ответ Google на «писать дважды» (ср. Shopify, [03 §3.4]) |
| 5 | Gen-AI в приложениях | Gemini Nano 4 preview, Firebase AI Logic, **ADK for Android** (AG-UI, A2UI) | опция, не база |
| 6 | **AppFunctions** | «apps to behave like on-device MCP servers», private preview | заготовить шов; не зашивать |
| 7 | **Compose-first** | «**Compose is our standard for UI development**, with Views now in **maintenance mode**»; Styles API, shared elements | XML-View в шаблон не попадает вообще |
| 8 | Adaptive by default | 580 M+ large-screen; Nav3; экспериментальные Grid/FlexBox | адаптив — инвариант с первого экрана |
| 9 | Googlebook | Desktop Emulator в AS Canary | — |
| 10 | Glance = Compose | единая Compose-модель виджетов (mobile / Wear / cars), RemoteCompose | — |
| 11 | Cars | Car App Library | вне профиля |
| 12 | Android XR DP4 | core-библиотеки → Beta | вне профиля |
| 13 | Media | CameraXViewfinder Composable, Media3 AI Effects | камера/QR — CameraX |
| 14 | TV | Engage SDK | вне профиля |
| 15 | Perf tools | R8 Configuration Analyzer, ProfilingManager, **встроенный LeakCanary** («Fix with Agent»), Android Performance Analyzer (26× быстрее трейсы) | в гейт/дебаг |
| 16 | Play | Play Shorts, discovery в Gemini, агентное управление каталогом | [05] |
| 17 | **Android 17 (API 37)** | lock-free MessageQueue, GC young-gen, contact picker, EyeDropper API; **обязательная resizability на больших экранах**, certificate transparency по умолчанию, ограничение local network | targetSdk 37 — миграционный чек-лист |

Android Studio **Quail** (I/O Edition): Agent Skills в Agent Mode, параллельные
агентные диалоги, New Project Agent, Firebase из Agent Mode, модели Google/Anthropic/OpenAI,
локальная **Gemma 4** (в Canary — скачивается из IDE), ADB Wi-Fi 2.0, поддержка Android
Developer Verification в IDE [П]. Квартал спустя — Quail 4 с 23 предустановленными
скиллами по стандарту agentskills.io [01 §6.2].

**2026-06-09 — «Top 3 updates for Android developer productivity»**
[П, https://android-developers.googleblog.com/2026/06/android-developer-productivity-updates.html]:
Android CLI **1.0 stable** (npm, Homebrew; `studio`-мост к профайлерам/Compose Previews/Device
Streaming; Journeys), Android Skills **> 17** (Adaptive UI, Glimmer XR, CameraX, Perfetto SQL,
Styles API, AppFunctions, Credential Manager, Engage, testing, Wear M3), Android Bench + открытые
модели (Gemma 4, Gemini 3.5 Flash).

**Android 17** — stable **2026-06-16** (June Pixel Drop) [П~, https://en.wikipedia.org/wiki/Android_17 ;
androidauthority.com], QPR1 stable **2026-09-15** [П~, gsmarena/droidfeats]. Traditional Developer
Previews отменены в пользу Android Canary; Minor SDK Release планируется в Q4 2026 [П~ Wikipedia].
Behavior changes для `targetSdk 37` [П, https://developer.android.com/about/versions/17/release-notes]:
на sw ≥ 600 dp игнорируются `screenOrientation`/`resizeableActivity`/`min|maxAspectRatio`
(кроме игр); Activity больше не пересоздаётся на `CONFIG_KEYBOARD|NAVIGATION|TOUCHSCREEN|COLOR_MODE|UI_MODE`
(opt-in `android:recreateOnConfigChanges`); LAN-доступ требует `ACCESS_LOCAL_NETWORK`;
NPU — `uses-feature neural_processing_unit`; фоновое аудио «fails silently» вне валидного
lifecycle; cross-profile loopback запрещён; программный доступ к SMS-OTP задержан на 3 часа
(→ SMS Retriever / User Consent API).

**Jetpack Compose August '26 (2026-08-12)** [П, https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html]:
Compose **1.12** (BOM 2026.08.00), **минимум AGP 9.1.1**, compileSdk **37**; mesh-градиенты,
Wide Color Gamut/HDR, `DeferredAnimatedContent`, rich text в `BasicTextField`, `SelectionState`,
**Credential Manager (passkeys) нативно в Compose**, named areas в Grid; «Time to Initial
Display… comparable to Views»; новые тестовые синхронизации `hasPendingWork()` /
`runWithoutImplicitWait()`; Styles API — по-прежнему experimental с обещанными breaking changes.

**Google Play — новые технические требования (объявлены 2026-08-26)**
[П, https://support.google.com/googleplay/android-developer/answer/17492799]:

| Требование | Порог | С какого срока |
|---|---|---|
| Память (Anonymous RSS+Swap, 90-й перцентиль, по RAM устройства) | приложения: 2 ГБ foreground / 1 ГБ background при 4 ГБ RAM … 4.25 / 2 ГБ при 16 ГБ; bitmap > 200 МБ (background) / > 400 МБ (cached) | **февраль 2027** |
| Оптимизация DEX (R8: obfuscation+optimization+shrinking) | **≥ 25 %** для приложений с DEX > 10 МБ (игры > 50 МБ) | **февраль 2027** |
| **Zero-Tap Sign-In restoration** (Restore Credentials API, Android 9+) | обязательно для приложений с логином; исключения — enterprise/private, regulatory | **апрель 2027** |
| Core vitals (действуют) | crash **1.09 %** overall / 8 % per phone model; ANR **0.47 %** / 8 %; partial wake locks 5 % | 28-дневное окно; снижение видимости в Play |
| 64-bit, **16 KB page size**, Wear/TV 64-bit | — | действуют (Wear с 2026-09-15) |

Это **прямые кандидаты в гейт**: R8 full mode обязателен (DEX-оптимизация), memory-бюджеты —
в Macrobenchmark/Perfetto-проверки, Zero-Tap restoration — в reference-фичу `auth` (Credential
Manager). Дедлайны `targetSdk`, 16 KB и developer verification — [05 §1.2].

### 1.2. Apple: WWDC25 → Xcode 26.3 → WWDC26 → iOS 27

**WWDC25 (2025-06-09)** — пресс-релиз «Apple supercharges its tools and technologies for
developers» [П, https://www.apple.com/newsroom/2025/06/apple-supercharges-its-tools-and-technologies-for-developers/]:

| Вектор | Анонс | Статус на 2026-09-17 |
|---|---|---|
| Дизайн | **Liquid Glass** — «optical qualities of glass with a sense of fluidity», все платформы 26 | обязателен: `UIDesignRequiresCompatibility` игнорируется при сборке под 27 [02 §1] |
| Xcode 26 | ChatGPT встроен; другие провайдеры по API-ключу; локальные модели на Apple silicon; Coding Tools (генерация/тесты/доки/фиксы), голосовое управление | beta 7 (2025-08-28): GPT-5 и вход Claude-аккаунтом (Sonnet 4) [П~, 9to5mac/MacRumors] |
| **Foundation Models framework** | on-device Apple Intelligence «as few as three lines of code», guided generation, tool calling, бесплатный инференс, офлайн | в проде с iOS 26; расширен в 27 |
| Swift 6.2 | WebAssembly; «configure modules or individual files to run on the main actor by default» | approachable concurrency — дефолт новых проектов [02 §1] |
| App Intents | visual intelligence, Siri/Spotlight/widgets/controls | в 27 — «personal context, app actions, onscreen awareness» |
| Прочее | Containerization (Linux-контейнеры на Mac), Icon Composer, Metal 4, Accessibility Nutrition Labels, Declared Age Range API | в проде |

**Xcode 26.3 (RC 2026-02-03, релиз 2026-02-26)** — «Xcode 26.3 unlocks the power of agentic
coding» [П, https://www.apple.com/newsroom/2026/02/xcode-26-point-3-unlocks-the-power-of-agentic-coding/]:
в Xcode интегрированы «Anthropic's Claude Agent and OpenAI's Codex»; агенты «search
documentation, explore file structures, update project settings, and verify their work
visually by capturing Xcode Previews and iterating through builds and fixes»; «Xcode 26.3
makes its capabilities available through the **Model Context Protocol**». Со стороны Anthropic
[П, https://www.anthropic.com/news/apple-xcode-claude-agent-sdk, 2026-02-03]: нативная
интеграция **Claude Agent SDK** — «the same underlying harness that powers Claude Code» —
subagents, background tasks, plugins; «Claude can be given a *goal*, rather than a set of
specific instructions». Дата релиза 26.02 — [П~, 9to5mac].

**WWDC26 (неделя 2026-06-08)** — пресс-релиз «Apple aids app development with new
intelligence frameworks and advanced tools» [П, https://www.apple.com/newsroom/2026/06/apple-aids-app-development-with-new-intelligence-frameworks-and-advanced-tools/]:

| Вектор | Анонс (формулировки Apple) |
|---|---|
| **Xcode 27 agentic** | модели Anthropic, Google и OpenAI «directly»; интерактивное планирование, multiturn Q&A, Markdown с side-by-side code changes и previews; агенты сами валидируют — «writing and running tests, trying ideas in isolation with Playgrounds, checking visual changes with previews, and interacting with the simulator»; **MCP** + **Agent Client Protocol**; GitHub и Figma «seamless installation» |
| Device Hub | единое окно симуляторов и физических устройств, интерактивное тестирование |
| Xcode | **только Apple silicon**, на 30 % меньше; Xcode Cloud до 2× быстрее |
| **Foundation Models** | «any language model» через **`LanguageModel` protocol**: Apple Foundation Models («custom-built with Google Gemini»), Claude, Gemini; on-device с image input, server models, custom skills; новый фреймворк **Core AI** (локальные полноразмерные LLM); Dynamic Profiles; участники **Small Business Program** (< 2 M первых загрузок) получают модели Apple на Private Cloud Compute **без оплаты cloud API** |
| App Intents / Siri | «personal context understanding, app actions, and onscreen awareness» |
| SwiftUI | быстрее инициализация состояния и layout; reorderable containers; Spatial Preview |
| Swift 6.4 | targeted warning suppression, `anyAppleOS` availability, лучшие диагностики |
| Liquid Glass | пользовательский слайдер прозрачности в Settings |

Сессия **«What's new in Xcode 27»** (WWDC26 #258) [П, https://developer.apple.com/videos/play/wwdc2026/258/]
добавляет: агентные диалоги как **editor panes** (табы/сплиты), параллельные диалоги, `/plan`
(агент собирает контекст без правок, суб-агенты), панель артефактов (файлы, скриншоты);
локализация агентом (кнопка **Generate Translations** в String Catalog); **Organizer**:
новые метрики Storage и Animation Hitches, **Metric Goals** (hang rate, disk writes, battery,
storage, hitches, launch time — калибруются по похожим приложениям и по истории вашего
приложения), **Generate Recommendations** агентом по диагностике; Instruments **Top Functions**.
Сессия **«Xcode, agents, and you»** (#259) [П, https://developer.apple.com/videos/play/wwdc2026/259/]:
встроенные инструменты (build, preview rendering, tests, Device Hub, Apple Document Search,
машинный перевод, суб-агенты), рекомендованный цикл **Explore → Plan → Build → Refine →
Orchestrate**, «capture learnings in reusable architecture documents stored in the project»,
inline-аннотации `// @agent …`. Apple-скиллы (SwiftUI, accessibility, universal sizing, testing,
performance) и экспорт скиллов в Claude/Codex/Cursor — [П~, dev.to/arshtechpro, byteiota].

**Релизы**: iOS 27.0 и Xcode 27 — 2026-09-14, Swift 6.4 — 2026-09-15; с апреля 2027 App Store
Connect требует iOS 27 SDK; Xcode 27 требует macOS 26.6+ и Apple silicon [02 §1].

### 1.3. JetBrains / Kotlin: KotlinConf 2025 → 2026

**KotlinConf 2025 (Копенгаген, 2025-05-21…23)** [П, https://blog.jetbrains.com/kotlin/2025/05/kotlinconf-2025-language-features-ai-powered-development-and-kotlin-multiplatform/]:
Kotlin 2.2 (guard conditions, multi-dollar interpolation, non-local break/continue, context
parameters Beta); **Compose Multiplatform for iOS Stable (1.8.0)**; Compose Hot Reload;
**Swift export — первый experimental в 2.2.20**; Amper «на пути к Alpha»; Kotlin LSP + VS Code
(Alpha «позже в 2025»); Koog (AI-агенты на Kotlin); Junie (EAP); новый KMP-плагин для IDEA/AS;
цифры: 2.5 M Kotlin-разработчиков, ~половина на бэкенде, Ktor +37 % YoY.

**KotlinConf'26 (Мюнхен, 2026-05-20…22)** — «Keynote Highlights» [П, https://blog.jetbrains.com/kotlin/2026/05/kotlinconf26-keynote-highlights/]:

| Область | Анонс |
|---|---|
| Язык | **Kotlin 2.4.0**: context parameters и explicit backing fields — stable; multi-field value classes — experimental; «locality» и rich errors — направление |
| Тулчейн | **Kotlin Toolchain** (Amper как ядро; build/run/test/format/docs одной точкой входа; впереди LSP, AI skills, native deps); **Kotlin LSP → Alpha**, официальное расширение VS Code; **`kdoc.jar`** — машиночитаемая документация «для IDE, Dokka и AI-агентов»; стандартизация **ktfmt** с Meta; first-class Bazel `rules_kotlin` |
| Политика | **18-месячная security-поддержка** с 2.4 (бэкпорты по активным линиям) |
| KMP | «top app usage more than doubled YoY»; PayPal, Booking.com, Sony, Duolingo; KMP-плагин на всех ОС; **новая дефолтная структура** (single-responsibility модули — [03 §2.B]); CMP stable на mobile/desktop, web Beta (09.2025); **Navigation 3 stable для multiplatform**; interop **Liquid Glass** (нативные компоненты вокруг Compose); **Swift Export → Alpha в 2.4**; **SPM import** (Obj-C-зависимости); Kotlin/Native −25 % времени сборки, < ½ RAM; klibs.io 3 500+ |
| AI | **Agent Client Protocol** (открытый стандарт, JetBrains — co-lead); Junie (CLI, Android-поддержка); JetBrains Air (мульти-агентная среда, worktrees/Docker); **Koog 1.0** stable; **Kotlin MCP SDK** — официальный, «built in collaboration with Anthropic»; **Claude нативно в IntelliJ IDEA и Android Studio**; **Kotlin SWE-bench** — 110 задач, Claude Code Opus 4.7 — 86.4 % |
| Android/Google | «**92 % of professional Android developers now use Kotlin**»; K2 — near-universal; KSP −17 % времени; R8 +50 % производительности Compose (удаление рефлексии) |

Kotlin 2.4.20 — 2026-09-07, репозиторий Amper архивирован 2026-05-29 (продукт → Kotlin
Toolchain, Alpha) — [03 §1]. Точная граница Swift export на сегодня — Alpha, «Alpha → Beta» в
роадмапе (08.2026) [03 §1].

### 1.4. Программы разработчиков и правила сторов — что изменилось за год

| Изменение | Дата | Источник / где подробно |
|---|---|---|
| Google Play: `targetSdk ≥ 36` для любого обновления; 16 KB page size; D-U-N-S для организаций | 2026-08-31 (продление до 11-01) / 2025-11-01 | [05 §1.2] |
| **Android developer verification** — все установки на сертифицированных устройствах, enforcement в 4 странах с 2026-09-30, глобально 2027 | 2026-09-30 | [05 §1.2, N-1] |
| Play: технические требования по памяти / DEX-оптимизации / Zero-Tap Sign-In | объявлено 2026-08-26, в силу 02–04.2027 | §1.1 выше [П] |
| Apple: с апреля 2027 обязателен iOS 27 SDK (сейчас — 26 SDK с 2026-04-28) | 2026-09-09 | [02 §1] |
| Apple прекратила обработку платежей в РФ (Developer Program из РФ — через иностранное юрлицо) | 2026-04-01 | [05 §1.3] |
| 194-ФЗ (RuStore на iPhone) Apple не исполнила; дело ФАС | 08.2026 | [05 §1.3] |
| **VK продала 100 % RuStore** гендиректору компании-разработчика (ООО «Много приложений», Д. Панкрушев); оценка ~2 млрд ₽, детали закрыты | 2026-07-16 | [П, https://habr.com/ru/news/1059712/ ; https://vk.company/ru/press/releases/12359/] — для [05] это новый вводный: владелец канала дистрибуции сменился |
| Xcode только на Apple silicon; macOS 27 — только ARM | 06–09.2026 | [02 §1] |
| Apple Small Business Program: модели Apple на PCC без оплаты API | WWDC26 | §1.2 |

### 1.5. «В проде уже» vs «обещание» (срез 2026-09-17)

| Анонс | Статус | Основание |
|---|---|---|
| Compose-first, Views в maintenance mode | **факт** | I/O 2026 [П]; все 8 живых OSS-образцов на Compose [01 §4.5] |
| Navigation 3 | **stable** (Android 1.1.7; KMP-сборка 1.1.7 / 1.2.0-beta) | [01 §1], [03 §1] |
| AGP 9.x со **встроенным Kotlin** (плагин `kotlin-android` не применяется, `kapt` только через `legacy-kapt`, `compileSdk` минорный) | **факт**; текущий stable 9.4.0 | [01 §1]; эмпирически — [06 TL;DR п. 1, 14]: канон convention-плагинов NiA под AGP 9 пришлось переписать (`CommonExtension` больше не генерик) |
| Material 3 Expressive | **stable в material3 1.4** | [01 §1] |
| Android CLI / Skills / Knowledge Base | **stable 1.0** (06.2026) | [П] |
| Android Bench, AI Studio → Android, Migration Assistant | работает, но метрики Google не независимы | [П], методика 3×/−70 % не раскрыта |
| AppFunctions / on-device MCP | **private preview** | [П] |
| Gemini Nano 4, ADK for Android | preview | [П] |
| Android XR core libs | Beta | [П] |
| Liquid Glass | **обязателен** при сборке под 27 SDK | [02 §1] |
| Xcode agentic (Claude Agent SDK / Codex / MCP) | **в проде** с 26.3 (02.2026), расширено в 27 | [П] |
| Foundation Models: provider-agnostic `LanguageModel`, Core AI | iOS 27 GA (09.2026), реальная зрелость — проверять | [П] |
| Swift 6 language mode + approachable concurrency | **дефолт** новых проектов | [02 §1] |
| Swift Testing | **дефолт**, XCTest interop | [02 §1] |
| Swift SDK for Android | official с 6.3, без UI | [03 §2.D-2] |
| KMP | **Stable** (11.2023), Google tier-1 библиотеки | [03 §1] |
| Compose Multiplatform iOS | Stable с 1.8 (05.2025), текущий 1.12.0 | [03 §1] |
| Swift export | **Alpha** | [03 §1] |
| Kotlin Toolchain (ex-Amper) | **Alpha** | [03 §1] |
| Kotlin LSP | **Alpha** | [П] |
| Koog 1.0, Kotlin MCP SDK | stable | [П] |
| Kotlin/Wasm | Beta | [П] |
| Agent Client Protocol (JetBrains + Apple Xcode 27) | стандарт принят двумя вендорами | [П] — заметное совпадение векторов |

**Вывод §1.** Три вендора сошлись на одной модели: **декларативный UI как единственный
стандарт (Compose / SwiftUI), агент как штатный участник цикла (CLI + скиллы + MCP/ACP), и
типизированные навигация/состояние как основа для агентной симметрии платформ**. Ни один
вендор не продвигает общий UI между платформами: Google строит «Migration Assistant» в
обратную сторону (в native), Apple открывает Foundation Models для чужих моделей, но UI
остаётся SwiftUI; JetBrains продаёт KMP через «shared logic», а Compose Multiplatform —
как опцию с обязательной нативной оболочкой под Liquid Glass.

## 2. Опросы и рынок

### 2.1. Опросы разработчиков

**Stack Overflow Developer Survey 2025** (49 100+ ответов; результаты 2026 года на
2026-09-17 **не опубликованы**: корень survey.stackoverflow.co показывает 2025 как последний,
`/2026/` → HTTP 404 [П]) — https://survey.stackoverflow.co/2025/technology ,
https://survey.stackoverflow.co/2025/ai [П]:

| Показатель | Все | Профессионалы |
|---|---|---|
| Kotlin — «использую» | 10.8 % | 11.5 % |
| Swift | 5.4 % | 5.7 % |
| Dart | 5.9 % | 6.1 % |
| Java | 29.4 % | 29.6 % |
| TypeScript | 43.6 % | 48.8 % |
| Rust | 14.8 % | 14.5 % |
| Objective-C | не в топе | — |
| Используют или планируют AI-инструменты | **84 %** (76 % годом ранее) | ежедневно — **50.6 %** |
| «Highly trust» точности AI | **3.1 %** | 2.7 % |
| Не доверяют точности (somewhat + highly distrust) | 45.7 % | — |
| Используют AI-агентов ≥ раз в месяц / не планируют | 30.9 % / 37.9 % | ежедневно — 17.5 % среди пользователей |
| Главная фрустрация — «almost right, but not quite» | **66 %** | — |
| «Debugging AI-generated code is more time-consuming» | 45.2 % | — |
| Инструменты среди использующих агентов | ChatGPT 81.7 %, GitHub Copilot 67.9 %, Claude Code 40.8 % (категории страницы `/ai` смешивают чаты и агенты — читать как порядок, не как точную долю) | — |

Чтение для нас: Kotlin — в 2× популярнее Swift и стабильно растёт (JetBrains DevEco:
«Rust, Go, and Kotlin show steady gains», Objective-C «continue declining» [П]); Dart на уровне
Swift — Flutter жив, но не растёт. По AI — **массовое использование при низком доверии**:
именно это и есть обоснование гейта «агент не заменяет линтеры/тесты» (ср. Shopify «native
expertise remained essential», [03 §3.4]).

**JetBrains State of Developer Ecosystem 2025** (апрель–июнь 2025, **24 534** разработчика из
194 стран; публикация 10.2025) [П, https://blog.jetbrains.com/research/2025/10/state-of-developer-ecosystem-2025/]:
85 % регулярно используют AI-инструменты; **62 % полагаются хотя бы на одного AI-ассистента/
агента/редактор**; ~90 % экономят ≥ 1 ч/нед, каждый пятый — ≥ 8 ч; 68 % ожидают, что
работодатель потребует AI-квалификацию; «хочу выучить»: Go 11 %, Rust 10 %, Python 7 %,
**Kotlin 6 %**, TS 6 %. Мобильный раздел отчёта (`devecosystem-2025.jetbrains.com/…`) боту
недоступен (домен заблокирован для WebFetch) — **цифры по Flutter/RN/KMP оттуда не сняты**.
DevEco 2026 — сбор ответов шёл с мая 2026 [П, blog.jetbrains.com/research/2026/05/…], результаты
на 2026-09-17 не опубликованы.

**Kotlin Multiplatform — опросы JetBrains**:
- доля KMP среди респондентов DevEco выросла **с 7 % (2024) до 18 % (2025)** — «more than doubled
  in just a year» [П, https://kotlinlang.org/docs/multiplatform/kotlin-multiplatform-react-native.html];
- **Kotlin Multiplatform Survey 2025**: ~70 % пользователей KMP-плагина «satisfied or very
  satisfied», **~80 % используют Compose Multiplatform** [П, https://kotlinlang.org/docs/multiplatform/kmp-for-ios.html , обновлено 2026-08-05];
- KotlinConf'26: «top app usage more than doubled year-over-year» (без абсолютных чисел) [П].

Оговорка: все три числа — от вендора и по самоотобранной выборке (пользователи плагина);
80 % CMP среди *KMP-пользователей* не противоречит тому, что в РФ-практике CMP почти не
виден ([03 §3.2], §3.2 ниже).

**JetBrains «Built for Productivity: What the Data Finally Shows About Kotlin» (05.2026)**
[П, https://blog.jetbrains.com/kotlin/2026/05/built-for-productivity-what-the-data-shows-about-kotlin/]:
телеметрия IntelliJ IDEA Ultimate 11.2023–06.2025, **320 000 разработчиков, ~28 M циклов**;
циклы на Kotlin короче Java на **15.7 % / 20.3 % / 15.1 %** (10-мин / 30-мин / 1.5–2-ч
задачи); Java-проекты замедлились на 9–17 % за 20 месяцев, Kotlin — «barely». Это единственное
крупное лонгитюдное измерение продуктивности языка, которое удалось найти; методика —
до/после миграции у тех же людей против контрольной группы.

**Google на KotlinConf'26**: «**92 % of professional Android developers now use Kotlin**»
[П, keynote highlights].

**DORA 2025 «State of AI-assisted Software Development»** (2025-09-24, ~5 000 респондентов)
[П, https://cloud.google.com/blog/products/ai-machine-learning/announcing-the-2025-dora-report]:
**90 %** используют AI на работе; 80 %+ считают, что продуктивность выросла; **30 %** доверяют
AI-коду мало или никак; AI положительно связан с **throughput** и product performance и
**отрицательно — со stability** поставки; тезис «AI is an amplifier… it doesn't fix a team; it
amplifies what's already there»; из семи capabilities для нас прямо релевантны «Prioritize
foundational practices», «Fortify your safety nets» (автотесты, VCS, быстрая обратная связь),
«Connect AI to your internal context» — то есть ровно то, чем являются `CLAUDE.md`/rules/гейт.

**Uber (Software Factory, 2026-08-27)** [П, https://www.uber.com/gb/en/blog/efficient-software-factory/]:
«More than **70 % of pull requests** are attributed to local or cloud agents», **3 600+ agent
skills**, «more than 30K agent skill executions per day», единый MCP-gateway на **1 000+ MCP
servers**; WAU агентных инструментов ×7 и запросы ×9.4 за февраль–август 2026. InfoQ
(2026-07-28) по «Zero Growth Stack» Uber: 92 % инженеров пользуются агентами ежемесячно, **31 %
нового кода — AI-authored**, Autocover генерирует 5 000+ unit-тестов/мес, лимит расходов
$1 500/разработчик [П~, https://www.infoq.com/news/2026/07/efficient-ai-infrastructure/].
Mobile-специфики в этих постах нет; их ценность — масштаб «скиллов как инфраструктуры».

**Android Developers survey** — публичного «Android Developers survey» с цифрами Google не
публикует; ближайшие суррогаты — цифры Google на KotlinConf (92 % Kotlin) и Android Bench.
**[не найдено]**

### 2.2. Рынок приложений и платформ

**Sensor Tower «State of Mobile 2026»** (01.2026; data.ai поглощён Sensor Tower, поэтому это
единственный отчёт линейки) [П, https://sensortower.com/blog/state-of-mobile-2026]:

| Метрика 2025 | Значение | YoY |
|---|---|---|
| Загрузки (iOS + Google Play) | ~150 млрд | +0.8 % |
| IAP-выручка | **$167 млрд** | +10.6 % |
| Время в приложениях | 5.3 трлн ч (3.6 ч/день на пользователя) | +3.8 % |
| Non-game IAP | ~$85 млрд | **+21 %** — впервые больше игр (~$82 млрд, +1.3 %) |
| GenAI-приложения | 3.8 млрd загрузок (×2), > $5 млрд IAP (×3), 48 млрд ч (×3.6) | — |
| США | ~$60 млрд, крупнейший рынок | +4 % времени |

Разбивки по России и по долям iOS/Google Play в публичном блоге нет [П]. Чтение: рынок
**монетизации, а не установок** — плоские загрузки при росте выручки подтверждают, что
конкуренция идёт за retention/качество, а не за «первый запуск»; для B2B-компаньонов это
означает приоритет crash-free/производительности над «фичами первого экрана».

**Доли платформ** (StatCounter напрямую боту недоступен — три попытки WebFetch/CSV
завершились обрывом соединения; цифры через вторичные источники):

| Срез | Android | iOS | Источник |
|---|---|---|---|
| Мир, мобильный веб-трафик, 03.2026 | 67.5 % | 32.3 % (было 27.6 % в 03.2025) | StatCounter через iphones.ru [П~] |
| **Россия, мобильный веб-трафик, 06.2026** | **~66 %** | **~34 %** | StatCounter через iphones.ru, 2026-08-14 [П~, https://www.iphones.ru/iNotes/skolko-lyudey-sidit-s-iphone-v-rossii-ssha-i-pochemu-afrika-glavnyy-skrytyy-rynok-dlya-apple] |
| Россия, доля iPhone среди **используемых** смартфонов | — | ~22–25 % (данные МТС за 2025, экстраполяция) | там же [П~] |
| Россия, доля мобильных ОС в трафике, 2025 (все устройства) | ~46 % | ~18 % | Statbase через anti-malware.ru [П~] |

Разрыв «трафик 34 % vs парк 22–25 %» — важен для планирования: **iOS-пользователи в РФ
генерируют непропорционально много активности**, то есть для B2B-компаньона iOS-версия — не
«вторая платформа», а половина реального использования.

**Российские мобильные ОС** (anti-malware.ru, обзор 2026-07-13 [П~, https://www.anti-malware.ru/analytics/Market_Analysis/Russian-mobile-operating-systems-2026]):
«Аврора» — сотни тысяч устройств, госсектор/РЖД/«Аэрофлот», полный набор регуляторных
сертификатов; РЕД ОС М (AOSP + Linux-режим, реестр с 2023); KasperskyOS for Mobile — прототипы;
ФСТЭК назвала AOSP-системы «потенциально небезопасными» для КИИ. Для разработчиков: сборка под
«Аврору» — отдельная платформа (Qt/C++), «втрое дороже» Android; **для нашего профиля — вне
шаблона, но потенциальный вопрос заказчика из госсектора** (открытый вопрос владельцу, §8).

**RuStore**: 2026-07-16 VK продала 100 % магазина менеджменту разработчика (§1.4) — на модель
дистрибуции [05] это пока не влияет, но меняет профиль контрагента.

### 2.3. Рынок труда

**Россия, вакансии hh.ru (вся Россия, снято 2026-09-17 потоком 03, метод — `totalResults`
страницы поиска; повторный снимок этим потоком заблокирован анти-ботом hh — одна точка
замера)** [03 §4]:

| Запрос | Вакансий | Запрос | Вакансий |
|---|---|---|---|
| `Android разработчик` | **333** | `Flutter` | 89 |
| `Kotlin` | **323** | `React Native` | 46 |
| `iOS разработчик` | **243** | `Jetpack Compose` | 35 |
| `Swift` | **211** | `SwiftUI` | 28 |
| `Kotlin Multiplatform` | **9** | `Compose Multiplatform` | 5 |
| `Objective-C` | 18 | `KMM` | 2 |

Резюме соискателей без работодательского аккаунта не считываются — **не проверено**.

**Зарплаты, Хабр Карьера, I полугодие 2026** (публикация 2026-08-28)
[П, https://habr.com/ru/companies/habr_career/articles/1075936/]:

| Срез | Медиана, ₽ «на руки» |
|---|---|
| Мобильная разработка (все) | **295 000** |
| **iOS / Swift** | **326 000** (+7 % к II п/г 2025) |
| **Android / Kotlin** | **320 000** |
| Backend (для сравнения) | 251 000 (Go — 325 000) |
| Frontend | 225 000 |
| Москва / СПб / регионы (все разработчики) | 270 000 / 247 000 / 200 000 |

Интерактивный срез по специализации «Разработчик мобильных приложений» —
https://career.habr.com/salaries?qualification=All&spec_aliases%5B%5D=mobileapp_developer [П].
Чтение: мобильная разработка — **самый дорогой сегмент после архитекторов**, Swift и Kotlin
стоят одинаково; кросс-платформенные навыки отдельной премии в данных Хабр Карьеры не имеют.

**Мир**: прямых свежих данных о вакансиях (Indeed/LinkedIn) без авторизации не снято —
**[не проверено]**; косвенные: SO 2025 доли Kotlin/Swift/Dart (§2.1), Google «92 % Kotlin».

**Вывод §2.** Опросы согласны между собой в трёх пунктах: (1) Kotlin растёт, Swift стабилен,
Dart стоит на месте, Objective-C уходит; (2) AI используют почти все, доверяют — единицы,
и измеримая цена — стабильность (DORA), которую компенсируют только «safety nets»;
(3) KMP растёт как **компонент стека** (7 → 18 % респондентов), но рынка труда под него в РФ
нет (9 вакансий) — это согласуется с рекомендацией B из [03]: нанимать обычных Kotlin/Swift-
разработчиков.

## 3. Компании: как строят приложения

Правило раздела: в ячейку попадает только то, что подтверждено первоисточником (блог
компании, репозиторий, доклад, вакансия); где источника нет — «неизвестно». Годы у источников
важны: у половины компаний последний публичный срез архитектуры — 2019–2023.

### 3.1. Глобальные компании

| Компания | Платформы | Язык / UI | Архитектура | Модульность | DI | Тесты | CI | Шаринг кода | Источник / дата |
|---|---|---|---|---|---|---|---|---|---|
| **Shopify** (Shop, POS…) | iOS, Android | Swift/SwiftUI + Kotlin/Compose (с 09.2026; до — React Native) | UDF-симметрия платформ «для агентов» | по фичам | неизвестно | тесты как обязательство чекпойнта (Helix) | Mac-парк, Tardis (агентный доступ к состоянию приложения) | **нет** общего кода; паритет — процессом | [03 §3.4], shopify.engineering/back-to-native, 2026-09-10 [П] |
| **Airbnb** | iOS, Android | Swift, Kotlin (native с 2018, «Sunsetting React Native») | неизвестно (публично — Epoxy/MvRx-эпоха) | неизвестно | неизвестно | неизвестно | неизвестно | **нет** (утверждения «Airbnb на KMP, 95 %» есть только в контент-фермах — **не подтверждено**) | medium.com/airbnb-engineering/whats-next-for-mobile-at-airbnb (2018; Medium отдаёт 403 боту) [П~] |
| **Uber** | iOS, Android | Swift (UIKit + SwiftUI), Kotlin (XML + Compose) | **RIBs** (repo жив: последний push 2026-07-15, 7.9k★) | RIB-дерево | RIBs DI | неизвестно | монорепо; 1 000+ MCP-серверов за gateway; > 70 % PR — агенты | **нет**; uSpec — агент в Cursor + Figma MCP генерит a11y-спеки VoiceOver/TalkBack/ARIA «< 2 мин» | github.com/uber/RIBs [П]; uber.com/…/automate-design-specs (2026-03-11) [П]; efficient-software-factory (2026-08-27) [П] |
| **Netflix** | iOS, Android | Swift, Kotlin/Compose | неизвестно | неизвестно | неизвестно | неизвестно | неизвестно | **KMP для studio-apps и Hendrix SDK** (логика, не UI) | netflixtechblog.com «…now powered by Kotlin Multiplatform» (2021) [П~] |
| **Spotify** | iOS, Android | Swift, Kotlin | неизвестно | 120+ команд, iOS на **Bazel** (2023) | неизвестно | неизвестно | Bazel | неизвестно | engineering.atspotify.com (2023) [П~] |
| **Block / Cash App** | iOS, Android | Swift, Kotlin/Compose | Molecule (presenter-как-composable), Workflow | много модулей | неизвестно | **Paparazzi** (JVM-скриншоты), Turbine | неизвестно | **KMP** (логика в существующих native-приложениях); **Redwood** (Compose → native UI на iOS/web), Zipline; Bitkey — 95 % кода, CMP | code.cash.app (Redwood, Molecule 1.0) [П]; dpe.org DPE Summit 2025 (Wharton) [П~]; [03 §3.1] |
| **Slack** | iOS, Android | 100 % Swift (Obj-C удалён), 92 % Kotlin (2022) | **Circuit** (Compose-driven UDF, presenters) | 92 % модуляризовано (Duplo) | Anvil → Metro (в Circuit-сэмплах 1.4.3) | circuit-test + Turbine | неизвестно | нет | slack.engineering «Stabilize, Modularize, Modernize» (2022) [П]; github.com/slackhq/circuit (1.85k★) [П] |
| **Duolingo** | iOS, Android | Swift, Kotlin/Compose | неизвестно | неизвестно | неизвестно | неизвестно | еженедельные релизы, 40 M+ | **KMP** (логика): «the more we use KMP, the faster we ship» | KotlinConf 2025 talk «Duolingo + KMP: A Case Study in Developer Productivity» [П~]; kotlinlang case studies [03 §3.1] |
| **Reddit / Pinterest / Lyft** | iOS, Android | Swift, Kotlin (Reddit: DS на Compose — из вакансии) | Lyft 2021: изолированные feature-модули, DeclarativeUI поверх DS | неизвестно | неизвестно | Lyft: unit + snapshot + standalone-app интеграционные | неизвестно | неизвестно | Reddit/Pinterest — первоисточников 2025–2026 **не найдено**; Lyft — scottberrevoets.com (2021) [П~] |
| **DoorDash** | iOS, Android | Swift/SwiftUI (PreviewSnapshots), Kotlin | Mosaic (server-driven UI), unified Dasher onboarding (01.2025) | по step-модулям | неизвестно | **snapshot из SwiftUI Preview** | неизвестно | SDUI-контракт | careersatdoordash.com/blog (mobile), InfoQ 2026-03 [П~] |
| **Wise** | iOS, Android | Swift/SwiftUI, Kotlin/**Compose (100 % UI)** | MVVM (Jetpack) | Android: **300+ Gradle-модулей**, 1 M LOC, 65 multiplatform-модулей; iOS: 250+ модулей, XcodeGen → **Tuist**, CocoaPods → SPM (zero-change build 28 s → 2 s) | неизвестно | неизвестно | неизвестно | KMP-модули внутри Android-репо | medium.com/wise-engineering «Wise Tech Stack (2025 update)» (403 боту; по вторичным) [П~] |
| **Monzo / Revolut** | iOS, Android | Swift, Kotlin | Revolut: MVI (Android) | Monzo: доменные пакеты | неизвестно | неизвестно | неизвестно | Monzo: KMP (i18n, a11y, регионы); Revolut: KMP-«multiplatform architecture» | вторичные источники, слабые [П~] |
| **Telegram** | iOS, Android | iOS: Swift + Obj-C, **Bazel, 274 модуля**, свой reactive-фреймворк SwiftSignalKit, свой Postbox поверх SQLite, свой компонентный слой над UIKit; Android: **Java** | собственная | 274 модуля (iOS) | ручная | неизвестно | Bazel; reproducible builds | нет (C++-либы общие) | github.com/TelegramMessenger/Telegram-iOS (8.97k★, push 2026-07-17), github.com/DrKLO/Telegram (29.9k★, push 2026-09-16) [П]; telegram-internals [П~] |
| **Signal** | iOS, Android | Swift/Obj-C; Kotlin+Java, Compose + Fragments | по слоям | ~50 модулей (Android) | ручной DI | JUnit/Robolectric; official Compose screenshot | GH Actions | нет (libsignal — Rust!) | [01 §4.1] |
| **Element X** | iOS, Android | SwiftUI; Compose | Screen+ViewModel+FlowCoordinators; Appyx + Molecule | 266 build-файлов; api/impl/test на фичу | Metro | Konsist ×17, Roborazzi, PreviewTests | GH Actions | **Rust** `matrix-rust-sdk` (uniffi) | [01 §4.6], [02 §2.7], [03 §2.E-1] |
| **Bitwarden** | iOS, Android | SwiftUI; Compose | Coordinator + Processor + State/Action/Effect + Store | 10 модулей (Android); XcodeGen (iOS) | Hilt | JUnit 6, MockK, ViewInspector, SnapshotTesting; Gradle dependency locking STRICT | GH Actions | нет между мобильными (переписка с Xamarin, 2024) | [01 §4.6], [02 §2.7] |
| **Proton Mail** (new apps, 02.2026) | iOS, Android | **SwiftUI; Jetpack Compose** | «Rust core + native UI» | неизвестно | неизвестно | неизвестно | неизвестно | **~80 % кода — общий Rust-SDK**, включая «complex navigation logic»; crash-rate iOS 0.12 → **0.05 %**, Android 0.19 % | proton.me/blog/next-generation-proton-mail-mobile-apps (2026-02-02) [П] |
| **1Password** | iOS, Android, desktop | SwiftUI на iOS; Kotlin | «headless app» на Rust + тонкий native UI | неизвестно | неизвестно | неизвестно | неизвестно | Rust core, **Typeshare** (типы Rust → Swift/Kotlin/TS) | 1password.com/blog/typeshare-for-rust [П~] |
| **Mozilla Firefox** | iOS, Android | Swift (UIKit + SwiftUI); Kotlin | неизвестно | локальные SwiftPM-пакеты; Android — монорепо mozilla-firefox/firefox | неизвестно | XCTest; SwiftLint | fastlane | Rust `application-services` | [02 §2.7]; AGENTS.md/CLAUDE.md в обоих репо (§5.4) [П] |
| **McDonald's** (Umain) | iOS, Android | Swift, Kotlin | неизвестно | неизвестно | неизвестно | неизвестно | неизвестно | **KMP**: сначала платежи, затем всё приложение; «fewer crashes», объединённая mobile-команда | medium.com/mcdonalds-technical-blog (2022) [П~] |
| **Instagram / Meta** | Android | Kotlin/Compose (поэтапно) | неизвестно | неизвестно | неизвестно | неизвестно | неизвестно | нет | engineering.fb.com «Bringing Jetpack Compose to Instagram» (2025-01-24, подкаст без цифр) [П] |

**Что видно из таблицы (мир):**

1. **Native UI — универсальный знаменатель.** Даже компании с максимальным шарингом (Proton ~80 %,
   1Password, Element, Cash App/Bitkey) держат UI нативным (SwiftUI/Compose); исключение — Bitkey
   на CMP и Redwood (Compose-логика → нативные виджеты). Ни у одной из компаний таблицы нет
   web-view/JS-UI как основы после 2026-09-10.
2. **Шарят логику, и только там, где она дорогая**: Rust — у тех, у кого ядро = криптография/протокол
   (Proton, Element, 1Password, Signal-libsignal, Mozilla); KMP — у тех, у кого ядро = бизнес-правила
   и сеть (Netflix studio-apps, Duolingo, McDonald's, Cash App, Wise, Monzo). Для CRM-профиля
   релевантна вторая группа — [03 §2.B].
3. **Модульность — обязательна на масштабе**, но глубина расходится в 30 раз (Bitwarden 10 модулей
   vs Wise 300+/Telegram 274). Общее: границы модулей проверяются машиной (Gradle-граф, Konsist,
   свои валидаторы), а не соглашением.
4. **Тесты без устройства — норма**: JVM-скриншоты (Paparazzi/Roborazzi), snapshot из SwiftUI
   Preview (DoorDash, Element X), Turbine/Molecule для потоков состояния. Инструментальные тесты
   — отдельная, редкая стадия.
5. **Агенты уже в процессе у крупных**: Shopify (Helix/Tardis), Uber (70 % PR, 3 600 скиллов, uSpec),
   Telegram (24 КБ `CLAUDE.md` с процедурами сборки/симулятора), Mozilla/DuckDuckGo/Element/Wire/
   Tuist (`AGENTS.md`) — подробно §5.4.

### 3.2. Российские компании

| Компания | Платформы | Язык / UI | Архитектура | Модульность | DI | Тесты | CI | Шаринг кода | Источник / дата |
|---|---|---|---|---|---|---|---|---|---|
| **Avito** | iOS, Android, Web | Swift; Kotlin/Compose (миграция с XML описана в блоге) | **Beduin v2** — BDUI-«язык» с нативным рендер-движком: 100+ сценариев на Android и iOS, ~столько же на Web; low-code Bricks; health-checks + quality gates; ядро BDUI на **KMP**, UI нативный «чтобы сохранить look&feel» | неизвестно | неизвестно | **Kaspresso** (соавтор с Kaspersky, hh) | неизвестно | **KMP — ядро BDUI**, не UI | habr.com/ru/companies/avito/articles/997010/ (2026-02-16) [П]; avito.tech «Beduin v2»; [03 §3.2] |
| **Яндекс** | iOS, Android (+ Web) | Swift, Kotlin; **Яндекс Про (супераппа водителей) — Flutter** (миграция iOS-зависимостей CocoaPods → SPM, 2026-07-13); Яндекс Карты/Навигатор — **KMP в большом проекте** (Mobius 2025) | **DivKit** (open-source SDUI, 08.2022) + BDUI в Яндекс Еде (Flex SDK; бэкенд Kotlin+Spring); ограничения BDUI прямо названы: сложные анимации, крутая кривая для iOS-инженеров, рост сложности, платформенные расхождения | неизвестно | неизвестно | Probator (UI-тесты, Mobius 2025); Яндекс 360 — упрощение дизайн-системы | неизвестно | DivKit (JSON-контракт), KMP в Картах | dev.go.yandex/blog/bdui-in-eda-2025-05-14 [П]; habr.com/ru/companies/yandex/articles/1054308/ (2026-07-13) [П]; github.com/divkit/divkit [П]; JUG Ru обзор Mobius 2025 [П~] |
| **Ozon** | iOS, Android | Swift (UIKit + SwiftUI), Kotlin/Compose | **BDUI** (Composer SDK — виджеты из JSON); дизайн-система на **SwiftUI + Compose** (meetup); доклады Mobius 2025: SwiftUI-перерисовки, custom-контейнеры, видеоплеер | одно приложение на 44 iOS/Android/QA (2021) | неизвестно | неизвестно | неизвестно | нет (BDUI-контракт) | habr.com/ru/companies/ozontech/articles/661941/ (2022), /756682/ (meetup), /566882/ (2021) [П] |
| **Wildberries** | iOS, Android | Swift (**SwiftUI + UIKit**), Kotlin | iOS: «оптимальный архитектурный шаблон» — **SVIP**, чистая логика, thin client (тимлид Олег Бахарев); техношкола WB: Flux/SOA, TDD/BDD | неизвестно | неизвестно | TDD/BDD в обучении | неизвестно | неизвестно | habr.com/ru/companies/wildberries/articles/798275/ (2024-10) [П]; вакансии SwiftUI [П~] |
| **VK** (Max, ВКонтакте, RuStore) | iOS, Android, Web | Max: **Kotlin (Android), Swift (iOS), TypeScript (Web)**; VKUI — React-компоненты «неотличимые от iOS/Android» | неизвестно | неизвестно | неизвестно | неизвестно | неизвестно | нет | en.wikipedia.org/wiki/Max_(app) [П~]; github.com/VKCOM/VKUI [П]; продажа RuStore 2026-07-16 [П] |
| **Т-Банк** | iOS, Android | Swift (миграция на **Swift 6** — Mobius 2025), Kotlin; кодоген — **Kotlin compiler plugin** «эффективнее KSP» | надёжность через клиентскую аналитику/метрики в реальном времени; Face-Shot (security) | неизвестно | неизвестно | неизвестно | неизвестно | **KMP** (доклады Mobius 2025; «Android/KMP» — самоописание автора статьи 2026-09-16); утверждение «внедряют KMP + Compose» — из статьи аутсорсера, **[П~ слабое]** | JUG Ru обзор Mobius 2025 [П~]; habr.com/ru/companies/tbank/articles/1082854/ (2026-09-16) [П]; habr 1036528 (SmartexSoft, 2026-05-18) [П~] |
| **Сбер** | iOS, Android (+ Aurora?) | Swift, Kotlin | Mobius 2025: sideloading, SQLite-оптимизации, Android Runtime; исторически Clean + RxJava (2016) | неизвестно | неизвестно | неизвестно | неизвестно | неизвестно | JUG Ru обзор Mobius 2025 [П~]; habr.com/ru/specials/322090/ (2016, устарело) |
| **Альфа-Банк** | iOS, Android | Swift, Kotlin/Compose (техлиды Alfa-Mobile о Compose — блог) | BDUI («шпаргалка для дизайнеров», 2024) | неизвестно | неизвестно | неизвестно | неизвестно | «KMP + Compose» — **только по вторичной статье** [П~ слабое] | habr.com/ru/companies/alfa/articles/844606/ (2024) [П]; /708518/ [П] |
| **МТС / МТС Банк** | iOS, Android | **Swift + SwiftUI; Kotlin + Jetpack Compose** (страница вакансий МТС Банка) | Mobius 2025: accessibility, Dynamic Type | неизвестно | неизвестно | неизвестно | неизвестно | неизвестно | job.mtsbank.ru/it [П~]; JUG Ru [П~] |
| **Kaspersky** | iOS, Android, KasperskyOS | Kotlin, Swift; **Flutter портирован на KasperskyOS** (12.2024) | MDM в Endpoint Security | неизвестно | неизвестно | **Kaspresso** (автор) | неизвестно | неизвестно | habr.com/ru/companies/kaspersky/articles/864200/ [П~]; Kaspresso [П] |
| **2ГИС** | iOS, Android, Flutter-SDK | SDK для клиентов: C++-ядро + кодогенераторы Swift/Kotlin/**Dart**; UI-компоненты SwiftUI/UIKit/View/Compose | C++-ядро, FFI | неизвестно | неизвестно | неизвестно | неизвестно | **C++ ядро** (SDK); стек собственного приложения в статье не раскрыт | habr.com/ru/companies/2gis/articles/898804/ (2025-05-21) [П] |
| **Lamoda** | iOS, Android | Swift/**SwiftUI**, Kotlin/**Compose** (техрадар 2023: декларативный UI, реактивность; DS-песочница на SwiftUI) | неизвестно | неизвестно | неизвестно | неизвестно | неизвестно | неизвестно | habr.com/ru/companies/lamoda/articles/746140/ (2023) [П] |
| **Dodo** (Dodo Pizza, Drinkit) | iOS, Android | «Приложение разработано и поддерживается на **Swift и Kotlin**» (оба приложения); Flutter — в перечне услуг | неизвестно | неизвестно | неизвестно | неизвестно | неизвестно | неизвестно (KMP не упомянут) | dodoengineering.ru (380+ инженеров, 26 стран) [П] |
| **hh.ru** | iOS, Android | 2019: iOS 75 % Swift / 25 % Obj-C, VIPER; Android — RxJava 2, Toothpick, Moxy, Cicerone, multi-module Clean | (2019) | multi-module | Toothpick (2019) | UI-тесты на основные сценарии; соавтор Kaspresso | неизвестно | неизвестно | habr.com/ru/companies/hh/articles/461341/ (2019 — **актуальность неизвестна**) [П] |
| **X5 Tech / MAGNIT OMNI / CleverPumpkin (Kassir.ru)** | iOS, Android | Kotlin, Swift | KMP-новые фичи (X5: 1 iOS : 3 Android, «ускорились в 2 раза»); MAGNIT — KMP ломал CI, крэши; Kassir — сеть на KMP (Ktor, Koin), XCFramework через SPM | — | Koin (Kassir) | — | — | **KMP — логика** | [03 §3.2] |
| **Совкомбанк Технологии** | iOS, Android | — | — | — | — | Espresso/Kaspresso, XCUITest; Maestro как тренд; 5 тестировщиков, релизы ежемесячно, регресс ~2 суток, автоматизация «начинается» | — | — | habr.com/ru/companies/sovcombank_technologies/articles/1053818/ (2026-07-03) [П] |
| **Самокат, Кинопоиск** | — | — | — | — | — | — | — | — | первоисточников **не найдено** |

**Что видно из таблицы (РФ):**

1. **BDUI — российская специфика №1.** Три крупнейших продуктовых компании (Avito — Beduin,
   Яндекс — DivKit/Еда, Ozon — Composer) и Альфа-Банк держат **server-driven UI как основную
   стратегию скорости** — обход релизного цикла сторов, который в РФ дороже (модерация RuStore,
   App Store без нормального платёжного канала). При этом все они прямо называют цену: кривая
   обучения для iOS, сложность, платформенные расхождения (Яндекс Еда), необходимость
   health-check/quality-gates (Avito). Для нашего профиля (B2B-компаньон, релизы владельцем)
   BDUI — **не база, а опция**; шов для него — контракт `data`-envelope и виджетный рендер
   (см. §7).
2. **KMP в РФ — «ядро, не UI»**: Avito (ядро BDUI), Яндекс Карты, X5, Kassir.ru, Т-Банк — все
   по схеме B из [03]. Публичного российского кейса «весь UI на Compose Multiplatform» не
   найдено; единственный подробный RU-разбор CMP (Максим Плахута, Онтико, 2025-04-10) —
   **отказ**: нет preview, нечитаемые iOS-стектрейсы, обёртки для Swift, дисбаланс
   iOS/Android-команды, вывод «KMP без CMP» [П, https://habr.com/ru/companies/oleg-bunin/articles/899096/].
3. **Flutter в РФ живёт в нишах**: Яндекс Про (водительский суперапп), Flutter-SDK 2ГИС,
   Flutter на KasperskyOS, аутсорс для банков (Surf/red_mad_robot — [П~]). Флагманские B2C-
   приложения (Avito, Ozon, WB, Max, Dodo, Т-Банк) — нативные. Это совпадает с запретом
   владельца и с общемировой картиной §3.1.
4. **Тестовая культура**: Kaspresso (Kaspersky + Avito + hh) как RU-стандарт Android-UI-тестов;
   Maestro входит в тренды 2026 (Совкомбанк); при этом даже банк с 5 тестировщиками признаёт,
   что автоматизация «начинается» — т. е. **гейт шаблона с JVM-тестами и скриншотами будет выше
   среднего по рынку**, это конкурентное преимущество, а не избыточность.
5. **Публичность падает**: у hh.ru, Сбера, Lamoda последние архитектурные разборы — 2016–2023;
   свежая информация живёт в докладах Mobius/AppsConf (не индексируется как текст) и в
   вакансиях. Таблица честно фиксирует «неизвестно» там, где текста нет.

### 3.3. «Переходы»: кто и куда двигался, и почему

| Кейс | Откуда → куда | Год | Причина (по первоисточнику) | Урок для шаблона |
|---|---|---|---|---|
| Airbnb | React Native → native Swift/Kotlin | 2018 | «product felt slow, team frustrated, architecture held them back» (по вторичным; Medium 403) | общий UI-рантайм = цена, которую платят все экраны |
| Dropbox | shared C++ core → native | 2019 | «(not so) hidden cost»: нестандартный код дороже, чем писать дважды | [03 §3.3] — общим делать только то, что реально дублируется |
| Slack (Duplo) | Obj-C/Java → 100 % Swift / 92 % Kotlin, модуляризация | 2020–2022 | «stabilize, modularize, modernize»; Anvil для скорости сборки | модульность и DI-кодоген окупаются на масштабе |
| McDonald's | native ×2 → KMP-логика (платежи → всё) | 2022 | меньше крэшей, единая команда | KMP входит «через одну фичу» |
| Bitwarden | Xamarin → native Swift/Kotlin | 2024 | (переписка; см. [01]/[02]) | уход с кросс-платформы третьей стороны |
| Shopify | native → all-in React Native (2020) → «RN подтверждён» (01.2025) → **native Swift+Kotlin** | **2026-09-10** | «coding models have gotten dramatically better… building the same feature in Swift and Kotlin no longer carries the cost it used to» | [03 §3.4]: процессные находки (чекпойнты, хеш плана, headless-логика) берём; про кодовую базу — B устойчив |
| Proton Mail | native ×2 → **Rust core (80 %) + SwiftUI/Compose** | 2025–2026 | скорость (×2), crash-rate 0.05 % iOS, недельные релизы | шаринг «ядро + native UI» работает и на mail-клиенте, не только на крипто-протоколе |
| Google (Migration Assistant) | iOS/RN/web → native Android | 2026-05 | вендор автоматизирует переезд **в native**, а не из него | направление индустрии — к нативу с агентами |
| Avito / Яндекс / Ozon | native → native + **BDUI** | 2022–2026 | обход релизного цикла, скорость экспериментов | опция-шов, не база |
| Яндекс Про | Flutter (остаётся Flutter), CocoaPods → SPM | 2026-07 | инфраструктурная миграция внутри Flutter | Flutter-проекты тоже платят «iOS-налог» (SPM, arm64-симулятор) |
| Максим Плахута (Онтико) | CMP → «больше так не делал бы» | 2025-04 | tooling, стектрейсы, найм | CMP — только осознанно |

Общая траектория 2018–2026: **маятник «общий UI ↔ native» качнулся к нативу**, а «общая
логика» стала нормой (KMP/Rust) — граница проходит там, где заканчивается бизнес-логика и
начинается платформенный опыт ([03 §3.3]). Новое в 2026 — **агенты как аргумент за native**
(Shopify) и одновременно **агенты как аргумент за меньшую поверхность кода** (Philo, [03 §3.1]):
индустрия разделена, и [03] уже посчитал, что B устойчив к обоим тезисам.

### 3.4. Российская специфика инженерии (работа под санкциями)

Дистрибуция, push, платежи, право — в [05]; здесь — **инженерная среда**.

| Аспект | Состояние на 2026-09-17 | Источник | Что это значит для шаблона |
|---|---|---|---|
| **JetBrains IDE** | Продукты скачиваются; **оплата из РФ напрямую невозможна** («payments may be rejected due to regulatory requirements and territorial limitations»), рынок посредников (Oplatym и т. п.); бесплатные лицензии OSS/студентам остаются; **Android Studio, Kotlin, Kotlin LSP (Apache-2.0), Kotlin Toolchain — бесплатны** | vc.ru/dtf.ru гайды 2026 [П~]; [03 §1] | шаблон **не должен требовать** платной IDE: `make`-гейт, CLI, LSP, `android` CLI; IDEA Ultimate — опция |
| **GitHub** | С 04.2022 блокировки аккаунтов компаний из санкционных списков (СберТех, АльфаЛаб…); к 2025 — случаи полной блокировки орг-аккаунтов, в т. ч. платных; частные аккаунты работают; 152-ФЗ (с 2025-07-01) ограничивает трансграничную обработку ПДн | securitylab.ru, mosdigitals.ru [П~] | экосистема stit уже на **Gitea** — правильно; OSS-зависимости с GitHub тянутся через Maven Central/CocoaPods/SPM без аккаунта; **не завязывать гейт на GitHub Actions** (у нас и так нет раннеров) |
| **Apple** | Developer Program — оплата через иностранное юрлицо/посредника; Apple прекратила платежи в РФ с 2026-04-01; Xcode скачивается (нужен Apple ID); Mac mini — серый импорт ~70 тыс. ₽ | [05 §1.3, §6.2] | iOS-ветка блокируется решением владельца о юрлице (O-1 в [05]) |
| **Google** | Play Console доступен; монетизация для РФ-счетов выключена с 2024-12-26; FCM-тракт открыт, аналитический тракт (GA/GTM) деградировал; **developer verification** с 2026-09-30 — статус РФ противоречив | [05 §1.2, §2.0, §4.4] | Firebase — не закладывать; push — мульти-провайдер; верификацию проходить заранее |
| **Импортозамещение SDK** | Аналитика: **AppMetrica** (Яндекс, бесплатно), MyTracker (VK); карты: Яндекс MapKit, 2ГИС SDK (C++-ядро, Swift/Kotlin/Dart); вход: VK ID, Яндекс ID, Сбер ID, ЕСИА; push: RuStore Push, HMS; крэши: Sentry self-host/GlitchTip | kokoc.com/AppMetrica гайд 2026 [П~]; [05 §3–4] | все внешние SDK — за швами (`TelemetrySink`, `PushProvider`, `MapProvider`, `AuthProvider`) |
| **Yandex-стек как «платформа»** | DivKit (OSS SDUI, Android/iOS/Web), AppMetrica, MapKit, Yandex Cloud, «Яндекс Про» на Flutter | github.com/divkit/divkit [П]; dev.go.yandex [П] | DivKit — единственный OSS-BDUI с RU-поддержкой; кандидат в опцию «SDUI-виджет» |
| **BDUI как ответ на релизный цикл** | Avito, Яндекс, Ozon, Альфа — основной путь «менять UI без релиза» | §3.2 | опция, не база |
| **Российские мобильные ОС** | «Аврора» — госсектор/КИИ, отдельный стек; ФСТЭК против AOSP в КИИ | anti-malware.ru 2026-07-13 [П~] | вне шаблона; открытый вопрос владельцу для гос-заказчиков |
| **Найм** | нативные Kotlin/Swift — 333 + 243 вакансии; KMP — 9; Flutter — 89 | [03 §4] | обычные роли; KMP — строка в стеке |
| **Конференции** | Mobius (весна: 2026-05-12…13, Москва + онлайн; осень: 2026-10-21), AppsConf; на Mobius 2026 Spring — треки про AI-агентов (Firebase AI Logic, agent loops, Koog; OpenCode как ревьюер MR в GitLab) | mobiusconf.com [П~] | источник свежих RU-практик, не индексируемый как текст |

**Вывод §3.** Индустрия — и мировая, и российская — построена одинаково: **нативный UI,
дорогая логика — в общем ядре (KMP/Rust) там, где она есть, модульность с машинной проверкой
границ, тесты без устройства, агентная обвязка в репозитории**. Российское отличие —
BDUI как штатный инструмент скорости и жёсткая гигиена внешних SDK. Оба отличия ложатся в
шаблон как **швы/опции**, а не как база.

## 4. Фундаментальное vs модное

Критерий «фундаментально»: практика существует ≥ 10 лет, переживает смену UI-фреймворков и
языков, и в 2026 году её **одновременно** предписывают Google, Apple (или их канонические
сэмплы) и живые продовые OSS-репозитории. Критерий «мода»: практика привязана к конкретной
библиотеке/фреймворку и уже сменялась хотя бы раз.

### 4.1. Практики, которые держатся 10+ лет

| Практика | С каких пор | Почему держится | Подтверждение 2026 |
|---|---|---|---|
| **Unidirectional data flow** (state ↓, events ↑) | Flux 2014 → Redux 2015 → MVI 2017 → Android UDF-guide 2021 → SwiftUI data flow | единственная модель, при которой состояние экрана воспроизводимо и тестируемо без UI; Compose/SwiftUI сами построены на ней | Android recommendations: «Follow UDF — **Strongly recommended**», «**Do not send events from the ViewModel to the UI**» (страница обновлена 2026-04-26) [П, https://developer.android.com/topic/architecture/recommendations]; Apple «Managing model data» [02 §2.1]; Bitwarden Processor/Store, Circuit, Element X (Molecule) [01 §4.3, 02 §2.4]; Shopify — «structural and mental model symmetries» SwiftUI↔Compose как основа агентной миграции [03 §3.4] |
| **Immutable state / value semantics** | Kotlin `data class`, Swift structs (2014–2016) | стабильность Compose/SwiftUI-перерисовок, безопасная конкуррентность (Swift 6 Sendable), детерминизм тестов | «Expose a UI state» (Android recs) [П]; Compose stability [01 §6]; Swift 6 strict concurrency [02 §1]; Konsist `KonsistImmutableTest` в Element X [01 §4.6] |
| **Single source of truth + repository/ports** | Repository pattern (Fowler 2002), Android guide 2017+ | одна точка правды о данных переживает любой UI и любую БД; ровно то, что делает `shared`/домен переносимым | «Expose application data from the data layer using a repository — Strongly recommended» [П]; NiA `core:data` [01 §4.2]; протокольные зависимости в iOS-каноне [02 §2.8]; go-backend «порты + адаптеры» [бриф] |
| **Dependency injection (конструкторная)** | Dagger 1 (2012), Swinject (2015) | тестируемость через фейки; границы модулей; агенту проще собрать граф, чем искать глобальные синглтоны | «Use dependency injection (constructor injection when possible) — Strongly recommended; Use Hilt — Recommended» [П]; iOS: протокол + `init`/`Environment` [02 §2.8]; реализация меняется (Dagger→Hilt→Metro/Koin), принцип — нет |
| **Модульность по фичам с машинной проверкой границ** | iOS-фреймворки/Gradle-модули ~2015; NiA 2022 | параллельная работа команд и агентов, инкрементальные сборки, невозможность cross-feature импорта «по построению» | все 8 живых OSS-репо [01 §4.5]; Wise 300+ модулей, Telegram 274, Slack 92 % [§3.1]; JET `ModularArchitectureValidator` [02 §1]; FSD/Steiger и depguard в наших шаблонах [бриф] |
| **Пирамида тестов с фейками, а не моками** | xUnit; Google Testing Blog (fakes) 2012+ | фейки переживают рефакторинг, моки фиксируют реализацию; для агентов — меньше ложно-зелёных | «Prefer fakes to mocks — Strongly recommended» [П]; Google Testing Blog «Increase Test Fidelity By Avoiding Mocks» (2024-02-27) [П, https://testing.googleblog.com/2024/02/increase-test-fidelity-by-avoiding-mocks.html]; `*-test`-модули с фейками у всех живых OSS [01 §4.5]; DORA 2025 «Fortify your safety nets» [П] |
| **Тесты без устройства (JVM/pure-Swift) + скриншоты как регрессия** | Robolectric 2010, screenshot-tests 2015, snapshot-testing 2017 | скорость цикла (секунды vs минуты), детерминизм; «глаза» агента | Paparazzi/Roborazzi/official `compose.screenshot`, «эталоны пишет CI» [01 §4.5]; Preview→snapshot (Element X, DoorDash) [02 §6.9, §3.1]; pure-Swift `swift test` на Linux [02 §1] |
| **Feature flags** (release/experiment/ops/permission) | Hodgson/Fowler 2017 (описание практики 2010-х) | trunk-based без long-lived веток; kill-switch без релиза — на мобильных особенно, т. к. **отката нет** [05 §7] | martinfowler.com/articles/feature-toggles.html (2017-10-09): «toggles as inventory with carrying cost», удалять за дни/недели [П]; H&M — общая feature-toggle библиотека на KMP [03 §3.1]; `flagkit` в golibinfra [07] |
| **Offline-кеш / stale-while-revalidate** | HTTP-кеширование; Android offline-first guide | мобильная сеть ненадёжна всегда; кеш = UX и меньше нагрузки | Android offline-first [01 §2]; Proton offline mode в новых приложениях [П]; профиль «offline-tolerant» [бриф] |
| **Accessibility как инвариант** | VoiceOver 2009, TalkBack 2009 | законодательство (EU EAA 2025), ревью, и — новое — агенты добавляют VoiceOver-лейблы сами | Apple Accessibility Nutrition Labels (WWDC25) [П]; Uber uSpec (a11y-спеки агентом) [П]; МТС на Mobius 2025 [П~]; Xcode 27 agents «Add VoiceOver labels» [П] |
| **Локализация как код (каталоги строк, plural rules)** | `strings.xml`/`.strings` с первого дня платформ | ru/en с плюралами — типовой источник багов; агенты в Xcode 27 переводят каталог | String Catalogs, per-app language, `LocaleConfig` [01 §3, 02 §6.16]; Xcode 27 «Generate Translations» [П] |
| **Crash/ANR/perf-телеметрия как гейт релиза** | Crashlytics 2011, Play vitals 2017, Xcode Organizer 2018 | сторы штрафуют видимостью; «rollback = новый релиз» | Play core vitals 1.09 %/0.47 % + требования 2027 [П]; Xcode 27 Organizer **Metric Goals** [П]; Proton 0.05 %, Shopify 99.95 % сессий [П, 03] |
| **CI-гейт как единственный вход в main** | 2010-е | DORA: AI ↑ throughput, ↓ stability — компенсируется только автоматическими проверками | DORA 2025 [П]; Shopify чекпойнты «докажи тестом» [03]; `make check`/`pnpm check` в экосистеме [бриф] |
| **Trunk-based + релизные поезда + фичефлаги** | Google/Facebook 2010-е; мобильные «release trains» | недельные релизы Duolingo/Proton; «Android and iOS must be at feature parity at all times» (Shopify) | [П, 03]; версионирование из тега [05 §7.1] |
| **Code review (теперь — и adversarial-ревью агентов)** | всегда | последний человеческий барьер; Shopify — два adversarial-ревьюера на чекпойнт | [03 §3.4]; Uber >70 % PR от агентов — при этом PR-процесс сохранён [П] |
| **Типизированный контракт с бэкендом** | Swagger 2011 → OpenAPI 3 | единственный общий артефакт двух нативных баз; кодоген детерминирован | huma OpenAPI 3.1 [бриф]; Swift OpenAPI Generator / openapi-generator kotlin [07] |
| **Отделение бизнес-логики от фреймворка** (чтобы тесты шли на JVM/pure-Swift) | Clean Architecture 2012, Hexagonal 2005 | «headless логика» — центральный принцип Shopify 2026 и основа KMP-`shared` | «business logic should be completely decoupled from the UI and be able to run headlessly» [03 §3.4]; [02 §1 п. 9] |

### 4.2. Маятник «паттерн года» — где он сейчас

| Ось | ~2014 | ~2018 | ~2022 | **2026** | Рекомендация |
|---|---|---|---|---|---|
| Презентация (Android) | MVP (Moxy) | MVVM + LiveData | MVI-фреймворки (Mavericks, Orbit), Circuit | **`ViewModel` + `StateFlow<UiState>` + UDF** — официальный канон; MVI-фреймворки — ниша Slack/Cash-лагеря [01 §2, §4.3] | ViewModel+UiState; sealed-события внутрь, не наружу |
| Презентация (iOS) | MVC → VIPER | MVVM + RxSwift | TCA | **MV / MVVM-lite на `@Observable`**; «Store + Reducer руками» (Bitwarden); TCA — точечно [02 §2] | `@Observable`-модель экрана только где есть async/ветвление |
| Асинхронность (Android) | AsyncTask/Rx | RxJava 2 | coroutines + Flow | **coroutines + Flow** — у 7 из 8 живых OSS (Rx только в Signal) [01 §4.4] | Flow; Turbine в тестах |
| Асинхронность (iOS) | GCD/closures | RxSwift | Combine | **async/await + AsyncSequence**; Combine заморожен [02 §2.6] | правило «`import Combine` требует обоснования» |
| DI (Android) | Dagger 1/2 | Dagger 2 + Anvil | Hilt | **Hilt** (recs), **Metro 1.x** (компиляторный плагин, KMP) у Slack/Element, Koin 4 в KMP [01 §1] | Hilt для Android-only; Koin/Metro при KMP — решение [08] |
| DI (iOS) | Swinject | Needle | Factory / swift-dependencies | **протокол + init/Environment**, `swift-dependencies` для подмен [02 §2.8] | без контейнера-фреймворка |
| UI (Android) | XML + Views | XML + DataBinding | Compose (alpha→stable 2021) | **Compose — «our standard», Views maintenance mode** [П]; TTID «comparable to Views» (Compose 1.12) | 100 % Compose |
| UI (iOS) | UIKit + storyboards | UIKit + code | SwiftUI (частично) | **SwiftUI-first**; UIKit — точечно; Liquid Glass — системный слой [02 §1] | SwiftUI |
| Навигация (Android) | Activities/Fragments | Navigation Component | Navigation Compose (строки → `@Serializable`) | **Navigation 3** — берут оба лагеря и Signal [01 §4.3] | Nav3 |
| Навигация (iOS) | Segues | Coordinators | `NavigationStack` | **`NavigationStack` + типизированный `[Route]`**, deep link = чистая функция [02 §2.5] | Router-объект |
| Кодоген (Kotlin) | annotation processing (apt) | kapt | KSP | **KSP2** везде; компиляторные плагины (Metro, Compose, Т-Банк «эффективнее KSP») [01 §4.4, §3.2] | KSP; никакого kapt |
| Сборка (Android) | Gradle Groovy | Gradle KTS + buildSrc | version catalog + convention plugins | **catalog + `includeBuild(build-logic)`** у всех [01 §4.5]; Amper → Kotlin Toolchain Alpha; Bazel — Spotify/Telegram/Uber на масштабе | Gradle + catalog; Toolchain — наблюдать |
| Сборка (iOS) | CocoaPods | Carthage/CocoaPods | SPM + XcodeGen/Tuist | **SPM + buildable folders**, генераторы — при многих таргетах (Wise → Tuist, Bitwarden/Element → XcodeGen) [02 §3]; CocoaPods → SPM даже у Flutter (Яндекс Про) | SPM; XcodeGen как страховка |
| Тесты UI | Espresso/XCUITest на устройстве | + Robolectric | screenshot на JVM | **Compose-тесты на Robolectric + Roborazzi/official screenshot; Preview-snapshots; Maestro** для e2e [01 §7, 02 §4] | JVM/Preview — в гейт; Maestro — отдельная стадия |
| Тесты (iOS-фреймворк) | XCTest | XCTest + Quick/Nimble | XCTest | **Swift Testing** дефолт, XCTest для UI/perf [02 §1] | Swift Testing |
| Моки | Mockito/OCMock | MockK/Cuckoo | Sourcery/Mockolo | **фейки** (official) + генерация где нужно [П] | фейки в `*-test`-модулях |
| Кросс-платформа | Xamarin, Cordova | React Native, Flutter | KMM (alpha) → KMP stable 2023 | **native UI + shared logic** (KMP/Rust) — консенсус; общий UI (CMP/RN/Flutter) — откаты (Shopify) [03] | B из [03] |
| Дизайн-система | Material 1 / iOS 7 flat | Material 2 | Material 3 / SF | **M3 Expressive / Liquid Glass**; собственный токен-слой переживает всё | свой токен-слой поверх платформы [01 §3, 02 §3] |
| Персистентность (Android) | SQLite/ORMLite | Room / Realm | Room + DataStore | **Room 3 (KMP)** / DataStore; для offline-tolerant — часто без БД [01 §1] | DataStore + кеш; Room по необходимости |
| Персистентность (iOS) | Core Data | Realm | SwiftData (хайп) | **без ORM**; GRDB если нужен SQLite; SwiftData — боли миграций [02 §1] | файловый кеш; GRDB опционально |
| AI-инструменты | — | автодополнение | Copilot-чат | **агенты в IDE/CLI + скиллы + MCP/ACP** у всех трёх вендоров [§1] | обвязка репо (`CLAUDE.md`/rules/skills) |

### 4.3. Что «ушло» (или уходит) — не брать в шаблон

| Что | Статус | Источник |
|---|---|---|
| React Native / Flutter как основа для native-first компании | Shopify 2026-09-10 — уход; Airbnb 2018; Google Migration Assistant — миграция **в** native; Flutter в РФ — ниши | [03 §2.F, §3.4], §3.3 |
| «KMP = общий UI» (дуга: KMM-хайп 2019 → «logic only» консенсус 2026; CMP-хайп 2025 → «с нативной оболочкой») | CMP stable, но JetBrains сами требуют SwiftUI-оболочку под Liquid Glass; RU-отказы | [03 §2.C], §3.2 |
| No-code/low-code мобильные конструкторы | вытеснены «app from prompt» (Google AI Studio → **настоящий Compose-код**) и BDUI у крупных; low-code остался внутри BDUI (Avito Bricks) | [П], §3.2 |
| MVP, VIPER (для новых SwiftUI-приложений), RxJava/RxSwift, kapt, `LiveData`, `AndroidViewModel`, `GlobalScope`, Accompanist, Combine (в новом коде), Objective-C | вытеснены; см. §4.2 | [01 §12 TL;DR, 02 §2] |
| Xamarin (retired 2024-05), Microsoft App Center (закрыт 2025-03-31) | закрыты вендором | [05 §4.4]; Bitwarden ушёл с Xamarin [02] |
| CocoaPods как основной менеджер | миграции на SPM у Wise, Яндекс Про; CocoaPods trunk — режим сопровождения [П~] | §3.1, §3.2 |
| `.pbxproj` как рукописный артефакт | buildable folders (Xcode 16+), генераторы; агентам — запрет | [02 §6.18], §5.2 |
| Firebase как «база» мобильного приложения (в РФ) | четыре независимых аргумента против | [05 §4.4] |
| Строковые navigation-роуты, XML-View, Fragments | только в стагнирующих сэмплах | [01 §4.4] |
| «Clean Architecture» с use-case на каждый вызов | «расцвет достаточно хорошей архитектуры» (Habr-дайджест 02.2026), domain-слой — «recommended in big apps», не обязателен | [П], [01 §12] |

### 4.4. Ожидания качества — пороги как гейты

| Метрика | Порог / ориентир | Откуда | Как использовать в шаблоне |
|---|---|---|---|
| User-perceived crash rate (Android) | **< 1.09 %** overall, < 8 % per device model | Play core vitals [П] | гейт промоушена в production (Sentry/GlitchTip → скрипт `/release`) |
| User-perceived ANR rate | **< 0.47 %** overall | Play [П] | StrictMode в debug; Macrobenchmark; запрет блокирующих вызовов на main (detekt) |
| Partial wake locks | < 5 % | Play [П] | WorkManager, никаких самописных фоновых циклов |
| Память (RSS+Swap, p90) | 2 ГБ fg / 1 ГБ bg на 4 ГБ RAM (с 02.2027) | Play [П] | LeakCanary в debug (теперь встроен в Studio), memory-бюджет в Macrobenchmark |
| Оптимизация DEX | ≥ 25 % (с 02.2027, DEX > 10 МБ) | Play [П] | R8 full mode обязателен в release; R8 Configuration Analyzer |
| 16 KB page size, 64-bit | обязательно | Play [П] | проверка в гейте (`aapt`/badging) |
| Zero-Tap Sign-In restoration | обязательно с 04.2027 для приложений с логином | Play [П] | reference-фича `auth`: Credential Manager + Restore Credentials |
| iOS: hang rate, launch time, disk writes, battery, storage, animation hitches | **Metric Goals** Xcode 27 Organizer — калибруются по похожим приложениям + по истории вашего | WWDC26 #258 [П] | дашборд релиза; агентные «Generate Recommendations» после каждого релиза |
| Crash-free sessions (индустриальная норма) | **≥ 99.9 %** для B2B; лучшие: Shopify native 99.95 %+, Proton iOS 0.05 % crash rate, OpenSooq > 99.7 %, Worldline > 99 % | [03 §3.1, §3.4], [П] | порог 99.9 % как условие STOP-2 [05 §7.4] — **предложение [?]** |
| Холодный старт | Shopify native: 2.2–2.5 с (их бенчмарк на реальных устройствах); Compose TTID «comparable to Views» | [03 §3.4], [П] | бюджет старта в Macrobenchmark (`startup`), Baseline Profiles [01] |
| Размер | Shopify: native Android 184 МБ vs RN 293 МБ; Markaz CMP — 137 МБ iOS | [03] | size-бюджет в гейте (аналог `size-limit` в SPA) |
| Доступность | Accessibility Nutrition Labels (App Store), TalkBack/VoiceOver-лейблы | [П] | axe-подобные проверки в Compose-тестах, `AccessibilityTests` таргет как у Element X |

**Вывод §4.** Фундамент — 16 практик из §4.1; все они **независимы от языка и UI-фреймворка**
и уже присутствуют в `go-backend-template`/`spa-frontend-template` в той или иной форме.
Маятник в 2026 году стоит в положении «официальный канон вендора + декларативный UI +
типизированная навигация + фейки», и это положение **совпадает у Google и Apple** — редкий
момент, когда «делать как вендор» и «делать фундаментально» — одно и то же. Пороги качества
впервые стали **нормативными** (Play 2027, Xcode Metric Goals) — их место в гейте, не в
документации.

## 5. AI-агенты в мобильной разработке

Эмпирика инструментов на этой машине — у потока 06; здесь — карта «что существует и что
говорит сообщество», с акцентом на **Linux-хост без Mac**.

### 5.1. Что реально работает в 2026 (по первоисточникам)

| Инструмент | Вендор / статус (дата) | Что делает | Хост | Для нашего Linux-first цикла |
|---|---|---|---|---|
| **Xcode 26.3 agentic coding** | Apple, релиз 2026-02-26 (RC 02-03) [П] | Claude Agent SDK (subagents, background tasks, plugins) и Codex внутри Xcode; агент ищет доки, меняет project settings, проверяет через Previews, итерирует build→fix; Xcode как **MCP-сервер** | **macOS** | недоступно; полезно на «удалённом Mac» стадии `check-ios` |
| **Xcode 27** | Apple, 2026-09-14 [П] | модели Anthropic/Google/OpenAI, `/plan`, скиллы (SwiftUI, accessibility, universal sizing, testing, performance), MCP + **ACP**, Device Hub, RenderPreview/String-Catalog MCP-инструменты, агентная локализация, Organizer-рекомендации | macOS | то же; описать в `CLAUDE.md`, какие MCP-инструменты ожидаются на Mac [02 §6.18] |
| **XcodeBuildMCP** | Cameron Cooke → **Sentry** (2026), 6 396★, push 2026-08-02 [П] | build/test/simulator/install/launch/tap/logs; result bundles по умолчанию; `XCODEBUILDMCP_HEADLESS_LAUNCH`; tvOS/watchOS/visionOS; CLI-режим | macOS | инструмент Mac-стадии; сравнить с нативным Xcode MCP — [06] |
| **Maestro + MCP** | mobile-dev, MCP с 02.2026; 15 648★, push 2026-09-15 [П]; форк luxury-labs/maestro-mcp — 32 инструмента | YAML-flows, агент «boots the simulator, installs the app, navigates every screen…»; Android-эмулятор/устройство, iOS-симулятор, web | Linux (Android) / macOS (iOS) | e2e-стадия вне быстрого гейта; на этой машине эмулятор — по памяти [06] |
| **Android CLI 1.0 + Skills + Knowledge Base + Journeys** | Google, stable 2026-06-09 [П] | `android create/describe/docs/sdk/emulator/run/screen capture/layout --diff/skills/journeys`; «−70 % токенов, ×3» (методика не раскрыта); агенты: Claude Code, Codex, Gemini CLI, Antigravity | **Linux** | базовый кирпич агентного цикла Android; телеметрия по умолчанию — `--no-metrics` [01 §6]; эмпирика [06 TL;DR п. 4]: бинарь `android` (v1.0.16261425) идёт в `cmdline-tools 23.0`, `sdkmanager` объявлен deprecated, `android create` даёт проект сразу на AGP 9 + Navigation 3 |
| **Android Studio Agent Mode / Gemini / Gemma 4** | Google, Quail (2026) [П] | параллельные агентные диалоги, New Project Agent, LeakCanary «Fix with Agent», MCP-маркетплейс, локальная Gemma 4 (12+ ГБ RAM) | GUI (IDE) | не для headless; `android studio …` требует запущенной IDE [01 §6] |
| **Android Bench** | Google, I/O 2026 [П] | лидерборд моделей на реальных Android-задачах, включая Gemma 4 / Gemini 3.5 Flash | web | выбор модели/провайдера |
| **Google AI Studio → native Android**, **Migration Assistant** | Google, I/O 2026 [П] | приложение из промпта на Compose/Kotlin; iOS/RN/web → Android агентом | cloud | не для шаблона; сигнал направления |
| **AppFunctions / Android MCP** | Google, private preview [П] | приложения как on-device MCP-серверы | Android 17 | шов на будущее |
| **JetBrains Junie / Air / Claude в IDEA & AS** | JetBrains, KotlinConf'26 [П] | агент с IDE-контекстом (CLI-версия), мульти-агентная среда (worktrees/Docker), ACP | IDE / CLI | Junie CLI — опционально; Claude Code — уже наш инструмент |
| **Kotlin LSP** (Apache-2.0), **`kdoc.jar`**, **klibs.io MCP**, **Compose Hot Reload MCP**, **Kotlin MCP SDK** | JetBrains, Alpha/stable [П, 03] | type-check без Gradle, машиночитаемые доки, поиск KMP-библиотек, «reload + screenshot + semantic tree» | **Linux** | ключевые преимущества Kotlin-ветки [03 §2.B]; проверка — [06-11, 06-12] |
| **Kotlin SWE-bench** (110 задач; Claude Code Opus 4.7 — 86.4 %) | JetBrains + Anthropic [П] | бенчмарк агентов на реальных Kotlin-репо | — | обоснование выбора модели |
| **Claude Code / Cursor / Codex + `gradlew` / `xcodebuild`** | — | универсальные агенты поверх CLI | Linux / macOS | наш основной режим; обвязка `CLAUDE.md` + rules + skills |
| **Shopify Helix / Tardis** | внутренние (описаны 2026-09-10) [03 §3.4] | чекпойнт-миграция с adversarial-ревью; агентный доступ к живому состоянию приложения | — | воспроизводимые принципы: `:shared:cli`, чекпойнты в `/plan` |
| **Uber uSpec** | Uber, 2026-03-11 [П] | Claude в Cursor + Figma Console MCP → a11y-спеки (VoiceOver/TalkBack/ARIA) «< 2 мин»; «AI judgment + programmatic precision» | — | образец разделения «агент решает / скрипт исполняет» для наших скиллов |
| Community: `Claude XcodePreviews`, Swift Testing Agent Skill (avanderlee), `Drjacky/claude-android-ninja`, `rcosteira79/android-skills`, `sosumi.net` / apple-docs-mcp | 2026 [П~] | превью/тесты/скиллы для агентов | Linux/macOS | кандидаты на вендоринг; качество проверять [06] |

### 5.2. Что говорит сообщество: продуктивность и ловушки

| Ловушка | Платформа | Свидетельство | Митигация в шаблоне |
|---|---|---|---|
| **`.pbxproj`** — «practically unparseable for AI agents», merge-conflict magnet; «never let them touch .pbxproj files» | iOS | dev.to «How to Automate iOS Development Without Breaking .pbxproj Files»; blakecrosley.com «Building iOS Apps with AI Agents» [П~] | buildable folders + модульность в `Package.swift`; правило «`.pbxproj` правится только Xcode/генератором» [02 §6.18]; XcodeGen как страховка |
| Code signing, provisioning | iOS | там же; [02 §6.1] | deny в `.claude/settings.json`; подпись — операция человека |
| Симулятор «slow, unreliable, missing basic features»; инфра-ошибки (runtime mismatch, derived data, package resolution) выглядят как ошибки кода | iOS | blakecrosley.com [П~]; byteiota «Xcode 26.3 AI agents won't fix its rotten foundation» [П~] | фиксированный симулятор/OS, `xcbeautify`, разбор `.xcresult`, `build-for-testing` [02 §6.18] |
| **Галлюцинации версий API**: агент пишет `kotlinOptions {}` из AGP 8, `onChange(of:)` старой сигнатуры, SwiftUI-модификаторы прошлых версий | обе | [01 §6, 02 §6.8]; knowledge cutoff отстаёт от платформы на месяцы | `android docs search/fetch`, Apple Document Search (Xcode 27), `kdoc.jar`, sosumi; **пин версий и «что не так в 2026» прямо в `CLAUDE.md`**; вендорские скиллы вместо памяти |
| **Gradle loop friction** — агент гоняет полную сборку на каждую правку | Android | vmobify/claudelab guides 2026 [П~] | правило «не собирать без просьбы» + Kotlin LSP для type-check; `:shared:jvmTest` как основной цикл; `--console=plain -q`, JUnit XML |
| **«Compiles but runs wrong»** — утечки lifecycle, «state scatter», нарушения Rules of Compose | Android | там же [П~] | Konsist/detekt/compose-rules ловят часть; Robolectric-тесты состояния; ревью |
| Context compaction на длинных сессиях; лимиты в пиковые дни | обе | [П~] | маленькие чекпойнты (Shopify), `/plan` с хешем, `AGENTS.md`-карта модулей |
| **Дрейф снапшотов** — агент «чинит» код вместо перезаписи эталона | обе | [02 §6.18] | явная команда `make snapshot-record`; правило «эталон не перезаписывается без причины в коммите» |
| Много «правильных» архитектур → агент смешивает паттерны | обе | [02 §6.18] | жёсткие инварианты в `CLAUDE.md` + `/scaffold` генерирует канон |
| Низкое доверие и «almost right» | обе | SO 2025: 3.1 % highly trust, 66 % «almost right» [П]; DORA: stability ↓ [П] | гейт обязателен; негативные фикстуры линтеров [06] |
| Телеметрия инструментов по умолчанию | Android CLI | [01 §6] | `--no-metrics` в `make setup` |
| Стоимость | — | Uber: cap $1 500/разработчик/мес после ×6 роста расходов [П~] | планирование через `/plan`; JVM-цикл дешевле симулятора |
| **Молчаливый SKIPPED**: `linkDebugFrameworkIosArm64` на Linux пропускается с кодом 0 — «зелёная» сборка не значит, что iOS-framework собрался | KMP | [06 TL;DR п. 7] | гейт печатает явную строку «стадия iOS пропущена» ([03 §6.4]); `check-ios` на Mac обязателен перед claim «готово» |
| **Два форматтера — гейт не сходится**: `detekt-formatting` (ktlint 0.50) против Spotless (ktlint 1.8); SwiftLint `trailing_comma` против `swift-format` `TrailingComma` — переписывают код друг за другом | обе | [06 TL;DR п. 11] | одна автофиксилка формата на язык; конфликтующие правила выключены явно |
| **Агент копирует «канон из памяти», который сломан новой платформой**: convention-плагины NiA не компилируются под AGP 9; `compose-rules 0.5+` требует detekt 2.0-alpha; `dependency-analysis` не знает AGP 9.4 | Android | [06 TL;DR п. 12–14] | пины совместимых связок (detekt 1.23.8 + compose-rules 0.4.28) в `CLAUDE.md`; образец `build-logic` — из spike 06, не из NiA |
| **Память, а не CPU**: Gradle-демон 3.6 ГБ + Kotlin-демон 1.2 ГБ; Android Lint падает с `OutOfMemoryError: Metaspace` при 512 МБ | Android | [06 TL;DR п. 6] | `gradle.properties` под агента (`MaxMetaspaceSize` ≥ 1 ГБ), один демон, `--max-workers` |

**Что сообщество называет работающим** (сходятся все источники): агенты **сильны** в SwiftUI-
вью/Compose-экранах, моделях, рефакторинге, диагностике ошибок сборки, тестах (Uber Autocover
5 000 тестов/мес), локализации (Xcode 27), a11y-спеках (uSpec), миграциях по чек-листу
(Android Skills: AGP 9, Nav3, edge-to-edge); **слабы** в проектных файлах, подписи, визуальной
отладке, платформенных нюансах lifecycle. Android назван «unusually fertile substrate for
agentic coding» из-за трёх свойств: массовый бэклог миграций, Gradle «rewards pattern-matching»,
«checklist-heavy development culture» [П~, claudelab]. Это ровно профиль, под который
строятся наши скиллы.

### 5.3. `AGENTS.md` / `CLAUDE.md` в мобильных open-source репозиториях

Проверено запросами к `raw.githubusercontent.com` 2026-09-17 (файлы `AGENTS.md`, `CLAUDE.md`,
`.cursorrules`, `.github/copilot-instructions.md`, `GEMINI.md` на ветках `main|develop|master`;
`master` для переименованных веток отдаёт редирект на дефолтную, поэтому дубли схлопнуты):

| Репозиторий | Файлы | Размер | Что внутри (заголовки) |
|---|---|---|---|
| `android/nowinandroid` | `AGENTS.md` | 3.0 КБ / 58 строк | Architecture, Modules, Commands to Build & Test (+ instrumented, creating tests), CI, VCS — **но описывает Navigation 2** (рассинхрон с кодом) [01 §4.2] |
| `element-hq/element-x-android` | `AGENTS.md` + `CLAUDE.md` (10 байт — указатель) | 4.7 КБ / 123 строки | **Strong Conventions**: Code Style, **PII & Logging**, Strings & Localisation, Previews; PR Guidelines; Build System; Gradle Modules; Architecture Appyx + Molecule («Files Per Screen»); DI (Metro); Compound DS; Rust SDK layer |
| `element-hq/element-x-ios` | `AGENTS.md` + `CLAUDE.md` | — | аналогично |
| `duckduckgo/android` | `CLAUDE.md` | 6.7 КБ / 138 строк | **«Privacy invariants for anything leaving the device»** первым разделом; «Read these when the situation applies» (роутер); Build & Test Commands; Build Variants; Code Formatting; Code Comments |
| `duckduckgo/apple-browsers` | оба | — | — |
| `mozilla-mobile/firefox-ios` | `AGENTS.md` + `CLAUDE.md` | 1.7 КБ / 45 строк | Global instructions, Repository Structure, Common Commands (Build & Test, JS user scripts, Linting), Pull requests, **Runbooks** |
| `mozilla-firefox/firefox` (Android внутри) | оба | — | — |
| `wireapp/wire-ios` | `AGENTS.md` + `CLAUDE.md` | 4.5 КБ / 105 строк | Project Overview, Prerequisites & Setup, Build, Testing, Linting & Formatting, Architecture, Conventions |
| `wordpress-mobile/WordPress-Android`, `-iOS` | оба | — | — |
| `nextcloud/android` | оба; `nextcloud/ios` — `AGENTS.md` | — | — |
| **`TelegramMessenger/Telegram-iOS`** | `CLAUDE.md` | **24 КБ / 211 строк** | Build; «Updating the running simulator after a rebuild (whole-`.app` copy)» с guard-rails («never rm the installed app unless SRC actually resolved»); Code Style; Project Structure; RichTextEditor; Watch app; View frame ownership; InstantPage; **«Postbox → TelegramEngine refactor (in progress)» с правилами «для каждой волны»**; tgcalls Testbench |
| `tuist/tuist` | `AGENTS.md` + `CLAUDE.md` | **27 КБ / 505 строк** | Repository Map, Global Guardrails, Intent Layer, Git/PR, Review Guidance, разделы по CLI (Swift) / Server (Elixir) / Handbook |
| `cashapp/paparazzi` | оба | 1 КБ | Repository guidance, upstream source |
| **Нет файлов** | `bitwarden/android`, `bitwarden/ios`, `signalapp/Signal-Android`, `Signal-iOS`, `ProtonMail/*`, `slackhq/circuit`, `pointfreeco/swift-composable-architecture`, `Kotlin/KMP-App-Template`, `touchlab/KaMPKit`, `cortinico/kotlin-android-template`, `skydoves/pokedex-compose`, `JetBrains/compose-multiplatform-template`, `kean/Nuke`, `DrKLO/Telegram`, `uber/RIBs`, `cashapp/redwood`, `android/architecture-templates`, `android/compose-samples`, `android/skills`, `wireapp/wire-android` | — | — |

**Паттерны, которые видны:**

1. **Первым идёт то, что нельзя нарушать** (privacy invariants у DuckDuckGo, PII & Logging у
   Element) — аналог наших `rules` с path-scoping; затем — команды сборки/тестов; затем карта
   модулей. Порядок «инварианты → команды → карта» совпадает с ядром нашего `CLAUDE.md`.
2. **Процедурные runbooks** (Telegram: как обновить приложение в симуляторе, как вести
   многоволновой рефакторинг; Firefox: «Runbooks») — это то, что у нас живёт в `.claude/skills/`.
   Там, где скиллов нет, `CLAUDE.md` разрастается до 24–27 КБ (Telegram, Tuist) — довод за наш
   формат «ядро ≤ 200 строк + роутер».
3. **`CLAUDE.md` как указатель на `AGENTS.md`** (Element X, 10 байт) — де-факто конвенция
   двух файлов; для шаблона: `AGENTS.md` = симлинк/указатель на `CLAUDE.md` или наоборот, чтобы
   работать и с Codex/Junie/Xcode-агентами (ACP).
4. **Рассинхрон файла с кодом** (NiA описывает Nav2 после миграции на Nav3) — главный
   операционный риск; наш ответ — forward-only + drift-проверки `/smartdev`.
5. **Шаблоны и библиотеки файлов не имеют** (все community-template-репо, TCA, Circuit) —
   т. е. агентная обвязка сегодня — свойство **продуктовых** репозиториев, а не стартовых;
   это ниша нашего шаблона.

### 5.4. Agent-friendly практики — короткие выводы

1. **Детерминированный CLI как единственный интерфейс** — `make check|test|lint|build|
   snapshot-record`, Gradle с `--console=plain -q`, JUnit XML, `xcbeautify`/`.xcresult`;
   агент читает `OK:/FAIL:`-строки, а не логи. Эмпирика [06 TL;DR п. 2]: полный Android-гейт
   на Linux (Spotless + detekt + Lint + JVM + Robolectric-Compose + Roborazzi + Konsist + Kover +
   `assembleRelease`/`bundleRelease`) — **37 с** при правке одного файла, **4.2 с** без изменений,
   вывод concise-обёртки — **12 строк**.
2. **Снапшоты как «глаза»**: Roborazzi/official screenshot PNG + `compare`, SwiftUI Preview →
   snapshot (Element X, DoorDash), `android screen capture --annotate` / `android layout
   --diff`, Xcode `RenderPreview` MCP; эталоны пишет CI/явная команда, не агент по ходу. Проверено [06 TL;DR п. 3]: Roborazzi
   рендерит Compose-экран в PNG под Robolectric за ~2.5 с/тест, и агент прочитал на снимке
   кириллицу и зачёркнутый текст — «глаза» на Linux работают буквально.
3. **JVM/pure-Swift тесты вместо эмулятора**: `:shared:jvmTest`, Compose-тесты на Robolectric,
   `swift test` для доменного пакета на Linux [02 §1]; эмулятор/симулятор — отдельная стадия.
   Проверено [06 TL;DR п. 7–10]: `compileKotlinIosArm64` собирает klib на Linux; Swift 6.4 —
   официальный тарболл под Debian 13, `swift test` (Swift Testing) **0.72 с** тёплый, SwiftLint
   0.65.1 нативно; `import SwiftUI`/`UIKit` на Linux — `no such module`.
4. **Headless бизнес-логика + агенто-адресуемый CLI** (принцип Shopify; у KMP — «бесплатно»
   через `:shared:cli`, [03 §6.4]).
5. **Типизированная навигация и UDF-симметрия платформ** — то, на что Shopify опирает
   «реализуй Android по iOS-референсу»; Nav3/`NavigationStack` + `@Serializable`/`Hashable`
   роуты.
6. **Свежие доки вместо памяти**: `android docs`, Apple Document Search/sosumi, `kdoc.jar`,
   klibs MCP; пины версий и «что изменилось» в `CLAUDE.md`.
7. **Вендорские скиллы поверх наших**: `android skills add --all` в `make setup`, Apple-скиллы
   с Mac; наши скиллы — про процесс (`/plan`, `/scaffold`, `/release`), их — про платформу [01 §6].
8. **Чекпойнты и хеш плана** (Shopify), `/plan` в Xcode 27 и в JetBrains — трёхстороннее
   подтверждение planning-first.
9. **Негативные фикстуры гейта** (Element X `failures/` для Konsist) — линтер обязан краснеть
   на заведомом нарушении [06].
10. **Человеческие операции — только человеку**: подпись, ключи, `--no-verify`, публикация;
    deny в `settings.json`.
11. **Телеметрия инструментов выключена по умолчанию**, лимиты расходов — процессная норма
    (Uber).
12. **`AGENTS.md` ⇄ `CLAUDE.md`** — оба имени, один источник; синхронность с кодом
    проверяется, а не предполагается.

**Вывод §5.** В 2026 году агентная поддержка мобильной разработки **асимметрична по хосту**:
Android полностью headless на Linux (CLI 1.0, скиллы, LSP, JVM-тесты, Hot Reload MCP), iOS —
богатый инструментарий (Xcode MCP/ACP, XcodeBuildMCP, Maestro), но только на macOS. Это не
аргумент против iOS, а аргумент за архитектуру «максимум логики вне UI + тонкий SwiftUI-слой +
удалённая Mac-стадия» ([03 §6.1], [05 §6.5]) и за обвязку, которая делает поведение агента
одинаковым на обоих хостах.

## 6. Open-source образцы и community-шаблоны

Глубокий разбор NiA, Bitwarden, Element X, Signal, isowords и др. — [01 §4], [02 §2.7].
Здесь — **чему учит каждый** одной строкой и что обычно содержит «шаблон» против того, что
нужно нашей экосистеме. Метаданные репозиториев сняты 2026-09-17 (звёзды — из HTML GitHub,
последний коммит — из Atom-ленты ветки; GitHub API боту отдал rate-limit).

### 6.1. Продовые образцы — чему учит каждый

| Проект | Чему учит | Чего не брать |
|---|---|---|
| **Now in Android** (21.8k★, push 2026-09-02) | эталон структуры: `feature:*:{api,impl}`, `core:*`, 17 convention-плагинов, custom `:lint` (аналог forbidigo), `graphUpdate` mermaid в README модулей, badging-baseline, Roborazzi, Nav3, `AGENTS.md` | 35 модулей на 310 файлов (учебная гранулярность); BOM из alpha-канала; `AGENTS.md` отстаёт от кода [01 §4.2] |
| **Tivi** (chrisbanes) | исторически — Circuit + kotlin-inject + CMP | **архивирован 2024-11-12**; не образец [01 §4.1] |
| **Signal** (Android 29.3k★) | производственная дисциплина в очень старом коде: свои `:lintchecks`, official Compose screenshot, Nav3 в `core:ui` рядом с Fragments | Rx, Fragments, ручной DI — наследие [01 §4.4] |
| **Element X** (Android/iOS) | максимальная строгость: `api/impl/test` навязаны кодом convention-плагина, 17 Konsist-тестов с `failures/`-фикстурами, Preview → snapshot, `AGENTS.md` с PII-инвариантами; Rust-ядро через uniffi | цена Rust-ядра для CRM-профиля [03 §2.E-1]; 266 build-файлов |
| **Bitwarden** (Android/iOS) | самый свежий стек (AGP 9.4, Kotlin 2.4.20, JUnit 6), **Gradle dependency locking STRICT**, 10 модулей — «мало модулей тоже работает»; iOS: Coordinator+Processor+State/Action/Effect, XcodeGen, ViewInspector + SnapshotTesting, Sourcery-моки | нет скриншот-тестов/GMD на Android; нет agent-файлов |
| **Wire** (iOS) | `AGENTS.md` с полным циклом setup→build→test→lint→architecture | Android-репо без agent-файлов |
| **Firefox** (iOS/Android) | Rust `application-services`, `AGENTS.md`+`CLAUDE.md` с runbooks, fastlane | огромное legacy, не эталон структуры [02 §2.7] |
| **Proton Mail** | Rust-SDK ≈ 80 % + SwiftUI/Compose, измеримый эффект (crash 0.05 %) | Rust — третий язык |
| **DuckDuckGo** (Android/apple-browsers) | `CLAUDE.md` начинается с privacy-инвариантов; «read these when the situation applies» — роутер по контексту | — |
| **WordPress**, **Nextcloud** (Android/iOS) | agent-файлы в обоих; SPM + `.xcodeproj` (WP), fastlane; «SwiftLint у всех» | legacy-архитектуры |
| **Telegram** (iOS/Android) | Bazel-монорепо 274 модуля, свой reactive/DB-слой, reproducible builds, **`CLAUDE.md` 24 КБ с runbooks и правилами многоволнового рефакторинга**; Android — Java | не переносимо (свой мир), но показывает, что агентный runbook пишут даже самые «закрытые» команды |
| **isowords** (Point-Free, 3.0k★, push 2024-08-16) | эталон «полный TCA + SwiftPM-монорепо» | стагнирует с 2024; TCA — не для нашей базы [02 §2.3] |
| **Circuit** (Slack, 1.85k★), **Redwood/Molecule/Paparazzi** (Cash App) | Compose-presenters, JVM-скриншоты, Compose→native UI | архитектурные фреймворки-«вселенные» — ниша |

### 6.2. Community-шаблоны и визарды

| Шаблон | Звёзды / последний коммит | Что содержит | Чего не хватает (по меркам экосистемы stit) |
|---|---|---|---|
| **`android/architecture-templates`** (Google) | 3 162★ / **2024-12-12** | ветки `base` и `multimodule`; `customizer.sh your.package DataItemType`; Compose + Room + Hilt + ViewModel + **Navigation 2** + Coroutines/Flow; unit + UI-тесты с фейками через Hilt; KTS + version catalog | **стагнирует** (до AGP 9, до Nav3); нет lint-дисциплины/detekt, скриншотов, coverage-гейта, agent-файлов, docs |
| **`cortinico/kotlin-android-template`** | 1 911★ / 2026-09-07 | 4 модуля (app, android lib, kotlin lib, Compose activity), **detekt + ktlint**, GitHub Actions (wrapper validation, pre-merge `build/check/publishToMavenLocal`), публикация в Maven Central, Issue/PR templates | ориентирован на **библиотеки**; нет архитектурного канона, скриншотов, coverage, gate без CI-раннеров, agent-обвязки |
| **`skydoves/pokedex-compose`** | 1 132★ / 2026-04-10 | MVVM по Google-гайду, Hilt, Room, Retrofit, KSP, Turbine, **Baseline Profiles**, shared element transitions, Compose HotSwan | учебное приложение, не шаблон; нет гейта/линтеров-как-политики |
| **`Kotlin/KMP-App-Template`** (JetBrains) | 695★ / 2026-09-11 | Android/iOS/desktop/web; CMP-UI; Ktor, kotlinx.serialization, Coil, Koin, Compose Navigation; ветка **`kotlin-toolchain`** | тесты/гейт в README не заявлены; нет agent-файлов; UI общий (наш вариант — B) |
| **`Kotlin/KMP-App-Template-Native`** (JetBrains) | — / активен | то же с **нативным UI** (Compose + SwiftUI), KMP-ObservableViewModel, KMP-NativeCoroutines; ветка `kotlin-toolchain` | **ближайший к рекомендации [03]** стартовый образец; те же пробелы (гейт, тесты, agent-файлы, docs) |
| **`touchlab/KaMPKit`** | 2 453★ / 2026-09-01 | «best practice model» от Touchlab: SKIE, shared ViewModel, интеграционные гайды в существующее приложение | коммерческий вендор-контекст; нет гейта/agent-файлов |
| **`JetBrains/compose-multiplatform-template`** | 638★ / **2023-12-19** | — | **архивирован**, отсылает к визарду kmp.jetbrains.com |
| **kmp.jetbrains.com (визард)** | — | выбор платформ и «share UI / native UI»; генерирует новую дефолтную структуру `shared/ + androidApp/ + iosApp/` [03 §2.B] | стартовая точка, не шаблон: без тестов-политики, линтеров, гейта, docs |
| **`android create <template>`** (Android CLI) | stable 1.0 | официальные шаблоны, напр. `empty-activity-agp-9` [01 §6.1] | каркас проекта, не процесс |
| **Xcode 27 templates** | — | SwiftUI App, macOS CLI, Swift Package, Playground; «untitled project» без имени [П] | то же |
| **Tuist** (`tuist init`, 5 804★ / 2026-09-16) | — | генерация проекта из Swift-манифеста, шаблоны; свой `AGENTS.md` 27 КБ | ценность — в платном облаке (cache/selective testing) [02 §1]; для нас — XcodeGen как страховка |
| **`pointfreeco/*`** (TCA 1.26.2, swift-dependencies, swift-snapshot-testing, swift-navigation) | активны | библиотеки-«кирпичи», isowords как сборка | не шаблон; берём отдельные библиотеки [02 §2.3] |
| **`kean/Nuke`, `kean/Pulse`** | активны | образцы упаковки SwiftPM-библиотеки с тестами | не шаблон |

### 6.3. Что обычно содержит «шаблон» и чего не хватает по меркам экосистемы

| Обычно есть | Обычно **нет** (и есть у нас в Go/SPA-шаблонах) |
|---|---|
| каркас проекта (Gradle/Xcode), 1–4 модуля, sample-экран | **agent-обвязка**: `CLAUDE.md`/`AGENTS.md` ядро + path-scoped rules + скиллы (`/smartdev`, `/plan`, `/scaffold`, `/de-template`, `/release`) — ни в одном шаблоне |
| DI + ViewModel + Room/Retrofit | **planning-first** (единый roadmap), forward-only доки |
| detekt/ktlint или SwiftLint (иногда) | **единый гейт одной командой** с coverage-полом, скриншот-verify, архитектурными тестами (Konsist/archlint), негативными фикстурами |
| CI на GitHub Actions | гейт, **не зависящий от CI-раннеров** (у нас их нет по решению) и от GitHub |
| unit-тесты «для галочки» | **тестовая политика по слоям** («что обязано иметь тест»), фейки в `*-test`-модулях |
| README | **docs-first**: `docs/stack/*` (8 kinds, `Generates`), `docs/reference/architecture.md`, мост в `<org>/strategy` |
| — | **mock/demo-режим как требование стора** (App Review 2.1) и как reference-фича [05 §8.4] |
| — | env-контракт сред (dev/staging/prod), **reference-фичи** (auth/profile/todos/theme), i18n ru/en, a11y-минимум, токен-слой без внешней DS |
| — | процедура **`/de-template`** (превращение шаблона в проект) и **`/release`** без раннеров (RuStore API, TestFlight с Mac) |
| — | **STOP-контракт** артефакта [05 §7.4] |

**Вывод §6.** Стартовые шаблоны индустрии останавливаются на «проект собирается и есть один
экран»; всё, что делает репозиторий пригодным для AI-driven процесса (инварианты, гейт,
скиллы, docs-first, de-template), сегодня существует только в продовых репо (Element X,
DuckDuckGo, Telegram) и в наших собственных шаблонах. Стартовой точкой для кода стоит брать
**`Kotlin/KMP-App-Template-Native` + `android create empty-activity-agp-9` + Xcode «SwiftUI
App»**, а всё остальное — переносить из `go-backend-template`/`spa-frontend-template`.

## 7. Выводы для шаблона

### 7.1. Тренд · статус · решение

Легенда решений: **зашить** — часть базы шаблона; **опция** — есть шов/документированный путь,
включается осознанно; **избегать** — запрет правилом; **наблюдать** — не трогать до триггера.

| Тренд / практика | Статус 2026-09-17 | Решение | Почему |
|---|---|---|---|
| Jetpack Compose как единственный UI (Android) | stable; «our standard», Views maintenance mode [П] | **зашить** | вендор + все живые OSS; XML в шаблоне не появляется |
| SwiftUI + `@Observable` + Swift 6 language mode + approachable concurrency | дефолт новых проектов [02] | **зашить** | канон Apple; Liquid Glass обязателен с 27 SDK |
| Navigation 3 (Android) / `NavigationStack` + типизированный `[Route]` (iOS) | stable; берут все лагеря [01 §4.3] | **зашить** | типизированная навигация = тестируемые deep links + агентная симметрия |
| UDF + immutable `UiState` + «events не из ViewModel в UI» | Strongly recommended [П] | **зашить** (инвариант) | 10+ лет; фундамент тестируемости |
| Repository/SSOT, модели по слоям | Strongly recommended [П] | **зашить** | контракт с Go-бэкендом живёт здесь |
| Конструкторная DI (Hilt для Android-only; Koin/Metro при KMP — решает [08]) | recommended [П] | **зашить принцип**, реализацию — по [08] | фейки и границы модулей |
| Модульность по фичам + `build-logic` convention-плагины + version catalog; SwiftPM-пакеты | у всех живых OSS [01 §4.5] | **зашить** | параллельная работа агентов; границы «по построению» |
| Архитектурные тесты (Gradle-граф + detekt `ForbiddenImport`/`ForbiddenMethodCall` + custom lint; Swift `archlint`) | практика Element X, NiA, JET [01, 02] | **зашить** | аналог Steiger/depguard; Konsist — с оговоркой о стагнации [01 §8] |
| Фейки вместо моков, `*-test`-модули | Strongly recommended; у всех OSS | **зашить** | Google Testing Blog 2024; DORA «safety nets» |
| JVM-тесты + Compose на Robolectric + скриншоты (Roborazzi/official) + Preview-snapshots | норма у Signal/NiA/Element/DoorDash | **зашить** | «глаза» агента без устройства |
| Swift Testing | дефолт [02] | **зашить** | XCTest только UI/perf |
| Coverage-пол (Kover / xccov) | как в Go-шаблоне (75 %) | **зашить** | единый стиль экосистемы |
| Пороги качества как гейт релиза (crash < 1.09 %, ANR < 0.47 %, crash-free ≥ 99.9 %, Metric Goals) | нормативны (Play 2027, Xcode 27) | **зашить** в `/release` (чек, не блокер локальной сборки) | сторы штрафуют видимостью; отката нет |
| R8 full mode, DEX-оптимизация ≥ 25 %, 16 KB, 64-bit, актуальный `targetSdk` | требования Play [П, 05] | **зашить** в гейт | иначе релиз невозможен |
| Adaptive/large-screen baseline (window size classes, `NavigationSplitView`) | Android 17 — обязательная resizability [П]; Apple — Liquid Glass «adaptable» | **зашить** минимум | иначе переписывать |
| Edge-to-edge, predictive back, per-app language, notification permission, photo picker | обязательны/дефолт [01 §3] | **зашить** | платформенные инварианты |
| Credential Manager / passkeys / Zero-Tap restore; Sign in with Apple при соц-логине | Play с 04.2027 [П]; App Review 4.8 [05] | **зашить** в reference-фичу `auth` | требование сторов; контракт auth — по [07 TL;DR п. 1–2]: Bearer + refresh в JSON-теле (cookie-only бэкенд ломает кодоген), `security-crypto` deprecated → своя `SecureStore` (Keystore + DataStore), Keychain `…ThisDeviceOnly` |
| Собственный токен-слой поверх M3 / SwiftUI (без внешней DS) | как в SPA-шаблоне | **зашить** | переживает M3 Expressive/Liquid Glass |
| i18n ru/en (String Catalogs, `LocaleConfig`), a11y-лейблы и проверки | инвариант | **зашить** | законы + ревью + агентная локализация |
| Mock/demo-режим (сценарии как в MSW) | требование App Review 2.1 [05 §8.4]; дизайн — [07 TL;DR п. 9]: JSON-фикстуры per сценарий (`empty\|populated\|authenticated\|error`) + `MockTransport` (Ktor `MockEngine` / swift-openapi `ClientTransport`), `demo`-flavor/схема, те же фейки в JVM/Swift-тестах | **зашить** (reference-фича №1) | одновременно инструмент тестов и стора |
| Observability: Sentry-совместимый self-host (GlitchTip), `release`/`environment`, mapping/dSYM до публикации | [05 §4]; [07 TL;DR п. 13]: `sentry-android` 8.57.0 / `sentry-cocoa` 9.28.0, пустой DSN = выключено; Kermit / `os.Logger` с запретом `Log`/`print`; OTel Android 1.7.0 / Swift 2.5.1 — шов | **зашить шов** `TelemetrySink`, sentry — дефолт | без SaaS; OTel — опция (коллектора в экосистеме нет) |
| Feature flags | практика 10+ лет; `flagkit` в golibinfra | **зашить шов** (провайдер — опция), правило «флаг — инвентарь, срок жизни в коммите» | kill-switch без релиза; [07 TL;DR п. 10]: флаги и min-version приходят в `GET /api/app/requirements` на `flagkit`, внешние платформы не закладывать (Unleash server — AGPL-3.0) |
| Offline-кеш stale-while-revalidate (DataStore + память); Room/GRDB | профиль offline-tolerant | **зашить лёгкий кеш**, БД — **опция** | не тащить ORM без нужды |
| Agent-обвязка: `CLAUDE.md` ≤ 200 строк + `AGENTS.md`-указатель, path-scoped rules, скиллы, `settings.json` deny | наш стандарт; у продовых OSS — есть, у шаблонов — нет | **зашить** | ниша шаблона |
| Вендорские скиллы (`android skills add`, Apple-скиллы на Mac) + `android docs` | stable [П] | **зашить** в `make setup` (`--no-metrics`) | доки свежее памяти модели |
| Kotlin LSP, `kdoc.jar`, klibs MCP, Compose Hot Reload MCP | Alpha/stable [П, 03] | **опция** (dev-тулинг), проверка [06] | ускоряют цикл, не критичны |
| Xcode MCP / XcodeBuildMCP / Maestro MCP | macOS-only / эмулятор | **опция** для Mac-стадии и e2e | вне быстрого гейта |
| KMP shared logic (кандидат B) | Stable; RU-практика «ядро, не UI» | **база по [03]**; при выборе A — путь описан | решение владельца [03 §9.2] |
| Compose Multiplatform как общий UI | Stable, но с нативной оболочкой; RU-отказы | **опция** (`sharedUi/`, явно) | look&feel, найм (5 вакансий) |
| BDUI / DivKit | RU-стандарт у Avito/Яндекс/Ozon | **опция-шов** (виджетный рендер по контракту), **избегать как базу** | цена: кривая обучения iOS, сложность; для B2B-компаньона не окупается |
| On-device AI (Foundation Models, Gemini Nano, ML Kit GenAI), App Intents, AppFunctions | GA/preview | **опция** (шов), не зависимость | вне профиля CRM; preview-статусы |
| Android XR / Wear / Auto / TV | Beta/stable | **вне профиля** | — |
| Kotlin Toolchain (ex-Amper), Swift export → Beta, Swift SDK for Android 1.0 | Alpha / Alpha / official без UI | **наблюдать** (триггеры R-1…R-3 в [03]) | — |
| React Native / Flutter | развороты; запрет владельца | **избегать** | [03 §2.F] |
| Rust-ядро, `gomobile` | работает у крипто-профиля / experimental | **избегать** для профиля | [03 §2.E] |
| TCA как база, MVI-фреймворки как база, VIPER/MVP | ниша/устарели | **избегать** | [01, 02] |
| RxJava/RxSwift, Combine в новом коде, kapt, `LiveData`, `AndroidViewModel`, `GlobalScope`, Accompanist, XML/Fragments, строковые роуты | вытеснены | **избегать** (detekt/SwiftLint-правила) | [01 §12, 02 §2.6] |
| SwiftData, Core Data по умолчанию | боли миграций | **избегать** как дефолт | [02 §6.13] |
| Firebase (Crashlytics/Analytics/App Distribution), GA/GTM | деградация в РФ, ПДн, privacy manifests, lock-in | **избегать** | [05 §4.4] |
| Ручные правки `.pbxproj` агентом; подпись/ключи агентом | ловушка №1 iOS | **избегать** (deny) | §5.2 |
| CocoaPods, Tuist Cloud, GitHub Actions как обязательный CI | миграции на SPM; платное облако; нет раннеров | **избегать** | §4.3, [02 §1] |
| Долгоживущие фичефлаги, `--no-verify` | процессные запреты | **избегать** | Fowler; экосистема |

### 7.2. Фундаментальные инварианты для `CLAUDE.md` / `.claude/rules/` (проект списка)

Формулировки — в стиле существующих шаблонов (кратко, проверяемо машиной там, где возможно):

1. **UDF**: состояние течёт вниз (`UiState`), события — вверх; ViewModel/модель экрана не
   шлёт one-off события в UI; состояние неизменяемо (`data class`/`struct`).
2. **SSOT**: данные приходят только через репозиторий; UI не ходит в сеть/БД; DTO ↔ доменные
   модели ↔ UI-модели — разные типы.
3. **Границы модулей — машинные**: `feature:a` не зависит от `feature:b`; `core/domain` без
   Android/UIKit-импортов; проверяется Gradle-графом + detekt/lint/Konsist (Android) и
   `archlint` (iOS); негативные фикстуры обязаны краснеть.
4. **Логика вне UI и вне фреймворка**: всё, что можно, тестируется на JVM (`jvmTest`) или
   pure-Swift (`swift test`) на Linux; UI-слой — «взять состояние + отрисовать».
5. **Типизированная навигация**: роуты — `@Serializable`/`Hashable` типы; deep link — чистая
   функция `URL → [Route]`; строковых роутов нет.
6. **DI — конструкторная**; никаких сервис-локаторов/глобальных синглтонов; фейки — в
   `*-test`-модулях; `Fake*`/`Default*`-нейминг.
7. **Асинхронность**: coroutines + Flow / async-await + AsyncSequence; Rx, Combine, `GlobalScope`,
   `Task.detached` без обоснования — запрещены правилом.
8. **Тестовая политика по слоям**: ViewModel/модель экрана, репозиторий, маппер, use-case —
   обязаны иметь тест; экран — Compose-тест на Robolectric/Preview-snapshot; coverage-пол.
9. **Снапшоты**: эталоны пишутся явной командой (`make snapshot-record`) на фиксированной
   конфигурации; агент не перезаписывает эталон «по ходу».
10. **Гейт не изображает зелёный**: `make check` печатает, какие стадии пропущены (нет Mac,
    нет эмулятора); pre-push — Linux-полная часть.
11. **Пороги качества — часть релиза**: crash/ANR/crash-free/размер/старт проверяются
    скриптом `/release` до промоушена; символы заливаются до публикации.
12. **Секреты и подпись — только человек**: keystore/`.p12`/`.p8`/profiles не в репо;
    токены — Keychain/Keystore, не `UserDefaults`/`SharedPreferences` (`androidx.security:security-crypto`
    deprecated — своя `SecureStore` на Keystore + DataStore [07]); `.pbxproj` правит только
    Xcode/генератор.
13. **Внешние SDK — за швами** (`TelemetrySink`, `PushProvider`, `AuthProvider`, `MapProvider`,
    `FlagProvider`); Firebase не подключается; каждая новая зависимость — privacy manifest +
    запись в `docs/stack/dependencies.md`.
14. **Токен-слой**: цвета/размеры/типографика только через семантические токены проекта; сырые
    значения в компонентах запрещены; тема — system/light/dark.
15. **Adaptive + a11y + i18n с первого экрана**: window size classes / `NavigationSplitView`;
    content descriptions/VoiceOver-лейблы; строки только через каталоги, плюралы ru/en
    тестируются.
16. **Mock/demo-режим — reference-фича**, сценарии общие для тестов и demo-сборки; App
    Review получает demo-режим, а не «напишите нам».
17. **Свежие доки вместо памяти**: перед платформенным кодом — `android docs`/Apple Document
    Search/`kdoc.jar`; версии стека и «что изменилось в 2026» зафиксированы в `CLAUDE.md`
    (AGP 9 без `kotlin-android`, Nav3, Swift 6 MainActor-by-default, Liquid Glass).
18. **Planning-first и чекпойнты**: содержательное изменение — секция в roadmap; план с хешем;
    чекпойнт = тест + визуальная проверка + ревью.
19. **Forward-only**: `AGENTS.md`/`CLAUDE.md`/docs описывают текущее состояние; drift проверяется
    `/smartdev`; changelog — в git.
20. **Комментарии — только WHY**; сгенерированный код не правится руками (`gen-check`).

### 7.3. Что из этого может устареть за год (и что с этим делать)

| Что | Вероятность за 12 мес | Признак | Действие |
|---|---|---|---|
| Версии стека (Kotlin 2.4.x → 2.5/2.6; AGP 9.5+; Compose 1.13; Xcode 27.x; Swift 6.5; iOS 27 SDK → 28) | **высокая** (кварталы) | релизные заметки | пины в `libs.versions.toml`/`.xcode-version`; обновление через `/plan`; Renovate-подобный скилл |
| Дедлайны Play (targetSdk 37 к 08.2027; память/DEX 02.2027; Zero-Tap 04.2027) и Apple (iOS 27 SDK с 04.2027) | **определённо** | support.google.com / developer.apple.com/news | таблица дедлайнов в `docs/stack/distribution.md`, проверка в гейте |
| Swift export Alpha → Beta; Kotlin Toolchain Alpha → Beta; SPM import | средняя | roadmap Kotlin (02.2027) | правило «плоский фасад» переживёт; Toolchain — не база |
| Agent-инструменты (Android CLI команды, Xcode MCP-инструменты, XcodeBuildMCP под Sentry, Maestro MCP) | **высокая** | release notes | скиллы ссылаются на `android skills`/официальные, а не копируют их |
| Формат/имя файлов агентной обвязки (`AGENTS.md` vs `CLAUDE.md` vs ACP-плагины) | средняя | ACP у JetBrains + Apple | один источник, второй — указатель |
| Kotlin LSP/`kdoc.jar`/klibs MCP — стабильность | средняя | Alpha → Beta | опция, не зависимость гейта |
| Konsist (нет релизов с 12.2024) | средняя (может «умереть» или ожить) | Maven Central | правила дублируются detekt/lint; Konsist — второй слой |
| Periphery (коммерциализация), SwiftLint/swift-format | средняя | GitHub | решение владельца [02] |
| Владелец RuStore, статус developer verification для РФ, Apple/ФАС | **высокая** | новости | [05]; шаблон не завязан на владельца стора |
| Тезис Shopify «агенты удешевили дважды» — подтвердится/опровергнется первым годом поддержки | средняя | их посты 2027 | триггеры R-8/R-9 в [03] |
| Пороги Play/Xcode Metric Goals | средняя (ужесточаются) | объявления за 6+ мес | пороги — конфиг скрипта `/release`, не код |
| Compose Multiplatform под Liquid Glass / Nav3-KMP stable | средняя | CMP 1.13+ | опция `sharedUi/` не в базе |
| Foundation Models provider-agnostic / Gemini Nano 4 / AppFunctions | высокая (preview) | GA-анонсы | швы, не зависимости |

**Вывод §7.** База шаблона — 20 инвариантов, все технологически нейтральные и совпадающие с
Go/SPA-шаблонами по духу; технологические выборы (Compose/SwiftUI/Nav3/Swift Testing/
скриншоты/Android CLI) — там, где вендор и OSS-практика совпадают. Всё российское
(BDUI/DivKit, импортозамещённые SDK, Аврора) и всё AI-продуктовое (on-device модели,
AppFunctions) — швы и опции. «Избегать» — короткий список запретов, каждый из которых
подтверждён разворотом индустрии или требованием стора.

## 8. Риски и открытые вопросы владельцу

### 8.1. Риски (уровень индустрии; технические — в [01]/[02]/[03]/[05])

| ID | Риск | Вероятность | Влияние | Митигация |
|---|---|---|---|---|
| RI-1 | **Асимметрия агентного тулинга по хосту** (Android — Linux-полный, iOS — только Mac) закрепится: Apple вкладывается в Xcode-агентов, а не в Linux-тулчейн | высокая | среднее | архитектура «логика вне UI + тонкий SwiftUI + удалённый Mac» [03 §6.1, 05 §6.5]; pure-Swift на Linux |
| RI-2 | **Быстрый дрейф вендорских агентных инструментов** (CLI-команды, MCP-инструменты, скиллы) делает наши скиллы устаревшими за квартал | высокая | среднее | вендорить минимум, ссылаться на официальные скиллы; drift-проверка `/smartdev`; forward-only |
| RI-3 | **Галлюцинации версий** у моделей с cutoff ≤ 06.2026 против стека 09.2026 (AGP 9, Nav3, Swift 6.4, Liquid Glass) | высокая | высокое | «что изменилось» в `CLAUDE.md`; `android docs`/Apple Document Search/`kdoc.jar`; пины версий; негативные фикстуры |
| RI-4 | **Снижение стабильности при росте throughput** (DORA 2025) — агентные PR ломают то, что не покрыто | высокая | высокое | coverage-пол, скриншоты, архитектурные тесты, пороги crash/ANR в релизе; adversarial-ревью (Shopify) |
| RI-5 | **Нормативные пороги Play 2027** (память, DEX ≥ 25 %, Zero-Tap) и Apple SDK-дедлайны сделают форк без обновлений непубликуемым через 6–12 мес | определённо | высокое | дедлайны в `docs/stack`, проверка в гейте, скилл обновления |
| RI-6 | **RU-специфика меняется быстрее кода**: владелец RuStore сменился (07.2026), developer verification, платежи Apple — шаблон, завязанный на конкретного контрагента, устареет | высокая | среднее | швы (push/auth/analytics/store), [05] |
| RI-7 | **BDUI-ожидания заказчика** («как у Авито — менять экраны без релиза») против нашей базы | средняя | среднее | опция-шов с DivKit/собственным виджетным рендером; честная документация цены |
| RI-8 | **Ожидание «как у Shopify»** — владелец захочет два нативных без `shared` | средняя | среднее | [03] показал устойчивость B; путь A задокументирован |
| RI-9 | **Стагнация ключевых OSS-инструментов** (Konsist без релизов ~2 года; Periphery коммерциализирован; architecture-templates Google стагнирует) | средняя | низкое | дублирование правил detekt/lint; свой `archlint`; не брать стагнирующие сэмплы за канон |
| RI-10 | **Публичные RU-первоисточники редеют** (последние архитектурные разборы у многих — 2019–2023) → «как делают в РФ» придётся уточнять докладами/вакансиями | средняя | низкое | периодический пересмотр §3.2 по Mobius/AppsConf |
| RI-11 | **Опросы — вендорские** (JetBrains про KMP, Google про Kotlin) и самоотобранные; SO 2026 не вышел | — | низкое | использовать как направление, не как доказательство |

### 8.2. Открытые вопросы владельцу (дополняют списки [02 §Риски], [03 §9.2], [05 §11.1])

1. **Тезис Shopify vs Philo** — принять ли «агенты удешевили писать дважды» как рабочую
   гипотезу экосистемы? От этого зависит вес критерия «доля общего кода» и, по [03], форма
   шаблона (B устойчив, но A становится ближе). Индустрия в 09.2026 разделена.
2. **BDUI как опция** — нужен ли в шаблоне шов под server-driven UI (DivKit или собственный
   виджетный рендер по контракту `data`-envelope)? Для B2B-компаньона это редкость; для
   заказчиков уровня маркетплейса — ожидание.
3. **Госсектор и «Аврора»** — будут ли заказчики, требующие сборку под российские мобильные
   ОС? Если да — это отдельная платформа (Qt/C++), вне шаблона; решить заранее, чтобы не
   обещать.
4. **On-device AI в профиле** — закладывать ли швы под Foundation Models / Gemini Nano /
   App Intents / AppFunctions (preview-статусы)? Рекомендация — шов без зависимости; но если
   заказчики ждут «AI-фичи», reference-фича может понадобиться.
5. **Имя и форма agent-файлов** — `CLAUDE.md` + `AGENTS.md`-указатель (как Element X) или
   наоборот; поддержка ACP-плагинов (JetBrains/Xcode) в скиллах — да/нет.
6. **Вендорские скиллы** — вендорить `android/skills` в репо (детерминизм, офлайн) или ставить
   в `make setup` (свежесть)? Плюс политика телеметрии (`--no-metrics`).
7. **Пороги в гейте релиза** — принять ли Play-пороги (1.09 %/0.47 %) и предложенные
   crash-free ≥ 99.9 % как условие STOP-2 [05 §7.4], и кто их измеряет (Sentry self-host /
   Play Console / App Store Connect API)?
8. **Платные инструменты** — политика «шаблон не требует платной IDE» (IDEA Ultimate,
   Tuist Cloud, Periphery Pro): подтвердить как инвариант open-core.
9. **Лимиты расходов на агентов** (Uber — $1 500/разработчик/мес) — нужна ли аналогичная
   норма в процессе экосистемы (в `/plan` — оценка бюджета)?
10. **Mac** — покупка Mac mini как условие iOS-ветки ([05 §6.5]); без него половина
    инструментов из §5.1 недоступна, и iOS-часть шаблона остаётся «по документации».
11. **Валидация на RU-сообществе** — стоит ли после первого проекта вынести опыт (гейт,
    агентная обвязка) докладом на Mobius/AppsConf — как способ получить обратную связь от
    компаний, которые не публикуют текстов (§3.2 п. 5)? Это не инженерный, а стратегический
    вопрос.
12. **hh.ru-снимок** — цифры найма получены потоком 03 одной точкой; повторный снимок этим
    потоком заблокирован анти-ботом. Считать ли их достаточными или снять руками из браузера
    при принятии решения?

### 8.3. Что осталось непроверенным в этом потоке

- Мобильный раздел JetBrains DevEco 2025 (домен `devecosystem-2025.jetbrains.com` заблокирован
  для WebFetch) — доли Flutter/RN/KMP среди мобильных разработчиков оттуда не сняты.
- StatCounter напрямую (три попытки — обрыв соединения); доли РФ — по iphones.ru (08.2026) и
  anti-malware.ru/Statbase.
- Stack Overflow 2026 — не опубликован на 2026-09-17.
- Airbnb 2018, Wise 2025, Etsy — Medium/сайты отдают 403 боту; содержание по вторичным.
- Reddit, Pinterest, Samokat, Кинопоиск, Kaspersky (архитектура), Сбер (свежее) — публичных
  первоисточников 2025–2026 не найдено.
- «Альфа-Банк и Т-Банк внедряют KMP + Compose» — только статья аутсорсера (SmartexSoft);
  Mobius-доклады Т-Банка про KMP подтверждают KMP, но не «Compose Multiplatform».
- Android 17 stable-дата (2026-06-16) — по Wikipedia/AndroidAuthority; страница release notes
  Google в момент снятия описывала Beta 4.1 как последнюю (вероятно, кэш/структура страницы).
- Все заявления вендоров об ускорении («×3, −70 % токенов», «15–20 % быстрее циклы») —
  без независимой методики.
- Эмпирика инструментов §5.1 на Linux — закрыта потоком 06 для Android-гейта, `android` CLI,
  KMP-klib и Swift 6.4/Swift OpenAPI Generator; Kotlin LSP, Compose Hot Reload MCP, klibs MCP и
  Maestro с эмулятором в TL;DR 06 не упомянуты — статус уточнять там или отдельным spike.

## 9. Сводная таблица версий и дат

Версии библиотек/тулчейнов — в [01 §Сводная], [02 §Сводная], [03 §10]; здесь — события,
релизы и источники данных, на которые опирается этот отчёт.

| Что | Версия / значение | Дата | Лицензия / тип | URL |
|---|---|---|---|---|
| Google I/O 2025 — «16 things» | — | 2025-05-20 | блог | https://android-developers.googleblog.com/2025/05/16-things-to-know-for-android-developers-google-io-2025.html |
| Android CLI + Skills (preview) | — | 2026-04-16 | Apache-2.0 (skills) | https://android-developers.googleblog.com/2026/04/build-android-apps-3x-faster-using-any-agent.html |
| Google I/O 2026 — «17 things» | — | 2026-05-19 | блог | https://android-developers.googleblog.com/2026/05/17-things-android-developers-google-io.html |
| Android Studio Quail (I/O Edition) | Canary | 2026-05-19 | проприетарная, бесплатная | https://android-developers.googleblog.com/2026/05/whats-new-android-developer-tools.html |
| Android CLI stable | 1.0 | 2026-06-09 | — | https://android-developers.googleblog.com/2026/06/android-developer-productivity-updates.html |
| Android 17 stable | API 37 | 2026-06-16 [П~] | — | https://en.wikipedia.org/wiki/Android_17 ; https://developer.android.com/about/versions/17/release-notes |
| Android 17 QPR1 stable | API 37.1 | 2026-09-15 [П~] | — | gsmarena / droidfeats |
| Jetpack Compose | 1.12 / BOM 2026.08.00; min AGP 9.1.1; compileSdk 37 | 2026-08-12 | Apache-2.0 | https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html |
| Play technical quality requirements | crash 1.09 % / ANR 0.47 %; память, DEX ≥ 25 % (02.2027); Zero-Tap (04.2027) | 2026-08-26 | политика | https://support.google.com/googleplay/android-developer/answer/17492799 |
| Android architecture recommendations | — | обновлено 2026-04-26 | док | https://developer.android.com/topic/architecture/recommendations |
| WWDC25 press release | Liquid Glass, Xcode 26, Foundation Models, Swift 6.2 | 2025-06-09 | — | https://www.apple.com/newsroom/2025/06/apple-supercharges-its-tools-and-technologies-for-developers/ |
| Xcode 26 beta 7 (GPT-5, Claude) | 26 beta 7 | 2025-08-28 [П~] | — | 9to5mac / MacRumors |
| Xcode 26.3 agentic coding | RC / release | 2026-02-03 / 2026-02-26 [П~] | — | https://www.apple.com/newsroom/2026/02/xcode-26-point-3-unlocks-the-power-of-agentic-coding/ ; https://www.anthropic.com/news/apple-xcode-claude-agent-sdk |
| WWDC26 press release | Xcode 27 agentic, Foundation Models providers, Core AI | 2026-06-08 | — | https://www.apple.com/newsroom/2026/06/apple-aids-app-development-with-new-intelligence-frameworks-and-advanced-tools/ |
| WWDC26 «What's new in Xcode 27» / «Xcode, agents, and you» | сессии 258 / 259 | 06.2026 | — | https://developer.apple.com/videos/play/wwdc2026/258/ ; https://developer.apple.com/videos/play/wwdc2026/259/ |
| iOS 27.0 / Xcode 27 / Swift 6.4 | GA | 2026-09-14 / 09-14 / 09-15 | — | [02 §1] |
| KotlinConf 2025 | Копенгаген | 2025-05-21…23 | — | https://blog.jetbrains.com/kotlin/2025/05/kotlinconf-2025-language-features-ai-powered-development-and-kotlin-multiplatform/ |
| KotlinConf'26 keynote highlights | Мюнхен | 2026-05-20…22 | — | https://blog.jetbrains.com/kotlin/2026/05/kotlinconf26-keynote-highlights/ |
| Kotlin 2.4.0 / 2.4.20 | — | 05–06.2026 / 2026-09-07 | Apache-2.0 | [03 §1] |
| Kotlin productivity study | 320k devs, 28M циклов | 05.2026 | — | https://blog.jetbrains.com/kotlin/2026/05/built-for-productivity-what-the-data-shows-about-kotlin/ |
| KMP: DevEco 7 % → 18 % | — | 2024 → 2025 | — | https://kotlinlang.org/docs/multiplatform/kotlin-multiplatform-react-native.html |
| KMP Survey 2025 (70 % satisfied, 80 % CMP) | — | стр. обновлена 2026-08-05 | — | https://kotlinlang.org/docs/multiplatform/kmp-for-ios.html |
| Stack Overflow Developer Survey | 2025 (49 100+); 2026 — не опубликован | 07.2025 [П~] / — | — | https://survey.stackoverflow.co/2025/ |
| JetBrains DevEco 2025 | 24 534 респондента | 10.2025 | — | https://blog.jetbrains.com/research/2025/10/state-of-developer-ecosystem-2025/ |
| DORA 2025 | ~5 000 респондентов | 2025-09-24 | — | https://cloud.google.com/blog/products/ai-machine-learning/announcing-the-2025-dora-report ; https://dora.dev/research/2025/dora-report/ |
| Sensor Tower State of Mobile 2026 | $167 млрд IAP, ~150 млрд загрузок | 01.2026 | — | https://sensortower.com/blog/state-of-mobile-2026 |
| StatCounter RU mobile (через iphones.ru) | iOS ~34 % / Android ~66 % трафика | 06.2026 (статья 2026-08-14) | — | https://www.iphones.ru/iNotes/skolko-lyudey-sidit-s-iphone-v-rossii-ssha-i-pochemu-afrika-glavnyy-skrytyy-rynok-dlya-apple |
| Российские мобильные ОС (обзор) | — | 2026-07-13 | — | https://www.anti-malware.ru/analytics/Market_Analysis/Russian-mobile-operating-systems-2026 |
| Хабр Карьера, зарплаты I п/г 2026 | mobile 295k; iOS 326k; Android 320k | 2026-08-28 | — | https://habr.com/ru/companies/habr_career/articles/1075936/ |
| hh.ru вакансии | см. §2.3 | 2026-09-17 | — | [03 §4] |
| Uber uSpec | — | 2026-03-11 | — | https://www.uber.com/us/en/blog/automate-design-specs/ |
| Uber Software Factory | >70 % PR — агенты; 3 600 скиллов; 1 000+ MCP | 2026-08-27 | — | https://www.uber.com/gb/en/blog/efficient-software-factory/ |
| Shopify «back to native» | — | 2026-09-10 | — | [03 §3.4] |
| Proton Mail new apps | Rust ≈ 80 %, crash iOS 0.05 % | 2026-02-02 | — | https://proton.me/blog/next-generation-proton-mail-mobile-apps |
| Instagram Compose (Meta) | — | 2025-01-24 | — | https://engineering.fb.com/2025/01/24/android/bringing-jetpack-compose-to-instagram-for-android/ |
| Slack Duplo | — | 2022 | — | https://slack.engineering/stabilize-modularize-modernize-scaling-slacks-mobile-codebases/ |
| Avito Beduin v2 | — | 2026-02-16 | — | https://habr.com/ru/companies/avito/articles/997010/ |
| Яндекс Еда BDUI (DivKit) | — | 2025-05-14 | — | https://dev.go.yandex/blog/bdui-in-eda-2025-05-14 |
| Яндекс Про: Flutter, CocoaPods → SPM | — | 2026-07-13 | — | https://habr.com/ru/companies/yandex/articles/1054308/ |
| DivKit | OSS SDUI | с 08.2022 | Apache-2.0 | https://github.com/divkit/divkit |
| Ozon BDUI на клиенте | — | 2022 | — | https://habr.com/ru/companies/ozontech/articles/661941/ |
| Wildberries iOS SVIP | — | 2024-10 | — | https://habr.com/ru/companies/wildberries/articles/798275/ |
| Т-Банк: разработка без бэкенда (моки) | — | 2026-09-16 | — | https://habr.com/ru/companies/tbank/articles/1082854/ |
| Совкомбанк: мобильное тестирование 2026 | — | 2026-07-03 | — | https://habr.com/ru/companies/sovcombank_technologies/articles/1053818/ |
| 2ГИС Flutter SDK на C++ ядре | — | 2025-05-21 | — | https://habr.com/ru/companies/2gis/articles/898804/ |
| CMP-регрет (Онтико) | — | 2025-04-10 | — | https://habr.com/ru/companies/oleg-bunin/articles/899096/ |
| Обзор Mobius 2025 (JUG Ru) | Москва | 2025-04-09…10 | — | https://habr.com/ru/companies/jugru/articles/892376/ |
| Mobius 2026 Spring / Autumn | Москва + онлайн | 2026-05-12…13 / 2026-10-21 | — | https://mobiusconf.com/ |
| VK продала RuStore | 100 % → менеджмент | 2026-07-16 | — | https://habr.com/ru/news/1059712/ ; https://vk.company/ru/press/releases/12359/ |
| Dodo Engineering (Swift + Kotlin) | 380+ инженеров | срез 2026-09-17 | — | https://dodoengineering.ru/ |
| Fowler/Hodgson Feature Toggles | — | 2017-10-09 | — | https://martinfowler.com/articles/feature-toggles.html |
| Google Testing Blog: fakes vs mocks | — | 2024-02-27 | — | https://testing.googleblog.com/2024/02/increase-test-fidelity-by-avoiding-mocks.html |
| `getsentry/XcodeBuildMCP` | 6 396★ | push 2026-08-02 | MIT [П~] | https://github.com/getsentry/XcodeBuildMCP |
| `mobile-dev-inc/maestro` (+MCP) | 15 648★; MCP с 02.2026 | push 2026-09-15 | Apache-2.0 [П~] | https://github.com/mobile-dev-inc/maestro ; https://docs.maestro.dev/get-started/maestro-mcp |
| `android/nowinandroid` | 21.8k★, `AGENTS.md` | push 2026-09-02 | Apache-2.0 | https://github.com/android/nowinandroid |
| `android/architecture-templates` | 3 162★ | push **2024-12-12** | Apache-2.0 | https://github.com/android/architecture-templates |
| `cortinico/kotlin-android-template` | 1 911★ | push 2026-09-07 | MIT [П~] | https://github.com/cortinico/kotlin-android-template |
| `skydoves/pokedex-compose` | 1 132★ | push 2026-04-10 | Apache-2.0 [П~] | https://github.com/skydoves/pokedex-compose |
| `Kotlin/KMP-App-Template` / `-Native` | 695★ / — | push 2026-09-11 / активен | Apache-2.0 | https://github.com/Kotlin/KMP-App-Template ; https://github.com/Kotlin/KMP-App-Template-Native |
| `touchlab/KaMPKit` | 2 453★ | push 2026-09-01 | Apache-2.0 [П~] | https://github.com/touchlab/KaMPKit |
| `JetBrains/compose-multiplatform-template` | 638★, **архив** | push 2023-12-19 | Apache-2.0 | https://github.com/JetBrains/compose-multiplatform-template |
| `tuist/tuist` | 5 804★, `AGENTS.md` 27 КБ | push 2026-09-16 | MIT | https://github.com/tuist/tuist |
| `pointfreeco/isowords` | 3 007★ | push **2024-08-16** | MIT | https://github.com/pointfreeco/isowords |
| `uber/RIBs` | 7 940★ | push 2026-07-15 | Apache-2.0 | https://github.com/uber/RIBs |
| `slackhq/circuit` / `cashapp/redwood` | 1 852★ / 2 022★ | активны | Apache-2.0 | https://github.com/slackhq/circuit ; https://github.com/cashapp/redwood |
| `TelegramMessenger/Telegram-iOS` (`CLAUDE.md` 24 КБ) / `DrKLO/Telegram` | 8 970★ / 29 881★ | push 2026-07-17 / 2026-09-16 | GPL-2.0 [П~] | https://github.com/TelegramMessenger/Telegram-iOS ; https://github.com/DrKLO/Telegram |
| `element-hq/element-x-android` / `-ios` (`AGENTS.md`) | — | активны | AGPL-3.0 [П~] | https://github.com/element-hq/element-x-android |
| `duckduckgo/android` (`CLAUDE.md`), `mozilla-mobile/firefox-ios`, `wireapp/wire-ios`, `nextcloud/*`, `wordpress-mobile/*` | agent-файлы есть | активны | разные | см. §5.3 |

## 10. Источники

**Google / Android**
- https://android-developers.googleblog.com/2025/05/16-things-to-know-for-android-developers-google-io-2025.html
- https://android-developers.googleblog.com/2026/04/build-android-apps-3x-faster-using-any-agent.html
- https://android-developers.googleblog.com/2026/05/17-things-android-developers-google-io.html
- https://android-developers.googleblog.com/2026/05/whats-new-android-developer-tools.html
- https://android-developers.googleblog.com/2026/06/android-developer-productivity-updates.html
- https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html
- https://developer.android.com/about/versions/17/release-notes
- https://developer.android.com/topic/architecture/recommendations (обновлено 2026-04-26)
- https://developer.android.com/tools/agents (Android CLI, skills, Knowledge Base)
- https://support.google.com/googleplay/android-developer/answer/17492799 (technical quality requirements, 2026-08-26)
- https://en.wikipedia.org/wiki/Android_17 ; https://www.androidauthority.com/android-17-stable-rollout-features-3675016/
- https://github.com/android/nowinandroid ; https://github.com/android/architecture-templates ; https://github.com/android/skills
- https://cloud.google.com/blog/products/ai-machine-learning/announcing-the-2025-dora-report ; https://dora.dev/research/2025/dora-report/
- https://testing.googleblog.com/2024/02/increase-test-fidelity-by-avoiding-mocks.html

**Apple**
- https://www.apple.com/newsroom/2025/06/apple-supercharges-its-tools-and-technologies-for-developers/
- https://www.apple.com/newsroom/2026/02/xcode-26-point-3-unlocks-the-power-of-agentic-coding/
- https://www.anthropic.com/news/apple-xcode-claude-agent-sdk
- https://www.apple.com/newsroom/2026/06/apple-aids-app-development-with-new-intelligence-frameworks-and-advanced-tools/
- https://developer.apple.com/videos/play/wwdc2026/258/ (What's new in Xcode 27)
- https://developer.apple.com/videos/play/wwdc2026/259/ (Xcode, agents, and you)
- https://developer.apple.com/videos/play/wwdc2026/339/ (Bring an LLM provider to the Foundation Models framework)
- https://developer.apple.com/wwdc26/guides/apple-intelligence/
- https://9to5mac.com/2025/08/28/new-xcode-beta-now-available-with-gpt-5-and-claude-support/ ; https://9to5mac.com/2026/02/26/apple-releases-xcode-26-3-with-support-for-agentic-coding/ (вторичные)

**JetBrains / Kotlin**
- https://blog.jetbrains.com/kotlin/2025/05/kotlinconf-2025-language-features-ai-powered-development-and-kotlin-multiplatform/
- https://blog.jetbrains.com/kotlin/2026/05/kotlinconf26-keynote-highlights/
- https://blog.jetbrains.com/kotlin/2026/05/built-for-productivity-what-the-data-shows-about-kotlin/
- https://blog.jetbrains.com/research/2025/10/state-of-developer-ecosystem-2025/
- https://blog.jetbrains.com/research/2026/05/developer-ecosystem-survey-2026-take-part-in-one-of-the-largest-developer-studies/
- https://kotlinlang.org/docs/multiplatform/kotlin-multiplatform-react-native.html (7 % → 18 %)
- https://kotlinlang.org/docs/multiplatform/kmp-for-ios.html (KMP Survey 2025)
- https://kotlinlang.org/docs/ai-for-development.html ; https://kotlinlang.org/benchmark/ (см. [03])
- https://github.com/Kotlin/KMP-App-Template ; https://github.com/Kotlin/KMP-App-Template-Native ; https://github.com/JetBrains/compose-multiplatform-template ; https://kmp.jetbrains.com/
- https://github.com/touchlab/KaMPKit

**Опросы и рынок**
- https://survey.stackoverflow.co/2025/technology ; https://survey.stackoverflow.co/2025/ai ; https://survey.stackoverflow.co/ (2026 не опубликован)
- https://sensortower.com/blog/state-of-mobile-2026 ; https://sensortower.com/report/state-of-mobile-2026
- https://www.iphones.ru/iNotes/skolko-lyudey-sidit-s-iphone-v-rossii-ssha-i-pochemu-afrika-glavnyy-skrytyy-rynok-dlya-apple (StatCounter RU, 06.2026)
- https://gs.statcounter.com/os-market-share/mobile/russian-federation (недоступен боту)
- https://www.anti-malware.ru/analytics/Market_Analysis/Russian-mobile-operating-systems-2026
- https://habr.com/ru/companies/habr_career/articles/1075936/ ; https://career.habr.com/salaries?qualification=All&spec_aliases%5B%5D=mobileapp_developer
- https://hh.ru/search/vacancy?text=<…>&area=113 (снимок [03 §4])

**Компании — мир**
- https://shopify.engineering/back-to-native ; https://shopify.engineering/shop-app-migration (см. [03])
- https://www.uber.com/us/en/blog/automate-design-specs/ ; https://www.uber.com/gb/en/blog/efficient-software-factory/ ; https://www.infoq.com/news/2026/07/efficient-ai-infrastructure/ ; https://github.com/uber/RIBs
- https://netflixtechblog.com/netflix-android-and-ios-studio-apps-now-powered-by-kotlin-multiplatform-d6d4d8d25d23
- https://engineering.atspotify.com/tag/mobile
- https://code.cash.app/native-ui-and-multiplatform-compose-with-redwood ; https://code.cash.app/molecule-1-0 ; https://dpe.org/sessions/jake-wharton/releasing-faster-with-kotlin-multiplatform/ ; https://github.com/cashapp
- https://slack.engineering/stabilize-modularize-modernize-scaling-slacks-mobile-codebases/ ; https://github.com/slackhq/circuit
- https://2025.kotlinconf.com/talks/812400/ (Duolingo + KMP)
- https://careersatdoordash.com/blog/category/engineering/mobile/ ; https://www.infoq.com/news/2026/03/doordash-dasherglobal-onboarding/
- https://medium.com/wise-engineering/wise-tech-stack-2025-update-d0e63fe718c7 (403 боту)
- https://proton.me/blog/next-generation-proton-mail-mobile-apps
- https://1password.com/blog/typeshare-for-rust
- https://medium.com/mcdonalds-technical-blog/mobile-multiplatform-development-at-mcdonalds-3b72c8d44ebc
- https://medium.com/airbnb-engineering/whats-next-for-mobile-at-airbnb-5e71618576ab (403 боту)
- https://engineering.fb.com/2025/01/24/android/bringing-jetpack-compose-to-instagram-for-android/
- https://www.etsy.com/codeascraft/adopting-jetpack-compose-for-etsy39s-android-app (403 боту)
- https://www.scottberrevoets.com/2021/10/14/ios-architecture-at-lyft/
- https://github.com/TelegramMessenger/Telegram-iOS ; https://github.com/DrKLO/Telegram ; https://telegram-internals.hidandelion.com/
- https://github.com/element-hq/element-x-android ; https://github.com/element-hq/element-x-ios ; https://github.com/bitwarden/android ; https://github.com/bitwarden/ios ; https://github.com/signalapp/Signal-Android ; https://github.com/mozilla-mobile/firefox-ios ; https://github.com/wireapp/wire-ios ; https://github.com/duckduckgo/android ; https://github.com/nextcloud/android ; https://github.com/wordpress-mobile/WordPress-iOS ; https://github.com/tuist/tuist ; https://github.com/cashapp/paparazzi (проверка agent-файлов через raw.githubusercontent.com)

**Компании — Россия**
- https://habr.com/ru/companies/avito/articles/997010/ (Beduin v2) ; https://avito.tech/content/fc2r8mjfv1-beduin-v2-backend-driven-ui-v-avito
- https://dev.go.yandex/blog/bdui-in-eda-2025-05-14 ; https://github.com/divkit/divkit ; https://divkit.tech/en/
- https://habr.com/ru/companies/yandex/articles/1054308/ (Яндекс Про, Flutter → SPM) ; https://habr.com/ru/companies/yandex/articles/1000608/ (Яндекс 360 DS)
- https://habr.com/ru/companies/ozontech/articles/661941/ ; https://habr.com/ru/companies/ozontech/articles/756682/ ; https://habr.com/ru/companies/ozontech/articles/566882/
- https://habr.com/ru/companies/wildberries/articles/798275/
- https://en.wikipedia.org/wiki/Max_(app) ; https://github.com/VKCOM/VKUI ; https://habr.com/ru/news/1059712/ ; https://vk.company/ru/press/releases/12359/
- https://habr.com/ru/companies/tbank/articles/1082854/ ; https://habr.com/ru/companies/jugru/articles/892376/ (Mobius 2025)
- https://habr.com/ru/companies/alfa/articles/844606/ ; https://habr.com/ru/companies/alfa/articles/708518/
- https://habr.com/ru/articles/1036528/ (SmartexSoft, «рейтинг для банков» — вторичный)
- https://job.mtsbank.ru/it
- https://habr.com/ru/companies/kaspersky/articles/864200/ ; https://habr.com/ru/company/kaspersky/blog/467617/ (Kaspresso)
- https://habr.com/ru/companies/2gis/articles/898804/
- https://habr.com/ru/companies/lamoda/articles/746140/
- https://dodoengineering.ru/
- https://habr.com/ru/companies/hh/articles/461341/ (2019)
- https://habr.com/ru/companies/sovcombank_technologies/articles/1053818/
- https://habr.com/ru/companies/oleg-bunin/articles/899096/ (CMP-регрет)
- https://habr.com/ru/companies/cleverpumpkin/articles/1012648/ (см. [03]) ; https://habr.com/ru/companies/cleverpumpkin/articles/945268/ ; https://habr.com/ru/companies/kts/articles/959950/
- https://habr.com/ru/articles/1002444/ ; https://habr.com/ru/articles/1060650/ (дайджесты «Мобильная разработка за неделю» #620, #639)
- https://mobiusconf.com/ ; https://mobiusconf.com/archive/2026%20Spring/speakers/
- https://www.securitylab.ru/news/531168.php ; https://mosdigitals.ru/blog/ispolzovanie-github-gitlab-v-usloviyah-sankczij-v-rossii (GitHub и санкции — вторичные)
- https://vc.ru/services/1675564-jetbrains-v-rossii-kak-skachat-ustanovit-i-oplatit-produkty-v-2026-godu (JetBrains и оплата из РФ — вторичный)
- https://kokoc.com/blog/yandeks-appmetrica/ (вторичный)

**AI-агенты и инструменты**
- https://github.com/getsentry/XcodeBuildMCP ; https://github.com/getsentry/XcodeBuildMCP/releases
- https://maestro.dev/blog/maestro-mcp-for-ai-coding-agents ; https://docs.maestro.dev/get-started/maestro-mcp ; https://github.com/luxury-labs/maestro-mcp
- https://dev.to/anicca_301094325e/how-to-automate-ios-development-without-breaking-pbxproj-files-2mpk ; https://blakecrosley.com/guides/ios-agent-development ; https://byteiota.com/xcode-26-3-ai-agents-wont-fix-its-rotten-foundation/ (вторичные)
- https://vmobify.com/blog/production-android-workflows-claude ; https://claudelab.net/en/articles/claude-code/claude-code-android-kotlin-production-guide ; https://codex.danielvaughan.com/2026/04/26/codex-cli-kotlin-android-teams-android-cli-skills-jetpack-compose/ (вторичные)
- https://github.com/Drjacky/claude-android-ninja ; https://github.com/rcosteira79/android-skills ; https://www.avanderlee.com/ai-development/swift-testing-agent-skill-write-high-quality-tests-with-ai/
- https://dev.to/arshtechpro/wwdc-2026-xcode-27-ships-with-apples-own-agent-skills-what-they-are-and-how-to-use-them-3g2 ; https://byteiota.com/xcode-27-agentic-coding-mcp-guide/ (вторичные)

**Фундамент**
- https://martinfowler.com/articles/feature-toggles.html
- https://developer.apple.com/documentation/swiftui/managing-model-data-in-your-app (см. [02])

**Внутренние**
- `/home/mm/git/stit-core/mobile-template/docs/research/brief.md`, `01-android-canon.md`, `02-ios-canon.md`, `03-shared-strategy.md`, `05-distribution-ru.md`, `06-tooling-agent.md` (TL;DR — эмпирика Linux-гейта, KMP-klib, Swift 6.4), `07-backend-contract.md` (TL;DR — auth Bearer, mock-режим, Sentry/OTel, flagkit, мост `mobile/<surface>/`)
- `/home/mm/git/stit-core/go-backend-template/CLAUDE.md`, `/home/mm/git/stit-core/spa-frontend-template/CLAUDE.md`
- Черновики и сырые результаты проверок: scratchpad `04/` (`notes-vendors.md`, `notes-companies.md`, `agents-files*.txt`, `gh-html.txt`, `agentfile-*`)
