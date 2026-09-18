# scaffold › widget — виджет системы

Читает строку `widgets.md` карты (`slug · kind (home|lock|live|tile) · shows · states ⊇
{placeholder, populated} · tap · actions · freshness · refresh (on-app-use|background:<slug>|
event:<ctx>.<Event>) · platforms · situation-ref`). Сущности из `shows` и точка входа `tap`
(`entry-points` с `kind: widget`, её `pattern` — slug виджета) существуют.

## Вопросы до начала (≤ 3)

1. `kind` и что видно без разблокировки: `lock`/`live` читаются на заблокированном устройстве —
   какие поля скрываем?
2. `shows` — все поля есть в `api/openapi.yaml`? `freshness` → какой порог делает снимок `stale`
   (считается по возрасту снимка и только когда нет сети)?
3. `refresh`: `on-app-use` (коллектор репозитория в `WidgetUpdates`), `background:<slug>` (задача
   существует — иначе сначала `/scaffold background-task`) или `event:<ctx>.<Event>`;
   `platforms ∋ ios` — расширение читает JSON из App Group без сети и токена, поэтому
   `signed-out` и `stale` обязаны рисоваться.

## Раскладка

Ядро (`core/widget`, модуль `:androidApp:widget`, порт `WidgetRefresher`) уже на месте — новый
виджет добавляет снимок, провайдер и свою Glance-поверхность.

```
shared/…/core/widget/WidgetState.kt                    # Placeholder | Populated | Empty | SignedOut | Stale — словарь states карты
shared/…/feature/<group>/<Slug>WidgetSnapshot.kt       # @Serializable data class: state, items (поля из shows), generatedAt, tapUrl (тот же https-адрес, что и deep link)
shared/…/feature/<group>/<Slug>WidgetProvider.kt       # class <Slug>WidgetProvider internal constructor(repo, session, connectivity, cache, deepLinkHost, clock) { suspend fun snapshot(); suspend fun publish() }; companion { SLUG, порог свежести, сортировка }
shared/…/SharedContainer.kt                            # + фабрика <slug>WidgetProvider() с cache("widget-<slug>", <Slug>WidgetSnapshot.serializer())
shared/src/commonTest/…/feature/<Group>WidgetsTest.kt  # строка на каждое состояние из states: placeholder · populated · empty · signed-out · stale
androidApp/widget/src/main/kotlin/<pkg>/android/widget/<Slug>Widget.kt          # GlanceAppWidget: switch по WidgetState, токены WidgetTokens, тап — actionStartActivity(ACTION_VIEW, snapshot.tapUrl) c setPackage
androidApp/widget/src/main/kotlin/<pkg>/android/widget/<Slug>WidgetReceiver.kt  # GlanceAppWidgetReceiver
androidApp/widget/src/main/kotlin/<pkg>/android/widget/GlanceWidgetRefresher.kt # + ветка slug: publish() снимка и updateAll (порт core:platform/widget/WidgetRefresher)
androidApp/widget/src/main/res/xml/widget_<slug>_info.xml                       # appwidget-provider (превью, размеры, resizeMode); имя с префиксом модуля — Lint ResourceName
androidApp/widget/src/main/AndroidManifest.xml                                  # <receiver android:exported="false"> + APPWIDGET_UPDATE + meta-data provider
androidApp/widget/src/main/res/values{,-ru}/strings.xml                         # widget_<slug>_* — оба файла (i18n-check)
androidApp/widget/src/test/kotlin/…/<Slug>WidgetTest.kt, <Slug>WidgetScreenshotTest.kt  # состояния рендерятся; эталоны — compose() → RemoteViews.apply() → Bitmap → captureRoboImage (@GraphicsMode(NATIVE), UTC)
iosApp/Packages/Core/Sources/Widgets/{<Slug>Entry,<Slug>SnapshotStore}.swift  # TimelineEntry и чтение снимка из контейнера App Group (Codable-зеркало <Slug>WidgetSnapshot, только Foundation); сети и Shared нет
iosApp/Packages/Features/Sources/WidgetsFeature/<Slug>WidgetView.swift  # SwiftUI, switch по состоянию, @Environment(\.theme); таргет вне featureNames, без Routing/SharedBridge
iosApp/Widgets/{StitWidgets.swift,Info.plist,Widgets.entitlements}       # appex: @main + App Group; сам таргет заводится на Mac (.pbxproj)
```

## Шаги

1. RED: `<Group>WidgetsTest` — по строке на каждое состояние из `states` карты; поля `shows`
   берутся из контракта (нет поля в `api/openapi.yaml` — BACKEND-HANDOFF, не выдумка).
2. Снимок + провайдер + фабрика; `make api-dump` (снимок — часть фасада).
3. Android: Glance-виджет и receiver в `:androidApp:widget`, `appwidget-provider`, ветка в
   `GlanceWidgetRefresher`, строки в оба `values`; `make screenshots-record` осознанно — эталоны
   на каждое состояние в коммит.
4. `tap` → запись `entry-points` `kind: widget` (`/scaffold entry-point`); кнопки → `/scaffold
   system-action` с каналом `widget:<slug>`; `refresh: background:<slug>` → `/scaffold
   background-task` (её `RefreshWidget` дергает тот же порт).
5. iOS (при `platforms ∋ ios`): `<Slug>Entry` + `<Slug>WidgetView`, таргет и App Group — атом
   «Mac-verify». `make check`.

## Anti-patterns

- ❌ Сеть, репозиторий или токен в `GlanceAppWidget`/`TimelineProvider` — только снимок из `shared`.
- ❌ ПДн и чувствительные поля в снимке App Group и на `lock`/`live`.
- ❌ Виджет без эталонов `placeholder` и `populated`; состояние из превью-мока, а не из снимка.
- ❌ Интервал обновления в коде вместо политики `refresh`; обещание свежести, которого система
  не даёт; `stale` по возрасту снимка при живой сети.
- ❌ Кнопка виджета без записи `system-actions` с каналом `widget:<slug>`; тап мимо `tapUrl`
  (свой `Intent` вместо ссылки, которую разбирает `DeepLinkParser`).
- ❌ Поле в `shows`, которого нет в контракте бэкенда; имя ресурса без префикса модуля.
- ❌ Виджет как уменьшенная копия экрана; тема приложения вместо `WidgetTokens`.
