# Radio.TV.Control

Standalone Android remote prototype intended to become a Radio.TV feature module later.

## Project layout

- `:app` — standalone demo host; app label `Radio.TV.Control`.
- `:tvremote-core` — transport abstractions and Android TV Remote v2 TLS/protobuf transport.
- `:tvremote-ui` — reusable Jetpack Compose remote pad.
- `:tvremote-cast` — SSDP discovery foundation for DLNA renderers.

**Package:** `com.radiotv.control`  
**Minimum Android:** 12 / API 31  
**Compile/target SDK:** Android 17 / API 37  
**Orientation:** portrait requested in the manifest; the main viewport is capped at 520 dp and centered on wider screens. Android 17 targeting rules may ignore forced orientation on large-screen devices, so validate the tablet experience on real hardware.

## Build

Requires JDK 17, Android SDK platform 37 and Build Tools 36.0.0. The CI installs Gradle 9.5.0.

```bash
cd Radio.TV.Control
gradle :app:assembleDebug :tvremote-core:test
```

Install:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Implemented in this baseline

- Material 3 dark UI, manually entered TV IP, pairing-code field, D-pad, navigation, volume, channel, source, digit and media buttons.
- Android TV Remote v2 TLS connection path on ports 6467/6466, protobuf framing, generated client identity, pairing-code exchange, basic key injection and certificate pinning after pairing.
- Core key-mapping unit tests.
- SSDP M-SEARCH renderer discovery starter.
- Public Compose API `TvRemotePad(enabled, onKey, modifier)`, independent of the demo app.
- Android 17 runtime request for `ACCESS_LOCAL_NETWORK` before opening the TV connection.

## Not implemented / not physically verified yet

- Voice PCM capture/streaming and full TV IME text injection.
- Bluetooth HID, Samsung Tizen, LG webOS, Roku ECP and external IR hardware.
- DLNA AVTransport playback/DIDL-Lite handoff; current cast code only discovers renderer candidates.
- Touchpad, gyroscope air mouse, TV model-specific fallbacks and advanced discovery.
- Pairing compatibility across TV firmware variants. CI cannot verify live TV pairing, command delivery, Bluetooth HID roles or gyroscope readings.

A green CI run proves the build and configured static checks/tests pass; it does not prove real TV compatibility. Pairing must still be tested against physical Android TV / Google TV hardware.

## Integration in Radio.TV (stage 2)

Add these modules after moving them into the Radio.TV Gradle build:

```kotlin
implementation(project(":tvremote-core"))
implementation(project(":tvremote-ui"))
implementation(project(":tvremote-cast"))
```

Embed the public Compose API:

```kotlin
TvRemotePad(
    enabled = remoteIsConnected,
    onKey = { key -> lifecycleScope.launch { transport.sendKey(key) } }
)
```

Keep protocol and connection state in `:tvremote-core`; UI does not depend on the demo host. For media handoff, implement `RadioTvMediaProvider.currentMedia(): CastMedia?` after DLNA AVTransport has been added.

## CI

GitHub Actions builds `:app:assembleDebug`, runs `:tvremote-core:test`, verifies the APK with `apksigner`, checks package/SDK/label/orientation/cleartext/local-network permission, inspects DEX for core/UI/cast classes, calculates SHA-256 and APK size, and uploads `Radio.TV.Control-debug-run-N`. The debug artifact is for device testing, not a Play Store release.
