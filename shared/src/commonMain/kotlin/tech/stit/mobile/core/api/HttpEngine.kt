package tech.stit.mobile.core.api

import io.ktor.client.engine.HttpClientEngine

// Движок Ktor платформы (OkHttp / Darwin); в режиме API_MODE=mock не создаётся вовсе — demo не ходит в сеть.
internal expect fun platformHttpEngine(): HttpClientEngine
