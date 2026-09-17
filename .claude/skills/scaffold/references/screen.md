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
androidApp/feature/<slug>/src/main/kotlin/<pkg>/android/feature/<slug>/
├── <Key>Route.kt        # stateful: hiltViewModel(), collectAsStateWithLifecycle(), вызывает <Key>Screen
├── <Key>Screen.kt       # stateless <Key>Screen(state: <Slug>State, onIntent: (<Slug>Intent) -> Unit, modifier)
├── <Key>ViewModel.kt    # @HiltViewModel: тонкая обёртка презентера из SharedContainer
├── <Key>NavEntry.kt     # entryProvider-регистрация NavKey ↔ Route.<Key>
└── <Key>Previews.kt     # @Preview на каждое состояние (private, PreviewParameter из State)
androidApp/feature/<slug>/src/test/kotlin/…/<Key>ScreenTest.kt       # Robolectric + Compose test v2: каждое состояние рендерится, интенты уходят
androidApp/feature/<slug>/src/test/kotlin/…/<Key>ScreenshotTest.kt   # Roborazzi: <Key>_<state>.png (+ dark, + fontScale 1.3 для ключевых)
androidApp/feature/<slug>/src/main/res/values{,-ru}/strings.xml      # feature_<slug>_* ключи
iosApp/Packages/Features/Sources/<Slug>Feature/
├── <Key>View.swift      # SwiftUI, switch по State
├── <Key>Model.swift     # @Observable адаптер презентера (for await state)
└── (umbrella Features реэкспорт)
iosApp/Packages/Features/Tests/<Slug>FeatureTests/<Key>SnapshotTests.swift   # снапшот на состояние (Mac)
iosApp/…/Localizable.xcstrings                                               # ключи <slug>.<key>.*
```

## Шаги

1. `Route.<Key>` в `shared` (если адресуем) + ветка `DeepLinkParser` при `entry = deep-link`
   (`/scaffold entry-point`); `make api-dump`.
2. Android: RED — `<Key>ScreenTest` на каждое состояние → экран → превью → Roborazzi
   (`make screenshots-record` осознанно, эталоны в коммит) → `NavEntry`, регистрация в `app`.
3. iOS (при `platforms ∋ ios`): View + Model + snapshot-тест; верификация — атом «Mac-verify».
4. Строки в ресурсах обеих платформ; `shell.md` — если `entry = tab`, обновить корневую
   навигацию (`feature/shell`).
5. `make check`; при iOS — `make check-mac`.

## Anti-patterns

- ❌ `hiltViewModel()` внутри `<Key>Screen` (не previewable).
- ❌ Состояние из превью-моков, а не из `State` презентера.
- ❌ Экран без Roborazzi на `error`/`empty`.
- ❌ Литералы строк/цветов; `Color(0xFF…)` — detekt.
- ❌ iOS-View с логикой (форматирование дат, ветвления бизнес-правил) — это презентер.
