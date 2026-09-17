---
name: design-tokens
description: Токены дизайна — design/tokens.json (DTCG) → make gen-tokens → Tokens.kt / Tokens.swift, паритет держит стадия tokens-check; семантические роли, тема system/light/dark, font scale / Dynamic Type; сырые цвета и размеры вне designsystem запрещены. Грузится при правке design/, core/designsystem, core/ui, DesignSystemTokens, DesignSystem.
paths:
  - "design/**"
  - "androidApp/core/designsystem/**"
  - "androidApp/core/ui/**"
  - "iosApp/Packages/Pure/Sources/DesignSystemTokens/**"
  - "iosApp/Packages/Core/Sources/DesignSystem/**"
---

# Токены дизайна

## Инварианты

- **Источник значений — `design/tokens.json`** (DTCG: `$type`, `$value`; цвет — пара
  `light`/`dark` `#RRGGBB`, размер — `<n>dp|sp`). `make gen-tokens` (`scripts/tools/gen-tokens.py`,
  чистый python3) пишет `androidApp/core/designsystem/…/generated/Tokens.kt`
  (`Tokens.Colors/Dimensions/Typography`, `ColorToken(light, dark)`) и
  `iosApp/Packages/Pure/Sources/DesignSystemTokens/Tokens.swift` (`nonisolated enum Tokens`,
  ARGB `UInt32`); оба коммитятся и руками не правятся — стадия `tokens-check` регенерирует во
  временный каталог и сравнивает `diff` (NF-19).
- **Сырые значения живут только в designsystem.** Android — `StitColors.kt`, `StitTypography.kt`,
  `StitSpacing.kt`, `StitShapes.kt`, `StitIcons.kt`, `Theme.kt`: `StitTheme.colors/typography/
  spacing/shapes` через `LocalStitColors/LocalStitTypography/LocalStitSpacing/LocalStitShapes`
  (allowlist detekt `CompositionLocalAllowlist`) поверх Material 3 (`toColorScheme`, dynamic
  color выключен). iOS — `Core/DesignSystem/Theme.swift`: `@Environment(\.theme)` →
  `theme.colors/metrics/typography`, `Color(ColorToken)` выбирает light/dark по trait collection.
  В фичах и `core:ui` — только `StitTheme.*` / `theme.*`.
- **Сырой литерал вне designsystem — красный**: `Color(0xFF…)`, `16.dp`, `14.sp` — detekt
  `MagicNumber` (`ignorePropertyDeclaration`) и Lint `StitRawDesignValue`
  (`:androidApp:lint`, `DesignSystemDetector` — ловит и property-декларации); в Swift —
  SwiftLint `no_hardcoded_color`, `no_fixed_font_size`.
- **Имена — DTCG-нотация, семантические роли**: `color.app.{bg,fg}`, `color.surface.muted`,
  `color.divider`, `color.action.{bg,fg}`, `color.{danger,success,warning,muted}`,
  `dimension.space.{xs,sm,md,lg,xl}`, `dimension.radius.{control,card}`, `dimension.touch.min`,
  `typography.body.{sm,md}`, `typography.heading.lg`; в коде — camelCase (`appBg`, `spaceMd`,
  `radiusControl`, `touchMin`). Роль, не палитра: `action.bg`, а не `green500`.
- **Паритет платформ — один файл на обе.** Новый токен: строка в `design/tokens.json` →
  `make gen-tokens` → потребление в `StitColors`/`StitSpacing`/… и `Theme.swift` → строка в
  `docs/stack/styling.md § Tokens`.
- **Тема** — `ThemeMode` `System | Light | Dark` в `SettingsState` (ключ `settings.theme`),
  применяется на корне: `StitTheme(darkTheme = …)` в `StitApp`, `.preferredColorScheme` в `RootView`.
- **Масштабируемость**: Android — `sp` из токенов через `StitTypography` → Material `Typography`;
  iOS — системные `Font.body/subheadline/title2` (Dynamic Type), pt токенов — ориентир паритета.
  Touch-target — `dimension.touch.min` (48 dp / `metrics.touchMin`).
- **Контраст** текстовых ролей light/dark ≥ 4.5:1, непрозрачность, рост шкал —
  `TokensTests` (`Pure`, `ArgbColor.contrastRatio`; стадия `swift pure build+tests`).
- Material 3 без Expressive; иконки — Material Symbols в `res/drawable` (`StitIcons`, префикс
  `core_designsystem_ic_`) и SF Symbols; `material-icons-*` — нет.
- Компоненты `component/Stit{Button,TextField,ListItem,StatusBadge,TopBar}.kt` — `ComponentsTest` +
  Roborazzi (`ComponentsScreenshotTest`, `StitThemeScreenshotTest`); изменение токена меняет
  эталоны → осознанный `make screenshots-record` (NF-09).

## Сигналы

- `tokens-check` красный → `Tokens.kt`/`Tokens.swift` правлены мимо `design/tokens.json` → `make gen-tokens`.
- `make screenshots` красный после правки токена — ожидаемо: проверь `*_compare.png`, затем `make screenshots-record`.
