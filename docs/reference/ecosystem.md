# Ecosystem

Место шаблона в экосистеме stit: **7 форкаемых шаблонов** `stit-core/` (`corp-infra-template`,
`project-infra-template-compose`, `go-backend-template`, `spa-frontend-template`,
`telegram-bot-template`, `business-strategy-template`, `mobile-template`) + 3 общие библиотеки
`stit-core/golibinfra` (Go-kits), `stit-core/pylibinfra` (Python-kits, `tgbotkit`) и
`stit-core/ui-lib` (`@stit/ui`, frontend design-system). Корп control-plane — репо
`corp-infra-template` (managed = инстанс на `*.stit.tech`; self-hosted = форк `<org>/infra`).
Канон контрактов и операционной модели агентов — `corp-infra-template/docs/ecosystem.md`.

## Роль

Шаблон мобильного приложения — один репо на обе платформы: KMP-ядро `shared/` + нативный UI
`androidApp/` (Compose) и `iosApp/` (SwiftUI). Форкается в `<org>/mobile`. STOP — **подписанные
артефакты в Gitea Release на теге** (`docs/reference/release.md`); образа и compose-сервиса у
него нет, в оркестрацию `<org>/infra` он не входит.

```
   <org>/strategy (← business-strategy-template)  — design-time SoT: карта mobile/<surface>/ (12 kinds)
        │ side-input: читают /scaffold и /de-template (рантайм не зависит)
        ▼
   ЭТОТ РЕПО ─── Bearer-auth, api/openapi.yaml, /api/app/requirements, /api/v1/devices ──► <org>/backend
        │                                                                                     ▲
        │ deep links: .well-known (assetlinks / AASA) ──► <org>/infra (сервис wellknown) ──────┘ (деплой)
        │
        ├──STOP-1──► Gitea Release <org>/mobile vX.Y.Z: AAB + APK + mapping / IPA + dSYM · checksums · release-notes · openapi.lock
        └──STOP-2──► Play internal · RuStore модерация · TestFlight ──► магазины (человек по STORE-HANDOFF)

   corp control-plane: Gitea (Release), Vaultwarden (mobile/<org>/<app>), ntfy (releases-<org>), GlitchTip/Sentry
```

## Зависит от / используется

- **Зависит от:** `<org>/backend` — Bearer + refresh JSON (`POST /api/auth/refresh`, `X-Client`),
  спека `api/openapi.yaml` (копия + lock), `GET /api/app/requirements` (min-version, флаги),
  `PUT /api/v1/devices` (push); `<org>/infra` — сервис `wellknown` (`assetlinks.json`, AASA),
  хосты `API_BASE_URL` per env, `SENTRY_DSN`, URL политики и веб-удаления аккаунта;
  `<org>/strategy` — карта `mobile/<surface>/`; corp-контур — Gitea, Vaultwarden, ntfy, GlitchTip.
- **Используется:** магазинами (RuStore, Google Play, App Store) и пользователями; `<org>/backend`
  читает `shared-api.md` карты как ожидания клиента (advisory).
- **Зависит (design-time) от:** `<org>/strategy` — `/scaffold` и `/de-template` читают stable-записи
  cross-repo, read-only. Соседа нет — путь без PR через
  [`strategy-mobile-canon/README.md`](strategy-mobile-canon/README.md).

Fallback'и, пока соседи отстают: `AuthMode.Cookie` (бэкенд без Bearer-ветки), reference-спека
шаблона (нет `api/openapi.yaml` у соседа), 404 на `requirements` ⇒ «требований нет», копия
канона `mobile/` (strategy без него).

## Sibling-checkout

Все форки одной org клонированы рядом: `~/git/<org>/{strategy,infra,backend,frontend,<bot>,mobile}`.
Резолв — поля `strategy-path` (дефолт `../strategy`), `backend-path` (`../backend`), `infra-path`
(`../infra`) в `docs/stack/service.md`; сиблинг не склонирован — скиллы печатают next-step, не
падают. Соседи — read-only: изменения их поверхности едут handoff-блоками
(`.claude/rules/infra-impact.md`, skill `/infra-handoff`).

## Контракт с корп-инфрой: artifact-in-release

Мобильный клиент не имеет образа и compose-сервиса. STOP-1 — Gitea Release на аннотированном теге
`vX.Y.Z` в `<org>/mobile` с ассетами: подписанные AAB + APK (Android), IPA + dSYM.zip (iOS),
`mapping.txt`, `checksums.sha256`, release-notes ru/en, `openapi.lock`; символы загружены в
GlitchTip/Sentry до публикации. STOP-2 — подача в каналы (Play internal, RuStore moderation,
TestFlight). Магазины, листинги и модерация — за STOP (STORE-HANDOFF). Ключи подписи и
store-credentials — Vaultwarden, коллекция `mobile/<org>/<app>`; уведомления — ntfy-топик
`releases-<org>`. Лимиты Gitea: `FILE_MAX_SIZE` 2048 МБ, `MAX_FILES` 5 на загрузку — ассеты
загружаются по одному через API.

## Операционная модель

`<org>/infra` — операционный хаб проекта; агент, пришедший сюда, работает **обвязкой этого репо**
(`CLAUDE.md`, `make`-вербы, скиллы, гейт `make check`). Прямой заход разработчика сюда — нормальный
режим. Mac-хост — ресурс org, не сервис control-plane: `MAC_HOST` по SSH/Tailscale,
`~/ci/<slug>/` (`docs/reference/mac-host.md`); Xcode Cloud — резерв. CI-раннеров нет: гейт локальный,
релиз — с машины владельца и Mac-хоста.

## Self-hosted путь

Gitea и Vaultwarden на managed-пути — хостинг stit (`git.stit.tech`), на self-hosted — форк
`corp-infra-template`. Хост Gitea выводится из `git remote get-url origin` при `/release`; архитектура
не меняется. Вендор-лока нет.

## Что мобильный репо не требует от экосистемы

Ни CI-раннеров, ни OTLP/Grafana (GlitchTip достаточно; `TelemetrySink` — шов), ни Maven-реестра до
триггера kit-библиотек, ни маршрута Traefik/домена/TLS для приложения — только для `wellknown` и
бэкенда.

## Триггер извлечения kit-библиотек

`shared/…/core/**` и `iosApp/Packages/Core` — граница будущих `mobilekit` (KMP, Gitea Maven +
git-tag) и `swiftkit` (SwiftPM по тегу). Триггер: ≥ 2 форков в эксплуатации **и** первый фикс в
`core`, который нужно доставить в оба. До него граница поддерживается структурно (Konsist,
archlint: `core` не импортирует `feature`/`entity`), чтобы извлечение было механическим.

## Границы ответственности

Репо STOP'ает на Gitea Release и подаче в каналы. Промоушен в production, ответы модерации,
листинги — человек. Деплой-команды чужих репо (`make ship`, `ssh`, env-прокатка) отсюда не
запускаются. Изменение поверхности соседа — handoff-блок до claim «готово».
