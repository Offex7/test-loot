# Radio.TV.Control

Standalone Android remote prototype designed to become a Radio.TV feature module.

## Project layout

- `:app` — standalone demo host; app label `Radio.TV.Control`.
- `:tvremote-core` — transport contracts, command models and Android TV Remote v2 TLS/protobuf transport.
- `:tvremote-ui` — reusable Jetpack Compose remote pad.
- `:tvremote-cast` — DLNA primitives and local-network discovery.

**Package:** `com.radiotv.control`  
**Minimum Android:** 12 / API 31  
**Compile/target SDK:** Android 17 / API 37  
**Orientation:** portrait requested in the manifest. Content is capped at 520 dp and centered on wider screens; Android 17 may ignore forced orientation on large-screen devices.

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

## Current implementation

- Material 3 dark UI, TV IP fallback, pairing-code input and remote controls.
- Automatic local-network discovery using Android NSD/mDNS for `_androidtvremote2._tcp`, `_googlecast._tcp`, `_airplay._tcp`; SSDP/M-SEARCH; and bounded TCP probing of ports 6466, 6467, 8008, 8009, 9080, 8060, 8001, 8002, 3000 and 3001.
- Subnet probing is deliberately bounded to the current /24 to avoid flooding larger networks. NetBIOS and ARP-table inspection are not used.
- Best-effort type estimates for Android TV, Google Cast, Samsung, LG, Roku, DLNA, AirPlay and unknown devices. A detected type does not prove its remote-control protocol works.
- Android TV Remote v2 TLS/pairing/control-channel foundation and basic key injection.
- Red/black Material 3 palette, compact side-mounted volume/channel controls, centered D-pad, 48 dp minimum control targets, digit pad and tactile feedback.
- R8/ProGuard minification and resource shrinking enabled for debug and release APKs.
- Public Compose API `TvRemotePad(enabled, onKey, modifier)`.

## Not implemented / not verified on hardware

- Bluetooth HID peripheral registration and HID reports.
- Voice PCM capture/streaming and complete TV IME injection.
- Working Samsung Tizen, LG webOS, Roku ECP adapters and IR hardware.
- DLNA AVTransport `SetAVTransportURI` / `Play` / `Stop`.
- Touchpad gestures and gyroscope air mouse (UI entry points are present; operation remains unimplemented).
- Physical TV pairing and real command delivery have not been tested; CI checks compilation, unit tests, APK signature/manifest and class presence.

## Integration in Radio.TV (stage 2)

Move the modules into Radio.TV and add:

```kotlin
implementation(project(":tvremote-core"))
implementation(project(":tvremote-ui"))
implementation(project(":tvremote-cast"))
```

Embed the shared Compose controls:

```kotlin
TvRemotePad(
    enabled = remoteIsConnected,
    onKey = { key -> lifecycleScope.launch { transport.sendKey(key) } }
)
```

Connection and protocol state stay in `:tvremote-core`; UI does not depend on the demo app. For media handoff, implement `RadioTvMediaProvider.currentMedia(): CastMedia?` after DLNA AVTransport is added.

## CI

GitHub Actions builds `:app:assembleDebug`, runs `:tvremote-core:test`, verifies APK signature and SDK/label/orientation/cleartext/local-network manifest values, scans DEX for remote/UI/cast/discovery classes, computes SHA-256 and APK size, and uploads `Radio.TV.Control-debug-run-N`. The artifact is debug-signed and intended for testing rather than Play Store release.

-keep class com.radiotv.control.core.BluetoothHidController { *; }
-keep class com.radiotv.control.core.HidKeymapKt { *; }
