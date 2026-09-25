---
name: stack-deep-dive
description: Углублённая интерактивная проработка 14 обязательных + 2 условных платформенных артефактов в `docs/stack/` (service · runtime · api · auth · deep-links · push · background · observability · styling · distribution · signing · compliance · dependencies · config · analytics · payments) — единственная точка входа для проектирования стека мобильного приложения. Ведёт сессию раундами `AskUserQuestion` (≤ 4 вопроса), фиксирует рабочее состояние в `state/`, не выдумывает платформенные решения. Применяй, когда архитектор проектирует или добивает стек, приносит сырьё под платформенный разбор (build.gradle.kts, xcconfig, описание магазинов и аккаунтов) или `/init-service` дошёл до стадии 4. Точечная правка уже стабильного артефакта — прямой Edit, не сюда.
---

# Skill: stack-deep-dive

Проектирование платформы мобильного приложения. Превращает драфты артефактов `docs/stack/`
(frontmatter + предзаполненные дефолты шаблона) в полные `status: stable` артефакты с
конкретной секцией **Generates** в каждом.

Где карта `mobile/<surface>/` в `<org>/strategy` отвечает «какой продукт и какие экраны», этот
скилл — «на какой платформе и как это доезжает до магазина»: toolchain'ы, контракт с бэкендом,
auth, deep links, push, observability, токены, каналы дистрибуции, ключи, право, зависимости, env.

Это **source of truth и вход для `/de-template`**: секция `Generates` каждого артефакта
описывает, что из него родится в коде и конфиге. Канон kinds, волны и cross-kind-инварианты
— `docs/stack/README.md`; скилл их не дублирует, а исполняет.

## Главный ритм: AskUserQuestion

Скилл — **интервьюер архитектора**, не редактор: платформенные решения принимает архитектор, а
скилл собирает их вопросами и записывает. Каждый шаг — раунд `AskUserQuestion` (до 4
вопросов) → ответ → `Edit` артефакта → следующий раунд:

- ❌ Prose-ответ «вот как я бы спроектировал» вместо вопросов.
- ❌ Применить «обычный» дефолт (хост, провайдер, магазин) молча — это решение архитектора.
- ❌ > 4 вопросов в одном `AskUserQuestion` — инструмент режет молча.
- ❌ Копить ответы «запишу в конце волны» — пиши после **каждого** раунда: сессия может
  оборваться, а незаписанный ответ теряется.
- ✅ Дефолты шаблона (Bearer, RuStore + Play, `ANALYTICS=none`, Sentry с пустым DSN) —
  предлагаются как **опции с пометкой «дефолт шаблона»**, архитектор подтверждает или меняет.

Anti-patterns и enum'ы канона — опциями; имена хостов, id, отпечатки, аккаунты, числа —
только через свободную форму (Other).

## Точки входа

| вызов | поведение |
|---|---|
| `/stack-deep-dive` | сводка состояния 16 kinds + `AskUserQuestion`: что углубить / что завести |
| `/stack-deep-dive <kind>` | завести (режим A — файла нет) или углубить (режим C — есть) |
| `/stack-deep-dive <сырьё>` | текст/файл (`build.gradle.kts`, xcconfig, письмо заказчика про магазины) → разбор (режим B) |

`kind` ∈ 14 обязательных: `service runtime api auth deep-links push background observability
styling distribution signing compliance dependencies config` + условные `analytics` (при
`preset: consumer` или `ANALYTICS ≠ none`) и `payments` (при монетизации).

## Что делать сразу при запуске

1. Прочитать `docs/stack/README.md` — kinds, волны, cross-kind-инварианты, frontmatter.
2. Прочитать `docs/stack/service.md` (если есть): `platforms`, `preset` — от них зависит набор
   обязательных kinds и iOS-секций.
3. Args пустые — сводка + выбор; kind — режим A/C по `references/workflow-patterns.md`;
   сырьё — режим B.

## Сводка состояния

1. `ls docs/stack/*.md` (без `README.md`, `_templates/`) — какие kinds есть.
2. Для каждого: frontmatter (`status`, `kind`), H2-заголовки, заполненность обязательных
   секций (`references/field-checklists.md`), плейсхолдеры `<…>`/`_…_`.
3. Условные kinds: существуют ⇔ `service.md#preset`/флаги их требуют (инвариант 7).
4. `state/<kind>.md` — открытые вопросы; `**blocker**` приоритетны.
5. Сжатая карта в чат:

   ```
   ## Состояние docs/stack

   | kind | файл | status | секции | дыры |
   |---|---|---|---|---|
   | service | ✅ | draft | 3/4 | Generates |
   | deep-links | ❌ нет | — | — | — |
   …
   ```

6. `AskUserQuestion` — что копать (опции = draft-kinds + «завести недостающие»).

## Три режима

- **A. Заведение.** `cp docs/stack/_templates/<kind>.md docs/stack/<kind>.md`; frontmatter
  из имени; iOS-секции удаляются, если `ios ∉ platforms` (не «пустые», а отсутствуют); раунд
  фундамента (одна фраза роли) → режим C.
- **B. Сырьё.** Извлечь платформенные решения → привязать к kinds → показать план →
  подтверждение → завести/дополнить драфты → режим C по каждому затронутому.
  Извлечённое — кандидаты, не факты; `Generates` из сырья не выводится.
- **C. Углубление.** Точечные вопросы по дырам (`references/field-checklists.md`), anti-pattern-
  подозрениям и cross-kind-дрейфу; чек-лист `draft → stable` после каждой правки.

Детали — `references/workflow-patterns.md`.

## Волны (inside-out; у архитектора не спрашивается)

W1 `service` → W2 `runtime` → W3 `api` → `auth` → W4 `deep-links` → `push` → `background`
(→ `analytics` → `payments` по пресету) → W5 `styling` → `observability` → W6 `distribution` →
`signing` → `compliance` → W7 `dependencies` → W8 `config` (rollup всех env-значений).

Юзеру в начале: «иду фундамент → rollup, начну с `service`; скажи, если хочешь прыгнуть».

## Память `state/<kind>.md`

`references/state-format.md`. Один файл на kind; секции `## Открытые вопросы` (опц.
`**blocker**`), `## Платформенные решения`, `## Что копать дальше`; пусто → удалить файл.
Коммитится; forward-only.

## Контракт `draft → stable`

`references/draft-stable.md` — 6 пунктов после каждой правки:

1. Обязательные секции заполнены без плейсхолдеров.
2. `## Generates` конкретна (пути, имена env, ключи, скрипты).
3. Cross-kind-инварианты 1–8 (`docs/stack/README.md`) для этого kind выполнены; rollup в
   `config.md` обновлён.
4. Frontmatter валиден (`id: stack.<name>`, `kind`, `name` = basename, `status`).
5. Тест-критерий kind'а подтверждён архитектором в этой сессии.
6. `state/` чист по `**blocker**`.

Все ✅ → `AskUserQuestion` «Готов к stable, повышаем?» → по «Да» `Edit` `status: stable`.
**Скилл не повышает сам.**

## Cross-kind sync

Правка kit-артефакта → скилл черновиком дополняет rollup и показывает дифф:

- `auth` → `config` (`AUTH_MODE`, TTL), `api` (Bearer-схема ⇔ `POST /api/auth/refresh`), BACKEND-HANDOFF.
- `deep-links` → `config` (хосты per env), `signing` (отпечатки), INFRA-HANDOFF (`.well-known`).
- `push` → `config` (`PUSH_PROVIDERS`), `dependencies` (SDK), `compliance` (privacy manifest), BACKEND-HANDOFF B-18; кнопки категорий ← `system-actions.md` карты.
- `background` → `compliance` (точные будильники, тип foreground service, background modes), `push` (локальные категории), `dependencies` (WorkManager), `service`/`config` (App Group для снимков виджетов); реестр задач ← `background-tasks.md` карты.
- `observability` → `config` (`SENTRY_DSN` per env), `distribution` (символы — шаг `/release` до STOP-2).
- `distribution` ↔ `service.platforms`; `ios ∈ platforms` ⇒ `runtime#mac-host` или `blocked-by: Mac`.
- `compliance` ← `permissions.md` карты strategy, `dependencies`, `analytics`/`payments`.
- `dependencies` → `compliance`, `styling` (шрифты), `config`.

## Завершение сессии

1. Перечислить затронутые артефакты (заведены / углублены / промоутнуты).
2. Нерешённые `**blocker**` из `state/`.
3. Предложить следующий шаг: онбординг → `/de-template` (когда все обязательные `stable`);
   отдельно — задета поверхность соседа → `/infra-handoff`.

## Запреты

- Не выдумывать хосты, id, отпечатки, аккаунты, провайдеры — только от архитектора.
- Не оставлять `Generates` пустым/абстрактным — это вход `/de-template`.
- Не повышать `draft → stable` без подтверждения; не понижать сам.
- Не игнорировать cross-kind sync; не создавать условные kinds без пресета/флага.
- Не задавать > 4 вопросов одним `AskUserQuestion`; не копить ответы.
- Не аккумулировать историю в `state/`.
- Не писать в `../strategy`/`../backend`/`../infra` — handoff-блоками.
