---
name: smartdev
description: Always-on cockpit дневного флоу. Запускай в начале сессии / после /clear / когда непонятно с чего начать. Гонит make doctor и make status, грузит контекст (активный план из roadmap, стек, Mac-хост, дрейф спеки) и САМ переключает в нужный режим (plan / triage-failure / scaffold / infra-handoff / Mac-verify / release), ведя RED→GREEN по дефолту. Активируй на "/smartdev", "что у нас?", "с чего начать?", "введи в курс дела", "статус проекта".
---

# Skill: smartdev (cockpit)

Каноничный entry-point. Не «показать сводку и замолчать», а **вести**: за один проход собрать
состояние, поднять контекст и **самому переключить** в правильный режим работы. Упёрлось в
красное/дрейф/handoff — не описывай, а чейни в нужный skill.

## Когда применять

- Начало сессии, после `/clear`, возврат после паузы: «/smartdev», «что у нас», «с чего начать»
- Юзер дал задачу, но непонятно насколько большая
- Не уверен, какой режим сейчас правильный

## Когда НЕ применять

- Узкая чёткая задача («поправь опечатку в X») — иди прямо в работу (RED→GREEN всё равно)
- Уже идёт активная сессия с известным контекстом — не повторяй briefing

## Шаги

### 1. Машина

```bash
make doctor
```

Первым делом: три самых частых стопора — нет `javac` (JDK 21), нет `ANDROID_HOME`, мало RAM.
`FAIL` → стоп, чинить окружение по next-step из вывода (`export JAVA_HOME=…`, `ANDROID_HOME`,
`SWIFT_TOOLCHAIN`, `MAC_HOST`); `WARN` — идём дальше, но помним (эмулятор не поднимется,
Swift-стадии в SKIP).

### 2. Состояние

```bash
make status
```

Парси секции: `== Branch ==`, `== Working tree ==`, `== Platforms ==` (android/ios, preset из
`docs/stack/service.md`), `== Mac ==` (`MAC_HOST` доступен / Xcode == `.xcode-version` / not
set), `== Gate ==` (последний `check` и `check-mac`), `== Critical ==` (`GEN` — lock ↔ спека ↔
сосед, `API-DUMP`, `KLIB`, `STACK-DRAFTS`, `SCREENSHOTS`), `== Handoffs ==` (INFRA / BACKEND
pending), `== Modules ==`, `== Plan ==` (focus + активная секция + следующий открытый атом),
`== Strategy ==` (карта `mobile/<surface>/` соседа: kinds, draft/stable), `== Recent commits ==`,
`== TODOs ==`. Последняя строка — `OK` или `ATTENTION:<что>`: это твой сигнал ветвления.

### 3. Поднять контекст — читай сам, не жди скрипта

`make status` — детерминированный детект критичного; ориентацию добери чтением:

- **Roadmap целиком — `.claude/plans/roadmap.md`**: `## Active` и `## Backlog`. Active пуст —
  назови юзеру 1–3 backlog-пункта как кандидатов «с чего начать».
- **Активный план** (`<slug>: in-progress | next: A#`) — прочти секцию: последний чекпоинт,
  следующий открытый атом. Продолжай с него, не перепланируя.
- **Что это за приложение** (свежая сессия): `docs/stack/service.md` (де-темплейтизирован ли
  форк: `applicationId` ≠ `tech.stit.mobile`, `platforms`, `preset`), `docs/stack/` (что
  `draft`, что `stable`), наличие reference-фич `profile`/`todos` (на месте → путь
  `/init-service`), `iosApp/` (есть ли, верифицирован ли: `build/gate-logs/mac.last`).
- Договорённости прошлых сессий — в закоммиченных артефактах (чекпоинты roadmap, `CLAUDE.md`,
  rules); авто-память не используется.

On-demand: контекст уже поднят в этой сессии — не перечитывай.

### 4. Чейнить — НЕ описывай, переключай

По приоритету сверху вниз. Первое сработавшее — переключаешься **сразу**, говоришь юзеру
«→ перехожу в `<skill>`, потому что <причина>»:

| Сигнал из status / задачи | Действие — чейн |
|---|---|
| `make doctor` FAIL | чинить окружение по next-step; дальше не идти |
| `ATTENTION:` gate-red / gen-drift / api-dump / klib / красный тест | → `/triage-failure` (RED-арм) |
| `== Handoffs ==` не `none` (или правишь хосты/DSN/спеку/`requirements`) | → `/infra-handoff` (блок до claim/коммита) |
| Свежий форк (`tech.stit.mobile`, reference-фичи на месте, stack `draft`) | → `/init-service` |
| Активный план `in-progress` | продолжай со следующего открытого атома; чекпоинт по правилам `/plan` |
| Атом или задача трогает `iosApp/**` вне `Packages/Pure` | режим **Mac-verify**: `make mac-doctor` → правка → `make check-mac`; без Mac — атом остаётся открытым, не «готово» |
| Юзер дал задачу, big (новый экран/фича/entity, контракт, security, > 1 слой) | → `/plan` (planner спрашивает, не угадывает) |
| Задача = скаффолд (entity / feature / screen / flow / entry-point / permission / api) | → `/scaffold` (вариант по kind) |
| Багфикс (любого размера) | → `/triage-failure` (RED-first; **не** `/plan`) |
| «Релизим», «катим версию» | режим **release**: `make status` чист, `check-full` на HEAD → `/release` |
| Задача мелкая и понятная | делай напрямую — по RED→GREEN (§5) |
| Пусто (нет задачи, нет плана, всё зелёно) | спроси юзера, какую задачу решаем |

### 5. RED→GREEN→REFACTOR — дефолтная петля

1. **RED** — сначала тест (`commonTest` для логики, Robolectric/Roborazzi для экрана):
   `make check-fast` / точечно `./gradlew :shared:jvmTest --tests '*XTest*'` → red **по той причине**.
2. **GREEN** — минимальная правда продакшен-кода.
3. **REFACTOR** — при зелёном; `make test` держит зелёным.
4. Перед claim «готово» — `make check`; «готово по iOS» — `make check-mac`.

## Формат вывода

Сводка ≤ 25 строк + явный переход:

```
## Состояние
**Машина:** doctor OK (1 warn: RAM)
**Ветка:** `<branch>` <ahead/behind> · дерево clean
**Платформы:** android ✓ ios ✓ (не верифицирован: check-mac never) · preset companion
**Critical:** GEN ok · API-DUMP ok · KLIB ok · STACK-DRAFTS 2 (push, signing)   (или ATTENTION:<...>)
**Handoffs:** none / INFRA pending: deep-links.md
**План:** `<slug>` in-progress → next A#  (или —)

## Что висит
- <TODO / draft-kinds / stale screenshots — max 3>

## Идеи в бэклоге (если Active пуст)
- <1–3 пункта из roadmap ## Backlog>

## → Перехожу в <skill> / Продолжаю план с A#
<одна фраза почему>
```

Если юзер дал задачу — после сводки сразу §4 (оценка + чейн) одним абзацем.

## Что smartdev НЕ делает

- Не гонит `make check` целиком сам (это перед claim «готово» / коммитом или по просьбе).
- Не делает edits/коммитов в фазе briefing.
- Не `git pull`/`fetch` без явной просьбы.
- Не перепланирует активный план — продолжает его.
- Не запускает `make check-mac` без `MAC_HOST` — печатает, что iOS не верифицирован.

## Anti-patterns

- ❌ Дамп raw `make status` в чат. Сводка — резюме, не сырьё.
- ❌ Briefing на 100 строк. Юзер хочет картину за 30 секунд.
- ❌ Описать проблему и остановиться («есть drift»). Cockpit **переключает**.
- ❌ Багфикс через `/plan`. Багфикс → `/triage-failure`.
- ❌ Считать зелёный Linux-гейт готовностью iOS-UI.
- ❌ Игнорить `== Handoffs ==`.
