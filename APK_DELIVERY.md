# APK Delivery Process

This repo treats APK delivery as part of normal implementation work. The user
has given standing authorization for future agents to build, commit, push, and
provide APK files without asking for conversational confirmation.

## Default Build Output

- Build type: debug.
- Expected command after Android project setup: `./gradlew test assembleDebug`.
- Expected Windows command after Android project setup:
  `.\gradlew.bat test assembleDebug`.
- Expected APK path after Android project setup:
  `app/build/outputs/apk/debug/app-debug.apk`.

Release APKs require signing configuration. Do not invent or request signing
credentials unless the user explicitly asks for a release build.

## Agent Workflow

After each coding run:

1. Run the most relevant test/build command available.
2. Build a debug APK when the Android project exists.
3. Confirm the APK file exists.
4. Update `IMPLEMENTATION_LOG.md` with tests, build result, APK path, blockers,
   and next tasks.
5. Update `BUILD_PLAN.md` progress when task status changed.
6. Commit the completed work.
7. Push the current tracked branch.
8. Final response must include:
   - commit hash,
   - branch pushed,
   - tests/builds run,
   - APK absolute path if built,
   - any blocker that prevented APK creation.

## Approval Handling

Do not ask separate chat questions before normal build, commit, push, or APK
delivery steps. If the Codex environment itself requires approval for network,
filesystem, credential, or sandbox access, use the required tool approval flow
with the narrowest practical command scope.

## Current Status

Android project setup is complete and the debug APK builds successfully.

- Current debug APK path:
  `C:\Users\fhidi\Documents\Rad phone camera\app\build\outputs\apk\debug\app-debug.apk`
- Current GitHub zip artifact:
  `RadPhoneCamera-debug.zip`
- Current debug app version:
  `0.1.2` / versionCode `3`
- Build command used:
  `.\gradlew.bat test assembleDebug`
- Local SDK path:
  `.android-sdk`

Release APKs are still not configured because no signing configuration has been
provided.
