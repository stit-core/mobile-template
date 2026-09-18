---
name: scaffold
description: Добавить строительный блок в существующий проект по конвенциям шаблона, читая stable-запись карты mobile/<surface>/ в соседнем <org>/strategy и выводя пути в трёх местах (shared / androidApp / iosApp). Варианты — entity, feature, screen, flow, entry-point (deep link / push / виджетный тап), permission, system-action (голос, автоматизация, кнопка виджета или уведомления), widget (главный экран / экран блокировки / живое событие / плитка), background-task (синхронизация, напоминание, обновление виджета), api (ресурс), stack/rule/skill (из шаблонов). Активируй когда юзер говорит "добавь экран/фичу/сущность/deep link/push-категорию/permission/ресурс API", "добавь виджет", "сделай напоминание/фоновую синхронизацию", "голосовая команда", "кнопка в уведомлении", "scaffold …", "новый маршрут", "заведи kind/правило/скилл". Для правки уже существующего блока scaffold НЕ нужен — это прямая правка.
---

# Skill: scaffold (роутер)

Добавляет новый строительный блок в **существующий** проект по конвенциям шаблона. Скилл —
**роутер**: общий ритм здесь, специфика варианта — в `references/<variant>.md`. Источник блока —
**stable**-запись карты `<strategy-path>/docs/projects/<slug>/mobile/<surface>/<kind>.md`
(резолв — `docs/stack/service.md`; дефолт `../strategy`); пути в коде скилл выводит сам из
kind + slug (`docs/reference/strategy-mapping.md`). `--from-args` — без карты, с предупреждением
и строкой в roadmap «завести запись в карте».

## Выбор варианта

| Что добавляешь | Вариант | Reference | Читает в карте |
|---|---|---|---|
| Сущность: модель + репозиторий + маппер + фикстуры | `entity <slug>` | `references/entity.md` | `entities.md` |
| Фича: презентер + State/Intent (+ use-case) в `shared` | `feature <slug>` | `references/feature.md` | `features.md` |
| Экран на обеих платформах с превью/скриншотами/снапшотами | `screen <Key> --feature <slug>` | `references/screen.md` | `screens.md` |
| Навигационный сценарий: Maestro-флоу + сценарный тест | `flow <slug>` | `references/flow.md` | `flows.md` |
| Deep link / push-категория / тап по виджету: `Route`, парсер, intent-filter/AASA, `deep-links.md`/`push.md` | `entry-point <slug>` | `references/entry-point.md` | `entry-points.md` |
| Разрешение: rationale-экран, манифест/`Info.plist`, `compliance.md` | `permission <slug>` | `references/permission.md` | `permissions.md` |
| Действие вне экранов: `<Slug>Action` в `shared` + канал (голос, автоматизация, кнопка виджета или уведомления) | `system-action <slug>` | `references/system-action.md` | `system-actions.md` |
| Виджет системы: снимок в `shared` + Glance в `:androidApp:widget` + вью расширения | `widget <slug>` | `references/widget.md` | `widgets.md` |
| Фоновая задача: `BackgroundRunner` в `shared` + планировщик по `timing` | `background-task <slug>` | `references/background-task.md` | `background-tasks.md` |
| Ресурс API: `<Resource>Api` над сгенерированными моделями + фикстуры | `api <resource>` | `references/api.md` | `shared-api.md` |
| Kind / правило / скилл из шаблонов обвязки | `stack <kind>` · `rule <name>` · `skill <name>` | `references/harness.md` | — |

Несколько вариантов в одной задаче (фича с экраном и deep link) — веди по основному (`feature`),
затем подключай шаги соседних (`screen`, `entry-point`). Устойчивые связки:

- **виджет** — ведёт `widget`, затем `entry-point` (`kind: widget`, `pattern` = slug виджета) и
  `system-action` на каждую кнопку (канал `widget:<slug>`);
- **локальное напоминание** — ведёт `background-task` (`trigger: time`, `timing: exact`), затем
  `entry-point` (`push`, `trigger: background:<slug>`); бэкенд не участвует;
- **действие из шторки** — ведёт `system-action` (канал `notification:<slug>`) поверх
  существующей push-записи;
- **фича без экрана** — носителем может быть `system-action` или `background-task`, а не `screen`.

## Когда НЕ применять

- **Правка существующего** блока — точечная правка без scaffold.
- **Проектирование с нуля** (записи в карте нет и не будет `--from-args`) — сначала
  `/mobile-deep-dive` в `<org>/strategy`.
- **Онбординг форка** — `/init-service` → `/de-template`.

## Общий ритм (для всех вариантов)

1. **Подтвердить вход.** Найти запись в карте (`kind:slug`), проверить `status: stable` и
   `platforms` (все / android / ios); недостающее — одним блоком вопросов до начала. Нет карты —
   `--from-args` + предупреждение.
2. **Сделать изменение** по шагам reference: `shared` → `androidApp` → `iosApp` (по `platforms`).
   Имена — по канону: `<X>Presenter/State/Intent`, `<X>Action`, `<X>Task`,
   `<X>Widget{Snapshot,Provider}` (shared), `<X>Route/<X>Screen/<X>ViewModel`, `<X>Widget{,Receiver}`
   (Android), `<X>View/<X>Model`, `<X>Intent`, `<X>Entry` (iOS), `Fake<Port>`
   (`commonTest/…/testing/Fakes.kt`).
3. **Тесты по RED→GREEN.** Где появляется логика — сначала тест (`commonTest` на фикстурах
   `mock/`), потом код; экран — Robolectric-тест + Roborazzi на каждое состояние; iOS —
   snapshot-заглушка (Mac).
4. **Генерация и дампы.** Тронул фасад `shared` → `make api-dump` (по решению в плане); новая
   сущность → фикстуры во всех сценариях + `make mock-check`; новый ресурс → `make gen-check`.
5. **Верифицируй перед claim «готово»:** `make check`; iOS-часть — атом «Mac-verify»
   (`make check-mac`) или открытый долг в плане.
6. **Conventional Commit:** `feat(<scope>): <subject>`. Тронул хосты/спеку/карту → handoff
   (skill `/infra-handoff`) до коммита.

## Anti-patterns (общие; variant-специфичные — в reference)

- ❌ Логика в `ViewModel`/`View` вместо презентера `shared`.
- ❌ Фича зависит от фичи — общее в `entity`/`core`.
- ❌ Строковые маршруты; `NavHost`; `androidx.navigation` 2.x.
- ❌ Экран без состояния `error`/`loading` там, где есть сеть.
- ❌ Скаффолд «на будущее» без записи в карте и без потребителя — виджет, системное действие и
  фоновая задача особенно склонны к «на всякий случай».
- ❌ Логика в платформенной обёртке (Glance-виджет, worker, обработчик канала) вместо `shared`.
- ❌ Обещание точного времени исполнения кода в фоне и частого обновления виджета.
- ❌ Claim «готово» без `make check`; iOS «готово» без `check-mac`.

## См. также

- `references/{entity,feature,screen,flow,entry-point,permission,system-action,widget,background-task,api,harness}.md`.
- `docs/reference/strategy-mapping.md` — kind → пути в трёх местах.
- Правила (автогрузятся): `kmp-shared`, `kmp-public-api`, `android-ui`, `ios-ui`, `navigation`,
  `deep-links-and-push`, `tests-pipeline`, `mock-and-fixtures`.
