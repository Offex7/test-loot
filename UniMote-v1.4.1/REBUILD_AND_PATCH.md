# UniMote v1.4.1 — clean repair record

The repair was rebuilt from the original `UniMote_v1.4.1.apk`, not from the previous broken APK.

Requested changes implemented:
- User-visible `UniMote` -> `TV пульт`.
- Existing developer/signature UI was not found, so no new field was added.
- Settings tab/navigation position and icon are preserved.
- SettingsAdapter item count is set to 0, leaving the tab empty.
- The original Settings item 0 ("Переподключиться") was identified from its `settings_i_connect` icon and `reconnect` string.
- Its original premium gate / toast / PremiumActivity navigation was reproduced in the duplication listener without changing the original behavior of existing duplication buttons.
- The new duplication card uses the original `settings_i_connect` drawable.
- Embedded `assets/stream.html` title is `TV пульт`.

Important validation details:
- AndroidManifest.xml is byte-for-byte identical to the original.
- Modified fragment_mirrroring.xml passes a binary-AXML structural parser.
- DEX headers/signatures/checksums are internally valid.
- resources.arsc is STORED and 4-byte aligned.
- APK contains valid V1/JAR plus V2 and V3 signing.
- The signing order is V1 sign -> zipalign -> V2/V3 sign.
- New signing certificate SHA-256:
  BF4339FF52EB70E584FF2F6B669861962E2D1F0EC1B4FA54475481A238351029

Environment limitation:
The standard `apktool`, `zipalign`, `apksigner`, `adb` and `jadx` binaries were not present in the execution environment. A Python ZIP alignment implementation and a standalone APK V2/V3 verifier were used to reproduce the required archive/signing invariants. No device/emulator runtime test was possible because adb and an emulator/device were unavailable.
