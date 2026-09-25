# Ecosystem

Место `<org>/mobile` в экосистеме stit — самодостаточная выжимка для этого репо. Канон карты
шаблонов и операционного флоу — `docs/reference/ecosystem.md` инфра-репо `<org>/infra` (форк
`project-infra-template-compose`; sibling-checkout — `../infra/docs/reference/ecosystem.md`).

## Шаблоны и форки

Шаблоны `stit-core/*` форкаются в репо клиентской орги `<org>/*`; форки одной орги лежат рядом
sibling-checkout'ами (`../<name>`).

| шаблон → форк | роль |
|---|---|
| `business-strategy-template` → `<org>/strategy` | design-time source of truth: бизнес, AJTBD, SWOT, карта гипотез, продукты (в т. ч. карта `mobile/<surface>/` этого приложения), ИТ-периметр |
| `project-infra-template-compose` → `<org>/infra` | runtime проекта (docker-compose на VPS) и **операционный хаб**: общий план, рассылка агентов по сиблингам, выкатка тегов (`make bump`) |
| `go-backend-template` → `<org>/backend` | Go-сервис |
| `spa-frontend-template` → `<org>/frontend` | React SPA (кабинет, всё за логином) |
| `ssr-frontend-template` → `<org>/site` | Astro SSR-сайт (индексируемый контент) |
| `telegram-bot-template` → `<org>/<bot>` | Telegram-бот (Python, aiogram) |
| **`mobile-template` → `<org>/mobile`** | **этот репо**: KMP-приложение Android + iOS; релиз — в магазины |

Библиотеки: `golibinfra` (Go-киты для Go-сервисов), `pylibinfra` (Python-киты; потребитель —
`telegram-bot-template`).

## Роль этого репо

Мобильный клиент проекта — один репо на обе платформы: KMP-ядро `shared/` + нативный UI
`androidApp/` (Compose) и `iosApp/` (SwiftUI). Релиз — в магазины своим `/release`: STOP-1 —
**подписанные артефакты в Gitea Release на теге**, STOP-2 — подача в каналы
([`release.md`](release.md)). Образа и compose-сервиса у приложения нет, поэтому выкатка тегов из
хаба (`make bump`) его не касается.

```
   <org>/strategy — design-time SoT: карта mobile/<surface>/ (12 kinds), /mobile-deep-dive
        │ read-only: /scaffold и /de-template
        ▼
   <org>/infra (хаб) ── задача, агент в ../mobile ──► ЭТОТ РЕПО ◄── прямой заход
                                                        │
        ├─ Bearer-auth, api/openapi.yaml, /api/app/requirements, /api/v1/devices ──► <org>/backend
        ├─ INFRA-HANDOFF: .well-known (assetlinks / AASA), SENTRY_DSN, API_BASE_URL ──► <org>/infra
        ├─ STOP-1 ──► Gitea Release <org>/mobile vX.Y.Z: AAB + APK + mapping / IPA + dSYM · checksums · release-notes · openapi.lock
        └─ STOP-2 ──► Play internal · RuStore модерация · TestFlight ──► магазины (человек по STORE-HANDOFF)
```

## Как сюда приходят

1. **Старт проекта — `<org>/strategy`.** Там проектируется продукт, в том числе карта
   `mobile/<surface>/` (`/mobile-deep-dive`); этот репо её только читает.
2. **Дальше задачи идут из хаба `<org>/infra`.** Сквозная задача — план-программа в
   `.claude/plans/` инфры; агент хаба приходит в `../mobile` с фазой, ведёт её планом здесь
   (`/plan`, `parent: infra:<slug>`) и работает **обвязкой этого репо** — `CLAUDE.md`,
   скиллы, гейт `make check`. Релиз в магазины — `/release` этого репо; деплой из инфры его не
   касается. Изменение дизайна по ходу — агентом из хаба в `../strategy`, скиллами strategy
   (отсюда — STRATEGY-HANDOFF).
3. **Прямой заход** в `<org>/mobile` — нормальный режим с той же обвязкой. Последствия для соседей
   оформляются handoff-блоком (`/infra-handoff`: INFRA-/BACKEND-/STRATEGY-HANDOFF) для хаба или
   соседа; задача пришла из хаба — блок уходит в ответ хабу.

## Что репо берёт и отдаёт

- **Берёт:** `<org>/strategy` — stable-записи карты `mobile/<surface>/` (read-only, cross-repo);
  `<org>/backend` — Bearer + refresh JSON (`POST /api/auth/refresh`, `X-Client`), спека
  `api/openapi.yaml` (копия + lock), `GET /api/app/requirements` (min-version, флаги),
  `PUT /api/v1/devices` (push); `<org>/infra` — требования через INFRA-HANDOFF: раздача
  `.well-known` (`assetlinks.json`, AASA) на хостах deep links, хосты `API_BASE_URL` per env,
  `SENTRY_DSN` per env, URL политики и веб-удаления аккаунта (как исполнить — решает инфра).
  Трекер ошибок — Sentry-совместимый (GlitchTip/Sentry) по `SENTRY_DSN` проекта
  ([`observability.md`](observability.md)).
- **Отдаёт:** подписанные артефакты в Gitea Release (STOP-1) и сборки в каналах магазинов
  (STOP-2); `<org>/backend` читает `shared-api.md` карты как ожидания клиента (advisory).

Fallback'и, пока соседи отстают: `AuthMode.Cookie` (бэкенд без Bearer-ветки), reference-спека
шаблона (нет `api/openapi.yaml` у соседа), 404 на `requirements` ⇒ «требований нет», копия
канона `mobile/` (strategy без него) —
[`strategy-mobile-canon/README.md`](strategy-mobile-canon/README.md).

## Sibling-checkout

Все форки одной орги клонированы рядом: `~/git/<org>/{strategy,infra,backend,frontend,site,<bot>,mobile}`.
Резолв — поля `strategy-path` (дефолт `../strategy`), `backend-path` (`../backend`), `infra-path`
(`../infra`) в `docs/stack/service.md`; сиблинг не склонирован — скиллы печатают next-step, не
падают. Соседи — read-only: изменения их поверхности едут handoff-блоками
(`.claude/rules/infra-impact.md`, skill `/infra-handoff`).

## Контракт релиза: artifact-in-release

STOP-1 — Gitea Release на аннотированном теге `vX.Y.Z` в `<org>/mobile` с ассетами: подписанные
AAB + APK (Android), IPA + dSYM.zip (iOS), `mapping.txt`, `checksums.sha256`, release-notes ru/en,
`openapi.lock`, `store-handoff.md`; символы загружены в трекер ошибок до публикации. STOP-2 —
подача в каналы (Play internal, RuStore moderation, TestFlight). Магазины, листинги и модерация —
за STOP (STORE-HANDOFF). Ключи подписи и store-credentials хранятся вне репозитория и приходят в
сборку через env (`STIT_KEYSTORE_*` и др.) на время `make release-*`. Gitea (`git.stit.tech`) —
git-хостинг форков орги; хост для `/release` выводится из `git remote get-url origin`. Лимиты Gitea:
`FILE_MAX_SIZE` 2048 МБ, `MAX_FILES` 5 на загрузку — ассеты загружаются по одному через API.

## Операционная модель

Mac-хост — ресурс орги, не сервис стека `<org>/infra`: `MAC_HOST` по SSH/Tailscale, `~/ci/<slug>/`
([`mac-host.md`](mac-host.md)); Xcode Cloud — резерв. CI-раннеров нет: гейт локальный, релиз —
с машины владельца и Mac-хоста.

Мобильному репо от экосистемы не нужны CI-раннеры, Maven-реестр до триггера kit-библиотек и
маршрут Traefik/домен/TLS для самого приложения — только `.well-known` на хостах deep links и
бэкенд. `TelemetrySink` — шов под OpenTelemetry, по умолчанию — Sentry SDK.

## Триггер извлечения kit-библиотек

`shared/…/core/**` и `iosApp/Packages/Core` — граница будущих `mobilekit` (KMP, Gitea Maven +
git-tag) и `swiftkit` (SwiftPM по тегу). Триггер: ≥ 2 форков в эксплуатации **и** первый фикс в
`core`, который нужно доставить в оба. До него граница поддерживается структурно (Konsist,
archlint: `core` не импортирует `feature`/`entity`), чтобы извлечение было механическим.

## Границы ответственности

Репо STOP'ает на Gitea Release и подаче в каналы. Промоушен в production, ответы модерации,
листинги — человек. Деплой-команды чужих репо (`make ship`, `make bump`, `ssh`, env-прокатка)
отсюда не запускаются. Изменение поверхности соседа — handoff-блок до claim «готово».
