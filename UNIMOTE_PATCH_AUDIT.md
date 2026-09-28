# UniMote v1.4.1 — TV пульт patch audit

Source APK: UniMote_v1.4.1.apk
Modified artifact SHA-256: 1325e5f08d11d718197fa61346c8058a91c22fc7482e5d87006fcfeea687e14b

Changes:
- User-visible app name changed to `TV пульт`.
- User-visible rating/feedback strings containing UniMote redirected to `TV пульт`.
- Embedded `assets/stream.html` title changed to `TV пульт`.
- Settings tab/navigation IDs and manifest declarations preserved.
- Settings content count changed from 7 to 0.
- Reconnect card added to Duplication using existing visual resources.
- Existing four duplication actions preserved.

Verification:
- AndroidManifest.xml unchanged byte-for-byte.
- ZIP test passed.
- Both DEX headers/checksums valid.
- APK JAR/V1 signature verified with a new Radio.TV keystore.
- Runtime device/emulator test not available in the execution environment.
- Original APK private signing key was not provided, so the modified APK cannot be installed over an already-installed original-signed build.

Important:
This audit commit records the work. The full binary/source project remains available as the conversation artifact because this GitHub connector cannot create a new repository or upload local binary files.
