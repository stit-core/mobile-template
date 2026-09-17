# Решение: мобильный шаблон экосистемы stit

Дата: 2026-09-17. Принято владельцем по итогам ресёрча (`00-synthesis.md`, отчёты 01–08).
Этот файл — форма принятого решения для следующей сессии (сборка репозитория). Что здесь не
названо, берётся из синтеза и отчётов; при расхождении приоритет — этот файл → `00-synthesis.md` →
`08-template-design.md` → отчёты потоков.

## 1. Что заводим

- **Один шаблон `mobile-template`** (репозиторий `stit-core/mobile-template`, форк → `<org>/mobile`,
  sibling-checkout `~/git/<org>/mobile`). Не `android-template` + `ios-template`, не kit-библиотеки.
- **Форма**: Kotlin Multiplatform-ядро `shared/` (модели, API-клиент, auth-сессия, репозитории,
  презентеры экранов, deep-link-роутер, mock-транспорт, конфиг, requirements, push-регистрация,
  observability-шов, i18n-ключи) + нативный UI: `androidApp/` (Jetpack Compose) и `iosApp/`
  (SwiftUI). Раскладка — JetBrains-дефолт 2026 (`shared/ + androidApp/ + iosApp/`), дерево —
  `08-template-design.md` §2.1.
- **Пресеты** `companion | consumer | enterprise` — поле `preset` в `docs/stack/service.md`,
  дефолт `companion`. Форк-пути «только Android» / «только iOS» — `/de-template --platform`.
- **Публичные строки**: две строки roadmap (`android-template`, `ios-template`) заменяются одной
  `mobile-template` (формулировки — синтез §8.1). Cross-repo правки — синтез §9, порядок обязателен;
  делаются отдельными сессиями в соседних репо, не в этой.

## 2. Стек (срез 2026-09-17; точные версии — `spikes/android-gate/gradle/libs.versions.toml`, поднимать только с проверкой)

| зона | решение |
|---|---|
| Kotlin / Gradle / AGP | Kotlin 2.4.20 · Gradle 9.7.1 · AGP 9.4.0 (встроенный Kotlin, без `kotlin-android`, без kapt) · KSP 2.3.12 · JDK-toolchain 21 |
| Android | compileSdk/targetSdk 37 · minSdk 26 · Compose BOM 2026.09.00 · Material 3 1.4 (без Expressive) · Navigation 3 · Hilt 2.60.1 (только `androidApp/`) · DataStore |
| shared | Ktor 3.6 (OkHttp / Darwin) · kotlinx-serialization 1.11 · kotlinx-coroutines · ручной composition root `SharedContainer` (без Koin/Metro) · без БД в v1 (in-memory + `FileCache` + `Settings` + `SecureStore`) · SKIE 0.10.14 для Swift-фасада · `apiDump` (BCV) |
| iOS | Xcode 27 · Swift 6.4 · iOS 18 deployment target · Swift 6 language mode + approachable concurrency (MainActor по умолчанию) · SwiftUI + `@Observable` · SwiftPM-пакеты + один `.xcodeproj` (buildable folders), без Tuist/XcodeGen · Swift Testing · swift-snapshot-testing · SwiftLint 0.65.1 + `swift format` |
| контракт API | закоммиченная копия `api/openapi.yaml` + `api/openapi.lock`; **модели генерируются** (openapi-generator 7.25.0, `kotlin` multiplatform, models-only), **клиенты пишутся руками** |
| auth | канон для мобильных — **Bearer + refresh в JSON** (`Authorization: Bearer`, `POST /api/auth/refresh {refresh_token}`, `X-Client`); режим совместимости `AuthMode.Cookie` для бэкенда без правок B-1…B-3; токены — `SecureStore` (Keystore/Keychain), access только в памяти; pinning выключен |
| mock/demo | JSON-фикстуры по сценариям `empty | populated | authenticated | error` + `MockTransport`; `demo`-flavor/схема; те же фикстуры в тестах; **demo-режим — reference-фича №1** (требование App Review) |
| observability | `sentry-android` / `sentry-cocoa` с пустым DSN по умолчанию (выключено); приёмник — self-host GlitchTip; логгер свой, `Log`/`print` запрещены; `X-Request-ID` на каждый запрос |
| push | мульти-провайдер RuStore + FCM (Android) + APNs (iOS); контракт `PUT /api/v1/devices`; payload data-first |
| дистрибуция | Android: RuStore (основной, API) + Google Play (free); iOS: App Store; `ANALYTICS=none`; без IAP; соцвход выключен; `AD_ID`/ATT не запрашивать |
| качество | Spotless + ktlint 1.8.0 (один форматер; `detekt-formatting` не подключать) · detekt 1.23.8 + compose-rules 0.4.28 · Android Lint `warningsAsErrors` + свой `:lint` · Konsist 0.17.3 · Robolectric 4.17 + Compose Test v2 · Roborazzi 1.74.0 · Kover 0.9.9 · `swift format` + SwiftLint (без `trailing_comma`) · `Tools/archlint` |

## 3. Гейт и STOP

- Один `make check` — Linux-полный (состав — `08-template-design.md` §4.2), завершается явным
  блоком «macOS-стадии не выполнены на этом хосте»; `make check-mac` по SSH; `make check-full`
  обязателен для `/release`; `make check-fast`; `make gate-selftest` на негативных фикстурах.
  Полы покрытия: `shared` 80 % · Android 70 % · Swift `Pure` 70 %. `GATE_RELEASE=1` (R8) — в
  pre-push и `/release`, не в ручном `make check`. Гейт обязан проверять klib-каталог на диске
  (`linkDebugFramework*` на Linux — SKIPPED с кодом 0).
- STOP-1 = Gitea Release на аннотированном теге `vX.Y.Z` с подписанными AAB+APK / IPA+dSYM.zip,
  `mapping.txt`, `checksums.sha256`, release-notes ru/en, `openapi.lock`, `store-handoff.md`;
  STOP-2 = подача в каналы (Play internal / RuStore модерация / TestFlight); дальше — человек по
  STORE-HANDOFF. Версия из тега: `versionCode = X·1e6 + Y·1e3 + Z`. Rollback не существует.

## 4. Обвязка

`CLAUDE.md` ≤ 200 строк (листинг — 08 §3.1) · 23 path-scoped правила (08 §3.2) ·
`.claude/settings.json` (08 §3.3) · 9 скиллов в форме Go/SPA-шаблонов (08 §3.4) · `AGENTS.md` —
указатель на `CLAUDE.md` · официальные Android-скиллы ставятся `make setup`, не вендорятся ·
`docs/stack` — 13 обязательных kinds + 2 условных (08 §5.2) · слой-мост в strategy
`mobile/<surface>/` с 9 kinds (08 §6), копия канона в `docs/reference/strategy-mobile-canon/`.

## 5. Порядок реализации

Фазы 0–8 — синтез §12 / 08 §9.2. **Критический путь до Android-релиза выполним без Mac**:
каркас + гейт → `shared/` → `androidApp/` → обвязка и docs → первый релиз в RuStore/Play internal.
Фаза iOS ждёт Mac-хост и Apple-аккаунт и не блокирует остальное; до неё iOS-половина помечается
«не верифицирована», `/de-template` для форков с `ios` требует `--ios-unverified`.

## 6. Принятые дефолты по открытым вопросам

Все рекомендации синтеза §10 принимаются как есть, в том числе: `minSdk 26` / `targetSdk 37` /
iOS 18; Hilt только в `androidApp/`; без БД в v1; полы покрытия 80/70/70; Konsist с fallback на
detekt; Roborazzi-эталоны с порогом различий; Ktor вместо Retrofit; Swift OpenAPI Generator не
входит; Bearer как канон бэкенда; кодоген только моделей; GlitchTip; `requirements` в домене
`app` бэкенда; `pushkit` в golibinfra; RuStore первым, Play параллельно; `AGENTS.md`-указатель;
ничего платного; BDUI не проектируется; «Аврора» вне; политика версий — latest stable с пином и
двухнедельной выдержкой, alpha/beta никогда.

## 7. Отложено (не блокирует сборку репозитория)

| вопрос | статус |
|---|---|
| Mac mini как build-host экосистемы | после Android-релиза; до него iOS не верифицируется |
| Оплата Apple Developer Program из РФ (юрлицо / партнёр) | до фазы iOS |
| Владелец store-аккаунтов | по умолчанию — заказчик, stit как admin; для demo-приложения шаблона — stit |
| Правовые тексты (политика ПДн, страница удаления аккаунта, согласия) | заказчик/юрист; шаблон везёт плейсхолдеры |
| Cross-repo P0 (corp-канон, бэкенд B-1…B-3/B-12/B-17, инфра `wellknown`/URL политики, strategy S-1…S-3, stit-public) | отдельные сессии в соседних репо; шаблон работает с fallback'ами (`AuthMode.Cookie`, reference-спека, 404 на `requirements`, путь без PR в strategy) |

## 8. Условия пересмотра

03 §7.2 (R-1…R-9) и синтез §11: регресс Kotlin/Native на Linux (два подряд релиза с падением
`compileKotlinIosArm64`) → откат к двум нативным репо через `subtree split`; JetBrains снижает
приоритет KMP или Swift export > 12 мес в Alpha → замораживание `shared/`; Google снимает
KMP-поддержку с Jetpack; профиль заказчиков сдвигается к «полировка не важна» → `sharedUi/`
(Compose Multiplatform) по умолчанию; рынок CMP > 50 вакансий; «эффект Shopify» на нашем
масштабе (доля коммитов, чинимых после `check-mac`).
