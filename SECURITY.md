# Безопасность

## Сообщить об уязвимости

Нашёл дыру в шаблоне — напиши на **security@stit.tech**. Опиши, что нашёл, как
воспроизвести и какой эффект. Мы ответим и согласуем сроки раскрытия.

Не открывай публичный issue для уязвимостей до того, как фикс готов и выпущен —
это подвергает риску всех, кто уже выпустил приложение из шаблона.

## Что в скоупе

Этот репозиторий — шаблон мобильного приложения. В скоупе: дефолты, которые делают
форк небезопасным из коробки (хранение токенов вне `SecureStore` — Keystore/Keychain,
утечка секретов или PII в логи, крэш-репорты или фикстуры, cleartext-трафик и ATS-исключения,
секреты в build-time конфиге и артефакте, слабый refresh-флоу — отсутствие single-flight,
ротации или отзыва при reuse, deep links с секретами в ссылке), и ошибки в
security-sensitive коде (`shared/…/core/auth`, `SecureStore`-actual'ы, `AuthPlugin`,
`SentryCrashReporter` — `beforeSend`, `scripts/release/*`, `build-logic` — подпись).

Вне скоупа: уязвимости в upstream-зависимостях (Ktor, Compose, Hilt, Sentry SDK, SwiftUI
и т. д.) — их мейнтейнерам; здесь — только если наш код или дефолт-конфиг делает их
эксплуатируемыми.

## Секреты не в репозитории

Шаблон не содержит реальных секретов. `local.properties.example` и
`iosApp/Config/Local.xcconfig.example` закоммичены с placeholder'ами (`.env.local.example` с
именами release-time ключей форк получает на `/de-template`); `local.properties`, `.env.local`,
`Local.xcconfig`, `*.jks`, `*.keystore`, `*.p12`, `*.p8`, `*.mobileprovision` — gitignored и в
deny-листе `pre-commit`. Ключи подписи и store-credentials хранятся вне репозитория и приходят в
сборку только через env (`STIT_KEYSTORE_*` и др.) на время `make release-*`.
В артефакте секретов нет: build-time конфиг (`config/env`, xcconfig) публичен по определению,
`SENTRY_DSN` — public by design. Если случайно закоммитил секрет в свой форк — ротируй его и
вычисти историю.
