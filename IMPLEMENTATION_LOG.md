# Implementation Log

## 2026-06-16

### Summary

Added the first stale-baseline reminder UI. The main screen now shows when the
last baseline was collected and recommends a refresh after 72 hours without new
dark data.

### Build-Plan Tasks Completed

- Bumped debug app version to `0.1.5` / versionCode `6` for update-over-install
  after the stale-reminder APK refresh.
- Added baseline age text to the baseline summary.
- Added a soft baseline refresh recommendation after 72 hours.
- Updated Start Here guidance to recommend baseline refresh when a usable
  baseline is stale.
- Refreshed the GitHub APK zip artifact.

### Tests And Verification

- Ran `.\gradlew.bat test assembleDebug`.
- Result: build successful.
- Refreshed downloadable zip:
  `C:\Users\fhidi\Documents\Rad phone camera\RadPhoneCamera-debug.zip`

### Files Changed

- `app/build.gradle.kts`
- `app/src/main/java/com/radphonecamera/app/ui/RadPhoneCameraApp.kt`
- `README.md`
- `BUILD_PLAN.md`
- `APK_DELIVERY.md`
- `IMPLEMENTATION_LOG.md`
- `RadPhoneCamera-debug.zip`

### Blockers

- Baseline reminders currently cover age only. Other triggers from the build
  plan, such as app update, OS update, temperature/noise changes, and repeated
  limited scans, still need implementation.

### Recommended Next Tasks

- Add a local event log for completed scans.
- Add multi-camera weighted scan aggregation.
- Expand baseline reminders beyond the 72-hour age rule.

---

## 2026-06-16

### Summary

Continued the detector core after the timer/Stop and Quick scan batch. Baseline
captures now persist compact event-rate statistics, and Quick scan evaluates
candidate counts against those baseline stats to show a conservative Z-score
when enough valid dark frames are available.

### Build-Plan Tasks Completed

- Bumped debug app version to `0.1.4` / versionCode `5` for update-over-install
  after the second APK refresh.
- Added `BaselineEventStats` to calculate candidate-event baseline statistics
  from dark-frame snapshots after hot-pixel masking.
- Persisted baseline event frame count, total candidate events, mean events per
  frame, and variance events per frame.
- Restored baseline event stats from local storage across app restarts.
- Fed the restored baseline model into Quick scan.
- Added Quick scan baseline Z-score output when at least 10 valid scan frames
  and enough baseline frames are available.
- Added unit tests for baseline event-stat calculation and Z-score-driven scan
  alarm status.
- Refreshed the GitHub APK zip artifact.

### Tests And Verification

- Ran `.\gradlew.bat test assembleDebug`.
- Result: build successful.
- Refreshed downloadable zip:
  `C:\Users\fhidi\Documents\Rad phone camera\RadPhoneCamera-debug.zip`

### Files Changed

- `app/build.gradle.kts`
- `app/src/main/java/com/radphonecamera/app/MainActivity.kt`
- `app/src/main/java/com/radphonecamera/app/baseline/BaselineQuality.kt`
- `app/src/main/java/com/radphonecamera/app/baseline/BaselineStore.kt`
- `app/src/main/java/com/radphonecamera/app/detector/BaselineEventStats.kt`
- `app/src/main/java/com/radphonecamera/app/detector/LiveScanAccumulator.kt`
- `app/src/main/java/com/radphonecamera/app/ui/RadPhoneCameraApp.kt`
- `app/src/test/java/com/radphonecamera/app/detector/BaselineEventStatsTest.kt`
- `app/src/test/java/com/radphonecamera/app/detector/LiveScanAccumulatorTest.kt`
- `README.md`
- `BUILD_PLAN.md`
- `APK_DELIVERY.md`
- `IMPLEMENTATION_LOG.md`
- `RadPhoneCamera-debug.zip`

### Blockers

- Quick scan Z-score is based on sampled baseline frames, not yet on a
  full-duration rolling baseline with time-windowed rates.
- Scan results are not yet saved to a local event log.

### Recommended Next Tasks

- Add a local event log for completed scans.
- Add multi-camera weighted scan aggregation.
- Add baseline stale/reminder logic in UI.

---

## 2026-06-16

### Summary

Fixed the timed capture lifecycle and continued the next detector UI slice. The
app now has a visible countdown, stronger Stop behavior, stale-callback
protection, a 30-second Quick scan mode that reports candidate events per minute
after a usable baseline, and compact hot-pixel-mask persistence for restarted
scan use.

### Build-Plan Tasks Completed

- Bumped source app version to `0.1.3` / versionCode `4` so the next debug APK
  can install over the previous build.
- Added timer progress fields to `FrameProbeResult`.
- Added one-second progress ticks for timed captures.
- Removed timeout callbacks when capture finishes or is stopped.
- Routed Stop cleanup through the camera worker handler and closed capture
  session, image reader, and camera device.
- Added active-capture IDs so stale callbacks from older captures cannot clear
  newer UI state.
- Kept baseline frame counts tied to real analyzed frames, not timer ticks.
- Added a live scan accumulator for valid dark-frame fraction, sparse candidate
  events, events/minute, rejected hot pixels, rejected artifacts, and conservative
  alarm status.
- Added Quick scan UI after a Good/Fair baseline with countdown, Stop control,
  candidate-event rate, and hot-pixel-mask status.
- Added compact hot-pixel-mask serialization.
- Persisted and restored the selected camera's bounded hot-pixel mask for Quick
  scan use after app restart.
- Refreshed the GitHub APK zip artifact.

### Tests And Verification

- Ran `git diff --check`.
- Result: passed, with only Git line-ending warnings.
- Ran `.\gradlew.bat test assembleDebug`.
- Result: build successful.
- Refreshed downloadable zip:
  `C:\Users\fhidi\Documents\Rad phone camera\RadPhoneCamera-debug.zip`

### Files Changed

- `app/build.gradle.kts`
- `app/src/main/java/com/radphonecamera/app/baseline/BaselineStore.kt`
- `app/src/main/java/com/radphonecamera/app/MainActivity.kt`
- `app/src/main/java/com/radphonecamera/app/camera/FrameProbe.kt`
- `app/src/main/java/com/radphonecamera/app/detector/HotPixelMap.kt`
- `app/src/main/java/com/radphonecamera/app/detector/LiveScanAccumulator.kt`
- `app/src/main/java/com/radphonecamera/app/ui/RadPhoneCameraApp.kt`
- `app/src/test/java/com/radphonecamera/app/detector/HotPixelMapTest.kt`
- `app/src/test/java/com/radphonecamera/app/detector/LiveScanAccumulatorTest.kt`
- `README.md`
- `BUILD_PLAN.md`
- `APK_DELIVERY.md`
- `IMPLEMENTATION_LOG.md`
- `RadPhoneCamera-debug.zip`

### Blockers

- The persisted hot-pixel mask is bounded to keep SharedPreferences storage
  small; very noisy devices may need a more compact binary mask format later.
- Live scan still reports conservative status from valid-frame fraction and
  candidate event rate; baseline event-rate/Z-score alarm evaluation is not yet
  wired to persisted baseline statistics.

### Recommended Next Tasks

- Feed persisted baseline event-rate data into Z-score alarm evaluation.
- Add a local event log for completed scans.
- Add multi-camera weighted scan aggregation.

---

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
