# 08 · Форма шаблона: сколько репо, дерево, обвязка, гейт, STOP, мост в strategy

Поток 08-template-design · 2026-09-17 · вход: отчёты 01–07, `brief.md`, `_synthesis-notes.md`, `prompts/PROMPTS.md`, прецеденты экосистемы (go-backend / spa-frontend / telegram-bot шаблоны, corp-infra, business-strategy, stit-public), SSR-прецедент дизайна (`spa-frontend-template/docs/research/ssr/round2/13-template-v2.md`, `00-synthesis.md` §9–§12).

**Как читать ссылки.** `01 §R7` — отчёт `01-android-canon.md`, рекомендация R7; `05 §7.4` — отчёт 05, секция 7.4; `06 L-13` — ловушка L-13 из 06; `07 B-12` — пункт BACKEND-HANDOFF из 07; `05 O-1` — открытый вопрос O-1 из 05; `SSR §9` — `ssr/00-synthesis.md` §9; `SSR-13 §N` — `ssr/round2/13-template-v2.md`. Прецеденты: `go:` — go-backend-template, `spa:` — spa-frontend-template, `bot:` — telegram-bot-template, `corp:` — corp-infra-template, `strategy:` — business-strategy-template, `infra:` — project-infra-template-compose.

Отчёт описывает **целевое состояние** шаблона (forward-only). Содержимое 01–07 не пересказывается — здесь только решения и форма; где потоки расходятся, противоречие названо и разрешено с обоснованием. Всё, что нужно проверить «руками» до реализации, помечено `[spike]` и собрано в §9.1.

---

## TL;DR

1. **Шаблонов — один: `mobile-template` → форк `<org>/mobile`.** Kotlin Multiplatform-ядро `shared/` (модели, API-клиент, auth-сессия, репозитории, презентеры экранов, deep-link-роутер, mock-транспорт) + нативный UI: `androidApp/` (Jetpack Compose) и `iosApp/` (SwiftUI). Это кандидат B из 03 §5 (86.0 против 75.0 у двух нативных) — принят. Две публичные строки roadmap (`android-template`, `ios-template`) сворачиваются в одну. Android-only / iOS-only форки — не отдельные шаблоны, а пути `/de-template --platform android|ios`. Продуктовое деление (companion / consumer / enterprise) — поле `preset:` в `docs/stack/service.md`, а не репозиторий. Kit-библиотеки (`androidkit`/`swiftkit`, вариант «4») — не сейчас: kit-образный код живёт внутри шаблона (`shared/…/core`, `iosApp/Packages/Core`), извлечение — по триггеру «второй форк + фикс, который должен доехать в оба» (прецедент golibinfra). Подробно — §1.
2. **Дерево** — раскладка JetBrains `shared/ + androidApp/ + iosApp/` (03 §6.3); внутри `androidApp/` — NiA-модульность 01 §R3 (`app`, `core/*`, `feature/*`, `lint`); внутри `iosApp/` — SwiftPM-пакеты 02 §7.1, но без пакетов `Networking`/`Domain` (их роль выполняет `Shared.framework`). Reference-фичи: `demo` (mock-режим, фича №1), `shell`, `requirements`, `auth`, `profile`, `todos`, `settings`, `push`. Полное дерево, матрица зависимостей и prune-лист — §2.
3. **DI и сеть** — Hilt только в `androidApp/`; в `shared/` DI-фреймворка нет — ручной composition root `SharedContainer` (единая точка входа и для Hilt-модуля, и для Swift). Сеть — Ktor 3.6 в `shared/`, модели генерируются из `api/openapi.yaml` (генератор `kotlin` multiplatform, models-only, 07 §2.5). Retrofit и Swift OpenAPI Generator в шаблон не входят. Обоснование — §2.4, §2.5.
4. **Обвязка** — `CLAUDE.md` ≤ 200 строк (листинг §3.1) с блоком «Где это в экосистеме»; **23** path-scoped правила — 21 мобильное + 2 общих экосистемных (§3.2); `settings.json` (§3.3); **9** скиллов в форме Go/SPA (§3.4) — `/smartdev`, `/plan`, `/triage-failure`, `/init-service`, `/stack-deep-dive`, `/de-template`, `/scaffold`, `/release`, `/infra-handoff`; `AGENTS.md` — указатель на `CLAUDE.md`; официальные Android-скиллы ставятся `make setup`, не вендорятся.
5. **Гейт** — один `make check` = Linux-полный (Kotlin-форматер → detekt → Android Lint → unit JVM/Robolectric → Roborazzi → Kover → assembleDebug → `shared` klib для iOS-таргетов с проверкой каталога → swift-format/SwiftLint → `swift build`/`swift test` чистых пакетов → gen-check / mock / i18n / deep-links / tokens) и завершается явным блоком «macOS-стадии не выполнены на этом хосте»; `make check-mac` — по SSH на Mac; `make check-full` = оба (обязателен для `/release`); `make check-fast`; `make gate-selftest` на негативных фикстурах (06 §8.6 + Swift/KMP). Один автофиксер на язык (Spotless/ktlint; swift-format); конфликты 06 L-01/L-03 закрыты конфигом. Подробно — §4.
6. **STOP-контракт** — двухступенчатый 05 §7.4, принят как есть и поглощает 01/03: STOP-1 = Gitea Release на теге `vX.Y.Z` с подписанными AAB+APK / IPA+dSYM.zip + `mapping.txt` + `checksums.txt` + release-notes ru/en + символы загружены (мобильный аналог image-in-registry); STOP-2 = Play internal / RuStore moderation / TestFlight uploaded; дальше — STORE-HANDOFF. `dist/` из 03 — локальная стадия перед загрузкой; Baseline Profile из 01 — внутри AAB. Подробно — §7.
7. **Мост в strategy** — слой `docs/projects/<slug>/mobile/<surface>/` (9 kinds по 07 §9.3) **замещает** `screens/` для `type: mobile` — по образцу `telegram-bot-flow/` для `type: bot`; поля 05 §9.4 вложены в `permissions.md`/`shell.md` и в stack-`distribution.md`. Канон-PR в strategy (S-1…S-3) + путь без PR (`docs/reference/strategy-mobile-canon/` в шаблоне). Подробно — §6.
8. **Stack-kinds** — 13 обязательных (`service runtime api auth deep-links push observability styling distribution signing compliance dependencies config`) + 2 условных (`analytics`, `payments`); волны и cross-kind-инварианты — §5.2.
9. **Cross-repo** — упорядоченный список §8: corp-канон (7-й шаблон, контракт «artifact-in-release», Vaultwarden-коллекция `mobile/<org>/<app>`, ntfy-топик `releases-<org>`, лимит вложений Gitea), backend B-1…B-3 / B-12 / B-17 (P0), infra `wellknown` (I-1), strategy (S-1…S-3), stit-public (две строки → одна).
10. **План** — 8 фаз с гейтами (§9): старт от `KMP-App-Template-Native` + `android create` + Xcode «SwiftUI App» + спайк 06 (`spikes/android-gate`); фаза iOS упирается в Mac (05 O-2) и Apple-аккаунт (05 O-1) — до их появления `shared/` + `androidApp/` доводятся до релиза RuStore/Play.
11. **Вопросов владельцу** — 44 (§10), блокирующих 5; сам шаблон блокирует только один — «сколько шаблонов».

Где разрешены названные в задании противоречия: (а) один KMP-репо vs два нативных — §1.4; (б) граница STOP — §7.1; (в) наборы kinds — §5.2; (г) пять списков правил — §3.2; (д) `linkDebugFramework…` SKIPPED / dependency-analysis / форматеры — §4.3, §4.6; (е) auth Bearer + refresh и reference-фича `auth` — §2.5; (ж) DI для KMP — §2.4.

---

## Содержание

- §1 Сколько шаблонов
- §2 Форма репозитория
- §3 Обвязка (CLAUDE.md, rules, settings, skills)
- §4 Гейт
- §5 `docs/` и родство с Go/SPA/bot
- §6 Слой-мост в strategy (`type: mobile`)
- §7 STOP-контракт и релиз
- §8 Cross-repo
- §9 План реализации
- §10 Вопросы владельцу
- §11 Риски
- Источники

---

## §1 Сколько шаблонов

### §1.1 Варианты × измерения

Кандидаты формы (03 §6.3 + постановка владельца): **В1** — два нативных репо (`android-template` + `ios-template`, как в текущем roadmap); **В2** — один KMP-репо `mobile-template` (общее Kotlin-ядро + нативный UI); **В3** — три репо (`mobile-shared` как библиотека + два app-репо); **В4** — четыре (два нативных + kit-библиотеки `androidkit`/`swiftkit`, по образцу golibinfra). Пресеты (companion / consumer / enterprise) — ортогональны форме и рассматриваются в §1.3.

| Измерение | В1 · два нативных | В2 · один KMP-репо | В3 · три (lib + 2 app) | В4 · четыре (2 + 2 kit) |
|---|---|---|---|---|
| **Обвязка и дрейф** | Две копии `CLAUDE.md`/rules/skills/`docs/stack`; ~70 % текста общий (strategy-мост, STOP, compliance, release, envs) → дрейф гарантирован при владельце-соло | Одна обвязка, две платформенные секции; платформенные правила path-scoped (`androidApp/**`, `iosApp/**`) | Три обвязки + версионирование библиотеки | Четыре обвязки; kit-обвязка минимальна, но релизный цикл — есть |
| **Гейт и Linux-выполнимость** | Android — Linux-полный (06 §3); iOS — на Linux только чистые Swift-пакеты и линтеры (02 §7.11), **вся логика приложения в Swift слепа без Mac** | Логика в `shared/` (45–60 %) + Android (20–25 %) проверяются на Linux — 65–80 % работы (03 §6.1); слепой только SwiftUI-слой | Как В2, плюс публикация `shared` между репо — XCFramework собирается только на Mac | Как В1 |
| **STOP-контракт** | Два независимых тега/версии → рассинхрон `versionCode`/`CFBundleVersion`, два Release | Один тег `vX.Y.Z` → один Gitea Release с обоими артефактами; `--platform` для частичных | Три релизных цикла (semver библиотеки + два приложения) | Два цикла приложений + два semver kit'ов |
| **Мост в strategy** | Одна карта `mobile/<surface>/` читается двумя репо; колонка `shared` в `features.md` бессмысленна; логика по одной карте пишется дважды | Одна карта → один репо; `shared: true\|false` реально управляет местом кода | Карта читается тремя репо | Как В1 |
| **Форк-путь потребителя** | Android-only / iOS-only — один форк; both — два форка + ручная синхронизация контрактов | both — один форк; Android-only / iOS-only — `/de-template --platform` удаляет половину | both = три форка | both = два форка + две зависимости |
| **Найм и передача заказчику** | «Чистые» Kotlin- и Swift-команды, ноль KMP-навыка | Kotlin-команда + Swift-разработчик, потребляющий `Shared.framework` (стандартный паттерн JetBrains-шаблона); iOS-разработчику нужен Gradle для `embedAndSign…` — трение, снимается прекомпилированным XCFramework при росте | Как В2 + библиотечная дисциплина | Как В1 + зависимость от kit'ов владельца |
| **Публичное обещание roadmap** | Совпадает буквально | Одна строка вместо двух; обещание «нативный Android / нативный iOS» выполняется (UI нативный), меняется формулировка | Новые строки | Новые строки |
| **Стоимость поддержки владельцем-соло** | 2 × (toolchain-апдейты + обвязка + reference-фичи + compliance-тексты) | 1 × обвязка + 2 toolchain'а (неизбежны при любом варианте) + reference-логика **один раз** | + релизы библиотеки при нуле внешних потребителей | + два kit-релиза |
| **Откатываемость** | В1→В2: невозможно без переписывания (общего ядра нет) | В2→В1: `git subtree split` по `androidApp/`+`shared/` и `iosApp/`; `shared/` растворяется в платформы (03 §7.4 fallback A) | В3→В2: слить репо (дёшево) | В4→В1: инлайн kit'ов |
| **Слепая зона владельца без Mac** | Весь iOS-шаблон (логика + UI) | Только SwiftUI-адаптеры (20–30 % iOS-половины) | Как В2 | Как В1 |
| **Матрица 03 §5** | A — 75.0 | B — 86.0 | не считалась; штраф за библиотечный цикл без потребителей | не считалась; kit'ы без потребителей преждевременны |

### §1.2 Рекомендация: один репо `mobile-template`, форк `<org>/mobile`

Принято В2. Решающие аргументы:

1. **Главный constraint экосистемы — агент на Linux.** Только общее Kotlin-ядро даёт агенту проверяемую работу по iOS-половине без Mac (03 §6.1: 65–80 % против ~20 % у нативного Swift). Нативный `ios-template` был бы шаблоном, который владелец не может ни собрать, ни протестировать у себя.
2. **Один STOP, один тег, одна карта strategy.** Мобильный клиент — одна сущность продукта; два репо расщепляют версию, Release, deep-link-таблицу и `features.md`.
3. **Владелец-соло.** Одна обвязка вместо двух — единственный способ удержать 23 правила, 15 kinds и 9 скиллов без дрейфа.
4. **Откат дешёв в одну сторону.** В2 → два нативных репо — `subtree split` + растворение `shared/`; обратный путь — переписывание. Поэтому стартовать нужно с В2.

Что В2 **не** обещает: общий UI (кандидат C, Compose Multiplatform на iOS) в шаблон v1 не входит — опциональный `sharedUi/` открывается по триггеру 03 §7.2 (стабильный CMP-iOS без замечаний App Review, Liquid Glass-паритет), не по умолчанию. Триггеры пересмотра формы — 03 §7.2 R-1…R-9; отдельно для формы репо: если Kotlin/Native-компиляция Apple-таргетов на Linux перестанет работать (`compileKotlinIosArm64` ломается в двух подряд Kotlin-релизах) — fallback A (03 §7.4), то есть В1, через `subtree split`.

Имя форка — `<org>/mobile`; sibling-checkout экосистемы становится `~/git/<org>/{strategy,infra,backend,frontend,<bot>,mobile}`.

### §1.3 Продуктовое деление → пресеты, не репозитории

У мобильных есть деление, аналогичное «приложение vs сайт» у web (SSR §9), но оно проходит **по дистрибуции и обязательствам**, а не по runtime:

| Пресет (`service.md#preset`) | Для кого | Что меняет | Что не меняет |
|---|---|---|---|
| `companion` (по умолчанию) | B2B/B2E-клиент к своему бэкенду: вход по логину, без монетизации, аналитика `none`, каналы RuStore + Play (free) + App Store (исключения 3.1.3(c)/4.8 — 05 §1.3) | ничего сверх базы; `demo`-режим обязателен (App Review) | — |
| `consumer` | B2C: публичная регистрация, монетизация, аналитика | kinds `analytics` (обязателен) и `payments` (если монетизация) переходят в обязательные; чек-лист compliance расширяется (возрастной рейтинг, SiwA при любом стороннем логине — 4.8, ATT если трекинг); `/release` требует store-listing-блок; env-дефолты `ANALYTICS`, `SOCIAL_LOGIN` | runtime, гейт, STOP-1, слой strategy, дерево |
| `enterprise` | Распространение **вне магазинов**: Gitea Release APK + Apple Business Manager custom apps / MDM | `/release` каналы `gitea-release` + `abm` вместо store-каналов; App Review-блоки чек-листа выключены; `demo`-режим необязателен; секция enterprise в `distribution.md`; для Android — Play developer verification 2027 для sideload (05 §1.2) | то же |

**Критерий отдельного репозитория** (адаптация SSR §9 к мобильным): другой toolchain/runtime; другой состав `make check` (более одной стадии); другой тип артефакта STOP-1; другой слой strategy. Ни один из трёх пресетов критерий не пересекает — значит, пресет. Что пересекает и **вне** шаблона: server-driven UI/BDUI-рантайм (другая архитектура — 04 §7.1 «опция/избегать»; в шаблоне только шов — см. §10), ОС «Аврора» (другой toolchain — 05 §1), десктоп (roadmap «десктоп» — другой шаблон), Compose-Multiplatform-UI (кандидат C — триггер, не пресет).

Пресет — одно поле; его читают `/init-service` (дефолты env), `/stack-deep-dive` (обязательность условных kinds), `/de-template` (prune-лист и чек-лист compliance), `/release` (набор каналов и блоков STORE-HANDOFF). Смена пресета после `/de-template` — обычная правка `service.md` + прогон `/stack-deep-dive` по условным kinds.

### §1.4 Разрешение противоречия (а): 03 против канонов 01/02

01 и 02 писали каноны для раздельных `android-template`/`ios-template`; 03 рекомендует один KMP-репо. Решение: каноны 01/02 применяются **к своим половинам** В2 с точечными заменами, а не «переписыванием половины R1» (01 §1 о решении 03):

| Канон | Применяется как есть | Заменяется решением 03/07 | Почему |
|---|---|---|---|
| 01 (Android) | версии §1 (AGP 9.4 / Kotlin 2.4 / Gradle 9.7 / Compose BOM 2026.09 / Navigation 3 / Hilt 2.60 / KSP), convention-плагины §R3, детект/Spotless/Lint/Kover §R4–R5, Konsist §R6, гейт §R7, Robolectric + Compose test v2 + Roborazzi §7, `resourcePrefix`, `:lint`, ловушки | `core:model/network/data/domain/common` → пакеты `shared/`; Retrofit → Ktor (в `shared/`); Room → в шаблоне нет БД (см. §2.4), persistence-actual только для SecureStore/настроек; Hilt остаётся в `androidApp/`, в `shared/` — нет | сетевой слой и домен должны компилироваться в iOS-таргет; Hilt — JVM-only (03 §4) |
| 02 (iOS) | Xcode 27 / Swift 6.4 / iOS 18 target, Swift 6 mode + approachable concurrency, SwiftUI + `@Observable`, `Router` с типизированным путём, SwiftPM local packages + один `.xcodeproj`, `xcconfig` per env, test plans, snapshot-testing, SwiftLint + swift-format, `Tools/archlint`, PrivacyInfo/Keychain/AASA-ловушки | пакеты `Networking` и `Domain` не создаются — их роль у `Shared.framework`; `Persistence` сокращается до Keychain-реализации `SecureStore`; парсинг deep-link (`URL → [Route]`) — в `shared/` (Kotlin), Swift-`Router` потребляет `Route` из фасада; «модель экрана в отдельном чистом Swift-таргете» (02 вопрос 5) — снимается: логика состояний экрана живёт в `shared/…/<X>Presenter` (Kotlin, JVM-тесты на Linux), Swift-модель — тонкий `@Observable`-адаптер | всё, что импортирует `Shared`, компилируется только на Mac — чистых Swift-таргетов с бизнес-логикой в В2 не бывает; дублировать роутинг в двух языках — источник расхождений |
| 03 (форма) | раскладка `shared/ + androidApp/ + iosApp/`, direct integration, flat-facade-правило, expect/actual без cinterop, `apiDump` | «AAB + IPA в `dist/` + handoff» как STOP — уточняется до 05 §7.4 (§7.1) | 05 глубже проработал канал |

Итог: 01 остаётся каноном `androidApp/` и `build-logic/`, 02 — каноном `iosApp/`, 03 — каноном `shared/` и швов, 07 — каноном контракта с бэкендом, 05 — каноном дистрибуции, 06 — каноном гейта.

### §1.5 Публичные формулировки

**`stit-public/docs/roadmap.md`** — две строки заменяются одной:

```
| mobile-template | мобильное приложение: нативный Android (Compose) + нативный iOS (SwiftUI) на общем Kotlin-ядре (KMP) | в планах |
```

**`stit-public/docs/templates.md`** — две строки заменяются одной:

```
| mobile-template | Android + iOS, KMP-ядро, нативный UI; форк-пути Android-only / iOS-only |
```

**`stit-public/README.md`** — в перечислении планов «нативные Android / iOS» → «мобильные приложения (Android + iOS, KMP-ядро, нативный UI)»; счётчик «5 форкаемых шаблонов» приводится к каноническому числу corp-канона одним sweep'ом с SSR (см. §8).

**`corp-infra-template/docs/ecosystem.md`** — правки (точные места — §8.1): семейство «6 форкаемых шаблонов» → «7» (после SSR — «8»); строка Identity-таблицы `| mobile-template | <org>/mobile | Android + iOS на KMP-ядре; STOP = артефакт в Gitea Release |`; в «Канонические контракты» — «Artifact-in-release (mobile)», «Vaultwarden-коллекция `mobile/<org>/<app>`», «ntfy-топик `releases-<org>`», требование к Gitea `[attachment]` (размер/число ассетов релиза); в Skill index — `mobile-template` в строках `/init-service, /stack-deep-dive, /de-template, /scaffold` и `/release, /infra-handoff`; sibling-checkout — `mobile`; в диаграмму — `mobile` рядом с `frontend`/`<bot>` как клиент `backend` (Bearer), без маршрута Traefik; happy path «новый мобильный клиент».

### §1.6 Вариант «4 = 2 шаблона + 2 kit-библиотеки» — почему не сейчас

Что вошло бы в kit'ы: API-клиент с auth-интерцептором, `SecureStore`, `RequirementsClient`/feature-flags, `PushRegistrar`, `TelemetrySink`/логгер, `MockTransport`, deep-link-роутер, DTCG-токены. В В2 всё это, кроме платформенных actual'ов, — **Kotlin в `shared/`**, то есть kit «в одном экземпляре» уже существует внутри шаблона; Swift-kit сжимается до Keychain-`SecureStore`, APNs-регистратора и инициализации Sentry. Извлечение сейчас означало бы: Maven-публикацию KMP-библиотеки (Gitea Packages поддерживает Maven — но iOS-потребление артефакта требует XCFramework, собираемого на Mac, либо source-зависимости через `includeBuild`), semver и релизный цикл при **нуле** внешних потребителей, и второй SwiftPM-репо. Прецедент golibinfra извлекали из уже работающих сервисов, не до них.

Триггер извлечения (фиксируется в `docs/reference/ecosystem.md` шаблона): ≥ 2 форков в эксплуатации **и** первый фикс в `shared/…/core` или `Packages/Core`, который нужно доставить в оба → `mobilekit` (KMP, Gitea Maven + git-tag; iOS-потребление через `includeBuild`/XCFramework в Gitea Release) и `swiftkit` (SwiftPM по git-тегу). До триггера граница kit'а поддерживается структурно: пакет `shared/src/commonMain/kotlin/<pkg>/core/**` и `iosApp/Packages/Core` не импортируют `feature`/`entity`-код (проверяется Konsist и `archlint`, §2.6) — чтобы извлечение было механическим.

---
## §2 Форма репозитория

### §2.1 Полное дерево

`<pkg>` — Kotlin-пакет шаблона `tech.stit.mobile`; `/de-template` переименовывает его в `<reverse-domain>.<app>` вместе с `applicationId` и `PRODUCT_BUNDLE_IDENTIFIER`.

```
mobile-template/                                   # форк → <org>/mobile
├── CLAUDE.md                                      # ядро + роутер, ≤ 200 строк (§3.1)
├── AGENTS.md                                      # указатель на CLAUDE.md (§3.5)
├── README.md · LICENSE · SECURITY.md
├── Makefile                                       # словарь вербов (§4.1); группы ##@ как в go:
├── .editorconfig                                  # ktlint-ключи (06 §2.9) + swift: 2 пробела (02 §7.5)
├── .gitignore                                     # build/, .gradle/, *.hprof, local.properties, Local.xcconfig,
│                                                  #   .env.local, DerivedData/, *.xcresult, .kotlin/, iosApp/.build/
├── .xcode-version                                 # 27.0 — читают doctor.sh / mac.sh
├── .swift-version                                 # 6.4
├── gradle.properties                              # 06 §8.2: jvmargs -Xmx2g -XX:MaxMetaspaceSize=1g -XX:HeapDumpPath=build/,
│                                                  #   caching, configuration-cache, parallel, kotlin.daemon.jvmargs
├── settings.gradle.kts                            # pluginManagement { includeBuild("build-logic") }, foojay-resolver 1.0.0,
│                                                  #   TYPESAFE_PROJECT_ACCESSORS, include(":shared", ":androidApp:app", …)
├── build.gradle.kts                               # только plugins { alias(libs.plugins.*) apply false }
├── gradle/
│   ├── libs.versions.toml                         # единый каталог (01 §R2 + 03 §6.3 + 07 §2.3/§7; итог — §2.9)
│   └── wrapper/gradle-wrapper.properties          # 9.7.1 + distributionSha256Sum
├── build-logic/                                   # convention-плагины stit.* (§2.3)
│   ├── settings.gradle.kts
│   └── convention/{build.gradle.kts, src/main/kotlin/…}
├── config/
│   ├── detekt/detekt.yml                          # 01 §R4 + compose-rules 0.4.28; detekt-formatting не подключён (06 L-01)
│   ├── lint/lint.xml                              # severity overrides Android Lint; baseline не используется (06 L-13)
│   ├── env/{dev,staging,prod,demo}.properties     # build-time env Android (07 §4.2) → BuildConfig через stit.android.application
│   └── openapi/kotlin-models.yaml                 # конфиг генератора моделей (07 §2.5): models-only, kotlinx-serialization
├── api/
│   ├── openapi.yaml                               # копия ../backend/api/openapi.yaml (07 §2.1, вариант B)
│   └── openapi.lock                               # sha256 копии · версия генератора · sha256 сгенерированного
├── mock/
│   ├── scenarios.yaml                             # empty | populated | authenticated | error (+ проектные)
│   └── fixtures/<scenario>/<entity>.json          # одни и те же для commonTest, demo, Roborazzi/snapshot, Maestro
├── shared/                                        # KMP-модуль :shared (§2.2)
│   ├── build.gradle.kts                           # stit.kmp.library: androidLibrary{withHostTest}, jvm(), iosArm64(), iosSimulatorArm64();
│   │                                              #   framework "Shared" (static), SKIE, BCV klib
│   ├── api/{shared.api, shared.klib.api}          # apiDump — коммитятся, apiCheck в гейте (§4.3)
│   └── src/{commonMain,commonTest,androidMain,androidHostTest,iosMain,jvmMain,jvmTest}/kotlin/<pkg>/…
├── androidApp/                                    # Gradle-проекты :androidApp:* (§2.3)
│   ├── app/                                       # Application, MainActivity, NavDisplay, Hilt-граф, flavors env=dev|staging|prod|demo
│   ├── core/{designsystem,ui,platform,navigation,testing}/
│   ├── feature/{shell,auth,profile,todos,settings}/
│   └── lint/                                      # :androidApp:lint — DesignSystemDetector и проектные детекторы (01 §4.1)
├── iosApp/                                        # Xcode-проект (§2.3); собирается только на Mac, кроме Packages/Pure
│   ├── iosApp.xcodeproj                           # имя постоянно; product name / bundle id / team — в xcconfig
│   ├── App/{iOSApp.swift, AppEnvironment.swift, RootView.swift, Resources/, Info.plist,
│   │        PrivacyInfo.xcprivacy, App.entitlements}
│   ├── Config/{Base,Dev,Staging,Prod,Demo}.xcconfig · Local.xcconfig.example
│   ├── Packages/
│   │   ├── Pure/                                  # Linux-собираемые таргеты: AppFoundation, DesignSystemTokens (+ тесты)
│   │   ├── Core/                                  # DesignSystem, Routing, Persistence, Platform, SharedBridge (импортируют SwiftUI/Shared)
│   │   ├── Features/                              # Shell, AuthFeature, ProfileFeature, TodosFeature, SettingsFeature; product "Features"
│   │   └── TestSupport/
│   ├── TestPlans/{Unit,Snapshot,UI}.xctestplan
│   ├── UITests/                                   # XCUITest smoke (test plan UI; вне gate)
│   ├── Tools/archlint/                            # Swift CLI над `swift package dump-package` (02 §7.7)
│   ├── scripts/embed-shared.sh                    # build phase: ./gradlew :shared:embedAndSignAppleFrameworkForXcode
│   └── .swiftlint.yml · .swift-format · .test-simulator
├── e2e/{common,android,ios}/*.yaml                # Maestro-флоу (устройство/эмулятор; вне gate)
├── scripts/
│   ├── tools/{check.sh, gradle-step.sh, swift-step.sh, doctor.sh, mac.sh, status.sh, gate-selftest.sh,
│   │          gen-check.sh, mock-check.sh, i18n-check.sh, deeplinks-check.sh, tokens-check.sh,
│   │          version-from-tag.sh, coverage-floor.sh, android-skills.sh}
│   ├── release/{preflight.sh, build-android.sh, build-ios.sh, publish-gitea.sh, publish-play.sh,
│   │            publish-rustore.sh, publish-testflight.sh, symbols.sh, notify.sh}
│   └── git-hooks/{commit-msg, pre-commit, pre-push}
├── .gate-fixtures/                                # негативные фикстуры NF-* (§4.7)
├── docs/                                          # §5
│   ├── README.md · getting-started.md · contributing.md
│   ├── reference/{architecture,ecosystem,release,observability,distribution,strategy-mapping,mac-host,kinship}.md
│   ├── reference/strategy-mobile-canon/           # README + 9 форм слоя mobile/ (путь без PR, §6.4)
│   └── stack/README.md · _templates/*.md · <kind>.md
└── .claude/
    ├── settings.json · plans/roadmap.md
    ├── rules/*.md                                 # 23 (§3.2)
    ├── skills/<9>/SKILL.md (+ references/)        # §3.4
    └── agents/{explorer,reviewer}.md              # как go:
```

Чего в дереве **нет** (осознанно, с указанием шва): базы данных (Room/SQLDelight — шов `entity/<slug>/cache`, §2.4); Retrofit; Koin/Metro/kotlin-inject; Compose Multiplatform; Swift OpenAPI Generator; TCA/Combine; Tuist/XcodeGen (02 §3); Periphery (архив — 02); fastlane на Linux (только на Mac-хосте, §7.4); xtool (05 O-3); Firebase кроме FCM-транспорта; карт (`MapSurface` — шов в `dependencies.md`); IAP/аналитики/рекламных SDK (условные kinds §5.2); `security-crypto` (07 §1.5); `androidx.navigation` 2.x; Material 2; Compose Expressive (01 §1); lint-baseline; `detekt-formatting`; SwiftFormat (nicklockwood).

### §2.2 Что где: `shared/` ↔ модули 01 ↔ пакеты 02

Принцип раздела: в `shared/` — всё, что **не касается экрана и ОС**; в платформах — UI, навигационный контейнер и реализации платформенных интерфейсов. Логика состояний экрана тоже в `shared/` (презентеры), чтобы тестироваться на Linux.

```
shared/src/commonMain/kotlin/<pkg>/
├── SharedContainer.kt         # composition root (§2.4): создаёт граф из PlatformDeps + AppConfig
├── PlatformDeps.kt            # интерфейсы, реализуемые платформой: SecureStore, PushTransport, CrashReporter,
│                              #   LocaleProvider, ConnectivityMonitor, AppInfo
├── core/                      # kit-граница (§1.6) — не импортирует entity/ и feature/
│   ├── api/                   # ApiClient (Ktor), AuthPlugin (Bearer + single-flight refresh), ApiError ↔ ErrorModel,
│   │                          #   RequestId, IdempotencyKey, RetryAfter, generated/ (модели из openapi.yaml)
│   ├── auth/                  # SessionStore (access в памяти 15 мин, refresh в SecureStore 90 д), TokenPair,
│   │                          #   AuthMode (Bearer | Cookie — совместимость, 07 §1.3), AuthState
│   ├── config/                # AppConfig (env, apiBaseUrl, sentryDsn, flags), Env
│   ├── requirements/          # RequirementsClient (GET /api/app/requirements), AppRequirements, FeatureFlags
│   ├── push/                  # PushRegistrar (PUT /api/v1/devices), PushPayload (data-first), Provider
│   ├── deeplink/              # DeepLinkParser: Url → Route (чистая функция), Route (sealed), PendingRoute
│   ├── mock/                  # MockTransport (Ktor MockEngine), Scenario, FixtureLoader
│   ├── observability/         # Log (фасад Kermit), TelemetrySink, Breadcrumb, PiiScrubber
│   ├── i18n/                  # MessageKey (`error.<CODE>` → ключ ресурса), LocaleTag
│   └── lib/                   # Outcome<T>, Clock, DispatcherProvider (expect), Validation, FileCache<T> (okio)
├── entity/<slug>/             # Model, Repository (interface + impl), Dto↔Model, cache policy   — user, session, todo, device
└── feature/<slug>/            # Presenter: state: StateFlow<State>, dispatch(Intent); UseCase; State/Intent
                               #   — shell, login, logout, deleteAccount, profile, todoList, todoDetail, settings, pushOptIn
```

| Слой 01 (Android-канон) | Судьба в В2 | Пакет 02 (iOS-канон) | Судьба в В2 |
|---|---|---|---|
| `core:model` | `shared/…/entity/<slug>/Model` | `Core/Domain` | не создаётся; типы — из `Shared` |
| `core:network` (Retrofit) | `shared/…/core/api` (Ktor) | `Core/Networking` | не создаётся; сеть только через `Shared` (SwiftLint запрещает `URLSession(` в `Features`) |
| `core:data` | `shared/…/entity/<slug>/Repository` | — | — |
| `core:domain` | `shared/…/feature/<slug>/UseCase`, презентеры | модели экранов (`@Observable`) | остаются в `Features/<X>` как тонкие адаптеры над презентером (`for await` через SKIE) |
| `core:common` (Result, dispatchers, Logger) | `shared/…/core/lib`, `core/observability` | `Core/AppFoundation` | остаётся в `Packages/Pure` (форматтеры, парсинг `Bundle` → `AppConfig`, адаптер `os.Logger`) — Linux-собираемый |
| `core:datastore` | `shared/src/androidMain` — actual `SecureStore` (Keystore AES-GCM + DataStore 1.2.1) и `Settings`; в `commonMain` — интерфейсы | `Core/Persistence` | только `KeychainSecureStore` (реализация интерфейса `Shared.SecureStore`, `AfterFirstUnlockThisDeviceOnly`, `kSecAttrSynchronizable=false`, очистка при первом запуске — 02 §5) |
| `core:designsystem` | `androidApp/core/designsystem` (без изменений) | `Core/DesignSystem` | без изменений; `DesignSystemTokens` (чистые значения) — в `Pure` для Linux-теста паритета токенов |
| `core:ui` | `androidApp/core/ui` (без изменений) | часть `DesignSystem` | без изменений |
| `core:navigation` (Nav3) | `androidApp/core/navigation` — `NavKey`-обёртки над `Shared.Route`, `rememberNavBackStack`, реестр `entryProvider` | `Core/Routing` | `NavigationStack`-путь `[Route]`, `Router`; парсинг URL — **в shared** (`DeepLinkParser`), Swift-роутер потребляет `Route` (SKIE → enum) |
| `core:testing` | `androidApp/core/testing` (MainDispatcherRule, Hilt-тестовый раннер, Roborazzi-хелперы, фейки над интерфейсами shared) + `shared/src/commonTest` (фейки `SecureStore`/`Clock`/транспорта) | `TestSupport` | без изменений + `withScenario(.populated)` над `MockTransport` |
| `feature:*` (VM + Compose) | `androidApp/feature/<slug>`: `@HiltViewModel` оборачивает презентер из shared, экраны Compose | `Features/<X>` | View + `@Observable` адаптер презентера |
| `app` | `androidApp/app` | `App/` | без изменений |
| `lint` | `androidApp/lint` | `Tools/archlint` + SwiftLint custom rules | без изменений |
| `baselineprofile` (01 §R10, опционально) | в шаблон v1 не входит — требует устройства/GMD (06 §1); шов: `androidApp/baselineprofile` в `docs/reference/release.md` | — | — |

Платформенные source-set'ы `shared/`: `androidMain` — actual'ы `SecureStore`, `Settings` (DataStore), `DispatcherProvider`, Ktor-движок OkHttp, `FileCache` через `okio` + `Context.filesDir` (передаётся в `SharedContainer`); `iosMain` — actual'ы поверх **Foundation только** (`NSUserDefaults` для `Settings`, `NSBundle`, `NSLocale`, Darwin-движок Ktor); всё, что требует Security/UserNotifications/LocalAuthentication, — Swift-реализация интерфейса из `PlatformDeps`, инжектируемая при старте. Так `shared/` компилируется в klib на Linux без cinterop (03 §8, 06 §5).

### §2.3 Gradle-проекты, convention-плагины, iOS-пакеты

**Gradle-проекты** (typesafe accessors `projects.androidApp.feature.todos`):

| Проект | Плагин `stit.*` | Содержимое | Зависит от |
|---|---|---|---|
| `:shared` | `stit.kmp.library` | §2.2 | только внешние библиотеки |
| `:androidApp:app` | `stit.android.application` + `.compose` + `.hilt` | `App`, `MainActivity`, `NavDisplay`, Hilt-модули биндингов (`SharedModule` → `SharedContainer`), flavors, signing, `AndroidManifest` (intent-filters deep links, FCM/RuStore-сервисы) | всё |
| `:androidApp:core:designsystem` | `stit.android.library.compose` + `.roborazzi` | DTCG-токены → `MaterialTheme`, компоненты, превью | — |
| `:androidApp:core:ui` | `stit.android.library.compose` + `.roborazzi` | композиты состояний (`Loading/Empty/Error/Offline/UpdateRequired/Maintenance`), `PullToRefresh`, диалоги | designsystem, shared |
| `:androidApp:core:platform` | `stit.android.library` + `.hilt` | реализации `PlatformDeps`: `FcmTransport`, `RuStoreTransport`, `NotificationChannels`, `PermissionsGate`, `BiometricUnlock`, `SentryCrashReporter`, `ConnectivityMonitor` | shared |
| `:androidApp:core:navigation` | `stit.android.library.compose` | `NavKey`-обёртки, back-stack, `DeepLink → NavKey` | shared |
| `:androidApp:core:testing` | `stit.android.library` | тестовые правила, фейки, `HiltTestRunner`, Roborazzi-хелперы | shared, core:* |
| `:androidApp:feature:<slug>` | `stit.android.feature` (= library.compose + hilt + roborazzi + стандартные зависимости) | `<X>ViewModel` (обёртка презентера), `<X>Screen`, `<X>NavEntry`, превью, тесты | shared, designsystem, ui, navigation, platform (интерфейсы) |
| `:androidApp:lint` | `stit.jvm.library` + lint-api | детекторы | — |

**Convention-плагины** (`build-logic/convention`, 06 §2.4 + 01 §R3, после AGP 9 — конфигурация через `ApplicationExtension`/`LibraryExtension` напрямую, 06 L-05):

| id | Что настраивает |
|---|---|
| `stit.android.application` | AGP 9.4 + встроенный Kotlin (без `org.jetbrains.kotlin.android`, без kapt), `compileSdk 37` + `compileSdkMinor 2`, `targetSdk 37`, `minSdk 26`, JDK-toolchain 21, flavors `env`, `buildConfigField` из `config/env/<flavor>.properties`, Lint (`warningsAsErrors`, `abortOnError`, `checkDependencies`, `lint.xml`), signing из env (§7.4), Kover, detekt, `resourcePrefix`, badging-проверка (01 §4.1) |
| `stit.android.application.compose` | Compose BOM 2026.09.00, compiler metrics/reports по флагу, `stabilityConfigurationFile` |
| `stit.android.library` / `stit.android.library.compose` | как выше для библиотек; `resourcePrefix` из пути модуля; `testOptions.unitTests.isIncludeAndroidResources`; `--add-opens` для Robolectric (06 L-07) |
| `stit.android.feature` | library.compose + hilt + roborazzi + `implementation(projects.shared, …designsystem, …ui, …navigation, …platform)`; в `afterEvaluate` — падение конфигурации при зависимости на другой `:androidApp:feature:*` |
| `stit.android.hilt` | Hilt 2.60.1 + KSP 2.3.12, androidx.hilt 1.4.0 (`hiltViewModel` из `androidx.hilt.lifecycle.viewmodel.compose` — 01 §1) |
| `stit.android.roborazzi` | Roborazzi 1.74.0, `@Config(sdk=[37])`, каталоги `screenshots/`, сравнение с порогом |
| `stit.kmp.library` | targets `androidLibrary { withHostTest {} }` (06 §5), `jvm()`, `iosArm64()`, `iosSimulatorArm64()`; `Shared` static framework + `export`-список; SKIE; BCV (`klib { enabled = true }`); `-Xexpect-actual-classes`; detekt/Kover (jvm); запрет `cinterops {}` (скрипт `kmp-check`, §4.3) |
| `stit.jvm.library` | Kotlin JVM 21, JUnit, detekt |
| `stit.quality` (root) | Spotless 8.10.2 + ktlint 1.8.0, агрегация Kover, Konsist-тест, `check`-цепочка |

**iOS-пакеты** (SwiftPM local, buildable folders, один `.xcodeproj`; правила 02 §7.2/7.3):

| Пакет | Таргеты | Импортирует | Собирается на Linux |
|---|---|---|---|
| `Pure` | `AppFoundation` (парсинг `AppConfig` из `Bundle`-словаря, форматтеры, `LogSink` над `os.Logger`/print-заглушкой, `Env`), `DesignSystemTokens` (значения DTCG-токенов), тесты Swift Testing | Foundation | **да** — единственная часть iOS в Linux-гейте |
| `Core` | `DesignSystem` (SwiftUI-тема, компоненты, Liquid Glass), `Routing` (`Router`, `[Route]`, pending route), `Persistence` (`KeychainSecureStore`), `Platform` (`ApnsRegistrar`, `NotificationCategories`, `Permissions`, `Biometrics`, `SentryCrashReporter`, `Connectivity`), `SharedBridge` (bootstrap `SharedContainer`, `AsyncStream`-адаптеры, `KotlinThrowable → AppError`) | Pure, Shared, SwiftUI | нет |
| `Features` | `Shell`, `AuthFeature`, `ProfileFeature`, `TodosFeature`, `SettingsFeature`; product `Features` — umbrella-таргет, реэкспортирующий все фичи, чтобы добавление фичи не трогало `.pbxproj` | Pure, `Core/{DesignSystem,Routing,SharedBridge}`, Shared | нет |
| `TestSupport` | фейки `PlatformDeps`, `withScenario`, конфиг snapshot-testing (`swift-snapshot-testing` 1.19.5), `MockTransport`-бутстрап | Core, Shared | нет |

`App/` — composition root: `AppEnvironment` собирает `SharedContainer` из Swift-реализаций (`KeychainSecureStore`, `ApnsRegistrar`, …) и `AppConfig` из `Info.plist` (значения из xcconfig; ловушка `https:/$()/host` — 02 §7.4).

### §2.4 DI и persistence — разрешение противоречия (ж)

06 показал, что Hilt + KSP работает на Android под AGP 9.4; 03 — что Hilt непригоден в `shared/` (JVM/Android-only). Оба правы для своих слоёв. Решение:

- **`androidApp/` — Hilt** (01 §2, 06 §2.10). `@HiltViewModel` оборачивает презентер из shared; платформенные реализации (`core:platform`) биндятся в `app`.
- **`shared/` — без DI-фреймворка.** Ручной composition root `SharedContainer(deps: PlatformDeps, config: AppConfig)` с ленивыми `val` и фабриками презентеров (`fun todoList(): TodoListPresenter`). Это одновременно **плоский фасад для Swift** (одна точка входа, final-класс, без generic'ов — правило `kmp-public-api`) и единственный источник графа для Hilt-модуля `SharedModule` (`@Provides fun container(): SharedContainer`, `@Provides fun todoRepository(c) = c.todoRepository`). Объём — десятки строк; тестируется на JVM конструированием с фейками.
- Koin отвергнут (service-locator без compile-time-проверки — 01 §2), Metro/kotlin-inject — не сейчас (молодые, найм); триггер пересмотра: `SharedContainer` > ~40 членов или третья платформа → Metro (compile-time, KMP).

**Persistence.** В шаблоне v1 базы данных нет: репозитории держат in-memory-кэш + `FileCache<T>` (okio, JSON) для «последнего снимка» офлайн-чтения; настройки — `Settings` (DataStore / `NSUserDefaults`); секреты — `SecureStore`. Причина — 03 §8: Room KMP и SQLDelight-native тянут cinterop/нативный драйвер, который ломает Linux-компиляцию iOS-klib (06 §5 подтверждает, что без cinterop klib собирается). Шов для offline-first проектов: `entity/<slug>/cache` + запись в `dependencies.md`; кандидат — SQLDelight с `[spike]` компиляции на Linux (§9.1). Room 2.8.5 остаётся Android-only опцией внутри `androidApp/core/platform`, если проект осознанно отказывается от общей персистентности (01 §1).

### §2.5 Сеть, контракт и auth — разрешение противоречия (е)

- **Ktor 3.6 в `shared/`**, движки OkHttp (Android) и Darwin (iOS); **модели** генерируются из `api/openapi.yaml` генератором `kotlin` (library `multiplatform`, `models-only`, kotlinx-serialization, `ignoreUnknownKeys`) в `shared/src/commonMain/kotlin/<pkg>/core/api/generated/` и **коммитятся**; `gen-check` в гейте (§4.3). Клиентские вызовы — рукописные (`TodosApi` над `ApiClient`), по 07 §2.5; Ktorfit 2.7.5 — опция, не дефолт. Причина отказа от генерации клиента — 06 §8.8 (cookie → query, DTO-обёртки) и 07 §2.3.
- **Auth = Bearer + refresh в JSON** (07 §1.3b): `Authorization: Bearer`, `POST /api/auth/refresh {refresh_token}`, `X-Client: android|ios`, ротация refresh, single-flight refresh в `AuthPlugin`, `GET/DELETE /api/auth/sessions`. Reference-фича `auth` построена именно под это. Бэкенд, ещё не реализовавший B-1…B-3, обслуживается **режимом совместимости `AuthMode.Cookie`** в `core/auth` (Ktor `HttpCookies` + персистентное хранилище cookie в `SecureStore`, CSRF-заголовок как у SPA); режим выбирается в `docs/stack/auth.md#mode`, `/init-service` печатает BACKEND-HANDOFF B-1…B-3 при `cookie`. Cookie-режим — `[spike]` на обоих движках (§9.1). Pinning — выключен, шов `ApiClient.pinning` (07 §1.6). OIDC/passkeys — расширения, не в v1 (07 §1.7).
- **Swift OpenAPI Generator** в шаблон не входит: в В2 iOS не имеет собственного сетевого слоя. Он остаётся задокументированной опцией только для гипотетического pure-Swift проекта вне шаблона (02 §4 — канон), не для форк-пути `--platform ios` (который сохраняет `shared/`, §2.8).

### §2.6 Матрица зависимостей и её enforcement

**Android / shared** (строка → может зависеть от столбца):

| ↓ от \ на → | shared | core:designsystem | core:ui | core:platform | core:navigation | core:testing | feature:* | app |
|---|---|---|---|---|---|---|---|---|
| shared | — | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| core:designsystem | ✗ | — | ✗ | ✗ | ✗ | test | ✗ | ✗ |
| core:ui | ✓ | ✓ | — | ✗ | ✗ | test | ✗ | ✗ |
| core:platform | ✓ | ✗ | ✗ | — | ✗ | test | ✗ | ✗ |
| core:navigation | ✓ | ✗ | ✗ | ✗ | — | test | ✗ | ✗ |
| core:testing | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✗ | ✗ |
| feature:X | ✓ | ✓ | ✓ | ✓ (интерфейсы) | ✓ | test | **✗** (другие фичи) | ✗ |
| app | ✓ | ✓ | ✓ | ✓ | ✓ | test | ✓ | — |
| lint | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |

Внутри `shared/`: `core/**` не импортирует `entity/**`, `feature/**`; `entity/**` не импортирует `feature/**`; `feature/<a>` не импортирует `feature/<b>` (общее поднимается в `entity` или `core`).

**iOS**: `App` → всё; `Features/<X>` → `Pure`, `Core/{DesignSystem,Routing,SharedBridge}`, `Shared`; `Features/<X>` ✗ `Features/<Y>`, ✗ `Core/{Persistence,Platform}` (реализации только из `App`); `Core/*` → `Pure`, `Shared`; `Pure` → Foundation.

| Механизм | Что ловит | Где в гейте |
|---|---|---|
| Gradle-граф + `stit.android.feature` (`afterEvaluate`) | feature → feature, feature → app | конфигурация (любой `./gradlew`) |
| Konsist 0.17.3 (`:androidApp:app` `ArchitectureTest`, 01 §R6) | слои `shared < core < feature < app`; `core/**` ↛ `entity/feature` внутри shared; `@HiltViewModel` только в `feature:*`; `*Screen`/`*Preview` именование; никаких `androidx.compose.ui.test.junit4.` v1-импортов; публичные классы `feature/*Presenter` — final, без generic'ов в сигнатурах (дополнение к `apiCheck`) | `unit` |
| detekt `ForbiddenImport` (01 §R4) | `android.util.Log`, `androidx.compose.material.` (M2), `retrofit2.`, `androidx.security.crypto.`, `java.util.logging`, `kotlinx.coroutines.GlobalScope`; в `shared/commonMain` — `android.`, `java.` | `detekt` |
| Android Lint + `:androidApp:lint` (01 §4.1/§R5) | сырые `Color(0xFF…)`/`dp`-литералы вне designsystem, `DesignSystemDetector`, `MissingPermission`, `ExportedReceiver`, `UnsafeIntentLaunch` | `lint` |
| `Tools/archlint` (02 §7.7) | граф `Package.swift` всех пакетов: Features ↛ Features/Persistence/Platform; Core ↛ Features | `swift` (Linux — по `dump-package`, без сборки) |
| SwiftLint custom regex (`.swiftlint.yml`, 02 §7.4) | `import (Auth\|Profile\|Todos\|Settings)Feature` в `Features/`, `import (Persistence\|Platform)$` в `Features/`, `URLSession(` вне `SharedBridge`, `print(`, `import Combine`, `@preconcurrency` без комментария | `swift` |
| `kmp-check` (06 §8.4, §4.3) | `cinterops {`/`kotlin("native.cocoapods")` в `shared/build.gradle.kts`; отсутствие klib-каталога после компиляции | `kmp` |

### §2.7 Reference-фичи (включая `demo` как фичу №1)

| # | Фича | Источник в strategy (`mobile/<surface>/`) | `shared/` | `androidApp/` | `iosApp/` | Что демонстрирует | `/de-template` |
|---|---|---|---|---|---|---|---|
| 1 | **`demo`** — mock-режим | `entry-points.md` (`demo`), `shell.md#overlays` | `core/mock` (`MockTransport`, сценарии, `FixtureLoader`), тесты презентеров на фикстурах | flavor `demo` (`applicationIdSuffix ".demo"`, `API_MODE=mock`), debug-панель (dev/staging: сценарий, задержка, ошибка N %), runtime-переключатель в prod по флагу `requirements.flags.demo_login` (для App Review — 05 §1.3) | scheme `Demo`, та же панель, тот же флаг | одна фикстурная база для тестов, demo-сборки, скриншотов и Maestro; ревьюер магазина без бэкенда | **оставить** (инфраструктурная) |
| 2 | `shell` | `shell.md`, `flows.md` (`app-start`) | `feature/shell/AppShellPresenter` (auth-gate, requirements, offline, pending route) | `NavDisplay` + табы, оверлеи `UpdateRequired`/`Maintenance`/`Offline`, edge-to-edge, predictive back | `RootView`, `NavigationStack`, те же оверлеи, Liquid Glass-таб-бар | старт приложения как единый сценарий; порядок: requirements → session → route | оставить |
| 3 | `requirements` | `entry-points.md`, `shared-api.md` | `core/requirements` | экраны в `core:ui` | то же | `GET /api/app/requirements`, `min_version`, maintenance, флаги (07 §4.3, B-17) | оставить |
| 4 | `auth` | `screens.md` (`Login`, `DeleteAccount`), `features.md` (`login`, `logout`, `delete-account`, `sessions`) | `entity/session`, `feature/login\|logout\|deleteAccount`, `core/auth` | `LoginScreen`, `SessionsScreen`, `DeleteAccountScreen`, биометрическая разблокировка (опция) | те же View | Bearer + refresh, `SecureStore`, удаление аккаунта (App Store 5.1.1(v)), список сессий | оставить при `auth.md#mode ≠ none` |
| 5 | `profile` | `screens.md` (`Profile`, `ProfileEdit`), `features.md` (`edit-profile`, `change-password`) | `entity/user`, `feature/profile` | экраны + pull-to-refresh + состояния | те же | форма с валидацией, оптимистичное обновление, ошибки `ErrorModel.code → MessageKey` | **удалить** |
| 6 | `todos` | `entities.md` (`todo`), `screens.md` (`TodoList`, `TodoDetail`), `flows.md` (`deep-link-todo`) | `entity/todo`, `feature/todoList\|todoDetail`, `core/deeplink` маршруты | список/деталь/создание/переключение/удаление, deep link `https://<host>/todos/{id}` + `<scheme>://todos/{id}`, pending route после логина, `Idempotency-Key` на мутациях, offline read-only | те же + Universal Links/AASA | CRUD-образец, deep links end-to-end, идемпотентность (07 §2.6) | **удалить** (deep-link-инфраструктура остаётся в `core/deeplink`) |
| 7 | `settings` | `screens.md` (`Settings`, `About`, `Legal`), `features.md` (`switch-theme`, `switch-language`, `toggle-notifications`) | `feature/settings`, `Settings` | тема system/light/dark, язык ru/en, уведомления (пред-запрос разрешения), «О приложении» (версия/build/`APP_ENV`), правовые документы (доступны до логина — 05 L-3) | те же | i18n-реестр, DTCG-токены обеих платформ, permission-flow | оставить (минимум: тема/язык/about/legal) |
| 8 | `push` | `permissions.md` (`notifications`), `entry-points.md` (`push`), `shared-api.md` (`devices`) | `core/push` (`PushRegistrar`, парсер payload, dedup по `notification_id`) | `FcmTransport` + `RuStoreTransport` (мульти-провайдер 05 §2), каналы, `PUT /api/v1/devices` при логине/смене токена/локали | `ApnsRegistrar`, категории, `sandbox` для dev | data-first payload → deep link; провайдер по `PUSH_PROVIDERS` (07 §6) | оставить при непустом `push.md#providers` |

Общие для всех фич образцы: тест презентера на JVM с `MockTransport` + фикстурой; Roborazzi-скриншот экрана в состояниях `loading/empty/populated/error` (Android) и snapshot (iOS, Mac); Maestro-флоу в `e2e/common` (обе платформы) на сценарии `populated`.

**Prune-лист `/de-template`** (форма — как `spa:`/`go:` `references/prune.md`, 8 пунктов):

1. `shared/src/commonMain/kotlin/<pkg>/{entity/todo,entity/user,feature/todoList,feature/todoDetail,feature/profile}` + их `commonTest`; маршруты `Route.TodoList/TodoDetail/Profile*` в `core/deeplink/Route.kt`; сценарные фикстуры `mock/fixtures/*/{todos,user}.json` (кроме `session`).
2. `androidApp/feature/{todos,profile}` (+ `settings.gradle.kts` `include`), их `screenshots/`, строки `feature_todos_*`/`feature_profile_*` в `res/values*/strings.xml`, `NavEntry`-регистрации в `app`.
3. `iosApp/Packages/Features/Sources/{TodosFeature,ProfileFeature}` + `__Snapshots__`, таргеты в `Package.swift` и umbrella; `Localizable.xcstrings`-ключи `todos.*`/`profile.*`; intent-filter/AASA-пути `/todos/*`.
4. `e2e/{common,android,ios}/{todos,profile}-*.yaml`; Konsist/архитектурные тесты, ссылающиеся на удалённые модули (перегенерируются).
5. `api/openapi.yaml` — **не** трогается (копия контракта бэкенда); удалённые фичи перестают вызывать `TodosApi` → удалить `core/api/TodosApi.kt`, `UsersApi.kt`; регенерировать модели (`make gen`) — неиспользуемые модели допускаются.
6. `docs/stack/*.md` — примеры значений (`todos`, `profile`) заменяются проектными; `docs/reference/strategy-mapping.md` — таблица reference-соответствий очищается.
7. Пресет: `preset: consumer` добавляет `analytics.md`/`payments.md` из `_templates`; `enterprise` — переключает `/release`-каналы.
8. `--platform android|ios` — §2.8; `--keep todos` — оставить фичу как учебную (не по умолчанию).

### §2.8 Форк-пути Android-only / iOS-only

| Шаг | `--platform android` | `--platform ios` |
|---|---|---|
| Удаляется | `iosApp/`, `.xcode-version`, `.swift-version`, `e2e/ios`, `scripts/tools/mac.sh`, `scripts/release/{build-ios,publish-testflight}.sh`, `docs/reference/mac-host.md`; в `shared/build.gradle.kts` — `iosArm64()`, `iosSimulatorArm64()`, framework, SKIE, klib-дампы `shared.klib.api`; iOS-секции `docs/stack/{runtime,distribution,signing,compliance,push,deep-links}.md`; правила `ios-ui`, `swift-concurrency`, `xcode-project`; Swift-стадии `check.sh` | `androidApp/`, Android-плагины `build-logic` (остаются `stit.kmp.library`, `stit.jvm.library`, `stit.quality`), `config/{detekt/compose-rules,lint,env}`, `e2e/android`, `scripts/release/{build-android,publish-play,publish-rustore}.sh`; в `shared/` — `androidLibrary {}` + `androidMain`/`androidHostTest`; Android-секции kinds; правила `android-ui`, `gradle-build` (сужается до KMP); Android-стадии `check.sh` |
| Остаётся | `shared/` с `androidLibrary`+`jvm`; Linux-гейт полный | `shared/` с `jvm`+iOS-таргетами (Linux: klib + jvmTest); `check-mac` обязателен для UI |
| `service.md` | `platforms: [android]` | `platforms: [ios]` |
| Обратимость | `/init-service --add-platform ios` восстанавливает половину из шаблона (upstream-ветка) — `[spike]` на реальном форке | аналогично `--add-platform android` |

iOS-only форк **сохраняет** `shared/` (логика на Kotlin, JVM-тесты на Linux): это цена ecosystem-constraint'а «агент на Linux»; чистый Swift-проект — вне шаблона (§2.5).

### §2.9 Версии и каталог: точки решения

Каталог `gradle/libs.versions.toml` собирается из 01 §R2 (Android) + 03 §6.3 (KMP) + 07 (сеть/observability). Ниже — только точки, где потоки предлагали альтернативы, с итогом:

| Область | Итог | Отвергнуто | Ссылка |
|---|---|---|---|
| Сеть | Ktor 3.6.0 (+ `content-negotiation`, `auth` не используется — свой `AuthPlugin` ради single-flight), kotlinx-serialization 1.11.0 | Retrofit 3.0.0, Ktorfit (опция) | 01 §1, 07 §2.3 |
| DI | Hilt 2.60.1 (Android) + ручной `SharedContainer` | Koin 4.2.2, Metro 1.4.x, kotlin-inject 0.9.0 | §2.4 |
| Навигация | Navigation 3 1.1.7; SwiftUI `NavigationStack` | Navigation 2.x, Decompose/Voyager | 01 §2, 02 §2 |
| Kotlin ↔ Swift | Obj-C export + SKIE 0.10.14; static framework; direct integration | Swift export (Alpha — триггер), SwiftPM+XCFramework (для split-team, документирован) | 03 §3 |
| API-модели | openapi-generator 7.25.0 `kotlin` multiplatform models-only | полный клиент; Swift OpenAPI Generator | 07 §2.5, 06 §8.8 |
| Логи/observability | Kermit 2.2.0 фасад; sentry-android 8.57.0 + Gradle-плагин 6.22.0; sentry-cocoa 9.28.0 (SPM); пустой DSN = выключено | OTel SDK на клиенте (шов `TelemetrySink`) | 07 §7 |
| Тесты Android | JUnit 4.13.2, Robolectric 4.17, Compose test v2, Roborazzi 1.74.0, Turbine 1.2.1, MockK 1.14.11 (фейки предпочтительнее), Kover 0.9.9, Konsist 0.17.3 | официальный screenshot-плагин (deprecated), Paparazzi | 01 §7 |
| Тесты iOS | Swift Testing; XCTest для UI/perf; swift-snapshot-testing 1.19.5; swift-dependencies 1.17.1 | Quick/Nimble | 02 §6 |
| Стиль | Spotless 8.10.2 + ktlint 1.8.0; detekt 1.23.8 + compose-rules 0.4.28; SwiftLint 0.65.1 (`--strict`) + swift-format (Swift 6.4, `--strict`) | detekt-formatting, SwiftFormat, Periphery | 06 L-01/L-03, 02 §6 |
| Анализ зависимостей | sort-dependencies 0.21.0 (в гейте); dependency-analysis 3.19.2 — вне гейта до поддержки AGP 9.4 (verb `deps-report`) | — | 06 L-05 |
| Изображения | Coil 3.6.2 (Android); `AsyncImage` (iOS) | Kingfisher/Nuke | 01 §1 |
| BCV | binary-compatibility-validator 0.18.2, `klib { enabled = true }` | — | §4.3 `[spike]` |

---
## §3 Обвязка

Форма обвязки — та же, что у `go:`/`spa:`/`bot:`: `CLAUDE.md` = ядро + роутер (≤ 200 строк), path-scoped правила с frontmatter `name`/`description`/`paths:`, `settings.json` с allow/deny, девять скиллов, два агента (`explorer`, `reviewer`), planning-first через `.claude/plans/roadmap.md`. Отличия — только содержательные: две платформы, Mac-хост как внешняя стадия гейта, магазины и право.

### §3.1 `CLAUDE.md` — листинг (≈ 150 строк)

```
# mobile-template — гайд для агентов

Шаблон мобильного приложения: KMP-ядро `shared/` (Kotlin 2.4, Ktor 3.6,
kotlinx-serialization, презентеры на StateFlow) + нативный UI — `androidApp/`
(AGP 9.4, Compose BOM 2026.09, Material 3, Navigation 3, Hilt) и `iosApp/`
(Xcode 27, Swift 6.4, SwiftUI, `@Observable`, SwiftPM local packages).
Гейт: Spotless/ktlint + detekt + Android Lint + Robolectric/Roborazzi + Kover +
Konsist; swift-format + SwiftLint + `swift test` чистых пакетов; macOS-стадии —
по SSH (`make check-mac`).

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
  corp-контур — Gitea (Release, Packages), Vaultwarden (`mobile/<org>/<app>`),
  ntfy (`releases-<org>`), GlitchTip/Sentry.
- **Используется:** магазинами (RuStore, Google Play, App Store) и пользователями.
  В оркестрацию `<org>/infra` не входит — образа и compose-сервиса у него нет.
- Полная карта — `docs/reference/ecosystem.md`.

## Точка входа

- **`/smartdev`** — cockpit сессии: `make doctor` → `make status` (ветка,
  платформы, Mac-хост, drift API-спеки, активный план, гейты) + переключение
  в режим. Запускай в начале / после `/clear`.
- **Потребителю шаблона** — `docs/getting-started.md`: форк → `/init-service` →
  проектирование (`/stack-deep-dive` здесь; карта `mobile/<surface>/` — в
  `<org>/strategy`) → `/de-template` → свой проект.
- **Planning-first.** Содержательное изменение — через секцию в
  `.claude/plans/roadmap.md` (skill `/plan`). Прямая правка — только
  truly-atomic и багфиксы.

## Проектирование первично, код вторичен

Карта приложения проектируется в соседнем `<org>/strategy`: слой
`docs/projects/<slug>/mobile/<surface>/` — 9 kinds (entities · screens · flows ·
features · shell · entry-points · permissions · shared-api · shared-lib); там её
ведёт `/mobile-deep-dive`. Этот репо её **читает** (cross-repo, read-only):
`/scaffold` и `/de-template` разворачивают stable-записи в код и сами выводят
пути (`shared/…/feature/<slug>/`, `androidApp/feature/<slug>/`,
`iosApp/Packages/Features/Sources/<Slug>Feature/`) из kind + slug. Артефакты
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
| `make fmt` | автоформат Kotlin (Spotless/ktlint) + Swift (swift-format) — единственные автофиксеры |
| `make test` | JVM-тесты: `shared` (jvmTest), Android (Robolectric, Konsist), Swift `Pure` |
| `make screenshots` / `screenshots-record` | Roborazzi verify / перезапись эталонов |
| `make kmp` | `shared` → klib под iOS-таргеты + проверка каталога (SKIPPED ≠ OK) |
| `make gen` / `gen-check` | модели из `api/openapi.yaml` (+ `openapi.lock`) / проверка дрейфа |
| `make check` | **gate** (Linux-полный): всё выше + detekt + Lint + Kover + assembleDebug + swift-стадии |
| `make check-fast` | формат + detekt + `shared` jvmTest + swift lint — цикл «пока пишу» |
| `make check-mac` | macOS-стадии по SSH (`MAC_HOST`): link framework, xcodebuild, планы Unit + Snapshot |
| `make check-full` | `check` + `check-mac`; обязателен перед `/release` |
| `make gate-selftest` | негативные фикстуры: гейт обязан краснеть |
| `make run-android` / `run-ios` | dev-сборка на эмулятор / (Mac) симулятор |
| `make e2e` | Maestro-флоу (нужно устройство/эмулятор; вне gate) |
| `make release-*` | см. `docs/reference/release.md`; оркеструет `/release` |
| `make status` | сводка для `/smartdev` |

Перед claim «готово» / push — **`make check`** (его же гоняет `pre-push`, с
`GATE_RELEASE=1`). «Готово по iOS» — только после `make check-mac`: Linux-гейт
явно печатает, какие стадии не выполнил.

## Карта: где что

| Тема | Где |
|---|---|
| Архитектура `shared/` ↔ `androidApp/` ↔ `iosApp/`, матрица зависимостей | `docs/reference/architecture.md` + rules `kmp-shared`, `android-ui`, `ios-ui` |
| Фасад Kotlin → Swift (SKIE, `apiDump`) | rule `kmp-public-api` |
| Навигация и deep links | rules `navigation`, `deep-links-and-push` |
| Экосистема (шаблоны, контракты, Gitea Release как STOP) | `docs/reference/ecosystem.md` |
| Релиз, версии, магазины, Mac-хост | `docs/reference/release.md`, `distribution.md`, `mac-host.md` |
| Observability (Sentry/GlitchTip, Kermit, `os.Logger`) | `docs/reference/observability.md` + rule `observability` |
| Тесты (JVM, Robolectric, Roborazzi, Swift Testing, snapshot, Maestro) | rule `tests-pipeline` |
| env, флейворы, xcconfig, секреты, сессия | rules `env-and-config`, `signing-and-secrets`, `auth-and-session` |
| API-контракт, кодоген, mock-режим | rules `api-contract`, `mock-and-fixtures` |
| Магазины и право | rules `store-compliance`, `privacy-and-law` |
| Карта приложения (9 kinds, cross-repo) | `../strategy/docs/projects/<slug>/mobile/<surface>/` (канон — `_template/mobile/README.md` в strategy; копия — `docs/reference/strategy-mobile-canon/`) |
| Stack-артефакты (13 + 2 kinds) | `docs/stack/README.md` |
| Forward-only стиль | rule `forward-only` |

## Инварианты (что в 2026 иначе, чем агент «помнит»)

- **AGP 9.4 со встроенным Kotlin**: без `org.jetbrains.kotlin.android`, без
  kapt (только KSP), `kotlin { compilerOptions }`, `targetSdk` задаётся явно;
  convention-плагины `stit.*` — единственное место конфигурации Android/KMP
- **Navigation 3**: `NavKey` + `rememberNavBackStack` + `NavDisplay`; без
  `NavHost`/`NavController`
- **Compose test API v2** (`androidx.compose.ui.test.junit4.v2`); Material 3
  без Expressive; `hiltViewModel` из `androidx.hilt.lifecycle.viewmodel.compose`
- **`shared/` — чистый KMP**: platform API в `commonMain` нет; cinterop и
  CocoaPods нет; iOS-actual'ы — только Foundation; остальное — Swift-реализации
  интерфейсов `PlatformDeps`
- **Фасад для Swift плоский**: final-классы, data/sealed, `suspend`/`StateFlow`,
  без generic'ов в публичных сигнатурах; `apiDump` коммитится
- **Swift 6 mode + approachable concurrency**: `MainActor` по умолчанию,
  предупреждения = ошибки, без Combine/TCA; `@preconcurrency` — только с
  комментарием-причиной
- **Liquid Glass / iOS 27 SDK**: системные компоненты; `PrivacyInfo.xcprivacy`
  обязателен
- **Один автофиксер на язык**: Spotless (ktlint 1.8) и swift-format;
  detekt-formatting и SwiftFormat не подключаются
- **Токены**: цвета/размеры только через DTCG-токены обеих платформ;
  паритет проверяет `tokens-check`
- **Секреты**: токены только в `SecureStore` (Keystore/Keychain), access — в
  памяти; `security-crypto` запрещён; ключи подписи — в Vaultwarden, не в репо
- **Сеть — только через `shared`**: `URLSession`/`OkHttp` напрямую в фичах запрещены
- **Комментарии** — только WHY (rule `comments`); **forward-only** (rule `forward-only`)

## Git workflow

- **Conventional Commits**: `feat|fix|refactor|docs|test|chore|meta|perf|style`,
  `<type>(<scope>)?: <subject>`, subject ≤ 72
- Push в `main` разрешён (прямой или через PR); линейная история
- Хуки: `pre-commit` (формат staged + commitlint), `pre-push`
  (`GATE_RELEASE=1 make check`); ставятся `make setup`
- Теги `vX.Y.Z` — только через `/release` (versionCode / CFBundleVersion
  выводятся из тега)

## Границы

### Делать всегда
- Перед claim «готово» — `make check`; «готово по iOS» — `make check-mac`
- Изменение — через план (skill `/plan`)
- Новая логика — в `shared/` (презентер + тест на JVM), UI — тонкий;
  cross-feature — через `entity`/`core`
- Новый endpoint/поле — сначала `api/openapi.yaml` (`make gen`); бэкенд
  отстаёт — BACKEND-HANDOFF
- При правке `docs/stack/*.md` — следить за `status: stable` и `## Generates`;
  карта `mobile/` правится только в `<org>/strategy`
- Правка deep-link-хостов, push-провайдеров, `SENTRY_DSN`, min-version —
  INFRA-HANDOFF (rule `infra-impact`)

### Сначала спросить
- Новая зависимость / SDK (запись в `docs/stack/dependencies.md` +
  `compliance.md` — privacy manifest)
- Изменение версий Kotlin / AGP / Compose BOM / Xcode (`.xcode-version`)
- Правка permissions в `AndroidManifest.xml`, usage-descriptions в `Info.plist`,
  entitlements
- Удаление reference-фич вручную (это работа `/de-template`)
- Любой `--no-verify`; любой `make release-*`

### Не делать никогда
- `--no-verify` без явного разрешения
- Коммитить `*.jks`, `*.p12`, `*.p8`, `*.mobileprovision`, `local.properties`,
  `Local.xcconfig`, `.env.local`
- Править `*.pbxproj` руками (product name, bundle id, team — в xcconfig;
  таргеты — в `Package.swift`)
- Править `build/`, `core/api/generated/`, `shared/api/*.api` руками
  (только `make gen` / `make api-dump`)
- `android sdk install` / `sdkmanager` и `./gradlew bundleRelease` «по пути»
- Считать зелёный Linux-гейт доказательством работоспособности iOS-UI

## Forward-only

Любой текст в репо описывает **только текущее состояние** — не «было/стало».
История — в `git log` + Conventional Commits. Полное правило —
`.claude/rules/forward-only.md`.

## Авто-память

Запись в файловую авто-память запрещена глобально. Не предлагай «сохранить в
память». Долгосрочное знание — в репо: `CLAUDE.md`, `.claude/rules/`, `state/`
каждого скила.
```

Что осталось за пределами `CLAUDE.md` осознанно (и где живёт): версии библиотек — `libs.versions.toml` + `docs/stack/runtime.md`; списки ловушек 01/02/06 — `docs/reference/architecture.md#traps` и `/triage-failure`; чек-листы магазинов — `docs/reference/distribution.md` + rule `store-compliance`.

### §3.2 Правила — единый список (разрешение противоречия (г))

Пять списков (01 §R8, 02 §7.8, 03 §6.3, 05 §8.5, 07 §10.3) сведены по принципу «одно правило = один путь-скоуп = один повод открыться агенту». Дубли схлопнуты: `module-architecture` (01/02) → `android-ui`/`ios-ui`/`kmp-shared` по половинам; `compose-ui` → `android-ui`; `swiftui` → `ios-ui`; `env-local` (01/03) + `env-and-config` (02/07) + `config-and-envs` (07) → `env-and-config`; `auth-and-secrets` (07) разделено на `auth-and-session` (код) и `signing-and-secrets` (релизные секреты, 02/05); `release-artifacts` (03) + `release-flow` (05) → `release-flow`. Итог — 23 файла (21 мобильных + 2 общих экосистемных `forward-only`/`comments`): больше, чем у `go:` (8) и `bot:` (10), потому что две платформы × магазины × право; каждое короткое (≤ 60 строк).

Форма frontmatter (как в `go:`/`spa:`):

```
---
name: kmp-shared
description: Что можно и нельзя в shared/ (KMP): чистый commonMain, actual'ы, Foundation-only на iOS, cinterop запрещён
paths:
  - "shared/**"
---
```

| # | Правило | `paths:` | Содержание (одной строкой) | Родство |
|---|---|---|---|---|
| 1 | `forward-only` | `**/*.md` | идентично `go:`/`spa:`: только текущее состояние, планы без архива | идентично |
| 2 | `comments` | `**/*.kt`, `**/*.kts`, `**/*.swift` | WHY-only, по-русски, однострочные; примеры на Kotlin и Swift | по форме |
| 3 | `kmp-shared` | `shared/**` | `commonMain` без platform API; `expect/actual` только для `DispatcherProvider`/`Settings`/`FileCache`; `iosMain` — Foundation only; cinterop/CocoaPods запрещены; презентер = `StateFlow<State>` + `dispatch(Intent)`; `core/**` не импортирует `entity/feature`; тест каждой логики — `commonTest` (запуск `jvmTest`) | новое |
| 4 | `kmp-public-api` | `shared/src/commonMain/**` | фасад для Swift: final-классы, data/sealed, `suspend`/`StateFlow`, без generic'ов/`Any`/интерфейсов с default-методами/`inline`-классов в публичных сигнатурах; `Route`/`State` — sealed (SKIE → enum); `apiDump` коммитится, изменение фасада = осознанное решение в плане | новое (03 §6.3) |
| 5 | `android-ui` | `androidApp/**` | модули и их допустимые зависимости (§2.6); `@HiltViewModel` — тонкая обёртка презентера; экран = `*Screen(state, onIntent)` без VM внутри (previewable); `Preview` на каждое состояние + Roborazzi; Material 3 без Expressive; edge-to-edge, predictive back; `resourcePrefix`; строки только из ресурсов; `hiltViewModel` из `androidx.hilt.lifecycle.viewmodel.compose` | по форме (01 §R8 module-architecture + compose-ui) |
| 6 | `ios-ui` | `iosApp/**/*.swift` | SwiftUI only; модель экрана — `@Observable` адаптер над презентером (`for await` через SKIE), без бизнес-логики; `Features` не импортируют друг друга/`Platform`/`Persistence`; строки — `Localizable.xcstrings`; Liquid Glass системными компонентами; a11y-идентификаторы для XCUITest; snapshot на каждое состояние | по форме (02 §7.8) |
| 7 | `swift-concurrency` | `iosApp/**/*.swift`, `iosApp/**/Package.swift` | Swift 6 mode, `MainActor` default, `SWIFT_APPROACHABLE_CONCURRENCY`, upcoming features (02 §2); `@preconcurrency` только с WHY; без Combine; `Sendable` на границе с `Shared`; `Task` со структурированной отменой | новое (02 §7.8) |
| 8 | `navigation` | `shared/**/deeplink/**`, `androidApp/core/navigation/**`, `androidApp/feature/**/*Nav*.kt`, `iosApp/**/Routing/**`, `iosApp/**/*Route*.swift` | `Route` — sealed в shared; `DeepLinkParser` — чистая функция `Url → Route?` с тестами; Android: Nav3 `NavKey` ↔ `Route`, back-stack сохраняется; iOS: `NavigationStack(path: [Route])`; pending route после логина; никаких строковых маршрутов | новое (01 navigation + 02 Router) |
| 9 | `design-tokens` | `androidApp/core/designsystem/**`, `androidApp/core/ui/**`, `iosApp/Packages/Pure/Sources/DesignSystemTokens/**`, `iosApp/Packages/Core/Sources/DesignSystem/**` | DTCG-имена (07 §8), семантические токены, паритет платформ (`tokens-check`), тема system/light/dark, dynamic type/font scale, без сырых цветов/размеров в фичах | по форме (`spa:` shared-ui) |
| 10 | `gradle-build` | `**/build.gradle.kts`, `build-logic/**`, `gradle/**`, `settings.gradle.kts`, `gradle.properties` | конфигурация только через `stit.*`; версии только в `libs.versions.toml` (SDK-уровни тоже — 06 §8.7); без kapt, без `org.jetbrains.kotlin.android`; новый модуль = `include` + плагин + запись в `architecture.md`; `gradle.properties` не трогать без плана; `sort-dependencies` | новое (01 §R8) |
| 11 | `xcode-project` | `iosApp/**/*.pbxproj`, `iosApp/**/Package.swift`, `iosApp/**/*.xcconfig`, `iosApp/**/*.xctestplan`, `iosApp/.test-simulator` | `.pbxproj` руками не править; новые таргеты — `Package.swift` + umbrella `Features`; настройки — xcconfig (`https:/$()/host`); test plans — три; симулятор фиксирован; `.xcode-version` — источник истины | новое (02 §3) |
| 12 | `tests-pipeline` | `**/src/*Test*/**`, `**/commonTest/**`, `**/androidHostTest/**`, `iosApp/**/Tests/**`, `**/__Snapshots__/**`, `**/screenshots/**`, `e2e/**`, `.gate-fixtures/**` | пирамида: презентер (JVM, фикстуры) → Robolectric/Compose test v2 → Roborazzi/snapshot → Maestro (вне gate); фейки предпочтительнее MockK; те же сценарии `mock/`; полы покрытия; эталоны пишет стандартное окружение (06 §10.8); негативные фикстуры — часть тестов гейта | по форме |
| 13 | `api-contract` | `api/**`, `config/openapi/**`, `shared/**/core/api/**`, `shared/**/entity/**/*Dto*.kt` | `api/openapi.yaml` — копия контракта, правится только `make gen` из `../backend`; модели генерируются, клиенты пишутся руками над `ApiClient`; конверт `data`/`data+total`, `ErrorModel.code → MessageKey`, `X-Request-ID`, `Idempotency-Key` на мутациях, `Retry-After`; аддитивная эволюция + `ignoreUnknownKeys`; несовместимость — BACKEND-HANDOFF | новое (07 §2.6) |
| 14 | `auth-and-session` | `shared/**/core/auth/**`, `shared/**/entity/session/**`, `**/SecureStore*`, `**/Keychain*`, `androidApp/**/auth/**`, `iosApp/**/AuthFeature/**` | Bearer + refresh JSON; access в памяти (15 мин), refresh в `SecureStore` (90 д, ротация); single-flight refresh; logout = revoke + очистка; `AuthMode.Cookie` — только по `auth.md#mode`; биометрия = разблокировка ключа, не замена сессии; pinning выключен, шов задокументирован; удаление аккаунта — обязательный экран | новое (07 §1) |
| 15 | `signing-and-secrets` | `androidApp/app/build.gradle.kts`, `iosApp/**/*.entitlements`, `iosApp/App/Info.plist`, `iosApp/Config/**`, `scripts/release/**`, `.env.local.example`, `local.properties.example` | ключи только в Vaultwarden `mobile/<org>/<app>`; в репо — `*.example`; подпись из env при `make release-*`; upload-key ≠ app-signing-key, оба отпечатка в `assetlinks.json` (05 T-18); `ITSAppUsesNonExemptEncryption`; ATS без исключений; `cleartextTrafficPermitted=false` | новое (02/05) |
| 16 | `env-and-config` | `config/env/**`, `iosApp/Config/**`, `shared/**/core/config/**`, `androidApp/app/src/*/AndroidManifest.xml`, `.env.local.example` | три класса значений build-time / release-time / runtime (07 §4.2); флейворы `dev\|staging\|prod\|demo` = схемы; `AppConfig` собирается платформой и передаётся в `SharedContainer`; runtime-значения — `/api/app/requirements`; секреты не в build-time | по форме (`spa:` env-local) |
| 17 | `store-compliance` | `androidApp/app/src/main/AndroidManifest.xml`, `iosApp/App/Info.plist`, `iosApp/App/PrivacyInfo.xcprivacy`, `docs/stack/{distribution,compliance}.md`, `**/feature/auth/**`, `**/AuthFeature/**`, `**/legal/**` | App Review 4.8 (SiwA при стороннем логине), 5.1.1(v) удаление аккаунта, 3.1.3(c)/(e) оплата вне (companion), demo-доступ для ревью; Play: Data safety, targetSdk-дедлайны, developer verification; RuStore: модерация, 14 дней черновика; версии монотонны, отката нет | новое (05 §8.5) |
| 18 | `privacy-and-law` | `**/consent/**`, `**/legal/**`, `docs/stack/{compliance,dependencies}.md`, `**/PrivacyInfo.xcprivacy`, `gradle/libs.versions.toml` | 152-ФЗ: согласие до сбора, политика до логина, локализация ПДн; каждый SDK — запись в `dependencies.md` + privacy manifest; `ANALYTICS=none` по умолчанию; никаких `AD_ID`/ATT без записи; чек-лист L-1…L-17 (05 §5) | новое (05 §8.5) |
| 19 | `release-flow` | `scripts/release/**`, `Makefile`, `docs/stack/distribution.md`, `docs/reference/release.md`, `.gitea/**` | STOP-1/STOP-2 (§7); версия из тега; символы до публикации; `check-full` до тега; отклонение модерации → `Z+1`; STORE-HANDOFF; уведомление в ntfy | по форме (`go:` release) |
| 20 | `observability` | `shared/**/core/observability/**`, `androidApp/**/Sentry*`, `iosApp/**/Sentry*`, `**/*Logger*`, `**/logging/**` | `Log` фасад (Kermit / `os.Logger`); пустой DSN = выключено; PII-скраббинг; breadcrumbs по навигации и сети (без тел); `X-Request-ID` в событиях; mapping/dSYM загружаются релизом; `TelemetrySink` — шов | новое (07 §7) |
| 21 | `mock-and-fixtures` | `mock/**`, `shared/**/core/mock/**`, `androidApp/**/src/demo/**`, `iosApp/Config/Demo.xcconfig`, `e2e/**` | одна фикстурная база; сценарии `empty\|populated\|authenticated\|error`; `MockTransport` — production-код (demo-режим для App Review); фикстуры валидируются схемой спеки (`mock-check`); в тестах — те же сценарии; `demo` не ходит в сеть (тест) | новое (07 §3) |
| 22 | `deep-links-and-push` | `androidApp/app/src/main/AndroidManifest.xml`, `iosApp/**/*.entitlements`, `shared/**/core/{deeplink,push}/**`, `androidApp/core/platform/**/push/**`, `iosApp/**/Platform/**`, `docs/stack/{deep-links,push}.md` | таблица хостов/схем per env = `deep-links.md` (проверяет `deeplinks-check`); AASA/assetlinks — в `<org>/infra` (`wellknown`); push: data-first payload, `notification_id` dedup, регистрация `PUT /api/v1/devices`, провайдеры по `PUSH_PROVIDERS`; пред-запрос разрешения | новое (07 §5/§6) |
| 23 | `infra-impact` | `docs/stack/{deep-links,push,config,distribution,signing,observability}.md`, `api/openapi.lock`, `shared/**/core/api/**`, `shared/**/core/requirements/**` | что считается «касанием» соседей и какой блок печатать: INFRA-HANDOFF (`wellknown` env, DSN, хосты), BACKEND-HANDOFF (B-*: новые endpoint'ы/поля/auth), STRATEGY (расхождение с картой); формат WHAT/WHY/ACTION/WHO/DECIDE/RISK | идентично по форме (`go:`) |

### §3.3 `.claude/settings.json`

Основа — 06 §8.5 (allow без промптов на всё читающее/проверяющее и на детерминированные автофиксеры; deny на релизные артефакты, ключи, SDK-инсталляции, генерированное), плюс deny-ядро `go:`/`spa:`.

```json
{
  "$schema": "https://json.schemastore.org/claude-code-settings.json",
  "effortLevel": "max",
  "autoMemoryEnabled": false,
  "permissions": {
    "allow": [
      "Bash(make help)", "Bash(make doctor)", "Bash(make setup)", "Bash(make status*)",
      "Bash(make fmt)", "Bash(make fmt-check)", "Bash(make lint)", "Bash(make detekt)",
      "Bash(make test*)", "Bash(make screenshots)", "Bash(make kmp*)", "Bash(make gen-check)",
      "Bash(make api-check)", "Bash(make build)", "Bash(make cover)",
      "Bash(make check)", "Bash(make check-fast)", "Bash(make check-mac)", "Bash(make check-full)",
      "Bash(make gate-selftest)", "Bash(make mac-doctor)", "Bash(make map*)", "Bash(make deps*)",
      "Bash(make search*)", "Bash(make swift-*)",
      "Bash(./gradlew help*)", "Bash(./gradlew tasks*)", "Bash(./gradlew projects*)",
      "Bash(./gradlew dependencies*)", "Bash(./gradlew :*:dependencies*)",
      "Bash(./gradlew spotlessApply*)", "Bash(./gradlew spotlessCheck*)", "Bash(./gradlew detekt*)",
      "Bash(./gradlew lint*)", "Bash(./gradlew test*)", "Bash(./gradlew :*:test*)",
      "Bash(./gradlew *Roborazzi*)", "Bash(./gradlew kover*)", "Bash(./gradlew apiCheck*)",
      "Bash(./gradlew :*:assembleDevDebug*)", "Bash(./gradlew compileKotlin*)",
      "Bash(./gradlew :shared:*)", "Bash(./gradlew --stop)", "Bash(./gradlew --version)",
      "Bash(android sdk list*)", "Bash(android docs*)", "Bash(android describe*)",
      "Bash(android skills list*)", "Bash(android --version)",
      "Bash(adb devices)", "Bash(adb logcat -d*)", "Bash(adb shell dumpsys*)",
      "Bash(swift build*)", "Bash(swift test*)", "Bash(swift format*)", "Bash(swift package dump-package*)",
      "Bash(swiftlint lint*)", "Bash(swiftlint version)",
      "Bash(ssh * xcodebuild -version)",
      "Bash(rg *)", "Bash(grep *)", "Bash(find *)",
      "Bash(git status)", "Bash(git diff*)", "Bash(git log*)", "Bash(git show*)",
      "Bash(git branch*)", "Bash(git ls-files*)", "Bash(git rev-parse*)", "Bash(git tag -l*)",
      "Read(*)", "Glob(*)", "Grep(*)"
    ],
    "deny": [
      "Bash(*--no-verify*)",
      "Bash(git push --force*)", "Bash(git push -f*)", "Bash(git reset --hard*)", "Bash(git clean -f*)",
      "Bash(rm -rf *)", "Bash(pkill*)", "Bash(killall*)",
      "Bash(./gradlew assembleRelease*)", "Bash(./gradlew bundleRelease*)", "Bash(./gradlew *Release)",
      "Bash(./gradlew publish*)", "Bash(./gradlew apiDump*)", "Bash(keytool*)",
      "Bash(android sdk install*)", "Bash(sdkmanager*)", "Bash(android skills add*)",
      "Bash(bw *)", "Bash(xcrun altool*)", "Bash(fastlane*)", "Bash(ssh * fastlane*)",
      "Read(**/*.jks)", "Read(**/*.keystore)", "Read(**/*.p12)", "Read(**/*.p8)",
      "Read(**/*.mobileprovision)", "Read(.env.local)", "Read(local.properties)", "Read(iosApp/Config/Local.xcconfig)",
      "Edit(**/*.jks)", "Edit(**/*.keystore)", "Edit(**/*.p12)", "Edit(**/*.p8)",
      "Edit(**/build/**)", "Write(**/build/**)",
      "Edit(**/core/api/generated/**)", "Write(**/core/api/generated/**)",
      "Edit(shared/api/**)", "Write(shared/api/**)",
      "Edit(iosApp/**/*.pbxproj)", "Write(iosApp/**/*.pbxproj)",
      "Edit(gradle/wrapper/gradle-wrapper.properties)",
      "Edit(api/openapi.yaml)", "Write(api/openapi.yaml)",
      "Write(**/.claude/projects/**/memory/**)"
    ]
  }
}
```

Логика: `make release-*`, `make gen`, `make api-dump`, `make screenshots-record`, `make snapshot-record` — **ни в allow, ни в deny**: они мутируют артефакты, которые нужно осознанно принять, поэтому проходят через промпт. `apiDump` напрямую — deny (изменение фасада — решение в плане, выполняется вербом). `api/openapi.yaml` правится только `make gen` (копирование из соседа). `android skills add` — только через `make setup`. `bw` (Vaultwarden CLI), `altool`, `fastlane` — человек на Mac / релиз-скрипт с секретами из env, не агент.

### §3.4 Скиллы

| Скилл | Родство | Мобильная специфика |
|---|---|---|
| `/smartdev` | по форме (`go:`) | `make doctor` первым шагом (06 §8.7); `make status` (ниже); цепочка режимов дополнена «Mac-verify» и «release» |
| `/plan` | идентично | тип атома «Mac-verify» обязателен, если план трогает `iosApp/**` вне `Pure`; Done = `make check` (+ `check-mac` при iOS-атомах) |
| `/triage-failure` | по форме | каталог мобильных провалов (ниже) |
| `/init-service` | по форме (`spa:` 5 стадий) | стадии ниже; поля `platforms`, `preset`, `backend-path`, `auth.mode`; `--add-platform` |
| `/stack-deep-dive` | по форме (`go:`: режимы A/B/C, волны, 6 пунктов draft→stable) | 13 + 2 kinds, волны и cross-kind-инварианты §5.2 |
| `/de-template` | по форме (`spa:`: Gate → Phase A → Phase B, prune) | Gate дополнен `platforms`/пресетом/Mac-верификацией; `--platform`, `--keep`, prune-лист §2.7 |
| `/scaffold` | по форме (роутер по kinds strategy) | варианты ниже; выводит пути в трёх местах (shared / androidApp / iosApp) |
| `/release` | по форме (`go:`: preflight → steps) | `--platform android\|ios\|both`, `--stop 1\|2`, STORE-HANDOFF; §7 |
| `/infra-handoff` | идентично по форме | три адресата: `infra` (I-*), `backend` (B-*), `strategy` (S-*); блок WHAT/WHY/ACTION/WHO/DECIDE/RISK |

**`make status` (`scripts/tools/status.sh`, форма `go:` `status.sh`: секции `== … ==`, финал `OK`/`ATTENTION:`):**

```
== Branch ==        main · ahead 0 / behind 0 · last tag v1.4.2 (12 commits ago)
== Working tree ==  clean | 3 modified (shared/…, androidApp/feature/todos/…)
== Platforms ==     android ✓  ios ✓  preset: companion
== Mac ==           mac-mini.local: reachable · Xcode 27.0 ✓ | unreachable | MAC_HOST not set
== Critical ==      BUILD: ok | GEN: openapi.lock ok · backend drift: ../backend/api/openapi.yaml differs → make gen
                    API-DUMP: ok | KLIB: iosArm64 ok | STACK-DRAFTS: 2 (push, signing) | SCREENSHOTS: 1 stale
== Handoffs ==      INFRA pending: deep-links.md changed | BACKEND pending: B-17 | none
== Plan ==          active: «Push: RuStore provider» (3/7 atoms) | none
== Strategy ==      ../strategy: mobile/app/ — 9 kinds, 7 stable, 2 draft (entry-points, permissions) | not found
== Recent commits == …
== TODOs ==         …
ATTENTION: gen drift, 2 stack drafts, plan active
```

Секции `Critical` и `Handoffs` вычисляются дёшево (файлы и хэши, без Gradle): `GEN` — сравнение `openapi.lock` с `api/openapi.yaml` и с соседом; `API-DUMP` — `git diff --quiet shared/api`; `KLIB` — наличие каталога из последнего `make kmp`; `SCREENSHOTS` — `*_compare.png` в дереве; `Mac` — `ssh -o BatchMode=yes -o ConnectTimeout=3`.

**`/triage-failure` — каталог** (форма `go:`: симптом → причина → действие → ссылка): `OutOfMemoryError: Metaspace` → `gradle.properties` (06 L-02); `SKIPPED` у `link*Framework*` → это Linux, framework только на Mac (06 L-10), логика проверяется `make kmp`; `no such module 'SwiftUI'` → таргет не в `Pure`, проверять на Mac; `InaccessibleObjectException` в Robolectric → `--add-opens` (06 L-07); Roborazzi `*_compare.png` → осознанное `make screenshots-record` только если изменение намеренное; `Inconsistent JVM-target` (06 NF-08); Spotless → `make fmt`; `MissingApplicationIcon`/Lint → манифест; `apiCheck failed` → фасад изменился — решение в плане, затем `make api-dump`; `gen-check` drift → `make gen` + BACKEND-HANDOFF, если несовместимо; `swift format lint` vs SwiftLint конфликт → конфиг (06 L-03); `xcodebuild: Signing … requires a development team` → Mac-хост без профиля (`docs/reference/mac-host.md`); `Konsist` → матрица §2.6; `Hilt: [Dagger/MissingBinding]` → биндинг в `app`; K/N download на первом прогоне 66 с (06 §5) — не ошибка; `RuStore: 4xx на publish` → JWE-токен 900 с (05 §1.1).

**`/init-service` — стадии** (форма `spa:`): 0 preflight (`make doctor`, соседи `../strategy`, `../backend`, `../infra`); 1 identity (`project-slug`, имя приложения, `applicationId`/bundle id per env, `platforms`, `preset`, `auth.mode`, схема deep link); 2 env/config (хосты API per env, `SENTRY_DSN`-политика, `PUSH_PROVIDERS`, `ANALYTICS`, локали); 3 strategy-связка (`strategy-path`, `surface`; проверка, что `surface.md` содержит `type: mobile`; если слоя `mobile/` нет — печать S-1 и путь без PR §6.4); 4 stack-черновики (`docs/stack/<kind>.md` из `_templates` с подставленными значениями, `status: draft`); 5 handoffs (BACKEND-HANDOFF по наличию `GET /api/app/requirements`, `POST /api/auth/refresh`, `PUT /api/v1/devices` в `../backend/api/openapi.yaml` — если отсутствуют, печатает B-*; INFRA-HANDOFF для `wellknown`); 6 next-step (`/stack-deep-dive`, затем `/de-template`). `--add-platform ios|android` — восстановление половины из upstream-ветки шаблона (`[spike]`).

**`/scaffold` — варианты** (роутер по kind → путь; читает stable-строку из strategy, `--from-args` с предупреждением):

| Вариант | Читает | Создаёт |
|---|---|---|
| `entity <slug>` | `entities.md` | `shared/…/entity/<slug>/{Model,Repository,Dto}.kt` + `commonTest` + `mock/fixtures/*/<slug>.json` + `MockTransport`-маршрут |
| `feature <slug>` | `features.md` (`shared`, `screens`) | `shared/…/feature/<slug>/{Presenter,State,Intent,UseCase}.kt` + тест; при `shared: false` — только платформенные заготовки |
| `screen <Slug> --feature <slug>` | `screens.md` (`states`, `entry`) | `androidApp/feature/<slug>/{<Slug>Screen,<Slug>ViewModel,<Slug>NavEntry}.kt` + превью на каждое состояние + Roborazzi-тест; `iosApp/Packages/Features/Sources/<Slug>Feature/{<Slug>View,<Slug>Model}.swift` + snapshot-тест; `Route.<Slug>` |
| `flow <slug>` | `flows.md` | `e2e/common/<slug>.yaml` (Maestro) + сценарный тест презентеров |
| `entry-point <slug>` | `entry-points.md` | deep-link-маршрут в `DeepLinkParser` + intent-filter/AASA-путь + строка `deep-links.md` / push-категория + строка `push.md` |
| `permission <slug>` | `permissions.md` | пред-запрос (rationale-экран), запись в манифест / `Info.plist` usage-description, строка `compliance.md` |
| `api <resource>` | `shared-api.md` | `shared/…/core/api/<Resource>Api.kt` над сгенерированными моделями + фикстуры |
| `stack <kind>` / `rule <name>` / `skill <name>` | — | из `_templates` |

**`/de-template` — Gate**: все обязательные kinds `stable`; условные по пресету `stable`; слой `mobile/<surface>/` в strategy — все 9 kinds `stable` (или `--no-strategy` с записью в roadmap); `make check` зелёный; при `ios ∈ platforms` — `make check-mac` зелёный или явный `--ios-unverified` (записывается в `.claude/plans/roadmap.md` как долг). **Phase A** (идентичность): пакет / `applicationId` / bundle id / product name / team (xcconfig), схема deep link, имя `Shared`-фреймворка не меняется, `README`, лицензия, `--platform`-обрезка (§2.8). **Phase B** (prune §2.7 + docs): удаление `profile`/`todos`, чистка фикстур/строк/снапшотов/e2e, `docs/reference/strategy-mapping.md`, примеры в `docs/stack`, пресетные kinds. **Verify**: `make check`, `make gate-selftest`, `make check-mac` (если iOS), `make kin-diff` (advisory, §5.3).

### §3.5 `AGENTS.md`, официальные Android-скиллы, агенты

- **`AGENTS.md`** — файл из ≤ 10 строк: «Инструкции для агентов — `CLAUDE.md`; правила — `.claude/rules/`; процедуры — `.claude/skills/`; гейт — `make check`». Причина: JetBrains/Kotlin-документация и Xcode 27 (ACP-агенты) читают `AGENTS.md` (04 §5, 06 §7), но единственный источник — `CLAUDE.md` (forward-only, без дублей). Три критерия KMP-приёмки из kotlinlang «AI for development» (тесты по таргетам, сборка всех таргетов, platform-API не течёт в common) — в инвариантах `CLAUDE.md`, не в `AGENTS.md`.
- **Официальные Android-скиллы** (`android skills add`, 01 §6) — **не вендорятся**: `make setup` вызывает `scripts/tools/android-skills.sh`, который ставит закреплённый список (`compose`, `navigation-3`, `testing`, `edge-to-edge`, `agp-9-upgrade`, `r8-analyzer`) в `.claude/skills/android/` (каталог в `.gitignore`); `[spike]` — поддерживает ли CLI целевой каталог, иначе список имён в `.gitignore`. Обновление — `make setup` повторно. `android` CLI везде с `--no-metrics` (`~/.androidrc` пишет `make setup`, 06 §8.7). Roborazzi-скилл (`npx skills add takahirom/roborazzi`) — не ставится (Node-зависимость), ссылка в `docs/reference/architecture.md`.
- **Apple-инструменты Xcode 27** (MCP/ACP, 02 §6) — только на Mac-хосте, к репо не относятся; `docs/reference/mac-host.md` описывает, как человек на Mac подключает агента к тому же `CLAUDE.md`.
- **Агенты** `.claude/agents/{explorer,reviewer}.md` — как в `go:`; `reviewer` дополнен чек-листом: фасад плоский, логика в shared, состояние экрана покрыто превью/снапшотом, права/PII записаны в `compliance.md`.

---
## §4 Гейт

Принципы — те же, что у `go:`: один верб `make check`, шаги печатают одну строку `OK:`/`FAIL:` с временем и путём к логу, гейт **не** fail-fast (агенту нужен весь список провалов за один прогон — 06 §8.1), негативные фикстуры доказывают, что гейт умеет краснеть. Мобильная специфика: часть стадий выполнима только на macOS — гейт **не изображает зелёный** (06 §8.3), а печатает явный список невыполненных стадий и не считает их пройденными.

### §4.1 `Makefile` — словарь вербов

```makefile
.PHONY: help setup doctor fmt fmt-check detekt lint test screenshots screenshots-record cover \
        kmp api-dump api-check gen gen-check gen-tokens swift-fmt swift-lint swift-test \
        build check check-fast check-mac check-full gate-selftest e2e run-android run-ios \
        mac-doctor mac-build mac-test snapshot-record release-preflight release-android \
        release-ios release-publish status map deps deps-report search clean

help: ## Сгруппированный список команд (##@ группы — как в go-backend-template)
	@awk 'BEGIN {FS = ":.*## "} /^##@ / {printf "\n\033[1m%s\033[0m\n", substr($$0, 5); next} \
	  /^[a-zA-Z0-9_-]+:.*## / {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

##@ LOOP
fmt:                ## Автоформат Kotlin (Spotless/ktlint) + Swift (swift-format) — единственные автофиксеры
	./gradlew spotlessApply --console=plain -q
	@[ -d iosApp ] && swift format --in-place --recursive iosApp/App iosApp/Packages iosApp/Tools || true
fmt-check:          ## Проверка формата без мутации дерева
	@bash scripts/tools/gradle-step.sh "format kotlin" spotlessCheck
	@[ -d iosApp ] && bash scripts/tools/swift-step.sh "format swift" swift format lint --strict --recursive iosApp/App iosApp/Packages iosApp/Tools || true
detekt:             ## Статический анализ Kotlin + compose-rules (без detekt-formatting)
	@bash scripts/tools/gradle-step.sh "detekt" detekt
lint:               ## Android Lint (warningsAsErrors, checkDependencies) по всем модулям
	@bash scripts/tools/gradle-step.sh "android lint" lintDevDebug
test:               ## JVM-тесты: shared (jvmTest), Android (Robolectric, Konsist, FixturesTest), Swift Pure
	@bash scripts/tools/gradle-step.sh "shared jvm tests" :shared:jvmTest
	@bash scripts/tools/gradle-step.sh "android unit tests" testDevDebugUnitTest
	@[ -d iosApp ] && bash scripts/tools/swift-step.sh "swift pure tests" swift test --package-path iosApp/Packages/Pure || true
screenshots:        ## Roborazzi verify (без устройства)
	@bash scripts/tools/gradle-step.sh "screenshots" verifyRoborazziDevDebug
screenshots-record: ## Перезаписать эталоны Roborazzi (осознанно; проходит через промпт агента)
	./gradlew recordRoborazziDevDebug --console=plain -q
cover:              ## Отчёты покрытия + проверка полов (shared 80 / android 70 / swift pure 70)
	@bash scripts/tools/gradle-step.sh "coverage kotlin" koverVerify koverXmlReport
	@[ -d iosApp ] && bash scripts/tools/coverage-floor.sh iosApp/Packages/Pure 70 || true
check-fast:         ## Цикл «пока пишу»: формат + detekt + shared jvmTest + swiftlint (< 30 с тёплый)
	@FAST=1 bash scripts/tools/check.sh

##@ KMP
kmp:                ## shared → klib под iOS-таргеты + проверка каталога (SKIPPED ≠ OK) + запрет cinterop
	@bash scripts/tools/kmp-check.sh
api-dump:           ## Обновить дампы публичного API shared (JVM + klib) — изменение фасада, решение в плане
	./gradlew :shared:apiDump --console=plain -q
api-check:          ## Сверить публичный API shared с дампами
	@bash scripts/tools/gradle-step.sh "shared api" :shared:apiCheck

##@ CONTRACT
gen:                ## Скопировать api/openapi.yaml из ../backend, сгенерировать модели, обновить openapi.lock
	@bash scripts/tools/gen.sh
gen-check:          ## Модели == регенерация; lock == спека; дрейф от соседа — WARN
	@bash scripts/tools/gen-check.sh
gen-tokens:         ## design/tokens.json → Tokens.kt + Tokens.swift (коммитятся; tokens-check сверяет)
	./gradlew :build-logic:convention:generateTokens --console=plain -q

##@ SWIFT (Linux-часть)
swift-lint:         ## SwiftLint --strict + archlint (граф Package.swift)
	@bash scripts/tools/swift-step.sh "swiftlint" swiftlint lint --strict --quiet --config iosApp/.swiftlint.yml iosApp
	@bash scripts/tools/swift-step.sh "archlint" swift run --package-path iosApp/Tools/archlint archlint iosApp/Packages
swift-test:         ## swift test чистых пакетов (Pure)
	@bash scripts/tools/swift-step.sh "swift pure tests" swift test --package-path iosApp/Packages/Pure

##@ GATE
build:              ## Debug APK (dev flavor)
	@bash scripts/tools/gradle-step.sh "assembleDevDebug" :androidApp:app:assembleDevDebug
check:              ## Канонический gate, Linux-полный. GATE_RELEASE=1 добавит assembleProdRelease (R8)
	@bash scripts/tools/check.sh
check-mac:          ## macOS-стадии по SSH (MAC_HOST): link framework, iosSimulatorArm64Test, xcodebuild, планы Unit+Snapshot
	@bash scripts/tools/mac.sh check
check-full:         ## check + check-mac; обязателен перед /release
	@bash scripts/tools/check.sh && bash scripts/tools/mac.sh check
gate-selftest:      ## Негативные фикстуры .gate-fixtures/: каждая обязана уронить свой шаг
	@bash scripts/tools/gate-selftest.sh
e2e:                ## Maestro-флоу (нужен эмулятор/устройство; P=android|ios)
	@bash scripts/tools/e2e.sh $(P)

##@ RUN
run-android:        ## Установить devDebug на подключённый эмулятор/устройство
	./gradlew :androidApp:app:installDevDebug --console=plain -q && adb shell monkey -p $$(bash scripts/tools/app-id.sh dev) 1
run-ios:            ## (Mac) собрать и запустить схему Dev на симуляторе из .test-simulator
	@bash scripts/tools/mac.sh run

##@ MAC
mac-doctor:         ## Доступность MAC_HOST, версия Xcode == .xcode-version, симулятор из .test-simulator
	@bash scripts/tools/mac.sh doctor
mac-build:          ## xcodebuild build (generic/platform=iOS)
	@bash scripts/tools/mac.sh build
mac-test:           ## xcodebuild test — планы Unit + Snapshot (+ UI при MAC_UI=1)
	@bash scripts/tools/mac.sh test
snapshot-record:    ## (Mac) перезаписать snapshot-эталоны
	@bash scripts/tools/mac.sh snapshot-record

##@ RELEASE (оркестрирует /release; секреты — из env, см. docs/reference/release.md)
release-preflight:  ## check-full + версия из тега + символы + чек-листы compliance
	@bash scripts/release/preflight.sh $(TAG)
release-android:    ## Подписанные AAB + APK + mapping.txt → dist/
	@bash scripts/release/build-android.sh $(TAG)
release-ios:        ## (Mac) архив + экспорт IPA + dSYM.zip → dist/
	@bash scripts/release/build-ios.sh $(TAG)
release-publish:    ## STOP-1: Gitea Release + ассеты; STOP-2 при CHANNELS=play,rustore,testflight
	@bash scripts/release/publish.sh $(TAG)

##@ INTEL
status:             ## Сводка для /smartdev
	@bash scripts/tools/status.sh
map:                ## Модули и таски
	@./gradlew projects --console=plain -q | sed 's/^/  /'
deps:               ## Дерево зависимостей (M=:androidApp:feature:todos C=devDebugRuntimeClasspath)
	@./gradlew $(if $(M),$(M):,)dependencies $(if $(C),--configuration $(C),) --console=plain -q
deps-report:        ## dependency-analysis buildHealth — advisory, вне гейта (не поддерживает AGP 9.4)
	./gradlew buildHealth --console=plain -q || true
search:             ## Поиск по коду (Q=pattern [P=path])
	@rg -n --max-count=80 -- "$(Q)" $(if $(P),$(P),.)

##@ SETUP
setup:              ## doctor + git-хуки + Android-скиллы + прогрев демона
	@bash scripts/tools/doctor.sh
	git config core.hooksPath scripts/git-hooks
	@bash scripts/tools/android-skills.sh
	./gradlew help --console=plain -q
doctor:             ## Fail-fast preflight: JDK 21, SDK 37, Swift 6.4, RAM/диск, MAC_HOST
	@bash scripts/tools/doctor.sh
clean:              ## Остановить демоны, снести build/ и .build/
	./gradlew --stop || true
	./gradlew clean --console=plain -q || true
	rm -rf iosApp/Packages/Pure/.build iosApp/Tools/archlint/.build
```

Все `[ -d iosApp ] && … || true` исчезают при `/de-template --platform android` (скилл переписывает `Makefile` без Swift/Mac-групп); при `--platform ios` исчезают Android-группы.

### §4.2 Состав `make check` (Linux-полный)

Времена — из 06 §3 (тёплый / холодный); Swift — из 06 §6; для стадий, не измеренных 06, стоит `[spike]`.

| # | Стадия | Что запускает | Тёплое / холодное | Что ловит |
|---|---|---|---|---|
| 1 | `format kotlin` | `spotlessCheck` | 1–2 с / 14 с | формат (самая частая поломка — первая и дешёвая) |
| 2 | `format swift` | `swift format lint --strict` | 0.03 с | формат Swift |
| 3 | `detekt` | `detekt` (type-resolution off) | ~1 с / 1 с (первый — 142 с скачивание) | стиль, сложность, `ForbiddenImport`, Compose-правила |
| 4 | `android lint` | `lintDevDebug` | 0–7 с / 44 с | платформенные ошибки, `checkDependencies`, проектные детекторы |
| 5 | `shared jvm tests` | `:shared:jvmTest` | секунды `[spike]` | вся общая логика: презентеры, парсеры, auth, `FixturesTest` (каждая фикстура десериализуется строгими сгенерированными моделями — это `mock-check`) |
| 6 | `android unit tests` | `testDevDebugUnitTest` | 1–16 с / 16 с | Robolectric + Compose test v2, **Konsist-архитектура**, `DeepLink ↔ NavKey` |
| 7 | `screenshots` | `verifyRoborazziDevDebug` | 1–12 с / 12 с | визуальные регрессии всех состояний экранов |
| 8 | `coverage kotlin` | `koverVerify` (shared 80 / android 70) | 1–5 с / 5 с | пол покрытия |
| 9 | `kmp` | `kmp-check.sh`: `compileKotlinIosArm64` + `compileKotlinIosSimulatorArm64` + проверка `shared/build/classes/kotlin/iosArm64/main/klib` + запрет `cinterops {`/`native.cocoapods` | `[spike]` тёплое; первый прогон 66 с (K/N download, 06 §5) | `expect/actual`, platform API в common, зависимости без iOS-таргета |
| 10 | `shared api` | `:shared:apiCheck` (BCV JVM + klib) | `[spike]` | несанкционированное изменение фасада для Swift |
| 11 | `assembleDevDebug` | `:androidApp:app:assembleDevDebug` | 1–2 с / 48 с | «оно вообще собирается» |
| 12 | `assembleProdRelease` (только `GATE_RELEASE=1`) | `:androidApp:app:assembleProdRelease` (подпись — из env, иначе debug-ключ с предупреждением) | +25 с / 58 с | R8/Proguard: reflection, Hilt, serialization (06 §8.1) |
| 13 | `swiftlint` | `swiftlint lint --strict` | 0.18 с | стиль Swift, custom-правила границ (§2.6) |
| 14 | `archlint` | `swift run archlint` над `dump-package` | ~1 с `[spike]` | граф пакетов iOS |
| 15 | `swift pure build+tests` | `swift build` + `swift test` в `Packages/Pure` | 4.1 с холодная / 0.72 с | `AppFoundation`, `DesignSystemTokens` |
| 16 | `coverage swift` | `coverage-floor.sh Pure 70` | ~1 с | пол покрытия чистых пакетов |
| 17 | `gen-check` | `gen-check.sh` | ~3 с `[spike]` | правленные руками модели, устаревший lock; дрейф от `../backend` — **WARN**, не FAIL |
| 18 | `tokens-check` | `generateTokens` + `git diff --exit-code` на `Tokens.kt`/`Tokens.swift` | ~1 с | расхождение токенов между платформами и `design/tokens.json` |
| 19 | `i18n-check` | `i18n-check.sh` | < 1 с | ключи `values/strings.xml` ↔ `values-ru/` ↔ `Localizable.xcstrings` ↔ `MessageKey`-реестр |
| 20 | `deeplinks-check` | `deeplinks-check.sh` | < 1 с | `docs/stack/deep-links.md` ↔ intent-filters ↔ `applinks:` в entitlements ↔ тесты `DeepLinkParser` |
| 21 | `sort-dependencies` | `checkSortDependencies` | ~1 с | порядок зависимостей |
| — | **итог** | | тёплый без изменений — ~10 с; один файл — ~40 с; свежий клон — ~2–3 мин `[spike]` (06: 4.2 с / 37 с / 94 с без Swift/KMP/contract-стадий) | |

Порядок стадий — от дешёвых к дорогим и от «формат» к «сборка»; Swift-стадии идут после Kotlin, чтобы не ждать Gradle ради опечатки в Swift — при `FAST=1` порядок обратный (Swift и detekt первыми).

**Хвост вывода на Linux** (стадии macOS — не «пропущены молча», а перечислены):

```
OK:   format kotlin            2s
…
OK:   sort-dependencies        1s
— macOS-стадии (не выполнены на этом хосте) —
SKIP: shared link framework      → make check-mac
SKIP: shared iosSimulatorArm64Test
SKIP: xcodebuild build
SKIP: test plan Unit
SKIP: test plan Snapshot
✓ check clean (Linux-часть) · macOS-стадий не выполнено: 5 · последний check-mac: 2026-09-16 14:02 (git 3f1c2a) | never
```

Код возврата 0. Отметка «последний check-mac» читается из `build/gate-logs/mac.last` (пишет `mac.sh`), чтобы `/smartdev` и `/release` видели, насколько устарела macOS-верификация; `/release` требует `check-full` на текущем HEAD.

Вне гейта (отдельные вербы): `connectedAndroidTest`/GMD (нужен KVM/устройство — 06 §1), `make e2e` (Maestro), `deps-report` (`buildHealth`, 06 L-05), `swiftlint analyze` (Mac), Baseline Profile.

### §4.3 KMP-стадии: SKIPPED-ловушка, `apiCheck`, cinterop — разрешение противоречия (д)

- **SKIPPED exit 0.** На Linux `linkDebugFramework*` завершается `SKIPPED` с кодом 0 (06 L-10); поэтому `kmp-check.sh` проверяет **артефакт на диске** (`shared/build/classes/kotlin/iosArm64/main/klib`, 06 §8.4) и падает, если каталога нет. `check-mac` делает то же с `shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework`. Ни одна стадия гейта не доверяет одному exit-коду Gradle для Apple-таргетов.
- **`iosSimulatorArm64Test` на Linux** отключён с явным сообщением (06 §5), а не «зелёным» — стадия перечислена в блоке SKIP; `kotlin.native.ignoreDisabledTargets=true` убирает шум предупреждений (06 §8.2).
- **`apiCheck` (BCV 0.18.2).** JVM-дамп `shared.api` проверяется на Linux всегда. Klib-дамп `shared.klib.api`: на Linux BCV не валидирует таргеты, «не поддерживаемые хостом», и **выводит** дамп для них из доступных (README BCV); `[spike]` S-2 — считает ли BCV `iosArm64` поддерживаемым на Linux при Kotlin 2.4 (компиляция klib работает — 06 §5). Если да — `strictValidation = false` на Linux, `= true` в `check-mac`, дампы регенерируются `make api-dump` (Linux) и сверяются на Mac. Если нет — klib-дамп проверяется только в `check-mac`, на Linux — JVM-дамп + Konsist-правило «публичные сигнатуры без generic'ов» как приближение.
- **cinterop/CocoaPods запрещены** структурно (`kmp-check.sh` grep по `shared/build.gradle.kts`), потому что ломают Linux-компиляцию Apple-klib (03 §8, 06 §5). Всё платформенное — через интерфейсы `PlatformDeps` (§2.2).
- **`dependency-analysis` вне гейта** до поддержки AGP 9.4 (06 L-05) — verb `deps-report`, advisory; в `.claude/plans/roadmap.md` шаблона — backlog-строка «вернуть `buildHealth` в гейт при поддержке AGP 9.x».

### §4.4 `check-mac` — состав и контракт с Mac-хостом

`scripts/tools/mac.sh` (06 §8.3): `rsync -a --delete --exclude build --exclude .build --exclude .gradle ./ "$MAC_HOST:~/ci/<slug>/"` → `ssh` с `set -o pipefail` и `xcbeautify` → обратно `*.xcresult`, `*.ipa`, snapshot-diff'ы → разбор `xcrun xcresulttool get test-results summary --format json`. Стадии:

| # | Стадия | Команда на Mac | Проверка |
|---|---|---|---|
| M1 | `mac doctor` | `xcodebuild -version`, `xcrun simctl list` | Xcode == `.xcode-version`; симулятор из `.test-simulator` существует |
| M2 | `shared link framework` | `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` | каталог `Shared.framework` существует |
| M3 | `shared ios tests` | `./gradlew :shared:iosSimulatorArm64Test` | rc + отчёт |
| M4 | `shared api (strict)` | `./gradlew :shared:apiCheck` с `strictValidation = true` | klib-дамп совпадает |
| M5 | `xcodebuild build` | `xcodebuild build -project iosApp/iosApp.xcodeproj -scheme Dev -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO` | rc |
| M6 | `test plan Unit` | `xcodebuild test -testPlan Unit -destination "$(cat .test-simulator)"` | xcresult summary: failures == 0 |
| M7 | `test plan Snapshot` | `-testPlan Snapshot` | failures == 0; diff-файлы копируются обратно |
| M8 (opt) | `test plan UI` (`MAC_UI=1`) | `-testPlan UI` | XCUITest smoke |
| M9 (opt) | `swiftlint analyze` | компиляционный лог → analyze | неиспользуемые импорты и т. п. (02 §6) |

Поведение при отсутствии Mac: `make check` — SKIP-блок и код 0 (Linux-цикл не должен ломаться из-за отсутствия Mac — иначе агент перестанет его запускать); `make check-full` — FAIL «`MAC_HOST` не задан / недоступен» (06 §8.3, `MAC_REQUIRED` не нужен: разделение на два верба делает флаг лишним). `/release --platform ios|both` требует `check-full`. `docs/reference/mac-host.md` описывает Mac mini как build-host экосистемы (05 §6): SSH-ключ, Tailscale, `~/ci/<slug>/`, установленные Xcode/simulators, fastlane match для подписи, ротация.

### §4.5 Хуки и обёртки

- **`scripts/git-hooks/commit-msg`** — Conventional Commits (тот же скрипт, что в `go:`).
- **`pre-commit`** — только проверки, без мутаций: `spotlessCheck` (1–2 с) + `swift format lint --strict` на staged `.swift` + запрет staged `*.jks|*.p12|*.p8|*.mobileprovision|local.properties|Local.xcconfig|.env.local`. Автоформат — руками `make fmt` (правило «хуки не меняют дерево» — как `go:`).
- **`pre-push`** — `GATE_RELEASE=1 make check` (06 §10.3: R8-проверка стоит +25 с — уместна на push, не на каждом ручном прогоне). `check-mac` в pre-push **не** входит (SSH-зависимость в хуке недопустима); Mac-верификация — обязанность `/plan`-атома «Mac-verify» и `/release`.
- **`gradle-step.sh`** (06 §4): `./gradlew <tasks> --console=plain -q` → `build/gate-logs/<step>.log`; `OK:   <step> <t>s` / `FAIL: <step> (<log>)` + последние строки лога с `e: `/`error:`/`FAILED`. **`swift-step.sh`** — то же для Swift-команд. **`check.sh`** — агрегатор без fail-fast, секции, SKIP-блок, `FAST=1`. **`doctor.sh`** — JDK 21 (toolchain), `ANDROID_HOME` + `platforms;android-37`, лицензии, свободная RAM (FAIL < 4 ГБ, WARN < 6 ГБ; эмулятор — 10+), диск (WARN < 15 ГБ), Swift 6.4 в `PATH`, `swiftlint`, `MAC_HOST` (если задан) + версия Xcode, KVM (инфо). **`android-skills.sh`** — §3.5.

### §4.6 Один форматер на язык — разрешение противоречия (д)

| Язык | Автофиксер | Линтер | Конфликт, закрытый конфигом |
|---|---|---|---|
| Kotlin | Spotless 8.10.2 + ktlint 1.8.0 (`.editorconfig` с ключами 06 §2.9 — `ktlint_function_naming_ignore_when_annotated_with = Composable`, `ktlint_standard_class-signature = disabled`) | detekt 1.23.8 **без** `detekt-formatting` (внутри ktlint 0.50 — 06 L-01) | detekt не судит формат |
| Swift | swift-format (Swift 6.4, `.swift-format`: 2 пробела, 02 §7.5) | SwiftLint 0.65.1 `--strict` с `trailing_comma: disabled` (06 L-03) и custom-правилами границ | SwiftLint не судит запятые/отступы |

SwiftFormat (nicklockwood) и Periphery в шаблон не входят (02 §6, архив Periphery). Baseline-файлов нет ни у Lint, ни у detekt, ни у SwiftLint (06 L-13, 06 §8.7).

### §4.7 Негативные фикстуры и `make gate-selftest`

`.gate-fixtures/<NF-id>/` содержит `patch.diff` (или `apply.sh`) и `expect` (имя стадии, которая обязана упасть). `gate-selftest.sh`: для каждой фикстуры — применить, запустить **только** ожидаемую стадию, потребовать `rc ≠ 0`, откатить (`git checkout -- . && git clean -fd -- <paths>` в пределах фикстуры). Прогон ~3–5 мин; входит в Verify `/de-template` и в `/release --stop 1` preflight шаблона (не форка), плюс ручной верб.

| ID | Что подкладывается | Кто ловит | Источник |
|---|---|---|---|
| NF-01 | `import android.util.Log` | detekt `ForbiddenImport` | 06 §8.6 ✅ |
| NF-02 | `feature:other` импортирует `feature:todos` | Konsist + `stit.android.feature` | 06 ✅ |
| NF-03 | строка не по ktlint | Spotless | 06 ✅ |
| NF-04 | неиспользуемая `private fun` | detekt `UnusedPrivateMember` | 06 ✅ |
| NF-05 | манифест без `android:icon` | Lint `MissingApplicationIcon` | 06 ✅ |
| NF-06 | сырой `0xFF…` в композабле | detekt `MagicNumber` + `DesignSystemDetector` | 06 ✅ |
| NF-07 | неверный ключ в `detekt.yml` | detekt config validation | 06 ✅ |
| NF-08 | jvmTarget 21 vs 17 | KGP | 06 ✅ |
| NF-09 | смена цвета токена без `screenshots-record` | Roborazzi verify | 06 (механика подтверждена) |
| NF-10 | удалён тест презентера | `koverVerify` | 06 |
| NF-11 | `java.io.File` в `commonMain` | `compileKotlinIosArm64` | новое |
| NF-12 | `cinterops { }` в `shared/build.gradle.kts` | `kmp-check.sh` | новое |
| NF-13 | публичный `fun <T> …` в `feature/*Presenter` | Konsist (и `apiCheck`) | новое |
| NF-14 | изменена сигнатура фасада без `api-dump` | `apiCheck` | новое `[spike]` |
| NF-15 | правка файла в `core/api/generated/` | `gen-check` | новое |
| NF-16 | фикстура с полем не по схеме | `FixturesTest` (`:shared:jvmTest`) | новое |
| NF-17 | ключ строки только в `values/` | `i18n-check` | новое |
| NF-18 | хост в `deep-links.md` без intent-filter | `deeplinks-check` | новое |
| NF-19 | токен добавлен только в `Tokens.kt` | `tokens-check` | новое |
| NF-20 | `print(` в Swift | SwiftLint | новое (06 §6 механика) |
| NF-21 | `import TodosFeature` внутри `AuthFeature` | SwiftLint custom + archlint | новое |
| NF-22 | `import UIKit` в `Packages/Pure` | `swift build` (Linux: `no such module`) | новое (06 L-16) |
| NF-23 | `URLSession(` в `Features` | SwiftLint custom | новое |
| NF-24 | `@preconcurrency` без комментария | SwiftLint custom regex | новое |

### §4.8 Полы покрытия

| Область | Пол | Инструмент | Исключения |
|---|---|---|---|
| `shared` (commonTest через jvmTest) | **80 % LINE** | Kover `koverVerify` на `:shared` | `core/api/generated/**`, `*$serializer*` |
| `androidApp` агрегат | **70 % LINE** (06 §8.1) | Kover merged | `*Hilt_*`, `*_Factory*`, `ComposableSingletons*`, `*Preview*`, `BuildConfig`, `*NavEntry*` (покрыты скриншотами) |
| Swift `Pure` | **70 % LINE** | `llvm-cov` JSON (`coverage-floor.sh`, 06 §6) | — |
| Swift `Core`/`Features` (Mac) | без пола; отчёт в `check-mac` | xcresult | UI закрывается snapshot-тестами |

Разные полы — потому что логика намеренно сосредоточена в `shared` (§2.2): там пол выше Go-шаблонного 75, UI-слои — ниже, их качество держат Roborazzi/snapshot. Вопрос владельцу — §10.

### §4.9 Требования к машине агента

Из 06 §1/§3: свободной RAM ≥ 6 ГБ (демоны Gradle + Kotlin ≈ 4.8 ГБ RSS; `MaxMetaspaceSize=1g` обязателен, иначе Lint падает — 06 L-02), 10+ ГБ с эмулятором; диск: Android SDK + Gradle-кэши ≈ 9.5 ГБ, K/N-тулчейн и кэш ≈ 1–3 ГБ (06 §5 фиксирует только время загрузки), Swift-тулчейн ≈ 1.5 ГБ; `gradle.properties` — 06 §8.2 (`-Xmx2g`, `workers.max=4`, configuration-cache, `HeapDumpPath=build/`, `ignoreDisabledTargets`); `*.hprof` в `.gitignore`; `make clean` останавливает демоны (06 §8.7). `doctor.sh` печатает всё это первым делом в `/smartdev`.

---
## §5 `docs/` и родство с Go/SPA/bot

### §5.1 Состав `docs/`

| Файл | Роль | Родство |
|---|---|---|
| `docs/README.md` | карта документации (роутер): что где, для кого, порядок чтения | по форме (`go:`/`spa:`) |
| `docs/getting-started.md` `[META]` | путь потребителя: 0 предусловия (Linux-машина; Mac — для iOS; аккаунты RuStore / Play / Apple — сроки из 05 §1) → 1 форк в `<org>/mobile` → 2 `make setup` → 3 `/smartdev` → `/init-service` → 4 карта `mobile/<surface>/` в `<org>/strategy` (или путь без PR) → 5 `/stack-deep-dive` → 6 `/de-template` → 7 handoffs (backend B-*, infra I-*) → 8 первый `/release --stop 1` | по форме (`go:` 0–7) |
| `docs/contributing.md` `[META]` | таблица «правило → кто enforce'ит» (Konsist / detekt / Lint / SwiftLint / archlint / скрипт гейта / человек); как добавить модуль, фичу, kind, правило; как обновлять toolchain'ы (Kotlin/AGP/Xcode) | по форме |
| `docs/reference/architecture.md` | слои и матрица §2.6, презентер-паттерн, фасад и SKIE, reference-фичи (`#reference`), выбранные ловушки 01/02/03/06/07 (`#traps`), где что лежит | по форме |
| `docs/reference/ecosystem.md` | «Где это в экосистеме» развёрнуто: 7 шаблонов + библиотеки, контракт artifact-in-release, соседи и sibling-checkout, триггер извлечения kit-библиотек (§1.6), что мобильный репо не делает (образ, compose) | по форме |
| `docs/reference/release.md` | STOP-1/STOP-2, версия из тега, вербы `release-*`, секреты из env/Vaultwarden, символы, ntfy, отклонение модерации | по форме (`go:` release) |
| `docs/reference/distribution.md` | факты магазинов (05 §1 в сжатом виде), чек-листы L-1…L-17, аккаунты и сроки (D-U-N-S, verification), enterprise-канал | новое |
| `docs/reference/observability.md` | Sentry/GlitchTip: DSN per env, mapping/dSYM, PII, breadcrumbs, `TelemetrySink` | по форме (`spa:`) |
| `docs/reference/strategy-mapping.md` | kinds `mobile/<surface>/` → пути в трёх местах кода; таблица reference-соответствий (очищается `/de-template`) | по форме (`go:` domain-mapping) |
| `docs/reference/mac-host.md` | Mac build-host: SSH/Tailscale, `~/ci/<slug>/`, Xcode/simulators, fastlane match, ротация, Xcode Cloud как резерв (05 §6) | новое |
| `docs/reference/kinship.md` | таблица родства (§5.3) + `make kin-diff` | по форме (SSR-13) |
| `docs/reference/strategy-mobile-canon/` | копия канона слоя `mobile/` (README + 9 форм) для strategy-форков без него (§6.4) | новое |
| `docs/stack/README.md` + `_templates/<kind>.md` | реестр kinds (§5.2), форма frontmatter, волны, cross-kind-инварианты | по форме |

Доки, которых **нет**: `deployment.md` (нет образа/nginx), `CHANGELOG.md` (forward-only), отдельных «канонов» платформ (их роль у отчётов 01/02 — они остаются в `docs/research/` шаблона как источники, а не как документация потребителя).

### §5.2 Stack-kinds — итоговый набор (разрешение противоречия (в))

Входы: 02 §7.10 (9 kinds, включая `navigation` и `design`), 05 §8.6 (`distribution`, `signing`, `compliance`), 07 §10.2 (10 + `dependencies`, `styling` вместо `design`, `deep-links`/`push`/`auth` отдельно), экосистемная восьмёрка `go:`/`spa:`. Решения: `navigation` **не kind** — платформенные аспекты навигации константны для шаблона (`architecture.md`), а инвентарь маршрутов живёт в strategy (`screens.md`/`entry-points.md`); дублировать его в stack — нарушение «stack = платформа, не продукт». `design` → `styling` (имя экосистемы); `i18n` — секция `styling.md` (07). `signing` и `compliance` остаются отдельными от `distribution` (разные адресаты: релиз-инженер vs юрист/владелец аккаунта; `/de-template`-гейт по отпечаткам).

Frontmatter — экосистемный: `id: stack.<name>`, `kind`, `name`, `status: draft|stable`; секция `## Generates` обязательна.

| # | Kind | Обязателен | Что описывает (ключевые поля/секции) | `## Generates` | Волна |
|---|---|---|---|---|---|
| 1 | `service` | да | `project-slug`, имя, `platforms: [android, ios]`, `preset`, `strategy-path`, `backend-path`, `surface`, `applicationId`/bundle id per env, схема deep link, языки, владелец аккаунтов | `config/env/*.properties` (ids), `iosApp/Config/*.xcconfig` (ids), `README` | 1 |
| 2 | `runtime` | да | toolchain'ы (Kotlin/AGP/Gradle/JDK; Xcode/Swift; `.xcode-version`), `minSdk`/`targetSdk`/deployment target, KMP-таргеты, политика concurrency (Swift 6 mode), Mac build-host (`MAC_HOST`), политика обновлений latest/latest-1 | `libs.versions.toml` (SDK-уровни), `gradle.properties`, `.xcode-version`, `.swift-version` | 2 |
| 3 | `api` | да | источник спеки (`backend-path`), способ доставки (копия + lock), генератор и конфиг, конверт/ошибки/заголовки, политика совместимости (`min_version`), список используемых ресурсов | `api/openapi.yaml` (копия), `config/openapi/kotlin-models.yaml`, `core/api/generated/**`, `<Resource>Api.kt` заготовки | 3 |
| 4 | `auth` | да | `mode: bearer\|cookie\|none`, TTL, ротация, хранилище (`SecureStore`), сессии, биометрия, pinning-политика, SSO/passkeys (опции), удаление аккаунта | `core/auth` конфиг, `AuthMode`, экраны `auth` (оставить/удалить), BACKEND-HANDOFF B-1…B-3 при `cookie`/отсутствии | 3 |
| 5 | `deep-links` | да | таблица хостов/схем per env, пути → `Route`, `assetlinks`/AASA-параметры (team id, app id, отпечатки), кастомная схема | intent-filters, `applinks:` entitlements, `DeepLinkParser`-тесты, INFRA-HANDOFF (`wellknown` env) | 4 |
| 6 | `push` | да (может быть `providers: []`) | провайдеры per flavor (`rustore`, `fcm`, `apns`), регистрация (`PUT /api/v1/devices`), каналы/категории, permission-flow, payload-контракт, `sandbox` | `core:platform` транспорты, `PUSH_PROVIDERS` в env, `PlatformDeps.PushTransport`, BACKEND-HANDOFF B-18 | 4 |
| 7 | `observability` | да | приёмник (GlitchTip/Sentry), DSN per env, PII-политика, что логируется, символы (mapping/dSYM), пороги crash-free для STOP-2 | Sentry-инициализация обеих платформ, `symbols.sh`, `SENTRY_*` env | 5 |
| 8 | `styling` | да | источник токенов (`design/tokens.json`), тема, типографика, платформенные компоненты (M3 / Liquid Glass), a11y-минимум; секция **i18n**: локали, реестр ключей, fallback | `Tokens.kt`/`Tokens.swift` (`gen-tokens`), `values-<lang>/`, `Localizable.xcstrings` заготовки | 5 |
| 9 | `distribution` | да | каналы per платформа (RuStore / Play tracks / App Store / TestFlight / enterprise), STOP-1/STOP-2 состав, версия/тег, аккаунты (кто владелец), сроки модерации, release-notes-политика, App Review-заметки (demo-доступ) | `scripts/release/*.sh` конфиг (`CHANNELS`), STORE-HANDOFF шаблон, `dist/`-структура | 6 |
| 10 | `signing` | да | инвентарь ключей (upload/app-signing/RuStore, Apple certs/profiles/APNs key), custody (Vaultwarden `mobile/<org>/<app>`, fastlane match репо), оба отпечатка для `assetlinks`, ротация | `signingConfigs` из env, `.env.local.example`, `Local.xcconfig.example`, чек `/de-template` (отпечатки ↔ `deep-links.md`) | 6 |
| 11 | `compliance` | да | собираемые данные (Data safety / Nutrition label), permissions-матрица с обоснованиями, privacy manifests SDK, 152-ФЗ-статус (РКН, локализация), возрастной рейтинг, шифрование (`ITSAppUsesNonExemptEncryption`), правовые документы (URL политики/удаления), чек-лист L-1…L-17 | `PrivacyInfo.xcprivacy`, `AndroidManifest` permissions, экраны `legal`, INFRA-HANDOFF I-6 (URL политики) | 6 |
| 12 | `dependencies` | да | библиотеки/SDK с назначением, лицензией, privacy-manifest-статусом, KMP-совместимостью (для `shared`), `MapSurface`/аналитика/платежи как швы | `libs.versions.toml`, `Package.swift` deps, записи `compliance.md` | 7 |
| 13 | `config` | да (последний, rollup) | таблица build-time / release-time / runtime per env (07 §4.2), схема `/api/app/requirements`, feature-flags | `config/env/*.properties`, `*.xcconfig`, `.env.local.example`, `AppConfig` | 8 |
| 14 | `analytics` | при `preset: consumer` или `ANALYTICS ≠ none` | провайдер (AppMetrica / self-hosted / none), события-реестр, согласие, `AD_ID`/ATT | SDK-инициализация, `consent`, записи `compliance.md` | 4 |
| 15 | `payments` | при монетизации | RuStore Pay / Google Play Billing / StoreKit 2 — продукты, восстановление, серверная валидация, правила 3.1.x | шов `Payments` в `core:platform`/`Platform`, BACKEND-HANDOFF (валидация чеков) | 4 |

**Волны `/stack-deep-dive`** (inside-out, как в `go:`): W1 `service` → W2 `runtime` → W3 `api` → `auth` → W4 `deep-links` → `push` (→ `analytics` → `payments` по пресету) → W5 `styling` → `observability` → W6 `distribution` → `signing` → `compliance` → W7 `dependencies` → W8 `config` (rollup всех env-значений).

**Cross-kind-инварианты** (проверяет `/stack-deep-dive` в конце волны и `/de-template`-гейт):

1. Каждый провайдер в `push.md#providers` имеет env-значения в `config.md` и SDK в `dependencies.md` (с privacy-manifest в `compliance.md`).
2. Каждый хост `deep-links.md` присутствует в `config.md` (per env) и в INFRA-HANDOFF; отпечатки в `deep-links.md` == отпечатки в `signing.md` (оба — upload и app-signing, 05 T-18).
3. Каждая permission в strategy `permissions.md` (или в манифесте) имеет строку в `compliance.md` с обоснованием.
4. `auth.md#mode = bearer` ⇔ спека в `api.md` содержит `POST /api/auth/refresh` (иначе — открытый B-1…B-3 в BACKEND-HANDOFF, `status: stable` невозможен без явной записи `blocked-by`).
5. `distribution.md#channels` содержит только платформы из `service.md#platforms`; `ios ∈ platforms` ⇒ `runtime.md#mac-host` заполнен или `blocked-by: O-2`.
6. `observability.md#receiver` ≠ none ⇒ `SENTRY_DSN` per env в `config.md` и `symbols.sh` включён в `release-preflight`.
7. Условные kinds существуют ⇔ `service.md#preset`/флаги их требуют (лишние — удалить, недостающие — создать из `_templates`).

**Форма `_templates/deep-links.md`** (пример компактного мобильного kind; остальные — той же формы):

```
---
id: stack.deep-links
kind: deep-links
name: Deep links
status: draft
---

# Deep links

## Хосты и схемы

| env | https-хост | кастомная схема | Android app id | iOS team id · bundle id |
|---|---|---|---|---|
| dev | app.dev.<domain> | <scheme>-dev | <id>.dev | <TEAM>.<bundle>.dev |
| staging | … | … | … | … |
| prod | app.<domain> | <scheme> | <id> | <TEAM>.<bundle> |

## Отпечатки (Android)

| ключ | SHA-256 | источник |
|---|---|---|
| upload | … | signing.md |
| app-signing (Play) | … | Play Console |
| RuStore | … | RuStore Console |

## Пути → Route

| путь | Route | требует сессии | pending после логина |
|---|---|---|---|
| /todos/{id} | TodoDetail(id) | да | да |

## Generates

- `androidApp/app/src/main/AndroidManifest.xml` — intent-filters (`autoVerify`) по таблице хостов
- `iosApp/App/App.entitlements` — `applinks:<host>` per env (через xcconfig)
- `shared/…/core/deeplink/DeepLinkParserTest.kt` — тест на каждую строку «Пути → Route»
- INFRA-HANDOFF → `<org>/infra`: `MOBILE_ANDROID_APP_ID`, `MOBILE_ANDROID_SHA256_*`, `MOBILE_IOS_TEAM_ID`, `MOBILE_IOS_BUNDLE_ID` для сервиса `wellknown` (07 §5.2)
```

Гейт `/de-template` (форма `spa:`): все обязательные kinds `stable`; условные — по пресету; ни одного `blocked-by`; cross-kind-инварианты 1–7 выполнены; для `platforms` без iOS — iOS-секции kinds отсутствуют (не «пустые»).

### §5.3 Родство с Go/SPA/bot и `kin-diff`

Таблица (форма SSR-13 §11): **идентично** — файл побайтно общий с экосистемой; **по форме** — та же структура/секции, своё содержание; **не переезжает** — экосистемный механизм, которого у мобильного репо нет.

| Идентично | По форме | Не переезжает |
|---|---|---|
| `.claude/rules/forward-only.md` | `CLAUDE.md` (секции: ядро/роутер, «Где это в экосистеме», Точка входа, Проектирование первично, Команды, Карта, Инварианты, Git workflow, Границы, Forward-only, Авто-память) | Dockerfile / nginx / compose-сервис / `<SVC>_TAG` / `make bump` |
| `.claude/rules/comments.md` (примеры заменены на Kotlin/Swift — единственное отличие) | `Makefile` (группы `##@`, `OK:`/`FAIL:`, `check`, `status`, `doctor`, `setup`) | STOP = image-in-registry (заменён на artifact-in-release) |
| `/plan` (SKILL.md целиком: атомы, чекпоинты, Done) | `scripts/tools/{check,status,doctor}.sh` (секции, финал `OK`/`ATTENTION:`) | MSW / lefthook / pnpm / Steiger / Biome / Vitest (`spa:`) |
| `.claude/plans/roadmap.md` формат (`## Active` / `## Backlog`) | `/smartdev`, `/triage-failure`, `/init-service`, `/stack-deep-dive`, `/de-template`, `/scaffold`, `/release`, `/infra-handoff` | sqlc / mockery / fx / golangci (`go:`) |
| `scripts/git-hooks/commit-msg` (Conventional Commits) | `.claude/rules/{tests-pipeline,env-and-config,release-flow,infra-impact,android-ui,ios-ui}.md` | aiogram / tgbotkit / dialog-architecture (`bot:`) |
| deny-ядро `settings.json` (`--no-verify`, force-push, `reset --hard`, ключи, memory) | `docs/{README,getting-started,contributing}.md`, `docs/reference/{architecture,ecosystem,release,observability}.md` | `docs/reference/deployment.md` |
| `LICENSE`, `SECURITY.md` | `docs/stack/README.md` + `_templates` (frontmatter, `status`, `## Generates`) | FSD-слой / `screens/` в strategy (заменён `mobile/`) |
| INFRA-HANDOFF формат блока (WHAT/WHY/ACTION/WHO/DECIDE/RISK) | `.claude/agents/{explorer,reviewer}.md` | `COVER_FLOOR` единый (у мобильного — три пола) |
| `.editorconfig` общие секции (`*.md`, `*.yaml`, `Makefile`) | `.gate-fixtures/` + `gate-selftest` (форма SSR) | |

`docs/reference/kinship.md` содержит эту таблицу и список «идентичных» файлов с их эталоном (`go-backend-template@main:<path>`). `make kin-diff` — advisory-скрипт: `git archive --remote=<gitea>/stit/go-backend-template main <paths> | tar -xO` → `diff` с локальными «идентичными» файлами → `WARN` при расхождении (не FAIL: форк вправе разойтись, но должен знать). Идея — из SSR-13 §11 (`kin:diff`), реализация без Node.

---
## §6 Слой-мост в strategy (`type: mobile`)

### §6.1 Идентичность слоя

- **Имя и путь:** `mobile` → `docs/projects/<slug>/mobile/<surface>/` в `<org>/strategy`. Для surface с `type: mobile` слой **замещает** `screens/` — ровно как `telegram-bot-flow/` замещает его для `type: bot` (strategy `telegram-bot-flow/README.md`); слоя `fsd/` у мобильного surface нет. Выше остаются `interface/` (ситуации) и `domains/` — на них ссылаются `screens.md`/`entities.md`.
- **Роль:** одновременно композиция (экраны/флоу/оболочка — что видит пользователь) и карта кода (фичи/сущности/точки входа — из чего `/scaffold` выводит пути в `shared/`, `androidApp/`, `iosApp/`). Двухролевость — как у bot-flow; чистая «карта модулей» из 01 (`modules/<surface>/` с kinds feature/core/screen) **отклонена**: инвентарь модулей выводим из `features.md`/`screens.md`, а stack-платформенное (Nav3/NavigationStack, convention-плагины) — не предмет strategy.
- **Ведёт:** `/mobile-deep-dive` в strategy (форма `fsd-deep-dive`/`telegram-bot-flow-deep-dive`: выбор проект × surface, режимы A/B/C, `AskUserQuestion` ≤ 4 в раунде, state-ветка `../entrypoint/state/projects/<slug>/mobile/<surface>/`, stub/cascade, чек-лист draft→stable). Проверяет `/mobile-audit` (read-only) и ракурсы `/cross-audit`.
- **Читают:** `<org>/mobile` (`/scaffold`, `/de-template`, `make status`), `<org>/backend` (`shared-api.md` как источник ожиданий клиента — advisory).
- **Идентификаторы:** структурные — английские (`PascalCase` для route-ключей экранов `TodoDetail`, `kebab-case` для slug'ов сущностей/фич/флоу/permissions `delete-account`, `UPPER_SNAKE` для push-категорий `ORDER_STATUS`), содержание — по-русски (форма bot-flow). Из route-ключа скиллы выводят `Route.TodoDetail`, `TodoDetailScreen.kt`, `TodoDetailView.swift`.

### §6.2 Девять kinds — итоговые формы

За основу — 07 §9.3; финализация: `platforms`-колонка у экранов/фич (по умолчанию все), `shell.md` — singleton с секциями (как `app.md` в FSD), поля 05 §9.4 вложены (см. §6.3). Порядок — inside-out.

| # | Kind | Файл | Колонки / секции | Инварианты | Куда разворачивает `/scaffold` |
|---|---|---|---|---|---|
| 1 | `entity` | `entities.md` | slug · name · описание · ключевые поля · источник (`shared-api` ресурс или `local`) · кэш (`none\|memory\|snapshot`) · domain-ref · status | slug уникален; `source` ≠ `local` ⇒ ресурс есть в `shared-api.md`; domain-ref резолвится в `domains/` | `shared/…/entity/<slug>/` + фикстуры |
| 2 | `screen` | `screens.md` | key (PascalCase) · name · назначение · entry (`tab\|push\|modal\|deep-link\|sheet`) · states (`loading,empty,populated,error,offline,…`) · entities · features · platforms (`all\|android\|ios`) · situation-ref · status | key уникален; states ⊇ `{loading,error}` для экранов с сетью; каждый экран достижим (из `shell.md`, `flows.md` или `entry-points.md`); situation-ref → `interface/` | `Route.<Key>`, `androidApp/feature/<feature>/<Key>Screen.kt`, `iosApp/…/<Key>View.swift` + превью/снапшот на каждое state |
| 3 | `flow` | `flows.md` | slug · name · триггер · шаги (последовательность screen-keys / действий) · выход · ветки ошибок · status | шаги ссылаются на существующие экраны; у флоу с сетью есть ветка ошибки; `app-start` и `login` обязательны | `e2e/common/<slug>.yaml` + сценарный тест презентеров |
| 4 | `feature` | `features.md` | slug · name · действие пользователя · screens · entities · api-ops (из `shared-api.md`) · shared (`true\|false`) · platforms · status | `shared: true` по умолчанию; `false` только с обоснованием в описании (платформенная возможность); api-ops резолвятся; фича ссылается ≥ 1 экран или `entry-point` | `shared/…/feature/<slug>/` (Presenter/State/Intent/UseCase) + платформенные VM/Model |
| 5 | `shell` | `shell.md` (singleton) | `## Root` (табы/корневая навигация, порядок) · `## Auth-gate` (что доступно без сессии; правовые документы до логина) · `## Overlays` (offline, update-required, maintenance, demo-баннер) · `## Offline` (что читается без сети, что блокируется) · `## Theme & locale` · `## Legal` (какие документы и где показываются) | все табы — существующие экраны; `Auth-gate` перечисляет экраны без сессии (минимум: Login, Legal) | `feature/shell/AppShellPresenter`, `RootView`/`NavDisplay` |
| 6 | `entry-point` | `entry-points.md` | slug · kind (`deep-link\|push\|shortcut\|share\|widget\|demo`) · паттерн (путь/категория) · целевой экран · требует сессии · pending после логина · status | путь deep link уникален и есть в stack `deep-links.md`; push-категория ↔ stack `push.md`; `demo` обязателен при `preset ≠ enterprise` | `DeepLinkParser` ветка + тест; intent-filter/AASA-путь; push-категория |
| 7 | `permission` | `permissions.md` | slug · Android permission id(s) · iOS usage-key · зачем (пользовательская формулировка rationale) · когда запрашивается (экран/фича, пред-запрос) · fallback без разрешения · status | каждая permission используется ≥ 1 фичей/экраном; rationale заполнен (магазины требуют); fallback описан | rationale-экран, манифест/`Info.plist` ключ, строка stack `compliance.md` |
| 8 | `shared-api` | `shared-api.md` | ресурс · операции (`GET /todos`, …) · владелец (backend-домен/сервис) · идемпотентность · нужен offline-кэш · status | операции существуют в `api/openapi.yaml` (проверяет `/mobile-audit` через `backend-path`, advisory); отсутствующие → BACKEND-HANDOFF | `<Resource>Api.kt` + фикстуры |
| 9 | `shared-lib` | `shared-lib.md` | `## Locales` (список, дефолт, fallback) · `## i18n` (реестр ключей ошибок `error.<CODE>`) · `## Logging` (что логируем, PII-политика) · `## Feature flags` (имена флагов из `/api/app/requirements`) · `## Analytics events` (реестр, если `analytics` включён) | локали ⊇ `ru`; каждый флаг используется ≥ 1 фичей | `values-<lang>/`, `Localizable.xcstrings`, `FeatureFlags`, `MessageKey` |

### §6.3 Куда легли поля 05 §9.4 и что остаётся в stack

| Поле из 05 §9.4 | Место | Почему |
|---|---|---|
| целевые магазины и каналы, min OS | stack `distribution.md`, `runtime.md` | платформенные факты, не продукт |
| permissions с обоснованиями | strategy `permissions.md` (rationale) + stack `compliance.md` (юридическая формулировка, Data safety) | rationale — продуктовый текст; декларация — платформенная |
| offline-объём | strategy `shell.md#Offline` + `entities.md#cache` | продуктовое решение «что работает без сети» |
| demo-сценарий для ревью | strategy `entry-points.md` (`demo`) + `flows.md` (`review-demo`) | ревьюер проходит продуктовый путь |
| правовые документы | strategy `shell.md#Legal` (какие и где) + stack `compliance.md` (URL, версии) | текст — продукт; хостинг — infra |
| push-категории | strategy `entry-points.md` (`push`) + stack `push.md` (провайдеры, каналы ОС) | категория — продукт; транспорт — платформа |

Правило разграничения (как в `spa:`): **strategy описывает продукт и его карту; stack — платформу и её значения**. Всё, что меняется при смене магазина/SDK/хоста, — stack; всё, что меняется при смене продукта, — strategy.

### §6.4 Канон-PR в strategy (S-1…S-3) и путь без PR

**S-1 — канон слоя** (`business-strategy-template`): `docs/projects/_template/mobile/README.md` (роль, 9 kinds, идентификаторы, inside-out, чек-лист draft→stable, связь с `interface/`/`domains/`, что читает `<org>/mobile`) + `_template/mobile/{entities,screens,flows,features,shell,entry-points,permissions,shared-api,shared-lib}.md` (пустые формы с колонками §6.2 и примером строки).

**S-2 — скиллы**: `/mobile-deep-dive` (клон формы `telegram-bot-flow-deep-dive`: picker проект × surface, scaffold 9 файлов, режимы A — «с нуля», B — «дозаполнение», C — «ревизия», stub/cascade при удалении экрана/сущности, чек-лист 6 пунктов: (1) все ссылки резолвятся (entities ↔ screens ↔ features ↔ flows ↔ entry-points ↔ permissions), (2) каждый экран достижим, (3) у каждой фичи есть api-ops или `local`, (4) каждая permission используется, (5) states у каждого экрана с сетью, (6) situation-/domain-ref заполнены); `/mobile-audit` (read-only: структура, source-refs, layer-refs, семантика, зрелость; advisory-проверка `shared-api.md` против `../backend/api/openapi.yaml`); ракурсы `/cross-audit`: `surface-mobile` (surface `type: mobile` ⇔ слой существует и stable), `mobile-domain` (entities ↔ `domains/`), `mobile-service` (shared-api ↔ карта сервисов/бэкенда), `mobile-web` (пути deep links ↔ `pages.path` web-surface того же продукта — одни URL должны открываться и в вебе, и в приложении).

**S-3 — формулировки в существующих файлах strategy:**

- `docs/projects/_template/project/surface.md` — в описании `type`: «`mobile` — нативное мобильное приложение (Android/iOS); карта ведётся в слое `mobile/<surface>/` (замещает `screens/`), код — в `<org>/mobile`»; в поле `lives-at` — пример `<org>/mobile`.
- `docs/projects/_template/fsd/README.md` — абзац «Область применения»: «FSD-карта — для `type: web|admin` (React SPA/SSR). Для `type: bot` — `telegram-bot-flow/`, для `type: mobile` — `mobile/`; у них нет слоя FSD».
- `docs/projects/_template/screens/README.md` — «Для surface с `type: bot` и `type: mobile` этот слой не ведётся: их композиция описывается в `telegram-bot-flow/` и `mobile/` соответственно».
- `docs/README.md` (таблица handoff): строка `| <org>/mobile | mobile/<surface>/ (9 kinds) | /scaffold, /de-template читают stable-записи |`.
- `CLAUDE.md` strategy — в перечне слоёв и скиллов: `mobile/` и `/mobile-deep-dive`, `/mobile-audit`.

**Путь без PR** (strategy-форк без канона): шаблон везёт копию S-1 в `docs/reference/strategy-mobile-canon/`. `/init-service` (стадия 3), не найдя `../strategy/docs/projects/_template/mobile/`, печатает next-step с точными командами копирования канона в форк strategy (`cp -r docs/reference/strategy-mobile-canon/. ../strategy/docs/projects/_template/mobile/` и создание `docs/projects/<slug>/mobile/<surface>/` из форм) — **не пишет** в соседний репо сам (экосистемное правило: соседи read-only). Без скиллов strategy карта заполняется руками по формам; `/scaffold` в `<org>/mobile` работает одинаково — он читает файлы, а не state скилла. Режим `--from-args` у `/scaffold` остаётся для проектов вовсе без strategy, с предупреждением и записью в roadmap «завести карту».

### §6.5 Отображение kinds в код (для `docs/reference/strategy-mapping.md`)

| Kind → | `shared/` | `androidApp/` | `iosApp/` | Прочее |
|---|---|---|---|---|
| entity `<slug>` | `entity/<slug>/{Model,Repository,Dto}.kt` | — | — | `mock/fixtures/*/<slug>.json` |
| screen `<Key>` (в фиче `<slug>`) | `Route.<Key>`; состояние — в `feature/<slug>/<Slug>State.kt` | `feature/<slug>/<Key>Screen.kt`, `<Key>NavEntry.kt`, `screenshots/<Key>_*.png` | `Features/Sources/<Slug>Feature/<Key>View.swift`, `__Snapshots__/` | — |
| flow `<slug>` | `feature/<…>/…PresenterTest` (сценарий) | — | — | `e2e/common/<slug>.yaml` |
| feature `<slug>` | `feature/<slug>/{<Slug>Presenter,<Slug>State,<Slug>Intent,<Slug>UseCase}.kt` | `feature/<slug>/<Slug>ViewModel.kt` (+ модуль `:androidApp:feature:<slug>`) | `Features/Sources/<Slug>Feature/<Slug>Model.swift` (+ таргет) | — |
| shell | `feature/shell/AppShellPresenter.kt` | `app/…/MainActivity.kt`, `core/ui` оверлеи | `App/RootView.swift`, `Features/Sources/Shell/` | — |
| entry-point | `core/deeplink/DeepLinkParser.kt` + тест; `core/push/PushPayload.kt` | `AndroidManifest.xml` intent-filter; `core:platform` категория | `App.entitlements` `applinks:`; `Platform/NotificationCategories.swift` | stack `deep-links.md`/`push.md` |
| permission | — | `core/platform/permissions/<Slug>Gate.kt` + rationale-экран в `core/ui` | `Platform/Permissions/<Slug>.swift` + `Info.plist` usage-key | stack `compliance.md` |
| shared-api ресурс | `core/api/<Resource>Api.kt` (+ сгенерированные модели) | — | — | `api/openapi.yaml` (проверка наличия операций) |
| shared-lib | `core/i18n/MessageKey.kt`, `core/requirements/FeatureFlags.kt`, `core/observability` | `values-<lang>/strings.xml` | `Localizable.xcstrings` | — |

---
## §7 STOP-контракт и релиз

### §7.1 Граница STOP — разрешение противоречия (б)

Три формулировки — 05 §7.4 (две ступени с Gitea Release), 03 (AAB + IPA в `dist/` + handoff), 01 (AAB + mapping + Baseline Profile как артефакты) — не противоречат, а описывают разные слои одного процесса. Принято:

- **STOP-1 «артефакт готов и проверяем»** = **Gitea Release на аннотированном теге `vX.Y.Z`** с полным набором ассетов (§7.2). Это мобильный аналог «образ в registry» у сервисов: артефакт лежит в corp-контуре, привязан к тегу, виден в репо, забирается по API, не зависит от магазинов. `dist/` (03) — локальная стадия сборки перед загрузкой, не STOP. Mapping и Baseline Profile (01) — часть ассетов (mapping — отдельным файлом, Baseline Profile — внутри AAB, если генерировался; в шаблоне v1 — нет, §2.2).
- **STOP-2 «подано в каналы»** = Play internal track загружен / RuStore-черновик отправлен на модерацию / TestFlight-билд загружен (05 §7.4). Автоматизируемо на 100 % (API есть у всех трёх); iOS-часть — на Mac.
- **За STOP** — модерация, листинги, ответы ревью, промоушен в production-трек, ротация ключей: человек по STORE-HANDOFF.

Почему Gitea Release, а не MinIO/Generic-пакеты: привязка к тегу и репо, страница релиза с release-notes, API ассетов, права по репо. Пределы Gitea: `[repository.release] FILE_MAX_SIZE` по умолчанию 2048 МБ (достаточно), `MAX_FILES` по умолчанию 5 **на загрузку** — при 8–10 ассетах загружать по одному через API (`POST /repos/{owner}/{repo}/releases/{id}/assets`) либо поднять лимит в corp `app.ini` (C-4 в §8); reverse-proxy body-limit на пути к Gitea — проверить (Traefik без лимита по умолчанию; middleware `buffering`, если включён). Generic-registry Gitea — резерв для артефактов > 2 ГБ (не ожидается).

### §7.2 Ассеты STOP-1

| Ассет | Платформа | Кем создан | Обязателен |
|---|---|---|---|
| `<app>-<X.Y.Z>-<code>-prod.aab` | Android | `bundleProdRelease` (Linux) | да |
| `<app>-<X.Y.Z>-<code>-prod.apk` | Android | `assembleProdRelease` (universal; RuStore принимает AAB и APK — 05 §1.1) | да |
| `mapping-<X.Y.Z>-<code>.txt` | Android | R8 | да (дубль того, что ушло в Sentry — для retrace без Sentry) |
| `<app>-<X.Y.Z>-<code>-ios.ipa` | iOS | `xcodebuild -exportArchive` (Mac, method `app-store-connect`) | да при `ios ∈ platforms` |
| `<app>-<X.Y.Z>-<code>-ios-dSYM.zip` | iOS | из архива (Mac) | да при iOS |
| `checksums.sha256` | обе | `publish-gitea.sh` | да |
| `release-notes.ru.md`, `release-notes.en.md` | обе | из `.claude/plans/roadmap.md`-секции релиза + Conventional Commits (черновик — `/release`, правит человек) | да |
| `openapi.lock` (снимок) | обе | копия из репо | да (фиксирует, какой контракт ожидает сборка) |
| `store-handoff.md` | обе | `/release` | да (тот же блок, что печатается в чат) |
| `baseline-prof.txt` | Android | Baseline Profile generator (устройство/GMD) | нет (опция) |

Символы: `mapping.txt` уходит в Sentry/GlitchTip Gradle-плагином (Linux, 07 §7); dSYM — `sentry-cli` на Mac (лицензия FSL-1.1 — допустимо для внутреннего использования, 07 §7). `release-preflight` проверяет, что символы загружены до STOP-2 (05 §7.4 «symbols before publish»).

### §7.3 Версия из тега

- Тег `vX.Y.Z` (аннотированный, только через `/release`). `versionName` = `CFBundleShortVersionString` = `X.Y.Z`; `versionCode` = `CFBundleVersion` = `X·1 000 000 + Y·1 000 + Z` (05 §7.1) — монотонность обеспечивает сам тег; `scripts/tools/version-from-tag.sh` печатает оба значения, Gradle и xcconfig читают их из env `APP_VERSION`/`APP_BUILD` (в dev-сборках — `0.0.0`/`1` + git-sha в `About`).
- `release-preflight` проверяет: тег больше последнего (`git tag -l 'v*' | sort -V`), больше опубликованного в каналах (через API Play/RuStore — 05 §7.1; при недоступности — WARN), рабочее дерево чистое, HEAD на `main`, `check-full` пройден на этом HEAD (`build/gate-logs/mac.last` == HEAD).
- Отклонение модерации → **новый тег `Z+1`**, тот же процесс (05 §7.4); повторное использование тега/кода запрещено; отката версий нет.
- `RELEASE`-идентификатор для Sentry: `<applicationId>@X.Y.Z+<code>` (одинаков на обеих платформах — общий дашборд).

### §7.4 `/release` — форма и шаги

Флаги: `--platform android|ios|both` (дефолт — `service.md#platforms`), `--stop 1|2` (дефолт 2), `--channels gitea,play,rustore,testflight` (дефолт — `distribution.md#channels`), `--dry-run`. Форма `go:` (preflight → steps → отчёт), pre-условие — секреты в env (§7.5), загруженные человеком.

| # | Шаг | Где | Верб |
|---|---|---|---|
| 0 | preflight: чистое дерево, `main`, `check-full` на HEAD, версия монотонна, L-1…L-9 (автоматизируемые пункты 05 §5: политика доступна, удаление аккаунта есть, `PrivacyInfo` валиден, permissions ↔ `compliance.md`, `ITSAppUsesNonExemptEncryption`, cleartext off, DSN prod задан, символы-креды есть, release-notes не пусты), env-секреты присутствуют | Linux | `make release-preflight TAG=` |
| 1 | аннотированный тег `vX.Y.Z` (локально; push после STOP-1) | Linux | внутри скрипта |
| 2 | Android: `bundleProdRelease` + `assembleProdRelease` с подписью из env → `dist/`; `mapping.txt`; Sentry-плагин загружает mapping | Linux | `make release-android` |
| 3 | iOS: `mac.sh archive` → `xcodebuild -exportArchive` → `dist/*.ipa` + `dSYM.zip`; `sentry-cli upload-dif`; подпись через fastlane match (репо `<org>/ios-certificates`) или профили из Vaultwarden | Mac | `make release-ios` |
| 4 | `checksums.sha256`, release-notes, `openapi.lock`, `store-handoff.md` → `dist/` | Linux | внутри |
| 5 | **STOP-1**: `git push --tags`; Gitea Release через API (`tea` CLI или `curl`), ассеты по одному; ntfy `releases-<org>`: «`<app> vX.Y.Z` STOP-1: <ссылка на Release>» | Linux | `make release-publish TAG= CHANNELS=gitea` |
| 6 | **STOP-2** (по каналам): Play internal (Google Play Developer API / GPP 4.1.1 — 05 §1.2), RuStore (публичное API: RSA-SHA512 → JWE 900 с, черновик → на модерацию; 05 §1.1), TestFlight (Mac: `xcodebuild -exportArchive … -exportOptionsPlist` с `destination: upload` или fastlane pilot); ntfy | Linux + Mac | `make release-publish TAG= CHANNELS=play,rustore,testflight` |
| 7 | STORE-HANDOFF (05 §7.7: ARTIFACTS / PLAY / RUSTORE / TESTFLIGHT / BACKEND / ENV / LEGAL / RISK) в чат и в `dist/store-handoff.md`; секция в `.claude/plans/roadmap.md` с человеческими шагами (ответы модерации, листинги, промоушен) | — | — |

Каналы `enterprise` (`preset: enterprise`): `gitea` (APK как ассет + страница «установить» с ссылкой на Release — deep link `wellknown` не нужен) и `abm` (Apple Business Manager custom app — загрузка тем же TestFlight-путём с назначением в ABM; человек). Play-канал при `enterprise` не используется.

Что `/release` **не делает**: не генерирует ключи (`keytool` в deny), не читает Vaultwarden (человек экспортирует в `.env.local`), не отвечает магазину на модерации, не переключает production-трек Play (промоушен — человек, после crash-free-порога из `observability.md`).

### §7.5 Секреты релиза и custody

| Переменная (`.env.local`, gitignored, deny на Read) | Источник (Vaultwarden `mobile/<org>/<app>`) | Нужна для |
|---|---|---|
| `STIT_KEYSTORE_PATH`, `STIT_KEYSTORE_PASS`, `STIT_KEY_ALIAS`, `STIT_KEY_PASS` | запись `android-upload-key` (+ файл `.jks` как вложение) | подпись AAB/APK (upload key; тот же ключ регистрируется в RuStore или отдельный — по `signing.md`) |
| `PLAY_SERVICE_ACCOUNT_JSON` | `play-service-account` | STOP-2 Play |
| `RUSTORE_KEY_ID`, `RUSTORE_PRIVATE_KEY` | `rustore-api-key` | STOP-2 RuStore |
| `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8` | `apple-asc-api-key` | TestFlight/App Store Connect API |
| `MATCH_GIT_URL`, `MATCH_PASSWORD` (или профили/сертификаты как вложения) | `apple-signing` | подпись IPA на Mac |
| `APNS_KEY_ID`, `APNS_TEAM_ID`, `APNS_KEY_P8` | `apns-key` (≤ 2 team-ключей на env — 02 §5) | не приложению, а бэкенду (`golibinfra/pushkit`) — хранится в той же коллекции, чтобы владелец видел полный инвентарь |
| `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_PROJECT_*` | `sentry` | символы |
| `GITEA_TOKEN` | `gitea-release-token` (scope `write:repository`) | STOP-1 |
| `NTFY_TOPIC=releases-<org>`, `NTFY_TOKEN` | `ntfy` | уведомления |
| `MAC_HOST` | не секрет; `.env.local` для удобства | iOS-стадии |

Custody (05 §7.6): ключи создаёт человек один раз (`keytool` / Apple Developer portal / fastlane match init), кладёт в Vaultwarden; `signing.md` хранит **инвентарь и отпечатки**, не значения; upload-key ≠ Play app-signing key — оба отпечатка в `deep-links.md` (05 T-18); ротация — запись в `signing.md` + INFRA-HANDOFF (отпечатки в `wellknown`). Потеря upload-ключа — процедура сброса в Play (05), для RuStore — новый ключ регистрируется в консоли; описано в `docs/reference/release.md`.

### §7.6 STORE-HANDOFF — форма

Блок печатается `/release` (форма из 05 §7.7, приведена к WHAT/WHY/ACTION/WHO/DECIDE/RISK экосистемного handoff'а внутри каждой секции):

```
STORE-HANDOFF · <app> vX.Y.Z (code <N>) · <дата>
ARTIFACTS  Release: <url> · aab/apk/ipa sha256 … · symbols: uploaded (mapping ✓ dSYM ✓)
PLAY       internal track: uploaded · WHO: владелец аккаунта · ACTION: промоушен в production после crash-free ≥ 99.5 % за 48 ч · DECIDE: staged rollout 20 %?
RUSTORE    черновик #… отправлен на модерацию (ожидание ≤ … дней, черновик живёт 14 дней) · ACTION: ответить на замечания; при отказе — `/release` с Z+1
TESTFLIGHT build <N> processing · ACTION: внешняя группа → App Review (demo-доступ: см. entry-points `demo`; App Review notes из distribution.md)
BACKEND    min_version в /api/app/requirements → <X.Y.Z> после раскатки (BACKEND-HANDOFF) · openapi.lock: <sha>
ENV        wellknown: отпечатки актуальны ✓ · SENTRY_DSN prod ✓ · MOBILE_* env в <org>/infra ✓
LEGAL      L-10…L-17 (человек): политика опубликована по <url>, Data safety обновлён?, возрастной рейтинг, РКН-статус
RISK       что может отклонить ревью (по правилам 4.8/5.1.1/3.1.3), что не проверено на устройстве, что ушло без check-mac (не должно)
```

### §7.7 Где это живёт в шаблоне

`docs/reference/release.md` (процесс, вербы, секреты, STORE-HANDOFF), `docs/stack/distribution.md` (каналы, аккаунты, сроки — значения проекта), `docs/stack/signing.md` (инвентарь ключей), rule `release-flow` (инварианты для агента), `scripts/release/*.sh` (реализация), corp `docs/ecosystem.md` (контракт artifact-in-release — одна строка канона, §8.1).

---
## §8 Cross-repo

Порядок — по SSR §11: сначала corp-канон (он определяет число шаблонов и контракт STOP), затем бэкенд (без него нет auth/requirements), библиотеки, инфра, strategy, sweep по остальным шаблонам, stit-public — последним. Колонка «Блокирует» — блокирует ли **первый релиз шаблона** (фаза 7 §9); «нет (fallback)» — есть обходной путь внутри шаблона.

### §8.1 Упорядоченный список

| № | Репо | Место | Изменение | Приоритет | Блокирует | Источник |
|---|---|---|---|---|---|---|
| C-1 | corp-infra-template | `docs/ecosystem.md` | «6 форкаемых шаблонов» → «7» (после SSR — «8»); строка Identity `\| mobile-template \| <org>/mobile \| Android + iOS на KMP-ядре; STOP = артефакт в Gitea Release \|`; диаграмма — узел `mobile` как клиент `backend` (Bearer), без маршрута Traefik; sibling-checkout `+ mobile`; happy path «новый мобильный клиент»; Skill index — `mobile-template` в строках `/init-service, /stack-deep-dive, /de-template, /scaffold` и `/release, /infra-handoff` | P0 | нет (доки), но канон первым | §1.5 |
| C-2 | corp-infra-template | `docs/ecosystem.md#Канонические контракты` | новый контракт **Artifact-in-release (mobile)**: «подписанные AAB/APK/IPA + символы + checksums + release-notes — ассеты Gitea Release на аннотированном теге `vX.Y.Z`; STOP-2 — подача в каналы; магазины — за STOP» (§7.1) | P0 | нет | §7 |
| C-3 | corp-infra-template | `docs/ecosystem.md` (операционная модель) + org-действие | конвенция Vaultwarden-коллекции `mobile/<org>/<app>` (инвентарь §7.5) и ntfy-топика `releases-<org>`; коллекцию создаёт владелец org | P0 (для релиза) | **да** — без custody нет подписи | 05 §7.6, §9.1 |
| C-4 | corp-infra-template | `compose/gitea` `app.ini` | `[repository.release] MAX_FILES = 20` (по умолчанию 5 на загрузку; `FILE_MAX_SIZE` 2048 МБ достаточно); проверить body-limit на пути Traefik → Gitea; scope токена `write:repository` для `/release` | P1 | нет (загрузка ассетов по одному через API) | §7.1 |
| C-5 | corp-infra-template | GlitchTip/Sentry (org) | проекты `<app>-android`, `<app>-ios` (или один с `platform`-тегом), DSN per env, auth-token для символов | P1 | нет (пустой DSN = выключено) | 07 §7 |
| C-6 | corp-infra-template | `docs/ecosystem.md` / onboarding | строка о Mac build-host как ресурсе org (не сервис control-plane): `MAC_HOST`, Tailscale, `~/ci/<slug>/`; Xcode Cloud — резерв | P2 | нет | 05 §6, §4.4 |
| B-1…B-3 | go-backend-template | auth-домен, `api/openapi.yaml` | Bearer-выдача, `POST /api/auth/refresh {refresh_token}` с ротацией, `Authorization: Bearer` в middleware при сохранении cookie для SPA; `X-Client` | P0 | нет (fallback `AuthMode.Cookie`, §2.5), **да** для `preset ≠ companion`-проектов с внешними пользователями | 07 §1.3b |
| B-12 | go-backend-template | `Makefile`, `api/openapi.yaml`, гейт | `make openapi` + закоммиченная спека + `gen-check` в гейте бэкенда — источник копии `api/openapi.yaml` мобильного | P0 | нет (шаблон везёт reference-спеку; для реального проекта — да) | 07 §2.1 |
| B-17 | go-backend-template | домен `app` | `GET /api/app/requirements` (07 §4.3: `min_version` per platform, `maintenance`, `flags`) | P0 | нет (404 ⇒ «требований нет») | 07 §4.3 |
| B-13, B-14, B-15 | go-backend-template | спека | `$schema`-ключ, конверт/nullable-списки, `ErrorModel.code` — дефекты, ломающие кодоген; пока не исправлены — `gen.sh` пост-процессит копию | P1 | нет (пост-процессинг) | 07 §2.2 |
| B-18 | go-backend-template (+ G-1) | домен `device`/`notification` | `PUT /api/v1/devices` (07 §6), отправка через `golibinfra/pushkit` | P1 | нет (push-фича тестируется на mock; end-to-end — при появлении) | 07 §6 |
| B-4…B-9, B-11, B-16 | go-backend-template | auth/сессии/гейт | `GET/DELETE /api/auth/sessions`, revoke при logout, `Idempotency-Key`-хранилище (B-9), `Retry-After`, тесты bearer-потока — полный перечень в 07 §1.3b/§2.6 | P1–P2 | нет | 07 |
| B-10 | go-backend-template | `docs/stack/auth.md` | описать два режима (cookie для web, bearer для mobile) и `X-Client` | P1 | нет | 07 |
| B-19 | go-backend-template | гейт | `oasdiff breaking` против последнего тега — защита аддитивной эволюции для мобильных клиентов, которые не обновляются мгновенно | P2 | нет | 07 §2.6 |
| G-1 | golibinfra | `pushkit` | мульти-провайдерная отправка (RuStore, FCM, APNs), data-first payload, `notification_id` | P1 | нет | 07 §6, 05 §2 |
| G-2 | golibinfra | `ErrorModel` | стабильное поле `code` (машиночитаемый код → `MessageKey` на клиенте) | P1 | нет | 07 §2.2 |
| G-3…G-5 | golibinfra | — | по 07 (idempotency middleware, `X-Client` в логах, `Retry-After` helper) | P3 | нет | 07 |
| I-1 | project-infra-template-compose | `compose/wellknown` + `configs/wellknown/*.tpl` + `render-configs.sh` + Traefik `PathPrefix(/.well-known/) priority 1000` | always-on сервис `wellknown` (nginx): `assetlinks.json` + `apple-app-site-association` из env `MOBILE_ANDROID_APP_ID`, `MOBILE_ANDROID_SHA256_{UPLOAD,PLAY,RUSTORE}`, `MOBILE_IOS_TEAM_ID`, `MOBILE_IOS_BUNDLE_ID` (07 §5.2) | P0 | нет для STOP-1; **да** для App Links/Universal Links в prod | 07 §5 |
| I-3 | project-infra-template-compose | `.env.example` (секция профиля `mobile`) | `MOBILE_*` переменные + `APP_MIN_VERSION_ANDROID/IOS` (если requirements читает env) | P1 | нет | 07 §4 |
| I-4 | project-infra-template-compose | `.env.example`, secrets бэкенда | креды провайдеров push для `pushkit` (APNs key, FCM service account, RuStore service token) | P1 | нет | 05 §2 |
| I-5 | project-infra-template-compose | `.env.example` | `SENTRY_DSN_MOBILE_*`/GlitchTip host (если мобильные DSN раздаются через requirements) | P2 | нет | 07 §7 |
| I-6 | project-infra-template-compose | `wellknown`/статика (или `landing`) | хостинг URL политики конфиденциальности и страницы удаления аккаунта (Play требует web-URL удаления; 05 L-3/L-4) | P0 для подачи в магазины | **да** для STOP-2 (Play/App Store) | 05 §5 |
| I-2 | project-infra-template-compose | Traefik/бэкенд | CORS — мобильному не нужен; пометить в доке, чтобы не расширяли `Origin` под приложение | P3 | нет | 07 |
| S-1 | business-strategy-template | `docs/projects/_template/mobile/` | канон слоя: README + 9 форм (§6.2) | P1 | нет (путь без PR, §6.4) | §6 |
| S-2 | business-strategy-template | `.claude/skills/{mobile-deep-dive,mobile-audit}`, `cross-audit` ракурсы | скиллы (§6.4) | P1 | нет | §6 |
| S-3 | business-strategy-template | `surface.md`, `fsd/README.md`, `screens/README.md`, `docs/README.md`, `CLAUDE.md` | формулировки §6.4 | P1 | нет | §6 |
| F-1 | spa-frontend-template | `docs/reference/ecosystem.md`, `.claude/rules/env-local.md` | sweep «6 → 7» и абзац «бэкенд отдаёт cookie для web и Bearer для mobile — режимы сосуществуют» | P2 | нет | 07 |
| F-2 | spa-frontend-template | `src/app/styles/index.css` | (позже) паритет с `design/tokens.json` мобильного — только когда появится общий продукт web+mobile | P3 | нет | 07 §8 |
| T-1 | telegram-bot-template, go-backend-template, project-infra-template-compose, business-strategy-template | `docs/reference/ecosystem.md` / `docs/ecosystem.md` | sweep «6 шаблонов → 7»; координируется с SSR-sweep («7 → 8») — один PR на репо | P2 | нет | SSR §11 |
| P-1 | stit-public | `docs/roadmap.md`, `docs/templates.md`, `README.md` | строки §1.5 (две → одна), счётчик шаблонов, формулировка планов | P1 (последним) | нет | §1.5 |

### §8.2 Формулировки для corp `docs/ecosystem.md`

**Семейство:** «… 7 форкаемых шаблонов: go-backend-template → `<org>/backend`, spa-frontend-template → `<org>/frontend`, telegram-bot-template → `<org>/<bot>`, project-infra-template-compose → `<org>/infra`, business-strategy-template → `<org>/strategy`, project-docs-template → `<org>/docs`, **mobile-template → `<org>/mobile`** … + 3 библиотеки».

**Канонические контракты — новая строка:**

> **Artifact-in-release (mobile).** Мобильный клиент не имеет образа и compose-сервиса. Его STOP-1 — Gitea Release на аннотированном теге `vX.Y.Z` в `<org>/mobile` с ассетами: подписанные AAB + APK (Android), IPA + dSYM.zip (iOS), `mapping.txt`, `checksums.sha256`, release-notes ru/en, `openapi.lock`; символы загружены в GlitchTip/Sentry до публикации. STOP-2 — подача в каналы (Play internal, RuStore moderation, TestFlight). Магазины, листинги и модерация — за STOP (STORE-HANDOFF). Ключи подписи и store-credentials — в Vaultwarden, коллекция `mobile/<org>/<app>`; уведомления о релизах — ntfy-топик `releases-<org>`.

**Skill index — строки:**

```
| /init-service, /stack-deep-dive, /de-template, /scaffold | go-backend-template, spa-frontend-template, telegram-bot-template, mobile-template |
| /release, /infra-handoff | go-backend-template, spa-frontend-template, telegram-bot-template, mobile-template (`/release --platform android\|ios\|both`) |
| /mobile-deep-dive, /mobile-audit | business-strategy-template (слой `mobile/<surface>/`) |
```

**Happy path «новый мобильный клиент»:** форк `mobile-template` → `<org>/mobile` → `/init-service` (platforms, preset) → карта `mobile/<surface>/` в `<org>/strategy` → `/stack-deep-dive` → BACKEND-HANDOFF (`<org>/backend`: bearer, requirements, devices) → INFRA-HANDOFF (`<org>/infra`: `wellknown`, `MOBILE_*`) → `/de-template` → `/release --stop 1` → Gitea Release → `/release --stop 2` → магазины.

### §8.3 Что мобильный репо **не** требует от экосистемы

Нет CI-раннеров (решение экосистемы — гейт локальный, релиз — с машины владельца/Mac-хоста); нет OTLP/Grafana (05: GlitchTip/Sentry достаточно; `TelemetrySink` — шов); нет отдельного Maven-реестра до триггера kit'ов (§1.6; Gitea Packages поддерживает Maven/Swift/Generic — готово к моменту извлечения); нет маршрута Traefik, домена и TLS для приложения — только для `wellknown` и бэкенда.

---
## §9 План реализации

Отправная точка — 04 §6.3: структура JetBrains `KMP-App-Template-Native` (раскладка `shared/`+`androidApp/`+`iosApp/`, Xcode-проект с direct integration), `android create empty-activity` (AGP 9-совместимый Android-каркас), Xcode «SwiftUI App» (или `iosApp/` из того же JetBrains-шаблона), и спайк 06 `spikes/android-gate` (convention-плагины, `check.sh`, `gradle-step.sh`, `doctor.sh`, `detekt.yml`, Konsist, Roborazzi, Hilt — всё уже прогнано). Фазы идут последовательно, каждая закрывается гейтом; размер — S/M/L в агент-сессиях (S ≈ 1–2, M ≈ 3–6, L ≈ 7+); «без Mac» — что выполнимо на Linux.

### §9.1 Спайки (фаза 1)

| ID | Что проверить | Где | Влияет на | Fallback |
|---|---|---|---|---|
| S-1 | SKIE 0.10.14 + direct integration: sealed → Swift enum, `StateFlow` → `AsyncSequence`, `suspend` → `async`; время `embedAndSign…` | Mac | §2.2, `ios-ui` | без SKIE — flat DTO-фасад с callback'ами (03 §3) |
| S-2 | BCV 0.18.2 klib-дамп для `iosArm64` на Linux при Kotlin 2.4 (`strictValidation` false/true) | Linux | §4.3 | klib-дамп только в `check-mac`; на Linux — JVM-дамп + Konsist |
| S-3 | `android skills add --agent=CLAUDE` — целевой каталог, список имён, идемпотентность | Linux | §3.5 | имена каталогов в `.gitignore` |
| S-4 | openapi-generator 7.25.0 `kotlin` multiplatform models-only — компиляция в `iosArm64`-klib (06 §8.8 проверил JVM); пост-процессинг B-13…B-15 | Linux | §2.5, `gen.sh` | рукописные DTO для reference-ресурсов |
| S-5 | Ktor `AuthPlugin` single-flight refresh на OkHttp и Darwin + `MockEngine`; `AuthMode.Cookie` с персистентными cookie на обоих движках | Linux (JVM) + Mac (Darwin) | §2.5 | cookie-режим объявить «Android-only до проверки» |
| S-6 | Воспроизводимость Roborazzi-эталонов на двух машинах с одним toolchain (06 §10.8); порог `changeThreshold` | Linux ×2 | §4.2 | порог различий + правило «эталоны пишет стандартное окружение» |
| S-7 | Konsist 0.17.3 по KMP-source-set'ам `shared/` (правила слоёв `core/entity/feature`, «без generic'ов в публичных сигнатурах») | Linux | §2.6 | detekt custom-rule / скрипт grep |
| S-8 | Swift 6.4 на Linux: `Observation` (`@Observable`) в `Pure`, Swift Testing, `llvm-cov` JSON (06 §6 проверил базовый цикл) | Linux | §2.3, §4.8 | `Pure` без `@Observable` |
| S-9 | Gradle-таск `generateTokens` в `build-logic` (DTCG JSON → `Tokens.kt` + `Tokens.swift`), детерминированный вывод | Linux | §4.2 `tokens-check` | скрипт сравнения имён по regex |
| S-10 | Xcode buildable folders + umbrella-product `Features`: добавление фичи без правки `.pbxproj` (02 §3) | Mac | §2.3, `xcode-project` | документированный шаг «добавить продукт в таргет» |
| S-11 | Gitea Release API с Linux: аннотированный тег, создание релиза, загрузка 8–10 ассетов по одному, размеры 100+ МБ через Traefik | Linux + corp | §7 | Generic-registry |
| S-12 | RuStore publish API и Google Play Developer API (internal) на throwaway-приложении (нужны аккаунты); GPP 4.1.1 под AGP 9.4 | Linux + аккаунты | §7.4 | ручная загрузка в консоли (STORE-HANDOFF) |
| S-13 | `/init-service --add-platform` восстановление половины из upstream-ветки на реальном форке после `--platform android` | Linux | §2.8 | документированный ручной путь |
| S-14 | `androidLibrary { withHostTest {} }` для `androidMain`-actual'ов (`SecureStore` на Robolectric) и время `:shared:jvmTest` тёплое | Linux | §4.2 | тесты actual'ов в `androidApp/core/testing` |
| S-15 | `xcodebuild -exportArchive` с `destination: upload` (App Store Connect API key) без fastlane; иначе fastlane pilot | Mac + Apple-аккаунт | §7.4 | fastlane на Mac |

### §9.2 Фазы и гейты

| Фаза | Содержание | Гейт (Done) | Размер | Без Mac |
|---|---|---|---|---|
| **0 · Решения и P0 cross-repo** | ответы владельца на блокирующие вопросы §10; C-1/C-2/C-3 в corp; B-1…B-3, B-12, B-17 в бэкенде (или принятые fallback'и); I-1/I-6 в infra; выбор Mac-варианта (O-2) | решения записаны в `.claude/plans/roadmap.md` шаблона; corp-PR слит; backend-PR слиты или fallback'и зафиксированы | S (шаблон) + M (соседи) | да |
| **1 · Спайки** | S-1…S-15 | у каждого спайка строка результата в roadmap; блокирующих «нет» не осталось | M | все, кроме S-1, S-10, S-15 (Mac) и S-12 (аккаунты) |
| **2 · Каркас и Linux-гейт** | из `spikes/android-gate` + JetBrains-раскладки: `settings.gradle.kts`, `build-logic` (11 плагинов), `shared/` пустой с таргетами, `androidApp/app` пустой, `iosApp/` из JetBrains-шаблона + `Packages/Pure`, `Makefile`, `scripts/tools/*`, `settings.json`, `.editorconfig`, `gradle.properties`, `.gate-fixtures/NF-01…NF-10`, `AGENTS.md` | `make check` зелёный на пустом каркасе ≤ 15 с тёплый; `make gate-selftest` — все NF красные; `make doctor` проходит; SKIP-блок печатается | M | да |
| **3 · `shared/`** | `core/*` (api, auth, config, requirements, push, deeplink, mock, observability, i18n, lib), `SharedContainer`/`PlatformDeps`, презентер-паттерн, `entity/{user,session,todo,device}`, `feature/*` презентеры, кодоген моделей + `gen.sh`/`gen-check.sh`, `FixturesTest`, `apiDump`, NF-11…NF-16 | `:shared:jvmTest` ≥ 80 %; `make kmp` зелёный; `apiCheck` зелёный; фикстуры всех сценариев валидны; презентеры всех 8 reference-фич покрыты сценарными тестами | L | да |
| **4 · Android** | `core/{designsystem,ui,platform,navigation,testing}`, `gen-tokens`, фичи `shell/auth/profile/todos/settings` + `demo`-flavor + `requirements`/`push` в `core`, Nav3, Hilt-биндинги, Roborazzi на все состояния, Konsist, `:lint`, deep links, `i18n-check`/`deeplinks-check`/`tokens-check` | `make check` и `GATE_RELEASE=1 make check` зелёные; скриншоты всех состояний; `make run-android` на эмуляторе — ручной прогон 8 фич; Maestro `e2e/common` на эмуляторе (если KVM) | L | да (эмулятор — при RAM/KVM) |
| **5 · iOS** | `Core/{DesignSystem,Routing,Persistence,Platform,SharedBridge}`, `Features/*` + umbrella, `TestSupport`, test plans, snapshot-тесты всех состояний, XCUITest smoke, `mac.sh`, `check-mac`, NF-20…NF-24, `mac-host.md` | `make check-full` зелёный; `check-mac` M1–M7; ручной прогон 8 фич на симуляторе; `swiftlint analyze` чист | L | **нет** — требует Mac (O-2); до Mac: `Pure`, archlint, SwiftLint, Swift-код по канону 02 без компиляции (не считается Done) |
| **6 · Обвязка и docs** | `CLAUDE.md`, 23 правила, 9 скиллов (+ `references/`), `docs/{README,getting-started,contributing}`, `docs/reference/*` (8 файлов + `strategy-mobile-canon/`), `docs/stack/README.md` + 15 `_templates`, `kinship.md` + `kin-diff`, `README.md` | dry-run на scratch-форке в трёх режимах: both / `--platform android` / `--platform ios` — `/init-service → /stack-deep-dive (mode A на reference-значениях) → /de-template`, после каждого `make check` зелёный и `gate-selftest` красный; `kin-diff` без WARN; `/scaffold` всех 8 вариантов создаёт компилируемый код | L | да (режим `--platform ios` проверяется только до `check-mac`) |
| **7 · Экосистема и первый релиз** | слияние C-*/B-*/I-*/S-* (те, что ещё не), `wellknown` на dev-VPS, бэкенд с B-17/B-1…B-3 задеплоен, `/release --stop 1` demo-приложения шаблона (org `stit`) → Gitea Release; `/release --stop 2` в RuStore (бесплатный аккаунт) и Play internal (аккаунт $25 + verification; O-5); TestFlight — при O-1; P-1 в stit-public | Release-страница со всеми ассетами; STORE-HANDOFF напечатан; RuStore-модерация demo-приложения пройдена (или зафиксирован отказ и `Z+1`); строки stit-public обновлены | M | STOP-1 Android — да; iOS — нет |
| **8 · Сопровождение** | политика обновлений (Kotlin/AGP/Compose BOM — ежемесячная проверка `make deps-report` + `libs.versions.toml`; Xcode — с каждым major, `.xcode-version`), backlog: `buildHealth` в гейт при поддержке AGP 9.x, Swift export при Beta, `sharedUi/` (CMP) по триггеру 03 R-*, извлечение `mobilekit`/`swiftkit` по §1.6, Baseline Profile + GMD-тесты, SQLDelight-шов | roadmap `## Backlog` заполнен; triggers зафиксированы | — | — |

Критический путь: 0 → 1 → 2 → 3 → 4 → 6 → 7 (Android-релиз) выполним целиком без Mac; 5 включается, как только появляется Mac-хост, и не блокирует остальное — но до фазы 5 `platforms: [android, ios]` в шаблоне помечается в README как «iOS — не верифицировано на устройстве/симуляторе», и `/de-template` для форков с `ios` требует `--ios-unverified`. Это честнее, чем ждать Mac, ничего не выпуская.

### §9.3 Что можно сделать до покупки Mac и что нет

| Можно (Linux) | Нельзя (Mac) |
|---|---|
| весь `shared/`, весь Android, гейт, обвязка, docs, strategy-канон, cross-repo PR'ы, STOP-1/STOP-2 для Android (RuStore, Play) | линковка `Shared.framework`, `iosSimulatorArm64Test`, компиляция `Core`/`Features`, snapshot-тесты, XCUITest, подпись, IPA, TestFlight, App Store |
| Swift `Pure` + SwiftLint + swift-format + archlint; Swift-код `Features` по канону 02 (без компиляции) | верификация того же Swift-кода |
| спайки S-2…S-9, S-11, S-13, S-14 | S-1, S-10, S-15 |

---
## §10 Вопросы владельцу

Сведены из 01 (8), 02 (9), 03 §9.2 (10), 04 §8.2 (12), 05 §11 (O-1…O-7), 06 §10 (12), 07 (O-1…O-10) — дубли схлопнуты, каждому дана рекомендация, по которой отчёт **уже** написан (то есть «да» ничего не меняет, «нет» — меняет указанную секцию). Колонка «Блокирует» — блокирует ли начало фазы (§9.2) или первый релиз.

| № | Вопрос | Рекомендация | Блокирует | Источник |
|---|---|---|---|---|
| **A · Форма и экосистема** | | | | |
| 1 | Один репо `mobile-template` (KMP-ядро + нативный UI) вместо обещанных `android-template` + `ios-template`? | **Да** (§1.2); roadmap-строки — §1.5 | **да** — фаза 0 (всё остальное) | 03 §7, brief |
| 2 | Имя форка `<org>/mobile`, sibling-checkout `+ mobile` | да | нет | §1.2 |
| 3 | Пресеты `companion \| consumer \| enterprise` как поле `service.md`, дефолт `companion`? | да (§1.3) | нет | brief |
| 4 | Kit-библиотеки (`mobilekit`/`swiftkit`) — отложить до триггера «второй форк + общий фикс»? | да (§1.6) | нет | brief |
| 5 | Compose Multiplatform UI (`sharedUi/`) — не в v1, только по триггеру 03 §7.2? | да | нет | 03 §9.2 |
| 6 | Sweep «число шаблонов» в экосистемных доках — одним PR на репо вместе с SSR («6 → 8»)? | да, после обоих решений | нет | SSR §11 |
| **B · Mac и Apple** | | | | |
| 7 | Mac mini M4 как build-host экосистемы (05 §6: 67–73 к₽): купить сейчас или после Android-релиза? | после фазы 4 (Android-релиз доказывает шаблон); до этого Android-first, iOS помечен «не верифицирован» | **да** — фаза 5 и iOS-часть релиза | 05 O-2, 06 §10.1 |
| 8 | Apple Developer Program без платежей из РФ (05 O-1): путь (иностранное юрлицо / партнёр / отложить)? | решить до фазы 5; шаблон от ответа не зависит | **да** — iOS-канал (TestFlight/App Store/ABM) | 05 O-1 |
| 9 | Xcode Cloud (25 ч/мес) как резерв Mac-хоста — документировать или не упоминать? | документировать как резерв в `mac-host.md`, не как путь | нет | 05 §6 |
| 10 | Политика версий toolchain'ов: latest (Swift 6.4 вышел 2026-09-14) или latest-1? | latest stable с пином (`.xcode-version`, `.swift-version`, toml) и 2-недельной выдержкой; alpha/beta — никогда (detekt 2.0-alpha, Swift export Alpha) | нет | 06 §10.6/§10.10, 02 |
| **C · Стек** | | | | |
| 11 | `minSdk 26`, `targetSdk 37`, iOS deployment target 18.0? | да (01 §1, 02 §1; дедлайны Play — 05 §1.2) | нет | 01/02 вопросы |
| 12 | DI: Hilt в `androidApp/`, ручной `SharedContainer` в `shared/` (без Koin/Metro)? | да (§2.4); Metro — триггер | нет | 03 §9.2, 06 §10.9 |
| 13 | Без БД в v1 (in-memory + `FileCache`, шов для SQLDelight/Room)? | да (§2.4); offline-first проекты — спайк SQLDelight | нет, если первый проект не offline-first | 01 (Room 3), 03 §8 |
| 14 | Полы покрытия: `shared` 80 / Android 70 / Swift `Pure` 70? | да (§4.8) | нет | 06 §10.2 |
| 15 | `GATE_RELEASE=1` (R8-сборка, +25 с) в `pre-push` и `/release`, не в ручном `make check`? | да (§4.5) | нет | 06 §10.3 |
| 16 | Konsist как архитектурный линтер (последний релиз 2024-12) — принять с fallback на detekt-правила? | да (06 §8.7; работает на Kotlin 2.4) | нет | 01 §R6 |
| 17 | Roborazzi-эталоны: порог различий + «эталоны пишет стандартное окружение»? | да; `changeThreshold` подобрать в S-6 | нет | 06 §10.8 |
| 18 | Ktor в `shared/` вместо Retrofit; Swift OpenAPI Generator не входит? | да (§2.5) | нет | 01, 07, 06 §10.11 |
| 19 | Модель экрана в отдельном чистом Swift-таргете (02 вопрос 5) — снимается: логика в презентерах `shared/`? | да (§1.4) | нет | 02 |
| 20 | Tuist/XcodeGen не используются; Compose Expressive выключен; Material 3 1.4? | да | нет | 02, 01 |
| **D · Контракт с бэкендом** | | | | |
| 21 | Bearer + refresh JSON как канон бэкенда для мобильных (B-1…B-3), cookie — переходный режим? | да (§2.5); fallback `AuthMode.Cookie` в шаблоне | нет для шаблона; **P0 для реального проекта** | 07 O-1 |
| 22 | Спека — закоммиченная копия + lock (не runtime-fetch, не сабмодуль)? | да (§2.5, 07 §2.1 B) | нет | 07 |
| 23 | Кодоген только моделей, клиенты руками (аналог решения SPA «рукописные контракты»)? | да | нет | 06 §10.11, 07 §2.5 |
| 24 | Certificate pinning выключен по умолчанию, шов задокументирован? | да | нет | 07 §1.6 |
| 25 | GlitchTip self-hosted как приёмник по умолчанию (Sentry SaaS — опция), пустой DSN = выключено? | да | нет | 05 §4, 07 §7 |
| 26 | `GET /api/app/requirements` в домене `app` бэкенда, feature-flags там же (не отдельный сервис)? | да (B-17) | нет | 07 §4.3 |
| 27 | `pushkit` в golibinfra (мульти-провайдер) vs локально в бэкенде проекта? | golibinfra (G-1) — переиспользуется всеми бэкендами | нет | 07 §6, 05 §2 |
| 28 | Паритет токенов с SPA `index.css` — сейчас или при первом общем продукте web+mobile? | при первом общем продукте (F-2); сейчас — `design/tokens.json` только в мобильном | нет | 07 §8 |
| **E · Дистрибуция и право** | | | | |
| 29 | Старт: RuStore + Play internal сразу или RuStore-only? | RuStore первым; Play-аккаунт ($25, D-U-N-S ~30 дней, developer verification до 2027) заводить параллельно и публиковать internal-трек, как только готов | нет | 05 O-5 |
| 30 | Владелец store-аккаунтов — заказчик (stit как admin) или stit? | заказчик; фиксируется в `distribution.md#accounts`; для demo-приложения шаблона — stit | **да** для STOP-2 реального проекта | 05 O-6 |
| 31 | `ANALYTICS=none` по умолчанию; AppMetrica — шов под `preset: consumer`? | да | нет | 05 O-7 |
| 32 | xtool (лицензия серая) — исключить? | да, исключить | нет | 05 O-3 |
| 33 | Порог crash-free для промоушена в production (STORE-HANDOFF): 99.5 % / 48 ч? | да; значение — поле `observability.md` | нет | 05 §7.4 |
| 34 | Demo-режим для App Review: встроенный `MockTransport` по флагу в prod-сборке + demo-аккаунт на бэкенде — оба? | да, оба (§2.7 фича 1) | нет | 05 §1.3, 07 §3 |
| 35 | Правовые тексты (политика, страница удаления, согласия — L-10…L-17): кто пишет и хостит? | заказчик/юрист; шаблон везёт плейсхолдеры и INFRA-HANDOFF I-6 | **да** для STOP-2 | 05 §5 |
| **F · Обвязка и процесс** | | | | |
| 36 | `AGENTS.md` — только указатель на `CLAUDE.md`? | да (§3.5) | нет | 04 §5 |
| 37 | Официальные Android-скиллы ставить `make setup` (не вендорить), телеметрия `android` CLI выключена? | да (§3.5) | нет | 01 §6, 06 §8.7 |
| 38 | Заимствования процесса Shopify (checkpoint-цикл, хэш плана, adversarial review) — менять `/plan` всей экосистемы? | не сейчас; мобильный добавляет только атом «Mac-verify»; остальное — отдельное экосистемное решение | нет | 03 §6.3 |
| 39 | Maestro `e2e/` в шаблоне (вне гейта) или только в доках? | в шаблоне, вне гейта (`make e2e`) | нет | 01 §7, 06 |
| 40 | Платные инструменты: ничего платного в шаблоне (SKIE — free, sentry-cli FSL — ок, fastlane — ок, Xcode Cloud — резерв)? | да | нет | 05, 07 |
| 41 | BDUI/server-driven UI — шов в v1 или «не проектируется»? | не проектируется (YAGNI); отдельный шаблон/пресет при реальном запросе | нет | 04 §7.1 |
| 42 | ОС «Аврора» и другие RU-платформы — вне шаблона? | вне | нет | 05 §1 |
| **G · Strategy** | | | | |
| 43 | Слой `mobile/<surface>/` с 9 kinds, замещающий `screens/` (не 6–7 kinds без `permissions`)? | 9 (§6.2): `permissions` нужен магазинам и compliance | нет | 07 O-7 |
| 44 | Канон-PR в strategy — до или после v1 шаблона? | после dry-run фазы 6, до фазы 7; до него — путь без PR | нет | §6.4 |

Блокирующих: **5** (№ 1, 7, 8, 30, 35); из них шаблон как таковой блокирует только № 1, остальные — iOS-половину или STOP-2 реального проекта.

---
## §11 Риски

Сведены риски формы и обвязки (риски платформ/магазинов/контракта — в 03 §9, 05 §11, 06 §10, 07 R-1…R-7; здесь только то, что порождает **это** решение о форме, или что меняет план §9).

| ID | Риск | Вероятность / удар | Митигация | Сигнал / владелец |
|---|---|---|---|---|
| RS-1 | Kotlin/Native перестаёт компилировать Apple-klib на Linux (регресс или изменение политики JetBrains) — рушится вся идея «65–80 % на Linux» | низкая / критический | fallback A через `subtree split` (§1.2); `make kmp` в гейте ловит первым; версии K/N пинятся | два подряд релиза Kotlin с падением `compileKotlinIosArm64` |
| RS-2 | Слепая зона Swift: код `Core`/`Features` пишется без компиляции до появления Mac, накапливает ошибки | высокая до фазы 5 / средний | `Pure` + SwiftLint + archlint на Linux; презентеры в `shared/` минимизируют Swift-логику; README-пометка «iOS не верифицирован»; `/de-template --ios-unverified` как явный долг | появление Mac → фаза 5 первым делом гонит `check-mac` по всему |
| RS-3 | iOS-канал недоступен из РФ (05 O-1: Apple Developer Program) — половина шаблона не доходит до магазина | средняя / высокий для iOS | Android-first; STOP-1 для iOS (IPA в Release) достижим без магазина; enterprise/ABM тоже требует аккаунт — путь через партнёра фиксируется в `distribution.md` | владелец, фаза 0 (№ 8) |
| RS-4 | Play: developer verification (2027) и ограничения RF-аккаунтов; RuStore — единственный гарантированный Android-канал | средняя / средний | RuStore primary (05); Play internal как второй; `distribution.md#deadlines` с датами; `/release` preflight печатает ближайшие дедлайны | 05 §1.2, ежеквартальная сверка |
| RS-5 | Ограничения Obj-C-экспорта/SKIE (generic'и, default-параметры, `inline`-классы) просачиваются в фасад и ломают Swift-эргономику | средняя / средний | правило `kmp-public-api` + `apiCheck` + Konsist; S-1 до кода фич; Swift export — триггер при Beta | `apiDump` diff в ревью |
| RS-6 | Рост времени гейта: два toolchain'а + contract-стадии; агент перестаёт запускать `make check` | средняя / высокий | `check-fast`; configuration cache; порядок дешёвое-первым; бюджет тёплого прогона ≤ 60 с фиксируется в фазе 4 и измеряется `gate-selftest`-отчётом | `make check` тёплый > 90 с |
| RS-7 | RAM: демоны Gradle+Kotlin ≈ 4.8 ГБ на общей машине агентов; эмулятор — ещё 4+ ГБ | высокая / средний | `doctor.sh` WARN/FAIL, `make clean` в конце сессии, `workers.max=4`, эмулятор вне гейта | `OutOfMemoryError: Metaspace`, swap |
| RS-8 | Качество кодогена моделей (дефекты спеки B-13…B-15; генератор мапит cookie в query) | высокая до фиксов бэкенда / низкий | models-only + пост-процессинг в `gen.sh`; клиенты руками; `FixturesTest` ловит расхождения; fallback — рукописные DTO | `gen-check` FAIL после обновления спеки |
| RS-9 | Недетерминизм Roborazzi/snapshot между машинами | средняя / низкий | toolchain-пин, порог различий (S-6), правило «эталоны пишет стандартное окружение» | ложные красные скриншоты |
| RS-10 | 23 правила раздувают контекст агента и начинают противоречить друг другу | средняя / средний | path-scoping (правило открывается только по своим путям), ≤ 60 строк, `contributing.md` таблица «кто enforce'ит» как единственный реестр; ревизия на фазе 6 dry-run | агент цитирует правило не по теме |
| RS-11 | Дрейф канона `mobile/` между копией в шаблоне и strategy (путь без PR) | средняя / низкий | `/init-service` сравнивает `docs/reference/strategy-mobile-canon/` с `../strategy/docs/projects/_template/mobile/` и печатает WARN с diff; канон-PR (S-1) закрывает риск | WARN в `/init-service` |
| RS-12 | Владелец-соло держит два toolchain'а (Kotlin/AGP/Compose ежемесячно; Xcode/Swift ежегодно с жёстким дедлайном SDK — апрель 2027 для iOS 27) | высокая / средний | политика обновлений в `runtime.md` и фазе 8; `deps-report` advisory; пины и `libs.versions.toml` как единственное место | `make deps-report` показывает > 2 minor отставания |
| RS-13 | Смена правил магазинов (targetSdk-дедлайны, privacy manifests, новые формы согласий) делает `compliance.md` устаревшим без сигнала | высокая / средний | датированные поля в `compliance.md`/`distribution.md`; `/release` preflight печатает дедлайны из `distribution.md#deadlines`; чек-лист L-* с датой проверки | отказ модерации |
| RS-14 | Gitea/Traefik: лимиты и таймауты на 100+ МБ ассеты (`MAX_FILES`, body-limit) | низкая / средний | S-11; загрузка по одному; C-4; Generic-registry как резерв | `publish-gitea.sh` FAIL |
| RS-15 | Связка Kotlin ↔ KSP (Hilt) отстаёт на минорную ветку; обновление Kotlin ломает Hilt | средняя / низкий | пары версий обновляются вместе (`libs.versions.toml`), Metro — триггер (§2.4) | `ksp` предупреждение о версии |
| RS-16 | Reference-фичи расходятся с reference-доменами бэкенда (нет общего CI) | средняя / низкий | `gen-check` WARN о дрейфе `../backend`; владелец гоняет `make check` шаблона после релизов go-backend-template; `FixturesTest` | WARN «backend drift» |
| RS-17 | `MockTransport` и фикстуры в prod-сборке (demo-режим для App Review) — утечка данных или сюрприз ревью | низкая / средний | фикстуры — синтетические (правило `mock-and-fixtures`), тест «demo не ходит в сеть», флаг из `requirements`; ревью-заметки описывают режим | ревью-вопрос, отчёт Sentry о сетевом вызове в demo |
| RS-18 | Форк-путь `--platform ios` оставляет Gradle iOS-команде, которая его не хочет | средняя / низкий | документированная альтернатива — прекомпилированный XCFramework в Gitea Release (`shared` как зависимость), путь к варианту В3 при росте; pure-Swift — вне шаблона (§2.5) | запрос от команды |
| RS-19 | Двухролевость слоя `mobile/` (композиция + карта кода) — карта пухнет и её перестают вести | средняя / средний | 9 kinds с чёткими колонками, `/mobile-audit` зрелость, `/scaffold` читает только stable; `screens.md` — единственный «большой» файл | draft-доля > 30 % на `make status` |

---

## Источники

Новые внешние (проверены точечно 2026-09-17; всё остальное — по отчётам 01–07, ссылки внутри них):

- Gitea — Configuration Cheat Sheet: секции `[repository.release]` (`FILE_MAX_SIZE` 2048, `MAX_FILES` 5), `[repository.upload]`, `[attachment]` — https://docs.gitea.com/administration/config-cheat-sheet
- Gitea — Package Registry overview: поддерживаемые реестры (в т. ч. Maven, Swift, Generic) — https://docs.gitea.com/usage/packages/overview
- Kotlin/binary-compatibility-validator README (0.18.2): klib-валидация (`klib { enabled = true }`, `strictValidation`, вывод дампов для неподдерживаемых хостом таргетов) — https://github.com/Kotlin/binary-compatibility-validator

Внутренние прецеденты, по форме которых написан отчёт (пути — в экосистеме `stit-core`):

- `corp-infra-template/docs/ecosystem.md` — семейство, контракты, Skill index, блок «Где это в экосистеме»
- `go-backend-template`: `CLAUDE.md`, `Makefile`, `.golangci.yml`, `docs/{README,getting-started,contributing}.md`, `docs/reference/*`, `docs/stack/README.md` + `_templates/`, `.claude/rules/*`, `.claude/skills/*` (`de-template`, `infra-handoff`, `init-service`, `plan`, `release`, `scaffold`, `smartdev`, `stack-deep-dive`, `triage-failure` + `references/`), `.claude/settings.json`, `.claude/agents/*`, `scripts/tools/status.sh`
- `spa-frontend-template`: `CLAUDE.md`, `README.md`, `package.json`, `docs/*`, `docs/stack/README.md` + `_templates/`, `.claude/rules/*`, `.claude/skills/*`, `.claude/settings.json`, `lefthook.yml`, `steiger.config.ts`; `docs/research/ssr/round2/13-template-v2.md`, `docs/research/ssr/00-synthesis.md` §9–§12
- `telegram-bot-template`: `CLAUDE.md`, `docs/stack/README.md`, `docs/reference/ecosystem.md`
- `business-strategy-template`: `docs/projects/_template/project/surface.md`, `_template/fsd/README.md` + формы, `_template/telegram-bot-flow/README.md` + формы, `_template/screens/README.md`, `docs/README.md`, `.claude/skills/{fsd-deep-dive,fsd-audit,telegram-bot-flow-deep-dive,cross-audit,project-audit}`
- `project-infra-template-compose`: `.env.example`, `Makefile` (`bump`)
- `stit-public`: `README.md`, `docs/roadmap.md`, `docs/templates.md`
- `mobile-template/docs/research/spikes/android-gate/*` (06), `spikes/swift-linux/.swiftlint.yml` (06)
