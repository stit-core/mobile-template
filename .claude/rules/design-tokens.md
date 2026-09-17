---
name: design-tokens
description: Токены дизайна — DTCG-имена, семантические роли, паритет платформ, тема system/light/dark, font scale/Dynamic Type; сырые цвета и размеры вне designsystem запрещены. Грузится при правке core/designsystem, core/ui, DesignSystemTokens, DesignSystem.
paths:
  - "androidApp/core/designsystem/**"
  - "androidApp/core/ui/**"
  - "iosApp/Packages/Pure/Sources/DesignSystemTokens/**"
  - "iosApp/Packages/Core/Sources/DesignSystem/**"
---

# Токены дизайна

## Инварианты

- **Единственное место сырых значений — токены**: `androidApp/core/designsystem`
  (`Color.kt`, `Type.kt`, `Spacing.kt`, `Shape.kt` → `StitTheme` через `CompositionLocal`
  поверх Material 3) и `Pure/DesignSystemTokens` → `Core/DesignSystem/Theme`. В фичах и
  `core:ui` — только `StitTheme.*` / `Theme.*`. Сырой `0xFF…`/`.dp` в композабле — detekt
  `MagicNumber` (`ignorePropertyDeclaration` — литералы живут в property-декларациях
  designsystem); `Color(red:…)` в Swift — SwiftLint.
- **Имена — DTCG-нотация, семантические роли**: `color.app.bg`, `color.surface.muted`,
  `color.divider`, `color.action.bg/fg`, `color.danger`, `dimension.radius.control`,
  `dimension.space.{xs,sm,md,lg}`, `typography.body.{sm,md}`, `typography.heading.lg`.
  Роль, не палитра: `action.bg`, а не `green500`.
- **Паритет платформ по именам** (значения могут различаться: Material vs Liquid Glass).
  Источник имён — `docs/stack/styling.md § Tokens` (таблица `token · light · dark ·
  android · ios`); новый токен — сначала строка там, потом обе платформы.
- **Тема** — `system | light | dark`, хранится в `Settings` (`app.theme`), применяется на
  корне (`StitTheme(darkTheme = …)` / `.preferredColorScheme`); `dynamicColor = false`.
- **Масштабируемость**: Android — `sp` и `fontScale` 1.3 не ломает экран (скриншот-вариант);
  iOS — Dynamic Type (`.font(.body)`, `relativeTo:`). Touch-target ≥ 48 dp / 44 pt.
- **Контраст** light/dark ≥ 4.5:1 для текстовых ролей — проверяется по таблице токенов при
  добавлении цвета.
- Material 3 без Expressive; иконки — Material Symbols в `res/drawable` (Android) и
  SF Symbols (iOS); `material-icons-*` — нет.
- Каталог компонентов `core:designsystem` — `@Preview` + Roborazzi-тест на каждый
  компонент (`StitThemeScreenshotTest` — образец); изменение токена меняет эталоны →
  осознанный `make screenshots-record`.

## Сигналы

- `make screenshots` красный после правки токена — ожидаемо; проверь `*_compare.png`,
  затем `make screenshots-record` (проходит через промпт).
