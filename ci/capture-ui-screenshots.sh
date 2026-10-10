#!/bin/sh
set -eu

ROOT="$(git rev-parse --show-toplevel)"
APP="$ROOT/Radio.TV.Control"
APK="$APP/app/build/outputs/apk/debug/app-debug.apk"
OUT="$APP/ui-screenshots"

if [ ! -s "$APK" ]; then
  echo "Debug APK was not found: $APK" >&2
  exit 1
fi

mkdir -p "$OUT"

adb shell wm size 1080x2400
adb shell wm density 440
adb install -r "$APK"
adb shell pm grant com.radiotv.control android.permission.ACCESS_LOCAL_NETWORK > "$OUT/permission-grant.txt" 2>&1 || true
adb logcat -c
adb shell am force-stop com.radiotv.control
adb shell am start -n com.radiotv.control/.MainActivity > "$OUT/am-start.txt" 2>&1 || true
sleep 8
adb shell dumpsys activity activities > "$OUT/activities.txt" 2>&1 || true
adb shell pidof com.radiotv.control > "$OUT/pid.txt" 2>&1 || true
adb logcat -d -v time > "$OUT/logcat.txt" 2>&1 || true
adb exec-out screencap -p > "$OUT/remote.png"

adb shell input tap 405 2230
sleep 3
adb exec-out screencap -p > "$OUT/device-search.png"

adb shell input tap 945 2230
sleep 2
adb exec-out screencap -p > "$OUT/bottom-navigation.png"

adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb shell wm size 1280x800
adb shell wm density 200
adb shell am force-stop com.radiotv.control
adb shell am start -n com.radiotv.control/.MainActivity > "$OUT/landscape-start.txt" 2>&1 || true
sleep 5
adb shell dumpsys activity activities > "$OUT/landscape-activities.txt" 2>&1 || true
adb exec-out screencap -p > "$OUT/large-screen-landscape.png"

for file in remote.png device-search.png bottom-navigation.png large-screen-landscape.png; do
  if [ ! -s "$OUT/$file" ]; then
    echo "Screenshot is missing or empty: $OUT/$file" >&2
    exit 1
  fi
done
