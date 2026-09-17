# scaffold › permission — разрешение ОС

Читает строку `permissions.md` карты (`slug · Android permission id(s) · iOS usage-key · зачем
(rationale) · когда запрашивается · fallback без разрешения · status`). Без записи в карте
разрешение не заводится: магазины требуют обоснование, `compliance.md` — строку.

## Вопросы до начала (≤ 2)

1. В какой момент ценности запрашивается (экран/фича) — не на первом экране?
2. Fallback без разрешения описан (что работает, что нет)?

## Раскладка

```
androidApp/core/platform/src/main/kotlin/…/<Slug>PermissionGate.kt     # Activity Result API; образец — NotificationPermissionGate (register(activity) в MainActivity.onCreate, suspend isGranted()/request())
androidApp/feature/<group>/…/<Slug>OptIn{Route,Screen,ViewModel}.kt      # pre-prompt (текст из rationale, ru/en), кнопки «Разрешить»/«Позже»; образец — PushOptIn* в feature/settings
androidApp/core/platform/src/main/AndroidManifest.xml                     # <uses-permission …/> в модуле, который его использует (+ maxSdkVersion где уместно)
iosApp/Packages/Core/Sources/Platform/<Slug>Permission.swift              # системный API; образец — NotificationCenterPermission (suspend Boolean → KotlinBoolean)
iosApp/Packages/Features/Sources/<Group>Feature/<Slug>OptInView.swift     # pre-prompt; образец — PushOptInView
iosApp/App/Info.plist                                                     # NS<…>UsageDescription (ru/en)
shared/…/PlatformDeps.kt                                                  # + интерфейс, если состояние нужно презентеру (образец — core/push/NotificationPermission)
docs/stack/compliance.md                                                  # + строка permissions-матрицы: id · зачем · данные · Data safety
```

## Шаги

1. Строка в `compliance.md` (обоснование из rationale карты, что собирается, куда уходит).
2. Android: RED — Robolectric-тест pre-prompt-экрана (состояния) → gate → манифест модуля.
3. iOS: usage-description непустой и не плейсхолдер (L-3) → `Platform/<Slug>Permission.swift` (Mac-verify).
4. Фича, которой нужно разрешение, обрабатывает `Denied` fallback'ом из карты.
5. `make check`; при новом типе собираемых данных — Data safety / Nutrition Label (L-13/L-14)
   человеку в STORE-HANDOFF.

## Anti-patterns

- ❌ Permission «на всякий случай» / в манифесте без строки `compliance.md` (L-1/L-2 класс).
- ❌ Системный запрос без pre-prompt.
- ❌ `AD_ID`, ATT, location-always без отдельного решения владельца.
- ❌ Фича падает без разрешения вместо fallback.
