# NF-18 — хост deep link задан в манифесте литералом вместо ${DEEP_LINK_HOST} из config/env: ловит deeplinks-check
EXPECT="deeplinks-check"
TOUCHES="androidApp/app/src/main/AndroidManifest.xml"
apply() {
    sed -i 's|android:host="${DEEP_LINK_HOST}"|android:host="app.example.com"|' "$TOUCHES"
}
