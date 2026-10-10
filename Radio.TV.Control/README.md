# Radio.TV.Control

Standalone Android remote prototype designed to become a Radio.TV feature module.

## Project layout

- `:app` — standalone demo host; app label `Radio.TV.Control`.
- `:tvremote-core` — transport contracts, command models, Android TV Remote v2, and Bluetooth HID.
- `:tvremote-ui` — reusable Jetpack Compose remote pad.
- `:tvremote-cast` — local network discovery and DLNA discovery foundation.

**Package:** `com.radiotv.control`  
**Minimum Android:** 12 / API 31  
**Compile/target SDK:** Android 17 / API 37  
**Orientation:** portrait requested by manifest. Content is capped at 520 dp and centered on wider screens. Android 17 may ignore forced orientation on large-screen devices; physical tablet letterbox behavior still needs validation.

## Build

Requires JDK 17, Android SDK platform 37 and compatible Android Gradle Plugin 9.x tooling. CI uses Gradle 9.5.0.

```bash
cd Radio.TV.Control
gradle :app:assembleDebug :tvremote-core:test
```

Install:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

R8 minification and resource shrinking are enabled for debug and release builds.

## Implemented in this baseline

- Red/black Material 3 palette with compact D-pad, side volume/channel controls, digit pad, media keys and haptic feedback.
- Automatic local network discovery using Android NSD/mDNS for `_androidtvremote2._tcp`, `_googlecast._tcp`, `_airplay._tcp`; SSDP/M-SEARCH; and bounded TCP probing of ports 6466, 6467, 8008, 8009, 9080, 8060, 8001, 8002, 3000 and 3001.
- Manual IP entry as a fallback. Subnet probing is deliberately capped to a /24; NetBIOS and ARP-table inspection are not implemented.
- Best-effort device-type detection for Android TV, Google Cast, Samsung, LG, Roku, DLNA, AirPlay and unknown devices. A detected type does not prove that the corresponding control protocol works.
- Android TV Remote v2 TLS/pairing/control-channel foundation and basic key injection.
- Bluetooth HID Device implementation with keyboard, media/volume Consumer Control and relative mouse reports, runtime permission flow and system discoverability prompt. TV-side connection is initiated from the TV's Bluetooth settings.
- Core unit tests for Remote key mapping and Bluetooth HID key mapping.
- Public Compose API `TvRemotePad(enabled, onKey, modifier, onFeatureAction)`.

## Not implemented / not verified on physical hardware

- Bluetooth HID connection and actual keyboard/mouse report delivery have not been tested with a real phone and TV. Some phone Bluetooth stacks may not expose the HID Device role.
- Touchpad panel and gyroscope air mouse are implemented for a connected Bluetooth HID host. Their gesture/sensor delivery still needs testing on a physical phone and TV.
- Voice PCM capture/streaming and complete Android TV Remote v2 text/IME injection are not implemented.
- Working Samsung Tizen, LG webOS, Roku ECP control adapters and external IR hardware are not implemented.
- DLNA AVTransport `SetAVTransportURI` / `Play` / `Stop` playback is not implemented; current code discovers possible renderers only.
- Real TV pairing and command delivery have not been physically tested. CI verifies compilation, unit tests, APK signing/manifest, selected DEX class presence, SHA-256 and byte size only.

## Integration in Radio.TV (stage 2)

Move the modules into the Radio.TV Gradle build and add:

```kotlin
implementation(project(":tvremote-core"))
implementation(project(":tvremote-ui"))
implementation(project(":tvremote-cast"))
```

Embed the shared Compose API in the host UI:

```kotlin
TvRemotePad(
    enabled = remoteIsConnected,
    onKey = { key -> lifecycleScope.launch { transport.sendKey(key) } }
)
```

Protocol and connection state stay in `:tvremote-core`; `:tvremote-ui` does not depend on the standalone app. For media handoff, implement `RadioTvMediaProvider.currentMedia(): CastMedia?` after DLNA AVTransport has been added.

## CI

GitHub Actions builds `:app:assembleDebug`, runs `:tvremote-core:test`, verifies APK signature and package/SDK/label/orientation/cleartext/local-network/Bluetooth permissions, scans DEX for transport/UI/discovery classes, computes SHA-256 and size, and uploads `Radio.TV.Control-debug-run-N`. The APK is debug-signed for testing, not a Play Store release.
