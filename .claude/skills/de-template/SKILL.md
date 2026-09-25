---
name: de-template
description: Развернуть форк mobile-template в самостоятельный проект по stable-артефактам docs/stack (карта — в соседнем <org>/strategy) — пакет и applicationId/bundle id, схема deep link, README, конфиги env/xcconfig, prune reference-фич profile/todos, обрезка платформы (--platform android|ios), пресетные kinds. Применяй, когда форк пора де-темплейтизировать — юзер просит развернуть фундамент или сделать из шаблона свой проект, либо init-service дошёл до стадии 6. Движок skill-only: механика и prune — reviewable edits под этой процедурой. На каноническом репозитории шаблона не запускается — это уничтожило бы шаблон.
---

# Skill: de-template (движок развёртывания)

Превращает форк шаблона в проект, который ничего не помнит про `mobile-template`. Читает
**stable** `docs/stack` и разворачивает фундамент по секциям `Generates`; карта продукта живёт в
`<org>/strategy` и разворачивается отдельно (`/scaffold`), не здесь. Движок **skill-only**:
committed-скрипта нет — механика (пакет, id, prune, обрезка) идёт точными командами ниже,
всё остальное — reviewable diff, который юзер ревьюит.

Запускается **один раз** на форке. Финал — зелёный `make check` (и `make check-mac` при iOS) и
проект без следов шаблона. Флаги: `--platform android|ios` (§2.8 формы — `references/platform.md`),
`--keep todos` (оставить фичу как учебную), `--no-strategy`, `--ios-unverified`.

## Когда применять

- «/de-template» / «разверни фундамент» / «сделай из шаблона свой проект»
- `/init-service` дошёл до стадии 6 (стек `stable`, карта готова)

## Когда не применять

- Стек не `stable` — сначала `/stack-deep-dive`; карты нет и нет `--no-strategy` — сначала
  `/mobile-deep-dive` в `<org>/strategy`.
- Проект уже де-темплейтизирован (свой `applicationId`, reference-фич нет) — `/smartdev`.
- **Канонический репозиторий шаблона** — никогда (guard ниже).

## Gate (проверить до любых правок)

1. **Не канонический шаблон.** `applicationId` в `androidApp/app/build.gradle.kts` всё ещё
   `tech.stit.mobile` **и** каталог — не временная копия/форк → остановись и спроси юзера явно:
   на каноническом шаблоне механика ниже необратимо сотрёт шаблон.
2. **Стек stable.** Все обязательные kinds существуют и `stable`, условные — по пресету
   (инвариант 7), `## Generates` непусты:
   ```bash
   preset=$(grep -m1 -E '^\| *preset *\|' docs/stack/service.md | awk -F'|' '{print $3}' | tr -d '` ')
   kinds="service runtime api auth deep-links push background observability styling distribution signing compliance dependencies config"
   [ "$preset" = consumer ] && kinds="$kinds analytics"
   for k in $kinds; do
     f="docs/stack/$k.md"
     [ -f "$f" ] || { echo "STOP: $f нет — стек не спроектирован (/stack-deep-dive)"; continue; }
     grep -q '^status: stable' "$f" || echo "STOP: $f не stable"
     grep -qE '^## Generates' "$f" || echo "STOP: $f без Generates"
     grep -q 'blocked-by' "$f" && echo "STOP: $f blocked-by"
   done
   ```
   Cross-kind-инварианты 1–8 (`docs/stack/README.md`) выполнены; `platforms: [android]` ⇒
   iOS-секций в kinds нет.
3. **Карта в strategy.** `<strategy-path>/docs/projects/<slug>/mobile/<surface>/` — 12 файлов
   карты на месте, **все записи** в них `stable` (`/mobile-audit` там): гейт считает записи, не
   файлы. `system-actions.md`, `widgets.md`, `background-tasks.md` бывают без записей —
   приложение без голоса, виджетов и фоновой работы нормально, пустой файл гейт не блокирует.
   Карты нет — `--no-strategy` + строка в `.claude/plans/backlog.md` «завести карту».
4. **Гейт зелёный:** `make check` → 0; `make gate-selftest` → все красные.
5. **iOS:** `ios ∈ platforms` ⇒ `make check-mac` зелёный на HEAD **или** явный
   `--ios-unverified` → строка-долг в `.claude/plans/backlog.md`
   («iOS не верифицирован на Mac»).
6. **State чист:** `grep -RlE '\*\*blocker\*\*' .claude/skills/stack-deep-dive/state/` пуст.

## Phase A — детерминированная механика (по одному reviewable диффу)

### A.1 Пакет, applicationId, bundle id

Из `docs/stack/service.md § Generates`: `<pkg>` (`<reverse-domain>.<app>`), `applicationId`, bundle id.

```bash
OLD=tech.stit.mobile
NEW=<pkg из service.md>
git grep -lF "$OLD" -- ':!docs/research' | xargs sed -i "s/$OLD/$NEW/g"
git mv shared/src/commonMain/kotlin/tech/stit/mobile shared/src/commonMain/kotlin/$(echo "$NEW" | tr . /)
# то же для commonTest, androidMain, androidHostTest, iosMain, jvmMain, jvmTest и androidApp/*/src/*/kotlin
```

Проверь: `androidApp/app/build.gradle.kts` (`applicationId`, `.demo`), `namespace` каждого
модуля, `config/env/*.properties` (`DEEP_LINK_HOST`, `DEEP_LINK_SCHEME`),
`iosApp/Config/*.xcconfig` (`BUNDLE_ID` в `Base.xcconfig`, team — только xcconfig, `.pbxproj` не
трогать), `e2e/*/*.yaml` (`appId`; `make run-android` берёт id из `scripts/tools/app-id.sh`),
Konsist `ArchitectureTest` в `shared/src/jvmTest`
(`PKG`) и в `androidApp/app` (`FEATURE_PACKAGE`), `.gate-fixtures/*` (пути пакетов), а также пути
со слэшами `tech/stit/mobile` — `shared/build.gradle.kts` (`embedFixtures` → `Fixtures.kt`),
`scripts/tools/gen.sh`/`gen-check.sh` (`TARGET`), `config/openapi/kotlin-models.yaml`; после смены
пакета моделей — `make gen` + `make api-dump`. Имя `Shared`-фреймворка не меняется.

### A.2 README и идентичность

Шапка `README.md` — из `service.md` (имя, назначение, платформы, магазины); убрать «шаблон»/
«template»/«форкни»; ссылки на `docs/reference/*` остаются. `LICENSE`, `SECURITY.md` — по
решению юзера (шаблон — Apache-2.0).

### A.3 Конфиги env

Из `docs/stack/config.md § Build-time`: `config/env/{dev,staging,prod,demo}.properties` и
`iosApp/Config/{Dev,Staging,Prod,Demo}.xcconfig` — хосты, `AUTH_MODE`, `PUSH_PROVIDERS`,
`SENTRY_DSN`; `.env.local.example` — имена release-time ключей из `signing.md`.

### A.4 Обрезка платформы (`--platform android|ios`)

По `references/platform.md`: удаление половины дерева, таргетов `shared`, правил, iOS/Android-
секций kinds, Swift/Android-стадий гейта и групп `Makefile`.

### A.5 Strip шаблонной меты

- Удали `docs/getting-started.md` ([META]).
- `docs/contributing.md` — оставь (система наследуется); «шаблон» → имя проекта.
- `docs/reference/strategy-mobile-canon/` — оставь только при `--no-strategy` (иначе канон уже
  в strategy).
- `docs/research/` — удали целиком (ресёрч шаблона, не проекта).
- `.claude/plans/` — форк стартует без планов шаблона: только `.claude/plans/backlog.md` с `_пусто_`
  (+ долг `--ios-unverified`, если был).

## Phase B — prune и docs

- **Prune reference-фич** `profile`/`todos` — полный чек-лист 9 точек в **`references/prune.md`**.
  Делать **после** того, как свои фичи спроектированы в strategy (иначе теряешь живой образец);
  `--keep todos` оставляет `todos` учебной.
- **Пресет**: `consumer` — `analytics.md`/`payments.md` из `_templates` уже `stable` (Gate);
  `enterprise` — каналы `/release` `gitea,abm`, App Review-блоки чек-листа выключены.
- **Docs**: `docs/reference/strategy-mapping.md` — таблица reference-соответствий очищается;
  примеры `todos`/`profile` в `docs/stack/*.md` заменены проектными; `docs/reference/architecture.md#reference`
  — оставшиеся reference-фичи (`demo`, `shell`, `requirements`, `auth`, `settings`, `push`).
- **Фикстуры и i18n**: `mock/fixtures/*/{todos,user}.json` и строки `feature_todos_*`/
  `feature_profile_*` удалены; `make mock-check` зелёный.
- **Compliance**: плейсхолдеры `{{ORG_NAME}}`, `{{ORG_INN}}`, `{{DPO_EMAIL}}` в правовых текстах
  заменены (L-7) — `git grep -n '{{ORG_'` пуст.

## Verify

```bash
make check            # Linux-полный гейт
make gate-selftest    # фикстуры красные (пути пакетов обновлены)
make check-mac        # при ios ∈ platforms (или долг --ios-unverified в backlog.md)
make kin-diff         # advisory: что разошлось с экосистемой (WARN допустим)
```

Зелёный `make check` без следов `tech.stit.mobile` (`git grep -n 'tech\.stit\.mobile'` пуст
вне `docs/reference/kinship.md`) — главный критерий.

## Anti-patterns

- ❌ Запуск на каноническом шаблоне — уничтожает шаблон.
- ❌ Старт до `stable`-артефактов и зелёного гейта.
- ❌ Prune reference-фич раньше, чем свои спроектированы.
- ❌ Правка `.pbxproj` для bundle id — только xcconfig.
- ❌ «Пустые» iOS-секции вместо отсутствующих при `--platform android`.
- ❌ Значения секретов в `.env.local.example`.

## См. также

- `references/prune.md` — удаление reference-фич (9 точек).
- `references/platform.md` — обрезка `--platform android|ios` и обратимость.
- `docs/stack/README.md` — kinds, `Generates`, cross-kind-инварианты.
- `.claude/skills/init-service/SKILL.md` — оркестратор, чейнит сюда последним шагом.
