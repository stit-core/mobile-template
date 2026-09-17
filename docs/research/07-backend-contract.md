# 07 — Контракт с бэкендом и экосистемой: auth, OpenAPI-кодоген, mock, среды, deep links, push, observability, i18n/токены, слой-мост в strategy

> Поток `07-backend-contract` ресёрча мобильного шаблона экосистемы stit. Дата среза фактов —
> **2026-09-17**. Автор — research-агент. Все версии и статусы проверены в этот день через
> первоисточники (чтение репозиториев `stit-core/*`, GitHub API/raw, Maven Central, Google Maven,
> npm, официальные доки); у каждого факта пометка:
> **[✓ URL, дата]** — проверено по первоисточнику; **[изм]** — измерено/выполнено на этой машине
> (Debian 13, Go 1.26.5, JDK 21); **[~]** — вторичный источник / отраслевой консенсус;
> **[?]** — предположение/инженерный вывод автора.
>
> Соседние отчёты, на которые ссылаюсь и которые **не дублирую**: `02-ios-canon.md` (Keychain §6.5,
> AASA §6.6, APNs §6.7, privacy manifests §6.3, xcconfig §3.4, токены §3.5, String Catalogs §6.16),
> `05-distribution-ru.md` (push-провайдеры и РФ §2, наблюдаемость §4, STOP-граница §7.4,
> env-контракт релиза §8.3, cross-repo §9), `01-android-canon.md` (модульность §3, токены §3.4,
> i18n §3.5), `03-shared-strategy.md` (рекомендация KMP `shared/` + нативный UI, форма репо §6.3).
> Отчёт `06-tooling-agent.md` на момент написания ещё не существовал.

---

## TL;DR

1. **Auth: бэкенд сегодня — cookie-only и заточен под браузер** (`tokenFromRequest` читает только cookie `access_token`; cookies — session без `Max-Age`, `SameSite=Strict`; refresh только из cookie; CSRF нет) [✓ код]. Для нативного клиента это означает самописный шифрованный cookie-jar на каждой платформе и **сломанный кодоген** (параметры `in: cookie`). **Рекомендация — Bearer + refresh в JSON-теле** (`Authorization: Bearer`, `POST /api/auth/refresh {refresh_token}`, заголовок `X-Client: android|ios`, сессии `/api/auth/sessions`), cookie-режим для web остаётся; BACKEND-HANDOFF B-1…B-11 — ~150 строк Go в `authgate/jwt.go`, `authgate/module.go`, `auth/delivery/http/*` + тесты (§1.3b).
2. **Хранение токенов**: `androidx.security:security-crypto` (EncryptedSharedPreferences/MasterKey) **целиком deprecated с 1.1.0** «in favour of existing platform APIs and direct use of Android Keystore» [✓ Android releases] → своя `SecureStore` (Keystore AES-GCM + DataStore 1.2.1, `setUserAuthenticationRequired` для биометрии); iOS — Keychain `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` + `kSecAttrSynchronizable=false` [✓ Apple]. Access — только в памяти (15m), refresh — 90 дней в SecureStore с ротацией; reuse → отзыв семейства сессий (B-4).
3. **OIDC/Authentik и passkeys — extension'ы, не дефолт**: AppAuth-Android последний релиз 2021-12 (0.11.1), AppAuth-iOS 3.0.0 (2026-08-24, iOS 15+); Authentik 2026.8.2 умеет public client + PKCE, refresh только с `offline_access`. Для SSO предложена схема **BFF-mobile** поверх `oidckit` (one-time code → наша bearer-пара), чтобы формат токенов и таблица сессий были едиными (§1.3c).
4. **Спек OpenAPI 3.1 добывается без запуска БД** — доказано на этой машине: 30-строчный `cmd/openapidump` собирает `huma.API` через `httpkit.NewServer` + `Register(api, NewHandler(nil))` и пишет `api.OpenAPI().YAML()`; `openapi-generator validate` — «No validation issues». Cross-repo B-12: `make openapi` + закоммиченный `api/openapi.yaml` + `gen-check` (как sqlc/mockery); мобильный репо читает копию соседа `../backend/api/openapi.yaml` и держит `api/openapi.lock`.
5. **Что в спеке мешает клиентам** [изм]: служебное поле `$schema` в каждой body-схеме (huma `SchemaLinkTransformer`, guard'а на пустой `SchemasPath` нет → убирать через `CreateHooks=nil`, B-13); `data` списков — `[array, null]`; `LoginOutputBody.user` вместо `data`; в `ErrorModel` **нет поля `code`**, хотя ответы его содержат (B-15); `refresh`/`logout` описаны `in: cookie` параметром типа «вся `http.Cookie` Go».
6. **Кодоген — эмпирика** [изм]: `openapi-generator 7.25.0 kotlin --library multiplatform` на реальном спеке: «OpenAPI 3.1 support is still in beta», security `Cookie(apiKey in cookie)` превращён в **`ApiKeyAuth("query","access_token")`** (токен уехал бы в URL), `refreshToken(refreshToken: Cookie?)`, `$schema→dollarSchema`, `size→propertySize`. Swift OpenAPI Generator 1.13.1: 3.0/3.1 ✅, `securitySchemes` и cookie-параметры **не поддерживаются** (auth — только `ClientMiddleware`). Вывод: **bearer + body делают спек генерируемым на обеих платформах**; рекомендация — iOS Swift OpenAPI Generator (плагин), KMP/Android — генерация **моделей** openapi-generator + свой Ktor-клиент/Ktorfit 2.7.5.
7. **Конвенции для долгоживущих клиентов** (§2.6): envelope `data`/`data+total`; `APIError.code` — стабильный контракт; `X-Request-ID` (UUID, ≤128 ASCII) на каждый запрос → breadcrumb Sentry ↔ логи бэкенда; `Idempotency-Key` на каждую мутацию (httpkit: 24h TTL, `422 IDEMPOTENCY_KEY_REUSED`, `409 IN_PROGRESS`, `Idempotent-Replayed`), **но store в шаблоне бэкенда не провайдится — ключ молча игнорируется (B-9)**; ретраи по `Retry-After`/429/503, POST только с ключом; аддитивная эволюция без `/v1` + `ignoreUnknownKeys` + `oasdiff breaking` в гейте + kill-switch `GET /api/app/requirements` (B-17).
8. **Побочная находка**: SPA-шаблон и бэкенд-шаблон **несовместимы по контракту** (`/api/v1/*`, cookie `app_session`, `X-CSRF-Token` vs `/api/auth/*`, `access_token`/`refresh_token`, без CSRF) — мобильный шаблон обязан ориентироваться на `openapi.yaml` бэкенда, а не на MSW-мок SPA; фикс SPA — F-1.
9. **Mock-режим**: двухслойный — JSON-фикстуры per сценарий (`empty|populated|authenticated|error`, формат = ответы API) + `MockTransport` (Ktor `MockEngine` 3.6.0 в shared / swift-openapi `ClientTransport` на iOS), `demo`-flavor/схема как в Now in Android (`applicationIdSuffix ".demo"`, `src/demo/.../FlavoredNetworkModule.kt`), debug-панель, **те же фейки в JVM/Swift-тестах** (§3).
10. **Среды**: «один образ на все среды» невозможен → 3 flavor/схемы + `demo`; конфиг делится на build-time / release-time (секреты только в env `make release`, Vaultwarden) / runtime (`GET /api/app/requirements`: min-version, kill-switch, флаги на `flagkit`, ссылки); полный env-контракт — таблица §4.2 (`API_BASE_URL`, `APP_ENV`, `DEEP_LINK_HOST`, `SENTRY_DSN`, `PUSH_PROVIDERS`, `AUTH_MODE`, …). Внешние feature-flag-платформы не закладывать (Unleash server — **AGPL-3.0**).
11. **Deep links**: `.well-known` отдаёт новый always-on сервис `wellknown` (nginx) в `<org>/infra` с роутером `PathPrefix(/.well-known/) && priority 1000`, файлы — `.tpl` через существующий `render-configs.sh`, env `MOBILE_ANDROID_SHA256_{UPLOAD,PLAY,RUSTORE}`, `MOBILE_IOS_TEAM_ID/BUNDLE_ID` per VPS; AASA — на каждом конкретном сабдомене, без редиректов, любому User-Agent (TN3155); проверка `swcutil`, `pm verify-app-links --re-verify`, `am start -W … -d`; черновик compose/nginx/tpl + INFRA-HANDOFF — §5.2.
12. **Push**: `PUT /api/v1/devices {provider, token, platform, app_version, locale, installation_id, sandbox, push_enabled}` (upsert по `(provider,token)`), `DELETE` при logout, отзыв по `UNREGISTERED`/`410`/`NOT_FOUND`; payload **data-first**: ключи локализации + args + `deep_link` + `notification_id` + `category`; APNs — `loc-key`/`loc-args` (клиентская локализация встроена), RuStore — без `notification.title` не покажет; лимит 4096 байт везде; `golibinfra/pushkit` (порт `Sender`, адаптеры FCM v1/APNs/RuStore) + outbox (§6).
13. **Observability**: `sentry-android` 8.57.0 (minSdk 21) / `sentry-cocoa` 9.28.0 (iOS 15+) с пустым DSN = выключено; release `package@version+build`; mapping через Gradle-плагин 6.22.0 на Linux, dSYM — только с Mac (`sentry-cli`, лицензия FSL-1.1); GlitchTip символицирует хуже Sentry; логи — Kermit 2.2.0 / `os.Logger` с запретом `Log`/`print` (detekt/SwiftLint); OTel Android 1.7.0 (agent stable, core alpha) и Swift 2.5.1 (traces stable, OTLP/HTTP experimental) — **шов, не дефолт** (коллектора в экосистеме нет).
14. **i18n/токены**: строки — нативные ресурсы с единым реестром ключей; ошибки и пуши — по ключу; токены — **независимые с DTCG-именами и проверкой паритета имён с `index.css` SPA**, Style Dictionary 5.5.3 (DTCG с v4, `compose/object`, `ios-swift/*`) — позже в `ui-lib`.
15. **Strategy-мост**: FSD как есть для мобильных **не годится** (нет `pages` как слоя, `flow` = viewport веба, `block` на `@stit/ui`, нет permissions/deep-link/push/offline); предложен слой **`mobile/<surface>/`** (9 kind: entities · screens · flows · features · shell · entry-points · permissions · shared-api · shared-lib), встающий на место `screens/` для `type: mobile` (как bot-flow), inside-out порядок, инварианты, что генерирует `/scaffold` в `shared/`, `androidApp/`, `iosApp/`; канон-PR в strategy (~1200 строк md + `/mobile-deep-dive`, `/mobile-audit`) и путь без PR — копия канона в мобильном репо + graceful next-step (§9).

---

## Findings

### §1. Auth-контракт мобильного приложения с Go-бэкендом

#### 1.1. Что есть сегодня (прочитано в коде, 2026-09-17)

| факт | где | статус |
|---|---|---|
| Единственный экстрактор credentials — cookie `access_token`; заголовок `Authorization` не читается вообще | `go-backend-template/internal/platform/authgate/jwt.go`: `tokenFromRequest` → `r.Cookie(CookieAccess)`, иначе `"authgate: no auth cookie"` | [✓ код] |
| Cookie-политика: `Path=/`, `HttpOnly`, `SameSite=Strict`, `Secure` = env `COOKIE_SECURE` (default `false`), **без `Domain`, без `Max-Age`/`Expires`** (session-cookie; тест `TestBuildAuthCookies` это фиксирует: «auth cookies must be session cookies without Max-Age») | `authgate/cookie.go`, `cookie_test.go` | [✓ код] |
| Refresh читается **только из cookie** `refresh_token` (huma-тег `cookie:"refresh_token"`), ответ — `204` + два `Set-Cookie` | `internal/domain/auth/delivery/http/{dto,handler}.go` | [✓ код] |
| Ротация refresh: `jti` в таблице `refresh_sessions`, `MarkUsed(old, new)` + `Insert(new)` в одной транзакции; повторное предъявление использованного refresh → `AUTH_REFRESH_REUSED` (401) + audit `refresh_reused`; истёкший → `AUTH_REFRESH_EXPIRED` (401); logout идемпотентен (битый refresh → всё равно `204` и очистка cookie) | `internal/domain/auth/usecase.go`, `handler.go` | [✓ код] |
| Reuse-detect **не отзывает всю цепочку** сессий пользователя (только отказывает в ротации) — «семейства» токенов (RFC 6819 §5.2.2.3-style) нет | `usecase.go: Refresh` | [✓ код, вывод [?]] |
| JWT: HS256 (`go-jose/v4`), claims `sub`, `role`, `typ ∈ {access, refresh}`, `iat`, `exp`, опц. `iss`/`aud` (`WithIssuer`/`WithAudience`, в шаблоне **не** заданы); `JWT_SECRET` ≥ 32 байт; `JWT_ACCESS_TTL` default `15m`, `JWT_REFRESH_TTL` default `720h` (30 дней), `JWT_LEEWAY` `60s` | `golibinfra/authnkit/jwt.go`, `authgate/authn.go` | [✓ код] |
| CSRF-защиты на бэкенде **нет** — от CSRF защищает только `SameSite=Strict`; SPA-шаблон при этом шлёт `X-CSRF-Token` из cookie `csrf_token`, которой бэкенд не выставляет, и ходит на пути `/api/v1/*` и cookie `app_session`, которых у бэкенда нет (у бэкенда `/api/auth/*`, `/api/users/*`, cookies `access_token`/`refresh_token`) | `spa-frontend-template/src/shared/api/client.ts`, `src/shared/api/mock/handlers/*.ts` vs backend | [✓ код; расхождение — см. §10 и Cross-repo] |
| Brute-force: `bruteforcekit` (Postgres-счётчики), lock → `429 AUTH_ACCOUNT_LOCKED` **без `Retry-After`** (в отличие от rate-limit httpkit, который ставит `Retry-After: 1`) | `auth/delivery/http/handler.go: mapErr`, `httpkit/ratelimit.go` | [✓ код] |
| OpenAPI security scheme — `Cookie` (`apiKey`, `in: cookie`, `name: access_token`); защищённые операции помечены `security: [{Cookie: []}]`; публичные — без `Security` (в httpkit публичность = пустой/отсутствующий `Security`) | `authgate/module.go`, дамп спека §2.2 | [✓ код + изм] |
| Idempotency middleware в шаблоне бэкенда **не включён**: `httpkit.Module` навешивает `Idempotency(...)` только если в графе есть `httpkit.IdempotencyStore` (`optional:"true"`), а `cmd/api/main.go` его не провайдит (`cachekit.NewIdempotencyStore` есть в библиотеке) | `golibinfra/httpkit/module.go`, `go-backend-template/cmd/api/main.go` | [✓ код] |
| OIDC BFF (`oidckit`) — cookie-сессия `__Host-access_token` с `typ=oidc_session`, PKCE S256, Authentik в корп control-plane (`auth.${DOMAIN}`, blueprints для Gitea/Vaultwarden); в go-backend-template `oidckit` **не подключён** (только `OIDC_*` env прокинуты в compose `backend.yml`) | `golibinfra/oidckit/README.md`, `corp-infra-template/compose/authentik.yml`, `project-infra-template-compose/compose/backend.yml` | [✓ код] |

Вывод: контракт спроектирован **под браузер** (httpOnly + SameSite=Strict + session-cookie). Для нативного
клиента httpOnly/SameSite ничего не защищают (нет чужого JS и нет cross-site-контекста), а вот
«session-cookie без Max-Age» — прямой источник проблем персистентности (см. 1.3a).

#### 1.2. Требования OWASP MASVS v2.1.0 (проверено по GitHub `OWASP/masvs`, 2026-09-17)

Точные тексты контролов [✓ raw.githubusercontent.com/OWASP/masvs/master/controls/*.md]:

| контроль | текст | что это значит для нас |
|---|---|---|
| MASVS-STORAGE-1 | «The app securely stores sensitive data.» | токены/refresh только в Keystore-защищённом хранилище (Android) / Keychain (iOS); не в `SharedPreferences`/`UserDefaults`/файлах открытым текстом |
| MASVS-STORAGE-2 | «The app prevents leakage of sensitive data.» | токены не в логах, не в бэкапах (`android:allowBackup`/`dataExtractionRules`, Keychain `ThisDeviceOnly`), не в crash-репортах, не в screenshot'ах |
| MASVS-AUTH-1 | «The app uses secure authentication and authorization protocols and follows the relevant best practices.» | для OAuth — RFC 8252 (внешний user-agent, PKCE); для своего логина — короткий access, ротация refresh, logout = инвалидация на сервере |
| MASVS-AUTH-2 | «The app performs local authentication securely according to the platform best practices.» | биометрия — только как **разблокировка ключа** (Keystore `setUserAuthenticationRequired` / Keychain `SecAccessControl`), не как «флаг в памяти» |
| MASVS-AUTH-3 | «The app secures sensitive operations with additional authentication.» | смена пароля/платёж → step-up (повторный ввод пароля/биометрия) |
| MASVS-NETWORK-1 | «The app secures all network traffic according to the current best practices.» | TLS 1.2+, без cleartext (Android `usesCleartextTraffic=false` в release, iOS ATS без исключений), dev-эмулятор — отдельный `network_security_config` только для debug |
| MASVS-NETWORK-2 | «The app performs identity pinning for all remote endpoints under the developer's control.» | pinning — контроль **L2/уровня «R»** [~]; для нас — осознанное решение (см. 1.5) |
| MASVS-RESILIENCE-1..4 | platform integrity / anti-tampering / anti-static / anti-dynamic | MASVS прямо пишет: «The absence of these measures does not in itself constitute a vulnerability» и предупреждает о platform lock-in при опоре на Play Integrity / App Attest [✓ `Document/11-MASVS-RESILIENCE.md`] |
| MASVS-PRIVACY-1/2 | минимизация доступа к данным; предотвращение идентификации | не собирать `AD_ID`, не слать device-fingerprint в аналитику; согласие на SDK до инициализации |

RFC 8252 (OAuth 2.0 for Native Apps) [✓ rfc-editor.org/rfc/rfc8252.txt]: «native apps MUST NOT use
embedded user-agents to perform authorization requests»; авторизация — через системный браузер /
in-app browser tab (Custom Tabs / `ASWebAuthenticationSession`); PKCE обязателен; redirect — private-use
URI scheme, claimed `https` URL (App Links/Universal Links) или loopback.

#### 1.3. Варианты × критерии

| вариант | безопасность (MASVS) | UX (биометрия, «оставаться в системе») | offline / несколько сред | изменения бэкенда | agent-friendliness | вердикт |
|---|---|---|---|---|---|---|
| **(a) cookie-сессия как есть** | ⚠️ cookie-jar хранится в открытом контейнере приложения (`HTTPCookieStorage`, файл OkHttp-jar) → STORAGE-1 требует ручного шифрования jar'а; refresh 30 дней в cookie | ⚠️ «оставаться» = самописная персистентность session-cookie без `Max-Age`; биометрия — поверх самописного jar'а | ✅ по одному jar на среду (домены разные) | 0 (или 1: `Max-Age`) | ⚠️ cookie-параметры ломают оба кодогенератора (§2.4); каждая платформа — свой cookie-jar со своими багами | **только как временный мост** |
| **(b) Bearer + refresh в JSON** | ✅ токены лежат там, где положено (Keystore/Keychain); нет «магии» транспорта; reuse-detect как сейчас | ✅ refresh 30–90 дней + биометрия на разблокировку refresh; «выйти со всех устройств» = список сессий | ✅ один код, N base-URL | **малые**: `tokenFromRequest` + один эндпоинт | ✅✅ стандартный `Authorization: Bearer` — понимают все генераторы, все HTTP-клиенты, все прокси/curl | **рекомендация (дефолт шаблона)** |
| **(c) OIDC/OAuth2 PKCE через Authentik** | ✅ (RFC 8252, AppAuth) | ⚠️ первый вход — через браузер (Custom Tabs/ASWebAuthenticationSession); SSO между приложениями заказчика | ✅ | **большие**: бэкенд как resource-server (verify JWKS Authentik) **или** BFF `oidckit` + мобильный «код за cookie»; Authentik нужен и на проектной стороне (сейчас только в корп control-plane) | ⚠️ ещё один движущийся компонент; но AppAuth — стандарт | **опция для B2B/корп-SSO**, не дефолт |
| **(d) Passkeys / Credential Manager / `ASAuthorization`** | ✅✅ (phishing-resistant) | ✅ вход без пароля; требует WebAuthn RP на бэкенде и AASA `webcredentials` + `assetlinks` `get_login_creds` | ✅ | **большие**: WebAuthn (`go-webauthn/webauthn`) в `authnkit`/новый kit, таблица credential'ов, endpoints register/assert | ⚠️ тесты только на устройстве/симуляторе | **опция «второй фазы»** — только после (b) |

#### 1.3a. Вариант (a) в деталях — что реально происходит с cookie в нативных клиентах

- **OkHttp 5.5.0** [✓ Maven Central, 2026-08-16]: cookie по умолчанию **не хранятся** (`CookieJar.NO_COOKIES`). Нужен свой `CookieJar` или `okhttp-java-net-cookiejar` (`JavaNetCookieJar` поверх `java.net.CookieManager`, артефакт `com.squareup.okhttp3:okhttp-java-net-cookiejar:5.5.0` [✓ Maven Central; changelog OkHttp: «Create a JPMS-compatible artifact for `JavaNetCookieJar`»]). `java.net.CookieManager` — **in-memory**, персист через смерть процесса — только своя реализация `CookieStore`. Значит «cookie как есть» на Android = **самописное персистентное хранилище cookie**, которое по MASVS-STORAGE-1 обязано быть зашифровано Keystore-ключом. Это ровно та же работа, что хранить bearer-токены, только с лишним слоем.
- **Ktor 3.6.0** [✓ Maven Central, 2026-09-16]: плагин `HttpCookies` с `CookiesStorage` (`AcceptAllCookiesStorage` — in-memory, `ConstantCookiesStorage`); **персистентного storage в комплекте нет**, «create a custom cookie storage by implementing the CookiesStorage interface» [✓ ktor.io/docs/client-cookies.html]. Важная деталь Darwin-движка: Ktor **отключает системное хранилище NSURLSession** — `NSURLSessionConfiguration.defaultSessionConfiguration().apply { setHTTPCookieStorage(null) }` [✓ raw github `ktor-client-darwin/.../internal/DarwinSession.kt`], т. е. в KMP-shared cookies живут только в Ktor-storage — единая точка, что хорошо, но персист опять свой.
- **URLSession / `HTTPCookieStorage.shared`** [✓ developer.apple.com/documentation/foundation/httpcookiestorage]: persistent-store per app container (не Keychain), session-cookie (`isSessionOnly`) «are local to a single process and are not shared» — наши cookie без `Max-Age` **сессионные** → пропадают при перезапуске процесса. Чтобы «оставаться в системе», iOS-клиенту пришлось бы либо переписывать cookie с `Expires`, либо вынимать `refresh_token` из `Set-Cookie` и класть в Keychain руками (что и предлагает `02-ios-canon.md` §6.5) — т. е. это уже bearer-схема, только в обёртке cookie.
- **`Secure` в dev**: при `COOKIE_SECURE=false` (dev по http на `10.0.2.2`/`localhost`) cookie ходят; при `true` через `http://` клиенты их отбрасывают — по сути ещё одна ось конфигурации. У bearer-схемы этого измерения нет.
- **Кодоген**: параметр `in: cookie` (huma `cookie:"refresh_token"`) — не поддерживается ни Swift OpenAPI Generator («form (in cookie)» — не отмечено; `securitySchemes` — не поддерживаются вовсе [✓ `Supported-OpenAPI-features.md`]), ни `openapi-generator kotlin` (Parameter Feature `Cookie` ✗ [✓ openapi-generator.tech/docs/generators/kotlin]); эмпирика §2.4: сгенерированный `refreshToken(refreshToken: Cookie?)` бесполезен, а security `Cookie` превращается в `ApiKeyAuth("query", "access_token")`.

Итог по (a): работает «из коробки» только для скриншот-демо; для production-приложения требует
самописного шифрованного cookie-persist на каждой платформе и ломает кодоген. Не рекомендуется как
целевой контракт; допустим как **режим совместимости** на переходный период (см. рекомендацию).

#### 1.3b. Вариант (b) — Bearer + refresh в JSON: точный контракт

**Транспорт.** Заголовок `Authorization: Bearer <access JWT>` на защищённых операциях; `access_token`
— тот же HS256-JWT `authnkit` (`typ=access`), TTL `JWT_ACCESS_TTL` (для мобильных разумно
**15–30 мин**, оставить 15m); `refresh_token` — тот же `typ=refresh`-JWT с `jti`, TTL для мобильного
клиента **длиннее** (предложение: `JWT_REFRESH_TTL_MOBILE=2160h` = 90 дней; текущие 30 дней для
браузера), ротация при каждом refresh как сейчас, reuse-detect как сейчас, **плюс** отзыв всей цепочки
при reuse (см. handoff B-4).

**Эндпоинты (черновик OpenAPI 3.1, huma-стиль; `/api/auth/*` — путь как в бэкенде сегодня).**

```yaml
# ЧЕРНОВИК · дополнение к спеку go-backend-template (мобильная ветка auth)
paths:
  /api/auth/login:
    post:
      operationId: loginUser
      # существующий: тело {email, password}; ответ 200 {user} + Set-Cookie.
      # Новое: заголовок X-Client решает форму ответа.
      parameters:
        - in: header
          name: X-Client
          schema: { type: string, enum: [web, android, ios] }
          description: "web (default) → cookies; android|ios → tokens в теле, cookies не ставятся"
      responses:
        "200":
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/LoginOutputBody"   # + поле tokens
  /api/auth/refresh:
    post:
      operationId: refreshToken
      requestBody:            # НОВОЕ: refresh в теле для не-браузерных клиентов
        required: false
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/RefreshInputBody"
      responses:
        "200":                # для X-Client=android|ios (сегодня 204 + Set-Cookie для web)
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/TokenPairBody"
  /api/auth/logout:
    post:
      operationId: logoutUser
      requestBody:
        required: false
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/RefreshInputBody"
      responses: { "204": { description: No Content } }
  /api/auth/sessions:
    get:
      operationId: listSessions
      security: [{ Bearer: [] }]
      responses:
        "200":
          content:
            application/json:
              schema: { $ref: "#/components/schemas/SessionListOutputBody" }   # data[] + total
    delete:
      operationId: revokeAllSessions      # «выйти со всех устройств»
      security: [{ Bearer: [] }]
      responses: { "204": { description: No Content } }
  /api/auth/sessions/{jti}:
    delete:
      operationId: revokeSession
      security: [{ Bearer: [] }]
      responses: { "204": { description: No Content } }
components:
  securitySchemes:
    Cookie: { type: apiKey, in: cookie, name: access_token }     # как есть (web)
    Bearer: { type: http, scheme: bearer, bearerFormat: JWT }     # НОВОЕ (mobile)
  schemas:
    TokenPairBody:
      type: object
      required: [access_token, refresh_token, token_type, expires_in]
      properties:
        access_token: { type: string }
        refresh_token: { type: string }
        token_type: { type: string, enum: [Bearer] }
        expires_in: { type: integer, format: int64, description: "секунды до exp access_token" }
    RefreshInputBody:
      type: object
      required: [refresh_token]
      properties:
        refresh_token: { type: string }
    LoginOutputBody:
      type: object
      required: [user]
      properties:
        user: { $ref: "#/components/schemas/AuthUserView" }
        tokens: { $ref: "#/components/schemas/TokenPairBody" }   # только при X-Client=android|ios
    SessionView:
      type: object
      required: [jti, issued_at, expires_at, current]
      properties:
        jti: { type: string }
        client: { type: string, enum: [web, android, ios] }
        device_name: { type: string }          # из X-Device-Name (опц., пользовательское имя устройства)
        app_version: { type: string }          # из User-Agent
        issued_at: { type: string, format: date-time }
        expires_at: { type: string, format: date-time }
        last_used_at: { type: string, format: date-time }
        current: { type: boolean }
```

Пример обмена:

```http
POST /api/auth/login HTTP/1.1
Host: api.dev.acme.ru
Content-Type: application/json
X-Client: android
User-Agent: acme-mobile/1.4.2+1004002 (Android 16; Pixel 8; android)
X-Request-ID: 0192f4d3-7b8a-7c2e-9d31-4b8e6f2a1c07

{"email":"me@example.com","password":"password123"}
```

```http
HTTP/1.1 200 OK
Content-Type: application/json
X-Request-ID: 0192f4d3-7b8a-7c2e-9d31-4b8e6f2a1c07
Cache-Control: no-store

{"user":{"id":"u1","email":"me@example.com","name":"Я","role":"client"},
 "tokens":{"access_token":"eyJhbGciOiJIUzI1NiIs…","refresh_token":"eyJhbGciOiJIUzI1NiIs…",
           "token_type":"Bearer","expires_in":900}}
```

```http
POST /api/auth/refresh HTTP/1.1
Content-Type: application/json
X-Client: ios

{"refresh_token":"eyJhbGciOiJIUzI1NiIs…"}
```

Ответ `200 {access_token, refresh_token, token_type, expires_in}`; при `401 AUTH_REFRESH_REUSED` /
`AUTH_REFRESH_EXPIRED` клиент обязан удалить пару из хранилища и показать экран входа (не ретраить).

**Почему `X-Client`, а не отдельные пути `/api/mobile/auth/*`.** Один usecase (`Login`/`Refresh`) и
один OperationID на оба клиента; различается только **delivery** (cookie vs body) — это ровно уровень
`delivery/http` в архитектуре бэкенда (`docs/reference/architecture.md`: «все ветвления — в usecase»
не нарушается, ветвление транспортное). Заголовок `X-Client` также нужен для аудита/сессий и для
`Retry-After`/rate-limit-политик. Альтернатива — `Accept: application/vnd.stit.tokens+json` — сложнее
для генераторов. [?]

**Что менять в бэкенде — BACKEND-HANDOFF (точные файлы).**

| # | файл / kit | изменение | приоритет |
|---|---|---|---|
| B-1 | `internal/platform/authgate/jwt.go` | `tokenFromRequest`: сначала `Authorization: Bearer <t>` (константа `HeaderAuthorization`, регистронезависимый префикс, `strings.CutPrefix`), затем fallback на cookie `access_token`. Тест `jwt_test.go`: кейсы «bearer», «bearer+cookie → bearer wins», «malformed bearer → 401» | P0 |
| B-2 | `internal/platform/authgate/module.go` | вторая `SecuritySchemeEntry{Name:"Bearer", Scheme:&huma.SecurityScheme{Type:"http", Scheme:"bearer", BearerFormat:"JWT"}}`; защищённые операции — `Security: []map[string][]string{{"Cookie":{}}, {"Bearer":{}}}` (OR-семантика OpenAPI). Замена на всех `huma.Operation` в `user`/`auth`/`verification` | P0 |
| B-3 | `internal/domain/auth/delivery/http/{dto,handler}.go` | `LoginInput.XClient string \`header:"X-Client"\``; `RefreshInput`: `RefreshCookie http.Cookie \`cookie:"refresh_token"\`` **плюс** `Body *struct{RefreshToken string \`json:"refresh_token"\`}` (huma допускает необязательное тело); выбор источника: тело → cookie; ответ для `X-Client≠web` — `200 TokenPairBody` без `Set-Cookie`; для logout — то же | P0 |
| B-4 | `internal/domain/auth/usecase.go`, `infra/postgres/queries/refresh_sessions.sql` | reuse-detect → `RevokeChain(userID, family)`: колонка `family_id` (uuid первой сессии) в `refresh_sessions`, при `UsedAt != nil` — `UPDATE … SET revoked_at=now() WHERE family_id=$1`; sentinel `ErrRefreshReused` остаётся | P1 |
| B-5 | `internal/domain/auth/*` | сущность `RefreshSession` расширить `Client`, `DeviceName`, `AppVersion`, `LastUsedAt` (+ миграция); usecases `ListSessions`, `RevokeSession`, `RevokeAll`; HTTP `GET/DELETE /api/auth/sessions[/ {jti}]` (см. черновик выше) | P1 |
| B-6 | `golibinfra/authnkit/jwt.go` или `authgate` | отдельный TTL refresh для мобильных: либо `JWTConfig.RefreshTTLMobile time.Duration \`envconfig:"JWT_REFRESH_TTL_MOBILE" default:"2160h"\`` в authnkit (breaking-minor по их правилам), либо `IssueRefreshWithTTL` через `Sign(ctx, claims, ttl)` (уже есть) в адаптере `auth/module/adapters.go` — **предпочтительно второе, без правки kit'а** | P1 |
| B-7 | `authgate/authn.go` | задать `authnkit.WithIssuer("<org>-backend")` и `WithAudience("api")` — иначе токен любого сервиса на общем `JWT_SECRET` проходит (kit это прямо предупреждает) | P1 |
| B-8 | `auth/delivery/http/handler.go` | `Retry-After` на `AUTH_ACCOUNT_LOCKED` (429) из `LockState.Until` — мобильный клиент показывает таймер | P2 |
| B-9 | `cmd/api/main.go` | провайдить `httpkit.IdempotencyStore` (`cachekit.NewIdempotencyStore`) — иначе `Idempotency-Key` от мобильного клиента молча игнорируется (§2.6) | P1 |
| B-10 | `docs/stack/_templates/auth.md` | секция «Clients: web (cookie) / mobile (bearer)», TTL-таблица, `X-Client` | P0 (док) |
| B-11 | `test/e2e` | pytest: login с `X-Client: android` → tokens; refresh по телу; reuse → 401 + отзыв цепочки; `/sessions` | P1 |

Инвариант, который стоит записать в правило бэкенда: **cookie- и bearer-ветки используют один
`JWTSigner`/`JWTParser` и одну таблицу `refresh_sessions`** — два клиента, один usecase.

**Хранение на клиенте.**

*Android.* `androidx.security:security-crypto` — **все API deprecated с 1.1.0-beta01 (2025-06-04),
stable 1.1.0 (2025-07-30) вышел уже с deprecation**: «Deprecated all APIs in favour of existing
platform APIs and direct use of Android Keystore» [✓ developer.android.com/jetpack/androidx/releases/security].
Канон 2026: **Android Keystore** (`KeyGenParameterSpec`, AES/GCM/NoPadding, `setUserAuthenticationRequired(true)`
+ `setUserAuthenticationParameters(timeout, AUTH_BIOMETRIC_STRONG|AUTH_DEVICE_CREDENTIAL)` для
«refresh за биометрией», `setIsStrongBoxBacked(true)` с fallback по `StrongBoxUnavailableException`) —
key material «never enters the application process», может быть привязан к TEE/StrongBox
[✓ developer.android.com/privacy-and-security/keystore]. Шифротекст токенов — в `DataStore`
(`androidx.datastore:datastore-preferences` 1.2.1 stable [✓ Google Maven]) или `Tink`
(`com.google.crypto.tink:tink-android` 1.23.0, 2026-07-09 [✓ Maven Central]; `AndroidKeysetManager`
оборачивает keyset Keystore-ключом — это то, что делала deprecated-библиотека, но без её обёрток).
Рекомендация шаблона: **своя ~150-строчная `SecureStore` (Keystore AES-GCM + DataStore)**, без Tink —
меньше зависимостей, полный контроль над `UserAuthentication*`. Tink — если нужен envelope/rotation.
Бэкап: `android:allowBackup="false"` либо `dataExtractionRules` с исключением файла DataStore — иначе
шифротекст уедет в облачный бэкап (ключ Keystore не бэкапится → мусор на новом устройстве, но
MASVS-STORAGE-2 всё равно требует не утекать).

*iOS.* Keychain: `kSecClassGenericPassword`, `kSecAttrAccessible = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`
(«cannot be accessed after a restart until the device has been unlocked once … recommended for items
that need to be accessed by background applications. Items with this attribute do not migrate to a
new device» [✓ developer.apple.com/documentation/security/ksecattraccessibleafterfirstunlockthisdeviceonly]),
`kSecAttrSynchronizable = false` (по умолчанию: «If the key is not supplied, or has a value of
kCFBooleanFalse, then no synchronizable items are added» [✓ …/ksecattrsynchronizable]; `ThisDeviceOnly`
и synchronizable несовместимы — там же). Биометрия — `SecAccessControlCreateWithFlags(.biometryCurrentSet)`
на **refresh-элементе**, access-токен — в памяти. Ловушка «Keychain переживает удаление приложения» —
`02-ios-canon.md` §6.5 (первый запуск → очистка).

*KMP-shared (если принята рекомендация 03).* `expect class SecureStore` в `shared/`, `actual` на
Android (Keystore+DataStore) и iOS (Keychain через `platform.Security` в Kotlin/Native **или** Swift-реализация,
переданная в shared через интерфейс — предпочтительно второе, чтобы не воевать с cinterop
`SecItemAdd` и `CFDictionary`). Готовые KMP-библиотеки: `com.liftric:kvault` 1.12.0 (последний релиз
**2023-10-11**, MIT) и `com.russhwolf:multiplatform-settings` 1.3.0 (2024-11-29, Apache-2.0; есть
`KeychainSettings`, но на Android — обычные `SharedPreferences`, не шифрованные) [✓ Maven Central] —
обе не закрывают «Keystore + биометрия» и застыли; не брать.

**TTL и «оставаться в системе».** Access 15 мин (как есть) + refresh 90 дней (mobile) + sliding
(каждый refresh продлевает) — стандарт для бизнес-компаньонов; refresh за биометрией — опция
`AUTH_BIOMETRIC_UNLOCK` в конфиге приложения (см. §4). Access-токен держится **только в памяти**
процесса (`StateFlow`/actor), refresh — в SecureStore; при cold start: прочитать refresh → сразу
`/api/auth/refresh` → access. Это же снимает вопрос «протух ли access за время сна».

**Клиентский auth-слой (общий дизайн для OkHttp/Ktor/URLSession).**

```
AuthInterceptor (OkHttp Interceptor / Ktor Auth plugin `bearer { loadTokens; refreshTokens }` /
                 swift-openapi ClientMiddleware)
  ├─ add Authorization: Bearer <access>          (кроме public операций — список из OpenAPI security)
  ├─ on 401 → single-flight refresh (Mutex / actor) → retry ×1
  ├─ refresh failed (401 REUSED/EXPIRED) → SessionStore.clear() → event LoggedOut
  └─ никогда не ретраить сам refresh
```

Ktor: встроенный плагин `Auth` с провайдером `bearer { loadTokens {…}; refreshTokens {…}; sendWithoutRequest { … } }`
делает ровно это (single refresh, повтор запроса) — в KMP-shared он общий для обеих платформ [~ ktor.io/docs/client-bearer-auth.html].
Swift OpenAPI Generator: `ClientMiddleware.intercept(_:body:baseURL:operationID:next:)` [✓ raw
`swift-openapi-runtime/.../ClientTransport.swift`] — middleware видит `operationID`, значит
«public/protected» можно решать по имени операции без правки сгенерированного кода.

#### 1.3c. Вариант (c) — OIDC/OAuth2 PKCE через Authentik

Факты [✓ docs.goauthentik.io/add-secure-apps/providers/oauth2/]: Authentik 2026.8.2
[✓ GitHub releases, 2026-09-09] поддерживает authorization code + PKCE для public-клиентов, device
code, refresh с опциональной ротацией; refresh только при scope `offline_access` («Starting with
authentik 2024.2, applications receive only an access token by default»); endpoints
`/application/o/authorize/`, `/application/o/token/`, `/application/o/userinfo/`,
`/application/o/<slug>/jwks/`, discovery `/application/o/<slug>/.well-known/openid-configuration`;
redirect URI — exact или regex (custom scheme `tech.stit.acme:/oauth2redirect` задаётся строкой;
`.` в regex экранировать); подпись RS256 при выбранном signing key, иначе HS256 с client secret
(для public-клиента — **обязательно** выбрать signing key, иначе JWKS пуст).

Клиенты: **AppAuth-Android 0.11.1** — последний релиз **2021-12-22** [✓ Maven Central `net.openid:appauth`,
GitHub tags], README без пометки maintenance, minSdk 16, Custom Tabs, PKCE, redirect через
`manifestPlaceholders["appAuthRedirectScheme"]` или https App Links [✓ github README] — библиотека
«застыла, но работает»; альтернатива — свой ~200-строчный PKCE-флоу поверх Custom Tabs
(`androidx.browser`) [?]. **AppAuth-iOS 3.0.0** (2026-08-24) — iOS 15+, SwiftPM (tools 5.7), «to
support Xcode 27» [✓ GitHub release 3.0.0]; под капотом `ASWebAuthenticationSession`
(«ensures that only the calling app's session receives the authentication callback, even when more
than one app registers the same callback URL scheme» [✓ developer.apple.com/…/aswebauthenticationsession]).

Две схемы бэкенда:

| схема | что добавить | плюсы | минусы |
|---|---|---|---|
| **Resource-server**: приложение получает access/ID-token от Authentik и шлёт `Authorization: Bearer` в наш API; бэкенд проверяет по JWKS | `golibinfra`: в `oidckit` уже есть `Verifier` (discovery, JWKS-кэш, verify iss/aud/azp/exp/nonce) — нужен **`oidckit.BearerExtractor`** (`httpkit.AuthExtractor` из `Authorization` → `Verifier.Verify` → `Identity`), плюс JIT-провижининг пользователя (`Hooks.OnLogin` есть) | нативный OAuth, SSO, нет своих паролей | наши `role`/`sub` ≠ Authentik `groups`/`sub` → маппинг; refresh делает Authentik (нет нашей таблицы сессий); Authentik нужен per-project (сейчас только корп) |
| **BFF-mobile**: приложение открывает `GET /auth/login` BFF в Custom Tab, BFF ведёт code-flow и в конце **возвращает наши токены** по custom-scheme redirect (`app://auth?code=<one-time>` → `POST /api/auth/exchange`) | `oidckit`: новый `RegisterMobileRoutes` + one-time code store поверх `cachekit`; сессия — наша bearer-пара из (b) | единый формат токенов для web и mobile; Authentik не виден приложению; refresh/сессии — наши | ещё один редирект-хоп; BFF должен уметь **не** ставить cookie для mobile |

Рекомендация: **(c) не в базовый шаблон**, а как задокументированный extension поверх (b) —
BFF-mobile-схема, потому что она сохраняет единый токен-контракт и таблицу сессий. Cross-repo:
`golibinfra/oidckit` (mobile routes + exchange), `corp/project infra` (Authentik provider с public
client + redirect regex), в `docs/stack/auth.md` бэкенда — поле `sso: none | bff-mobile`.

#### 1.3d. Вариант (d) — passkeys

Android: `androidx.credentials` **1.6.0 stable** (1.7.0-alpha03 в работе) [✓ Google Maven] —
`CredentialManager.createCredential/getCredential` с `CreatePublicKeyCredentialRequest`; RP-домен
должен публиковать `assetlinks.json` с relation `delegate_permission/common.get_login_creds` [~
developer.android.com/identity/sign-in/credential-manager, страница JS-only — не извлечена curl'ом].
iOS: `ASAuthorizationPlatformPublicKeyCredentialProvider` (iOS 16+) + AASA-сервис `webcredentials`
[~]. Бэкенд: WebAuthn RP (`github.com/go-webauthn/webauthn` [~]) — новый kit `webauthnkit` или
расширение `authnkit`; таблица `passkeys(user_id, credential_id, public_key, sign_count, aaguid,
transports)`; эндпоинты `POST /api/auth/passkeys/register/{begin,finish}`, `POST /api/auth/passkeys/login/{begin,finish}`,
после `finish` — та же пара токенов из (b). Оценка: **вторая фаза**; в шаблоне — только шов
(интерфейс `Authenticator` с реализациями `password`, `passkey`).

#### 1.4. Рекомендация

**Дефолт шаблона — (b) Bearer + refresh в JSON, с cookie-режимом бэкенда без изменений для web.**
Обоснование: (1) это единственный вариант, который проходит MASVS-STORAGE-1/2 без самописного
cookie-jar на каждой платформе; (2) минимальный BACKEND-HANDOFF (B-1…B-3 — ~150 строк Go + тесты);
(3) единственный вариант, при котором OpenAPI-кодоген обеих платформ работает без пост-обработки
спека; (4) `oidckit`/passkeys подключаются как расширения поверх, не заменяя контракт. **Переходный
режим**: пока B-1…B-3 не влиты, мобильный шаблон умеет `AUTH_MODE=cookie` (Ktor `HttpCookies` +
in-memory storage; «оставаться в системе» не работает) — только для демо/skeleton'а, флаг помечен
как временный в `docs/stack/auth.md`.

#### 1.5. Certificate pinning — рекомендация «нет по умолчанию, шов есть»

- MASVS-NETWORK-2 требует pinning «for all remote endpoints under the developer's control», но это
  контроль уровня hardening; MASTG прямо называет pinning «hardening practice … not foolproof»,
  рекомендует **Network Security Configuration** как «preferred and recommended way» на Android,
  пиннить **SPKI-хеш** и **всегда иметь backup pin**, ставить `expiration` [✓ raw github
  `OWASP/mastg/knowledge/android/MASVS-NETWORK/MASTG-KNOW-0015.md`]; Android-док: NSC `<pin-set expiration="…">` с backup-pin, «setting an expiration time on pins may enable attackers to bypass your pinned certificates» [✓ developer.android.com/privacy-and-security/security-config]. iOS: `NSPinnedDomains` в `Info.plist` — `NSPinnedCAIdentities` (SPKI CA/sub-CA) и/или `NSPinnedLeafIdentities`, `NSIncludesSubdomains` [✓ developer.apple.com/…/nspinneddomains].
- Наша инфра — Traefik + Let's Encrypt HTTP-01, **ключ leaf-сертификата ротируется каждые ~60–90
  дней** (acme.json в volume, «не бэкапится — при потере VPS сертификаты перевыпускаются заново»
  [✓ `project-infra-template-compose/.claude/rules/traefik-routing.md`]). Пин leaf невозможен; пин
  **intermediate LE** (сейчас R10–R14/E5–E9 [~]) живёт годами, но LE меняет промежуточные ключи
  плановыми ротациями, и старые версии приложения живут месяцами → нужен пин **на корни ISRG Root X1 +
  X2** и backup на второй CA (на случай смены `ACME_CASERVER`). Реальный выигрыш против MITM с
  подменённым корнем на устройстве пользователя — только против «случайных» корпоративных прокси;
  против malware с root-доступом pinning обходится Frida за минуту (MASTG-TECH-0012/0064 [~]).
- **Решение для шаблона**: pinning **выключен по умолчанию**; шов — `network_security_config.xml`
  (release-вариант) и `NSPinnedDomains` с пустым набором + документированная процедура «включить пин
  на корни ISRG + backup + expiration через 12 мес + kill-switch через `GET /api/app/requirements`
  (`pinning: off`)». Владельцу — решение по классу заказчиков (банки/госы потребуют).
- Также: запрет `NSAllowsArbitraryLoads`/`usesCleartextTraffic` в release (гейт: lint `HardcodedText`-класса
  проверка манифеста/Info.plist), self-signed для dev — только через **доверенный корень на
  эмуляторе/симуляторе** + `debug-overrides` в NSC (не через `trust-all` `TrustManager`).

#### 1.6. Root/jailbreak-детект, Play Integrity / App Attest

- Root/jailbreak-детект — **не делать** (MASVS: «absence … does not in itself constitute a vulnerability»;
  ложные срабатывания на кастомных ROM — platform lock-in [✓ `11-MASVS-RESILIENCE.md`]; для
  RU-рынка доля «неканонических» Android заметна — `05-distribution-ru.md` §2.0).
- **Play Integrity API** (`com.google.android.play:integrity` 1.6.0 [✓ Google Maven]): требует Google
  Play services, «up to 10,000 total requests per day across all installs» по умолчанию
  [✓ developer.android.com/google/play/integrity/overview] — на RuStore-устройствах без GMS не
  работает; **App Attest** (`DCAppAttestService`, ключ в Secure Enclave, attestation через Apple)
  [✓ developer.apple.com/documentation/devicecheck/establishing-your-app-s-integrity] — работает
  только при `isSupported`. Оба — **опция за флагом**, серверная проверка — отдельный kit
  (`integritykit`: Play Integrity decrypt/verify + App Attest CBOR) — **не в первой версии**.
- «Секретов в приложении нет» — инвариант (см. §4): в артефакте только public-by-design значения;
  всё платное — через бэкенд.

### §2. Контракт API и кодоген

#### 2.1. Как мобильный репо получает спек

Факты о huma [✓ huma.rocks/features/openapi-generation, /features/cli; код `huma@v2.39.1`]:
- рантайм-эндпоинты: `/openapi.json` и `/openapi.yaml` (**3.1.0**), `/openapi-3.0.json` /
  `/openapi-3.0.yaml` (downgrade до 3.0.3), `/docs`, `/schemas/{schema}`; в httpkit пути включены,
  пока `HTTP_DOCS_ENABLED=true` (default) [✓ `httpkit/server.go`];
- программный экспорт: `api.OpenAPI().YAML()` / `.DowngradeYAML()` / `MarshalJSON()`; в
  `humacli` документирован паттерн кастомной команды `openapi`, которая «never starts the server».

Варианты доставки спека в мобильный репо:

| вариант | как | оценка |
|---|---|---|
| A. Запущенный бэкенд (`curl https://api.dev.<domain>/openapi.yaml`) | dev-среда всегда есть | ❌ недетерминированно (что задеплоено ≠ что в `main`), требует сети в гейте, ломает sibling-checkout-модель |
| **B. Закоммиченный `openapi.yaml` в backend-репо через `make openapi` + `gen-check`** | в `go-backend-template`: `cmd/openapi/main.go` (сборка `huma.API` **без** fx/БД: `httpkit.NewServer(cfg, WithSecuritySchemes(...))` + `Register(api, NewHandler(nil…))` всех доменов — ровно так сделан дамп в §2.2), цель `make openapi` пишет `api/openapi.yaml`, `make gen-check` падает при дрейфе (как `sqlc diff`) | ✅ детерминизм, ревью diff'а спека в PR, версия спека = коммит бэкенда; **это cross-repo запрос B-12** |
| C. Sibling-checkout `../backend/api/openapi.yaml` | мобильный `make gen` читает файл соседа по `backend-path` из `docs/stack/service.md` (дефолт `../backend`), копирует в `api/openapi.yaml` мобильного репо **и коммитит копию** (как `routeTree.gen.ts` у SPA — сгенерированное лежит в репо) | ✅ работает офлайн; `make gen-check` мобильного репо сравнивает копию с соседом (если сосед есть) и сгенерированный код с копией |
| D. Артефакт релиза бэкенда (`openapi.yaml` как ассет Gitea Release `vX.Y.Z`) | мобильный репо пиннит `BACKEND_API_VERSION=vX.Y.Z` и скачивает | ✅ для «старые версии приложения живут месяцами» — зафиксировано, против какой версии API собрано приложение; ⚠️ требует сети |

**Рекомендация: B + C как основа, D как расширение `/release` бэкенда** (одна строка в
`scripts/release.sh`: приложить `api/openapi.yaml` к Gitea Release). Мобильный репо держит
`api/openapi.yaml` (копия) + `api/openapi.lock` (`{backend_commit, backend_tag, sha256}`) — по нему
`/smartdev` показывает drift относительно `../backend`.

Дамп спека текущего шаблона выполнен на этой машине [изм]: копия репо в scratchpad +
`cmd/openapidump/main.go` (30 строк, `go run` с локальным module cache, Go 1.26.5) → `openapi.yaml`
743 строки (3.1.0) и `openapi-3.0.yaml` 721 строка (3.0.3); `openapi-generator-cli 7.25.0 validate`
→ «No validation issues detected». Это доказывает, что B реализуем без запуска БД/NATS/Redis.

#### 2.2. Что реально лежит в спеке (дамп 2026-09-17) и что это значит для клиента

| наблюдение | цитата из дампа | следствие |
|---|---|---|
| Все body-схемы имеют служебное поле `$schema` (`readOnly`, `format: uri`) — его добавляет `SchemaLinkTransformer` через `CreateHooks` в `huma.DefaultConfig`; **выключить его через `SchemasPath=""` нельзя** (guard'а в `transforms.go` нет), только убрав hook/transformer | `LoginInputBody.properties.$schema` | генераторы плодят поле `dollarSchema: String?` в каждой модели (Kotlin) — мусор в DTO. Handoff B-13: в httpkit `HumaConfigGroup`-функция бэкенда `func(c *huma.Config){ c.CreateHooks = nil }` **или** post-фильтр спека в `make openapi` (`yq 'del(.. | .["$schema"]?)'`) |
| Envelope: единичные ответы — `{data: <View>}`, списки — `{data: [], total, page, size}`; **но** `LoginOutputBody` — `{user: AuthUserView}` (не `data`) | `UserMeOutputBody.data`, `UserListOutputBody.{data,total,page,size}`, `LoginOutputBody.user` | канон `architecture.md` («Response-envelope — `Body.Data`») нарушен в reference-домене auth — унифицировать (B-14) или закрепить исключение для auth в клиентском маппере |
| `data` списка — `type: [array, "null"]` (Go `[]T` → nullable) | `UserListOutputBody.data` | Kotlin: `List<UserView>?`; Swift: `[UserView]?`. Клиент обязан трактовать `null` как пустой список. Лучше на бэкенде инициализировать `[]` (huma-тег `nullable:"false"` [~]) — B-14 |
| Ошибки — RFC 9457 `application/problem+json`, схема `ErrorModel{type,title,status,detail,instance,errors[]{location,message,value}}`; **поле `code` (из `httpkit.APIError`) в схеме отсутствует**, хотя в ответах оно есть (`{"code":"AUTH_INVALID_CREDENTIALS",...}`) | `components.schemas.ErrorModel`; `httpkit/error.go: APIError.Code json:"code"` | генератор не знает про `code` → в клиенте `code` теряется. Handoff B-15: зарегистрировать `httpkit.APIError` как error-схему (huma `Config.Components.Schemas` / `huma.ErrorModel` override) **или** документировать `x-codes`; до этого — клиентский `ApiError` парсит `code` руками |
| Валидационные ошибки huma — `422` с `errors[]` (`location: body.email`) | huma docs [✓] | клиент маппит `errors[].location` → поле формы; SPA-паритет: zod-ошибки vs серверные |
| `refresh`/`logout` — `parameters: [{in: cookie, name: refresh_token, schema: $ref Cookie}]`, где `Cookie` — **вся структура `http.Cookie` Go** (Domain, Expires, HttpOnly, …, `Unparsed: [array,null]`) | `components.schemas.Cookie` | кодоген генерирует бессмысленный тип `Cookie` и параметр `refreshToken: Cookie?`; исчезает при переходе на body (B-3) |
| `Set-Cookie` в responses — `headers.Set-Cookie.schema.type: string` | `/api/auth/login.responses.200.headers` | безвредно |
| Имена схем = имена Go-типов (`UserMeOutputBody`, `AuthUserView`) — глобально уникальны (правило huma registry) | — | хорошо для генераторов; правило naming бэкенда уже это гарантирует |
| Enum query `status: active,blocked` | `/api/users.parameters.status` | Kotlin: `StatusListUsers` enum (сгенерирован) |
| `date-time` → Kotlin `kotlin.time.Instant` (dateLibrary=kotlinx-datetime), Swift `Foundation.Date` | дамп + генерация | ok |
| `info.version: 0.1.0`, `title: API` — из `HTTP_API_VERSION`/`HTTP_API_TITLE` (env) | `httpkit/server.go` | версия спека должна ставиться из git-тега в `make openapi` (B-12) |
| `servers` отсутствуют | — | Swift OpenAPI Generator требует передавать `serverURL` в `Client(serverURL:transport:)`; Kotlin — `baseUrl` в `ApiClient` — как раз наш `API_BASE_URL` per flavor |

#### 2.3. Генераторы — сравнение

| критерий | **Swift OpenAPI Generator** 1.13.1 (2026-09-01, Apache-2.0) | **openapi-generator `kotlin`** 7.25.0 (2026-08-24, Apache-2.0), `library=multiplatform` | openapi-generator `swift6` 7.25.0 | **Ktorfit** 2.7.5 (2026-06-07, Apache-2.0) + рукописные DTO | полностью рукописные модели (как zod в SPA) |
|---|---|---|---|---|---|
| OpenAPI 3.1 | ✅ «focused on supporting OpenAPI 3.0.3 and OpenAPI 3.1.0», внутри всё конвертируется в 3.1; «preliminary support for 3.2» [✓ README, Supported-features] | ⚠️ «OpenAPI 3.1 support is still in beta» — предупреждение при генерации [изм]; README: «3.1 (beta support)» [✓] | ⚠️ то же | n/a (не читает спек) | n/a |
| `in: cookie` параметры | ❌ («form (in cookie)» не поддержан) | ❌ (Parameter Feature Cookie ✗) [✓] | ✅ (Cookie ✓) [✓] | вручную | вручную |
| security schemes | ❌ **не поддержаны вовсе** — auth только через `ClientMiddleware` [✓ Supported-features] | ✅ ApiKey/Bearer/OAuth2, но `apiKey in cookie` → `ApiKeyAuth("query", …)` — **токен уехал бы в query string** [изм, §2.4] | ✅ | вручную (Ktor `Auth` plugin) | вручную |
| oneOf/anyOf/allOf | ✅ (wrapper-структуры, discriminator) | ❌ (Schema Support Feature oneOf/anyOf/allOf/Polymorphism ✗) [✓] | ✅ Polymorphism, ❌ oneOf/allOf/anyOf | вручную (`sealed interface` + `kotlinx.serialization` polymorphic) | вручную |
| качество кода | идиоматичный Swift 6, `namingStrategy: idiomatic`, `typeOverrides`, `filter` по тегам/операциям; сгенерированный код не коммитится (build plugin) либо коммитится (CLI/command plugin) [✓ Configuring-the-generator] | Ktor-клиент с `ApiClient`, `HttpResponse<T>`, `@Required`, `kotlin.time.Instant`; переименования `size→propertySize`, `$schema→dollarSchema` [изм]; шаблон multiplatform требует Ktor 3.5.1 / Kotlin 2.4.0 (в mustache-шаблоне) [✓ raw template] — версии зашиты в **сгенерированный** `build.gradle.kts`, но в нашем случае мы генерируем только `src/` в существующий модуль | Swift 5/6 URLSession/Alamofire | код пишется руками, генерируется только клей (KSP) | 0 генерации |
| детерминизм / `gen-check` | ✅ build plugin генерирует в `DerivedData`/`.build` на каждой сборке — дрейф невозможен по построению; CLI-режим — `git diff --exit-code` | ✅ CLI детерминирован (проверить: `--additional-properties=…` фиксированы, генератор не пишет timestamp'ы в код [~]) → `make gen && git diff --exit-code` | ✅ | ✅ (KSP на каждой сборке) | ❌ дрейф ловится только тестами против mock-фикстур |
| Linux | генератор — CLI на Linux ✅ (`Package.swift` platforms: macOS 10.15 + Linux/Windows для запуска [✓]); плагин SwiftPM — на Linux ✅ (см. 06); сгенерированный код — pure Swift + `swift-openapi-urlsession` (Foundation) — `swift test` на Linux ✅ [~] | JVM ✅ | JVM ✅ | JVM ✅ | ✅ |
| размер сгенерированного | `Types.swift`/`Client.swift` — для нашего спека ~1–2 kLOC [?]; runtime `swift-openapi-runtime` 1.12.1 + `swift-openapi-urlsession` 1.3.1 | для 14 операций — 34 файла `.kt` (модели + api + infrastructure + auth + тесты) [изм] | — | минимум | минимум |
| middleware для auth/retry/`X-Request-ID` | ✅ `ClientMiddleware` с `operationID` | ⚠️ через `HttpClientConfig` (Ktor plugins) — `ApiClient(baseUrl, httpClientEngine, httpClientConfig)` [изм] | ⚠️ `OpenAPIClient` interceptors | ✅ Ktor plugins | ✅ |
| зрелость / поддержка | Apple + SSWG incubating; активные релизы (1.13.1 — 2026-09-01) | стабильно (`STABLE` у kotlin-генератора) [✓], 26.7k★, активен | STABLE | 2.1k★, релизы регулярно, Kotlin ≥ 2.2.0, KSP ≥ 2.0.2, Ktor 3.5.0 [✓ CHANGELOG 2.7.5] | — |

#### 2.4. Эмпирика: `openapi-generator kotlin --library multiplatform` на реальном спеке [изм]

Команда: `java -jar openapi-generator-cli-7.25.0.jar generate -g kotlin --library multiplatform
--additional-properties serializationLibrary=kotlinx_serialization,dateLibrary=kotlinx-datetime,packageName=tech.stit.api
-i openapi.yaml -o gen-kmp` (JDK 21.0.12.1, 2026-09-17). Результат: 34 `.kt`, ворнинг «OpenAPI 3.1
support is still in beta». Ключевые находки в сгенерированном коде:

```kotlin
// infrastructure/ApiClient.kt — security scheme Cookie(apiKey in cookie) превратился в QUERY-параметр:
private val authentications: Map<String, Authentication> by lazy {
    mapOf("Cookie" to ApiKeyAuth("query", "access_token"))
}
// auth/ApiKeyAuth.kt — поддержаны только "query" и "header"; location "cookie" молча игнорируется
when (location) { "query" -> query[paramName] = listOf(value); "header" -> headers[paramName] = value }

// apis/AuthApi.kt — cookie-параметр стал бессмысленным типом:
open suspend fun refreshToken(refreshToken: Cookie? = null): HttpResponse<Unit>
open suspend fun logoutUser(refreshToken: Cookie? = null): HttpResponse<Unit>

// models/LoginInputBody.kt / UserListOutputBody.kt
@SerialName(value = "\$schema") val dollarSchema: kotlin.String? = null
@SerialName(value = "data") @Required val `data`: kotlin.collections.List<UserView>?,
@SerialName(value = "size") @Required val propertySize: kotlin.Long,
```

Выводы: (1) на cookie-контракте кодоген даёт **опасный** артефакт (`setApiKey(...)` положил бы
access-token в query, т. е. в access-логи Traefik/CrowdSec); (2) после B-1…B-3 (bearer + body) и
B-13 (`$schema`) спек становится «чистым» и генерируется без правок; (3) `List<UserView>?` требует
B-14 либо клиентского `orEmpty()`.

#### 2.5. Рекомендация по кодогену

- **iOS: Swift OpenAPI Generator** (build plugin в SwiftPM-пакете `Packages/API`, `generate: [types, client]`,
  `namingStrategy: idiomatic`, `accessModifier: package`, `filter.tags` по мере роста API; транспорт
  `URLSessionTransport`; middleware: `AuthMiddleware` (bearer + single-flight refresh), `RequestIDMiddleware`,
  `RetryMiddleware` (по `Retry-After`/429/503, только идемпотентные + `Idempotency-Key`), `LoggingMiddleware`
  (`os.Logger`, без тел). `swiftlint:disable all` через `additionalFileComments`. Проверено потоком 02
  (§4.7: mock через кастомный `ClientTransport`).
- **Android/KMP-shared: openapi-generator `kotlin` `library=multiplatform` + `serializationLibrary=kotlinx_serialization`**
  **только для моделей и сигнатур**, с `make gen` в репо (CLI-jar пиннится в `~/.cache/stit-mobile-research`-стиле
  или через Gradle-плагин `org.openapi.generator` той же версии) и `make gen-check` (`git diff --exit-code -- shared/src/commonMain/kotlin/<pkg>/api/generated`).
  Сетевой клиент — **свой тонкий `ApiClient` поверх Ktor 3.6.0** (Auth-plugin bearer, `HttpCookies`
  не нужен, `ContentNegotiation(json)`, `HttpRequestRetry`, `X-Request-ID`, `Idempotency-Key`), а
  сгенерированный `infrastructure/ApiClient.kt` **не используется** — генератор настраивается
  `--global-property models,apis,supportingFiles=false`… (проверить точный набор в spike 06; если
  `apis` без `supportingFiles` не компилируются — генерировать только `models` и писать `apis`
  Ktorfit-интерфейсами). Альтернатива без второго генератора — **Ktorfit-интерфейсы вручную +
  сгенерированные модели** (сочетание, которое даёт type-safety и не тянет чужой `ApiClient`).
- **Не брать**: `swift6`-генератор openapi-generator (дублирует Apple-инструмент, хуже интеграция со
  Swift 6 concurrency [~]); полностью рукописные модели на обеих платформах (это zod-подход SPA, но у
  SPA нет второго языка — на мобильных дублирование ×2 и дрейф ×2).
- **Гейт**: `make gen-check` = (а) копия `api/openapi.yaml` == `../backend/api/openapi.yaml` (если
  сосед есть), (б) `git diff --exit-code` по сгенерированным Kotlin-моделям, (в) Swift — build plugin
  (дрейфа нет по построению), но `swift build` пакета `API` на Linux — часть `make check`.

#### 2.6. Конвенции контракта (обязательны для бэкенда — правила «API для долгоживущих клиентов»)

| конвенция | как сейчас | требование для мобильного |
|---|---|---|
| Envelope | `{data}` / `{data,total,page,size}` (кроме auth) | единый `data`; списки — `data` **всегда массив** (не `null`); пагинация `page/size/total` (offset) сегодня; для лент — добавить cursor-вариант `{data, next_cursor}` (B-16, по необходимости) |
| Ошибки | `application/problem+json`, `APIError{type,title,status,detail,instance,code}`; незамапленные → `500 {"code":"INTERNAL","detail":"internal error"}` [✓ httpkit docs] | `code` — **стабильный контракт** (клиент маппит на локализованные строки по ключу `error.<CODE>`); `detail` — только для логов/dev; новые коды добавлять, старые не переименовывать |
| `X-Request-ID` | принимается от клиента, если печатный ASCII ≤ 128 байт, иначе UUIDv7 сервера; возвращается в ответе; попадает во все логи (`logkit.RegisterAttr("request_id")`) [✓ `httpkit/middleware.go`] | клиент генерирует UUIDv7/v4 на каждый запрос, логирует его в breadcrumb Sentry, при ошибке показывает пользователю «код обращения: <первые 8 символов>» — корреляция с логами бэкенда |
| `Idempotency-Key` | httpkit `Idempotency`: `POST/PUT/PATCH` с заголовком выполняются один раз, повтор → сохранённый ответ + `Idempotent-Replayed: true`; тот же ключ с другим телом → `422 IDEMPOTENCY_KEY_REUSED`; параллельный повтор → `409 IDEMPOTENCY_IN_PROGRESS`; TTL `HTTP_IDEMPOTENCY_TTL` 24h; ключ scope'ится субъектом auth; **без валидных credentials дедупликации нет** [✓ `httpkit/docs/usage.md`, `idempotency.go`] | клиент: UUID на **каждую мутацию**, хранится вместе с задачей в очереди (`WorkManager`/`BGTaskScheduler`) и переиспользуется при ретрае; тело запроса **байт-в-байт** одинаковое (сериализация детерминированная — `Json { encodeDefaults=true }`; в Swift — `JSONEncoder.outputFormatting = .sortedKeys`); лимит тела 64 КиБ (`HTTP_IDEMPOTENCY_MAX_BODY_BYTES`) — больше не дедуплицируется; на бэкенде **включить store** (B-9) |
| 429 / `Retry-After` | rate-limit → `429 RATE_LIMITED` + `Retry-After: 1` [✓ `ratelimit.go`]; brute-force lock → `429 AUTH_ACCOUNT_LOCKED` без `Retry-After` | клиент: `Retry-After` (секунды или HTTP-дата, RFC 9110) с потолком (5 мин, как у `httpclient.ParseRetryAfter`), экспоненциальный backoff с jitter для 408/429/502/503/504; **не** ретраить 500; POST — только с `Idempotency-Key` (симметрично политике `httpclient` Go) |
| Версионирование API | путей `/v1` **нет** (`/api/auth`, `/api/users`); SPA-мок ходит на `/api/v1/*` | правило: **аддитивная эволюция без версии в пути** (новые поля optional; удаление/переименование поля или смена типа = новая операция/путь `…/v2`), потому что старые версии приложения живут месяцами и «принудить обновиться» можно только через `GET /api/app/requirements` (§4). Клиент **обязан игнорировать неизвестные поля** (Kotlin `Json { ignoreUnknownKeys = true }`; Swift `Codable` игнорирует по умолчанию) и **не падать на неизвестных enum-значениях** (`coerceInputValues` / `unknown` case; в Swift генераторе — `oneOfUnknownDefaultCase`-аналог для enum отсутствует → enum'ы в спеке помечать `x-open-enum` и генерировать `String` [?]) |
| Обратная совместимость — процесс | — | правило `.claude/rules/api-compat.md` бэкенда: любое изменение `api/openapi.yaml` проходит `oasdiff breaking` (инструмент `oasdiff` [~]) в `make gen-check`; breaking → отдельная операция + запись в `docs/stack/api.md § Deprecations` с датой минимальной поддерживаемой версии приложения |
| `User-Agent` / `X-Client` | — | `User-Agent: <app-slug>/<version>+<build> (<os> <ver>; <model>; <client>)`; `X-Client: android|ios` — для сессий и `requirements` |
| Время/деньги | `date-time` RFC 3339 UTC; денег в reference нет | деньги — `{amount: "123.45", currency: "RUB"}` строкой (decimal), не float [?] |
| Загрузка файлов | — | `multipart/form-data` + presigned S3 (MinIO `s3.${DOMAIN}`) — предпочтительно presigned (без прокачки через бэкенд); Swift OpenAPI Generator multipart ✅ [✓] |
| CORS | `HTTP_CORS_HEADERS` default `Authorization,Content-Type,X-Request-ID`; `Idempotency-Key` **не** в allow-list [✓ `cors.go`] | для мобильных CORS не действует; для SPA-паритета добавить `Idempotency-Key` в `HTTP_CORS_HEADERS` (infra env) — ловушка |

### §3. Mock-режим (аналог `empty | populated | authenticated | error`)

#### 3.1. Что даёт SPA и что переносим

SPA: `runtimeConfig.API_MODE = mock|real`, `MOCK_SCENARIO`, `MOCK_DEVTOOLS`; MSW + `@mswjs/data`
(in-memory ORM), сценарии — функции `loadScenario(db, scenario)`, `error` = все ручки `500`;
persist в `localStorage`; те же handlers в тестах через `setupServer(...buildHandlers(db, false))`
[✓ `spa-frontend-template/src/shared/api/mock/*`]. Переносимые идеи: **сценарий = детерминированное
состояние in-memory БД**, **один набор фейков для dev-сборки и тестов**, **переключатель в UI**.

#### 3.2. Где ставить шов на мобильных

| уровень шва | Android | iOS | плюсы | минусы |
|---|---|---|---|---|
| **Репозиторий/data-source** (fake `NetworkDataSource`, как NiA `demo`-flavor: `core/network/src/demo/.../FlavoredNetworkModule.kt` биндит `DemoNiaNetworkDataSource`, читающий JSON из assets [✓ raw github nowinandroid]) | `demo` product flavor (`applicationIdSuffix ".demo"`, отдельный source set `src/demo`) [✓ `NiaFlavor.kt`] | отдельная схема/`xcconfig` `Demo` + `#if DEMO` / DI-контейнер | быстро, без сети, детерминированно; **App Review требует demo-mode** (`05` §8.4) | не проверяет сериализацию/HTTP-слой |
| **HTTP-транспорт** (in-process): Ktor `MockEngine` (`io.ktor:ktor-client-mock` 3.6.0, common-таргеты [✓ Maven Central; docs: `MockEngine { request -> respond(content, status, headers) }`]); swift-openapi `ClientTransport`-мок (протокол `ClientTransport` — любая реализация; `02` §4.7) / `URLProtocol`-стаб для `URLSession` | ✅ в KMP-shared один `MockEngine` на обе платформы | `ClientTransport` мок для swift-openapi; `URLProtocol` — если shared-слоя нет | проверяет DTO/сериализацию/ошибки/`Retry-After`; **один JSON-фикстурный набор** на обе платформы | нужен «мини-сервер» с роутингом путей |
| **Сетевой mock-сервер**: OkHttp `mockwebserver3` 5.5.0 (`com.squareup.okhttp3:mockwebserver3`, JUnit-независимый; API стабилизирован в 5.0) [✓ Maven Central; OkHttp CHANGELOG] | JVM-тесты | — | реальный TCP, проверка интерцепторов OkHttp | только JVM, только тесты |
| **Внешний mock** (MSW-подобный HTTP-сервер из тех же фикстур) | эмулятор → `10.0.2.2:PORT` | симулятор → `localhost` | единые фикстуры с SPA (`@mswjs/data` уже есть) | ещё один процесс; для демо в сторе непригоден |

**Рекомендация — двухслойный дизайн, единый для обеих платформ:**

1. **Слой фикстур (общий)** — `mock/fixtures/<scenario>/<entity>.json` (тот же формат, что ответы
   API, включая envelope) + `mock/scenarios.yaml` (какие фикстуры и какие «правила» —
   `error` = все ручки 500, `authenticated` = seed-сессия). Файлы — Android `assets/mock/`,
   iOS — ресурсы SwiftPM-пакета `Mock` (`Bundle.module`); в KMP — `commonMain/resources` (или
   генерация Kotlin-объектов из JSON на `make gen`, чтобы не тащить IO в common [?]).
2. **Слой транспорта (общий по форме, разный по реализации)**: `MockTransport` — маленький роутер
   «метод+путь → фикстура/поведение» с in-memory состоянием (create/patch/delete меняют состояние,
   как `@mswjs/data`): Ktor `MockEngine` (shared/Android) и swift-openapi `ClientTransport` (iOS)
   **читают один и тот же `scenarios.yaml`/фикстуры**. Так `AUTH_MODE`, cookies/bearer, `Retry-After`,
   `422 errors[]` — всё проходит через настоящий клиентский стек.
3. **Переключение**: build-flavor/схема `demo` (для стора: **не ходит в прод**, `API_MODE=mock`
   зашит) **плюс** debug-панель в `dev`/`staging` сборках (сценарий, задержка сети, `error`-режим,
   сброс состояния — аналог `mountMockDevtools`), состояние — в `DataStore`/`UserDefaults` под
   ключом `app.mock.scenario`.
4. **Тесты используют те же фейки**: JVM-тесты (`shared/jvmTest`, `feature/*/test`) поднимают
   `MockEngine` с `scenario=populated`; Swift Testing — тот же `MockTransport` пакета `Mock`;
   screenshot-тесты (Roborazzi / swift-snapshot-testing) — сценарии `empty|populated|error` как
   «состояния экрана» (паритет с канон-состояниями screens `loading/empty/error/…`).
5. **Паритет с SPA**: имена сценариев те же (`empty | populated | authenticated | error`), фикстуры
   можно генерировать из одной `@mswjs/data`-схемы SPA (`make mock-sync` читает
   `../frontend/src/shared/api/mock/seeds` [?]) — но это опционально; обязателен лишь **общий
   словарь сценариев** в strategy (`shared-api.md § Сценарии данных` уже есть в FSD-карте — тот же
   список для мобильного слоя, см. §9).

### §4. Среды и конфиг

#### 4.1. Модель: build-time · release-time · runtime

«Один образ на все среды» (SPA: `window.__APP_CONFIG__` из env контейнера) на мобильных
**невозможен**: application ID / bundle ID, иконка, `API_BASE_URL`, deep-link-хост и подпись
запекаются в артефакт (`05` §8.3). Три категории конфига:

| категория | где живёт | секрет? | примеры |
|---|---|---|---|
| **build-time** (в артефакте, публичен) | Android: `productFlavors` (`dev`/`staging`/`prod`, `flavorDimensions += "env"`, `applicationIdSuffix`, `versionNameSuffix`, `buildConfigField`, `resValue`, `manifestPlaceholders`) [✓ developer.android.com/build/build-variants]; iOS: `Config/{Base,Dev,Staging,Prod}.xcconfig` → `Info.plist` `$(VAR)` → типизированный `AppConfig` (`02` §3.4, ловушка `https:/$()/host`) | нет | `API_BASE_URL`, `APP_ENV`, `DEEP_LINK_HOST`, `SENTRY_DSN`, `PUSH_PROVIDERS`, `FEATURES_DEFAULT` |
| **release-time** (нужны сборке/публикации, **не** в артефакте) | env процесса сборки из Vaultwarden; `local.properties` / `*.xcconfig.local` — в `.gitignore` | да | keystore, Play/RuStore/ASC ключи, `SENTRY_AUTH_TOKEN` (`05` §8.3) |
| **runtime** (меняется без релиза) | **бэкенд**: `GET /api/app/requirements` + feature-flags; локально кэшируется | нет | min-version, kill-switch, URL политики, флаги, `pinning: off` |

Debug-переключатель среды в рантайме (одна `dev`-сборка ходит в dev/staging/локальный
`10.0.2.2:8080`): **только в `dev`-flavor/схеме**, список — `API_BASE_URL_CHOICES` в build config,
выбор — DataStore/UserDefaults; `prod`/`staging` артефакты его не содержат (иначе ревьюер стора
увидит скрытые меню, а пользователь — не ту среду). Смена среды = очистка SecureStore и кэшей.

#### 4.2. Env-контракт мобильного репо (черновик таблицы для `docs/stack/config.md`)

| ключ | где живёт | секрет? | пример / формат | кто задаёт |
|---|---|---|---|---|
| `APP_ENV` | build config (flavor/xcconfig) | нет | `dev` \| `staging` \| `prod`; виден на экране «О приложении» | шаблон |
| `APP_ID_SUFFIX` / `BUNDLE_ID_SUFFIX` | build config | нет | `.dev`, `.staging`, `` (prod) | шаблон |
| `API_BASE_URL` | build config | нет | `https://api.dev.acme.ru` (без `/api`; путь операций уже содержит `/api/...`) | `<org>/infra` (домены сред: `api.${DOMAIN}` per VPS) |
| `API_BASE_URL_CHOICES` | build config, только `dev` | нет | `https://api.dev.acme.ru,http://10.0.2.2:8080` | шаблон |
| `API_MODE` | build config (`demo`-flavor фиксирует `mock`) + debug-панель | нет | `real` \| `mock` | шаблон |
| `MOCK_SCENARIO` | debug-панель/DataStore, дефолт build config | нет | `populated` | шаблон |
| `AUTH_MODE` | build config (переходный) | нет | `bearer` (дефолт) \| `cookie` | шаблон |
| `AUTH_BIOMETRIC_UNLOCK` | build config + настройка пользователя | нет | `true` | проект |
| `DEEP_LINK_HOST` | build config → `intent-filter`/`applinks:` | нет | `app.dev.acme.ru` (per env — свои AASA/assetlinks) | `<org>/infra` |
| `DEEP_LINK_SCHEME` | build config | нет | `acme` (fallback custom scheme) | шаблон |
| `SENTRY_DSN` | build config | нет (public by design) | `https://<key>@glitchtip.acme.ru/2`; пусто = SDK не инициализируется | `<org>/infra` (GlitchTip/Sentry хост) |
| `SENTRY_ENVIRONMENT` | = `APP_ENV` | нет | — | шаблон |
| `RELEASE` | вычисляется из git-тега на сборке | нет | `tech.stit.acme@1.4.2+1004002` (формат Sentry `package@version+build`) | `/release` |
| `PUSH_PROVIDERS` | build config | нет | Android RU-flavor `rustore,fcm,hms`; global `fcm`; iOS `apns` | проект (`docs/stack/push.md`) |
| `FCM_*` (`google-services.json`) / `RUSTORE_PROJECT_ID` / `HMS_APP_ID` | файлы/ресурсы per flavor | **нет** (client-side ID, не секрет) — но серверные ключи (`FCM_SERVICE_ACCOUNT_JSON`, `RUSTORE_SERVICE_TOKEN`, `APNS_P8`) **только на бэкенде** | — | проект / `<org>/infra` (.env бэкенда) |
| `FEATURE_FLAGS_DEFAULT` | build config | нет | `new_checkout=false` — офлайн-дефолты флагов | проект |
| `REQUIREMENTS_ENDPOINT` | константа `/api/app/requirements` | нет | — | бэкенд (B-17) |
| `PRIVACY_POLICY_URL`, `ACCOUNT_DELETION_URL` | build config → экран «О приложении»/стор | нет | `https://acme.ru/privacy` | `<org>/infra` (public URL, `05` §9.2) |
| `MAPS_*` (Yandex MapKit key и т. п.) | build config | «public-by-design с ограничением по applicationId/bundle» **или** прокси через бэкенд | — | проект |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | build config, опц. | нет | `https://otlp.acme.ru` | проект (если OTel включён, §7) |
| `ANDROID_KEYSTORE_*`, `PLAY_SERVICE_ACCOUNT_JSON`, `RUSTORE_KEY_ID`/`RUSTORE_PRIVATE_KEY`, `APPSTORE_API_KEY_ID`/`ISSUER_ID`/`P8`, `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`/`SENTRY_PROJECT`/`SENTRY_URL` | **release-time**, env процесса `make release` / `local.properties` / `*.xcconfig.local` (gitignored) | **да** | — | Vaultwarden `mobile/<org>/<app>` (`05` §7.6) |

Аналог `docs/stack/config.md` — тот же kind `config`, но с тремя секциями «build-time / release-time
/ runtime» вместо «required/optional env» бэкенда; `Generates`: `gradle/env.<flavor>.properties` →
`buildConfigField`, `Config/<Env>.xcconfig`, `AppConfig.kt`/`AppConfig.swift`, `.gitignore`-строки,
release-checklist (какие секреты должны быть в env `make release`).

#### 4.3. Runtime-конфиг с бэкенда — `GET /api/app/requirements` (черновик, cross-repo B-17)

```yaml
# ЧЕРНОВИК · go-backend-template, новый домен `app` (или platform-эндпоинт без домена)
/api/app/requirements:
  get:
    operationId: getAppRequirements
    security: []            # публичный: работает до логина
    parameters:
      - { in: query, name: platform, required: true, schema: { type: string, enum: [android, ios] } }
      - { in: query, name: version,  required: true, schema: { type: string, description: "semver 1.4.2" } }
      - { in: query, name: build,    required: false, schema: { type: integer, format: int64 } }
      - { in: query, name: store,    required: false, schema: { type: string, enum: [play, rustore, appgallery, appstore, direct] } }
    responses:
      "200":
        headers: { Cache-Control: { schema: { type: string, example: "public, max-age=300" } } }
        content:
          application/json:
            schema:
              type: object
              required: [data]
              properties:
                data:
                  type: object
                  required: [status, min_version, latest_version, flags, links]
                  properties:
                    status: { type: string, enum: [ok, update_recommended, update_required, maintenance] }
                    min_version: { type: string, example: "1.2.0" }
                    latest_version: { type: string, example: "1.5.0" }
                    store_url: { type: string, format: uri }
                    maintenance_until: { type: string, format: date-time }
                    flags:
                      type: object
                      additionalProperties: { type: string }      # как flagkit: строки, типизация на клиенте
                      example: { new_checkout: "true", pinning: "off" }
                    links:
                      type: object
                      properties:
                        privacy_policy: { type: string, format: uri }
                        account_deletion: { type: string, format: uri }
                        support: { type: string, format: uri }
```

Реализация на бэкенде — `flagkit` как есть (`FEATURE_FLAGS=…` env, «Снимок на старте … изменение
флага требует рестарта» [✓ `flagkit/README.md`]) + env `APP_MIN_VERSION_ANDROID`,
`APP_MIN_VERSION_IOS`, `APP_LATEST_VERSION_*`, `APP_STORE_URL_*` — достаточно для kill-switch без
нового сервиса; перезапуск контейнера через `make deploy` — приемлемая цена. Клиент: запрос на
старте и при возврате из фона (не чаще раза в 5 мин), `update_required` → блокирующий экран,
`maintenance` → экран обслуживания, флаги → `FeatureFlags` со значениями по умолчанию из
`FEATURE_FLAGS_DEFAULT`.

Внешние feature-flag-платформы (проверено 2026-09-17): **Unleash** — сервер v8.2.0, но лицензия
сервера **AGPL-3.0** [✓ raw LICENSE], Android SDK `io.getunleash:unleash-android` 3.3.2 (Frontend
API/Edge, polling 60 с, bootstrap-режим, API 21+) [✓ Maven Central; docs.getunleash.io]; **Flagsmith**
v2.273.0, self-host Docker/K8s [✓ docs.flagsmith.com/deployment/overview], лицензия OSS-издания
BSD-3-Clause [~]. Оба — отдельный сервис с БД на проектной VPS. **Не закладывать в шаблон**:
`requirements.flags` + `flagkit` закрывает kill-switch/статические флаги; A/B и таргетинг — по
требованию заказчика (шов `FeatureFlags` интерфейс с реализациями `backend` / `unleash`).

### §5. Deep links: App Links / Universal Links + `.well-known` в инфре

#### 5.1. Факты

**Android App Links** [✓ developer.android.com/training/app-links/verify-android-applinks]:
`https://<host>/.well-known/assetlinks.json`, HTTPS, `Content-Type: application/json`, без
редиректов, публично; `intent-filter android:autoVerify="true"` с `<data android:scheme="https"
android:host="…"/>`; `sha256_cert_fingerprints` — массив: **и upload-ключ, и ключ Play App Signing,
и релизный ключ RuStore/direct** (`05` T-18: «две подписи одного приложения»); проверка:
`adb shell pm get-app-links <pkg>`, `adb shell pm verify-app-links --re-verify <pkg>` (Android 12+),
`adb shell pm set-app-links --package <pkg> 0 all` (сброс), тест
`adb shell am start -W -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d "https://app.dev.acme.ru/todos/42" <pkg>`;
Android 15+ периодически ре-верифицирует (до 7 дней), ниже — только при установке/обновлении;
состояние `verified | legacy_failure | none | 1024+`; на Android 12+ пользователь может вручную
разрешить домены (`Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS`).

**iOS Universal Links** — `02` §6.6 (AASA без расширения, `appIDs: ["<TeamID>.<bundle>"]`,
`components` с `/`, `?`, `#`, `exclude`; CDN Apple забирает файл ≤ 24 ч, устройства обновляют ~раз
в неделю; `?mode=developer` для bypass CDN). Дополнительно из TN3155 [✓ developer.apple.com/documentation/technotes/tn3155-debugging-universal-links]:
редиректы (301/302) не поддерживаются; **каждый конкретный сабдомен требует своего AASA**
(`applinks:app.example.com` → файл на `app.example.com`; wildcard `*.example.com` матчится файлом
на корне); порт в `applinks` недопустим; сервер обязан отдавать файл **любому User-Agent и с любого IP**
(CDN Apple не публикует диапазоны); проверка на Mac — `sudo swcutil dl -d <domain>` и
`sudo swcutil verify -d <domain> -j <json> -u <url>`; на устройстве — Settings → Developer →
Associated Domains Development → Diagnostics; ссылка, вставленная в Notes и удержанная, показывает
«Open in <App>». Симулятор: `xcrun simctl openurl booted "https://app.dev.acme.ru/todos/42"` [~].

**Passkeys/Credential Manager** (если когда-нибудь включат — §1.3d): те же файлы; в `assetlinks.json`
дополнительная relation `delegate_permission/common.get_login_creds` [~], в AASA — сервис
`webcredentials` [~].

#### 5.2. Кто отдаёт `.well-known` в инфре — предложение для `project-infra-template-compose`

Traefik v3 статических файлов не отдаёт [~ community.traefik.io]; в шаблоне уже есть контейнер
`landing` (`nginx:1.27-alpine` на корне `${DOMAIN}`, priority 1, «после раскатки фронта … можно
удалить» [✓ `compose/landing.yml`]) — вешать `.well-known` на него нельзя (он временный). Нужен
**отдельный always-on сервис `wellknown`** с роутером высокого приоритета на путь
`/.well-known/` для **каждого хоста, где живут ссылки** (обычно `${DOMAIN}` и `app.${DOMAIN}`;
`api.${DOMAIN}` — не нужно, если в ссылках его нет).

```yaml
# ЧЕРНОВИК · compose/wellknown.yml (always-on, как landing)
services:
  wellknown:
    image: nginx:1.27-alpine
    container_name: wellknown
    restart: unless-stopped
    volumes:
      - ./configs/wellknown/nginx.conf:/etc/nginx/conf.d/default.conf:ro
      - ./configs/wellknown/root:/usr/share/nginx/html:ro     # rendered из *.tpl
    networks: [proxy]
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://127.0.0.1/.well-known/assetlinks.json"]
      interval: 30s
      timeout: 5s
      retries: 3
    labels:
      traefik.enable: "true"
      traefik.docker.network: proxy
      # Один роутер на все хосты со ссылками; PathPrefix обязан побеждать роутеры фронта/лендинга.
      traefik.http.routers.wellknown.rule: (Host(`${DOMAIN}`) || Host(`app.${DOMAIN}`)) && PathPrefix(`/.well-known/`)
      traefik.http.routers.wellknown.priority: "1000"
      traefik.http.routers.wellknown.entrypoints: websecure
      traefik.http.routers.wellknown.tls: "true"
      traefik.http.routers.wellknown.tls.certresolver: letsencrypt
      # crowdsec@file — да (публичный роут); rate-limit — да; secure-headers глобально уже на websecure.
      traefik.http.routers.wellknown.middlewares: crowdsec@file,rate-limit@file
      traefik.http.services.wellknown.loadbalancer.server.port: "80"
```

```nginx
# ЧЕРНОВИК · configs/wellknown/nginx.conf
server {
  listen 80;
  root /usr/share/nginx/html;
  # AASA — без расширения; отдаём как JSON явно (Apple принимает application/json).
  location = /.well-known/apple-app-site-association { default_type application/json; add_header Cache-Control "public, max-age=3600"; }
  location = /.well-known/assetlinks.json            { default_type application/json; add_header Cache-Control "public, max-age=3600"; }
  location /.well-known/ { try_files $uri =404; }
  location / { return 404; }
}
```

```json
// ЧЕРНОВИК · configs/wellknown/root/.well-known/assetlinks.json.tpl — рендерится render-configs.sh (envsubst) из .env
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "${MOBILE_ANDROID_APP_ID}",
    "sha256_cert_fingerprints": ["${MOBILE_ANDROID_SHA256_UPLOAD}", "${MOBILE_ANDROID_SHA256_PLAY}", "${MOBILE_ANDROID_SHA256_RUSTORE}"]
  }
}]
```

```json
// ЧЕРНОВИК · configs/wellknown/root/.well-known/apple-app-site-association.tpl
{
  "applinks": {
    "details": [{
      "appIDs": ["${MOBILE_IOS_TEAM_ID}.${MOBILE_IOS_BUNDLE_ID}"],
      "components": [
        { "/": "/todos/*", "comment": "детали задачи" },
        { "/": "/invite/*", "?": { "code": "?*" } },
        { "/": "/auth/*", "exclude": true, "comment": "веб-логин остаётся в браузере" }
      ]
    }]
  },
  "webcredentials": { "apps": ["${MOBILE_IOS_TEAM_ID}.${MOBILE_IOS_BUNDLE_ID}"] }
}
```

`.env.example` (`<org>/infra`): секция `# ───── profile mobile (deep links) ─────` с
`MOBILE_ANDROID_APP_ID`, `MOBILE_ANDROID_SHA256_*` (не секрет — публичные отпечатки), `MOBILE_IOS_TEAM_ID`,
`MOBILE_IOS_BUNDLE_ID`; per env свои значения (`tech.stit.acme.dev` на dev-VPS, `tech.stit.acme` на prod).
`envsubst` в `render-configs.sh` уже обходит все `configs/**/*.tpl` [✓ `scripts/render-configs.sh`]
— новых механизмов не нужно; `make validate` должен проверять, что `${VAR}` объявлены в `.env.example`.

**INFRA-HANDOFF (формат бэкенда, `infra-handoff` skill):**

```
=== INFRA HANDOFF · передай инфра-админу ===
WHAT   compose/wellknown.yml + configs/wellknown/{nginx.conf,root/.well-known/*.tpl}; env MOBILE_ANDROID_APP_ID,
       MOBILE_ANDROID_SHA256_UPLOAD|PLAY|RUSTORE, MOBILE_IOS_TEAM_ID, MOBILE_IOS_BUNDLE_ID (required при profile mobile)
WHY    App Links / Universal Links мобильного приложения; без файлов ссылки открываются в браузере
ACTION добавить сервис в always-on (или profile mobile), заполнить env на КАЖДОМ VPS-среде (у dev/staging/prod
       свои app-id и отпечатки), make ship; проверить: curl -sI https://app.<domain>/.well-known/apple-app-site-association
       → 200, content-type application/json, без 301/302; curl с случайным -A тоже 200
WHO    проект (<org>/infra)
DECIDE хосты со ссылками: только ${DOMAIN} или ещё app.${DOMAIN}? (AASA нужен на каждом конкретном сабдомене)
RISK   Apple CDN кэширует до 24 ч, устройства ~неделю; ошибка в отпечатке = App Links «verified: false» до переустановки
===
```

#### 5.3. Маппинг ссылок → навигация (обе платформы)

`docs/stack/deep-links.md` (kind) держит таблицу `pattern → screen/route → params → guard`;
`/scaffold` генерирует Android `intent-filter` (host из `DEEP_LINK_HOST`, `pathPattern`), Navigation 3
парсер `Uri → NavKey`, iOS `.onOpenURL`/`onContinueUserActivity(NSUserActivityTypeBrowsingWeb)` → `Route`,
плюс custom-scheme fallback `acme://todos/42` (для писем/QR, где Universal Links не сработают —
TN3155: переход по ссылке на том же домене внутри Safari остаётся в браузере). Правила: (1) ссылка
никогда не несёт секретов (только id/одноразовые коды с TTL); (2) deep link при незалогиненном
пользователе → сохранить «pending route» → после логина продолжить; (3) каждая запись таблицы имеет
тест-кейс `adb shell am start … -d` / `simctl openurl` в `docs/stack/deep-links.md § Generates`
(checklist для `/release`); (4) паритет с SPA: маршрут `/todos/:id` в FSD-карте `pages.path` = путь
в AASA `components`.

### §6. Push — контракт с бэкендом

Провайдеры, доступность в РФ, relay-риски, ntfy — **`05` §2 (не дублирую)**. Здесь — контракт.

#### 6.1. Регистрация устройства (черновик OpenAPI 3.1, домен `device` в go-backend-template — B-18)

```yaml
paths:
  /api/v1/devices:
    put:                                  # идемпотентная регистрация: ключ (provider, token)
      operationId: registerDevice
      security: [{ Bearer: [] }, { Cookie: [] }]
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: "#/components/schemas/DeviceRegisterInputBody" }
      responses:
        "200":
          content: { application/json: { schema: { $ref: "#/components/schemas/DeviceOutputBody" } } }
    get:
      operationId: listDevices            # «мои устройства» (для экрана сессий/уведомлений)
      security: [{ Bearer: [] }, { Cookie: [] }]
      responses:
        "200":
          content: { application/json: { schema: { $ref: "#/components/schemas/DeviceListOutputBody" } } }
  /api/v1/devices/{id}:
    delete:
      operationId: unregisterDevice       # logout / отключение уведомлений
      security: [{ Bearer: [] }, { Cookie: [] }]
      parameters: [{ in: path, name: id, required: true, schema: { type: string } }]
      responses: { "204": { description: No Content } }
components:
  schemas:
    DeviceRegisterInputBody:
      type: object
      required: [provider, token, platform, app_version, locale]
      properties:
        provider:    { type: string, enum: [fcm, apns, rustore, hms, ntfy] }
        token:       { type: string, maxLength: 4096 }
        platform:    { type: string, enum: [android, ios] }
        app_version: { type: string, example: "1.4.2+1004002" }
        os_version:  { type: string, example: "Android 16" }
        device_model:{ type: string, example: "Pixel 8" }
        locale:      { type: string, example: "ru-RU" }
        timezone:    { type: string, example: "Europe/Moscow" }
        installation_id: { type: string, format: uuid, description: "стабильный id установки (генерируется на первом запуске; не ADID)" }
        sandbox:     { type: boolean, description: "APNs sandbox (debug-сборки) / FCM dev-проект" }
        push_enabled:{ type: boolean, description: "разрешение на уведомления в системе" }
    DeviceView:
      type: object
      required: [id, provider, platform, app_version, created_at, last_seen_at]
      properties:
        id: { type: string }
        provider: { type: string }
        platform: { type: string }
        app_version: { type: string }
        device_model: { type: string }
        created_at: { type: string, format: date-time }
        last_seen_at: { type: string, format: date-time }
        revoked_at: { type: string, format: date-time }
    DeviceOutputBody:     { type: object, required: [data], properties: { data: { $ref: "#/components/schemas/DeviceView" } } }
    DeviceListOutputBody: { type: object, required: [data, total], properties: { data: { type: array, items: { $ref: "#/components/schemas/DeviceView" } }, total: { type: integer, format: int64 } } }
```

Семантика на бэкенде (домен `device`, таблица `devices(id, user_id, installation_id, provider, token,
platform, app_version, os_version, device_model, locale, timezone, sandbox, push_enabled, created_at,
last_seen_at, revoked_at)`, `UNIQUE(provider, token)`): `PUT` — upsert по `(provider, token)`;
если тот же `installation_id` пришёл с новым токеном того же провайдера — старый помечается
`revoked_at` (ротация токена: FCM `onNewToken`, APNs токен может меняться при восстановлении из
бэкапа [~]); при logout клиент делает `DELETE`; при `401` на refresh-reuse бэкенд отзывает
устройства сессии (связь `devices.session_family_id` — опц.). Приложение регистрирует **все**
доступные провайдеры (`05` §2.5) и повторяет `PUT` при старте не чаще раза в сутки или при
изменении `app_version`/`locale`/`push_enabled`.

#### 6.2. Отправка на бэкенде — `golibinfra/pushkit` (B-1 в `05` §9.3, здесь — контракт порта)

```go
// ЧЕРНОВИК · golibinfra/pushkit
type Provider string // fcm | apns | rustore | hms | ntfy

type Message struct {
    // Локализация на клиенте по ключу: сервер знает только ключ и аргументы.
    TitleKey, BodyKey string
    Args             map[string]string
    // Fallback-тексты (для провайдеров/версий, где клиент не умеет ключи) — на языке devices.locale.
    Title, Body string
    DeepLink    string            // https://app.<domain>/todos/42 — клиент открывает через свой роутер
    Category    string            // группа/канал: "orders", "chat" → Android channel_id / APNs category, thread-id
    CollapseKey string            // дедупликация "одно логическое уведомление"
    Priority    Priority          // Normal | High (Android priority / APNs apns-priority 5|10)
    TTL         time.Duration     // ≤ 4 недели (FCM/RuStore default), APNs apns-expiration
    Silent      bool              // data-only / content-available
    Badge       *int
    Data        map[string]string // ≤ 4 КиБ суммарно
}

type Sender interface {
    Send(ctx context.Context, p Provider, token string, m Message) (Result, error)
}
// Result.Status: delivered_to_provider | invalid_token (→ revoke) | retry_after(d) | error
```

Адаптеры и их «сырой» формат (проверено 2026-09-17):

| провайдер | endpoint / auth | payload-конвенция шаблона | мёртвый токен |
|---|---|---|---|
| FCM HTTP v1 | `POST https://fcm.googleapis.com/v1/projects/{project}/messages:send`, OAuth2 Bearer из service-account [✓ firebase.google.com/docs/reference/fcm/rest/v1/projects.messages] | **data-message** (`message.data{title_key, body_key, args_json, deep_link, category, notification_id}` + `android.priority: HIGH` для видимых, `android.ttl`, `android.collapse_key`); notification-объект **не** используем — «Data messages … handled by the client app» [✓ …/customize-messages/set-message-type]; лимит **4096 байт** для обоих типов [✓ там же]; TTL по умолчанию 4 недели [✓ …/setting-message-lifespan]; high priority доставляется и в Doze [✓ …/android-message-priority] | `UNREGISTERED` / `NOT_FOUND` → `revoked_at` [✓ error codes] |
| APNs | `POST https://api.push.apple.com/3/device/{token}` (sandbox `api.sandbox.push.apple.com`), HTTP/2, JWT ES256 из `.p8` (обновлять ~раз в час), заголовки `apns-topic`=bundle id, `apns-push-type: alert|background`, `apns-priority`, `apns-expiration`, `apns-collapse-id` [✓ `02` §6.7, `05` §2.1] | `aps.alert{title-loc-key, title-loc-args, loc-key, loc-args}` — **локализация на клиенте по ключу из `Localizable.strings`** [✓ developer.apple.com/…/generating-a-remote-notification: Table 2]; `aps.category`, `aps.thread-id`, `aps.interruption-level`; custom-ключи — **рядом с `aps`**, не внутри («APNs ignores custom keys» внутри `aps`) [✓]; silent — `content-available: 1` без alert/badge/sound, `apns-push-type: background`, **не гарантируется и троттлится** [✓ `02` §6.7]; лимит 4096 байт (VoIP 5120) [✓] | `410 Gone` (`Unregistered`) → `revoked_at` |
| RuStore Push | `POST https://vkpns.rustore.ru/v1/projects/{project_id}/messages:send`, `Authorization: Bearer {service-token}`, форма как FCM v1: `message{token, data{}, notification{title,body,image}, android{ttl, notification{channel_id, click_action, click_action_type, …}}}`, ≤ 4096 байт, TTL default 4 недели; **без `title` в `notification` SDK не покажет пуш** [✓ rustore.ru/help/en/sdk/push-notifications/send-push-notifications] | как FCM: `data{…}` + для «гарантированного показа» дублировать `notification.title/body` на языке устройства (RuStore SDK показывает сам) | `NOT_FOUND` (404) — «expired/invalid push token» → `revoked_at`; `429` → `Retry-After` |
| HMS / ntfy | `05` §2 | — | — |

Конвенции (для `docs/stack/push.md` мобильного репо и `docs/stack/messaging.md` бэкенда):

1. **Data-first**: сервер шлёт ключи + аргументы + deep link; **клиент** строит текст из своих
   `strings.xml`/`xcstrings` (актуальные переводы едут с приложением, не с бэкендом). Исключение —
   iOS без Notification Service Extension показывает `alert` **системой**, поэтому там `loc-key`
   обязателен (клиентская локализация встроена в APNs), а для Android RuStore — `notification.title`
   как fallback, т. к. приложение может быть убито и `data` не обработан [~].
2. **`notification_id`** (uuid) в data — дедупликация на клиенте, если пользователь получил одно
   уведомление через два транспорта (`05` §2.5).
3. **Deep link — всегда https-URL из таблицы `deep-links.md`** (не custom scheme): единый роутер.
4. **Silent push** — только для «обновить бейдж/кэш», с бюджетом: APNs — «несколько в час» [~],
   Android — high-priority data-message тратит квоту при злоупотреблении (FCM deprioritization [~]).
   Не строить на нём бизнес-логику; fallback — polling при открытии.
5. **Permission-флоу**: запрос разрешения (`POST_NOTIFICATIONS` на Android 13+, `UNUserNotificationCenter.requestAuthorization`)
   — **не на первом экране**, а в момент ценности (после первого действия), с pre-prompt; отказ →
   `push_enabled=false` в `PUT /devices`, бэкенд не шлёт (метрика `push_skipped_total{reason}`).
6. **Очередь**: отправка через outbox домена `notification` → `pushkit.Sender` в drain-job; ретраи
   по `Retry-After`; poison → dead (как `outboxkit`) — `05` §2.5.

### §7. Observability

#### 7.1. Крэши/ошибки: Sentry SDK + self-host приёмник

Факты (2026-09-17): `io.sentry:sentry-android` **8.57.0** (2026-09-16) [✓ Maven Central], **minSdk 21**
(«Bumping `minSdk` level to 21» в 8.0; AAR `minCompileSdk` пиннят к 21) [✓ sentry-java CHANGELOG];
Gradle-плагин `io.sentry.android.gradle` **6.22.0** (2026-09-10; AGP ≥ 7): `autoUploadProguardMapping`
(default true), `uploadNativeSymbols`, `includeSourceContext`, `autoInstallation`, `tracingInstrumentation`,
`url` для self-hosted, креды `SENTRY_AUTH_TOKEN`/`org`/`projectName`; mapping заливается на
`assemble<Variant>` через встроенный sentry-cli [✓ docs.sentry.io/platforms/android/configuration/gradle].
`sentry-cocoa` **9.28.0** (2026-09-10) [✓ GitHub releases], **iOS 15.0+** («If you need to support
iOS 11-14, use Sentry Cocoa 8.x») [✓ docs.sentry.io/platforms/apple/guides/ios/manual-setup.md]; dSYM —
`sentry-cli debug-files upload --include-sources <DWARF_DSYM_FOLDER_PATH>` (build phase / Fastlane
`sentry_debug_files_upload`), требует `DEBUG_INFORMATION_FORMAT = dwarf-with-dsym`; env
`SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_PROJECT`, `SENTRY_URL` (self-hosted) [✓ …/apple/dsym].
Формат release по умолчанию на обеих платформах — `package@version+build` (`my.project.name@2.3.12+1234`)
[✓ …/android/configuration/releases; …/apple/configuration/releases]. `sentry-cli` 3.8.0 — лицензия
**FSL-1.1 (MIT Future License)** [✓ raw LICENSE], SDK — MIT. `sentry-kotlin-multiplatform` 0.27.0
(2026-06-03; пара cocoa 8.58.2) — обёртка над sentry-android/cocoa для KMP-shared [✓ Maven Central, README].

Приёмник: GlitchTip (MIT [✓ gitlab LICENSE]) — Sentry-совместимый DSN, но символикация ProGuard/dSYM
**слабее** Sentry (MR !84 2021 г.; обзоры 2026: «self-hosted Sentry is needed if you need native crash
symbolication») [~]; полный Sentry self-host — тяжёлый (`05` §4.2). Решение владельца O-4 в `05`.

Политика шаблона:
- DSN пустой → SDK **не инициализируется** (как SPA); `environment=APP_ENV`, `release=RELEASE`,
  `dist=<build>`; `sendDefaultPii=false` (default); `beforeSend` — скраббинг (`Authorization`,
  `Cookie`, email в breadcrumbs), `attachScreenshot=false`; согласие пользователя на крэш-репорты —
  экран первого запуска (152-ФЗ, `05` §5) → `Sentry.init` только после согласия (флаг в DataStore).
- `make release`: Android — mapping через Gradle-плагин (Linux ✅); iOS — dSYM с Mac build-host
  (`02`, `05` §6.5) — **символы до публикации**, иначе крэши нечитаемы (`05` T).
- Корреляция: `X-Request-ID` каждого запроса — breadcrumb `http` + tag `request_id` на событии
  ошибки API; бэкенд пишет `request_id` в каждую строку лога — `grep request_id` на VPS.
- Контекст пользователя — `user.id` только (без email), выставляется после логина, сбрасывается при
  logout.

#### 7.2. Логи приложения — политика «запрет `Log`/`print` напрямую» (аналог forbidigo)

| платформа | инструмент | правило гейта |
|---|---|---|
| Android / KMP-shared | **Kermit 2.2.0** (Touchlab, Apache-2.0, 2026-09-08 [✓ GitHub/Maven]) — KMP-логгер с `LogWriter`; Timber 5.0.1 (**2021-08-13**, без релизов 5 лет [✓ Maven]) — не брать | detekt `ForbiddenImport`: `android.util.Log`, `java.util.logging`, `println`/`print` (`ForbiddenMethodCall`), `System.out` (`01` §8.1); в release — `LogWriter` только Sentry-breadcrumbs (уровень ≥ warn), никаких тел запросов |
| iOS | `os.Logger` (`Logger(subsystem:category:)`), privacy-аннотации `\(token, privacy: .private)` | SwiftLint `custom_rules`: запрет `print(`/`NSLog(`/`debugPrint(` вне `Logging`-модуля (`02` §7.4); `os_log` с `.private` для PII |
| общее | структурные поля: `request_id`, `screen`, `user_id`; **никогда**: токены, cookie, тела ответов, ПДн | правило `.claude/rules/observability.md` |

#### 7.3. OpenTelemetry на мобильных — статус и решение

- **opentelemetry-android 1.7.0** (2026-09-04): агент поверх OTel Java SDK; артефакт
  `io.opentelemetry.android:android-agent` 1.7.0, но `core` — **1.7.0-alpha**; minSdk 23 (< 26 —
  desugaring); инструментации: activity/fragment lifecycle, ANR, crash, network change, slow rendering,
  startup, sessions; экспорт OTLP/HTTP [✓ github README; Maven Central]. Статус на opentelemetry.io:
  Android как отдельной строки **нет** (Java — stable/stable/stable), Kotlin — Development [✓ opentelemetry.io/docs/languages/].
- **opentelemetry-swift 2.5.1** (2026-08-06): «Tracing and Baggage are considered stable», логи —
  beta, metrics по устаревшей спеке; OTLP: «only GRPC is production ready, HTTP is still
  experimental» [✓ github README]; на opentelemetry.io Swift: Traces Stable, Metrics/Logs Development [✓].
- В экосистеме коллектора нет (`05` §4.1: `telemetry` kit экспортирует OTLP/HTTP в коллектор
  «уровня проекта»). Значит **OTel в шаблон — не дефолт**; шов `TelemetrySink` (`sentry` | `otlp`), env
  `OTEL_EXPORTER_OTLP_ENDPOINT` — та же переменная, что у Go-китов; включать, когда у проекта
  появится коллектор и нужен сквозной trace mobile→backend (`traceparent` W3C уже понимает
  `httpkit.OTel` middleware [✓ `telemetry/README.md`: propagator TraceContext+Baggage]).

#### 7.4. Метрики качества как гейты релиза

| метрика | источник | порог (дефолт шаблона, `docs/stack/observability.md`) | где проверяется |
|---|---|---|---|
| crash-free sessions / users | Sentry release health (`autoSessionTracking=true` [✓ options]) | ≥ 99.5 % за 48 ч на `internal`/TestFlight перед промоушеном | `/release` STOP-2 → STORE-HANDOFF checklist (`05` §7.4) |
| ANR rate (Android) | Sentry ANR (`anrEnabled`), Play Console Android vitals (порог Google «bad behaviour» 0.47 % [~]) | < 0.47 % | там же |
| холодный старт | Sentry app-start spans / Macrobenchmark (`01` §7.10, нужен девайс) | p50 < 1.5 с, p90 < 3 с [?] | в `make check` — нет (нужно устройство); в релиз-чеклисте |
| размер артефакта | `bundletool`/`app-size` | AAB ≤ N МБ, рост ≤ +10 % за релиз | `make check` (аналог `size-limit` SPA) |
| hang rate (iOS) | Sentry `enableAppHangTracking` | < 1 % | релиз-чеклист |

### §8. i18n и токены дизайна

#### 8.1. i18n (ru/en)

Платформенные механики — `01` §3.5 (Android: `res/values[-ru]/strings.xml`, `plurals`,
`generateLocaleConfig = true` + `resources.properties`, per-app language через
`AppCompatDelegate.setApplicationLocales`) и `02` §3.4/§6.16 (iOS: `Localizable.xcstrings` с
plural-вариациями, по каталогу на пакет с UI, `BUILD_ONLY_KNOWN_LOCALIZATIONS`). Что добавляет
контракт с бэкендом и паритет с SPA:

| вопрос | решение |
|---|---|
| Язык API-ответов | бэкенд **не локализует** пользовательские тексты: ошибки — стабильный `code` (§2.6) → клиентский ключ `error.<CODE>`; push — ключи + args (§6.2). `Accept-Language` шлём (`ru-RU,ru;q=0.9,en;q=0.8`) — пригодится для e-mail/документов, которые генерирует бэкенд [?] |
| Словарь ключей | единый **реестр ключей** в strategy (`mobile/<surface>/i18n.md` — см. §9) или в `docs/stack/i18n.md`: `key · ru · en · plural? · где`; из него `/scaffold` генерирует `strings.xml` и `.xcstrings` (генерация в обе стороны — только «strategy → код»; правки в IDE запрещены правилом) [?] |
| Плюрализация | Android `<plurals>` (`one/few/many/other` для ru), iOS `.xcstrings` variations; **тест-кейс на 1/2/5** обязателен (`02` §6.16) |
| Форматы | даты/числа/валюта — только через `java.text`/`kotlinx-datetime` + `NumberFormat` и `Foundation.FormatStyle`; никаких ручных `"%d.%m.%Y"` — правило детекта (detekt `ForbiddenMethodCall` на `SimpleDateFormat` с литералом [?]) |
| KMP-shared | строки **не** в shared (нативный UI → нативные ресурсы); shared возвращает ключи/коды; `moko-resources` 0.27.0 [✓ GitHub tags] — только если появится общий UI (CMP), в дефолте не брать |
| SPA-паритет | Lingui runtime-only каталоги (`messages = { hello: 'Привет, мир' }`) — ключи те же, что в реестре; ключ = стабильный идентификатор, не английский текст [?] |

#### 8.2. Токены дизайна — единый источник или паритет по чек-листу

Что уже есть: SPA — `src/app/styles/index.css`: `@theme` (`--text-body-sm/md`, `--text-heading-lg`,
`--radius-control`, `--font-sans`) + `@theme inline` семантические цвета
`app-bg/app-fg/surface-muted/divider/muted/danger/action-bg/action-fg`, значения по `data-scheme=light|dark`
[✓ файл]; Android — `core:designsystem` с `MaterialTheme` + `CompositionLocal` (`01` §3.4); iOS —
`Theme{colors,metrics,typography}` в `Environment` (`02` §3.5).

Варианты:

| вариант | как | плюсы | минусы |
|---|---|---|---|
| **A. Единый источник — DTCG JSON + Style Dictionary** | `tokens/*.json` в strategy (или в `ui-lib`), **Style Dictionary 5.5.3** (2026-09-06, Apache-2.0, Node ≥ 22) [✓ npm, GitHub] генерирует: `android/resources` (colors/dimens XML), `compose/object` (Kotlin `object` с `val`), `ios-swift/class.swift` / `enum.swift` / `any.swift`, плюс CSS (`css/variables`) для SPA [✓ styledictionary.com/reference/hooks/formats/predefined]; DTCG (`$value/$type/$description`) поддерживается с v4, полная спека 2025.10 — в работе для v5 [✓ styledictionary.com/info/dtcg] | один источник правды для трёх поверхностей; переименование токена — один diff; дизайнер (Figma Tokens/Tokens Studio) → JSON | Node-тулчейн в мобильном репо (или генерация в `ui-lib` с публикацией артефактов); тёмная тема — через `modes`/двойные наборы (SD не имеет встроенных «схем», делается двумя сборками или `outputReferences`); Material `ColorScheme` и Liquid Glass живут своей жизнью — маппинг семантики → платформенные роли всё равно ручной |
| **B. Независимые токены + чек-лист паритета** | каждая платформа держит свой семантический набор с **одинаковыми именами** (`app.bg`, `surface.muted`, `divider`, `muted`, `danger`, `action.bg/fg`, `radius.control`, `text.body-sm/md`, `text.heading-lg`); `docs/stack/styling.md` — таблица имён; `make check` — скрипт сверяет список имён между `index.css` SPA (sibling), `Color.kt`/`Spacing.kt` и `Theme.swift` | ноль новых тулчейнов; платформенные значения могут осознанно отличаться (Dynamic Type, Material) | значения дрейфуют; паритет только по именам |

**Рекомендация: B сейчас, с формой, совместимой с A** — имена токенов в DTCG-нотации
(`color.app.bg`, `dimension.radius.control`, `typography.body.md`), значения — в
`docs/stack/styling.md § Tokens` как таблица `token · light · dark · android · ios`, и `make check`
проверяет **имена** (не значения) против `../frontend/src/app/styles/index.css` при наличии соседа.
Переход на A — когда появится второй проект с тем же брендом или `ui-lib` начнёт публиковать токены
(тогда SD-сборка живёт в `ui-lib`, мобильный репо только `make gen` подтягивает сгенерированные
`Tokens.kt`/`Tokens.swift`). Это forward-совместимо и не добавляет Node в мобильный гейт.

**Тема** (аналог `features/theme`): `system | light | dark`, хранится в DataStore/UserDefaults
(`app.theme`), применяется на корне (`AppTheme(darkTheme = …)` / `.preferredColorScheme(…)`);
`dynamicColor=false` по умолчанию (`01` §3.4); Liquid Glass — только `.glassEffect()` на «важных
функциональных» элементах (`02` §3.5).

**A11y-минимум** (гейт): контентные описания/`accessibilityLabel` у интерактивных элементов
(Android Lint `ContentDescription`, `02` §3.6 + `XCUIVoiceOverService`), контраст токенов
light/dark ≥ 4.5:1 (проверка — скрипт по таблице токенов), Dynamic Type/`fontScale` 1.3 не ломает
экраны (screenshot-варианты), touch-target ≥ 48dp/44pt.

### §9. Слой-мост в strategy для `type: mobile`

#### 9.1. Как устроены существующие мосты (прочитано, 2026-09-17)

| слой | партиция | форма | источники | потребитель | что читает `/scaffold` |
|---|---|---|---|---|---|
| `interface/<surface>/` | по surface (любого типа) | 7 плоских md-таблиц: who · stage · situation · zone · block · ux-substrate · data-items; ссылки только внутри поверхности [✓ `interface/README.md`] | бизнес | screens / bot-flow / fsd | — |
| `screens/<surface>/` (web/admin/desktop/embed) | по surface | 4 kind: `flow` (desktop-screen ↔ N mobile-screens — **viewport-адаптив веба**, «не нативное приложение» [✓ `flow.md`]), `screen` (route, shell, slot-fills, states `loading/empty/error/permission-denied/offline/not-found`), `block` (дерево `ui:`/`layout:` по манифесту `@stit/ui`), `shell` (слоты + responsive) | interface | «wireframe вручную», FSD-карта | — (screens → код напрямую не мапится) |
| `fsd/<surface>/` (SPA) | по surface | 8 плоских таблиц: entities · features · pages · widgets · app · shared-ui · shared-api · shared-lib; inside-out; зеркала (`widgets.pages`), pages-first, cross-feature запрещён; **без `src/`-путей и библиотек** [✓ `fsd/README.md`] | interface (situation, data-items), screens (screen, block), services/domains (агрегаты) | `<org>/frontend` `/scaffold` + `/de-template` (только stable) | `kind + slug` → `src/<layer>/<slice>/` |
| `telegram-bot-flow/<surface>/` (bot) | по surface, **встаёт на место screens** | `features/<feature>/{dialog,window,use-case}.md` + корневые `command.md`, `notification.md`; язык контракта — английские структурные идентификаторы (`states-group` PascalCase, `state` UPPER_SNAKE, `operation`, widget `id`), русский контент; мост к домену через `use-case` бота [✓ `telegram-bot-flow/README.md`] | interface, services/domains (use-case, event) | `<org>/<bot>` | dialog/window/use-case → StatesGroup/окна/getter'ы |

Скиллы: `fsd-deep-dive` (одна точка входа, режимы A/B/C, `AskUserQuestion`, state-ветка в
`entrypoint/state/projects/<slug>/fsd/<surface>/`, чек-лист `draft → stable`, каскадное удаление,
зеркала пересчитываются автоматически) и `fsd-audit` (structure / source-refs / layer-refs /
semantics / maturity; не правит) [✓ SKILL.md обоих]. Реестр `project/surface.md`: `type: mobile`,
`lives-at` = app-id/store [✓].

Пробел [✓ бриф + `fsd/README.md`: «для `type: bot` слоя нет … для `type: api` — тоже»]: для
`type: mobile` `/fsd-deep-dive` **отказывает** (кандидаты только `web|admin|desktop|embed`), `screens/`
описывает viewport веба и компоненты `@stit/ui` (которых в нативном приложении нет), т. е. мобильная
поверхность сегодня получает только `interface/`.

#### 9.2. Годится ли FSD как есть для мобильного

| слой FSD | Android (NiA-стиль `feature:*`/`core:*`, `01` §3.2) | iOS (SwiftPM `Packages/Features/*`, `Packages/Core/*`, `02` §3.3) | KMP-shared (`03` §6.3) | вердикт |
|---|---|---|---|---|
| `app` | `:app` (Application, MainActivity, NavDisplay, DI-корень) | app-таргет (`App.swift`, `AppRouter`) | — | ✅ 1:1 |
| `pages` | **нет прямого аналога**: экран = `feature:<x>` (Screen + ViewModel + Route/NavKey) | экран = `Features/<X>/View+Model+Route` | — | ⚠️ «page» и «feature» **сливаются в screen-модуль**; FSD-докa сама допускает «pages … also known as screens or activities» [✓ feature-sliced.design/docs/reference/layers] |
| `widgets` | переиспользуемые композиты в `core:ui` или отдельный `feature`-модуль без навигации | `Packages/UI` | — | ⚠️ инвариант «≥2 page-референса» сохраняется; но модуль на каждый widget — дорого (цена Android-модуля, `01` §3.2) → widgets живут пакетами/папками **внутри** `core:ui`, не модулями |
| `features` | `feature:<x>` = экран **или** сценарий (login, create-todo) — NiA: «feature module = screen or closely related screens» [✓ `01` §3.1] | `Packages/Features/<X>` | usecase/оркестрация в `shared/feature/<x>` (без UI) | ⚠️ у NiA feature ≈ FSD page+feature вместе; **cross-feature запрещён физически** (нет Gradle-зависимости) — сильнее Steiger |
| `entities` | `core:model` (модели) + `core:data` (репозитории SSOT) + `core:network` (DTO) | `Packages/Core/{Domain,Data,API}` | `shared/entity/<x>` (модель + репозиторий + DTO-маппер) | ✅ но в FSD entity = модель+API+презентер; на мобильных презентер (`<Entity>Card`) — в `core:ui`, данные — в `core:data` |
| `shared` | `core:common`, `core:designsystem`, `core:network` (клиент), `core:datastore`, `core:testing`, `core:analytics` | `Packages/Core/{DesignSystem,Networking,Storage,Logging}` | `shared/core/*` | ✅ 1:1 (shared-ui ↔ designsystem, shared-api ↔ network+mock, shared-lib ↔ common/logging) |

Что теряется/ломает канон: (1) **`pages` как отдельный слой не нужен** — на мобильных нет URL как
первичной адресации; адресация — навигационный ключ (`NavKey`/`Route`) + deep link; (2) `flow`
(desktop↔mobile-декомпозиция) бессмысленен — мобильный экран **и есть** единственный viewport, зато
появляется **навигационный граф** (стек, табы, модальные шторки, back-поведение), которого в FSD нет;
(3) `block`-деревья на `ui:<Name>` из манифеста `@stit/ui` нерелевантны (нативные компоненты
Material 3 / SwiftUI), но семантика «состояний экрана» (`loading/empty/error/permission-denied/offline/not-found`)
переносится дословно; (4) появляются сущности, которых нет ни в FSD, ни в bot-flow: **permissions**
(камера, геолокация, уведомления — с обоснованием для Data safety/Nutrition Label, `05` §9.4),
**push-notification** как входная точка (аналог `notification.md` бота), **deep-link** как входная
точка (аналог `command.md` бота), **offline-политика** экрана (что показываем без сети, что
кэшируем), **background-задачи** (sync, upload).

Сообщество: FSD для мобильных обсуждается в основном в React Native
(«Slices, Not Layers … app > screens > widgets > features > entities > shared» —
medium.com/@devanshtiwari365 [~]); для нативных Kotlin/Swift канон другой — Google
modularization guide («feature ↔ feature через медиатор и общий data-модуль», «prefer pure
Kotlin modules») [✓ `01` §3.1] и JET/Qonto-стиль модулей для iOS (`02` §2.7/§5.5). Официальная
FSD-документация про нативные мобильные ничего не говорит [✓ feature-sliced.design/docs/reference/layers].
Вывод: **не переносить FSD-имена в мобильный слой**; переносить **принципы** (вертикальные срезы ×
горизонтальные слои, public API, запрет cross-feature, pages-first→«screen-first», источник в
аналитике) в родных понятиях мобильных платформ — ровно так поступил bot-flow (dialog/window вместо
screen/block).

#### 9.3. Предложение: слой `mobile/<surface>/` (аналог FSD-карты / bot-flow)

Имя слоя — `mobile` (не `fsd`, не `screens`); встаёт **на место `screens/<surface>/`** для
`type: mobile` (как bot-flow для `type: bot`), FSD-карты для мобильной поверхности нет. Читает
`interface/<surface>/` (situation, data-items, who, block) и `services/*/domains/` (агрегаты,
use-case, event). Потребитель — форк `mobile-template` (`<org>/mobile`): `/scaffold` и
`/de-template` разворачивают stable-записи в **три места** — `shared/` (KMP, модель+репозиторий+usecase),
`androidApp/feature/<x>` и `iosApp/Packages/Features/<X>` (или в два, если платформа одна).

Структура папки поверхности:

```
docs/projects/<project-slug>/mobile/<surface>/
├── entities.md        # kind entity      — вещь (данные) с источником в домене / data-items (как в fsd)
├── screens.md         # kind screen      — экран: ключ навигации, ситуации, состояния, guard, offline-политика
├── flows.md           # kind flow        — навигационный сценарий: стек/табы/модалки, переходы между screens (аналог dialog.transitions)
├── features.md        # kind feature     — модуль feature: экраны + сценарии + entities; правило «нет feature→feature»
├── shell.md           # kind shell       — каркас: корневая навигация (tabs/drawer), auth-gate, глобальные overlay (аналог shell)
├── entry-points.md    # kind entry-point — deep links, push-категории, app shortcuts/widgets, share-in (аналог command.md + notification.md)
├── permissions.md     # kind permission  — разрешение · ситуация-обоснование · текст объяснения · обязательное? (вход в Data safety / Nutrition Label / privacy manifest)
├── shared-api.md      # как в fsd: покрытие entities контрактом + сценарии данных (empty|populated|authenticated|error) для mock
└── shared-lib.md      # как в fsd: локали, offline/sync-политика, наблюдаемость (что логируем/что нельзя)
```

Колонки (в стиле канона interface: `slug · name · description · … · status`):

| файл | колонки | связи |
|---|---|---|
| `entities.md` | `slug · name · description · source · поля · связи · cache` (`cache`: `none | memory | disk` — offline-политика сущности) · `status` | `source` → `services/<svc>/domains/<ctx>/<slug>` или `interface/<surface>/data-items.md` |
| `screens.md` | `slug · name · description · route-key` (PascalCase, структурный идентификатор → `NavKey`/`Route`) · `situations` · `who` · `guard` (`public | authenticated | <who>`) · `entities` · `states` (подмножество `loading/empty/error/permission-denied/offline/not-found`) · `offline` (`read-only | full | blocked`) · `entry` (`[deep-link:<slug>, push:<slug>]` \| `—`) · `status` | `situations` → interface; `entities` → entities.md; `entry` → entry-points.md |
| `flows.md` | `slug · name · description · start` (screen) · `screens` (упорядоченно) · `presentation` (`push | modal | sheet | tab`) · `transitions` (`[{from, to, trigger}]`) · `who` · `status` | screens.md |
| `features.md` | `slug · name · description · situation · screens · entities · результат · shared` (`true` — оркестрация в KMP-shared) · `status` | interface.situation; screens.md; entities.md; **feature → feature запрещён** |
| `shell.md` | `slug · name · description · root` (`tabs | stack | drawer`) · `tabs` (`[screen]`) · `auth-gate` (screen логина) · `overlays` (`[offline-banner, update-required, maintenance]`) · `status` | screens.md |
| `entry-points.md` | `slug · name · description · kind` (`deep-link | push | shortcut | share-in`) · `pattern` (для deep-link: `https://<host>/todos/{id}`; для push: `category`) · `target` (screen) · `payload` (что из ссылки/пуша уезжает в route-key) · `trigger` (для push: `event:<ctx>.<Event>`) · `idempotency` (push) · `who` · `status` | screens.md; `services/*/domains/<ctx>/events/`; **прямой вход для `docs/stack/deep-links.md` и `push.md`** |
| `permissions.md` | `slug · name · description · permission` (`camera | location-when-in-use | notifications | photos | contacts | …`) · `situation` · `rationale` (текст объяснения, ru) · `required` (`true|false`) · `data-safety` (`collected | not-collected`, куда уходит) · `status` | interface.situation; вход в `docs/stack/compliance.md` (`05` §8.6) |

Инварианты слоя (для `/mobile-audit`): screen без situation — error; feature → feature — error;
entry-point без target — error; permission без situation — error (нельзя просить «на всякий
случай»); deep-link `pattern` должен совпадать с `pages.path` SPA-поверхности того же проекта, если
такая есть (hint, `/cross-audit`, угол `mobile-web`); push `trigger` → существующий event домена
(`/cross-audit`, угол `mobile-domain`); `shared-api` покрывает все entities.

Порядок проектирования inside-out: **entities → screens → flows → features → shell → entry-points →
permissions → shared-api → shared-lib**.

Что читает `/scaffold` мобильного репо из stable-записей:

| запись | генерирует |
|---|---|
| `entities.<slug>` | `shared/src/commonMain/kotlin/<pkg>/entity/<slug>/{Model,Repository,Dto+mapper}.kt` + фикстуры `mock/fixtures/populated/<slug>.json` + тест; iOS — если KMP: экспорт через фасад; если нативно — `Packages/Core/Domain/<Slug>.swift` |
| `screens.<slug>` | Android `feature/<feature>/<Slug>Screen.kt` + `<Slug>ViewModel.kt` + `<Slug>UiState.kt` (`states` → sealed-варианты) + `<Slug>Route` (`route-key`) + Robolectric/screenshot-тесты на каждое состояние; iOS `Packages/Features/<Feature>/<Slug>View.swift` + `<Slug>Model.swift` + `Route.<slug>` + snapshot-тесты |
| `flows.<slug>` | Navigation 3 back-stack сценарий / `NavigationStack` path + тест переходов |
| `features.<slug>` | Gradle-модуль `feature:<slug>` (convention-plugin) / SwiftPM target; `shared: true` → `shared/feature/<slug>/UseCase.kt` |
| `shell` | `app/Navigation.kt` (tabs), `RootView.swift`, auth-gate, overlay-компоненты |
| `entry-points` | `intent-filter` + `applinks` entitlement + AASA/assetlinks-шаблоны в handoff (§5), `NotificationCategory`/channel_id, роутер deep link → route-key |
| `permissions` | манифест/`Info.plist` usage-strings (ru/en из `rationale`), экран pre-prompt, строки для Data safety/Nutrition Label (`docs/stack/compliance.md`) |
| `shared-api` | `MockTransport`-сценарии (§3) |
| `shared-lib` | локали (`strings.xml`/`.xcstrings` заготовки), политика логирования |

#### 9.4. Минимальный канон-PR в `business-strategy-template`

| файл | изменение |
|---|---|
| `docs/projects/_template/mobile/README.md` | канон слоя (по образцу `telegram-bot-flow/README.md`): назначение, воркфлоу `interface → mobile → код (<org>/mobile)`, 9 kind, язык контракта (`route-key` PascalCase, `slug` kebab, русский контент), инварианты, связи с interface/services, «Связь с кодом» (read-only для `mobile-template`) |
| `docs/projects/_template/mobile/_template/{entities,screens,flows,features,shell,entry-points,permissions,shared-api,shared-lib}.md` | формы файлов с «Критерии / Что НЕ / Тест-критерий / Колонки» (форма ячеек — как interface) |
| `docs/projects/_template/project/surface.md` | абзац: `type: mobile` → папка композиции `mobile/<surface>/` (вместо `screens/`) |
| `docs/projects/_template/screens/README.md`, `fsd/README.md`, `interface/README.md` | одна строка в каждом: «для `type: mobile` — слой `mobile/<surface>/`» (forward-only, без «раньше») |
| `.claude/skills/mobile-deep-dive/SKILL.md` | клон `fsd-deep-dive` со своими kind/inside-out/чек-листом; пикер поверхностей — только `type: mobile`; state-ветка `entrypoint/state/projects/<slug>/mobile/<surface>/` |
| `.claude/skills/mobile-audit/SKILL.md` | клон `fsd-audit`: structure / source-refs / layer-refs / semantics (нет `src/`-путей, Gradle-модулей, SwiftPM-имён) / maturity |
| `.claude/skills/cross-audit/SKILL.md` | углы `mobile-web` (deep-link ↔ `pages.path`), `mobile-domain` (push trigger ↔ event), `mobile-service` |
| `.claude/skills/project-audit/SKILL.md` | «поверхность `type: mobile` без `mobile/<surface>/`» — hint |
| `docs/README.md § Что docs отдаёт соседям` | строка про `<org>/mobile` |

Оценка объёма: ~1200 строк md, без кода; ревьюер — владелец strategy-канона.

#### 9.5. Путь без PR (шаблон обязан работать со старым форком strategy)

`/init-service` мобильного репо резолвит `<strategy-path>/docs/projects/<slug>/mobile/<surface>/`;
если папки нет — три ветки: (1) в strategy-форке нет канона `mobile/` (старый форк) → скилл
**печатает next-step**: «перенеси канон: скопируй `docs/reference/strategy-mobile-canon/` из
mobile-template в `<org>/strategy/docs/projects/_template/mobile/` и заведи поверхность» — т. е.
мобильный шаблон **несёт копию канона и форм** в `docs/reference/strategy-mobile-canon/` (read-only
зеркало, с пометкой «источник правды — business-strategy-template после PR»); (2) канон есть, папки
поверхности нет → «спроектируй через `/mobile-deep-dive` в `<org>/strategy`»; (3) соседа нет вообще
→ deградация как у SPA (`docs/stack/service.md` без `strategy-path` → скиллы работают в режиме
«без карты», `/scaffold` принимает kind+slug из аргументов с пометкой «без источника» и
warning'ом). Гейт `/de-template` — как у SPA: только stable-записи; при отсутствии слоя —
reference-фичи (`auth`, `todos`, `settings`, `profile`) разворачиваются как учебные и удаляются.

---

## Рекомендации для шаблона

### 10.1. Контракты — сводная таблица решений

| область | решение по умолчанию | шов/опция | cross-repo |
|---|---|---|---|
| Auth | Bearer access (15m, в памяти) + refresh (90d mobile, SecureStore, ротация, reuse → отзыв цепочки) в JSON; `X-Client`; сессии `/api/auth/sessions` | `AUTH_MODE=cookie` (переходный), `AUTH_BIOMETRIC_UNLOCK`, SSO через `oidckit` BFF-mobile, passkeys | backend B-1…B-11, golibinfra (opc.) |
| Спек | `../backend/api/openapi.yaml` (закоммичен бэкендом через `make openapi` + `gen-check`) → копия `api/openapi.yaml` + `api/openapi.lock` | Gitea Release asset | backend B-12/B-13/B-14/B-15 |
| Кодоген | iOS — Swift OpenAPI Generator (плагин, `types+client`); KMP/Android — openapi-generator `kotlin` `multiplatform` **models** (+ Ktorfit-интерфейсы или свой Ktor-клиент) | — | — |
| HTTP-клиент | Ktor 3.6.0 (OkHttp-engine Android 5.5.0, Darwin iOS — если shared; иначе URLSession через swift-openapi-urlsession) | — | — |
| Ошибки/повторы | `APIError{code,…}` → типизированный `ApiError`; `X-Request-ID` на каждый запрос; `Idempotency-Key` на каждую мутацию; ретраи 408/429/502/503/504 + `Retry-After`; POST только с ключом | — | backend B-9 (store), infra: `HTTP_CORS_HEADERS` += `Idempotency-Key` для SPA |
| Совместимость | аддитивная эволюция, `ignoreUnknownKeys`, unknown-enum, `oasdiff breaking` в гейте бэкенда, `GET /api/app/requirements` kill-switch | — | backend B-17 + правило `api-compat` |
| Mock | фикстуры JSON per сценарий + `MockTransport` (Ktor `MockEngine` / swift-openapi `ClientTransport`), `demo`-flavor/схема, debug-панель, те же фейки в тестах | внешний MSW-сервер из фикстур SPA | strategy: `shared-api.md` сценарии |
| Среды | 3 flavor/схемы (`dev`/`staging`/`prod`) + `demo`; build-time/release-time/runtime; env-контракт §4.2 | debug-переключатель среды (dev only) | infra: домены сред |
| Deep links | App Links + Universal Links per env, custom-scheme fallback; таблица `deep-links.md`; `.well-known` — сервис `wellknown` в `<org>/infra` | — | infra (compose + env), strategy (`entry-points.md`) |
| Push | `PUT /api/v1/devices` (provider,token,platform,…), data-first payload с ключами локализации, `notification_id`, deep link; `pushkit` в golibinfra; outbox | ntfy для команды | backend B-18, golibinfra pushkit |
| Observability | sentry-android 8.57 / sentry-cocoa 9.28 с пустым DSN по умолчанию, mapping/dSYM в `/release`, Kermit/os.Logger + запрет `Log`/`print`, `X-Request-ID` в breadcrumbs; OTel — шов | GlitchTip vs Sentry self-host (O-4 в `05`) | infra: хост приёмника |
| i18n | ru/en нативными ресурсами; реестр ключей; ошибки/пуши по ключу | moko-resources при CMP | strategy: `shared-lib.md` |
| Токены | независимые токены с DTCG-именами + проверка паритета имён с SPA | Style Dictionary в `ui-lib` | ui-lib (позже) |
| Strategy-мост | слой `mobile/<surface>/` (9 kind) + `/mobile-deep-dive`, `/mobile-audit`; копия канона в мобильном репо | — | strategy PR (§9.4) |

### 10.2. `docs/stack/` — kinds мобильного репо (черновики форм с секцией `Generates`)

Восемь kinds бэкенда/SPA не покрывают мобильную специфику; предложение — **10 kinds** (объединяет
`02` §7.10 и `05` §8.6, добавляет kinds этого потока):

| kind | файл | описывает | Generates |
|---|---|---|---|
| `service` | `service.md` | app-slug, `applicationId`/`bundleId` per env, `project-slug` / `strategy-path` / `surface` / `backend-path` (sibling), платформы (`android`, `ios`, `shared`), deployment targets | `settings.gradle.kts` имена модулей, `Config/Base.xcconfig`, `AppConfig` идентификаторы |
| `runtime` | `runtime.md` | Kotlin/AGP/Gradle/JDK, Swift/Xcode/iOS, KMP-таргеты, concurrency-политика | `gradle/libs.versions.toml` версии, `Package.swift` tools-version, `.tool-versions` |
| `config` | `config.md` | **build-time / release-time / runtime** таблицы (§4.2), где живёт каждый ключ, release-checklist | flavors/xcconfig, `AppConfig.kt/.swift`, `.gitignore`, `local.properties.example`, `Config/Local.xcconfig.example` |
| `api` | `api.md` | спек (путь соседа, версия/коммит), кодоген (генератор, опции, что коммитится), клиент (Ktor/URLSession), auth-режим, ошибки/ретраи/idempotency, совместимость (`min backend version`) | `api/openapi.yaml`+`.lock`, `openapi-generator-config.yaml`, `make gen`/`gen-check`, `ApiClient`, `AuthMiddleware` |
| `auth` | `auth.md` | режим (`bearer|cookie`), TTL, SecureStore-политика (Keystore/Keychain атрибуты, биометрия, backup), сессии, SSO/passkeys (опц.), pinning-политика | `SecureStore`, `SessionStore`, `AuthInterceptor`, `network_security_config.xml`, `NSPinnedDomains`, `dataExtractionRules` |
| `push` | `push.md` | провайдеры per flavor, регистрация (`/api/v1/devices`), payload-конвенции, категории/каналы, permission-флоу | `PushTransport` реализации, `NotificationChannel`s / `UNNotificationCategory`, `FirebaseMessagingService`/`RuStorePushService`/`AppDelegate` hooks |
| `deep-links` | `deep-links.md` | хосты per env, таблица `pattern → route-key → guard → test`, custom scheme | `intent-filter`, `applinks:` entitlement, `DeepLinkRouter`, INFRA-HANDOFF-шаблон (`assetlinks`/AASA значения) |
| `observability` | `observability.md` | Sentry (DSN per env, приёмник, PII-политика, согласие), логгер и запреты, `X-Request-ID`, OTel-шов, гейты качества (crash-free/ANR/старт/размер) | `Sentry.init`, `LogWriter`s, detekt/SwiftLint запреты, `sentry { }` Gradle-блок, dSYM build phase |
| `styling` | `styling.md` | токены (DTCG-имена, light/dark, паритет с SPA), тема, Material/Liquid Glass политика, a11y-минимум | `core:designsystem` (`Color.kt`, `Type.kt`, `Spacing.kt`, `Theme.kt`), `Packages/Core/DesignSystem/Theme.swift`, скрипт паритета |
| `distribution` | `distribution.md` | сторы, подпись, треки, версионирование, STOP-1/STOP-2 (`05` §7–8; `signing`/`compliance` — подсекции или отдельные kinds по решению потока 08) | flavors стора, GPP/RuStore-скрипты, `versionCode` из тега, чек-листы `/release` |

`dependencies` (как у SPA/бэкенда) — обязательный одиннадцатый; `i18n` — секция `shared-lib`/`styling`,
не отдельный kind [?]. Frontmatter/`status: draft|stable`/валидатор — как у бэкенда
(`make validate-stack-docs`).

Черновик формы `docs/stack/_templates/api.md` мобильного репо:

```markdown
---
id: stack.api
kind: api
name: api
status: draft
---

# api

## Спек
<Откуда: `<backend-path>/api/openapi.yaml` (sibling, дефолт `../backend`); копия `api/openapi.yaml`;
`api/openapi.lock` (backend commit/tag/sha256); минимальная версия бэкенда, против которой собрано приложение.>

## Кодоген
| платформа | генератор · версия | что генерируется | что коммитится | команда |
|---|---|---|---|---|
| iOS | swift-openapi-generator 1.13.1 (build plugin) | types + client | ничего (плагин) / всё (CLI) | `swift build` |
| KMP/Android | openapi-generator 7.25.0 `kotlin` `multiplatform` | models (+ apis?) | `shared/.../api/generated/**` | `make gen` |

## Клиент
<Ktor 3.x: engine per платформа, плагины (Auth bearer, ContentNegotiation, HttpRequestRetry, Logging), таймауты,
`X-Request-ID`, `Idempotency-Key`, `User-Agent`/`X-Client`. Или URLSession через swift-openapi-urlsession.>

## Auth scheme
| механизм | детали |
|---|---|
| режим | `bearer` (дефолт) \| `cookie` (переходный) |
| access | в памяти, TTL с сервера (`expires_in`) |
| refresh | SecureStore; ротация; single-flight; 401 REUSED/EXPIRED → logout |

## Ошибки и повторы
<`APIError.code` → `ApiError` sealed; ретраи; `Retry-After`; идемпотентность; 422 `errors[]` → поля форм.>

## Совместимость
<`ignoreUnknownKeys`, unknown-enum политика; `GET /api/app/requirements` (min-version, флаги); правила аддитивной эволюции.>

## Mock
<Сценарии `empty|populated|authenticated|error`; фикстуры `mock/fixtures/<scenario>/`; `MockTransport`; `demo`-flavor; debug-панель.>

## Generates
- `api/openapi.yaml`, `api/openapi.lock`, `Makefile: gen/gen-check`
- `shared/.../api/{ApiClient,AuthInterceptor,ApiError}.kt`, `openapi-generator-config.yaml`, `Packages/API/`
- `mock/fixtures/**`, `MockTransport` (Kotlin/Swift)

## Открытые вопросы
- _что_
```

### 10.3. Правила `.claude/rules/` (path-scoped) — предлагаемый набор этого потока

| правило | `paths:` | суть (terse-инварианты) |
|---|---|---|
| `auth-and-secrets.md` | `shared/**/auth/**`, `**/SecureStore*`, `**/network_security_config.xml`, `**/*.entitlements`, `**/Info.plist`, `**/*.xcconfig*`, `local.properties*` | токены только через `SecureStore` (Keystore/Keychain, `ThisDeviceOnly`, `kSecAttrSynchronizable=false`); access только в памяти; refresh не логировать; `allowBackup`/`dataExtractionRules` исключают DataStore токенов; секретов в артефакте нет — deny-лист файлов (`*.jks`, `*.p8`, `*.p12`, `google-services.json` для prod — только через env/gitignore); `usesCleartextTraffic`/`NSAllowsArbitraryLoads` запрещены в release; pinning — только по `docs/stack/auth.md § Pinning`; биометрия — только как разблокировка ключа |
| `api-contract.md` | `api/**`, `shared/**/api/**`, `**/Packages/API/**`, `openapi-generator-config.yaml`, `Makefile` | сгенерированное не редактируется руками (`settings.json` deny на `**/generated/**`); `make gen` после смены `api/openapi.yaml`, `make gen-check` в гейте; каждая мутация — `Idempotency-Key`; каждый запрос — `X-Request-ID`; `ignoreUnknownKeys=true`; неизвестный enum не роняет парсинг; `ApiError.code` — единственный ключ для ветвления; никаких `/api/v1` в клиенте, только пути из спека; версия бэкенда в `api/openapi.lock` |
| `observability.md` | `**/logging/**`, `**/Sentry*`, `**/observability/**`, `build.gradle.kts`, `**/*.xcconfig` | запрет `android.util.Log`/`println`/`print`/`NSLog` (detekt/SwiftLint); PII-скраббинг в `beforeSend`; DSN пустой → SDK выключен; символы (`mapping.txt`/dSYM) — часть `make release`, не «потом»; `X-Request-ID` в breadcrumb; согласие до `init` |
| `mock-and-fixtures.md` | `mock/**`, `**/src/demo/**`, `**/Packages/Mock/**` | сценарии только из `docs/stack/api.md § Mock` (и `mobile/<surface>/shared-api.md`); фикстуры в формате ответов API (envelope включён); те же фейки в тестах; `demo` не ходит в сеть (тест: `MockTransport` + запрет `HttpClient` без mock-engine в `demo`) |
| `deep-links-and-push.md` | `**/AndroidManifest.xml`, `**/*.entitlements`, `**/DeepLink*`, `**/Push*`, `docs/stack/{deep-links,push}.md` | каждая ссылка/категория — строка таблицы в `docs/stack`; изменение хостов/отпечатков → INFRA-HANDOFF; payload — только ключи и id, без ПДн; permission-запрос — не на первом экране |
| `config-and-envs.md` | `gradle/env.*.properties`, `Config/*.xcconfig`, `**/AppConfig*`, `.env*` | три категории конфига; секрет в build config — запрещено; `APP_ENV` на экране «О приложении»; runtime-значения только через `requirements`; новый ключ → строка в `docs/stack/config.md` тем же PR |
| (наследуются) `forward-only.md`, `comments.md`, `tests-pipeline.md`, `env-local.md` (переработанный под `local.properties`/`*.xcconfig.local`), плюс правила потоков 01/02/05 (`kmp-shared`, `android-ui`, `ios-ui`, `signing-and-secrets`, `store-compliance`, `privacy-and-law`, `release-flow`) | | |

### 10.4. Гейт (вклад этого потока в `make check`)

`make gen-check` (спек-копия ↔ сосед, сгенерированные модели ↔ спек, `swift build` пакета `API` на
Linux) · `make mock-check` (все фикстуры парсятся сгенерированными моделями — тест в `shared/jvmTest`)
· `make tokens-check` (паритет имён токенов с SPA-соседом, контраст) · `make i18n-check`
(ключи ru == ключи en; плюралы 1/2/5 присутствуют; нет hardcoded-строк — Android Lint `HardcodedText`,
SwiftLint `nslocalizedstring_key`) · `make deeplinks-check` (таблица `deep-links.md` ↔ манифест/entitlements)
· detekt/SwiftLint запреты логгеров.

---

## Ловушки

| # | ловушка | источник / проверка | обход |
|---|---|---|---|
| T-1 | Cookie-контракт бэкенда: session-cookie **без `Max-Age`** → `HTTPCookieStorage` считает их process-local, после перезапуска приложения сессии нет | `authgate/cookie_test.go` (тест требует отсутствие Max-Age); Apple `HTTPCookieStorage` doc | bearer (B-1…B-3); в cookie-режиме — только in-memory сессия |
| T-2 | `openapi-generator kotlin` превращает `apiKey in cookie` в **query-параметр** `access_token` — токен в URL/логах Traefik | [изм] §2.4 | не использовать `setApiKey`; bearer-схема; `ApiClient` генератора не подключать |
| T-3 | `in: cookie` параметры (`refresh_token`) генерируются как тип `Cookie` (вся `http.Cookie` Go) и не поддерживаются Swift OpenAPI Generator | [изм] дамп + `Supported-OpenAPI-features.md` | refresh в теле (B-3) |
| T-4 | huma добавляет `$schema` в каждую body-схему; отключить через `SchemasPath=""` **нельзя** (нет guard'а) | `huma@v2.39.1/transforms.go` | `c.CreateHooks = nil` через `httpkit.HumaConfigGroup` или фильтр в `make openapi` (B-13) |
| T-5 | `data` списков — `[array, null]` → `List<T>?`/`[T]?` в клиентах | дамп §2.2 | `orEmpty()`/B-14 |
| T-6 | `APIError.code` отсутствует в OpenAPI `ErrorModel` → генераторы теряют код ошибки | дамп + `httpkit/error.go` | B-15; до этого — ручной парсинг `code` |
| T-7 | Idempotency в шаблоне бэкенда не включена (нет `IdempotencyStore` в графе) — `Idempotency-Key` молча игнорируется | `httpkit/module.go`, `cmd/api/main.go` | B-9 |
| T-8 | SPA-шаблон и бэкенд-шаблон **не совместимы по контракту** (`/api/v1/*` + `app_session` + CSRF vs `/api/auth/*` + `access_token`/`refresh_token` без CSRF) — мобильный шаблон не должен копировать SPA-мок как «контракт» | grep обоих репо | источник правды — `api/openapi.yaml` бэкенда; cross-repo фикс SPA (см. ниже) |
| T-9 | `EncryptedSharedPreferences`/`MasterKey` — deprecated (1.1.0); гайды/LLM-память до 2025 года предлагают их | Android releases page | Keystore + DataStore (или Tink) |
| T-10 | Ktor Darwin **отключает** `NSURLSession` cookie storage (`setHTTPCookieStorage(null)`) — системные cookie iOS в Ktor-клиенте не работают, только `HttpCookies` | `DarwinSession.kt` | не смешивать URLSession-cookie и Ktor; в bearer-режиме неважно |
| T-11 | AppAuth-Android последний релиз 2021-12 (0.11.1); AppAuth-iOS 3.0.0 требует iOS 15 | Maven/GitHub | для SSO — BFF-mobile схема или свой PKCE-флоу |
| T-12 | Pinning на Let's Encrypt: leaf ротируется ~60–90 дней; пин промежуточных LE тоже меняется плановыми ротациями | MASTG-KNOW-0015, Android NSC doc | pin на корни ISRG + backup + expiration + kill-switch; дефолт — выключено |
| T-13 | AASA: Apple CDN до 24 ч, устройства ~неделя; каждый конкретный сабдомен — свой файл; редиректы не поддерживаются; должен отдаваться любому User-Agent/IP | TN3155 | сервис `wellknown` с priority 1000; `?mode=developer` в dev-сборках |
| T-14 | `assetlinks.json` с одним отпечатком (upload-ключ) → на Play-сборках App Links не верифицируются | Android doc + `05` T-18 | все отпечатки (upload/Play/RuStore) в массиве |
| T-15 | Android 15+ ре-верифицирует App Links раз до 7 дней, ниже — только при (пере)установке; для теста — `pm verify-app-links --re-verify` | Android doc | чек-лист в `deep-links.md` |
| T-16 | RuStore не покажет пуш без `notification.title`; FCM data-only не показывается системой при убитом приложении на некоторых OEM [~] | RuStore doc | для RuStore — дублировать title/body; для FCM — data + high priority + `notification` fallback по решению проекта |
| T-17 | APNs `content-available` (silent) троттлится и не гарантируется; custom-ключи внутри `aps` игнорируются | Apple doc | silent только для бейджа/кэша; custom-ключи рядом с `aps` |
| T-18 | Sentry: без mapping/dSYM крэши нечитаемы; dSYM только с Mac; GlitchTip символицирует хуже | Sentry docs, `05` §4.2 | символы — шаг `make release` до публикации |
| T-19 | `sentry-cli` — лицензия FSL-1.1 (не OSI-open-source до MIT-конверсии через 2 года) | raw LICENSE | ок для использования как инструмента; фиксировать в `dependencies.md` |
| T-20 | Unleash server — AGPL-3.0 | raw LICENSE | self-host ок, но open-core проекта заказчика должен это знать; дефолт — `requirements.flags` |
| T-21 | Play Integrity — 10 000 запросов/день по умолчанию, требует GMS; на RuStore-устройствах без GMS не работает | Android doc | опция за флагом, не гейт |
| T-22 | OpenAPI 3.1 в openapi-generator — «beta»; при странностях использовать `openapi-3.0.yaml` (huma downgrade) | [изм] | `make gen` может генерировать из 3.0-варианта как fallback |
| T-23 | `HTTP_CORS_HEADERS` не содержит `Idempotency-Key` — SPA не сможет слать ключ (preflight) | `cors.go` | infra env для web; мобильных не касается |
| T-24 | `JWT_SECRET` общий у нескольких сервисов без `iss`/`aud` → токен одного сервиса валиден в другом | `authnkit` README | B-7 |
| T-25 | `X-Request-ID` > 128 байт или не-ASCII отбрасывается сервером (свой UUIDv7) — корреляция теряется молча | `httpkit/middleware.go` | UUID строкой, ≤ 36 символов |

---

## Cross-repo изменения (сводно, с приоритетом)

### go-backend-template (`<org>/backend`)

| # | что | файлы | приоритет |
|---|---|---|---|
| B-1 | `tokenFromRequest`: `Authorization: Bearer` → fallback cookie | `internal/platform/authgate/jwt.go` (+ тест) | P0 |
| B-2 | security scheme `Bearer` + `Security: {Cookie}, {Bearer}` на защищённых операциях | `authgate/module.go`, `*/delivery/http/handler.go` | P0 |
| B-3 | `X-Client` header; refresh/logout с телом `{refresh_token}`; ответ `200 TokenPairBody` для mobile | `auth/delivery/http/{dto,handler}.go` | P0 |
| B-4 | reuse-detect → отзыв семейства сессий (`family_id`) | `auth/usecase.go`, `infra/postgres/queries/refresh_sessions.sql`, миграция | P1 |
| B-5 | сессии: `client/device_name/app_version/last_used_at`, `GET/DELETE /api/auth/sessions[/{jti}]` | домен `auth` | P1 |
| B-6 | mobile refresh TTL через `Sign(ctx, claims, ttl)` в адаптере | `auth/module/adapters.go` | P1 |
| B-7 | `WithIssuer`/`WithAudience` в `authgate/authn.go` | — | P1 |
| B-8 | `Retry-After` на `AUTH_ACCOUNT_LOCKED` | `auth/delivery/http/handler.go` | P2 |
| B-9 | провайдить `httpkit.IdempotencyStore` (`cachekit.NewIdempotencyStore`) | `cmd/api/main.go` | P1 |
| B-10 | `docs/stack/_templates/auth.md`: секция Clients (web cookie / mobile bearer), TTL | — | P0 |
| B-11 | e2e pytest для mobile-ветки auth | `test/e2e/` | P1 |
| B-12 | `cmd/openapi` + `make openapi` (пишет `api/openapi.yaml` без БД, версия из git-тега) + `make gen-check` (дрейф спека) + ассет `openapi.yaml` в `/release` | `cmd/openapi/main.go`, `Makefile`, `scripts/release.sh`, `.claude/rules` (запрет ручной правки `api/openapi.yaml`) | P0 |
| B-13 | убрать `$schema` из схем для клиентов (`c.CreateHooks = nil` через `HumaConfigGroup`) или фильтр в `make openapi` | `internal/platform/httpserver`/`cmd/api` | P1 |
| B-14 | envelope: `LoginOutputBody.user` → `data`; списки — non-nullable массив | `auth`/`user` delivery | P2 |
| B-15 | `code` в error-схеме OpenAPI (`httpkit.APIError` как `ErrorModel`) | `golibinfra/httpkit` или huma-config бэкенда | P1 |
| B-16 | cursor-пагинация для лент (по необходимости) | — | P3 |
| B-17 | `GET /api/app/requirements` (min-version/kill-switch/флаги/ссылки) на `flagkit` + `APP_*` env | новый домен `app` или platform-роут; `.env.example`; `docs/stack/config.md` | P0 |
| B-18 | домен `device` (`PUT/GET/DELETE /api/v1/devices`), таблица, отзыв мёртвых токенов; домен `notification` с outbox → `pushkit` | reference-домен | P1 |
| B-19 | правило `.claude/rules/api-compat.md` + `oasdiff breaking` в `gen-check` | — | P1 |

### golibinfra

| # | что | приоритет |
|---|---|---|
| G-1 | `pushkit`: `Sender` + адаптеры FCM v1 / APNs (JWT ES256, HTTP/2) / RuStore (+ HMS/ntfy опц.), `Result.invalid_token`, метрики `push_send_total{provider,result}` (`05` B-1) | P1 |
| G-2 | `httpkit`: экспорт `APIError` в OpenAPI `ErrorModel` (с `code`) — B-15 | P1 |
| G-3 | `oidckit`: `BearerExtractor` (resource-server) и/или `RegisterMobileRoutes` + one-time exchange (BFF-mobile) — только при выборе SSO | P3 |
| G-4 | (опц.) `authnkit`: `JWTConfig.RefreshTTLMobile` — если не хочется TTL в адаптере | P3 |
| G-5 | (опц.) `integritykit` (Play Integrity / App Attest verify), `webauthnkit` (passkeys) | P3 |

### project-infra-template-compose (`<org>/infra`)

| # | что | приоритет |
|---|---|---|
| I-1 | сервис `wellknown` (compose + nginx + `.well-known/*.tpl`), env `MOBILE_*` в `.env.example` (секция `mobile`), `make validate` coverage, INFRA-HANDOFF-шаблон §5.2 | P0 |
| I-2 | `HTTP_CORS_HEADERS` += `Idempotency-Key` для SPA (env бэкенда) | P2 |
| I-3 | `APP_MIN_VERSION_*`, `APP_STORE_URL_*`, `FEATURE_FLAGS` в env бэкенда (B-17) | P1 |
| I-4 | серверные push-креды в `.env` бэкенда: `FCM_SERVICE_ACCOUNT_JSON` (base64), `APNS_KEY_ID`/`APNS_TEAM_ID`/`APNS_P8` (base64), `RUSTORE_SERVICE_TOKEN`, `RUSTORE_PROJECT_ID` | P1 |
| I-5 | хост приёмника крэшей (GlitchTip) — `05` §9.2; `SENTRY_DSN` per env в handoff мобильного релиза | P1 |
| I-6 | публичные URL политики и удаления аккаунта (`05` §9.2) | P1 |

### business-strategy-template (`<org>/strategy`)

| # | что | приоритет |
|---|---|---|
| S-1 | канон слоя `mobile/<surface>/` (9 kind) + формы `_template/` (§9.3–9.4) | P0 |
| S-2 | скиллы `/mobile-deep-dive`, `/mobile-audit`; углы `/cross-audit` (`mobile-web`, `mobile-domain`, `mobile-service`); hint в `/project-audit` | P0 |
| S-3 | правки README interface/screens/fsd/surface (одна строка «для `type: mobile` — `mobile/`») | P0 |

### corp-infra-template

| # | что | приоритет |
|---|---|---|
| C-1 | Authentik: провайдер с public client + PKCE + redirect regex для мобильных — только при SSO (§1.3c) | P3 |
| C-2 | (из `05` §9.1) ntfy-топик релизов, Vaultwarden-коллекция `mobile/<org>/<app>`, Gitea Releases как якорь артефактов | P1 |

### spa-frontend-template (побочная находка)

| # | что | приоритет |
|---|---|---|
| F-1 | привести reference-мок и `client.ts` к контракту бэкенда: пути `/api/auth/*`, `/api/users/me`, cookies `access_token`/`refresh_token`, убрать несуществующий `X-CSRF-Token`/`csrf_token` (или добавить CSRF на бэкенд осознанно), envelope `data` | P2 |
| F-2 | `docs/stack/_templates/api.md` SPA: ссылка на `../backend/api/openapi.yaml` как источник zod-схем (после B-12) | P2 |

---

## Риски и открытые вопросы владельцу

### Решения владельца (блокируют проектирование)

| ID | вопрос | дефолт в отчёте |
|---|---|---|
| O-1 | Принять bearer-контракт (B-1…B-3) в go-backend-template как канон для не-браузерных клиентов? Это меняет reference-домен `auth` и `docs/stack/auth.md` бэкенда | да, с сохранением cookie для web |
| O-2 | Спек как артефакт: закоммиченный `api/openapi.yaml` в бэкенде (`make openapi` + `gen-check`) — согласен ли с «сгенерированное лежит в репо»? | да (аналог `mocks/`, `sqlc db/`) |
| O-3 | Кодоген на Kotlin-стороне: генерировать только модели (свой Ktor-клиент) или брать `apis` генератора целиком? Зависит от spike 06 (компилируется ли `apis` без `supportingFiles`) | модели + свой клиент/Ktorfit |
| O-4 | Certificate pinning: выключен по умолчанию или включён на корни ISRG для всех проектов? Класс заказчиков (банки/госы) может требовать | выключен, шов + процедура |
| O-5 | SSO через Authentik для мобильных — нужен ли в базовом шаблоне (тянет Authentik на проектную сторону) или только extension? | extension |
| O-6 | Приёмник крэшей — GlitchTip (лёгкий, слабая символикация) vs self-hosted Sentry (16–32 ГБ RAM) — совпадает с O-4 потока 05 | GlitchTip + возможность DSN на Sentry |
| O-7 | Имя и место слоя-моста в strategy: `mobile/<surface>/` вместо `screens/` для `type: mobile` — принять канон 9 kind или сократить (например, без `permissions.md`)? | 9 kind |
| O-8 | Единый источник токенов дизайна (Style Dictionary в `ui-lib`) — заводить сейчас или отложить до второго бренд-проекта? | отложить, паритет имён |
| O-9 | Feature flags: достаточно `requirements.flags` + `flagkit` (рестарт контейнера при смене) или нужен живой сервис (Unleash AGPL / Flagsmith)? | достаточно |
| O-10 | `/api/app/requirements` — отдельный домен `app` в бэкенде или platform-роут вне доменов (нарушает «делivery только в доменах»)? | домен `app` (reference) |

### Технические риски

| ID | риск | митигация |
|---|---|---|
| R-1 | OpenAPI 3.1 в openapi-generator — beta; регрессии между версиями генератора | пиннить версию jar; fallback на `openapi-3.0.yaml`; `gen-check` |
| R-2 | Дрейф спека: бэкенд меняет API, старые версии приложения в проде месяцами | `oasdiff breaking` в гейте бэкенда (B-19), `requirements.min_version`, аддитивная политика |
| R-3 | Keystore на части устройств (StrongBox отсутствует, `KeyPermanentlyInvalidatedException` при смене биометрии) | fallback на TEE, обработка инвалидации = повторный логин |
| R-4 | Apple CDN/AASA-кэш и Play App Links верификация — «работает не у всех» | все отпечатки, `?mode=developer` в dev, чек-лист в релизе |
| R-5 | FCM политически хрупок; RuStore SDK 8.0.0-rc01 (rc) | мульти-провайдер (`05`), `pushkit` с адаптерами, метрика по провайдерам |
| R-6 | Sentry SDK major-ы (8.x → 9.x cocoa) ломают API ежегодно; sentry-cli FSL | пиннить, отдельная строка в `dependencies.md` |
| R-7 | Слой `mobile/` в strategy без владельца канона зарастёт неконсистентностью | `/mobile-audit` в pre-commit strategy как `fsd-audit` |

### Что проверить дальше (эмпирика, поток 06 / spike)

- `openapi-generator kotlin multiplatform` с `--global-property models` (без `apis`/`supportingFiles`) и компиляция в `shared/` (Kotlin 2.4 / Ktor 3.6 / serialization 1.11) — компилируется ли сгенерированное без чужого `ApiClient`.
- Swift OpenAPI Generator build plugin на Linux против `openapi.yaml` бэкенда: `swift build` пакета `API` с `swift-openapi-urlsession` (Foundation на Linux) — время сборки, предупреждения по `Set-Cookie` header и `default` responses.
- Ktor `Auth` bearer-плагин + `MockEngine`: single-flight refresh под параллельными запросами (тест в `jvmTest`).
- Детерминизм `openapi-generator` (два прогона → нулевой diff) для `gen-check`.
- Keystore-`SecureStore` на эмуляторе без StrongBox: `setUserAuthenticationParameters` + `BiometricPrompt.CryptoObject` — на Robolectric не тестируется, нужен эмулятор (или чистый unit с fake `KeyStore`).

---

## Сводная таблица версий

| пакет / инструмент | версия | дата | лицензия | источник |
|---|---|---|---|---|
| go-backend-template: huma/v2 | 2.39.1 | 2026-07-29 | MIT [~] | `go.mod`; github releases |
| golibinfra | v1.1.0 (в go.mod бэкенда) | — | Apache-2.0 [~] | `go.mod` |
| OWASP MASVS | v2.1.0 | 2024-01-18 | CC-BY-SA [~] | github releases |
| OWASP MASTG | v2.0.0 | 2026-06-30 | CC-BY-SA [~] | github releases |
| RFC 8252 | — | 2017-10 | IETF | rfc-editor |
| androidx.security:security-crypto | 1.1.0 (**all APIs deprecated**) | 2025-07-30 | Apache-2.0 | Google Maven; releases page |
| androidx.datastore | 1.2.1 stable (1.3.0-alpha11) | — | Apache-2.0 | Google Maven |
| androidx.credentials | 1.6.0 stable (1.7.0-alpha03) | — | Apache-2.0 | Google Maven |
| com.google.crypto.tink:tink-android | 1.23.0 | 2026-07-09 | Apache-2.0 | Maven Central |
| com.google.android.play:integrity | 1.6.0 | — | Play Terms | Google Maven |
| com.google.firebase:firebase-messaging | 25.1.3 | 2026-09 | Apache-2.0 | Google Maven |
| ru.rustore.sdk:pushclient | 8.0.0-rc01 | 2026-09-02 | RuStore SDK license | artifactory-external.vkpartner.ru |
| com.squareup.okhttp3:okhttp / mockwebserver3 / okhttp-java-net-cookiejar | 5.5.0 | 2026-08-16 | Apache-2.0 | Maven Central; CHANGELOG |
| io.ktor:ktor-client-{core,okhttp,darwin,mock} | 3.6.0 | 2026-09-16 | Apache-2.0 | Maven Central |
| de.jensklingenberg.ktorfit:ktorfit-lib | 2.7.5 (Kotlin ≥ 2.2, KSP ≥ 2.0.2, Ktor 3.5.0) | 2026-06-07 | Apache-2.0 | Maven Central; CHANGELOG |
| org.jetbrains.kotlinx:kotlinx-serialization-json | 1.11.0 (1.12.0-RC) | 2026-09-04 (RC) | Apache-2.0 | Maven Central |
| com.squareup.retrofit2:retrofit | 3.0.0 | 2025-05-15 | Apache-2.0 | Maven Central |
| net.openid:appauth (Android) | 0.11.1 | 2021-12-22 | Apache-2.0 | Maven Central; GitHub tags |
| AppAuth-iOS | 3.0.0 (iOS 15+) | 2026-08-24 | Apache-2.0 | GitHub releases |
| apple/swift-openapi-generator | 1.13.1 | 2026-09-01 | Apache-2.0 | GitHub releases |
| apple/swift-openapi-runtime | 1.12.1 | 2026-09-02 | Apache-2.0 | GitHub tags |
| apple/swift-openapi-urlsession | 1.3.1 | 2026-06-23 | Apache-2.0 | GitHub releases |
| OpenAPITools/openapi-generator (cli jar) | 7.25.0 | 2026-08-24 | Apache-2.0 | GitHub releases; Maven Central |
| io.sentry:sentry-android | 8.57.0 (minSdk 21) | 2026-09-16 | MIT | Maven Central; sentry-java CHANGELOG |
| io.sentry.android.gradle (plugin) | 6.22.0 | 2026-09-10 | MIT | Maven Central; docs |
| getsentry/sentry-cocoa | 9.28.0 (iOS 15+) | 2026-09-10 | MIT | GitHub releases; docs |
| io.sentry:sentry-kotlin-multiplatform | 0.27.0 | 2026-06-03 | MIT | Maven Central |
| getsentry/sentry-cli | 3.8.0 | 2026-09 | FSL-1.1 (MIT future) | GitHub tags; LICENSE |
| GlitchTip | — | — | MIT | gitlab LICENSE |
| io.opentelemetry.android:android-agent | 1.7.0 (core 1.7.0-alpha) | 2026-09-04 | Apache-2.0 | Maven Central; GitHub |
| open-telemetry/opentelemetry-swift | 2.5.1 | 2026-08-06 | Apache-2.0 | GitHub releases |
| co.touchlab:kermit | 2.2.0 | 2026-09-08 | Apache-2.0 | Maven Central |
| com.jakewharton.timber:timber | 5.0.1 | 2021-08-13 | Apache-2.0 | Maven Central |
| Unleash server / unleash-android | v8.2.0 / 3.3.2 | 2026-09-08 / 2026-08-21 | **AGPL-3.0** / Apache-2.0 [~] | GitHub; Maven Central |
| Flagsmith | v2.273.0 | 2026-09-16 | BSD-3-Clause [~] | GitHub releases |
| style-dictionary | 5.5.3 (Node ≥ 22) | 2026-09-06 | Apache-2.0 | npm; GitHub |
| goauthentik/authentik | 2026.8.2 | 2026-09-09 | MIT (+ enterprise) | GitHub releases; LICENSE |
| binwiederhier/ntfy | v2.22.0 (в corp compose) | — | Apache-2.0 | corp compose; LICENSE |
| com.liftric:kvault | 1.12.0 | 2023-10-11 | MIT [~] | Maven Central |
| com.russhwolf:multiplatform-settings | 1.3.0 | 2024-11-29 | Apache-2.0 | Maven Central |
| icerockdev/moko-resources | 0.27.0 | — | Apache-2.0 [~] | GitHub tags |
| Traefik / CrowdSec / nginx (в infra) | v3.6.16 / v1.7.7 / 1.27-alpine | — | MIT / MIT / BSD-2 | compose файлы |
| JDK / Go на машине (для spike) | 21.0.12.1 / 1.26.5 | — | — | [изм] |

---

## Источники

### Внутренние (чтение репозиториев 2026-09-17)
- `go-backend-template`: `internal/platform/authgate/{jwt,cookie,module,authn}.go` (+ тесты), `internal/domain/auth/{usecase,errors,entity,dto}.go`, `internal/domain/auth/delivery/http/{handler,dto,view}.go`, `internal/domain/user/delivery/http/{dto,handler}.go`, `internal/platform/config/config.go`, `cmd/api/main.go`, `.env.example`, `Makefile`, `docs/reference/{architecture,release}.md`, `docs/stack/README.md`, `docs/stack/_templates/{auth,config}.md`, `test/README.md`, `.claude/rules/infra-impact.md`, `.claude/skills/infra-handoff/SKILL.md`
- `golibinfra`: `authnkit/{jwt,module}.go`, `authnkit/README.md`, `oidckit/README.md`, `httpkit/docs/usage.md`, `httpkit/{server,module,error,middleware,cors,ratelimit,idempotency}.go`, `httpclient/README.md`, `telemetry/{README.md,config.go,init.go}`, `flagkit/{README.md,flag.go,module.go}`, `healthkit/{README.md,healthkit.go}`, `cachekit/idempotency.go`
- `spa-frontend-template`: `src/shared/api/client.ts`, `src/shared/api/mock/**`, `src/shared/config/index.ts`, `src/shared/observability/index.ts`, `src/shared/i18n/**`, `src/entities/*/model/schema.ts`, `src/entities/*/api/queries.ts`, `src/app/styles/index.css`, `docs/stack/_templates/{api,config,observability,styling}.md`, `docs/stack/README.md`, `docs/reference/{observability,deployment,ecosystem}.md`, `docker/nginx-entrypoint.sh`, `.claude/skills/scaffold/SKILL.md`, `.claude/rules/{env-local,shared-ui}.md`
- `business-strategy-template`: `docs/projects/_template/project/surface.md`, `interface/README.md`, `screens/README.md`, `screens/_template/{flow,screen,block,shell}.md`, `fsd/README.md`, `fsd/_template/*.md`, `telegram-bot-flow/README.md`, `telegram-bot-flow/_template/**`, `.claude/skills/{fsd-deep-dive,fsd-audit}/SKILL.md`
- `corp-infra-template`: `docs/ecosystem.md`, `compose/{authentik,ntfy}.yml`, `docs/{architecture,onboarding}.md` (grep)
- `project-infra-template-compose`: `README.md`, `.env.example`, `compose/*.yml`, `configs/traefik/{traefik.yml.tpl,dynamic/middlewares.yml.tpl}`, `scripts/render-configs.sh`, `.claude/rules/{traefik-routing,multi-env}.md`, `docs/**`
- `mobile-template/docs/research/{brief,01-android-canon,02-ios-canon,03-shared-strategy,05-distribution-ru}.md`
- [изм] дамп `openapi.yaml`/`openapi-3.0.yaml` из go-backend-template (`api.OpenAPI().YAML()`/`DowngradeYAML()`), `openapi-generator-cli 7.25.0 validate/generate -g kotlin --library multiplatform`; сгенерированный код в scratchpad `07/gen-kmp/`

### Auth / безопасность
- https://developer.android.com/jetpack/androidx/releases/security (deprecation security-crypto)
- https://developer.android.com/privacy-and-security/keystore
- https://developer.android.com/privacy-and-security/security-config (pin-set, expiration)
- https://developer.android.com/google/play/integrity/overview
- https://developer.apple.com/documentation/security/ksecattraccessibleafterfirstunlockthisdeviceonly
- https://developer.apple.com/documentation/security/ksecattraccessiblewhenunlockedthisdeviceonly
- https://developer.apple.com/documentation/security/ksecattrsynchronizable
- https://developer.apple.com/documentation/bundleresources/information-property-list/nsapptransportsecurity/nspinneddomains
- https://developer.apple.com/documentation/foundation/httpcookiestorage
- https://developer.apple.com/documentation/devicecheck/establishing-your-app-s-integrity
- https://developer.apple.com/documentation/authenticationservices/aswebauthenticationsession
- https://raw.githubusercontent.com/OWASP/masvs/master/controls/MASVS-*.md ; https://raw.githubusercontent.com/OWASP/masvs/master/Document/11-MASVS-RESILIENCE.md
- https://raw.githubusercontent.com/OWASP/mastg/master/knowledge/android/MASVS-NETWORK/MASTG-KNOW-0015.md
- https://www.rfc-editor.org/rfc/rfc8252.txt
- https://docs.goauthentik.io/add-secure-apps/providers/oauth2/
- https://github.com/openid/AppAuth-Android ; https://github.com/openid/AppAuth-iOS/releases/tag/3.0.0
- https://raw.githubusercontent.com/ktorio/ktor/main/ktor-client/ktor-client-darwin/darwin/src/io/ktor/client/engine/darwin/internal/DarwinSession.kt
- https://ktor.io/docs/client-cookies.html ; https://ktor.io/docs/client-engines.html
- https://raw.githubusercontent.com/square/okhttp/master/CHANGELOG.md

### OpenAPI / кодоген
- https://huma.rocks/features/openapi-generation/ ; https://huma.rocks/features/cli/ ; https://huma.rocks/features/response-errors/
- https://github.com/apple/swift-openapi-generator ; https://raw.githubusercontent.com/apple/swift-openapi-generator/main/Sources/swift-openapi-generator/Documentation.docc/Articles/{Supported-OpenAPI-features,Configuring-the-generator,Frequently-asked-questions}.md ; Package.swift
- https://raw.githubusercontent.com/apple/swift-openapi-runtime/main/Sources/OpenAPIRuntime/Interface/ClientTransport.swift
- https://openapi-generator.tech/docs/generators/kotlin ; https://openapi-generator.tech/docs/generators/swift6 ; https://raw.githubusercontent.com/OpenAPITools/openapi-generator/master/docs/generators/kotlin.md ; README (3.1 beta) ; шаблоны `kotlin-client/libraries/multiplatform/{build.gradle.kts,auth/ApiKeyAuth.kt}.mustache`
- https://github.com/Foso/Ktorfit ; https://foso.github.io/Ktorfit/ ; https://raw.githubusercontent.com/Foso/Ktorfit/master/docs/CHANGELOG.md

### Mock
- https://ktor.io/docs/client-testing.html
- https://raw.githubusercontent.com/android/nowinandroid/main/README.md ; `build-logic/.../NiaFlavor.kt` ; `core/network/src/demo/.../FlavoredNetworkModule.kt` ; `core/network/src/main/.../demo/DemoNiaNetworkDataSource.kt`

### Deep links
- https://developer.android.com/training/app-links/verify-android-applinks
- https://developer.apple.com/documentation/technotes/tn3155-debugging-universal-links
- https://community.traefik.io/t/serve-static-files-with-traefik-2-with-nginx-apache-or-caddyserver/4591

### Push
- https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages
- https://firebase.google.com/docs/cloud-messaging/customize-messages/set-message-type ; …/setting-message-lifespan ; https://firebase.google.com/docs/cloud-messaging/android-message-priority
- https://www.rustore.ru/help/en/sdk/push-notifications/send-push-notifications
- https://developer.apple.com/documentation/usernotifications/generating-a-remote-notification

### Observability
- https://docs.sentry.io/platforms/android/configuration/gradle/ ; https://docs.sentry.io/platforms/android/configuration/releases/ ; https://docs.sentry.io/platforms/android/configuration/options/ ; https://docs.sentry.io/platforms/android/manual-setup.md ; https://raw.githubusercontent.com/getsentry/sentry-java/main/CHANGELOG.md
- https://docs.sentry.io/platforms/apple/dsym/ ; https://docs.sentry.io/platforms/apple/guides/ios/manual-setup.md ; https://docs.sentry.io/platforms/apple/configuration/releases.md
- https://github.com/getsentry/sentry-kotlin-multiplatform
- https://opentelemetry.io/docs/languages/ ; https://github.com/open-telemetry/opentelemetry-android ; https://github.com/open-telemetry/opentelemetry-swift
- https://gitlab.com/glitchtip/glitchtip-backend/-/merge_requests/84 ; https://gitlab.com/glitchtip/glitchtip-backend/-/raw/master/LICENSE

### Feature flags / токены / FSD
- https://docs.getunleash.io/reference/sdks/android ; https://raw.githubusercontent.com/Unleash/unleash/main/LICENSE
- https://docs.flagsmith.com/deployment/overview
- https://styledictionary.com/ ; https://styledictionary.com/reference/hooks/formats/predefined/ ; https://styledictionary.com/info/dtcg/ ; https://registry.npmjs.org/style-dictionary/latest
- https://feature-sliced.design/docs/reference/layers
- https://medium.com/@devanshtiwari365/from-chaos-to-clarity-how-feature-sliced-design-transformed-our-react-native-app-38a5a8fedd92 (вторичный)

### Реестры версий
- Maven Central `maven-metadata.xml` (okhttp, ktor, ktorfit, kotlinx-serialization, sentry, kermit, timber, appauth, tink, opentelemetry-android, unleash-android, retrofit, mockwebserver3, okhttp-java-net-cookiejar, multiplatform-settings, kvault, openapi-generator-cli)
- Google Maven `group-index.xml` (androidx.security, androidx.credentials, androidx.datastore, androidx.appcompat, com.google.firebase, com.google.android.play)
- GitHub API `releases/latest` / `tags` / raw `LICENSE` (список в таблице версий); npm registry (style-dictionary); artifactory VK (rustore pushclient)
