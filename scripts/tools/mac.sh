#!/usr/bin/env bash
# macOS-стадии гейта по SSH (08 §4.4): rsync дерева в ~/ci/<slug>/ на MAC_HOST, xcodebuild там, результаты обратно.
# Без MAC_HOST — fail-fast: check-full и релиз не должны изображать зелёный. Успешный check пишет build/gate-logs/mac.last.
#   mac.sh doctor|check|build|test|snapshot-record|run      MAC_UI=1 — плюс план UI
set -uo pipefail
cd "$(dirname "$0")/../.."
CMD="${1:?doctor|check|build|test|snapshot-record|run}"
if [ -z "${MAC_HOST:-}" ]; then
    echo "FAIL: MAC_HOST не задан — macOS-стадии недоступны. export MAC_HOST=user@mac (docs/reference/mac-host.md); Linux-часть — make check"
    exit 2
fi
SSH="ssh -o BatchMode=yes -o ConnectTimeout=10 $MAC_HOST"
REMOTE="~/ci/$(basename "$PWD")"
XCODE=$(cat .xcode-version)
SIM=$(cat iosApp/.test-simulator 2>/dev/null || echo "platform=iOS Simulator,name=iPhone 17")
PROJECT="iosApp/iosApp.xcodeproj"
LOGDIR="${STIT_LOGDIR:-build/gate-logs}"
mkdir -p "$LOGDIR" build/mac

$SSH true 2>/dev/null || { echo "FAIL: $MAC_HOST недоступен по SSH (BatchMode: нужен ключ без пароля)"; exit 2; }

remote() { $SSH "cd $REMOTE && set -o pipefail && $*"; }
sync() { rsync -a --delete --exclude build --exclude .build --exclude .gradle --exclude .kotlin --exclude .git ./ "$MAC_HOST:$REMOTE/"; }
fetch() { rsync -a "$MAC_HOST:$REMOTE/build/mac/" build/mac/ 2>/dev/null || true; }
xcodebuild() { remote "mkdir -p build/mac && xcodebuild $* 2>&1 | (command -v xcbeautify >/dev/null && xcbeautify || cat)"; }
step() {
    local label="$1" log start
    shift
    log="$LOGDIR/${label// /_}.log"
    start=$(date +%s)
    if "$@" >"$log" 2>&1; then
        printf 'OK:   %-28s %3ds\n' "$label" "$(( $(date +%s) - start ))"
    else
        printf 'FAIL: %-28s %3ds  (лог: %s)\n' "$label" "$(( $(date +%s) - start ))" "$log"
        grep -E 'error:|failed|FAILED|✖' "$log" | head -20
        return 1
    fi
}
doctor() {
    local have
    have=$(remote xcodebuild -version | awk 'NR==1 {print $2}')
    [ "$have" = "$XCODE" ] || { echo "FAIL: Xcode $have ≠ .xcode-version ($XCODE)"; return 1; }
    remote "xcrun simctl list devices available" | grep -q "${SIM##*name=}" || { echo "FAIL: симулятора '$SIM' нет на $MAC_HOST"; return 1; }
    echo "OK:   mac doctor — Xcode $XCODE, симулятор '${SIM##*name=}'"
}
test_plan() { xcodebuild test -project "$PROJECT" -scheme Dev -testPlan "$1" -destination "'$SIM'" -resultBundlePath "build/mac/$1.xcresult"; }

case "$CMD" in
    doctor) doctor ;;
    build) sync && step "xcodebuild build" xcodebuild build -project "$PROJECT" -scheme Dev -destination "'generic/platform=iOS Simulator'" CODE_SIGNING_ALLOWED=NO ;;
    test)
        sync
        fails=0
        step "test plan Unit" test_plan Unit || fails=$((fails + 1))
        step "test plan Snapshot" test_plan Snapshot || fails=$((fails + 1))
        [ "${MAC_UI:-0}" = "1" ] && { step "test plan UI" test_plan UI || fails=$((fails + 1)); }
        fetch
        exit "$fails" ;;
    snapshot-record)
        sync && step "snapshot record" remote "SNAPSHOT_TESTING_RECORD=all xcodebuild test -project $PROJECT -scheme Dev -testPlan Snapshot -destination '$SIM'"
        rsync -a --include '*/' --include '__Snapshots__/**' --exclude '*' "$MAC_HOST:$REMOTE/iosApp/" iosApp/ ;;
    run)
        sync && remote "xcodebuild -project $PROJECT -scheme Dev -destination '$SIM' build && xcrun simctl boot '${SIM##*name=}' || true" \
            && remote "open -a Simulator && xcrun simctl launch booted \$(defaults read \$(find build -name '*.app' | head -1)/Info CFBundleIdentifier)" ;;
    check)
        fails=0
        doctor || fails=$((fails + 1))
        sync
        step "shared link framework" remote "./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --console=plain -q && test -d shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework" || fails=$((fails + 1))
        step "shared iosSimulatorArm64Test" remote "./gradlew :shared:iosSimulatorArm64Test --console=plain -q" || fails=$((fails + 1))
        step "shared api (strict)" remote "./gradlew :shared:apiCheck -Pstit.klibStrict=true --console=plain -q" || fails=$((fails + 1))
        step "xcodebuild build" xcodebuild build -project "$PROJECT" -scheme Dev -destination "'generic/platform=iOS Simulator'" CODE_SIGNING_ALLOWED=NO || fails=$((fails + 1))
        step "test plan Unit" test_plan Unit || fails=$((fails + 1))
        step "test plan Snapshot" test_plan Snapshot || fails=$((fails + 1))
        [ "${MAC_UI:-0}" = "1" ] && { step "test plan UI" test_plan UI || fails=$((fails + 1)); }
        fetch
        if [ "$fails" -eq 0 ]; then
            echo "$(date '+%F %H:%M') (git $(git rev-parse --short HEAD))" >build/gate-logs/mac.last
            echo "✓ check-mac clean"
        else
            echo "✗ check-mac: провалено стадий — $fails"
        fi
        exit "$fails" ;;
    *) echo "FAIL: неизвестная команда '$CMD' (doctor|check|build|test|snapshot-record|run)"; exit 2 ;;
esac
