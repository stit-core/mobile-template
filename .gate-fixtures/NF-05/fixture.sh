# NF-05 — манифест без android:icon: ловит Android Lint MissingApplicationIcon (warningsAsErrors)
EXPECT="android lint"
TOUCHES="androidApp/app/src/main/AndroidManifest.xml"
apply() {
    sed -i '/android:icon=/d' "$TOUCHES"
}
