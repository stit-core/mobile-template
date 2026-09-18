# Артефакты мобильного приложения

Слой композиции и карты кода **мобильной поверхности**, партиционированный по поверхности
(surface): из аналитических артефактов `docs/projects/<project-slug>/interface/<surface>/` и
доменов `services/*/domains/` синтезируется карта приложения в родных понятиях мобильных
платформ — **сущность (entity) · экран (screen) · сценарий (flow) · фича (feature) · оболочка
(shell) · точка входа (entry-point) · разрешение (permission) · системное действие
(system-action) · виджет (widget) · фоновая задача (background-task) · контракт данных
(shared-api) · сквозное (shared-lib)**. Для поверхности с `type: mobile` из реестра `surface.md`
этот слой встаёт **на место `screens/<surface>/`** — так же, как `telegram-bot-flow/<surface>/`
для `type: bot`; слоя `fsd/` у мобильной поверхности нет. Этот файл — **канон слоя**; формы
двенадцати файлов лежат рядом в `_template/`. Данные каждого проекта живут в
`docs/projects/<project-slug>/mobile/<surface>/`.

Источник правды канона — `business-strategy-template` (`docs/projects/_template/mobile/`);
эта копия в `mobile-template` (`docs/reference/strategy-mobile-canon/`) — путь без PR для
strategy-форков, у которых канона ещё нет: `/init-service` печатает команду копирования.

## Воркфлоу

```
interface/<surface>/ + services/*/domains/ → синтез → mobile/<surface>/ → код приложения
 (бизнес, домены)                                      (карта: сущности,      (<org>/mobile — форк mobile-template:
                                                        экраны, флоу, фичи,    shared/ + androidApp/ + iosApp/)
                                                        входы, действия,
                                                        виджеты, фон)
```

1. В `interface/<surface>/` заведены ситуации, блоки, роли и данные мобильной поверхности;
   в `services/*/domains/` — агрегаты, use-case'ы, события (если бэкенд есть).
2. Через `/mobile-deep-dive <slug> <surface>` заводятся и углубляются записи двенадцати файлов;
   каждая ссылается на источник через `slug`. Консистентность проверяет `/mobile-audit`;
   сквозные рёбра — `/cross-audit` (углы `surface-mobile`, `mobile-domain`, `mobile-service`,
   `mobile-web` — пути deep links ↔ `pages.path` web-поверхности того же продукта).
3. Stable-записи читает код-репо `<org>/mobile`: `/scaffold` и `/de-template` разворачивают их
   в три места — `shared/`, `androidApp/`, `iosApp/` — и сами выводят пути из kind + slug.
   Имена файлов, Gradle-модули, SwiftPM-таргеты и библиотеки — забота код-репо, не слоя.

## 12 kind

| kind | файл | один-в-одно про |
|---|---|---|
| [`entity`](_template/entities.md) | `entities.md` | вещь с данными: источник в домене, поля, кэш-политика |
| [`screen`](_template/screens.md) | `screens.md` | экран: ключ навигации, вход, состояния, сущности, платформы |
| [`flow`](_template/flows.md) | `flows.md` | навигационный сценарий: шаги по экранам, выход, ветки ошибок |
| [`feature`](_template/features.md) | `features.md` | действие пользователя: экраны + сущности + операции API, где живёт логика |
| [`shell`](_template/shell.md) | `shell.md` (singleton) | каркас: корневая навигация, auth-gate, оверлеи, offline, тема/язык, правовые документы |
| [`entry-point`](_template/entry-points.md) | `entry-points.md` | как в приложение попадают извне на экран: deep link, push, shortcut, share, тап по виджету, demo |
| [`permission`](_template/permissions.md) | `permissions.md` | разрешение ОС с обоснованием, моментом запроса и fallback'ом |
| [`system-action`](_template/system-actions.md) | `system-actions.md` | действие, которое система выполняет вне экранов приложения: голос, автоматизации, кнопки виджета и уведомления |
| [`widget`](_template/widgets.md) | `widgets.md` | представление вне экранов приложения: главный экран, экран блокировки, живое событие, плитка |
| [`background-task`](_template/background-tasks.md) | `background-tasks.md` | работа без пользователя на экране: триггер, точность по времени, результат, отказ |
| [`shared-api`](_template/shared-api.md) | `shared-api.md` | ресурсы бэкенда и сценарии данных для mock/demo |
| [`shared-lib`](_template/shared-lib.md) | `shared-lib.md` | локали, ключи ошибок, логирование, флаги, события аналитики |

Порядок проектирования — **inside-out**: entities → screens → flows → features → shell →
entry-points → permissions → system-actions → widgets → background-tasks → shared-api →
shared-lib.

## Что умеет телефон — и где это в слое

Карта-ориентир: возможность платформы → kind, в котором она проектируется. Продуктовая сторона
той же возможности живёт выше, в слое проекта: ценность для пользователя — `capability/<surface>`,
внешняя система-посредник (сервис доставки пушей, ассистент, облачное распознавание речи) —
`integration`, неснимаемое ограничение платформы или магазина — `constraint`.

| возможность | где проектируется |
|---|---|
| push от бэкенда | `entry-points` (`push`: категория, целевой экран, `trigger: event:…`) + `permissions` (`notifications`) |
| напоминание без бэкенда | `background-tasks` (`trigger: time`, `timing: exact`) → `entry-points` (`push`, `trigger: background:<slug>`) |
| кнопки в уведомлении, ответ из шторки | `system-actions` (`channels: notification:<slug>`) |
| виджет на главном экране / экране блокировки | `widgets` (`home` / `lock`) + тап — `entry-points` (`widget`) + кнопки — `system-actions` |
| живое событие (доставка, запись, таймер) | `widgets` (`live`) |
| плитка быстрых настроек | `widgets` (`tile`) + `system-actions` |
| голосовая команда через ассистента | `system-actions` (`channels: voice`, `phrases`) |
| автоматизации и команды системы | `system-actions` (`channels: automation`) |
| собственное «слово активации» | `background-tasks` (`timing: continuous`) + `permissions` (микрофон) — отдельное решение владельца |
| синхронизация, отложенная выгрузка | `background-tasks` (`periodic` / `connectivity`) |
| запись или воспроизведение при свёрнутом приложении | `background-tasks` (`user-started`, `continuous`) + `permissions` |
| работа без сети | `shell.md#Offline` + `entities.cache` (+ `background-tasks` для офлайн-правок) |
| ссылки в приложение | `entry-points` (`deep-link`); общий путь с web — угол `mobile-web` |
| ярлыки иконки, «Поделиться» в приложение | `entry-points` (`shortcut` / `share`) |
| камера, геолокация, микрофон, контакты, биометрия | `permissions` + фича (`shared: false` для платформенной возможности) |
| часы, автомобиль, телевизор | отдельная поверхность реестра `surface.md`, не kind этого слоя |

## Двухролевость слоя

Слой одновременно **композиция** (экраны, флоу, оболочка, виджеты — что видит пользователь) и
**карта кода** (фичи, сущности, точки входа, системные действия, фоновые задачи — из чего
`/scaffold` выводит пути). Платформенное (Navigation 3 / `NavigationStack`, convention-плагины,
планировщики фона, версии) — не предмет слоя; оно живёт в `docs/stack/` код-репо. Правило
разграничения: **strategy описывает продукт и его карту; stack — платформу и её значения**;
меняется при смене магазина/SDK/хоста — stack, при смене продукта — strategy.

## Язык контракта

Структурные идентификаторы, которые читает код-репо, — **английские** и не переименовываются:

- `key` экрана — PascalCase (`TodoDetail`) → `Route.TodoDetail`, `TodoDetailScreen.kt`,
  `TodoDetailView.swift`;
- `slug` сущности/фичи/флоу/permission/системного действия/виджета/фоновой задачи — kebab-case
  (`delete-account`) → `DeleteAccountPresenter` в `feature/<group>/` (группа экранов, например
  `auth`);
- push-категория — UPPER_SNAKE (`ORDER_STATUS`);
- `states` экрана — из словаря `loading, empty, populated, error, offline, permission-denied,
  not-found, update-required, maintenance`; `states` виджета — `placeholder, populated, empty,
  signed-out, stale`;
- enum'ы колонок (`kind`, `entry`, `mode`, `channels`, `trigger`, `timing`, `refresh`, `result`,
  `conditions`, `cache`, `platforms`) — значения канона форм.

Всё остальное — **русский контент**: названия, описания, rationale, фразы, тексты состояний.

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
  в stack `deep-links.md`; push `category` — в stack `push.md`; `trigger` →
  `services/*/domains/<ctx>/events/` или `background:<slug>` → `background-tasks.md`.
- **permission → ситуация** (M:1) и **← фичи/экраны/фоновые задачи/системные действия**
  (≥ 1 использование).
- **system-action → фича** (M:1): `system-actions.feature` → `features.md`; **→ экран** при
  `mode: opens-app` (`target` → `screens.key`); каналы `widget:<slug>` → `widgets.md`,
  `notification:<slug>` → push-запись `entry-points.md`.
- **widget → сущности** (M:N): `widgets.shows` → `entities.md`; **→ точка входа** (M:1):
  `widgets.tap` → `entry-points.md` (`kind: widget`, её `pattern` — slug виджета);
  **widget ← system-actions** (M:N): `widgets.actions` — **зеркало для наглядности**, канон —
  `system-actions.channels ∋ widget:<slug>`; `refresh` → `background-tasks.md` или
  `services/*/domains/<ctx>/events/`.
- **background-task → фичи / сущности** (M:N): `runs` → `features.md`, `entities` →
  `entities.md`; **→ результат**: `notification:<slug>` → push-запись `entry-points.md`,
  `widget:<slug>` → `widgets.md`; `permissions` → `permissions.md`.
- **entity → источник** (M:1): `entities.source` — `<svc>/<ctx>/<slug>` из `services/<svc>/domains/<ctx>/`
  или `local`; `shared-api` покрывает все entities с `source ≠ local`.

Поверхности изолированы: ссылки — только внутри `mobile/<surface>/` и в слои той же
поверхности; общее сводит `/cross-audit`.

## Инварианты слоя (проверяет `/mobile-audit`)

- экран без `situation-ref` — error; экран с сетью без `loading` и `error` в `states` — error;
  экран недостижим (нет в `shell`, `flows`, `entry-points`, `system-actions.target`) — error;
- `feature → feature` — error; фича без экрана, entry-point, системного действия и фоновой
  задачи — error;
- `flows`: `app-start` и `login` обязательны для поверхности с входом; флоу с сетью без ветки
  ошибки — error;
- entry-point без `target` — error; `demo` обязателен при `preset ≠ enterprise`;
  вход `widget` без записи в `widgets.md` — error;
  deep-link `pattern` совпадает с `pages.path` web-поверхности того же продукта — hint;
- permission без ситуации-обоснования — error («на всякий случай» не бывает);
- system-action без фичи или без `response` — error; канал `voice` без `phrases` — error;
  `opens-app` без `target` — error;
- widget без `placeholder`/`populated` в `states` — error; зеркало `widgets.actions` разошлось
  с `system-actions.channels` — error; `live` без длящегося события — warning;
- background-task без `fallback` — error; `timing: exact` без `result: notification:<slug>` —
  error; `timing: continuous` без названного разрешения и видимости пользователю — error;
  офлайн `full` в `shell.md#Offline` без задачи синхронизации — hint;
- `shared-api` покрывает все entities с `source ≠ local` — иначе blocker;
- `shared-lib.locales ⊇ ru`; каждый флаг используется ≥ 1 фичей.

## Связь с кодом

Stable-записи слоя — **вход** для форка `mobile-template` (`<org>/mobile`): его `/scaffold`
и `/de-template` читают файлы (не state скилла) и разворачивают их в `shared/`, `androidApp/`,
`iosApp/`; маппинг kind → пути — `docs/reference/strategy-mapping.md` код-репо. Kind без строки
в этом маппинге `/scaffold` не разворачивает: его запись остаётся продуктовым требованием,
которое реализуется в код-репо вручную. Контракт read-only и односторонний: код-репо в strategy
не пишет; расхождение кода с картой — STRATEGY-HANDOFF в `<org>/strategy`.

## Anti-patterns

- ❌ Код в артефактах: имена Kotlin/Swift-файлов, Gradle-модули, SwiftPM-таргеты, библиотеки,
  имена платформенных API.
- ❌ Экран без источника в interface; фича без ситуации; сущность без домена/`local`.
- ❌ Feature, ссылающаяся на feature; экран-сирота; flow без ветки ошибки при сети.
- ❌ Permission без ситуации и rationale; deep link, которого нет в stack `deep-links.md`.
- ❌ Виджет, системное действие или фоновая задача «на всякий случай» — без ситуации-источника.
- ❌ Механика фона и обновлений (интервалы, планировщики, расписания в минутах) — здесь только
  требование смыслом; обещание точного времени исполнения кода в фоне.
- ❌ Собственное постоянное прослушивание микрофона как «просто голосовой ввод» — это длящаяся
  фоновая работа с разрешением и отдельным решением владельца.
- ❌ FSD-смыслы (`page`, FSD-`widget` как блок страницы) и веб-viewport-декомпозиция (`flow`
  desktop ↔ mobile) — у мобильной поверхности один viewport и навигационный граф; `widgets.md`
  этого слоя — виджеты системы.
- ❌ Битый slug-реф; ссылка из одной поверхности в сущность другой.
- ❌ `mobile/<surface>/` для поверхности, которой нет в реестре `surface.md` или у которой
  `type` не `mobile`.
