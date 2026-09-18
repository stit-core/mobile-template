# shell

> Оболочка (shell) — каркас приложения, singleton поверхности: корневая навигация, что
> доступно без сессии, глобальные оверлеи, offline-политика, тема и язык, правовые документы.
> Код-репо разворачивает секции в `feature/shell/ShellPresenter` (порядок старта: demo → кэши →
> session → route, pending route) и `feature/requirements/RequirementsPresenter` (оверлеи),
> `NavDisplay`/`RootView` и оверлеи `core:ui`.

## Критерии shell

1. **Все табы и корневые экраны — существующие `screens.key`.**
2. **Auth-gate явный.** Перечислены экраны, доступные без сессии (минимум `Login` и `Legal`).
3. **Оверлеи и offline — решения продукта**, не платформы: что показывается при
   `update_required`, `maintenance`, отсутствии сети; что читается офлайн, что блокируется.
4. **Правовые документы доступны до логина** — где именно.

## Что shell НЕ

- ❌ Экран — экраны в `screens.md`; здесь только их размещение в каркасе.
- ❌ Платформенная навигация (Navigation 3, `NavigationStack`) — `docs/stack/` код-репо.
- ❌ Значения токенов темы — код-репо (`styling.md`); здесь — только политика.

## Тест-критерий

«Описан каркас, в который встают экраны: корень, что без сессии, оверлеи, offline, тема/язык,
документы?»

- «Табы `TodoList` · `Settings`; без сессии — `Login`, `Legal`; offline — задачи read-only» ✅
- «Экран `TodoList`» — screen ❌

## Root

- `root` — тип корня · `tabs` \| `stack` \| `drawer` · обяз.
- `tabs` — упорядоченный список · массив ссылок → `screens.key` · обяз. при `tabs`
- `start` — стартовый экран после сессии · реф → `screens.key` · обяз.

| root | tabs | start | status |
|---|---|---|---|
| `tabs` | [`<Key>`, …] | `<Key>` | `draft` |

## Auth-gate

| без сессии | с сессией | после логина |
|---|---|---|
| [`Login`, `Legal`, …] | всё остальное | pending route из entry-point или `start` |

## Overlays

| overlay | когда | что видит пользователь | status |
|---|---|---|---|
| `update-required` | `requirements.status = update_required` | блокирующий экран + ссылка в магазин | `draft` |
| `maintenance` | `requirements.status = maintenance` | экран обслуживания до `maintenance_until` | `draft` |
| `offline-banner` | нет сети | баннер; экраны по `Offline` | `draft` |
| `demo-banner` | режим demo | пометка «демо-данные» | `draft` |

## Offline

| экран / сущность | без сети | status |
|---|---|---|
| `<Key>` / `<entity>` | `read-only` \| `full` \| `blocked` | `draft` |

`full` означает офлайн-правки: их догоняет фоновая задача синхронизации
(`background-tasks.md`, `trigger: connectivity`).

## Theme & locale

Тема `system` \| `light` \| `dark` (дефолт `system`); локали — `shared-lib.md#locales`;
смена — экран `Settings`, применяется без перезапуска.

## Legal

| документ | где показывается | доступен до логина |
|---|---|---|
| политика обработки ПДн | `Legal`, первый запуск, `Settings → About` | да |
| согласия (крэш-репорты, аналитика) | первый запуск, `Settings` | да |
| удаление аккаунта | `Settings → Account` | — |
