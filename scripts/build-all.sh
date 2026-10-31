#!/usr/bin/env bash
# YunSeek one-click build: Open build (no protected parts, no local API), zipalign, NO signature.
# Output: out/YunSeek-<ver>-unsigned-aligned.apk + out/SHA256.txt. Sign it yourself with your own .jks.
# The original multi-module pipeline is kept as scripts/build-all-legacy.sh.
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
bash "$ROOT/scripts/gradle-open-debug.sh"
source /workspace/work/tools/env.sh 2>/dev/null || true
APK="$ROOT/out/YunSeek-1.8.1-unsigned-aligned.apk"
if "$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner" verify "$APK" >/dev/null 2>&1; then
  echo "ERROR: APK still carries a signature" >&2; exit 1
fi
echo "OK: unsigned + zipalign -> $APK"
