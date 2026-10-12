#!/bin/sh
set -eu

ROOT="$(git rev-parse --show-toplevel)"
APP="$ROOT/Radio.TV.Control"
APK="$APP/app/build/outputs/apk/debug/app-debug.apk"
OUT="$APP/ui-screenshots"
PACKAGE="com.radiotv.control"
ACTIVITY="$PACKAGE/.MainActivity"

echo "Capturing the three requested UI screens."
test -s "$APK" || { echo "Debug APK not found: $APK" >&2; exit 1; }
mkdir -p "$OUT"
adb shell wm size 540x1200
adb shell wm density 220
adb shell settings put global stay_on_while_plugged_in 7 >/dev/null 2>&1 || true
adb shell svc power stayon true >/dev/null 2>&1 || true
timeout 90s adb install -r "$APK"
adb shell pm grant "$PACKAGE" android.permission.ACCESS_LOCAL_NETWORK >/dev/null 2>&1 || true

wake_and_unlock() {
  adb shell input keyevent 224 >/dev/null 2>&1 || true
  adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
  adb shell input keyevent 82 >/dev/null 2>&1 || true
}

wait_for_foreground() {
  elapsed=0
  while [ "$elapsed" -lt 45 ]; do
    wake_and_unlock
    if adb shell dumpsys activity activities 2>/dev/null | grep -E "mResumedActivity.*$PACKAGE|topResumedActivity=.*$PACKAGE" >/dev/null; then
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  echo "MainActivity for $PACKAGE did not reach foreground in 45 seconds." >&2
  adb logcat -d -t 700 -v brief 2>&1 | grep -E "$PACKAGE|FATAL EXCEPTION|AndroidRuntime|ANR in" | tail -n 100 >&2 || true
  adb shell dumpsys activity activities >&2 || true
  return 1
}

capture_screen() {
  screen="$1"
  filename="$2"
  temp="$OUT/.${filename}.tmp.png"
  echo "Capturing $screen -> $filename"
  timeout 15s adb shell am force-stop "$PACKAGE" >/dev/null 2>&1 || true
  wake_and_unlock
  # am start -W times out on this headless emulator even after dispatching the intent.
  timeout 15s adb shell am start -n "$ACTIVITY" --es ui-screen "$screen"
  wait_for_foreground
  wake_and_unlock
  sleep 5
  timeout 15s adb exec-out screencap -p > "$temp"
  test -s "$temp" || { echo "Missing screenshot: $filename" >&2; exit 1; }
  # Reject an all-black/empty render instead of publishing it as a successful UI capture.
  if command -v identify >/dev/null 2>&1; then
    colors="$(identify -format '%k' "$temp" 2>/dev/null || echo 0)"
    mean="$(identify -format '%[fx:mean]' "$temp" 2>/dev/null || echo 0)"
    python3 - "$colors" "$mean" <<'PY'
import sys
colors = int(sys.argv[1])
mean = float(sys.argv[2])
if colors < 16 or mean < 0.015:
    raise SystemExit(f"Screenshot looks blank (colors={colors}, mean={mean:.4f}).")
PY
  fi
  mv "$temp" "$OUT/$filename"
  file "$OUT/$filename"
}

capture_screen remote remote.png
capture_screen digital digital-panel.png
capture_screen pairing settings.png

for file in remote.png digital-panel.png settings.png; do
  test -s "$OUT/$file"
done
