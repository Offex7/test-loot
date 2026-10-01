# UniMote v1.4.1 — install failure diagnosis

## Original APK

SHA-256: 8a2cc33198cf27a9be326e96fc80472de1bdc6d9e0c79cf406d2d81fc7a21964
Size: 8,236,995 bytes
ZIP test: PASS
Entries: 1108
AndroidManifest.xml: unchanged baseline
Manifest package/version:
- package: sensustech.universal.tv.remote.control
- versionCode: 24
- versionName: 1.4.1
- minSdkVersion: 16
- targetSdkVersion: 30

resources.arsc:
- compression: STORED
- data offset: 7,469,576
- offset % 4: 0

APK signing:
- V1: STRANNIK certificate present and structurally valid
- V2: present
- V3: present

## Previous broken APK

SHA-256: 1325e5f08d11d718197fa61346c8058a91c22fc7482e5d87006fcfeea687e14b
Size: 8,087,726 bytes
ZIP test: PASS
Entries: 1108
AndroidManifest.xml: byte-for-byte identical to original

resources.arsc:
- compression: DEFLATED
- data offset: 7,857,192
- offset % 4: 0

APK signing:
- V1: TV-PULT, jarsigner verification PASS
- V2: ABSENT
- V3: ABSENT

Therefore the previous APK had TWO independent Android 11+/targetSdk 30 installation blockers:
1. resources.arsc was compressed.
2. The APK was V1-only and had no V2/V3 APK Signing Block.

The lack of V2/V3 is independently fatal for apps targeting Android 11/API 30+. Android's compatibility requirements also reject compressed or non-4-byte-aligned resources.arsc.

## Runtime log limitation

adb is not installed in the execution environment:
- adb: command not found
- No physical device or Android emulator is available.

Therefore there is no genuine adb install/logcat line from a device in this environment. I will not fabricate one. The installation cause above is established from direct APK inspection and official Android compatibility requirements.

## Additional previous-patch defects found

The prior patch also had two functional mistakes unrelated to the package-manager rejection:
- the reconnect listener was instantiated/attached with an invalid register usage;
- the new duplication row used settings_i_share instead of the original settings_i_connect icon.

Both were removed by rebuilding from the original APK.

