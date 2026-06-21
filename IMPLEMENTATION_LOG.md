# Implementation Log

## 2026-06-21

### Summary

Expanded baseline refresh guidance beyond the existing 72-hour age reminder and
added the first privacy controls. Each new baseline records the app, Android,
device/camera profile, and thermal state that produced it. The app evaluates
that snapshot with the local scan log, lets users opt out of future local scan
summary storage, and can delete all local detector data after confirmation.

### Build-Plan Tasks Completed

- Bumped debug app version to `0.2.3` / versionCode `14` for update-over-install.
- Persisted per-camera baseline environment metadata: app version, Android API
  level, device model, Camera2 capability signature, and thermal status.
- Added deterministic refresh reasons for stale baselines, app updates, Android
  updates, device/camera-profile changes, warm thermal drift, and repeated
  limited-sensitivity or invalid scans.
- Kept earlier baseline records valid when they lack the new metadata; their
  next successful refresh records the profile automatically.
- Displayed refresh reasons in the main status and first-use guidance.
- Added a local scan-summary logging toggle. When off, new Quick, multi-camera,
  and Patrol scan summaries remain on-screen but are not persisted.
- Added a two-step Delete local data action that clears baselines, hot-pixel
  masks, local scan summaries, and detector settings without touching photos,
  files, GPS, or other device data.
- Added unit coverage for missing baselines, environment drift, thermal drift,
  repeated limited scans, and legacy baseline compatibility.
- Ignored workspace-local Gradle and Kotlin verification caches.

### Tests And Verification

- Ran `test assembleDebug` with the local Android SDK and shared read-only
  dependency cache.
- Result: `BUILD SUCCESSFUL`; unit tests and debug APK assembly completed.
- Kotlin daemon startup remains restricted in the sandbox; Gradle used its
  successful in-process compiler fallback.

### Files Changed

- `.gitignore`
- `app/build.gradle.kts`
- `app/src/main/java/com/radphonecamera/app/MainActivity.kt`
- `app/src/main/java/com/radphonecamera/app/baseline/BaselineQuality.kt`
- `app/src/main/java/com/radphonecamera/app/baseline/BaselineRefresh.kt`
- `app/src/main/java/com/radphonecamera/app/baseline/BaselineStore.kt`
- `app/src/main/java/com/radphonecamera/app/data/DetectorSettingsStore.kt`
- `app/src/main/java/com/radphonecamera/app/ui/RadPhoneCameraApp.kt`
- `app/src/test/java/com/radphonecamera/app/baseline/BaselineRefreshEvaluatorTest.kt`
- `README.md`
- `BUILD_PLAN.md`
- `APK_DELIVERY.md`
- `IMPLEMENTATION_LOG.md`
- `RadPhoneCamera-debug.zip`

### Blockers

- The app has no long-running opportunistic dark-data accumulation yet, so it
  cannot assess the build plan's insufficient-recent-dark-data reminder.
- Thermal refresh guidance is intentionally one-way: it recommends a refresh
  only when the current phone state is warmer than the baseline state.

### Recommended Next Tasks

- Add the remaining onboarding/privacy controls for expert diagnostics and the
  optional model/profile update and anonymous-report settings.
- Add broad alarm and confidence UX without exact dose-rate claims.
- Add bounded opportunistic dark-data maintenance while foreground Patrol is
  active.

---

## 2026-06-20

### Summary

Completed the next multi-camera and Patrol execution batch. Baselines now
persist independently for each camera channel, the app can collect those
baselines sequentially, and foreground-only Patrol now performs bounded,
gated detector bursts instead of being a display-only scaffold.

### Build-Plan Tasks Completed

- Bumped debug app version to `0.2.1` / versionCode `12` for update-over-install.
- Migrated the prior installed single-camera baseline into the new per-camera
  storage format on first read, preserving the baseline, compact hot-pixel
  mask, and candidate-event statistics.
- Added per-camera baseline coverage calculation and UI so multi-camera Quick
  scans require a usable baseline for every selected channel.
- Added sequential 60-second baseline collection for up to three weighted
  eligible Camera2 channels, including progress, retry, and Stop states.
- Routed single-camera, multi-camera, and Patrol detector runs to each
  camera's own baseline model and hot-pixel mask.
- Added foreground-only Patrol burst execution using the selected baseline
  camera, with short capture duration, interval backoff, local summary logging,
  and immediate stop when the app leaves the foreground.
- Added foreground visibility to Patrol scheduling gates and unit tests for
  background pause plus multi-camera baseline coverage.

### Tests And Verification

- Ran `test assembleDebug` through Gradle 8.9 using the local Android SDK and
  shared read-only dependency cache.
- Result: `BUILD SUCCESSFUL`; unit tests and debug APK assembly completed.
- Kotlin daemon startup is restricted in the sandbox, so Gradle compiled with
  its successful in-process fallback.

### Files Changed

- `app/build.gradle.kts`
- `app/src/main/java/com/radphonecamera/app/MainActivity.kt`
- `app/src/main/java/com/radphonecamera/app/baseline/BaselineStore.kt`
- `app/src/main/java/com/radphonecamera/app/baseline/CameraBaselineCoverage.kt`
- `app/src/main/java/com/radphonecamera/app/patrol/PatrolScheduler.kt`
- `app/src/main/java/com/radphonecamera/app/ui/RadPhoneCameraApp.kt`
- `app/src/test/java/com/radphonecamera/app/baseline/CameraBaselineCoverageCalculatorTest.kt`
- `app/src/test/java/com/radphonecamera/app/patrol/PatrolSchedulerTest.kt`
- `README.md`
- `BUILD_PLAN.md`
- `APK_DELIVERY.md`
- `IMPLEMENTATION_LOG.md`
- `RadPhoneCamera-debug.zip`

### Blockers

- Patrol deliberately stays foreground-only. It has no foreground service,
  notification permission flow, background scheduling, or background camera
  access yet.
- Multi-camera capture is sequential. Simultaneous camera access remains
  device-specific future work.

### Recommended Next Tasks

- Add baseline-refresh triggers for app/OS/camera changes, repeated limited
  scans, and measured noise/thermal changes.
- Add the planned permission/privacy onboarding and settings controls.
- Build conservative alarm/dose-band and expert-diagnostics UI without adding
  exact dose-rate claims.

---

## 2026-06-20

### Summary

Completed the next three implementation slices: accelerometer-based
motion/orientation checks, sequential multi-camera Quick scan, and a
battery-aware Patrol policy scaffold. The app now rejects moving frames from
baseline and scan validity, can scan up to three weighted camera channels one
after another, and exposes conservative Patrol readiness without continuous
camera or background capture.

### Build-Plan Tasks Completed

- Bumped debug app version to `0.2.0` / versionCode `11` for update-over-install.
- Added accelerometer-based Still, Mostly still, Moving, and unavailable sensor
  states plus face-down, face-up, upright, and side posture classification.
- Displayed motion/orientation state and rejected moving frames from baseline
  and Quick scan valid-frame counts.
- Added sequential multi-camera Quick scan for the top two or three eligible
  weighted Camera2 channels.
- Added weighted combined candidate rate, Z-score, frame-quality result, and
  local aggregate scan-log entry for completed multi-camera scans.
- Added Patrol Battery Saver, Balanced, and Max Sensitivity selection.
- Added baseline, low-battery, thermal, motion, and posture gates to Patrol
  readiness, with no continuous or autonomous camera capture yet.
- Added unit tests for motion classification, multi-camera aggregation, and
  Patrol scheduling.

### Tests And Verification

- Ran `.\gradlew.bat test assembleDebug`.
- Result: build successful.
- Refreshed downloadable zip:
  `C:\Users\fhidi\Documents\Rad phone camera\RadPhoneCamera-debug.zip`

### Files Changed

- `app/build.gradle.kts`
- `app/src/main/java/com/radphonecamera/app/MainActivity.kt`
- `app/src/main/java/com/radphonecamera/app/detector/MultiCameraScanAggregator.kt`
- `app/src/main/java/com/radphonecamera/app/patrol/PatrolScheduler.kt`
- `app/src/main/java/com/radphonecamera/app/sensors/BatteryThermalStateProvider.kt`
- `app/src/main/java/com/radphonecamera/app/sensors/MotionState.kt`
- `app/src/main/java/com/radphonecamera/app/sensors/MotionStateProvider.kt`
- `app/src/main/java/com/radphonecamera/app/ui/RadPhoneCameraApp.kt`
- `app/src/test/java/com/radphonecamera/app/detector/MultiCameraScanAggregatorTest.kt`
- `app/src/test/java/com/radphonecamera/app/patrol/PatrolSchedulerTest.kt`
- `app/src/test/java/com/radphonecamera/app/sensors/MotionStateClassifierTest.kt`
- `README.md`
- `BUILD_PLAN.md`
- `APK_DELIVERY.md`
- `IMPLEMENTATION_LOG.md`
- `RadPhoneCamera-debug.zip`

### Blockers

- Multi-camera scan currently has a stored baseline/hot-pixel mask only for the
  camera used to collect the baseline; per-camera baselines are not yet stored.
- Patrol is a visible policy/readiness scaffold only. It does not start
  foreground service, scheduled background work, or autonomous camera bursts.

### Recommended Next Tasks

- Add per-camera baseline storage and sequential multi-camera baseline
  collection before using combined results beyond experimental testing.
- Add bounded foreground Patrol short-burst execution with notification and
  lifecycle controls.
- Add the remaining onboarding, privacy settings, dose-band, calibration, and
  validation documentation work.

---

## 2026-06-19

### Summary

Fixed the guided baseline capture lifecycle so the 60-second baseline stops by
itself when the timer completes, even if camera frame callbacks are busy. Stop
now requests cleanup immediately instead of waiting behind camera callback work.

### Build-Plan Tasks Completed

- Bumped debug app version to `0.1.9` / versionCode `10` for update-over-install.
- Added a duration-reached guard inside live frame processing so timed captures
  finish as soon as the requested duration is reached.
- Made Stop request capture cleanup immediately.
- Updated first-use baseline text to state that baseline capture stops
  automatically when the timer reaches 0.
- Refreshed the GitHub APK zip artifact.

### Tests And Verification

- Ran `.\gradlew.bat test assembleDebug`.
- Result: build successful.
- Refreshed downloadable zip:
  `C:\Users\fhidi\Documents\Rad phone camera\RadPhoneCamera-debug.zip`

### Files Changed

- `app/build.gradle.kts`
- `app/src/main/java/com/radphonecamera/app/camera/FrameProbe.kt`
- `app/src/main/java/com/radphonecamera/app/ui/RadPhoneCameraApp.kt`
- `README.md`
- `BUILD_PLAN.md`
- `APK_DELIVERY.md`
- `IMPLEMENTATION_LOG.md`
- `RadPhoneCamera-debug.zip`

### Blockers

- Baseline auto-stop still needs confirmation on the physical test phone.

### Recommended Next Tasks

- Install this APK and verify the baseline screen clears itself at 60 seconds.
- Add sequential multi-camera Quick scan orchestration.
- Add motion/orientation stability checks.

---

## 2026-06-19

### Summary

Added the first multi-camera aggregation slice. The app now computes a weighted
multi-camera plan from discovered camera detector scores and shows combined
readiness in the device check area.

### Build-Plan Tasks Completed

- Bumped debug app version to `0.1.8` / versionCode `9` for update-over-install.
- Added deterministic multi-camera weighting from usable Camera2 channels.
- Added combined multi-camera detector readiness score and support level.
- Added UI display for usable channels, combined score, and top camera weights.
- Added unit coverage for unsupported-camera exclusion, normalized weights, and
  no-eligible-camera behavior.
- Refreshed the GitHub APK zip artifact.

### Tests And Verification

- Ran `.\gradlew.bat test assembleDebug`.
- Result: build successful.
- Refreshed downloadable zip:
  `C:\Users\fhidi\Documents\Rad phone camera\RadPhoneCamera-debug.zip`

### Files Changed

- `app/build.gradle.kts`
- `app/src/main/java/com/radphonecamera/app/detector/MultiCameraWeighting.kt`
- `app/src/main/java/com/radphonecamera/app/ui/RadPhoneCameraApp.kt`
- `app/src/test/java/com/radphonecamera/app/detector/MultiCameraWeightingTest.kt`
- `README.md`
- `BUILD_PLAN.md`
- `APK_DELIVERY.md`
- `IMPLEMENTATION_LOG.md`
- `RadPhoneCamera-debug.zip`

### Blockers

- The app does not yet run sequential or simultaneous multi-camera scans.
- Combined score is currently based on camera capability scores, not live
  per-camera scan statistics.

### Recommended Next Tasks

- Add sequential multi-camera Quick scan orchestration.
- Add motion/orientation stability checks.
- Add optional foreground Patrol mode scaffolding.

---

## 2026-06-17

### Summary

Added local scan-log management. Users can now export completed Quick scan
summary records as CSV through Android share targets and delete the local
summary log from the main screen.

### Build-Plan Tasks Completed

- Bumped debug app version to `0.1.7` / versionCode `8` for update-over-install.
- Added CSV export formatting for local scan-event summaries.
- Added a Scan log export button that shares text/csv data through Android's
  standard share flow.
- Added a Delete log button that clears only the local scan summary history.
- Added unit coverage for CSV export field escaping and header output.
- Refreshed the GitHub APK zip artifact.

### Tests And Verification

- Ran `.\gradlew.bat test assembleDebug`.
- Result: build successful.
- Refreshed downloadable zip:
  `C:\Users\fhidi\Documents\Rad phone camera\RadPhoneCamera-debug.zip`

### Files Changed

- `app/build.gradle.kts`
- `app/src/main/java/com/radphonecamera/app/MainActivity.kt`
- `app/src/main/java/com/radphonecamera/app/data/ScanEventLog.kt`
- `app/src/main/java/com/radphonecamera/app/data/ScanEventLogStore.kt`
- `app/src/main/java/com/radphonecamera/app/ui/RadPhoneCameraApp.kt`
- `app/src/test/java/com/radphonecamera/app/data/ScanEventLogCodecTest.kt`
- `README.md`
- `BUILD_PLAN.md`
- `APK_DELIVERY.md`
- `IMPLEMENTATION_LOG.md`
- `RadPhoneCamera-debug.zip`

### Blockers

- CSV export currently uses Android text sharing rather than writing a dedicated
  file through a document picker.
- Event logging is still limited to completed Quick scans.

### Recommended Next Tasks

- Add multi-camera weighted scan aggregation.
- Add motion/orientation stability checks.
- Add optional foreground Patrol mode scaffolding.

---

## 2026-06-17

### Summary

Added a local rolling event log for completed Quick scan runs. The app now keeps
summary-only scan history on device and shows the latest entries on the main
screen.

### Build-Plan Tasks Completed

- Bumped debug app version to `0.1.6` / versionCode `7` for update-over-install.
- Added `ScanEvent` summary records for completed Quick scans.
- Added a compact local scan-event codec and SharedPreferences-backed store.
- Wrote completed Quick scan results into the local rolling log.
- Added a main-screen scan log panel showing recent scan status, candidate
  event rate, valid-frame count, and baseline Z-score when available.
- Added unit tests for scan-event log encoding and invalid-row handling.
- Refreshed the GitHub APK zip artifact.

### Tests And Verification

- Ran `.\gradlew.bat test assembleDebug`.
- Result: build successful.
- Refreshed downloadable zip:
  `C:\Users\fhidi\Documents\Rad phone camera\RadPhoneCamera-debug.zip`

### Files Changed

- `app/build.gradle.kts`
- `app/src/main/java/com/radphonecamera/app/MainActivity.kt`
- `app/src/main/java/com/radphonecamera/app/data/ScanEventLog.kt`
- `app/src/main/java/com/radphonecamera/app/data/ScanEventLogStore.kt`
- `app/src/main/java/com/radphonecamera/app/ui/RadPhoneCameraApp.kt`
- `app/src/test/java/com/radphonecamera/app/data/ScanEventLogCodecTest.kt`
- `README.md`
- `BUILD_PLAN.md`
- `APK_DELIVERY.md`
- `IMPLEMENTATION_LOG.md`
- `RadPhoneCamera-debug.zip`

### Blockers

- The local scan log does not yet have delete/export controls.
- Event logging is currently tied to completed Quick scans, not baseline runs,
  patrol bursts, or multi-camera aggregate scans.

### Recommended Next Tasks

- Add delete local data and CSV export controls for the event log.
- Add multi-camera weighted scan aggregation.
- Add motion/orientation stability checks.

---

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
