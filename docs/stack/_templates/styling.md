---
id: stack.styling
kind: styling
name: styling
status: draft
---

# styling

## Tokens

Имена — DTCG-нотация, семантические роли; паритет платформ по именам. Источник значений —
`design/tokens.json` (DTCG); `make gen-tokens` пишет `Tokens.kt`/`Tokens.swift`, стадия
`tokens-check` держит паритет; потребители — `androidApp/core/designsystem` и `Core/DesignSystem/Theme.swift`.

| token | light | dark | android | ios |
|---|---|---|---|---|
| `color.app.bg` | _#…_ | _#…_ | `StitTheme.colors.appBg` | `Theme.colors.appBg` |
| `color.app.fg` | _…_ | _…_ | `colors.appFg` | `colors.appFg` |
| `color.surface.muted` | _…_ | _…_ | `colors.surfaceMuted` | `colors.surfaceMuted` |
| `color.divider` | _…_ | _…_ | `colors.divider` | `colors.divider` |
| `color.action.bg` / `color.action.fg` | _…_ | _…_ | `colors.actionBg/Fg` | `colors.actionBg/Fg` |
| `color.{danger,success,warning,muted}` | _…_ | _…_ | `colors.danger/success/warning/muted` | `colors.danger/success/warning/muted` |
| `dimension.radius.{control,card}` | _…_ | — | `shapes.control/card` | `metrics.radiusControl/radiusCard` |
| `dimension.space.{xs,sm,md,lg,xl}` | _…_ | — | `spacing.xs…xl` | `metrics.spaceXs…spaceXl` |
| `dimension.touch.min` | _…_ | — | `spacing.touchTarget` | `metrics.touchMin` |
| `typography.body.{sm,md}`, `typography.heading.lg` | _…_ | — | `typography.bodySm/bodyMd/headingLg` | `typography.bodySm/bodyMd/headingLg` |

## Theme

`system | light | dark` (`ThemeMode`), хранится в `Settings` (`settings.theme`), применяется на
корне; dynamic color выключен. Контраст текстовых ролей light/dark ≥ 4.5:1 (`TokensTests` в `Pure`).

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

Реестр ключей — `shared-lib.md` карты strategy; паритет `values/` == `values-ru/` и строки
`core_ui_error_<code>` держит стадия `i18n-check` (iOS xcstrings — advisory); ошибки API — `error.<code>`
(`MessageKey.forErrorCode`); плюрализация (`one/few/many/other` для ru) — тест на 1/2/5; форматы
дат/чисел — только платформенные форматтеры (`java.time`/`NumberFormat` на Android,
`FormatStyle`/`NumberFormat` на iOS) над `kotlin.time.Instant` из `shared`; строки не в `shared`
(нативные ресурсы).

## Generates

- `design/tokens.json` → `make gen-tokens` → `androidApp/core/designsystem/…/generated/Tokens.kt`, `iosApp/Packages/Pure/Sources/DesignSystemTokens/Tokens.swift` (стадия `tokens-check`)
- `androidApp/core/designsystem/…/{StitColors,StitTypography,StitSpacing,StitShapes,StitIcons,Theme}.kt`, `Packages/Core/Sources/DesignSystem/Theme.swift`
- `androidApp/*/src/main/res/values{,-ru}/strings.xml`, `iosApp/**/Resources/Localizable.xcstrings` (стадия `i18n-check`)
- `dependencies.md` — шрифты (если не системные)

## Открытые вопросы

- _что_
