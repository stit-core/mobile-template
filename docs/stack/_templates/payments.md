---
id: stack.payments
kind: payments
name: payments
status: draft
---

# payments

Условный kind: существует при монетизации внутри приложения (`preset: consumer`). При
`companion` платежей в приложении нет (App Store 3.1.3(c)).

## Providers

| платформа / магазин | провайдер | SDK |
|---|---|---|
| Android / RuStore | RuStore Pay (только ИП/ЮЛ) | _…_ |
| Android / Google Play | Play Billing (для расчётного счёта в РФ — выключен) | _…_ |
| iOS / App Store | StoreKit 2 | системный |

## Products

| product id | тип (`consumable` \| `subscription` \| …) | где показывается |
|---|---|---|
| _…_ | _…_ | _…_ |

## Restore

Восстановление покупок — экран/действие в настройках; серверная запись — источник правды.

## Server validation

Чеки валидирует бэкенд (BACKEND-HANDOFF: endpoint валидации per провайдер); приложение
показывает состояние из `entity/<purchase>`.

## Store rules

Нет ссылок на внешнюю оплату внутри приложения (3.1.1); цены — из магазина; отмена подписки —
системный экран.

## Generates

- шов `Payments` в `core:platform` / `Core/Platform`, интерфейс в `PlatformDeps`
- `entity/purchase` в `shared` + фикстуры
- строки `compliance.md`, `dependencies.md`; BACKEND-HANDOFF (валидация чеков)

## Открытые вопросы

- _что_
