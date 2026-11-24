# Modern Claude Widget — notes for Claude Code

A Jetpack Glance home-screen widget that deep-links into the Claude Android app.
One module (`app`), package `com.clayboicardi.claudewidget`. No activities, no network.

Longer docs: [README](README.md) · [DESIGN](DESIGN.md) (what Glance can render) ·
[route probe](docs/route-probe.md) (on-device results) ·
[November 2026 audit](docs/audit-2026-11.md) (findings and open proposals).

## Build and test

The check CI runs, and the one to run before every commit:

```
./gradlew lint test assembleDebug --no-daemon --stacktrace
```

- JDK 17 or newer (CI uses 21). Confirm with `./gradlew --version`: on the Windows laptop the
  first `java` on PATH is Java 8, but `JAVA_HOME` is JDK 21, which is what Gradle uses.
- Needs `platforms;android-37.0` and `build-tools;37.0.0`, plus a `local.properties` with
  `sdk.dir=...` (ignored by git; a fresh worktree doesn't have one).
- On Windows, run Gradle and the `android` CLI from **PowerShell** (`.\gradlew.bat ...`). In
  the sandboxed Git Bash every JVM fails with `Unable to establish loopback connection`.
- Tests are JVM-only (`app/src/test`): plain JUnit, Glance's unit-test harness, and
  Robolectric. `ManifestContractTest` fails if a dependency adds a permission or an unguarded
  exported component; update its expected set only after checking the new entry is acceptable.
- Install on a device: `android install --apks=app/build/outputs/apk/debug/app-debug.apk`.

## Probing routes on a device

Tests can't tell where Claude lands or whether the system allows the launch, so every route
change needs on-device evidence, recorded as a new dated section in `docs/route-probe.md`.

Per URI, against `com.anthropic.claude`:

1. `adb shell cmd package resolve-activity --brief -a android.intent.action.VIEW -d <uri> com.anthropic.claude`
2. Cold: `adb shell am force-stop com.anthropic.claude`, then
   `adb shell am start -W -a android.intent.action.VIEW -d <uri> -f 0x10008000 com.anthropic.claude`
   (`0x10008000` = `NEW_TASK | CLEAR_TASK`, the flags the widget uses).
3. Look at the screen (`android layout`, `android screen capture`). Resolving is not the same
   as landing on the right screen.
4. Warm: open a different Claude screen, press Home, fire the URI again, look again.
5. Finally tap the real widget buttons and check logcat for the `START u0 {... claude://...}`
   line with `result code=0`.

An emulator has no Claude app, which makes it the place to test the not-installed fallback
(deep link → Claude launcher → Play Store app → Play Store web page) and widget sizes.

## Boundaries (permanent)

- Product promises: no `INTERNET` permission, no backend or keys, original icons, the
  Clayboicardi look, applicationId `com.clayboicardi.claudewidget`, minSdk 31. Ask Clay before
  changing any of them.
- Never commit to, push to, or merge into `main` without Clay's approval. No force-push, no
  history rewrite, no tags or releases.
- No signing keys or release keystores in the repo.
- Never weaken, skip or delete a test to get to green.
- Probe screenshots can show private content (session titles). They stay out of git
  (`probe_*.png` is ignored) and are never uploaded. A screenshot committed to the repo shows
  only the widget and the home screen.
- On Clay's phone: only this app may be installed, uninstalled or cleared. In Claude, navigate
  only as far as needed to see where a link lands; never type or send anything.
- New features and redesigns are proposals (see the audit doc), not drive-by changes.
