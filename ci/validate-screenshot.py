#!/usr/bin/env python3
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
