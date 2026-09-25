---
name: infra-handoff
description: Оформить handoff-блок хабу или соседу при изменении его поверхности — INFRA-HANDOFF в <org>/infra (.well-known — app id и отпечатки, DSN, хосты, URL политики), BACKEND-HANDOFF в <org>/backend (B-* — endpoint'ы, поля, auth-режим, min_version), STRATEGY-HANDOFF в <org>/strategy (S-* — расхождение с картой mobile/). Применяй, когда правишь deep-links/push/config/distribution/signing/observability.md, openapi.lock, core/api, core/requirements, когда make status показал Handoffs или изменение требует действия инфры, бэкенда или карты продукта.
---

# Skill: infra-handoff

Процедура фиксации изменения, которое требует действия соседа. Зона репо — код → Gitea
Release; всё, что нужно инфре, бэкенду или карте продукта, должно уйти им **явным
структурированным сообщением**, а не остаться в голове. Получатель — операционный хаб
`<org>/infra` (он ведёт общий план и рассылает задачи по сиблингам) или сосед-адресат.
Правило-якорь — `.claude/rules/infra-impact.md` (грузится при правке путей из его `paths:`);
этот skill — как именно оформить и куда положить. Формы блоков — `references/forms.md`.

## Когда применять

- `make status` → `== Handoffs ==` не `none` (cockpit чейнит сюда)
- Правишь: `docs/stack/{deep-links,push,config,distribution,signing,observability}.md`,
  `api/openapi.lock`, `shared/**/core/api/**`, `shared/**/core/requirements/**`, манифест/
  entitlements с новым хостом
- `/init-service` стадия 5 или `/stack-deep-dive` нашёл, что у соседа нет операции/канона
- Юзер: «это задевает инфру», «бэкенд не умеет», «в карте этого нет»

## Когда не применять

- Внутренняя логика/тесты/рефактор без смены хостов, спеки, карты
- Правка значений в пределах уже согласованных хостов и env
- Уже оформлен актуальный блок для этого же изменения — не дублируй

## Шаги

### 1. Классифицировать и выбрать адресата

| адресат | класс изменения | код |
|---|---|---|
| **`<org>/infra`** | deep-link-хост / отпечаток → раздача `.well-known` на хосте: `assetlinks.json` (`applicationId`, SHA-256 upload / Play app-signing / RuStore) и AASA (Team ID · bundle id, пути); `SENTRY_DSN` per env; `API_BASE_URL` per env; URL политики и веб-удаления аккаунта | I-* |
| **`<org>/backend`** | отсутствующий endpoint/поле (`POST /api/auth/refresh` + `X-Client`, `GET /api/app/requirements`, `PUT /api/v1/devices`, ресурс фичи); `ErrorModel.code`; `Idempotency-Key`-store; `min_version` после раскатки; push-событие/категория | B-* |
| **`<org>/strategy`** | экран/фича/entity/entry-point/permission/системное действие/виджет/фоновая задача без записи или с расхождением в `mobile/<surface>/`; нужен канон слоя (`docs/reference/strategy-mobile-canon/`) | S-* |

Ни один класс не подходит — это не касание соседа, выходи.

### 2. Сформировать блок

Форма едина (WHAT/WHY/ACTION/WHO/DECIDE/RISK), заголовок — по адресату; полные образцы —
`references/forms.md`:

```
=== INFRA HANDOFF · передай инфра-админу (<org>/infra) ===
WHAT   <env/файл/сервис; значение или «нужно»; per env>
WHY    <фича/причина — коммит/план-атом>
ACTION <конкретное действие: env на каждом VPS, make ship, проверка curl>
WHO    <org>/infra
DECIDE <что решает админ: хосты, значения>
RISK   <последствие бездействия: ссылки открываются в браузере, крэши без DSN>
===
```

Конкретно. `WHAT: assetlinks.json на app.<domain> (prod) — добавить SHA-256 Play app-signing
<отпечаток из Play Console>` — годится. `WHAT: что-то с deep links` — нет. Значение секрета **не**
писать — только имя ключа.

### 3. Положить так, чтобы доехало до релиза

- **Ответ:** задача пришла из хаба `<org>/infra` — блок идёт в ответ хабу; прямой заход — покажи
  блок юзеру с явным «передай <адресату>». Из этого репо в соседний не пишем.
- **Активный план:** добавь/дополни раздел `## Handoff` активного плана
  (`.claude/plans/<slug>.md`); `/release` собирает его в STORE-HANDOFF (`BACKEND`, `ENV`).
  Закрытый план удаляется — handoff обязан уехать адресату **до** этого; если план
  закрывается раньше релиза — строкой в `.claude/plans/backlog.md`.
- **Нет активного плана:** блок только в ответ (чат или хабу); при прямом заходе проследи, что
  юзер подтвердил передачу, прежде чем пометить «готово».

### 4. Граница

Не выполняй `make ship`/`ssh` инфры, не правь `../backend`/`../strategy`/`../infra` — соседи
read-only. Роль — сформировать и отдать сообщение.

## Anti-patterns

- ❌ Пометить задачу «готово»/закоммитить без предъявленного блока.
- ❌ «Допишу на релизе» — release агрегирует уже записанное; не записал → потеряно.
- ❌ Значение секрета в блоке/чате.
- ❌ Размытый WHAT/ACTION — админ не сможет действовать.
- ❌ Самому чинить карту strategy или спеку бэкенда «чтобы не ждать».
- ❌ Гнать блок на не-касание (шум обесценивает сигнал).
