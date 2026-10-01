# UniMote v1.4.1 — final2 install candidate

Canonical local APK: TV_pult_v1.4.1.apk
SHA-256: 8857183527224824a27274fc4bcfddebc6e51949a3287cfbe2a46c577a739f2e

This build is produced from the original working UniMote_v1.4.1.apk.

## Install-failure repair
The previous APK shown in the user's screenshot has a malformed resources.arsc global string pool boundary:
- global pool size 140841
- following ResTable_package begins at 140853
- both are not 4-byte aligned
- the package payload after the shifted boundary matches the original byte-for-byte

The corrected build:
- global pool size 140844
- first ResTable_package begins at 140856
- resources.arsc total size 598304
- ZIP_STORED and data offset is 4-byte aligned

The supplied screenshot has only the generic Package Installer message and no numeric code. adb/logcat is unavailable in the execution environment, so no device log is claimed.

## App changes
- visible UniMote/Unimote -> TV пульт
- WebView title -> TV пульт
- Settings navigation slot/icon preserved; list empty
- Reconnect moved to Duplication
- reconnect row uses original settings_i_connect
- reconnect branch preserves the original Settings position-1 disconnect sequence and PremiumActivity navigation
- existing Duplication actions unchanged
- Manifest and package metadata preserved
- all permissions/services/receivers/intent-filters preserved

## Signature
- V1/JAR: PASS
- V2: PASS
- V3: PASS
- V3 minSdk: 16
- V3 maxSdk: 2147483647
- certificate SHA-256: BF:43:39:FF:52:EB:70:E5:84:FF:2F:6B:66:98:61:96:2E:2D:1F:0E:C1:B4:FA:54:47:54:81:A2:38:35:10:29

## Environment limitation
Standard apktool/apksigner/zipalign/aapt/aapt2/adb/jadx executables and an Android device/emulator are unavailable here. The final APK therefore has no physical adb-install confirmation from this environment.
