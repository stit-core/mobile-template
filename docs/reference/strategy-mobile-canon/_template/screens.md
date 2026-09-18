# screens

> Экран (screen) — адресуемое место приложения: один ключ навигации (`key`), набор
> состояний, входящие сущности и фичи. Мобильный экран и есть единственный viewport — flow
> desktop ↔ mobile веба здесь не существует. Из `key` код-репо выводит `Route.<Key>`,
> `<Key>Screen.kt`, `<Key>View.swift` и превью/снапшот на каждое состояние.

## Критерии screen

1. **Есть ситуация-источник.** `situation-ref` указывает на записи
   `interface/<surface>/situation.md` — экран не выдумывается без шага деятельности.
2. **Достижим.** Экран есть в `shell.md` (таб/корень), в `flows.md` (шаг) или в
   `entry-points.md` (`target`).
3. **Состояния явные.** `states` ⊇ `{loading, error}` для экранов с сетью; словарь —
   `loading, empty, populated, error, offline, permission-denied, not-found, update-required,
   maintenance`. Каждое состояние — превью/скриншот в коде.
4. **Композиция, не логика.** Экран показывает сущности и запускает фичи; правил не содержит.

## Что screen НЕ

- ❌ Действие пользователя — feature.
- ❌ Модальный шаг внутри экрана без своего ключа навигации — часть экрана (`states`/
  описание), пока не адресуем.
- ❌ Переиспользуемый UI-блок экрана — компонент дизайн-системы код-репо (виджеты системы —
  `widgets.md`).
- ❌ Веб-страница с URL — адресация здесь через `key`; ссылки — `entry-points.md`.

## Тест-критерий

«Сюда можно прийти по ключу навигации, у экрана есть источник в interface и явные состояния?»

- «**Детали задачи**» (ситуация `view-todo`, входы: список, deep link) → `TodoDetail` ✅
- «**Отметить задачу выполненной**» — действие → feature ❌
- «**Шапка списка**» — часть экрана ❌

## Колонки

- `key` — ключ навигации · `PascalCase` · обяз.
- `name` — человекочитаемое имя · текст · обяз.
- `description` — назначение · текст · обяз.
- `entry` — как достигается · `tab` \| `push` \| `modal` \| `sheet` \| `deep-link` · обяз.
- `states` — состояния · массив из словаря · обяз.
- `entities` — сущности · массив ссылок → `entities` (или `—`) · опц.
- `features` — фичи · массив ссылок → `features` (или `—`) · опц.
- `platforms` — `all` \| `android` \| `ios` · обяз.
- `situation-ref` — ситуации-источники · массив ссылок → `interface/<surface>/situation.md` · обяз.
- `status` — зрелость записи · `draft` \| `stable` · обяз.

| key | name | description | entry | states | entities | features | platforms | situation-ref | status |
|---|---|---|---|---|---|---|---|---|---|
| `<Key>` | <имя> | <назначение> | `tab` \| `push` \| `modal` \| `sheet` \| `deep-link` | [`loading`, `error`, …] | [`<entity>`] \| — | [`<feature>`] \| — | `all` | [`<situation>`] | `draft` \| `stable` |

> Пример: `TodoList` · «Список задач» · «Все задачи пользователя с быстрым переключением» ·
> `tab` · `[loading, empty, populated, error, offline]` · `[todo]` · `[toggle-todo, create-todo]`
> · `all` · `[view-todos]` · `draft`.
