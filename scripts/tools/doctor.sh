#!/usr/bin/env bash
# Fail-fast preflight окружения: три частых стопора (нет javac, нет Android SDK, мало RAM) плюс Swift/Mac.
# Пути тулчейнов — только через env (JAVA_HOME, ANDROID_HOME, SWIFT_TOOLCHAIN, MAC_HOST); машина в файлах не хардкодится.
# Read-only. FAIL-класс — exit 2; WARN — exit 0. Последняя строка: `OK: doctor clean (N warn)` / `FAIL: N blocking, M warn`.
set -uo pipefail
cd "$(dirname "$0")/../.."

FAIL=0
WARN=0
ok()   { echo "OK   : $*"; }
warn() { echo "WARN : $*"; WARN=$((WARN + 1)); }
fail() { echo "FAIL : $*"; FAIL=$((FAIL + 1)); }
info() { echo "INFO : $*"; }
version_of() { sed -n "s/^$1 = \"\(.*\)\"/\1/p" gradle/libs.versions.toml; }

# 1. JDK 21 с javac: демон Gradle компилирует Hilt-сгенерированную Java; JRE недостаточно.
JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/javac}"
[ -x "${JAVAC:-/nonexistent}" ] || JAVAC=$(command -v javac || true)
if [ -n "$JAVAC" ]; then
    jv=$("$JAVAC" -version 2>&1 | awk '{print $2}')
    case "$jv" in
        21.*) ok "JDK: javac $jv" ;;
        *) fail "JDK: javac $jv, нужен 21 (Robolectric SDK 36/37, toolchain) — export JAVA_HOME=<Temurin 21>" ;;
    esac
else
    fail "JDK: нет javac — export JAVA_HOME=<Temurin 21> (системный JRE не подходит)"
fi

# 2. Android SDK с платформой compileSdk (ANDROID_HOME / ANDROID_SDK_ROOT / local.properties#sdk.dir).
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -z "$SDK" ] && [ -f local.properties ] && SDK=$(sed -n 's/^sdk\.dir=//p' local.properties)
compile_sdk=$(version_of compileSdk)
platform="android-$compile_sdk.$(version_of compileSdkMinor)"
if [ -z "$SDK" ] || [ ! -d "$SDK/platforms" ]; then
    fail "Android SDK: нет ANDROID_HOME и local.properties#sdk.dir (образец — local.properties.example)"
elif [ -d "$SDK/platforms/$platform" ] || [ -d "$SDK/platforms/android-$compile_sdk" ]; then
    ok "Android SDK: $SDK ($(ls "$SDK/platforms" | tr '\n' ' '| sed 's/ $//'))"
else
    fail "Android SDK: нет platforms/$platform — android --no-metrics sdk install platforms/$platform build-tools platform-tools"
fi
if [ -n "$SDK" ] && [ -d "$SDK" ]; then
    [ -f "$SDK/licenses/android-sdk-license" ] && ok "SDK licenses: приняты" \
        || warn "SDK licenses: не приняты — Gradle не сможет доустанавливать пакеты (android sdk install …)"
fi

# 3. RAM: пара демонов Gradle+Kotlin ≈ 4.8 ГБ RSS; ниже 4 ГБ Lint падает по Metaspace (06 L-02).
# Свои уже запущенные демоны держат память, которую гейт и так получит, — прибавляем их RSS к доступной.
daemons_mb=$(ps -u "$(id -u)" -o rss=,args= | awk '/GradleDaemon|KotlinCompileDaemon|GradleWorkerMain/ {s += $1} END {print int(s / 1024)}')
free_mb=$(( $(awk '/MemAvailable/ {print int($2/1024)}' /proc/meminfo) + daemons_mb ))
if [ "$free_mb" -lt 4096 ]; then
    fail "RAM: доступно ${free_mb} MB (< 4 ГБ) — освободи память или закрой чужие демоны (свои: ./gradlew --stop)"
elif [ "$free_mb" -lt 6144 ]; then
    warn "RAM: доступно ${free_mb} MB (< 6 ГБ) — гейт пройдёт, эмулятор не поднимется"
else
    ok "RAM: доступно ${free_mb} MB (из них ${daemons_mb} MB у своих демонов сборки)"
fi

# 4. Диск: SDK + кеши Gradle ≈ 9.5 ГБ, K/N ≈ 1 ГБ, Swift ≈ 3.6 ГБ.
free_gb=$(df -Pm . | awk 'NR==2 {print int($4/1024)}')
[ "$free_gb" -ge 15 ] && ok "Диск: свободно ${free_gb} GB" || warn "Диск: свободно ${free_gb} GB (< 15) — кеши сборки могут не поместиться"

# 5. Swift-тулчейн и SwiftLint: обязательны, как только появляется iosApp/; до этого — предупреждение.
[ -n "${SWIFT_TOOLCHAIN:-}" ] && export PATH="$SWIFT_TOOLCHAIN:$PATH"
want_swift=$(cat .swift-version)
if command -v swift >/dev/null 2>&1; then
    have_swift=$(swift --version 2>&1 | sed -n 's/.*Swift version \([0-9.]*\).*/\1/p' | head -1)
    [ "$have_swift" = "$want_swift" ] && ok "Swift: $have_swift ($(command -v swift))" \
        || warn "Swift: $have_swift, .swift-version требует $want_swift"
elif [ -d iosApp ]; then
    fail "Swift: нет swift в PATH — export SWIFT_TOOLCHAIN=<swift-$want_swift>/usr/bin"
else
    warn "Swift: нет swift в PATH (SWIFT_TOOLCHAIN) — Swift-стадии в SKIP, пока iosApp/ пуст"
fi
if command -v swiftlint >/dev/null 2>&1; then
    ok "SwiftLint: $(swiftlint version)"
elif [ -d iosApp ]; then
    fail "SwiftLint: не найден — положи бинарь в PATH (release realm/SwiftLint, linux amd64)"
else
    warn "SwiftLint: не найден в PATH — понадобится вместе с iosApp/"
fi

# 6. Mac-хост: macOS-стадии идут по SSH; без MAC_HOST они честно SKIP в make check.
want_xcode=$(cat .xcode-version)
if [ -n "${MAC_HOST:-}" ]; then
    have_xcode=$(ssh -o BatchMode=yes -o ConnectTimeout=5 "$MAC_HOST" xcodebuild -version 2>/dev/null | awk 'NR==1 {print $2}')
    if [ -z "$have_xcode" ]; then
        fail "MAC_HOST: $MAC_HOST недоступен по SSH (BatchMode: ключ без пароля) или нет xcodebuild"
    elif [ "$have_xcode" = "$want_xcode" ]; then
        ok "MAC_HOST: $MAC_HOST, Xcode $have_xcode"
    else
        fail "MAC_HOST: Xcode $have_xcode ≠ .xcode-version ($want_xcode)"
    fi
else
    info "MAC_HOST не задан — macOS-стадии в SKIP (make check); для check-mac: export MAC_HOST=user@mac (Xcode $want_xcode)"
fi
[ -e /dev/kvm ] && info "KVM: есть (эмулятор возможен при ≥ 10 ГБ RAM)" || info "KVM: нет — эмулятор недоступен, тесты через Robolectric"

echo
if [ "$FAIL" -gt 0 ]; then
    echo "FAIL: $FAIL blocking, $WARN warn"
    exit 2
fi
echo "OK: doctor clean ($WARN warn)"
