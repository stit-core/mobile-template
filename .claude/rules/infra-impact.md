---
name: infra-impact
description: Что считается «касанием» соседей и какой блок печатать — INFRA-HANDOFF (<org>/infra: wellknown env, DSN, хосты), BACKEND-HANDOFF (<org>/backend: B-* endpoint'ы, поля, auth), STRATEGY (расхождение с картой mobile/); формат WHAT/WHY/ACTION/WHO/DECIDE/RISK; блок — до claim «готово». Грузится при правке deep-links/push/config/distribution/signing/observability.md, openapi.lock, core/api, core/requirements.
paths:
  - "docs/stack/deep-links.md"
  - "docs/stack/push.md"
  - "docs/stack/config.md"
  - "docs/stack/distribution.md"
  - "docs/stack/signing.md"
  - "docs/stack/observability.md"
  - "api/openapi.lock"
  - "shared/**/core/api/**"
  - "shared/**/core/requirements/**"
---

# Infra-impact

Репо останавливается на Gitea Release; окружение, `.well-known`, бэкенд и карта продукта — соседи.
Изменение, **требующее действия соседа**, молча разъедется с прод-окружением, если его не
зафиксировать. Процедура и формат блока — skill `/infra-handoff`.

**Железобетонно:** тронул поверхность соседа → до claim «готово» / коммита оформи handoff-блок
и **попроси юзера передать адресату**. Soft-механизм (правило + skill, не хук) — гарантия
только в твоей дисциплине.

## Что считается касанием соседа

| адресат | триггер | блок |
|---|---|---|
| **`<org>/infra`** | новый/изменённый deep-link-хост или отпечаток подписи (`deep-links.md`, `signing.md`); `SENTRY_DSN`/хост GlitchTip; `API_BASE_URL` per env; URL политики/удаления аккаунта (`compliance.md`); `MOBILE_*` env сервиса `wellknown` | INFRA-HANDOFF (I-*) |
| **`<org>/backend`** | нужный endpoint/поле отсутствуют в `api/openapi.yaml` соседа (`POST /api/auth/refresh`, `GET /api/app/requirements`, `PUT /api/v1/devices`, новый ресурс); несовместимое изменение спеки; `min_version` после раскатки; push-категория/событие; `AuthMode.Cookie` | BACKEND-HANDOFF (B-*) |
| **`<org>/strategy`** | код расходится с картой `mobile/<surface>/` (экран/фича/entry-point/permission без записи или с другим `status`); нужна новая запись | STRATEGY-HANDOFF (S-*) — карта правится там, `/mobile-deep-dive` |

Не-касание: внутренняя логика, тесты, рефактор, правка значений в `config/env` в пределах
уже согласованных хостов, code-default без нового обязательного ключа.

## Куда кладётся

1. **В чат** — блок юзеру с явным «передай <адресату> out-of-band».
2. **В активный план** — подсекция `**Infra impact.**` в `### `-секции
   `.claude/plans/roadmap.md`; `/release` агрегирует её в STORE-HANDOFF (`BACKEND`/`ENV`).
   План удаляется по закрытии — блок обязан переехать в handoff **до** этого.

Нет активного плана — блок только в чат, проследи, что юзер зафиксировал передачу.

## Инварианты

- Блок — **до** claim «готово»/коммита; один блок на одно цельное изменение.
- Конкретика: имя переменной/операции, значение или default, точное действие, адресат.
- ❌ Молча закоммитить новый хост, отпечаток, DSN, endpoint-зависимость без блока.
- ❌ Самому писать в соседний репо (`../strategy`, `../backend`, `../infra`) — они read-only.
- ❌ Значение секрета в блоке/чате — только имя ключа + факт «нужен».

## Сигналы

- `make status` → `== Handoffs ==` не `none` (cockpit чейнит в `/infra-handoff`).
- Правишь файл под `paths:` этого правила.
