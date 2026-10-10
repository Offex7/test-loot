#!/usr/bin/env python3
from pathlib import Path
import base64, hashlib, shutil

root = Path("app/src/main/res/drawable-nodpi/unistar_logo.jpg")
parts = Path("tools/unistar_logo_parts")
expected = "161d14948fb51c243d32ac1799fe881835ec4c94c070c89d928fecd4cc1eafb8"
if parts.exists():
    encoded = "".join(p.read_text(encoding="ascii").strip() for p in sorted(parts.glob("*.b64")))
    data = base64.b64decode(encoded, validate=True)
    actual = hashlib.sha256(data).hexdigest()
    if actual != expected:
        raise SystemExit(f"Unistar logo checksum mismatch: {actual}")
    root.write_bytes(data)
    shutil.rmtree(parts)
actual = hashlib.sha256(root.read_bytes()).hexdigest()
if actual != expected:
    raise SystemExit(f"Unistar logo checksum mismatch after install: {actual}")
print(f"Verified local Unistar logo SHA-256: {actual}")
