#!/usr/bin/env bash
# Build phase «Compile Kotlin Framework»: KGP кладёт Shared.framework в продукты сборки Xcode (direct integration).
# Вызывается Xcode с его окружением (SRCROOT, CONFIGURATION, SDK_NAME, ARCHS, TARGET_BUILD_DIR, FRAMEWORKS_FOLDER_PATH).
set -euo pipefail
if [ "${OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED:-}" = "YES" ]; then
    echo "Shared.framework собирает IDE (OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES)"
    exit 0
fi
cd "${SRCROOT:?SRCROOT задаёт Xcode}/.."
# Xcode не передаёт JAVA_HOME из shell; JDK 21 берётся у java_home, если переменная не задана.
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null || true)}"
./gradlew :shared:embedAndSignAppleFrameworkForXcode --console=plain -q
