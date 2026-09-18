# entry-points

> Точка входа (entry-point) — как в приложение попадают извне: deep link (https-путь или
> кастомная схема), push-уведомление (категория), ярлык, share-in, тап по виджету, demo-режим
> для ревью магазина. Аналог `command.md` + `notification.md` бота. Прямой вход для stack
> `deep-links.md` (пути, хосты) и `push.md` (категории) код-репо; из записи выводятся ветка
> `DeepLinkParser`, intent-filter / AASA-путь, push-категория.

## Критерии entry-point

1. **Есть цель.** `target` — существующий `screens.key`; вход без экрана не бывает.
2. **Паттерн уникален.** Путь deep link не пересекается с другими; push-категория —
   UPPER_SNAKE, уникальна; у входа `widget` паттерн — slug записи `widgets.md`.
3. **Сессия и pending явные.** Требует ли входа и продолжается ли после логина.
4. **`demo` обязателен** при `preset ≠ enterprise` — App Review требует demo-режим.
5. **Push-триггер — событие домена** (`event:<ctx>.<Event>`), фоновая задача устройства
   (`background:<slug>` — локальное уведомление без бэкенда) или расписание; без домена —
   требование в state, не выдумка.

## Что entry-point НЕ

- ❌ Переход внутри приложения (кнопка, таб) — `flows.md`.
- ❌ Действие без экрана (голос, кнопка виджета или уведомления) — `system-actions.md`; здесь
  — только входы, ведущие на экран.
- ❌ Сам виджет (что показывает, состояния) — `widgets.md`; здесь — лишь тап по нему.
- ❌ Хост, отпечаток, провайдер — платформа (`docs/stack/{deep-links,push}.md`).
- ❌ Текст уведомления — ключи локализации `shared-lib.md`; здесь — категория и цель.

## Тест-критерий

«Это вход извне (ссылка, пуш, ярлык, share, demo), ведущий на конкретный экран?»

- «`https://<host>/todos/{id}` → `TodoDetail`, нужна сессия, pending после логина» → `todo-link` ✅
- «Нажатие на задачу в списке» — flow ❌
- «Хост `app.<domain>`» — stack `deep-links.md` ❌

## Колонки

- `slug` — идентификатор · `kebab-case` · обяз.
- `name` — человекочитаемое имя · текст · обяз.
- `description` — что происходит при входе · текст · обяз.
- `kind` — `deep-link` \| `push` \| `shortcut` \| `share` \| `widget` \| `demo` · обяз.
- `pattern` — путь (`/todos/{id}`) · категория (`ORDER_STATUS`) · тип share · slug виджета · текст · обяз.
- `target` — целевой экран · реф → `screens.key` · обяз.
- `требует сессии` — `да` \| `нет` · обяз.
- `pending после логина` — `да` \| `нет` · обяз.
- `trigger` — для push: `event:<ctx>.<Event>` \| `background:<slug>` \| расписание \| `—` · опц.
- `status` — зрелость записи · `draft` \| `stable` · обяз.

| slug | name | description | kind | pattern | target | требует сессии | pending после логина | trigger | status |
|---|---|---|---|---|---|---|---|---|---|
| `demo` | Demo-режим | <ревьюер магазина и demo без бэкенда: фикстуры, сеть выключена> | `demo` | flavor/схема `demo` · флаг `demo_login` | `<start>` | нет | нет | — | `draft` |
| `<slug>` | <имя> | <что происходит> | `deep-link` \| `push` \| … | `/<path>/{id}` \| `<CATEGORY>` | `<Key>` | `да` \| `нет` | `да` \| `нет` | `event:<ctx>.<Event>` \| — | `draft` \| `stable` |

> Пример: `todo-link` · «Ссылка на задачу» · «Открывает задачу из письма или веба» ·
> `deep-link` · `/todos/{id}` · `TodoDetail` · `да` · `да` · `—` · `draft`;
> `todo-due-push` · «Срок задачи» · «Напоминание о дедлайне» · `push` · `TODO_DUE` ·
> `TodoDetail` · `да` · `да` · `event:todos.TodoDueSoon` · `draft`.
