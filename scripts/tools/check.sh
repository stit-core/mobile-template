#!/usr/bin/env bash
# Канонический гейт, Linux-полный (08 §4.2): одна строка на стадию, без fail-fast — агенту нужен весь
# список провалов за один прогон. macOS-стадии не изображают зелёный: они перечислены SKIP-блоком в конце.
#   check.sh                  полный гейт;  GATE_RELEASE=1 — плюс assembleProdRelease (R8)
#   check.sh --only <стадия>  одна стадия (так гоняет gate-selftest);  FAST=1 — короткий цикл (make check-fast)
set -uo pipefail
cd "$(dirname "$0")/../.."
GRADLE="bash scripts/tools/gradle-step.sh"
SWIFT="bash scripts/tools/swift-step.sh"
IOS_SOURCES="iosApp/App iosApp/Widgets iosApp/Packages iosApp/Tools iosApp/UITests"

# Стадия → команда. Порядок: от дешёвых к дорогим, формат → сборка.
stage() {
    case "$1" in
        "format kotlin")       $GRADLE "$1" spotlessCheck checkSortDependencies ;;
        "format swift")        $SWIFT "$1" swift format lint --strict --recursive $IOS_SOURCES ;;
        "swiftlint")           $SWIFT "$1" swiftlint lint --strict --quiet --config iosApp/.swiftlint.yml iosApp ;;
        "archlint")            $SWIFT "$1" swift run --package-path iosApp/Tools/archlint archlint iosApp/Packages ;;
        "swift pure build+tests") $SWIFT "$1" swift test --package-path iosApp/Packages/Pure --enable-code-coverage ;;
        "coverage swift")      bash scripts/tools/coverage-floor.sh iosApp/Packages/Pure 70 ;;
        "tokens-check")        bash scripts/tools/tokens-check.sh ;;
        "i18n-check")          bash scripts/tools/i18n-check.sh ;;
        "deeplinks-check")     bash scripts/tools/deeplinks-check.sh ;;
        "capability-check")    bash scripts/tools/capability-check.sh ;;
        "detekt")              $GRADLE "$1" detekt ;;
        "android lint")        $GRADLE "$1" lintDevDebug ;;
        "shared jvm tests")    $GRADLE "$1" :shared:jvmTest ;;
        "android unit tests")  $GRADLE "$1" testDevDebugUnitTest :shared:testAndroidHostTest :androidApp:lint:test ;;
        "screenshots")         $GRADLE "$1" verifyRoborazziDevDebug ;;
        "coverage kotlin")     $GRADLE "$1" :shared:koverVerify :androidApp:app:koverVerifyGate ;;
        "kmp")                 bash scripts/tools/kmp-check.sh ;;
        "gen-check")           bash scripts/tools/gen-check.sh ;;
        "mock-check")          bash scripts/tools/mock-check.sh ;;
        "shared api")          $GRADLE "$1" :shared:apiCheck ;;
        "assembleDevDebug")    $GRADLE "$1" :androidApp:app:assembleDevDebug ;;
        "assembleProdRelease") $GRADLE "$1" :androidApp:app:assembleProdRelease ;;
        *) echo "FAIL: неизвестная стадия '$1'"; return 2 ;;
    esac
}

if [ "${1:-}" = "--only" ]; then
    stage "${2:?имя стадии}"
    exit $?
fi

if [ "${FAST:-0}" = "1" ]; then
    # Пока пишу: Swift и detekt первыми — не ждать Gradle ради опечатки.
    STAGES=("format swift" "swiftlint" "format kotlin" "detekt" "shared jvm tests")
else
    # Swift-стадии — до Gradle: дешёвые, без демона; красный Swift виден раньше минутных Android-стадий.
    STAGES=("format kotlin" "format swift" "swiftlint" "archlint" "swift pure build+tests" "coverage swift"
        "tokens-check" "i18n-check" "deeplinks-check" "capability-check"
        "gen-check" "detekt" "android lint" "shared jvm tests" "mock-check" "android unit tests"
        "screenshots" "coverage kotlin" "kmp" "shared api" "assembleDevDebug")
    [ "${GATE_RELEASE:-0}" = "1" ] && STAGES+=("assembleProdRelease")
fi

echo "→ gate: $(basename "$PWD")$([ "${FAST:-0}" = "1" ] && echo ' (fast)')"
start=$(date +%s)
failed=()
for name in "${STAGES[@]}"; do
    stage "$name" || failed+=("$name")
done

MAC_STAGES=("shared link framework" "shared iosSimulatorArm64Test" "xcodebuild build" "test plan Unit" "test plan Snapshot")
if [ "${FAST:-0}" != "1" ]; then
    echo "— macOS-стадии (не выполнены на этом хосте) —"
    printf 'SKIP: %-28s → make check-mac\n' "${MAC_STAGES[0]}"
    for name in "${MAC_STAGES[@]:1}"; do printf 'SKIP: %s\n' "$name"; done
fi

last_mac="never"
[ -f build/gate-logs/mac.last ] && last_mac=$(cat build/gate-logs/mac.last)
dur=$(( $(date +%s) - start ))
mkdir -p build/gate-logs
if [ ${#failed[@]} -eq 0 ]; then
    echo "✓ check clean (Linux-часть, ${dur}s) · macOS-стадий не выполнено: ${#MAC_STAGES[@]} · последний check-mac: $last_mac"
    echo "$(date '+%F %H:%M') OK $(git rev-parse --short HEAD 2>/dev/null || echo '-')" >build/gate-logs/check.last
else
    echo "✗ check: провалено стадий — ${#failed[@]} ($(IFS=,; echo "${failed[*]}")) · ${dur}s"
    echo "$(date '+%F %H:%M') FAIL ${#failed[@]}" >build/gate-logs/check.last
fi
exit ${#failed[@]}
