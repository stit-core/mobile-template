# scaffold › system-action — действие вне экранов приложения

Читает строку `system-actions.md` карты (`slug · feature · channels (voice|automation|widget:<slug>|
notification:<slug>) · phrases · parameters · mode (headless|opens-app) · target · response ·
требует сессии · на заблокированном экране · platforms · situation`). Фича-источник существует —
иначе сначала `/scaffold feature`; адресаты каналов (`widgets.md`, push-запись `entry-points.md`)
тоже существуют.

## Вопросы до начала (≤ 3)

1. `channels` и `mode`: `headless` возможен только там, где действие объявлено системе
   (реестр AppFunctions на Android — платформенный API 36, интент на iOS, кнопка
   виджета/уведомления); остальное — `opens-app` с `target`.
2. Мутация идемпотентна — какой исход у повтора (отдельная ветка результата, а не ошибка)? Что
   отвечаем офлайн (очереди у записи нет — честный `Failed`)?
3. `требует сессии` + «на заблокированном экране»: без сессии `<Slug>Result.SignedOut` и текст
   «войдите»; канал `widget:<slug>` / `notification:<slug>` — зеркало `widgets.actions` и
   категория в `push.md` уже на месте?

## Раскладка

```
shared/…/feature/<group>/<Slug>Action.kt              # class <Slug>Action internal constructor(deps) { suspend fun run(...): <Slug>Result } — публичный класс, internal-конструктор; оркестрация фичи, без StateFlow и UI; run не бросает
                                                      # <Slug>Result — sealed: <успех>(title) | <идемпотентный исход> | SignedOut | Failed(error: MessageKey); офлайн → Failed(MessageKey.NetworkError) без запроса; после мутации — widget.publish()
shared/…/SharedContainer.kt                            # + фабрика <slug>Action()
shared/src/commonTest/…/feature/<Group>ActionsTest.kt  # успех · повтор (идемпотентность) · SignedOut · офлайн · ошибка API
androidApp/core/platform/…/action/<Slug>ActionHandler.kt        # @Singleton: контейнер → <slug>Action().run(...) → видимая реакция (строка ответа, снятие уведомления, WidgetRefresher.refresh(slug))
androidApp/core/platform/…/action/NotificationActionReceiver.kt # кнопка уведомления: goAsync() → handler; адресат берётся из deep link записи; кнопку рисует push/PushNotifier.kt
androidApp/widget/…/<Slug>WidgetAction.kt                       # кнопка строки виджета: ActionCallback + ActionParameters.Key → handler
androidApp/core/platform/…/action/AppFunctions.kt               # реестр voice/automation: @AppFunction-функции BaseStitAppFunctionService (KDoc — метаданные, isDescribedByKDoc); KSP генерирует StitAppFunctionService и assets/stit_app_functions.xml
androidApp/core/platform/src/main/AndroidManifest.xml           # <receiver> кнопки + <service> AppFunctions: exported, BIND_APP_FUNCTION_SERVICE, tools:targetApi="36"
androidApp/core/platform/src/main/res/values{,-ru}/strings.xml  # core_platform_action_<slug>_{<успех>,signed_out,failed} — оба файла (i18n-check)
iosApp/Packages/Core/Sources/SystemActions/<Slug>Intent.swift # perform() → <slug>Action(); openAppWhenRun при opens-app; authenticationPolicy из «на заблокированном экране»
iosApp/App/AppShortcutsProvider.swift                    # только канал voice: phrases → AppShortcut (провайдер один на приложение, живёт в app-таргете)
iosApp/Packages/Core/Sources/Platform/PushPayload.swift  # + UNNotificationAction в NotificationCategories для notification:<slug>
docs/stack/push.md                                       # + кнопки категории; compliance.md — что доступно без разблокировки
```

## Шаги

1. Строка в `push.md` (кнопки) и `compliance.md` (данные без разблокировки).
2. RED: `<Group>ActionsTest` — успех, повтор, `SignedOut`, офлайн, ошибка; затем `<Slug>Action` +
   фабрика; `make api-dump` (фасад вырос).
3. Android: обработчик канала + обёртка (`NotificationActionReceiver` / `ActionCallback` /
   функция реестра AppFunctions) + строки в оба `values`; Robolectric-тест обработчика (сам
   сервис AppFunctions на JVM не тестируется — Robolectric пинит `sdk=35`, поэтому в нём только
   делегирование).
4. iOS (при `platforms ∋ ios`): `<Slug>Intent` + `AppShortcutsProvider` при `voice` — атом
   «Mac-verify».
5. `make check`; зеркало `widgets.actions` в карте разошлось → STRATEGY-HANDOFF.

## Anti-patterns

- ❌ Бизнес-правило в обёртке канала вместо `<Slug>Action` — обёртка только зовёт shared.
- ❌ Исключение наружу: `run` возвращает `Failed(error)`, канал переводит его в строку.
- ❌ Повтор как ошибка: закрытое повторным нажатием не открывается заново — отдельная ветка исхода.
- ❌ Действие, возвращающее сессию или токен наружу; `headless` с чувствительными данными на
  заблокированном экране без решения владельца.
- ❌ Фраза или `response` литералом в коде — только ресурс (`i18n-check` ловит односторонний ключ).
- ❌ Действие без `response`: пользователь не узнал, что произошло (у `headless`-канала без
  собственного ответа реакция — уведомление).
- ❌ `opens-app` без `Route.<Key>`; дубль логики фичи «потому что здесь без UI».
- ❌ Логика внутри сервиса AppFunctions: он поднимается только на API 36 и на JVM не тестируется.
