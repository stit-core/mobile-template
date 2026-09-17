#!/usr/bin/env bash
# Fail-fast preflight: всё, без чего гейт не поедет.
set -uo pipefail
fail=0
say() { printf '%-28s %s\n' "$1" "$2"; }

if command -v javac >/dev/null 2>&1; then
  say "JDK" "OK: $(javac -version 2>&1)"
else
  say "JDK" "FAIL: нет javac. Нужен JDK 17+ (JRE недостаточно)."; fail=1
fi

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ] && [ -f local.properties ]; then
  SDK=$(grep -E '^sdk\.dir=' local.properties | cut -d= -f2-)
fi
if [ -n "$SDK" ] && [ -d "$SDK/platforms" ]; then
  say "Android SDK" "OK: $SDK ($(ls "$SDK/platforms" | tr '\n' ' '))"
else
  say "Android SDK" "FAIL: нет ANDROID_HOME / local.properties#sdk.dir"; fail=1
fi

if [ -n "$SDK" ] && [ -f "$SDK/licenses/android-sdk-license" ]; then
  say "SDK licenses" "OK"
else
  say "SDK licenses" "WARN: лицензии не приняты — Gradle не сможет доустановить пакеты"
fi

free_mb=$(awk '/MemAvailable/ {print int($2/1024)}' /proc/meminfo 2>/dev/null || echo 0)
if [ "$free_mb" -lt 3000 ]; then
  say "RAM" "WARN: доступно ${free_mb}MB — lint/Robolectric могут упасть по Metaspace"
else
  say "RAM" "OK: ${free_mb}MB"
fi

[ $fail -eq 0 ] && echo "✓ doctor clean" || echo "✗ doctor: окружение неполное"
exit $fail
