package tech.stit.mobile.core.lib

enum class Platform {
    Android,
    Ios,
    Jvm,
}

// expect/actual вместо runtime-детекции: платформа известна при компиляции, а host-тест ловит перепутанный source set.
expect val currentPlatform: Platform
