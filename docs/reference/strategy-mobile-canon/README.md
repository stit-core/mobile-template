# Артефакты мобильного приложения

Слой композиции и карты кода **мобильной поверхности**, партиционированный по поверхности
(surface): из аналитических артефактов `docs/projects/<project-slug>/interface/<surface>/` и
доменов `services/*/domains/` синтезируется карта приложения в родных понятиях мобильных
платформ — **сущность (entity) · экран (screen) · сценарий (flow) · фича (feature) · оболочка
(shell) · точка входа (entry-point) · разрешение (permission) · контракт данных (shared-api) ·
сквозное (shared-lib)**. Для поверхности с `type: mobile` из реестра `surface.md` этот слой встаёт
**на место `screens/<surface>/`** — так же, как `telegram-bot-flow/<surface>/` для `type: bot`;
слоя `fsd/` у мобильной поверхности нет. Этот файл — **канон слоя**; формы девяти файлов лежат
рядом в `_template/`. Данные каждого проекта живут в
`docs/projects/<project-slug>/mobile/<surface>/`.

Источник правды канона — `business-strategy-template` (`docs/projects/_template/mobile/`);
эта копия в `mobile-template` (`docs/reference/strategy-mobile-canon/`) — путь без PR для
strategy-форков, у которых канона ещё нет: `/init-service` печатает команду копирования.

## Воркфлоу

```
interface/<surface>/ + services/*/domains/ → синтез → mobile/<surface>/ → код приложения
 (бизнес, домены)                                      (карта: сущности,      (<org>/mobile — форк mobile-template:
                                                        экраны, флоу, фичи,    shared/ + androidApp/ + iosApp/)
                                                        входы, permissions)
```

1. В `interface/<surface>/` заведены ситуации, блоки, роли и данные мобильной поверхности;
   в `services/*/domains/` — агрегаты, use-case'ы, события (если бэкенд есть).
2. Через `/mobile-deep-dive <slug> <surface>` заводятся и углубляются записи девяти файлов;
   каждая ссылается на источник через `slug`. Консистентность проверяет `/mobile-audit`;
   сквозные рёбра — `/cross-audit` (углы `surface-mobile`, `mobile-domain`, `mobile-service`,
   `mobile-web` — пути deep links ↔ `pages.path` web-поверхности того же продукта).
3. Stable-записи читает код-репо `<org>/mobile`: `/scaffold` и `/de-template` разворачивают их
   в три места — `shared/…/{entity,feature}/<slug>/`, `androidApp/feature/<slug>/`,
   `iosApp/Packages/Features/Sources/<Slug>Feature/` — и сами выводят пути из kind + slug.
   Имена файлов, Gradle-модули, SwiftPM-таргеты и библиотеки — забота код-репо, не слоя.

## 9 kind

| kind | файл | один-в-одно про |
|---|---|---|
| [`entity`](_template/entities.md) | `entities.md` | вещь с данными: источник в домене, поля, кэш-политика |
| [`screen`](_template/screens.md) | `screens.md` | экран: ключ навигации, вход, состояния, сущности, платформы |
| [`flow`](_template/flows.md) | `flows.md` | навигационный сценарий: шаги по экранам, выход, ветки ошибок |
| [`feature`](_template/features.md) | `features.md` | действие пользователя: экраны + сущности + операции API, где живёт логика |
| [`shell`](_template/shell.md) | `shell.md` (singleton) | каркас: корневая навигация, auth-gate, оверлеи, offline, тема/язык, правовые документы |
| [`entry-point`](_template/entry-points.md) | `entry-points.md` | как в приложение попадают извне: deep link, push, shortcut, share, widget, demo |
| [`permission`](_template/permissions.md) | `permissions.md` | разрешение ОС с обоснованием, моментом запроса и fallback'ом |
| [`shared-api`](_template/shared-api.md) | `shared-api.md` | ресурсы бэкенда и сценарии данных для mock/demo |
| [`shared-lib`](_template/shared-lib.md) | `shared-lib.md` | локали, ключи ошибок, логирование, флаги, события аналитики |

Порядок проектирования — **inside-out**: entities → screens → flows → features → shell →
entry-points → permissions → shared-api → shared-lib.

## Двухролевость слоя

Слой одновременно **композиция** (экраны, флоу, оболочка — что видит пользователь) и **карта
кода** (фичи, сущности, точки входа — из чего `/scaffold` выводит пути). Платформенное
(Navigation 3 / `NavigationStack`, convention-плагины, версии) — не предмет слоя; оно живёт в
`docs/stack/` код-репо. Правило разграничения: **strategy описывает продукт и его карту; stack
— платформу и её значения**; меняется при смене магазина/SDK/хоста — stack, при смене продукта
— strategy.

## Язык контракта

Структурные идентификаторы, которые читает код-репо, — **английские** и не переименовываются:

- `key` экрана — PascalCase (`TodoDetail`) → `Route.TodoDetail`, `TodoDetailScreen.kt`,
  `TodoDetailView.swift`;
- `slug` сущности/фичи/флоу/permission — kebab-case (`delete-account`) → `DeleteAccountPresenter` в
  `feature/<group>/` (группа экранов, например `auth`);
- push-категория — UPPER_SNAKE (`ORDER_STATUS`);
- `states` экрана — из словаря `loading, empty, populated, error, offline, permission-denied,
  not-found, update-required, maintenance`.

Всё остальное — **русский контент**: названия, описания, rationale, тексты состояний.

## Принципы оформления

Один файл — одна таблица сущностей одного типа (`shell.md` и `shared-lib.md` держат секции по
канону своего файла). H1 = имя файла = kind, frontmatter не нужен. Первые три колонки везде —
`slug`/`key` · `name` · `description`, **последняя** — `status` (`draft` \| `stable`). Форма
ячеек — единая по проекту: slug-рефы в бэктиках, массивы питон-стилем, пустое опциональное — `—`.
Имена — по источнику (`situation.name`, имя агрегата), не аббревиатуры.

## Связи

Внутри поверхности:

- **screen → ситуации** (M:N): `screens.situation-ref` → `interface/<surface>/situation.md`.
- **screen → сущности / фичи** (M:N): `screens.entities` → `entities.md`; `screens.features` → `features.md`.
- **flow → экраны** (упорядоченно): `flows.шаги` → `screens.key`.
- **feature → ситуация** (M:1): `features.situation` → `interface/<surface>/situation.md`;
  **feature → api-ops** → `shared-api.md`; **feature ↛ feature** (cross-feature запрещён).
- **shell → экраны**: табы и auth-gate — существующие `screens.key`.
- **entry-point → экран** (M:1): `entry-points.target` → `screens.key`; deep-link `pattern` —
  в stack `deep-links.md`; push `category` — в stack `push.md`; `trigger` → `services/*/domains/<ctx>/events/`.
- **permission → ситуация** (M:1) и **← фичи/экраны** (≥ 1 использование).
- **entity → источник** (M:1): `entities.source` — `<svc>/<ctx>/<slug>` из `services/<svc>/domains/<ctx>/`
  или `local`; `shared-api` покрывает все entities с `source ≠ local`.

Поверхности изолированы: ссылки — только внутри `mobile/<surface>/` и в слои той же
поверхности; общее сводит `/cross-audit`.

## Инварианты слоя (проверяет `/mobile-audit`)

- экран без `situation-ref` — error; экран с сетью без `loading` и `error` в `states` — error;
  экран недостижим (нет в `shell`, `flows`, `entry-points`) — error;
- `feature → feature` — error; фича без экрана и без entry-point — error;
- `flows`: `app-start` и `login` обязательны; флоу с сетью без ветки ошибки — error;
- entry-point без `target` — error; `demo` обязателен при `preset ≠ enterprise`;
  deep-link `pattern` совпадает с `pages.path` web-поверхности того же продукта — hint;
- permission без ситуации-обоснования — error («на всякий случай» не бывает);
- `shared-api` покрывает все entities с `source ≠ local` — иначе blocker;
- `shared-lib.locales ⊇ ru`; каждый флаг используется ≥ 1 фичей.

## Связь с кодом

Stable-записи слоя — **вход** для форка `mobile-template` (`<org>/mobile`): его `/scaffold`
и `/de-template` читают файлы (не state скилла) и разворачивают их в `shared/`, `androidApp/`,
`iosApp/`; маппинг — `docs/reference/strategy-mapping.md` код-репо. Контракт read-only и
односторонний: код-репо в strategy не пишет; расхождение кода с картой — STRATEGY-HANDOFF в
`<org>/strategy`.

## Anti-patterns

- ❌ Код в артефактах: имена Kotlin/Swift-файлов, Gradle-модули, SwiftPM-таргеты, библиотеки.
- ❌ Экран без источника в interface; фича без ситуации; сущность без домена/`local`.
- ❌ Feature, ссылающаяся на feature; экран-сирота; flow без ветки ошибки при сети.
- ❌ Permission без ситуации и rationale; deep link, которого нет в stack `deep-links.md`.
- ❌ FSD-имена (`widget`, `page`) и веб-viewport-декомпозиция (`flow` desktop ↔ mobile) — у
  мобильной поверхности один viewport и навигационный граф.
- ❌ Битый slug-реф; ссылка из одной поверхности в сущность другой.
- ❌ `mobile/<surface>/` для поверхности, которой нет в реестре `surface.md` или у которой
  `type` не `mobile`.
