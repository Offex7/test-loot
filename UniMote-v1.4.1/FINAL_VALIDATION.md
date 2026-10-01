# Final validation — UniMote v1.4.1

Final APK:
`UniMote_TV_pult_v1.4.1.apk`

SHA-256:
`dc922efd78ed029321e688aeeefa94299870cc4ae35f5b7601fc12fa42edff2b`

Archive:
- ZIP test: PASS
- Entry count: 1108
- AndroidManifest.xml SHA-256 is identical to original:
  `991e0b956e2f5852ee66f1dfe2ca5b6d93aa79a2f6db4746289b490dd389cda8`
- Modified fragment_mirrroring.xml: binary AXML parser PASS
- DEX checksum/header validation: PASS
- resources.arsc: STORED
- resources.arsc data offset: 7,847,008
- resources.arsc offset % 4: 0

Signatures:
- V1/JAR: jarsigner verification PASS
- V2: PASS
- V3: PASS
- Certificate SHA-256:
  `bf4339ff52eb70e584ff2f6b669861962e2d1f0ec1b4fa54475481a238351029`

User-visible name scan:
- resources.arsc occurrences of `UniMote` replaced with `TV пульт`
- DEX user-visible name occurrences replaced with `TV пульт`
- technical identifiers `Theme_UniMote`, `UniMote.Network`, `UniMoteSDK` intentionally retained because they are internal identifiers, not display text
- WebView title changed to `TV пульт`

Modified non-signature entries:
- assets/stream.html
- classes.dex
- classes2.dex
- res/layout/fragment_mirrroring.xml
- resources.arsc

Not modified:
- AndroidManifest.xml
- permissions/services/receivers/intent-filters
- unrelated resources/files

Runtime:
No adb/device/emulator was available in the execution environment, so `adb install -r`, `adb logcat`, and on-device UI tests could not be truthfully reported as executed.
