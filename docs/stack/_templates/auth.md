---
id: stack.auth
kind: auth
name: auth
status: draft
---

# auth

## Mode

| поле | значение |
|---|---|
| mode | `bearer` _(`bearer` — канон; `cookie` — режим совместимости до B-1…B-3 у бэкенда; `none` — без входа)_ |
| контракт | `Authorization: Bearer <access>` · `POST /api/auth/login` + `X-Client` · `POST /api/auth/refresh {refresh_token}` → `TokenPair` · `POST /api/auth/logout` |
| backend readiness | _есть ли `POST /api/auth/refresh` в спеке соседа; нет — BACKEND-HANDOFF B-1…B-3_ |

## Tokens

| токен | где | TTL | ротация |
|---|---|---|---|
| access | память (`SessionStore`) | ≈ 15 мин (с сервера `expires_in`) | по refresh |
| refresh | `SecureStore` | 90 дней | при каждом refresh; reuse → отзыв цепочки → logout |

Single-flight refresh — `Mutex` в `SessionStore`, повтор запроса после 401 — `AuthPlugin`; logout
(`SessionRepository.logout()`) = `DELETE /api/v1/devices/{id}` + `POST /api/auth/logout` + очистка.

## Storage

`SecureStore`: Android — `KeystoreSecureStore` (Keystore AES-GCM + DataStore, `androidMain`); iOS —
`KeychainSecureStore` (`iosMain`, `platform.Security`: `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`,
`kSecAttrSynchronizable = false`; очистка при первом запуске — Swift); `allowBackup`/
`dataExtractionRules` исключают хранилище токенов.

## Sessions

`GET /api/auth/sessions` (экран «Устройства»), `DELETE /api/auth/sessions/{jti}`, `DELETE
/api/auth/sessions` («выйти везде»).

## Biometrics

| поле | значение |
|---|---|
| biometric unlock | `false` _(`AUTH_BIOMETRIC_UNLOCK`; биометрия — разблокировка ключа `SecureStore`, не замена сессии)_ |

## Pinning

`off` (Let's Encrypt ротирует leaf каждые 60–90 дней). Шов: `network_security_config.xml`
(release) / `NSPinnedDomains` с пустым набором; включение — пин корней ISRG X1/X2 + backup +
expiration 12 мес + kill-switch `requirements.flags.pinning`.

## Account deletion

Экран удаления аккаунта в приложении (из настроек) — обязателен при регистрации (App Store
5.1.1(v)); web-URL удаления — `compliance.md#legal-documents` (Play). SSO/passkeys — расширения,
не в v1; соцвход выключен (тянет Sign in with Apple).

## Generates

- `config/env/*.properties`, `iosApp/Config/*.xcconfig` — `AUTH_MODE`, `AUTH_BIOMETRIC_UNLOCK`
- `shared/…/core/auth/{AuthPlugin,AuthState,SecureStore,SessionStore,TokenPair}.kt`, `core/config/AuthMode.kt`, `KeystoreSecureStore`/`KeychainSecureStore`
- экраны reference-фичи `auth` (login, sessions, delete-account) — оставить при `mode ≠ none`
- BACKEND-HANDOFF B-1…B-3 при `mode = cookie` или отсутствии refresh в спеке

## Открытые вопросы

- _что_
