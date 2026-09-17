#!/usr/bin/env bash
# Версия из тега vX.Y.Z (08 §7.3): versionName = X.Y.Z, versionCode = X·1e6 + Y·1e3 + Z — монотонность даёт сам тег.
#   version-from-tag.sh [vX.Y.Z]   без аргумента — тег на HEAD. Вывод пригоден для eval: APP_VERSION=… APP_BUILD=…
set -euo pipefail
tag="${1:-$(git describe --tags --exact-match HEAD 2>/dev/null || true)}"
[ -n "$tag" ] || { echo "FAIL: на HEAD нет тега — передай vX.Y.Z или поставь аннотированный тег" >&2; exit 2; }
[[ "$tag" =~ ^v([0-9]+)\.([0-9]+)\.([0-9]+)$ ]] || { echo "FAIL: тег '$tag' не соответствует vX.Y.Z" >&2; exit 2; }
major=${BASH_REMATCH[1]}; minor=${BASH_REMATCH[2]}; patch=${BASH_REMATCH[3]}
# Play принимает versionCode < 2 100 000 000, а разряды minor/patch — три цифры.
{ [ "$major" -le 2099 ] && [ "$minor" -le 999 ] && [ "$patch" -le 999 ]; } \
    || { echo "FAIL: $tag не помещается в versionCode = X·1e6 + Y·1e3 + Z (X ≤ 2099, Y,Z ≤ 999)" >&2; exit 2; }
echo "APP_VERSION=$major.$minor.$patch"
echo "APP_BUILD=$((major * 1000000 + minor * 1000 + patch))"
