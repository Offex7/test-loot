# Radio.TV.Control

Standalone Android remote application prepared for a later move into Radio.TV as a feature module.

## Project layout

- `:app` — standalone demo host; label `Radio.TV.Control`.
- `:tvremote-core` — transport abstractions and Android TV Remote v2 TLS/protobuf transport.
- `:tvremote-ui` — reusable Jetpack Compose remote pad.
- `:tvremote-cast` — SSDP discovery foundation for DLNA renderers.

**Package:** `com.radiotv.control`  
**Minimum Android:** 12 / API 31  
**Target Android:** API 36  
**Orientation:** portrait, with content capped at 520 dp on wide screens.

## Build

Requires JDK 17, Android SDK platform 36 and Build Tools 36.0.0. This subproject currently uses a Gradle 8.13 installation rather than a committed Gradle Wrapper.

```bash
cd Radio.TV.Control
gradle :app:assembleDebug :tvremote-core:test
```

Install locally:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Implemented in this baseline

- Material 3 dark UI, TV IP entry, pairing-code flow, D-pad, navigation, volume, channel, source, digit and media buttons.
- Android TV Remote v2 TLS connection path on ports 6467/6466, protobuf framing, generated client identity, pairing-code exchange, basic key injection, and server certificate pinning after pairing.
- Core key-mapping unit tests.
- SSDP M-SEARCH renderer discovery starter.
- Public Compose API `TvRemotePad(enabled, onKey, modifier)`, independent of the demo app.

## Not implemented / not physically verified yet

- Voice PCM capture/streaming and complete TV IME text injection.
- Bluetooth HID, Samsung Tizen, LG webOS, Roku ECP and external IR hardware.
- DLNA AVTransport playback/DIDL-Lite handoff; current cast code only discovers renderer candidates.
- Touchpad, gyroscope air mouse, TV model-specific fallbacks and advanced discovery.
- Pairing compatibility across TV firmware variants. CI cannot verify live TV pairing, command delivery, Bluetooth HID roles or gyroscope readings.

A green CI run proves the build and configured static checks/tests pass. It does not prove compatibility with every TV brand. The Android TV Remote v2 flow still needs tests against physical Android TV / Google TV hardware.

## Integration in Radio.TV (stage 2)

Move these modules into the Radio.TV build and add:

```kotlin
implementation(project(":tvremote-core"))
implementation(project(":tvremote-ui"))
implementation(project(":tvremote-cast"))
```

In the host Compose screen, render the public API:

```kotlin
TvRemotePad(
    enabled = remoteIsConnected,
    onKey = { key -> lifecycleScope.launch { transport.sendKey(key) } }
)
```

Keep protocol connection state in `:tvremote-core`; UI does not depend on the demo host. For media handoff, implement `RadioTvMediaProvider.currentMedia(): CastMedia?` and add DLNA AVTransport before shipping a cast action.

## CI

GitHub Actions builds `:app:assembleDebug`, runs `:tvremote-core:test`, verifies the APK with `apksigner`, checks package/SDK/label/orientation/cleartext values, scans DEX for core/UI/cast classes, and uploads `Radio.TV.Control-debug-run-N`. The debug APK is for device testing, not a Play Store release.
