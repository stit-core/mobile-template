---
name: scaffold
description: Добавить строительный блок в существующий проект по конвенциям шаблона, читая stable-запись карты mobile/<surface>/ в соседнем <org>/strategy и выводя пути в трёх местах (shared / androidApp / iosApp). Варианты — entity, feature, screen, flow, entry-point (deep link / push), permission, api (ресурс), stack/rule/skill (из шаблонов). Активируй когда юзер говорит "добавь экран/фичу/сущность/deep link/push-категорию/permission/ресурс API", "scaffold …", "новый маршрут", "заведи kind/правило/скилл". Для правки уже существующего блока scaffold НЕ нужен — это прямая правка.
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
| Deep link / push-категория: `Route`, парсер, intent-filter/AASA, `deep-links.md`/`push.md` | `entry-point <slug>` | `references/entry-point.md` | `entry-points.md` |
| Разрешение: rationale-экран, манифест/`Info.plist`, `compliance.md` | `permission <slug>` | `references/permission.md` | `permissions.md` |
| Ресурс API: `<Resource>Api` над сгенерированными моделями + фикстуры | `api <resource>` | `references/api.md` | `shared-api.md` |
| Kind / правило / скилл из шаблонов обвязки | `stack <kind>` · `rule <name>` · `skill <name>` | `references/harness.md` | — |

Несколько вариантов в одной задаче (фича с экраном и deep link) — веди по основному (`feature`),
затем подключай шаги соседних (`screen`, `entry-point`).

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
   Имена — по канону: `<X>Presenter/State/Intent` (shared), `<X>Route/<X>Screen/<X>ViewModel`
   (Android), `<X>View/<X>Model` (iOS), `Fake<Port>` (`commonTest/…/testing/Fakes.kt`).
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
- ❌ Скаффолд «на будущее» без записи в карте и без потребителя.
- ❌ Claim «готово» без `make check`; iOS «готово» без `check-mac`.

## См. также

- `references/{entity,feature,screen,flow,entry-point,permission,api,harness}.md`.
- `docs/reference/strategy-mapping.md` — kind → пути в трёх местах.
- Правила (автогрузятся): `kmp-shared`, `kmp-public-api`, `android-ui`, `ios-ui`, `navigation`,
  `deep-links-and-push`, `tests-pipeline`, `mock-and-fixtures`.
