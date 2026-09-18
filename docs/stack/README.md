# Stack-артефакты

Дизайн платформы мобильного приложения — **первичный source of truth** и **вход для
`/de-template`**: 14 обязательных + 2 условных singleton-артефакта (по одному на kind),
заполняются через `/stack-deep-dive`. Карта продукта (экраны, фичи, сущности, точки входа,
permissions, системные действия, виджеты, фоновые задачи) живёт в соседнем `<org>/strategy` —
слой `mobile/<surface>/`
([`../reference/strategy-mobile-canon/README.md`](../reference/strategy-mobile-canon/README.md));
здесь — **платформа и её значения**: всё, что меняется при смене магазина, SDK, хоста или
аккаунта, а не продукта.

## Kinds

| # | kind | файл | обязателен | что описывает | волна |
|---|---|---|---|---|---|
| 1 | `service` | `service.md` | да | идентичность: `project-slug`, имя, `platforms`, `preset`, `strategy-path`, `backend-path`, `surface`, `applicationId`/bundle id per env, схема deep link, языки, владелец аккаунтов | W1 |
| 2 | `runtime` | `runtime.md` | да | toolchain'ы (Kotlin/AGP/Gradle/JDK, Xcode/Swift), `minSdk`/`targetSdk`/deployment target, KMP-таргеты, Swift-concurrency, Mac build-host, политика обновлений | W2 |
| 3 | `api` | `api.md` | да | источник спеки, копия + lock, генератор, конверт/ошибки/заголовки, совместимость, mock | W3 |
| 4 | `auth` | `auth.md` | да | `mode: bearer\|cookie\|none`, TTL, ротация, `SecureStore`, сессии, биометрия, pinning, удаление аккаунта | W3 |
| 5 | `deep-links` | `deep-links.md` | да | хосты/схемы per env, отпечатки, пути → `Route`, `wellknown` | W4 |
| 6 | `push` | `push.md` | да (может быть `providers: []`) | провайдеры per flavor, регистрация, каналы/категории, permission-flow, payload | W4 |
| 7 | `observability` | `observability.md` | да | приёмник, DSN per env, PII, логи, символы, пороги качества | W5 |
| 8 | `styling` | `styling.md` | да | токены (DTCG-имена), тема, компоненты, a11y, **i18n** | W5 |
| 9 | `distribution` | `distribution.md` | да | каналы per платформа, STOP-1/STOP-2, аккаунты, модерация, release-notes, App Review notes, дедлайны | W6 |
| 10 | `signing` | `signing.md` | да | инвентарь ключей, custody (Vaultwarden), отпечатки, ротация | W6 |
| 11 | `compliance` | `compliance.md` | да | собираемые данные, permissions с обоснованиями, privacy manifests, 152-ФЗ, рейтинг, шифрование, правовые документы, чек-лист L-1…L-17 | W6 |
| 12 | `dependencies` | `dependencies.md` | да | библиотеки/SDK: назначение, лицензия, privacy manifest, KMP-совместимость; швы | W7 |
| 13 | `background` | `background.md` | да (может быть без задач) | таблица «`timing` → механизм» Android/iOS, реестр фоновых задач, идентификаторы, лимиты и разрешения, App Group | W4 |
| 14 | `config` | `config.md` | да (rollup) | build-time / release-time / runtime per env, `requirements`, флаги, release-checklist | W8 |
| 15 | `analytics` | `analytics.md` | при `preset: consumer` или `ANALYTICS ≠ none` | провайдер, реестр событий, согласие, `AD_ID`/ATT | W4 |
| 16 | `payments` | `payments.md` | при монетизации | RuStore Pay / Play Billing / StoreKit 2: продукты, восстановление, валидация, правила 3.1.x | W4 |

Виджеты и системные действия своего kind'а не имеют: платформенных значений per env у них нет, а
то, что есть, лежит по адресам — кнопки категорий и локальные категории в `push.md`, данные на
экране блокировки в `compliance.md`, Glance и AppFunctions в `dependencies.md`, App Group id и
bundle id расширения в `service.md`/`config.md`. Раскладка по коду — `../reference/strategy-mapping.md`.

Каждый артефакт обязан иметь секцию **`## Generates`** — что из него родится в коде, конфиге,
скриптах и handoff-блоках. Это контракт для `/de-template` и `/scaffold`: маппинг живёт рядом с
источником, не отдельным файлом. `navigation` — не kind: платформенная навигация константна
([`../reference/architecture.md`](../reference/architecture.md)), инвентарь маршрутов — в карте
strategy (`screens.md`, `entry-points.md`).

## Frontmatter

```yaml
id: stack.deep-links   # stack.<name>
kind: deep-links       # один из 16
name: deep-links       # == basename файла без .md
status: draft          # draft → stable (по чек-листу /stack-deep-dive, повышает архитектор)
```

## Волны `/stack-deep-dive` (inside-out)

W1 `service` → W2 `runtime` → W3 `api` → `auth` → W4 `deep-links` → `push` → `background`
(→ `analytics` → `payments` по пресету) → W5 `styling` → `observability` → W6 `distribution` →
`signing` → `compliance` → W7 `dependencies` → W8 `config` (rollup всех env-значений).

## Cross-kind-инварианты

Проверяет `/stack-deep-dive` в конце волны и Gate `/de-template`:

1. Каждый провайдер `push.md#providers` имеет env в `config.md`, SDK в `dependencies.md` и
   privacy manifest в `compliance.md`.
2. Каждый хост `deep-links.md` присутствует в `config.md` per env и в INFRA-HANDOFF; отпечатки в
   `deep-links.md` == отпечатки в `signing.md` (upload и app-signing — оба).
3. Каждая permission карты `permissions.md` (или манифеста) имеет строку в `compliance.md` с
   обоснованием.
4. `auth.md#mode = bearer` ⇔ спека в `api.md` содержит `POST /api/auth/refresh`; иначе —
   открытый B-1…B-3 в BACKEND-HANDOFF, `stable` невозможен без явного `blocked-by`.
5. `distribution.md#channels` содержит только платформы из `service.md#platforms`;
   `ios ∈ platforms` ⇒ `runtime.md#mac-host` заполнен или `blocked-by: Mac`.
6. `observability.md#receiver ≠ none` ⇒ `SENTRY_DSN` per env в `config.md` и символы в
   `release-preflight`.
7. Условные kinds существуют ⇔ `service.md#preset`/флаги их требуют (лишние — удалить,
   недостающие — создать из `_templates`).
8. Каждая задача `background-tasks.md` карты имеет строку в `background.md`; идентификаторы там
   == `BGTaskSchedulerPermittedIdentifiers` в `Info.plist` и id планировщика Android; `result:
   notification:<slug>` имеет локальную категорию в `push.md`; `continuous` и точные будильники
   имеют строку в `compliance.md`. Виджет с `platforms ∋ ios` требует App Group id в `service.md`.

Для `platforms` без iOS iOS-секции kinds **отсутствуют** (не «пустые»); аналогично для Android.

## Workflow

1. `/init-service` копирует `_templates/<kind>.md` → `docs/stack/<kind>.md` с значениями
   идентичности (или `/stack-deep-dive <kind>` заводит по одному).
2. `/stack-deep-dive` — волнами вопросов; после каждой правки — чек-лист `draft → stable`
   (6 пунктов, `.claude/skills/stack-deep-dive/references/draft-stable.md`).
3. Промоушн `status: draft → stable` — по подтверждению архитектора.
4. `/de-template` читает stable-артефакты и разворачивает фундамент по `## Generates`;
   `/scaffold` читает `service.md` (резолв соседей) и kinds по вариантам.

## Gate

`/de-template` не запускается, пока все обязательные kinds не `stable`, условные — по пресету,
`## Generates` непусты, ни одного `blocked-by`, инварианты 1–8 выполнены. Точечная правка
стабильного артефакта (новый хост, провайдер) — прямой `Edit` + rollup в `config.md` + handoff.
