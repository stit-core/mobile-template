---
id: stack.styling
kind: styling
name: styling
status: draft
---

# styling

## Tokens

Имена — DTCG-нотация, семантические роли; паритет платформ по именам (значения могут
различаться). Источник значений — `androidApp/core/designsystem` и `Pure/DesignSystemTokens`.

| token | light | dark | android | ios |
|---|---|---|---|---|
| `color.app.bg` | _#…_ | _#…_ | `StitTheme.colors.appBg` | `Theme.colors.appBg` |
| `color.app.fg` | _…_ | _…_ | `colors.appFg` | `colors.appFg` |
| `color.surface.muted` | _…_ | _…_ | `colors.surfaceMuted` | `colors.surfaceMuted` |
| `color.divider` | _…_ | _…_ | `colors.divider` | `colors.divider` |
| `color.action.bg` / `color.action.fg` | _…_ | _…_ | `colors.actionBg/Fg` | `colors.actionBg/Fg` |
| `color.danger` | _…_ | _…_ | `colors.danger` | `colors.danger` |
| `dimension.radius.control` | _…_ | — | `shapes.control` | `metrics.radiusControl` |
| `dimension.space.{xs,sm,md,lg}` | _…_ | — | `spacing.*` | `metrics.space*` |
| `typography.body.{sm,md}`, `typography.heading.lg` | _…_ | — | `typography.*` | `typography.*` |

## Theme

`system | light | dark`, хранится в `Settings` (`app.theme`), применяется на корне;
`dynamicColor = false`. Контраст текстовых ролей light/dark ≥ 4.5:1.

## Components

Android — Material 3 (без Expressive), иконки Material Symbols в `res/drawable`; iOS — системные
компоненты, Liquid Glass (`.glassEffect()` на функциональных элементах), SF Symbols. Каталог
`core:designsystem` с `@Preview` + Roborazzi на компонент.

## Accessibility

Content descriptions / `accessibilityLabel` у интерактивных элементов; Dynamic Type / `fontScale`
1.3 не ломает экраны (скриншот-вариант); touch-target ≥ 48 dp / 44 pt.

## i18n

| locale | роль |
|---|---|
| `ru` | `default` |
| `en` | `supported` |

Реестр ключей — `shared-lib.md` карты strategy; ошибки API — `error.<code>`
(`MessageKey.forErrorCode`); плюрализация (`one/few/many/other` для ru) — тест на 1/2/5; форматы
дат/чисел — только платформенные форматтеры (`java.time`/`NumberFormat` на Android,
`FormatStyle`/`NumberFormat` на iOS) над `kotlin.time.Instant` из `shared`; строки не в `shared`
(нативные ресурсы).

## Generates

- `androidApp/core/designsystem/…/{Color,Type,Spacing,Shape,Theme}.kt`
- `iosApp/Packages/Pure/Sources/DesignSystemTokens/*.swift`, `Packages/Core/Sources/DesignSystem/Theme.swift`
- `androidApp/*/src/main/res/values{,-ru}/strings.xml`, `iosApp/…/Localizable.xcstrings`
- `dependencies.md` — шрифты (если не системные)

## Открытые вопросы

- _что_
