#!/usr/bin/env bash
# Preflight релиза (08 §7.3–7.4): main, чистое дерево, аннотированный тег vX.Y.Z на HEAD (== TAG),
# тег старше всех предыдущих, doctor, полный гейт с R8, check-mac (кроме PLATFORM=android).
#   preflight.sh [vX.Y.Z]      PLATFORM=android|ios|both (по умолчанию both)
set -euo pipefail
cd "$(dirname "$0")/../.."
fail() { echo "release-preflight: $*" >&2; exit 2; }

branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null) || fail "не git-репозиторий"
[ "$branch" = "main" ] || fail "branch=$branch, релиз только с main"
[ -z "$(git status --porcelain)" ] || fail "working tree dirty — закоммить или stash"

tag=$(git describe --tags --exact-match HEAD 2>/dev/null || true)
[ -n "$tag" ] || fail "на HEAD нет тега — git tag -a vX.Y.Z -m '<release notes>'"
[ -z "${1:-}" ] || [ "$1" = "$tag" ] || fail "TAG=$1 ≠ тег на HEAD ($tag)"
[ "$(git cat-file -t "$tag")" = "tag" ] || fail "тег $tag не annotated — пересоздай через git tag -a"
eval "$(bash scripts/tools/version-from-tag.sh "$tag")"
newest=$(git tag -l 'v*' | sort -V | tail -1)
[ "$newest" = "$tag" ] || fail "тег $tag не новее последнего ($newest): версии только растут, отката нет"

echo "release-preflight: → doctor"
bash scripts/tools/doctor.sh || fail "doctor не прошёл"
echo "release-preflight: → GATE_RELEASE=1 check"
GATE_RELEASE=1 bash scripts/tools/check.sh || fail "make check не прошёл"
if [ "${PLATFORM:-both}" != "android" ]; then
    echo "release-preflight: → check-mac"
    bash scripts/tools/mac.sh check || fail "check-mac не прошёл — iOS-часть релиза требует Mac"
fi
echo "release-preflight: OK ($tag → versionName $APP_VERSION, versionCode $APP_BUILD)"
