#!/bin/sh
set -eu

ROOT="$(git rev-parse --show-toplevel)"
APP="$ROOT/Radio.TV.Control"
APK="$APP/app/build/outputs/apk/debug/app-debug.apk"
OUT="$APP/ui-screenshots"
echo "Capturing UI screenshots at equivalent 540x1200/220dpi and 640x400/100dpi viewports."
wait_for_main_activity() {
  timeout=${1:-90}
  elapsed=0
  while [ "$elapsed" -lt "$timeout" ]; do
    if adb logcat -d -v brief 2>/dev/null | grep -Fq "Displayed com.radiotv.control/.MainActivity"; then
      sleep 2
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  echo "MainActivity did not report Displayed within ${timeout}s; preserving current screen and logcat." >&2
  return 1
}

if [ ! -s "$APK" ]; then
  echo "Debug APK was not found: $APK" >&2
  exit 1
fi

mkdir -p "$OUT"

adb shell wm size 540x1200
adb shell wm density 220
adb install -r "$APK"
adb shell pm grant com.radiotv.control android.permission.ACCESS_LOCAL_NETWORK > "$OUT/permission-grant.txt" 2>&1 || true
adb logcat -c
adb shell am force-stop com.radiotv.control
adb shell am start -n com.radiotv.control/.MainActivity > "$OUT/am-start.txt" 2>&1 || true
wait_for_main_activity 90 || true
adb shell dumpsys activity activities > "$OUT/activities.txt" 2>&1 || true
adb shell pidof com.radiotv.control > "$OUT/pid.txt" 2>&1 || true
adb logcat -d -v time > "$OUT/logcat.txt" 2>&1 || true
adb exec-out screencap -p > "$OUT/remote.png"

# Enter Control once. Discovery starts on tab entry (not behind the remote screen).
adb shell input tap 202 1138
sleep 5
# Store a UIAutomator hierarchy and a machine-readable check to ensure this is the
# Control/search page, not just the remote with a pressed navigation ripple.
adb shell uiautomator dump /sdcard/device-search-ui.xml > "$OUT/uiautomator-dump.txt" 2>&1 || true
adb shell cat /sdcard/device-search-ui.xml > "$OUT/device-search-ui.xml" 2>/dev/null || true
if grep -Fq 'КОНТРОЛЬ УСТРОЙСТВ' "$OUT/device-search-ui.xml"; then
  echo "PASS: control/search screen visible" > "$OUT/device-search-screen-check.txt"
else
  echo "WARN: control/search title not present in UI hierarchy; inspect PNG and diagnostics" > "$OUT/device-search-screen-check.txt"
fi
adb exec-out screencap -p > "$OUT/device-search.png"
adb logcat -d -v time > "$OUT/navigation-logcat.txt" 2>&1 || true

# Settings screenshot also verifies that a second tab remains reachable.
adb shell input tap 472 1138
sleep 4
adb exec-out screencap -p > "$OUT/bottom-navigation.png"

adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb shell wm size 640x400
adb shell wm density 100
adb shell am force-stop com.radiotv.control
adb logcat -c
adb shell am start -n com.radiotv.control/.MainActivity > "$OUT/landscape-start.txt" 2>&1 || true
wait_for_main_activity 90 || true
adb shell dumpsys activity activities > "$OUT/landscape-activities.txt" 2>&1 || true
adb logcat -d -v time > "$OUT/landscape-logcat.txt" 2>&1 || true
adb exec-out screencap -p > "$OUT/large-screen-landscape.png"

for file in remote.png device-search.png bottom-navigation.png large-screen-landscape.png; do
  if [ ! -s "$OUT/$file" ]; then
    echo "Screenshot is missing or empty: $OUT/$file" >&2
    exit 1
  fi
done
