# UniMote 1.4.1 → TV пульт

This branch contains the traceable patch notes for the supplied UniMote_v1.4.1 APK.

## Important execution limits

The current GitHub connection can manage the existing repository `Offex7/test-loot` but cannot create a new GitHub repository or upload binary release assets. For that reason this work is isolated to branch `unimote-v1.4.1` and the binary APK/keystore are supplied separately as local artifacts.

The execution environment also did not contain `apktool`, `apksigner`, Android SDK, `adb`, or an emulator. The APK was therefore patched at the DEX/resources/binary-XML level and signed with `jarsigner`. The APK/JAR signature and internal DEX checksums were verified statically; runtime device testing could not be performed here.

## Final artifact

APK SHA-256: 19717fde3a2632b84f583e6336549796787b787b176045cdbe8d0ef591af2a1a

Signing certificate SHA-256: BF:43:39:FF:52:EB:70:E5:84:FF:2F:6B:66:98:61:96:2E:2D:1F:0E:C1:B4:FA:54:47:54:81:A2:38:35:10:29

Signing subject: CN=TV пульт, OU=Radio.TV, O=Radio.TV, C=UA

## Installability caveat

The original APK was signed with a different certificate and its private key was not supplied. Therefore the modified APK cannot be installed as an update over an already-installed original build. Use the original private key to re-sign, or uninstall the old signed build before installing this one.

See `BASELINE.md`, `LABELS.md`, and `SETTINGS_AND_DUPLICATION.md` for the staged change record.
