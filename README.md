# RadPhoneCamera

RadPhoneCamera is an Android-first, software-only prototype for camera-sensor
radiation-like anomaly screening. It uses Camera2 dark-frame analysis,
device-side processing, conservative alarm language, and a required baseline
workflow before normal detector use.

This is an early debug build. It is not a dosimeter, survey meter,
contamination meter, isotope identifier, PRD replacement, or regulatory
instrument. Confirm any alarm with a proper calibrated radiation instrument or
trained radiation specialist.

## Current Build

- Platform: Android native Kotlin.
- UI: Jetpack Compose.
- Camera: Camera2 API.
- Required permission: camera only.
- Default processing: local-only, no GPS, no cloud upload, no saved photos.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Current debug version: `0.1.5` / versionCode `6`.
- Current GitHub debug zip artifact: `RadPhoneCamera-debug.zip`.

## Implemented So Far

- Android Gradle project and wrapper.
- Camera2 camera discovery and capability scoring.
- Camera IDs, hardware levels, YUV/RAW/manual-control support display.
- Single-camera YUV probe.
- Manual exposure, ISO, focus, and flash-off capture request attempts.
- Luma mean, variance, min/max frame metrics.
- First-use guidance that tells the user to start with baseline collection.
- Visible countdown for timed camera tests, 60-second baseline collection, and
  30-second quick scans.
- Stop button for active camera tests, baseline collection, and quick scans.
- Stale camera callbacks are ignored after Stop or after a new capture starts.
- Dark-frame quality classification.
- Initial 60-second baseline/refresh workflow with Good/Fair/Poor/Invalid
  scoring.
- Baseline summary persistence across app restarts.
- Baseline age display and a soft refresh reminder after 72 hours.
- Live baseline capture hot-pixel map generation and bounded compact mask
  persistence across app restarts.
- Quick scan mode after a usable baseline, showing candidate events/minute,
  valid dark-frame fraction, scan status, baseline Z-score when available, and
  hot-pixel-mask status.
- Hot-pixel map generation from dark frames.
- Sparse bright-cluster event detection.
- Persisted baseline candidate-event statistics for conservative scan
  comparison.
- Rolling baseline model and Z-score calculation.
- Basic alarm-state evaluation.
- Unit tests for detector and baseline helpers.

## Local Build

The repo uses a local ignored Android SDK folder at `.android-sdk` on this
machine. To build:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_SDK_ROOT=(Resolve-Path ".android-sdk").Path
.\gradlew.bat test assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release APKs are not configured yet because signing credentials have not been
provided.
