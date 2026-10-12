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
adb shell settings put system screen_brightness_mode 0 >/dev/null 2>&1 || true
adb shell settings put system screen_brightness 255 >/dev/null 2>&1 || true
timeout 90s adb install -r "$APK"
adb shell pm grant "$PACKAGE" android.permission.ACCESS_LOCAL_NETWORK >/dev/null 2>&1 || true

wake_and_unlock() {
  adb shell input keyevent 224 >/dev/null 2>&1 || true
  adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
  adb shell input keyevent 82 >/dev/null 2>&1 || true
  # A swipe is needed on some headless images even when the lock screen has no PIN.
  adb shell input swipe 270 1080 270 260 350 >/dev/null 2>&1 || true
}

recover_system_dialog() {
  attempt=1
  while [ "$attempt" -le 5 ]; do
    timeout 15s adb shell uiautomator dump /sdcard/radiotv-window.xml >/dev/null 2>&1 || { sleep 2; attempt=$((attempt + 1)); continue; }
    timeout 5s adb shell cat /sdcard/radiotv-window.xml > "$OUT/.window-hierarchy.xml" 2>/dev/null || { sleep 2; attempt=$((attempt + 1)); continue; }
    if grep -q "Process system isn.t responding" "$OUT/.window-hierarchy.xml"; then
      point="$(python3 - "$OUT/.window-hierarchy.xml" <<'PY'
import re, sys
xml = open(sys.argv[1], encoding="utf-8", errors="replace").read()
m = re.search(r'text="Wait"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
if not m:
    m = re.search(r'text="Wait"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]".{0,300}?', xml)
if m:
    x1, y1, x2, y2 = map(int, m.groups())
    print((x1+x2)//2, (y1+y2)//2)
PY
      )"
      if [ -n "$point" ]; then
        echo "System-not-responding dialog detected; selecting Wait (attempt $attempt)." >&2
        adb shell input tap $point >/dev/null 2>&1 || true
        sleep 10
      else
        echo "System-not-responding dialog detected but Wait button could not be located." >&2
        cat "$OUT/.window-hierarchy.xml" >&2 || true
        return 1
      fi
    else
      return 0
    fi
    attempt=$((attempt + 1))
  done
  echo "System-not-responding dialog persists after five Wait attempts." >&2
  cat "$OUT/.window-hierarchy.xml" >&2 || true
  return 1
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
  recover_system_dialog
  sleep 5
  timeout 15s adb shell screencap -p /sdcard/radiotv-capture.png
  timeout 15s adb pull /sdcard/radiotv-capture.png "$temp" >/dev/null
  test -s "$temp" || { echo "Missing screenshot: $filename" >&2; exit 1; }
  # Decode the screenshot with Python stdlib and reject blank frames even if ImageMagick is absent.
  if ! python3 - "$temp" <<'PY'
import struct, sys, zlib

path = sys.argv[1]
data = open(path, "rb").read()
if data[:8] != b"\x89PNG\r\n\x1a\n":
    raise SystemExit("Screenshot is not a PNG.")
pos, idat, ihdr = 8, bytearray(), None
while pos < len(data):
    length = struct.unpack(">I", data[pos:pos+4])[0]
    kind = data[pos+4:pos+8]
    chunk = data[pos+8:pos+8+length]
    pos += 12 + length
    if kind == b"IHDR":
        ihdr = struct.unpack(">IIBBBBB", chunk)
    elif kind == b"IDAT":
        idat.extend(chunk)
    elif kind == b"IEND":
        break
if ihdr is None:
    raise SystemExit("PNG has no IHDR.")
width, height, depth, colour, compression, filtering, interlace = ihdr
if depth != 8 or colour not in (2, 6) or interlace != 0:
    raise SystemExit(f"Unsupported screenshot PNG format: depth={depth}, type={colour}, interlace={interlace}.")
bpp = 4 if colour == 6 else 3
stride = width * bpp
raw = zlib.decompress(idat)
previous = bytearray(stride)
cursor = 0
colours = set()
total = 0
pixel_count = width * height

def paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p-a), abs(p-b), abs(p-c)
    return a if pa <= pb and pa <= pc else b if pb <= pc else c

for _ in range(height):
    ft = raw[cursor]
    cursor += 1
    row = bytearray(raw[cursor:cursor+stride])
    cursor += stride
    for i in range(stride):
        left = row[i-bpp] if i >= bpp else 0
        above = previous[i]
        upper_left = previous[i-bpp] if i >= bpp else 0
        if ft == 1:
            row[i] = (row[i] + left) & 255
        elif ft == 2:
            row[i] = (row[i] + above) & 255
        elif ft == 3:
            row[i] = (row[i] + ((left + above) // 2)) & 255
        elif ft == 4:
            row[i] = (row[i] + paeth(left, above, upper_left)) & 255
        elif ft != 0:
            raise SystemExit(f"Unsupported PNG filter: {ft}.")
    for i in range(0, stride, bpp):
        rgb = (row[i], row[i+1], row[i+2])
        total += rgb[0] + rgb[1] + rgb[2]
        if len(colours) < 64:
            colours.add(rgb)
    previous = row
mean_rgb = total / (pixel_count * 3)
print(f"Screenshot check: {width}x{height}, sampled-colours={len(colours)}, mean-RGB={mean_rgb:.2f}")
if len(colours) < 16 or mean_rgb < 3.0:
    raise SystemExit("Screenshot appears blank/undrawn; refusing to publish it.")
PY
  then
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
