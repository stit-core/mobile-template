# scaffold › feature — презентер в shared

Читает строку `features.md` карты (`slug · name · действие · screens · entities · api-ops ·
shared · platforms · status`). `shared: true` (дефолт) — оркестрация в KMP; `false` — только
платформенные заготовки (обоснование — в описании записи).

## Вопросы до начала (≤ 3)

1. Какие `entities` и `api-ops` задействованы (должны существовать — иначе сначала `entity`/`api`)?
2. Состояния экрана из `screens.md` (`loading, empty, populated, error, offline, …`)?
3. Есть ли логика, заслуживающая use-case (валидация, оркестрация двух репозиториев)?

## Раскладка (`shared: true`)

```
shared/src/commonMain/kotlin/<pkg>/feature/<group>/          # группа экранов: auth, todos, profile, settings, push, …
├── <Slug>Presenter.kt   # class <Slug>Presenter internal constructor(deps, dispatchers) { val state: StateFlow<<Slug>State>; fun dispatch(intent: <Slug>Intent); fun close() }
├── <Slug>State.kt       # data class: данные, loading, error: MessageKey?, offline + производные val (canSubmit, …)
├── <Slug>Intent.kt      # sealed class: data object Start/Refresh, data class <Action>(id)
└── <Slug>UseCase.kt     # только при реальной логике (в reference-фичах не нужен)
shared/src/commonTest/kotlin/<pkg>/feature/
└── <Slug>PresenterTest.kt   # сценарии на testContainer(scenario): populated → данные, empty → пустой список, error → error, offline → offline
```

`SharedContainer`: `fun <slug>Presenter(): <Slug>Presenter = <Slug>Presenter(...)` (фабрика, не singleton;
`presenterScope(dispatchers)` закрывает платформа через `close()`).

## Шаги

1. RED: `<Slug>PresenterTest` — каждое состояние из карты + ошибка + offline (Turbine,
   `runTest`, `StandardTestDispatcher`, `FakeClock`).
2. `State` (`data class`) / `Intent` (`sealed class`) — плоские, rule `kmp-public-api`; презентер
   поверх репозиториев, ошибки — через `attempt {}` → `toMessageKey()`.
3. Идемпотентность мутаций (`Idempotency-Key` в `ApiClient` — по умолчанию); оптимистичное
   обновление — только если запись карты это требует.
4. Фабрика в `SharedContainer`; `make api-dump`; `make check-fast`.
5. Экраны — `/scaffold screen <Key> --feature <slug>` по каждому `screens` записи.

## `shared: false`

Платформенная возможность (например, системный share-sheet): только `androidApp/feature/<slug>`
и `Features/<Slug>Feature` без презентера; в `shared` — максимум интерфейс в `PlatformDeps`.

## Anti-patterns

- ❌ Презентер с generic-параметрами или callback'ами — фасад плоский.
- ❌ `feature/<a>` импортирует `feature/<b>` — общее в `entity`/`core`.
- ❌ One-off события наружу (`SharedFlow<Event>`) — всё в `State`.
- ❌ Состояния «на всякий случай», которых нет в карте.
