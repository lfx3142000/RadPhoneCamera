# Build Plan: Phone-Only Radiation Anomaly Detector App

## Progress Status

Last updated: 2026-06-16

| Area | Status | Notes |
| ---- | ------ | ----- |
| Repository agent workflow | Complete | Added `AGENTS.md` with coding-first autonomous run rules. |
| Implementation log | Complete | Added `IMPLEMENTATION_LOG.md` for run summaries, tests, blockers, and next tasks. |
| APK delivery process | Complete | Added standing build/commit/push/APK delivery defaults in `APK_DELIVERY.md`. |
| Android project setup | Complete | Added Kotlin/Compose Android project, Gradle wrapper, local SDK build path, and debug APK build. |
| Camera2 discovery and capability display | Complete | Enumerates cameras, reports IDs, hardware level, YUV/RAW support, manual controls, focus control, and support score. |
| Single-camera YUV probe | Complete | Captures YUV frames, attempts manual exposure/ISO/focus lock, and reports luma mean/variance/range. |
| First-use guidance and stop controls | Complete | Added start-here instructions, baseline collection directions, clearer button labels, and Stop control for active capture. |
| Dark-state and baseline workflow | In progress | Added dark-frame classifier plus guided 60-second baseline/refresh scoring; hot-pixel map is implemented as pure logic but not yet generated from live baseline captures. |
| Detector core logic | In progress | Added hot-pixel rejection, sparse event detection, rolling baseline, Z-score, and alarm-state helpers with unit tests. |
| APK artifact | Complete | Debug APK builds at `app/build/outputs/apk/debug/app-debug.apk`. |

Agent runs should update this section and `IMPLEMENTATION_LOG.md` after completed work.

---

## 1. Product Summary

Build an Android-first, software-only radiation anomaly detection app that uses smartphone camera sensors to detect gamma/X-ray-like events above the phone’s learned dark-frame baseline.

The app will not require a scintillator, cap, case, external electronics, GPS, cloud upload, or saved photos. It will rely on:

* camera-frame analysis,
* opportunistic dark conditions,
* multiple camera sensors where available,
* required initial dark baseline scan,
* hot-pixel rejection,
* local background learning,
* statistical alarms based on counts above background,
* optional dose-band estimates,
* optional expert/agency calibration.

The default product is **non-calibrated** and should report alarm categories and broad dose-band classes, not exact dose rates.

---

## 2. Product Positioning

### Intended Use

This app is a privacy-first radiation anomaly screening aid.

Best use cases:

* law-enforcement-style awareness screening,
* “radiation-like anomaly” alerts,
* opportunistic pocket/face-down monitoring,
* preliminary triage,
* training and demonstration,
* detecting elevated gamma/X-ray-like signals above the phone’s own baseline.

### Not Intended For

The app must clearly state that it is not:

* a dosimeter,
* a survey meter,
* a contamination meter,
* a PRD replacement,
* an isotope identifier,
* a package receipt instrument,
* a release survey instrument,
* a regulatory compliance instrument,
* a tool for alpha/beta contamination surveys.

### Core Product Claim

> Camera-only radiation-like anomaly screening based on counts above learned baseline.

### Secondary Optional Claim

> Optional calibrated dose-band estimates for approved phone models or agency-calibrated devices.

---

## 3. Core Design Philosophy

The app should detect statistically significant radiation-like camera events above the phone’s own learned background.

Prioritize:

1. Low false alarms.
2. Minimal permissions.
3. Local-only processing.
4. No GPS by default.
5. No saved photos by default.
6. Approved phone models only.
7. Multi-camera counting where available.
8. Required initial dark baseline.
9. Opportunistic dark-frame maintenance.
10. Battery-aware patrol mode.
11. Counts-over-background as the primary signal.
12. Optional dose-band labels, not exact dose rates.
13. Optional calibration only for expert/agency users.

---

## 4. Technical Scope

### MVP Platform

* Android native app.
* Kotlin.
* Camera2 API for low-level camera access.
* Jetpack Compose for UI.
* Local-only frame processing.
* Room or DataStore for local settings/baseline storage.
* Optional TensorFlow Lite later, but deterministic algorithms first.

### Avoid Initially

* iOS.
* GPS maps.
* cloud processing.
* crowd maps.
* scintillator cap.
* Bluetooth detector pairing.
* exact dose-rate display.
* automatic background location.
* broad “works on any phone” claims.

---

## 5. Supported Device Strategy

Do **not** claim support for every phone.

The app should classify each phone as:

* **Supported**
* **Experimental**
* **Limited**
* **Not Supported**

Initial product should support only approved Android models after testing.

Preferred device characteristics:

* 2 or more accessible camera sensors,
* Camera2 `FULL` or `LEVEL_3` capability if possible,
* manual exposure control,
* manual ISO/sensitivity control,
* stable frame timing,
* usable RAW or YUV frame access,
* stable dark-frame behavior,
* low hot-pixel instability,
* manageable thermal behavior,
* ability to access multiple cameras simultaneously or sequentially.

If a phone fails qualification, detector mode should be limited or disabled.

---

## 6. Permissions and Data Use

The app should use a trust-building setup flow. It should clearly explain required and optional permissions.

Every optional feature must answer:

1. What does this improve?
2. What data is used?
3. Is anything stored?
4. Does anything leave the phone?
5. Can it be turned off?

### Required Permission

#### Camera

Purpose:

* The camera sensor is the detector input.
* The app analyzes dark or near-dark frames for radiation-like transient events.

Privacy:

* Frames are processed on-device.
* Frames are not saved by default.
* Frames are not uploaded.

If denied:

* The app cannot perform detection.

### Optional Features

#### Motion and Orientation Data

Default: On if available without intrusive permission.

Benefit:

* Detects whether the phone is still, moving, face down, pocket-dark, or unstable.
* Helps reject bad frames.
* Helps find useful dark-frame opportunities.
* Reduces false alarms.

Data used:

* accelerometer,
* gyroscope,
* orientation state,
* stillness/face-down/pocket-like state.

No GPS.

#### Notifications

Default: Off until Patrol Mode is enabled.

Benefit:

* Lets the app alert the user during active Patrol Mode.

Data used:

* local alarm state only.

#### Local Event Log

Default: Optional.

Benefit:

* Lets users review prior alarms and scan quality.

Stored data:

* timestamp,
* alarm category,
* confidence,
* active cameras,
* valid-frame fraction,
* baseline count rate,
* candidate event count rate,
* net count rate,
* Z score,
* dose-band class if enabled,
* calibration status.

Do not store:

* images,
* GPS,
* audio,
* contacts,
* personal files.

#### Expert Diagnostics

Default: Off.

Benefit:

* Shows technical detector data for testing and validation.

Data displayed:

* candidate events/minute,
* baseline events/minute,
* net counts,
* sigma above background,
* active camera channels,
* dark-frame quality,
* invalid-frame reasons,
* thermal/battery proxy if available.

#### Optional Model/Profile Updates

Default: Off in MVP, optional later.

Benefit:

* Downloads approved phone-model profiles.
* May improve thresholds, camera selection, false-alarm rejection, and dose-band mapping.

Data that may be sent if opted in:

* phone model,
* Android version,
* app version,
* camera capability summary,
* detector quality score.

Do not upload:

* camera frames,
* images,
* GPS,
* audio,
* names,
* contacts.

#### Optional Anonymous Performance Reports

Default: Off.

Benefit:

* Helps improve supported phone profiles.
* Helps identify OS/camera behavior changes.
* Helps reduce false alarms.

Possible data:

* phone model,
* app version,
* Android version,
* detector state summary,
* baseline event rate,
* false-alarm flag,
* calibration status.

No images. No GPS.

#### Optional Calibration Mode

Default: Off / expert only.

Benefit:

* Allows an agency or expert user to map count-rate response to approximate dose-band classes.

Data stored locally:

* calibration date,
* phone model,
* camera IDs,
* calibration coefficient,
* uncertainty,
* dose-band mapping,
* calibration source/field note if entered.

### Permissions Not Requested in MVP

Do not request:

* GPS/location,
* microphone,
* contacts,
* SMS,
* phone call data,
* broad file access,
* Bluetooth,
* nearby devices,
* background location.

---

## 7. Onboarding Flow

### Step 1: Safety and Privacy Summary

Show before any permission request:

```text
This app uses your phone camera sensor to look for radiation-like events above your phone’s learned baseline.

It is a screening aid only. It is not a dosimeter, survey meter, contamination meter, isotope identifier, or PRD replacement.

By default:
• No GPS is used.
• No cloud upload is used.
• No photos are saved.
• Camera frames are processed on-device and discarded.
```

### Step 2: Camera Permission

Explain:

```text
Camera access is required because the camera sensor is the detector input.

The app analyzes dark or near-dark camera frames for radiation-like sensor events. Frames are processed on this device and are not saved by default.
```

### Step 3: Device Qualification

Run device qualification before enabling detector mode.

Show:

* phone support status,
* cameras detected,
* whether manual exposure/ISO is available,
* whether multiple cameras can be used,
* detector quality score.

### Step 4: Required Initial Dark Baseline

After device qualification, require an initial baseline scan.

Instruction:

```text
Place the phone face down on a flat surface or put it in a pocket for 60 seconds.

This creates your phone’s baseline. The app uses this to tell the difference between normal camera noise and possible radiation-like events.
```

### Step 5: Optional Feature Setup

Show toggles for:

* notifications,
* event log,
* expert diagnostics,
* model/profile updates,
* anonymous diagnostics,
* dose-band labels,
* calibration mode.

Each toggle must include benefit and data-use explanation.

---

## 8. Required Initial Dark Baseline Scan

The app must not enable normal alarm mode until it has obtained a valid initial dark baseline.

### Minimum Baseline Requirements

The initial baseline must require:

* 60 seconds attempted scan time,
* at least 30 seconds of valid dark/stable frames,
* at least one usable camera channel,
* stable exposure and ISO,
* acceptable motion/stability score,
* acceptable dark-frame brightness,
* acceptable light-leak score,
* hot-pixel map generation,
* baseline candidate event-rate estimate,
* baseline variance estimate.

If the phone has multiple usable cameras, collect baseline data from all usable cameras.

### Baseline Quality Levels

| Quality     | Meaning                                                      | App Behavior                     |
| ----------- | ------------------------------------------------------------ | -------------------------------- |
| **Good**    | Enough valid dark data for normal operation                  | Enable normal alarm mode         |
| **Fair**    | Usable but limited confidence                                | Enable alarm mode with warning   |
| **Poor**    | Not enough valid dark/stable data                            | Keep app in Limited Sensitivity  |
| **Invalid** | Light leak, motion, unstable camera, or unsupported behavior | Require another baseline attempt |

### Baseline Status Display

Always show baseline status somewhere in the app.

Examples:

```text
Baseline: Good
Last dark baseline: Today
Detection confidence: Normal
```

```text
Baseline: Fair
More dark data recommended
Place phone face down for 60 seconds to improve sensitivity.
```

```text
Baseline: Poor
Detection limited
Initial dark scan required.
```

---

## 9. Periodic Dark Baseline Reminders

The app must track whether it has enough recent valid dark-frame data.

### Reminder Triggers

Show a reminder when:

* no valid baseline exists,
* baseline is Poor or Invalid,
* baseline is stale,
* insufficient opportunistic dark data has been collected recently,
* phone temperature/noise behavior changed,
* app was updated,
* Android OS was updated,
* camera behavior changed,
* user enabled a new camera channel,
* repeated scans are Limited Sensitivity,
* false-alarm rejection confidence is low,
* not enough valid dark data has been collected in the last 24–72 hours.

### Reminder Logic

```text
If baseline quality is Poor or Invalid:
  require dark scan before normal alarm mode.

If baseline quality is Fair:
  remind once per day until improved.

If baseline quality is Good but no valid dark data in last 72 hours:
  show soft reminder.

If app/OS/camera profile changes:
  require or strongly recommend baseline refresh.
```

### Reminder Wording

Soft reminder:

```text
Baseline refresh recommended

The app has not collected enough recent dark-frame data. Place the phone face down for 60 seconds to improve sensitivity and reduce false alarms.
```

Required reminder:

```text
Initial dark scan required

The app needs a dark baseline before it can provide radiation-like anomaly alerts.
```

---

## 10. Manual Baseline Refresh

Add a **Refresh Baseline** button in Settings and on the main detector screen when baseline is Fair, Poor, Invalid, or stale.

### Workflow

1. User taps **Refresh Baseline**.
2. App instructs user to place phone face down or in pocket.
3. App collects dark/stable frames.
4. App updates hot-pixel map and baseline model.
5. App reports new baseline quality.

### Refresh Durations

| Refresh Type                 |     Duration |
| ---------------------------- | -----------: |
| Quick refresh                |   30 seconds |
| Standard refresh             |   60 seconds |
| High-confidence refresh      |  180 seconds |
| Calibration-support baseline | 3–10 minutes |

Default setup should use **60 seconds**.

---

## 11. User Modes

### Mode A: Quick Check

User opens app and starts a scan.

Workflow:

1. Check camera validity.
2. Check dark/near-dark condition.
3. If bright or unstable, show Limited Sensitivity.
4. If valid, count radiation-like candidate events.
5. Update result at 10, 30, 60, and 180 seconds.

### Mode B: Face-Down Scan

User places phone face down.

Workflow:

1. Detect face-down/stable/dark state.
2. Use all valid cameras.
3. Count events over 30–180 seconds.
4. Give higher-confidence alarm category.

This is the highest-sensitivity no-accessory mode.

### Mode C: Pocket / Patrol Mode

User intentionally enables Patrol Mode.

Workflow:

1. App runs with visible status/foreground indication.
2. App waits for naturally dark/stable periods.
3. App captures short bursts.
4. App maintains baseline.
5. App alarms if event rate rises above baseline.

No GPS. No cloud. No photos saved.

### Mode D: Open-Camera Limited Mode

Camera is uncovered in a normal bright scene.

Workflow:

1. Do not attempt sensitive detection.
2. Only watch for gross abnormalities.
3. Display: “Limited Sensitivity — dark scan recommended.”

Do not market this as the primary detection mode.

---

## 12. Battery-Aware Opportunistic Dark Sensing

Opportunistic dark sensing must minimize battery drain.

The app must not continuously run high-frame-rate camera capture unless the user explicitly starts an active scan.

### Duty-Cycle Model

1. **Idle monitoring**

   * No continuous camera capture.
   * Use low-cost signals where available, such as screen state, motion/orientation, charging state, and recent app activity.

2. **Dark opportunity check**

   * Briefly sample frames to determine if camera is dark and stable.
   * If poor conditions, stop quickly.

3. **Short burst capture**

   * If conditions are good, collect a short burst.
   * Process frames immediately.
   * Store only summary statistics.
   * Close or pause camera.

4. **Active scan**

   * User intentionally starts scan.
   * Higher frame rate and longer capture are allowed.

### Suggested Duty Cycle

| Mode                 | Camera Use                                       | Purpose                    |
| -------------------- | ------------------------------------------------ | -------------------------- |
| **Idle**             | No camera or rare short probe                    | Save battery               |
| **Patrol Low Power** | 1–5 sec burst every few minutes when likely dark | Maintain baseline          |
| **Patrol Enhanced**  | 5–15 sec burst when pocket/face-down/stable      | Improve sensitivity        |
| **Guided Scan**      | 30–180 sec active scan                           | User-requested measurement |
| **Calibration**      | 3–10 min active scan                             | Expert/agency only         |

### Short-Frame Strategy

Prefer many short frames over long exposures.

Initial test settings:

```text
Exposure: 10–50 ms
Frame burst: 1–5 seconds
Frame rate: 5–15 fps in low-power patrol
Frame rate: 15–30 fps in active scan
Resolution: lowest usable resolution that still preserves event detection
```

Rationale:

* short frames reduce thermal buildup,
* short bursts limit battery drain,
* transient events can still be counted,
* hot pixels and thermal noise are easier to reject,
* sustained camera/CPU/GPU use is avoided.

### Adaptive Opportunistic Scanning

```text
If baseline quality is Poor:
  request user-guided dark scan instead of draining battery.

If baseline quality is Fair:
  opportunistic bursts may occur more often, but still capped.

If baseline quality is Good:
  use infrequent maintenance bursts.

If battery is low:
  reduce or disable opportunistic scanning.

If phone is charging:
  opportunistic baseline refresh may run more often.

If phone is warm/hot:
  pause opportunistic scanning.

If repeated dark opportunities fail:
  back off and remind user to run a guided 60-second dark scan.
```

### Battery Safeguards

The app must:

* pause opportunistic scanning below configurable battery threshold, such as 20%,
* pause scanning if phone is warm/hot,
* avoid camera use when another app is using the camera,
* avoid continuous camera capture in Patrol Mode by default,
* show a foreground notification when Patrol Mode is active,
* allow user to choose Battery Saver / Balanced / Max Sensitivity.

### Patrol Battery Settings

| Setting             | Behavior                                                |
| ------------------- | ------------------------------------------------------- |
| **Battery Saver**   | Rare short bursts; mostly reminders                     |
| **Balanced**        | Default; short bursts during likely dark/stable periods |
| **Max Sensitivity** | More frequent bursts; higher battery use; clear warning |

Example UI copy:

```text
Patrol Mode Battery Use

Battery Saver: lowest battery use, fewer opportunistic checks.
Balanced: recommended.
Max Sensitivity: more frequent dark-frame checks and higher battery use.
```

---

## 13. Opportunistic Capture Trigger Logic

Only start opportunistic camera bursts when conditions suggest useful data.

### Possible Triggers

* phone appears face down,
* phone is still,
* screen is off or app is in Patrol Mode,
* baseline is stale,
* phone is charging,
* recent frames showed darkness,
* user has enabled Patrol Mode,
* user has not completed recent dark baseline.

### Avoid Capture When

* phone is moving heavily,
* screen is actively used,
* battery is low,
* phone is hot,
* baseline is already good and recent,
* repeated recent checks failed,
* camera access would disrupt other apps.

### Pseudocode

```text
opportunisticScheduler():
  if patrolModeDisabled:
    return

  if batteryLow or phoneHot:
    scheduleLater()
    return

  if baselineGoodAndRecent:
    useLongBackoff()
    return

  if likelyDarkAndStable():
    startShortCameraProbe()
  else:
    scheduleLater()
    return

shortCameraProbe():
  openCameraLowRate()
  captureFrames(duration = 1 to 3 seconds)

  if darkQualityPoor:
    closeCamera()
    increaseBackoff()
    return

  captureBurst(duration = 3 to 10 seconds)
  processFrames()
  updateBaselineIfSafe()
  closeCamera()
  scheduleNextBasedOnNeed()
```

---

## 14. Baseline Freeze Rule

The app must freeze baseline updates when:

* Low Anomaly or higher alarm state is active,
* combined Z score exceeds baseline-update threshold,
* dark data quality is poor,
* light leak is suspected,
* motion is too high,
* exposure or ISO changed,
* camera temperature/noise appears unstable,
* scan is Invalid or Limited Sensitivity.

Opportunistic dark data may improve a valid baseline, but it must not overwrite the baseline during suspicious conditions.

---

## 15. Detection Physics Approach

The app should detect sparse transient sensor events that may be caused by gamma/X-ray interactions.

Core assumptions:

* radiation-like events are sparse,
* they are transient,
* they should not repeatedly occur at the exact same pixel,
* they should not correlate with visible scene edges,
* they are most detectable in dark/stable frames,
* thermal noise, hot pixels, light leaks, and processing artifacts are major false-positive sources.

---

## 16. Camera Acquisition Engine

Use Camera2.

Requirements:

* enumerate camera IDs,
* determine physical cameras,
* determine supported formats,
* determine manual exposure support,
* determine ISO/sensitivity support,
* determine frame rate,
* determine RAW/YUV availability,
* lock exposure if possible,
* lock ISO if possible,
* lock focus if possible,
* avoid flash,
* avoid HDR/night/computational modes,
* collect per-frame metadata,
* drop frames if exposure or ISO changes.

Preferred frame formats:

1. RAW_SENSOR if usable.
2. YUV_420_888 if RAW is too slow.
3. Avoid JPEG for detection.

Exposure strategy:

* prefer many short frames over long exposures,
* initial exposure target: 10–50 ms,
* use stable, repeatable settings,
* tune per phone model.

---

## 17. Device Qualification Engine

On first launch:

1. List available cameras.
2. Test manual control.
3. Test YUV/RAW access.
4. Test dark-frame stability.
5. Test hot-pixel rate.
6. Test multi-camera access.
7. Test frame timing stability.
8. Assign detector score per camera.
9. Assign overall phone support level.

Camera score should include:

* manual control score,
* dark noise score,
* hot-pixel score,
* frame stability score,
* multi-camera availability,
* processing artifact risk,
* thermal stability.

---

## 18. Opportunistic Dark Engine

The app should automatically identify when the camera is naturally dark or near-dark.

Useful states:

* pocket-dark,
* face-down dark,
* holster-dark,
* dim stable scene,
* night/dark vehicle interior.

Bad states:

* bright scene,
* moving scene,
* glare,
* exposure changing,
* edge light leak,
* rapid motion,
* thermal instability,
* camera switching modes.

Dark quality score should use:

* mean brightness,
* brightness variance,
* edge brightness,
* frame-to-frame stability,
* exposure metadata,
* motion/orientation data,
* hot-pixel stability,
* valid-frame fraction.

---

## 19. Sparse Event Detection Engine

For each valid frame:

1. Extract luma or raw plane.
2. Subtract baseline/dark model.
3. Apply hot-pixel mask.
4. Compute local median/MAD threshold.
5. Detect high-delta pixels.
6. Group connected components.
7. Extract cluster features.
8. Classify cluster as candidate or reject.
9. Store candidate count and quality metrics.

Cluster features:

* pixel count,
* peak value,
* integrated intensity,
* duration,
* shape,
* eccentricity,
* repetition at same pixel,
* proximity to edge,
* relation to motion,
* relation to whole-frame brightness change,
* local background.

Reject:

* persistent hot pixels,
* repeating warm pixels,
* whole-frame brightness jumps,
* light leaks,
* motion artifacts,
* scene-edge artifacts,
* compression/block artifacts,
* autofocus/exposure shifts.

---

## 20. Background and Noise Model

Each camera gets a local digital baseline model.

Store per camera:

* baseline candidate events/minute,
* baseline variance,
* hot-pixel map,
* warm-pixel map,
* false event rate,
* dark-frame brightness distribution,
* exposure/ISO settings,
* valid temperature range or proxy,
* last baseline update,
* app/OS version associated with baseline.

Maintain:

* short-term baseline,
* session baseline,
* daily baseline,
* long-term baseline.

Important:

* Do not update baseline during suspected anomaly.
* Freeze baseline when alarm state is Low Anomaly or higher.

---

## 21. Multi-Camera Counting

Use multiple cameras as additional counting area, not as coincidence detectors.

Do not require simultaneous event coincidence.

For each camera:

```text
Net_i = N_i - B_i
Z_i = Net_i / sqrt(B_i + systemVariance_i)
```

Combined:

```text
Net_combined = Σ weighted Net_i
Variance_combined = Σ weighted variance_i + systemVariance
Z_combined = Net_combined / sqrt(Variance_combined)
```

Camera weights should depend on:

* valid-frame fraction,
* camera score,
* dark quality,
* false-positive rate,
* frame rate,
* stability,
* historical usefulness.

Expected benefit:

* two good cameras may reduce measurement time,
* three good cameras may further reduce measurement time,
* real-world benefit will be less than ideal because cameras differ.

---

## 22. Alarm Engine

Use sequential statistics, not fixed count times.

Evaluate windows:

* 2 seconds,
* 10 seconds,
* 30 seconds,
* 60 seconds,
* 180 seconds.

Alarm categories:

1. **Baseline**
2. **Low Anomaly**
3. **Elevated**
4. **High Elevated**
5. **Limited Sensitivity**
6. **Invalid**

Placeholder thresholds:

```text
Baseline:
  Z < 3

Low Anomaly:
  Z >= 3 sustained for 30–60 sec

Elevated:
  Z >= 5 sustained for 10–30 sec
  OR clear rising trend across windows

High Elevated:
  Z >= 8 rapidly
  OR very large count-rate increase in short window

Limited Sensitivity:
  too bright, too much motion, too few valid dark frames

Invalid:
  light leak, exposure changes, thermal instability, camera instability
```

Thresholds must be tuned experimentally.

---

## 23. Dose-Band Labeling

Default primary alarm is counts-over-background.

Dose-band labels are broad severity classes, not measurements.

### Non-Calibrated Default Labels

| App Label           | Meaning                                                      |
| ------------------- | ------------------------------------------------------------ |
| Baseline            | No statistically significant increase above learned baseline |
| Low Anomaly         | Weak but sustained increase above baseline                   |
| Elevated            | Clear increase above baseline                                |
| High Elevated       | Strong increase above baseline                               |
| Limited Sensitivity | Conditions do not support reliable classification            |
| Invalid             | Data rejected                                                |

### Optional Dose-Band Class Labels

When enabled:

| App Label     | Approximate Class                                |
| ------------- | ------------------------------------------------ |
| Baseline      | Background / below practical detection threshold |
| Low Anomaly   | ~1–5 mrem/hr class                               |
| Elevated      | ~5–20 mrem/hr class                              |
| High Elevated | ~20–100 mrem/hr class                            |
| Very High     | >100 mrem/hr class                               |

Use wording:

```text
Estimated band: 5–20 mrem/hr class
Screening estimate only
```

Avoid wording:

```text
Dose rate: 12.4 mrem/hr
```

---

## 24. Calibration Strategy

Calibration is optional and secondary.

### Default

No calibration. Counts-over-background only.

### Level 1: Phone-Model Profile

Developer or agency validates a phone model and ships a model profile.

Profile includes:

* phone model,
* camera IDs,
* detector score,
* expected dark baseline,
* camera weights,
* alarm thresholds,
* approximate dose-band mapping,
* uncertainty.

### Level 2: Device-Specific Calibration

Agency or expert calibrates the exact phone.

Workflow:

1. Run dark baseline.
2. Place phone in known radiation field.
3. Enter known dose-rate or dose band.
4. Collect 3–10 minutes of valid dark/stable frames.
5. Fit coefficient.
6. Store calibration locally.
7. Mark calibration stale after app/OS/camera changes.

### Level 3: QA Check

Periodic check to confirm response still resembles calibration profile.

Calibration should output bands, not exact readings, unless expert mode explicitly enables approximate numeric display.

---

## 25. Estimated Performance Targets

Internal development targets only, not marketing claims.

| Approx Field   | Expected Behavior                        |
| -------------- | ---------------------------------------- |
| Background     | No reliable detection claim              |
| <1 mrem/hr     | Not reliable                             |
| 1–5 mrem/hr    | Borderline; may require long dark scan   |
| 5–20 mrem/hr   | Plausible with 1–3 min valid dark scan   |
| 20–100 mrem/hr | Main useful target range; 10–90 sec goal |
| >100 mrem/hr   | Faster alarm, but not main focus         |

Do not overbuild around >100 mrem/hr. The main target is lower-to-moderate anomalous fields.

---

## 26. UI Screens

### Onboarding

Includes:

* safety disclaimer,
* privacy summary,
* camera permission explanation,
* optional feature explanation,
* no GPS/no cloud/no photos statement.

### Device Check

Shows:

* phone support status,
* cameras detected,
* detector quality,
* multi-camera availability,
* recommendation.

### Initial Baseline Screen

Shows:

* baseline explanation,
* instruction to place phone face down or in pocket,
* progress bar,
* valid-frame count,
* baseline quality result.

### Main Detector Screen

Shows:

* current status,
* confidence,
* dose-band class if enabled,
* scan quality,
* baseline status,
* Patrol battery setting,
* recommended action.

Example:

```text
Status: Elevated
Estimated band: 5–20 mrem/hr class
Confidence: Medium
Baseline: Good
Mode: Non-calibrated screening
Action: Hold still or place phone face down for 60 seconds. Confirm with proper meter.
```

### Guided Scan Screen

Shows:

* “Place phone face down.”
* “Hold still.”
* “Too bright — limited sensitivity.”
* “Good dark-frame quality.”
* countdown,
* live confidence.

### Expert Screen

Shows:

* counts/min,
* baseline counts/min,
* net counts,
* sigma above background,
* active camera count,
* valid-frame fraction,
* invalid-frame reasons,
* camera scores,
* dose-band mapping status,
* calibration status.

### Settings Screen

Includes toggles for:

* notifications,
* event log,
* expert diagnostics,
* optional model/profile updates,
* optional anonymous reports,
* dose-band labels,
* calibration mode,
* baseline reminders,
* Patrol battery mode,
* delete local data,
* export local logs.

---

## 27. Competitive Complaint Lessons to Address

Existing phone-camera radiation apps appear to trigger several user complaints that this app should explicitly avoid.

Design responses:

1. **Phone incompatibility**

   * Use device qualification.
   * Clearly label Supported / Experimental / Limited / Not Supported.

2. **Long initialization**

   * Require a shorter 60-second baseline.
   * Use opportunistic short bursts afterward.
   * Allow longer high-confidence scans only when user chooses.

3. **Camera covering friction**

   * Do not make “cover the camera” the main message.
   * Use face-down, pocket-dark, and opportunistic dark workflows.

4. **Privacy concerns**

   * No GPS by default.
   * No cloud by default.
   * No photos saved by default.
   * Explain optional permissions clearly.

5. **Unclear results**

   * Default to simple labels.
   * Provide expert counts/Z score.
   * Provide broad dose-band classes only as screening estimates.

6. **Crashes/reliability**

   * Prioritize stability.
   * Use aggressive invalid-state handling.
   * Avoid unsupported camera modes.

7. **Calibration limitations**

   * Keep non-calibrated counts-over-background as default.
   * Treat calibration as optional expert mode.

---

## 28. Data Storage

Default local logs should contain:

* timestamp,
* alarm category,
* confidence,
* active cameras,
* valid-frame fraction,
* candidate event rate,
* baseline event rate,
* net count rate,
* Z score,
* dose-band class if enabled,
* calibration status,
* app version,
* phone model.

Do not store:

* photos,
* raw frames,
* GPS,
* audio,
* contacts,
* personal files.

Debug frame storage must be developer-only and opt-in.

---

## 29. Safety Language

Use in onboarding and settings:

```text
This app is a screening aid only.

It is not a dosimeter, radiation survey meter, contamination meter, isotope identifier, or PRD replacement.

Do not use it for regulatory compliance, dose assessment, clearance, package receipt, emergency response decisions, or contamination surveys.

Confirm alarms with a proper calibrated radiation instrument or trained radiation specialist.
```

Alarm action language:

```text
Radiation-like signal above baseline.
Increase distance if safe to do so.
Confirm with a calibrated radiation instrument or radiation specialist.
```

---

## 30. Algorithm Pseudocode

### Main Detector Loop

```text
startDetector():
  qualifyDevice()
  selectUsableCameras()

  if noValidInitialBaseline:
    requireInitialDarkBaseline()
    return

  lockCameraSettings()

  while detectorActive:
    frames = acquireFramesFromActiveCameras()
    motionState = readMotionState()
    darkState = classifyDarkState(frames, motionState, metadata)

    if darkState.invalid:
      emit INVALID or LIMITED_SENSITIVITY
      continue

    for each cameraFrame:
      metrics = processFrame(cameraFrame)
      updateWindowBuffers(metrics)

    if enoughValidFrames:
      perCameraStats = computePerCameraStats()
      combinedStats = combineCameraStats(perCameraStats)
      alarm = evaluateAlarm(combinedStats)
      emitAlarmState(alarm)

      if alarm == BASELINE and baselineUpdateSafe:
        updateBaseline(perCameraStats)
      else:
        freezeBaseline()
```

### Frame Processing

```text
processFrame(frame):
  luma = extractLumaOrRawPlane(frame)
  corrected = subtractDarkModel(luma)
  corrected = applyHotPixelMask(corrected)

  highDeltaMap = detectHighDeltaPixels(corrected)
  clusters = groupConnectedComponents(highDeltaMap)

  candidateEvents = []

  for cluster in clusters:
    features = extractClusterFeatures(cluster)

    if isRadiationLike(features):
      candidateEvents.add(cluster)
    else:
      reject(cluster)

  return DetectorFrameMetrics(
    candidateEvents,
    rejectedHotPixels,
    rejectedArtifacts,
    validFrame,
    invalidReason
  )
```

### Alarm Evaluation

```text
evaluateAlarm(stats):
  if stats.invalid:
    return INVALID

  if stats.limitedSensitivity:
    return LIMITED_SENSITIVITY

  if stats.Z_2s >= highThreshold:
    return HIGH_ELEVATED

  if stats.Z_10s >= elevatedThreshold:
    return ELEVATED

  if stats.Z_30s >= elevatedThreshold:
    return ELEVATED

  if stats.Z_60s >= lowThreshold:
    return LOW_ANOMALY

  return BASELINE
```

---

## 31. Development Phases

### Phase 0: Technical Spike

Deliverables:

* Android Kotlin project.
* Camera2 camera discovery.
* Camera capability display.
* Single-camera YUV frame capture.
* Manual exposure/ISO lock attempt.
* Frame mean/variance display.
* Local debug screen.

Acceptance:

* App can capture stable dark frames from at least one camera.
* App can report camera metadata.
* App can detect whether exposure/ISO are stable.

### Phase 1: Single-Camera Detector

Deliverables:

* dark-state classifier,
* hot-pixel map,
* sparse event detector,
* rolling baseline,
* candidate events/minute,
* Z score,
* basic alarm states.

Acceptance:

* hot pixels are rejected,
* light leaks are flagged,
* synthetic transient events are detected,
* baseline does not update during simulated anomaly.

### Phase 2: Required Initial Baseline

Deliverables:

* 60-second baseline workflow,
* Good/Fair/Poor/Invalid baseline scoring,
* baseline status display,
* Refresh Baseline button,
* stale baseline reminders.

Acceptance:

* app cannot enter normal alarm mode without valid initial baseline,
* baseline quality is displayed clearly,
* baseline can be refreshed manually.

### Phase 3: Multi-Camera Support

Deliverables:

* multi-camera discovery,
* per-camera detector scores,
* optional simultaneous or sequential capture,
* combined statistic,
* camera weighting.

Acceptance:

* app can use 2+ cameras on supported phones or fall back gracefully,
* combined score updates in real time,
* invalid/noisy cameras can be down-weighted or disabled.

### Phase 4: Opportunistic Dark and Battery-Aware Patrol

Deliverables:

* pocket/face-down detection,
* short burst capture,
* Patrol battery settings,
* battery/thermal safeguards,
* adaptive backoff,
* foreground Patrol indication.

Acceptance:

* app does not continuously run camera in Patrol Mode by default,
* app uses short bursts,
* app pauses when battery is low or phone is hot,
* app recommends guided scan when baseline is poor.

### Phase 5: Guided Scans and UX

Deliverables:

* guided face-down scan,
* Quick Check mode,
* Patrol Mode screen,
* Limited Sensitivity messaging,
* recommended action text.

Acceptance:

* user can understand how to improve scan quality,
* app distinguishes good dark scan from poor conditions.

### Phase 6: Alarm UX and Dose-Band Labels

Deliverables:

* main user screen,
* alarm labels,
* dose-band class labels,
* confidence display,
* expert diagnostics.

Acceptance:

* default user sees simple alarm category,
* no exact dose rate by default,
* dose-band labels are broad and clearly marked as screening estimates.

### Phase 7: Permission Onboarding and Privacy Controls

Deliverables:

* permission setup flow,
* optional feature toggles,
* settings privacy summary,
* delete local data,
* local event log toggle,
* export local logs.

Acceptance:

* camera permission has clear explanation,
* optional features explain benefit and data use,
* app does not request GPS,
* app does not save photos by default.

### Phase 8: Calibration Mode

Deliverables:

* expert-only calibration workflow,
* phone-model profile support,
* device-specific calibration file,
* calibration stale warning,
* approximate dose-band mapping.

Acceptance:

* calibration is optional,
* non-calibrated mode remains default,
* calibration produces bands, not misleading exact values.

### Phase 9: Validation and Pilot Readiness

Deliverables:

* synthetic test harness,
* bench test protocol,
* radiation validation protocol,
* false-alarm test report,
* supported phone profile,
* pilot guide,
* safety limitations document.

Acceptance:

* app can be tested against known fields,
* performance limitations are documented,
* phone model support is documented,
* pilot build is ready for controlled evaluation.

---

## 32. Testing Plan

### Unit Tests

Test:

* Z-score calculation,
* rolling baseline,
* baseline freeze,
* hot-pixel rejection,
* cluster detection,
* alarm thresholds,
* dose-band mapping,
* multi-camera weighting,
* invalid-frame logic.

### Synthetic Frame Tests

Generate synthetic data for:

* dark noise,
* hot pixels,
* warm pixels,
* transient random clusters,
* light leak,
* frame brightness shift,
* motion artifact,
* mixed valid/invalid frames.

Acceptance:

* transient clusters are detected,
* persistent pixels are rejected,
* light leaks are rejected,
* baseline does not absorb anomalies.

### Phone Bench Tests

For each candidate phone:

* cool dark baseline,
* warm dark baseline,
* pocket-dark scan,
* face-down scan,
* bright scene,
* motion scene,
* multi-camera availability,
* battery/thermal behavior,
* long patrol run.

### Radiation Validation Tests

Under proper controls:

* background,
* low field,
* moderate field,
* 5–20 mrem/hr class if available,
* 20–100 mrem/hr class if available,
* different orientations,
* repeated trials,
* comparison against calibrated instrument.

Measure:

* response time,
* false alarm rate,
* valid-frame fraction,
* threshold performance,
* model-to-model variation,
* orientation dependence,
* temperature dependence.

---

## 33. Repository Structure

```text
radiation-phone-detector/
  app/
    src/main/java/...
      camera/
        CameraDiscovery.kt
        CameraSessionManager.kt
        FrameAcquirer.kt
        CameraProfile.kt
      detector/
        DarkStateClassifier.kt
        SparseEventDetector.kt
        HotPixelMap.kt
        BaselineModel.kt
        FrameMetrics.kt
      baseline/
        InitialBaselineWorkflow.kt
        BaselineQuality.kt
        BaselineReminderScheduler.kt
      alarm/
        AlarmEngine.kt
        DoseBandMapper.kt
        SequentialStats.kt
      sensors/
        MotionStateProvider.kt
        BatteryThermalStateProvider.kt
      patrol/
        OpportunisticScheduler.kt
        PatrolBatteryMode.kt
        ShortBurstCapture.kt
      data/
        LocalLogStore.kt
        PhoneProfileStore.kt
      calibration/
        CalibrationEngine.kt
        CalibrationProfile.kt
      privacy/
        PermissionExplainer.kt
        DataUseSettings.kt
      ui/
        onboarding/
        devicecheck/
        baseline/
        main/
        guidedscan/
        expert/
        settings/
        calibration/
  docs/
    PRODUCT_SPEC.md
    DETECTION_ALGORITHM.md
    PRIVACY_POLICY_DRAFT.md
    SAFETY_LIMITATIONS.md
    VALIDATION_PLAN.md
    TEST_PROTOCOL.md
    PHONE_PROFILE_FORMAT.md
    CALIBRATION_MODE_SPEC.md
    USER_GUIDE.md
    EXPERT_MODE_GUIDE.md
    PERMISSION_AND_DATA_USE.md
    BASELINE_AND_PATROL_MODE.md
  tools/
    synthetic_frame_generator/
    analysis_notebooks/
  README.md
```

---

## 34. Documentation Required

Create:

1. `README.md`
2. `PRODUCT_SPEC.md`
3. `DETECTION_ALGORITHM.md`
4. `PRIVACY_POLICY_DRAFT.md`
5. `SAFETY_LIMITATIONS.md`
6. `VALIDATION_PLAN.md`
7. `TEST_PROTOCOL.md`
8. `PHONE_PROFILE_FORMAT.md`
9. `CALIBRATION_MODE_SPEC.md`
10. `USER_GUIDE.md`
11. `EXPERT_MODE_GUIDE.md`
12. `PERMISSION_AND_DATA_USE.md`
13. `BASELINE_AND_PATROL_MODE.md`
14. `KNOWN_LIMITATIONS.md`

---

## 35. Marketing Language Guardrails

Allowed:

* “Radiation-like anomaly detection.”
* “Camera-only screening aid.”
* “Counts above learned baseline.”
* “Estimated dose-band class.”
* “No GPS by default.”
* “No cloud by default.”
* “No photos saved by default.”
* “Confirm with proper instrument.”

Do not say:

* “Geiger counter.”
* “PRD replacement.”
* “Measures radiation dose rate.”
* “Certified detector.”
* “Detects all radiation.”
* “Detects contamination.”
* “Safe/unsafe determination.”
* “Regulatory survey tool.”

---

## 36. MVP Acceptance Criteria

The MVP is successful if:

1. It runs on at least one approved Android phone.
2. It uses camera permission as the only required permission.
3. It does not request GPS.
4. It does not save images by default.
5. It runs device qualification.
6. It requires an initial dark baseline scan.
7. It classifies baseline as Good/Fair/Poor/Invalid.
8. It clearly displays baseline status.
9. It reminds the user when baseline data is stale or insufficient.
10. It offers manual Refresh Baseline.
11. It freezes baseline updates during suspected anomalies.
12. It can classify dark/stable vs limited/invalid conditions.
13. It can count sparse transient candidate events.
14. It maintains a per-camera baseline.
15. It supports multiple cameras where available.
16. It combines camera channels statistically.
17. It does not continuously run camera in Patrol Mode by default.
18. It uses short bursts for opportunistic dark data.
19. It pauses opportunistic scanning when battery is low.
20. It pauses/reduces scanning when phone is warm/hot.
21. It includes Battery Saver, Balanced, and Max Sensitivity Patrol settings.
22. It displays Baseline / Low Anomaly / Elevated / High Elevated / Limited / Invalid.
23. It labels broad dose-band classes when enabled.
24. It does not display exact dose rate by default.
25. It includes optional calibration mode.
26. It includes clear safety limitations.
27. It has local-only logs.
28. It has expert diagnostics.
29. It includes a validation test plan.

---

## 37. First Implementation Task List

1. Create Android Kotlin project.
2. Add Camera2 camera discovery.
3. Display camera IDs and capabilities.
4. Implement single-camera YUV capture.
5. Attempt manual exposure/ISO/focus lock.
6. Compute frame brightness and variance.
7. Build dark-state classifier.
8. Build device qualification scoring.
9. Build initial 60-second baseline workflow.
10. Build baseline quality scoring.
11. Build hot-pixel map.
12. Build sparse event detector.
13. Display candidate events/minute.
14. Implement rolling baseline.
15. Implement baseline freeze rule.
16. Implement Z-score calculation.
17. Implement alarm states.
18. Add invalid/limited-sensitivity logic.
19. Add motion/orientation stability checks.
20. Add guided face-down scan mode.
21. Add baseline reminders.
22. Add Refresh Baseline button.
23. Add multi-camera discovery.
24. Add multi-camera weighting and combined Z score.
25. Add battery-aware opportunistic scheduler.
26. Add short burst patrol capture.
27. Add Patrol battery settings.
28. Add dose-band labels.
29. Add onboarding and permission explanation screens.
30. Add optional feature toggles.
31. Add local event log.
32. Add expert diagnostics.
33. Add CSV export.
34. Add calibration profile data model.
35. Add optional calibration workflow.
36. Add synthetic frame generator tests.
37. Add validation documents.
38. Add privacy and safety documents.
39. Build pilot APK.

---

## 38. Final Product Definition

This project should produce:

> A privacy-first, software-only Android radiation anomaly screening app that uses approved phone camera sensors, a required initial dark baseline, opportunistic dark sensing, battery-aware short-burst patrol mode, multi-camera counting, and counts-over-background statistics to provide broad alarm and dose-band classes without GPS, cloud upload, saved photos, or default dose-rate claims.
