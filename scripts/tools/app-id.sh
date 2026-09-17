#!/usr/bin/env bash
# applicationId варианта из androidApp/app/build.gradle.kts и build-logic: базовый id + суффикс flavor + суффикс buildType.
#   app-id.sh [dev|staging|prod|demo] [debug|release]   → tech.stit.mobile.demo.debug
set -euo pipefail
cd "$(dirname "$0")/../.."
flavor="${1:-dev}"
build_type="${2:-debug}"
app=androidApp/app/build.gradle.kts
plugin=build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt

base=$(sed -n 's/^ *applicationId = "\(.*\)"$/\1/p' "$app")
[ -n "$base" ] || { echo "FAIL: applicationId не найден в $app" >&2; exit 2; }
flavor_suffix=$( (grep "getByName(\"$flavor\")" "$app" || true) | sed -n 's/.*applicationIdSuffix = "\([^"]*\)".*/\1/p')
type_suffix=$( (grep -E "^\s*$build_type \{" "$plugin" || true) | sed -n 's/.*applicationIdSuffix = "\([^"]*\)".*/\1/p')
echo "$base$flavor_suffix$type_suffix"
