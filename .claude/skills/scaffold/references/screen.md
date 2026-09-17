# scaffold › screen — экран на платформах

Читает строку `screens.md` карты (`key` PascalCase · `name` · `entry` · `states` · `entities` ·
`features` · `platforms` · `situation-ref` · `status`). Презентер фичи (`--feature <slug>`) уже
существует в `shared` — иначе сначала `/scaffold feature`.

## Вопросы до начала (≤ 3)

1. `states` записи (минимум `loading, error` для экрана с сетью)?
2. `entry`: `tab | push | modal | deep-link | sheet` — как экран достигается (влияет на `Route`
   и `shell.md`)?
3. `platforms`: `all | android | ios`?

## Раскладка

```
shared/…/core/deeplink/Route.kt                      # + data object/class <Key>(id?) : Route   (если entry = deep-link или экран адресуем)
androidApp/core/navigation/…/Destination.kt          # + Destination.<Key> (+ isTopLevel при entry = tab) и ветка Route.toDestination()
androidApp/feature/<slug>/src/main/kotlin/<pkg>/android/feature/<slug>/
├── <Key>Route.kt        # stateful: hiltViewModel(), collectAsStateWithLifecycle(), вызывает <Key>Screen
├── <Key>Screen.kt       # stateless <Key>Screen(state: <Key>State, onIntent: (<Key>Intent) -> Unit, …колбэки, modifier)
├── <Key>ViewModel.kt    # @HiltViewModel: тонкая обёртка презентера из SharedContainer (Start в init, close в onCleared)
├── <Group>NavEntry.kt   # + entry<Destination.<Key>> { <Key>Route(...) } в fun EntryProviderScope<NavKey>.<group>Entries(...)
├── <Group>Previews.kt   # + @Preview на каждое состояние (private)
└── <Group>TestTags.kt   # + testTag-константы (Compose-тесты и Maestro)
androidApp/app/…/AppNavigation.kt                    # колбэк перехода в <group>Entries(...) — только если экран открывается из другой фичи
androidApp/feature/<slug>/src/test/kotlin/…/<Key>ScreenTest.kt        # Robolectric + Compose test v2: каждое состояние рендерится, интенты уходят
androidApp/feature/<slug>/src/test/kotlin/…/<Group>ScreenshotTest.kt  # Roborazzi (ScreenshotTest.capture): <key>_<state>[_dark].png
androidApp/feature/<slug>/src/main/res/values{,-ru}/strings.xml       # feature_<slug>_* — оба файла (i18n-check)
iosApp/Packages/Features/Sources/<Group>Feature/
├── <Key>View.swift      # SwiftUI, switch по State; accessibilityIdentifier "<slug>.<key>…"
├── <Key>Model.swift     # final class <Key>Model: PresenterModel<<Key>State, <Key>Intent> + методы-интенты
└── Resources/Localizable.xcstrings                     # ключи <slug>.<key>.* (ru/en)
iosApp/Packages/Core/Sources/Routing/AppRoute.swift  # + case (если адресуем) + RouteBridge; App/RootView.swift — destination(...) и фабрика в AppEnvironment
iosApp/Packages/Features/Tests/<Group>FeatureTests/<Key>ModelTests.swift   # Swift Testing на Scenario.container + waitUntil
iosApp/Packages/Features/Tests/SnapshotTests/ScreenSnapshotTests.swift     # + @Test на состояние: Snapshots.assertScreen (Mac)
```

`Shell` живёт в `Features/Sources/Shell/` (без суффикса `Feature`); модуль Android и таргет iOS
заводятся на группу экранов (`auth`, `todos`, `profile`, `settings`), не на экран.

## Шаги

1. `Route.<Key>` в `shared` (если адресуем) + ветка `DeepLinkParser` при `entry = deep-link`
   (`/scaffold entry-point`); `make api-dump`.
2. Android: RED — `<Key>ScreenTest` на каждое состояние → экран → превью → Roborazzi
   (`make screenshots-record` осознанно, эталоны в коммит) → `Destination` + `entry` в
   `<group>Entries` → колбэк в `AppNavigation`, если нужен.
3. iOS (при `platforms ∋ ios`): View + Model + фабрика `AppEnvironment.<key>Model()` + snapshot-тест;
   верификация — атом «Mac-verify».
4. Строки в ресурсах обеих платформ (`make i18n-check`); `shell.md` — если `entry = tab`,
   обновить `ShellTab` (Android) и `AppTab`/`RootView` (iOS).
5. `make check`; при iOS — `make check-mac`.

## Anti-patterns

- ❌ `hiltViewModel()` внутри `<Key>Screen` (не previewable; Konsist).
- ❌ Состояние из превью-моков, а не из `State` презентера.
- ❌ Экран без Roborazzi на `error`/`empty`.
- ❌ Литералы строк/цветов; `Color(0xFF…)` — detekt + Lint `StitRawDesignValue`.
- ❌ iOS-View с логикой (форматирование дат, ветвления бизнес-правил) — это презентер.
