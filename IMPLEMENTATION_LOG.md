# Implementation Log

## 2026-06-16

### Summary

Continued build-plan work while preserving the Start Here first-use guidance for
testing. The app version was bumped so the APK can install over the previous
debug build, baseline summaries now persist locally, and guided baseline capture
now generates a hot-pixel map count from live dark frames.

### Build-Plan Tasks Completed

- Bumped debug app version to `0.1.2` / versionCode `3` for update-over-install.
- Kept the Start Here first-use guidance visible for continued first-use
  testing.
- Added local baseline persistence with camera ID, quality, frame counts,
  hot-pixel count, and collection timestamp.
- Added luma frame snapshots to the camera probe.
- Wired guided baseline capture into bounded live hot-pixel map generation.
- Updated UI to show saved baseline camera and hot-pixel count.
- Refreshed the GitHub APK zip artifact.

### Tests And Verification

- Ran `.\gradlew.bat test assembleDebug`.
- Result: build successful.
- Refreshed downloadable zip:
  `C:\Users\fhidi\Documents\Rad phone camera\RadPhoneCamera-debug.zip`

### Files Changed

- `.gitignore`
- `app/build.gradle.kts`
- `app/src/main/java/com/radphonecamera/app/baseline/BaselineQuality.kt`
- `app/src/main/java/com/radphonecamera/app/baseline/BaselineStore.kt`
- `app/src/main/java/com/radphonecamera/app/camera/FrameProbe.kt`
- `app/src/main/java/com/radphonecamera/app/MainActivity.kt`
- `app/src/main/java/com/radphonecamera/app/ui/RadPhoneCameraApp.kt`
- `README.md`
- `BUILD_PLAN.md`
- `APK_DELIVERY.md`
- `IMPLEMENTATION_LOG.md`
- `RadPhoneCamera-debug.zip`

### Blockers

- Hot-pixel map count is generated and persisted, but the full map is not yet
  stored or fed into the live detector loop.
- Live detector loop and event logging still need implementation.

### Recommended Next Tasks

- Persist the full hot-pixel map or compact mask for the selected camera.
- Feed baseline/hot-pixel data into a live scan loop.
- Add user-facing active detector status after baseline completion.

---

## 2026-06-16

### Summary

Improved the first-run user experience after device feedback. The app now tells
the user what to do first, explains how to collect a dark baseline, uses clearer
button labels, and provides a Stop button for active camera tests or baseline
collection.

### Build-Plan Tasks Completed

- Added a Start Here guidance panel.
- Added baseline collection directions: put the phone face down or in a dark
  pocket, keep it still for 60 seconds, and use Stop to cancel.
- Renamed user-facing actions from debug-oriented labels to clearer actions:
  Test camera and Start baseline.
- Added a cancellable `FrameProbeSession`.
- Added Stop behavior for YUV probe and 60-second baseline capture.
- Refreshed the debug APK zip artifact.

### Tests And Verification

- Ran `.\gradlew.bat test assembleDebug`.
- Result: build successful.
- Refreshed downloadable zip:
  `C:\Users\fhidi\Documents\Rad phone camera\RadPhoneCamera-debug.zip`

### Files Changed

- `app/src/main/java/com/radphonecamera/app/camera/FrameProbe.kt`
- `app/src/main/java/com/radphonecamera/app/MainActivity.kt`
- `app/src/main/java/com/radphonecamera/app/ui/RadPhoneCameraApp.kt`
- `README.md`
- `BUILD_PLAN.md`
- `IMPLEMENTATION_LOG.md`
- `RadPhoneCamera-debug.zip`

### Blockers

- Live detector loop and persistent baseline storage are still not wired.

### Recommended Next Tasks

- Add persistence for baseline results and selected camera.
- Wire live baseline frames into hot-pixel map generation.
- Add a clearer post-baseline next step for starting the detector loop.

---

## 2026-06-16

### Summary

Built the first working Android debug app and APK. The app now has Camera2
device discovery, camera capability scoring, a Compose debug UI, a single-camera
YUV probe, frame statistics, dark-frame baseline scoring, and unit-tested
detector foundations.

### Build-Plan Tasks Completed

- Created Android Kotlin project with Jetpack Compose.
- Added Gradle wrapper and local Android SDK build setup.
- Added camera permission only; no GPS, microphone, cloud, broad file access, or
  photo-saving permission.
- Added Camera2 discovery and camera capability display.
- Added device qualification scoring for YUV, RAW, manual exposure, manual ISO,
  focus control, hardware level, and physical camera IDs.
- Added single-camera YUV probe with manual exposure/ISO/focus attempts.
- Added frame brightness mean, variance, min, and max metrics.
- Added dark-state classifier.
- Added 60-second baseline/refresh workflow and Good/Fair/Poor/Invalid baseline
  scoring.
- Added hot-pixel map, sparse event detector, rolling baseline, Z-score, and
  alarm-state helper logic.
- Added first debug APK build.
- Added README.

### Tests And Verification

- Ran `.\gradlew.bat test assembleDebug`.
- Result: build successful.
- Unit tests: 8 Kotlin test files covering frame stats, dark-state scoring,
  device qualification, baseline quality, hot-pixel mapping, sparse event
  detection, rolling baseline, and alarm evaluation.
- Debug APK generated:
  `C:\Users\fhidi\Documents\Rad phone camera\app\build\outputs\apk\debug\app-debug.apk`

### Files Changed

- Android Gradle project files and wrapper.
- `app/src/main/...` Android app, Camera2, baseline, detector, and Compose UI
  code.
- `app/src/test/...` detector and baseline unit tests.
- `README.md`
- `BUILD_PLAN.md`
- `APK_DELIVERY.md`
- `IMPLEMENTATION_LOG.md`

### Blockers

- Live camera behavior still needs testing on an Android phone.
- Hot-pixel map generation exists as pure logic but is not yet wired into live
  baseline captures.
- Release APK signing is not configured.

### Recommended Next Tasks

- Install the debug APK on an Android phone and verify camera permission,
  discovery, YUV probe, and baseline workflow.
- Wire live baseline captures into hot-pixel map generation.
- Add persistent baseline storage with DataStore or Room.
- Add event log storage and CSV export.

---

## 2026-06-16

### Summary

Added standing delivery authorization so future agents build, commit, push, and
provide debug APK files as part of normal implementation runs.

### Build-Plan Tasks Completed

- Updated agent behavior rules to treat build, commit, push, and APK delivery
  as pre-authorized workflow steps.
- Added `APK_DELIVERY.md` with default debug APK path, build commands, and
  final-response requirements.
- Updated `BUILD_PLAN.md` progress with APK delivery process status.

### Tests And Verification

- Markdown files were reviewed for clear headings and consistent formatting.
- No Android build was run because the Android project has not been created yet.

### Files Changed

- `AGENTS.md`
- `APK_DELIVERY.md`
- `BUILD_PLAN.md`
- `IMPLEMENTATION_LOG.md`

### Blockers

- No APK can be built until Phase 0 creates the Android Gradle project.

### Recommended Next Tasks

- Start Phase 0 by creating the Android Kotlin project structure.
- Add Camera2 camera discovery and capability display.
- Run the first Gradle build and produce the first debug APK.

---

## 2026-06-16

### Summary

Added the repository agent operating guide and initialized the implementation
log so future coding runs can work autonomously through `BUILD_PLAN.md`.

### Build-Plan Tasks Completed

- Added coding-first agent behavior rules for future implementation runs.
- Established documentation update requirements for completed work.
- Created the implementation log used to track completed build-plan progress.

### Tests And Verification

- Markdown files were reviewed for clear headings and consistent formatting.
- No Android build was run because this change only adds repository workflow
  documentation.

### Files Changed

- `AGENTS.md`
- `IMPLEMENTATION_LOG.md`
- `BUILD_PLAN.md`

### Blockers

- Android implementation has not started yet.

### Recommended Next Tasks

- Start Phase 0 by creating the Android Kotlin project structure.
- Add Camera2 camera discovery and a basic capability display.
- Add the first build verification step for the generated Android project.
