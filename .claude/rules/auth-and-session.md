---
name: auth-and-session
description: Bearer + refresh в JSON — access в памяти (15 мин), refresh в SecureStore (90 дней, ротация), single-flight refresh, logout = revoke + очистка, AuthMode.Cookie только по auth.md#mode, биометрия — разблокировка ключа, pinning выключен, удаление аккаунта — обязательный экран. Грузится при правке core/auth, entity/session, SecureStore*, Keychain*, auth-фич.
paths:
  - "shared/**/core/auth/**"
  - "shared/**/entity/session/**"
  - "**/SecureStore*"
  - "**/Keychain*"
  - "**/Keystore*"
  - "androidApp/**/auth/**"
  - "iosApp/**/AuthFeature/**"
---

# Auth и сессия

Контракт с бэкендом — `docs/stack/auth.md` (значения проекта) и `docs/reference/architecture.md`.

## Инварианты

- **Канон — Bearer + refresh в JSON**: `Authorization: Bearer <access>`, `POST /api/auth/refresh
  {refresh_token}` → `TokenPair`, `X-Client: android|ios`; access-токен — только в памяти
  (`SessionStore`, TTL с сервера ≈ 15 мин), refresh — `SecureStore` (Keystore AES-GCM +
  DataStore / Keychain `AfterFirstUnlockThisDeviceOnly`, `kSecAttrSynchronizable = false`),
  ротация при каждом refresh.
- **Single-flight refresh** — `Mutex` в `SessionStore` (`validAccessToken()` до запроса,
  `refreshAfter(stale)` после 401 в `AuthPlugin`); параллельные 401 ждут один refresh; 401 на
  самом refresh (`AUTH_REFRESH_REUSED`/`_EXPIRED`) → `SignedOut`. Тесты — `AuthPluginTest`,
  `SessionStoreTest`.
- **Logout (`SessionRepository.logout()`) = `PushRegistrar.unregister()` (`DELETE /api/v1/devices/{id}`)
  + `POST /api/auth/logout` + `SessionStore.clear()` → `onSignedOut` чистит кэши `user`/`todos`**;
  смена среды в dev — тоже очистка.
- **`AuthMode.Cookie` — режим совместимости** для бэкенда без Bearer-ветки (B-1…B-3):
  включается только `docs/stack/auth.md#mode = cookie` и `AUTH_MODE` в `config/env`;
  «оставаться в системе» в нём не гарантируется; `/init-service` печатает BACKEND-HANDOFF.
- **Биометрия (Android, `AUTH_BIOMETRIC_UNLOCK`) — замок UI при живой сессии, не замена входа**:
  `DefaultBiometricUnlock` (`BIOMETRIC_STRONG | DEVICE_CREDENTIAL`, хост — `AppCompatActivity`)
  → `ShellViewModel.unlocked` → `LockedState`; на iOS реализации нет.
- **Refresh не логируется** (`Logger`/`TelemetrySink` получают только `method path → status`;
  `beforeSend` Sentry на платформе режет `Authorization`, `Cookie`, тела auth);
  `backup_rules.xml`/`data_extraction_rules.xml` исключают `sharedpref`, шифртекст в DataStore
  без Keystore-ключа бесполезен; Keychain чистится при первом запуске (`FirstLaunchReset`,
  переживает удаление приложения).
- **Pinning выключен** (Let's Encrypt ротирует leaf каждые 60–90 дней); файлов
  `network_security_config.xml`/`NSPinnedDomains` в шаблоне нет — шов с kill-switch
  `requirements.flags.pinning`; cleartext запрещён (targetSdk ≥ 28), ATS без исключений.
- **`security-crypto` (EncryptedSharedPreferences) запрещён** — deprecated (detekt
  `ForbiddenImport`); `UserDefaults`/`SharedPreferences` для токенов — нет.
- **Обязательные экраны reference-фичи `auth`**: логин, список сессий
  (`GET/DELETE /api/auth/sessions`), **удаление аккаунта** (App Store 5.1.1(v), Play —
  URL веб-удаления в `compliance.md`); правовые документы доступны до логина.
- OIDC (Authentik, BFF-mobile) и passkeys — расширения поверх контракта, не в v1; соцвход
  тянет Sign in with Apple (4.8) — по умолчанию выключен.

## Сигналы

- Тест `AuthPluginTest` красный при параллельных 401 → сломан single-flight.
- `make status` → `BACKEND pending: B-1…B-3` → у соседа нет `POST /api/auth/refresh`.
