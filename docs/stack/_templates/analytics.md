---
id: stack.analytics
kind: analytics
name: analytics
status: draft
---

# analytics

Условный kind: существует при `service.md#preset = consumer` или `ANALYTICS ≠ none`.

## Provider

| поле | значение |
|---|---|
| provider | `none` _(`appmetrica` \| `self-hosted` \| `none`)_ |
| SDK | _…_ (строка в `dependencies.md` + `compliance.md`) |
| данные | _события без ПДн; трансграничка — уведомление РКН (L-11)_ |

## Events

Реестр — `shared-lib.md#analytics-events` карты strategy; здесь — платформенная привязка.

| событие | параметры | экран/фича | согласие |
|---|---|---|---|
| _`screen_view`_ | _`screen`_ | все | да |

## Consent

Отдельный переключатель на первом запуске и в настройках; SDK инициализируется только после
согласия; отказ — события не собираются.

## Identifiers

`AD_ID` и ATT не запрашиваются; идентификатор — `installation_id` (не рекламный).

## Generates

- `config/env/*.properties`, `*.xcconfig` — `ANALYTICS`
- `TelemetrySink`-реализация провайдера в `core:platform` / `Core/Platform`
- `consent` в `feature/settings`
- строки `compliance.md#data-collected`, `dependencies.md#sdks`

## Открытые вопросы

- _что_
