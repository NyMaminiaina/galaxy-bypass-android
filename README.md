# Galaxy Bypass (Android)

On-device version of [MNDL-27/galaxy-bypass-utility](https://github.com/MNDL-27/galaxy-bypass-utility):
exactly the same four ADB commands as `Battery Bypass Tool.bat` (same order, same wording in the log), but run from the phone, no PC needed.

| Action                               | Command (run as shell)                                              |
| ------------------------------------ | ------------------------------------------------------------------- |
| Power bypass ON / OFF                | `settings put system pass_through 1 / 0`                            |
| Game Tools / Game Launcher / GOS OFF | `pm disable-user com.samsung.android.game.{gametools,gamehome,gos}` |
| Restore                              | `pm enable <same packages>`                                         |

## How it works

`pm disable-user` needs shell privileges, which a normal app does not have. The app uses
**[Shizuku](https://shizuku.rikka.app/)**: you start it once (Wireless debugging, no PC), and the app
talks to a small privileged `ShellService` (AIDL + Shizuku _UserService_).

```
Compose UI ─ MainViewModel ─ ShizukuController ══(Shizuku)══▶ ShellService (uid 2000)
                  │                                                │
              Bypass.kt  (commands, allow-list, parsing)  ◀────────┘ re-checks the allow-list
```

- `ShellService` only executes the exact commands listed above (full-match regex allow-list, no
  chaining/redirection), so it is not a generic root/shell hole.
- The top card shows **Gaming Performance Mode** or **Normal Mode (Default)** (or _Custom configuration_ if the
  settings are mixed), and updates after every change.
- After every action the app **reads the real state back** (`settings get`, `pm list packages -d`)
  and reports "verified", instead of printing DONE like the .bat.
- Packages missing on a device (e.g. no Game Launcher) are skipped.

## Setup on a Galaxy S22

1. Install **Shizuku** (Play Store or https://shizuku.rikka.app/download/).
2. Settings → About phone → Software information → tap _Build number_ 7×.
3. Settings → Developer options → turn on **Wireless debugging** (phone must be on Wi-Fi).
4. In Shizuku: _Start via Wireless debugging_ → pair (code from the Wireless debugging screen) → Start.
5. Install this app, open it, tap **Allow** when it asks to use Shizuku.
6. Plug a 20 W+ USB-PD charger, tap **Gaming mode**.

Shizuku (ADB mode) must be restarted after each phone reboot.

## Build

Android Studio (JDK 17+), open this folder, or:

```
./gradlew test               # unit tests (command generation, allow-list, parsing)
./gradlew assembleRelease    # app/build/outputs/apk/release/app-release.apk (debug-signed, personal use)
```

## Status / known limits

- The pure logic (`Bypass.kt`) is unit-tested (11 tests). The Android/Shizuku layer follows the
  official Shizuku-API demo but has **not been run on a device yet**: expect a first-run debug pass.
- "Verified" confirms the _settings_ took effect. Whether the phone really stops charging the battery
  (true bypass) depends on One UI / hardware; check it on your S22 (battery current/temperature).
- Disabling GOS removes Samsung's thermal throttling protection. Watch the temperature.

## Ideas

Quick Settings tile · auto-enable when a chosen game starts · battery temperature guard ·
built-in wireless-debugging client (e.g. libadb-android) to drop the Shizuku dependency.
