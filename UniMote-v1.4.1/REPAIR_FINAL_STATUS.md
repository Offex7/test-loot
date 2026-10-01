# UniMote v1.4.1 — final corrected rebuild

Final local artifact:
UniMote_TV_pult_v1.4.1_FIXED.apk
SHA-256: f700331f72842af7aa357c88b78cb65e081095df42f8a9eaf6b2cff6d39fe623

Implemented from the original APK, not from the broken build:
- UniMote -> TV пульт for user-visible text
- embedded stream.html title -> TV пульт
- Settings navigation slot/icon/position preserved
- Settings content made empty
- Reconnect added to Duplication using the original settings_i_connect resource
- the reconnect layout is present in base, v17 and sw600dp variants
- original duplication actions remain untouched
- AndroidManifest.xml content preserved byte-for-byte
- permissions, services, receivers and intent-filters were not changed

Signing key:
- path: app-independent local signing/TV_pult_release.keystore
- alias: tv-pult
- certificate SHA-256: BF:43:39:FF:52:EB:70:E5:84:FF:2F:6B:66:98:61:96:2E:2D:1F:0E:C1:B4:FA:54:47:54:81:A2:38:35:10:29

The GitHub connector available in this workspace can commit text/source metadata but cannot create a new repository or upload local binary artifacts/releases. The complete local repair archive is therefore supplied as a conversation artifact.

adb/device runtime testing remains pending because adb and an Android device/emulator are unavailable here.
