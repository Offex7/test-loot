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
adb install -r "$APK"
adb shell pm grant "$PACKAGE" android.permission.ACCESS_LOCAL_NETWORK >/dev/null 2>&1 || true

wait_for_foreground() {
  elapsed=0
  while [ "$elapsed" -lt 40 ]; do
    if adb shell dumpsys activity activities 2>/dev/null | grep -E "mResumedActivity.*$PACKAGE|topResumedActivity=.*$PACKAGE" >/dev/null; then
      sleep 2
      return 0
    fi
    if ! adb shell pidof "$PACKAGE" >/dev/null 2>&1; then
      # A cold start may take a moment before the process exists.
      :
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  echo "MainActivity for $PACKAGE did not reach foreground in 40 seconds." >&2
  adb logcat -d -t 500 -v brief 2>&1 | grep -E "$PACKAGE|FATAL EXCEPTION|AndroidRuntime" | tail -n 80 >&2 || true
  return 1
}

capture_screen() {
  screen="$1"
  filename="$2"
  echo "Capturing $screen -> $filename"
  adb shell am force-stop "$PACKAGE"
  adb shell am start -W -n "$ACTIVITY" --es ui-screen "$screen"
  wait_for_foreground
  sleep 2
  adb exec-out screencap -p > "$OUT/$filename"
  test -s "$OUT/$filename" || { echo "Missing screenshot: $OUT/$filename" >&2; exit 1; }
}

capture_screen remote remote.png
capture_screen digital digital-panel.png
# The pairing entry starts discovery but is mapped to the Settings tab, showing its skeleton state.
capture_screen pairing settings.png

for file in remote.png digital-panel.png settings.png; do
  test -s "$OUT/$file"
  file "$OUT/$file"
done
