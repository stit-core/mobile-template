#!/usr/bin/env bash
# KMP на Linux (08 §4.3): klib под iOS компилируется, но link-таски молча SKIPPED с кодом 0 (06 L-10) —
# поэтому проверяем артефакт на диске. cinterop/CocoaPods запрещены: ломают Linux-компиляцию Apple-klib.
set -uo pipefail
cd "$(dirname "$0")/../.."
fails=0

if grep -nE 'cinterops\s*\{|native\.cocoapods' shared/build.gradle.kts; then
    printf 'FAIL: %-24s cinterop/CocoaPods в shared/build.gradle.kts запрещены — платформенное идёт через PlatformDeps\n' "kmp cinterop-free"
    fails=$((fails + 1))
else
    printf 'OK:   %-24s\n' "kmp cinterop-free"
fi

bash scripts/tools/gradle-step.sh "kmp ios klib" :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64 || fails=$((fails + 1))

for target in iosArm64 iosSimulatorArm64; do
    dir="shared/build/classes/kotlin/$target/main/klib"
    if [ -d "$dir" ] && [ -n "$(ls -A "$dir")" ]; then
        printf 'OK:   %-24s %s\n' "klib $target" "$dir"
    else
        printf 'FAIL: %-24s нет каталога %s — таск мог быть SKIPPED с кодом 0\n' "klib $target" "$dir"
        fails=$((fails + 1))
    fi
done
exit "$fails"
