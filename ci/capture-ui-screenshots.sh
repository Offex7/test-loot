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
timeout 10s adb start-server >/dev/null 2>&1 || true
timeout 30s adb wait-for-device
# Do not touch the emulator display until Android's system boot is fully complete.
boot_completed=0
attempt=0
while [ "$attempt" -lt 60 ]; do
  boot_completed="$(timeout 8s adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  if [ "$boot_completed" = "1" ]; then
    break
  fi
  sleep 2
  attempt=$((attempt + 1))
done
[ "$boot_completed" = "1" ] || { echo "Android did not report sys.boot_completed=1 within 120 seconds." >&2; exit 1; }

# Avoid failing the whole emulator action if Android Settings briefly loses Binder/ADB during first boot.
# The reactivecircus action's built-in animation setup was fatal on API 35 (Broken pipe), so these are best-effort.
for setting in window_animation_scale transition_animation_scale animator_duration_scale; do
  timeout 8s adb shell settings put global "$setting" 0 >/dev/null 2>&1 || echo "Warning: could not set $setting; continuing."
done
timeout 8s adb shell wm size 540x1200 >/dev/null 2>&1 || echo "Warning: could not set 540x1200; using default display size."
timeout 8s adb shell wm density 220 >/dev/null 2>&1 || echo "Warning: could not set 220 dpi; using default display density."
timeout 8s adb shell settings put global stay_on_while_plugged_in 7 >/dev/null 2>&1 || true
timeout 8s adb shell svc power stayon true >/dev/null 2>&1 || true
timeout 8s adb shell settings put system screen_brightness_mode 0 >/dev/null 2>&1 || true
timeout 8s adb shell settings put system screen_brightness 255 >/dev/null 2>&1 || true
timeout 90s adb install -r "$APK"
timeout 8s adb shell pm grant "$PACKAGE" android.permission.ACCESS_LOCAL_NETWORK >/dev/null 2>&1 || true

# Verify that the emulator can render a system screen before blaming the app or publishing screenshots.
echo "Checking emulator rendering on the Android Home screen."
timeout 8s adb shell input keyevent 3 >/dev/null 2>&1 || true
sleep 3
home_check="$OUT/.emulator-home-check.png"
if ! timeout 20s adb exec-out screencap -p > "$home_check"; then
  echo "Could not capture the emulator Home screen." >&2
  exit 1
fi
if ! python3 "$ROOT/ci/validate-screenshot.py" "$home_check"; then
  echo "The emulator Home screen is also blank; this is an emulator/display failure, not a Radio.TV.Control screenshot." >&2
  timeout 8s adb shell dumpsys window windows >&2 || true
  timeout 8s adb logcat -d -t 500 -v brief 2>&1 | grep -E "SurfaceFlinger|OpenGLRenderer|EGL_emulation|ANR in|AndroidRuntime" | tail -n 100 >&2 || true
  exit 1
fi
rm -f "$home_check"

wake_and_unlock() {
  timeout 5s adb shell input keyevent 224 >/dev/null 2>&1 || true
  timeout 5s adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
  timeout 5s adb shell input keyevent 82 >/dev/null 2>&1 || true
  timeout 5s adb shell input swipe 270 1080 270 260 350 >/dev/null 2>&1 || true
}

recover_system_dialog() {
  xml="$OUT/.window-hierarchy.xml"
  if timeout 8s adb shell uiautomator dump /sdcard/radiotv-window.xml >/dev/null 2>&1 && timeout 5s adb shell cat /sdcard/radiotv-window.xml > "$xml" 2>/dev/null; then
    if grep -q "Process system isn.t responding" "$xml"; then
      # With the 540x1200 viewport, Androids system ANR dialog places "Wait" at this location.
      echo "System ANR dialog detected; tapping Wait." >&2
      timeout 5s adb shell input tap 270 667 >/dev/null 2>&1 || true
      sleep 6
      timeout 8s adb shell uiautomator dump /sdcard/radiotv-window.xml >/dev/null 2>&1 || true
      timeout 5s adb shell cat /sdcard/radiotv-window.xml > "$xml" 2>/dev/null || true
      if grep -q "Process system isn.t responding" "$xml" 2>/dev/null; then
        echo "System ANR dialog remains; refusing to publish a blank frame." >&2
        return 1
      fi
    fi
  else
    echo "UI hierarchy dump timed out; continuing to bounded capture and pixel validation." >&2
  fi
  return 0
}

wait_for_foreground() {
  elapsed=0
  while [ "$elapsed" -lt 45 ]; do
    wake_and_unlock
    if timeout 6s adb shell dumpsys activity activities 2>/dev/null | grep -E "mResumedActivity.*$PACKAGE|topResumedActivity=.*$PACKAGE" >/dev/null; then
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  echo "MainActivity for $PACKAGE did not reach foreground in 45 seconds." >&2
  timeout 8s adb logcat -d -t 700 -v brief 2>&1 | grep -E "$PACKAGE|FATAL EXCEPTION|AndroidRuntime|ANR in" | tail -n 100 >&2 || true
  timeout 8s adb shell dumpsys activity activities >&2 || true
  return 1
}
capture_screen() {
  screen="$1"
  filename="$2"
  temp="$OUT/.${filename}.tmp.png"
  echo "Capturing $screen -> $filename"
  timeout 8s adb shell am force-stop "$PACKAGE" >/dev/null 2>&1 || true
  wake_and_unlock
  # am start -W times out on this headless emulator even after dispatching the intent.
  timeout 12s adb shell am start -n "$ACTIVITY" --es ui-screen "$screen"
  wait_for_foreground
  wake_and_unlock
  recover_system_dialog
  sleep 5
  # Capture directly over ADB to avoid a second remote-file/pull operation.
  # A blank or stalled emulator is a hard failure; bounded diagnostics are attached to the CI log.
  if ! timeout 20s adb exec-out screencap -p > "$temp"; then
    echo "ADB screencap failed or timed out for $filename; collecting bounded diagnostics." >&2
    timeout 6s adb shell dumpsys power >&2 || true
    timeout 6s adb shell dumpsys window windows >&2 || true
    timeout 8s adb logcat -d -t 500 -v brief 2>&1 | grep -E "$PACKAGE|FATAL EXCEPTION|AndroidRuntime|ANR in|system_server|SurfaceFlinger" | tail -n 80 >&2 || true
    exit 1
  fi
  test -s "$temp" || { echo "Missing screenshot: $filename" >&2; exit 1; }
  # Decode the screenshot with Python stdlib and reject blank frames even if ImageMagick is absent.
  if ! python3 "$ROOT/ci/validate-screenshot.py" "$temp"; then
    echo "Diagnostic dump for blank screenshot: $filename" >&2
    adb shell dumpsys power >&2 || true
    adb shell dumpsys window windows >&2 || true
    adb shell dumpsys activity activities >&2 || true
    adb shell uiautomator dump /sdcard/radiotv-window.xml >/dev/null 2>&1 || true
    adb shell cat /sdcard/radiotv-window.xml >&2 || true
    adb logcat -d -t 1200 -v brief 2>&1 | grep -E "$PACKAGE|FATAL EXCEPTION|AndroidRuntime|ANR in|OpenGLRenderer|EGL_emulation|SurfaceFlinger" | tail -n 180 >&2 || true
    exit 1
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
