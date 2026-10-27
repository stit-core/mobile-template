<div align="center">

# mobile-template

### Android + iOS на общем Kotlin-ядре, нативный UI, гейт на Linux

Шаблон мобильного приложения экосистемы stit: Kotlin Multiplatform-ядро `shared/`
(модели, API-клиент, auth, презентеры, deep links, mock-режим) + нативный UI —
`androidApp/` (Jetpack Compose) и `iosApp/` (SwiftUI). Логика тестируется на JVM,
экраны — скриншотами, релиз — подписанные артефакты в Gitea Release; вы пишете
экраны по карте продукта, не обвязку.

[![License](https://img.shields.io/badge/license-Apache--2.0-2563eb)](LICENSE)
[![Часть stit](https://img.shields.io/badge/stit-открытые_шаблоны-6366f1)](https://github.com/stit-core)
[![Kotlin](https://img.shields.io/badge/Kotlin_2.4-KMP-7f52ff)](#что-внутри)
[![Android](https://img.shields.io/badge/Android-AGP_9.4_%2B_Compose-3ddc84)](#что-внутри)
[![iOS](https://img.shields.io/badge/iOS-Swift_6.4_%2B_SwiftUI-0a84ff)](#что-внутри)
[![Gate](https://img.shields.io/badge/gate-make_check_on_Linux-22c55e)](#команды)

</div>

> Разработка идёт в приватном Gitea (`git.stit.tech/stit-core/mobile-template`), публичное зеркало — [github.com/stit-core/mobile-template](https://github.com/stit-core/mobile-template). Как с ним работать вне инфраструктуры stit — в [профиле stit-core](https://github.com/stit-core).

> **Статус:** шаблон в разработке и ещё ни разу не тестировался.

---

## Место в экосистеме

```
business-strategy-template (<org>/strategy)   ← карта продукта: слой mobile/<surface>/ (12 kinds)
        │  /scaffold и /de-template читают stable-записи
        ▼
mobile-template (<org>/mobile)  ← вы здесь ──► <org>/backend (Bearer-auth, openapi.yaml, requirements, devices)
        │                                  ──► <org>/infra (хаб; INFRA-HANDOFF: .well-known — assetlinks / AASA, SENTRY_DSN)
        │  STOP-1: Gitea Release vX.Y.Z (AAB + APK + mapping · IPA + dSYM · checksums · release-notes)
        ▼
RuStore · Google Play · App Store   ← STOP-2: подача в каналы; модерация и промоушен — человек
```

Задачи сюда обычно приносит агент из операционного хаба `<org>/infra` — там общий план
проекта; прямой заход в репо — тот же режим с той же обвязкой. Релиз в магазины — свой
`/release`: деплой из инфры приложения не касается. Трекер ошибок — Sentry-совместимый по
`SENTRY_DSN` проекта. Подробнее — [`docs/reference/ecosystem.md`](docs/reference/ecosystem.md).

**Один репо — обе платформы.** Общее Kotlin-ядро даёт агенту на Linux проверяемую работу
над iOS-половиной без Mac: `shared/` компилируется в klib под iOS-таргеты, тестируется в
`jvmTest`; на Mac остаётся только SwiftUI-слой (`make check-mac` по SSH). Android-only /
iOS-only форки — `/de-template --platform`, пресеты `companion | consumer | enterprise` — поле
в `docs/stack/service.md`.

## Использовать как шаблон

В Gitea помечен `template: true`. **Полный путь** от форка к своему проекту —
[`docs/getting-started.md`](docs/getting-started.md): форк → `/init-service` → карта в
`<org>/strategy` → `/stack-deep-dive` → `/de-template` → handoffs → первый `/release`.
Android-first: до релиза в RuStore/Play всё выполнимо на Linux; **Mac build-host —
предусловие iOS-ветки** ([`docs/reference/mac-host.md`](docs/reference/mac-host.md)).

Быстрый старт (чтобы собралось):

```bash
# 1. Use this template → <org>/mobile, склонировать рядом с <org>/{strategy,backend,infra}
# 2. окружение: JDK 21, Android SDK, Swift 6.4 — пути через env (или local.properties)
export JAVA_HOME=… ANDROID_HOME=… SWIFT_TOOLCHAIN=…
# 3. хуки, Android-скиллы, прогрев демона; зелёный гейт с явным SKIP-блоком macOS-стадий
make setup && make check
# 4. онбординг
claude                 # → /smartdev → /init-service
```

Пакет, `applicationId`/bundle id, конфиги и удаление reference-фич делает флоу
`/init-service` → `/de-template` по заполненным `docs/`. Не правьте фундамент руками до этого.

## Что внутри

- **`shared/`** — KMP: Ktor 3.6 + kotlinx-serialization, модели из `api/openapi.yaml`
  (кодоген, models-only), Bearer + refresh (`SecureStore`), презентеры `StateFlow<State>` +
  `dispatch(Intent)`, `DeepLinkParser`, `MockTransport` с фикстурами, `SharedContainer` без
  DI-фреймворка; плоский фасад для Swift (`apiDump`).
- **`androidApp/`** — AGP 9.4 (встроенный Kotlin), Compose BOM 2026.09, Material 3, Navigation 3,
  Hilt; модули `app`, `core/*`, `feature/*`, `lint`; flavors `dev | staging | prod | demo`;
  convention-плагины `stit.*` в `build-logic/`.
- **`iosApp/`** — Xcode 27, Swift 6.4, SwiftUI + `@Observable`, SwiftPM local packages
  (`Pure` и `Tools/archlint` собираются на Linux); один `.xcodeproj`, xcconfig per схема.
- **`design/`** — `tokens.json` (DTCG) → `make gen-tokens` → `Tokens.kt`/`Tokens.swift`;
  **`e2e/`** — Maestro-флоу против demo-сборки (`make e2e`).
- **Reference-фичи** — `demo` (mock-режим для App Review), `shell`, `requirements`, `auth`
  (с удалением аккаунта), `profile`, `todos` (deep link), `settings`, `push` — учебные образцы;
  `profile`/`todos` удаляет `/de-template`.
- **Качество** — Spotless/ktlint, detekt + compose-rules, Android Lint (`warningsAsErrors`),
  Konsist, Robolectric + Compose test v2, Roborazzi, Kover (80 / 70 %), BCV; swift-format,
  SwiftLint, archlint (граф SwiftPM), Swift Testing; паритет токенов / строк / deep links и
  поверхностей вне экранов (`tokens-check`, `i18n-check`, `deeplinks-check`,
  `capability-check`); негативные фикстуры `make gate-selftest`.
- **Релиз** — версия из тега `vX.Y.Z`, подписанные AAB/APK (Linux) и IPA (Mac), символы в
  трекер ошибок, Gitea Release, подача в RuStore / Play / TestFlight, STORE-HANDOFF.

## Команды

`make help` — полный список по группам; concise-by-default (`OK:`/`FAIL:` + лог в
`build/gate-logs/`). Канонический gate — **`make check`**. Гайд агента —
[`CLAUDE.md`](CLAUDE.md); точка входа в сессию — `/smartdev`.

```bash
make doctor          # JDK 21, Android SDK, Swift 6.4, RAM/диск, MAC_HOST
make check-fast      # Swift-формат + SwiftLint + Kotlin-формат + detekt + shared jvmTest — цикл «пока пишу»
make build           # dev+demo debug APK (demo — MockTransport, без сети); make e2e — Maestro против demo
make check           # gate, Linux-полный; macOS-стадии перечислены SKIP-блоком
make check-mac       # macOS-стадии по SSH (MAC_HOST); check-full = check + check-mac
make gate-selftest   # негативные фикстуры обязаны краснеть
make release-preflight TAG=vX.Y.Z && make release-android TAG=vX.Y.Z   # skill /release
```

## Документация

Карта — [`docs/README.md`](docs/README.md):
- Потребителю — [`getting-started.md`](docs/getting-started.md); контрибьютору —
  [`contributing.md`](docs/contributing.md).
- Reference — [architecture](docs/reference/architecture.md) · [release](docs/reference/release.md)
  · [distribution](docs/reference/distribution.md) · [observability](docs/reference/observability.md)
  · [mac-host](docs/reference/mac-host.md) · [ecosystem](docs/reference/ecosystem.md).
- Design SoT — карта `mobile/<surface>/` в соседнем strategy-репо
  ([канон](docs/reference/strategy-mobile-canon/README.md)) · [`docs/stack/`](docs/stack/README.md).

## Часть экосистемы stit

Это один из открытых шаблонов stit (Apache-2.0) — фундамент «-1 уровня»,
берите бесплатно и владейте. Карта всех шаблонов и точка входа —
**[витрина stit](https://github.com/stit-core)**. Нужна
команда, стандарт или поддержка — **[пишите в Telegram @gommgo](https://t.me/gommgo)**.

## Лицензия

**Apache-2.0** — см. [`LICENSE`](LICENSE).
