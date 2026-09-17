package tech.stit.mobile.core.api

import kotlinx.serialization.json.Json

// Аддитивная эволюция (07 §2.6): неизвестные поля игнорируются; encodeDefaults — повтор с тем же Idempotency-Key байт в байт.
internal val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    coerceInputValues = true
}
