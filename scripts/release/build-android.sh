#!/usr/bin/env bash
# Android-ассеты STOP-1 (08 §7.2): подписанные prod AAB + APK, mapping.txt и checksums.sha256 → dist/.
# Подпись — только из env STIT_KEYSTORE_* (Vaultwarden mobile/<org>/<app>); версия — из тега.
#   build-android.sh [vX.Y.Z]
set -euo pipefail
cd "$(dirname "$0")/../.."
eval "$(bash scripts/tools/version-from-tag.sh "${1:-}")"
export APP_VERSION APP_BUILD
for name in STIT_KEYSTORE_PATH STIT_KEYSTORE_PASSWORD STIT_KEY_ALIAS STIT_KEY_PASSWORD; do
    [ -n "${!name:-}" ] || { echo "FAIL: $name не задан — секреты подписи берутся из env (Vaultwarden mobile/<org>/<app>)"; exit 2; }
done
[ -f "$STIT_KEYSTORE_PATH" ] || { echo "FAIL: keystore $STIT_KEYSTORE_PATH не найден"; exit 2; }

app=$(basename "$PWD")
out="androidApp/app/build/outputs"
./gradlew :androidApp:app:bundleProdRelease :androidApp:app:assembleProdRelease --console=plain -q
mkdir -p dist
cp "$out/bundle/prodRelease/app-prod-release.aab" "dist/$app-$APP_VERSION-$APP_BUILD-prod.aab"
cp "$out/apk/prod/release/app-prod-release.apk" "dist/$app-$APP_VERSION-$APP_BUILD-prod.apk"
cp "$out/mapping/prodRelease/mapping.txt" "dist/mapping-$APP_VERSION-$APP_BUILD.txt"

# Подпись проверяем тем же apksigner, что и магазины: неподписанный артефакт не должен дойти до STOP-1.
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$(sed -n 's/^sdk\.dir=//p' local.properties 2>/dev/null)}}"
apksigner=$(ls -d "$sdk"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1)
"$apksigner" verify --print-certs "dist/$app-$APP_VERSION-$APP_BUILD-prod.apk" | grep -E 'Verifies|SHA-256'
(cd dist && sha256sum -- *.aab *.apk mapping-*.txt >checksums.sha256)
echo "OK: dist/ — $(ls dist | tr '\n' ' ')"
