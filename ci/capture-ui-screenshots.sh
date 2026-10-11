#!/bin/sh
set -eu

ROOT="$(git rev-parse --show-toplevel)"
APP="$ROOT/Radio.TV.Control"
APK="$APP/app/build/outputs/apk/debug/app-debug.apk"
OUT="$APP/ui-screenshots"
PACKAGE="com.radiotv.control"
ACTIVITY="$PACKAGE/.MainActivity"

echo "Capturing onboarding, remote, digital keypad, pairing and API 37 large-screen UI."
if [ ! -s "$APK" ]; then
  echo "Debug APK was not found: $APK" >&2
  exit 1
fi

mkdir -p "$OUT"
adb shell wm size 540x1200
adb shell wm density 220
adb install -r "$APK"
adb shell pm grant "$PACKAGE" android.permission.ACCESS_LOCAL_NETWORK > "$OUT/permission-grant.txt" 2>&1 || true

wait_for_main_activity() {
  timeout=90
  elapsed=0
  while [ "$elapsed" -lt "$timeout" ]; do
    if adb logcat -d -v brief 2>/dev/null | grep -Fq "Displayed $ACTIVITY"; then
      sleep 2
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  echo "MainActivity did not report Displayed within $timeout seconds." >&2
  return 1
}

capture_screen() {
  screen="$1"
  filename="$2"
  echo "Capturing screen: $screen -> $filename"
  adb shell am force-stop "$PACKAGE"
  adb logcat -c
  adb shell am start -W -n "$ACTIVITY" --es ui-screen "$screen" > "$OUT/$filename-start.txt" 2>&1 || true
  wait_for_main_activity || true
  sleep 2
  adb exec-out screencap -p > "$OUT/$filename"
  adb shell uiautomator dump /sdcard/current-ui.xml > "$OUT/$filename-uiautomator.txt" 2>&1 || true
  adb shell cat /sdcard/current-ui.xml > "$OUT/$filename-ui.xml" 2>/dev/null || true
  if [ ! -s "$OUT/$filename" ]; then
    echo "Screenshot is missing or empty: $OUT/$filename" >&2
    exit 1
  fi
}

capture_screen onboarding onboarding.png
capture_screen remote remote.png
capture_screen digital digital-panel.png
capture_screen pairing pairing.png
capture_screen settings settings.png

adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb shell wm size 1280x800
adb shell wm density 160
capture_screen remote large-screen-landscape.png

adb logcat -d -v time > "$OUT/logcat.txt" 2>&1 || true
adb shell dumpsys activity activities > "$OUT/activities.txt" 2>&1 || true
for file in onboarding.png remote.png digital-panel.png pairing.png settings.png large-screen-landscape.png; do
  test -s "$OUT/$file"
done
