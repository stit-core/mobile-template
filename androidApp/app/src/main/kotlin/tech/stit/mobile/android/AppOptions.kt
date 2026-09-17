package tech.stit.mobile.android

// Ключи config/env, которые нужны только платформе (в AppConfig shared их нет).
data class AppOptions(
    val pushProviders: String,
    val rustoreProjectId: String,
    val biometricUnlock: Boolean,
)
