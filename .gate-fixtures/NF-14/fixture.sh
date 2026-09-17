# NF-14 — cinterops {} в shared/build.gradle.kts: ловит kmp-check.sh (cinterop ломает Linux-сборку Apple-klib)
EXPECT="kmp"
TOUCHES="shared/build.gradle.kts"
apply() {
    printf '\nkotlin {\n    iosArm64().compilations.getByName("main").cinterops { create("keychain") }\n}\n' >>"$TOUCHES"
}
