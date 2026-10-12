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
  # A swipe is needed on some headless images even when the lock screen has no PIN.
  adb shell input swipe 270 1080 270 260 350 >/dev/null 2>&1 || true
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
  # Decode the screenshot with Python stdlib and reject blank frames even if ImageMagick is absent.
  python3 - "$temp" <<'PY'
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
  mv "$temp" "$OUT/$filename"
  file "$OUT/$filename"
}

capture_screen remote remote.png
capture_screen digital digital-panel.png
capture_screen pairing settings.png

for file in remote.png digital-panel.png settings.png; do
  test -s "$OUT/$file"
done
