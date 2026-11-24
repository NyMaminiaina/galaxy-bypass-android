# Galaxy Bypass for Android 🚀

**Power Bypass & GOS manager that runs on the phone. No PC, no cable.**

![Platform](https://img.shields.io/badge/platform-Android%2011%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Release](https://img.shields.io/badge/release-v1.0.0-blue?style=for-the-badge)
![License](https://img.shields.io/badge/license-MIT-green?style=for-the-badge)

*🎮 Samsung Galaxy gaming tool · Power Bypass · Disable GOS · Gaming Performance Mode*

[📥 Download latest](https://github.com/NyMaminiaina/galaxy-bypass-android/releases/latest) •
[🐛 Report issues](https://github.com/NyMaminiaina/galaxy-bypass-android/issues)

---

## 🔥 What this app does

This is the on-device version of
[MNDL-27/galaxy-bypass-utility](https://github.com/MNDL-27/galaxy-bypass-utility).
It runs the **same four commands** as `Battery Bypass Tool.bat`, in the same order, straight from your phone:

- 🔋 **Enables Power Bypass**: the charger powers the phone directly, so the battery stays cooler while you play
- 🚫 **Disables GOS** (Game Optimization Service), plus Game Tools and Game Launcher
- 🔄 **Restores everything** with one tap
- ✅ **Shows the mode you are really in**: the app reads the phone's settings back after every change
- 📱 **No PC needed**: it uses [Shizuku](https://shizuku.rikka.app/) (started once with Wireless debugging)
- 🌍 English and French interface

---

## ✨ Two modes

| 🚀 **Gaming Performance Mode** | 🔄 **Normal Mode (Default)** |
|---|---|
| ✅ Power Bypass ON | ❌ Power Bypass OFF |
| ❌ Game Tools disabled | ✅ Game Tools enabled |
| ❌ Game Launcher disabled | ✅ Game Launcher enabled |
| ❌ GOS disabled | ✅ GOS enabled |
| **Result:** maximum performance for gaming | **Result:** factory default behavior |

The top card of the app always shows which one is active (or *Custom configuration* if the settings are mixed).

| Action | Command (run as shell) |
|---|---|
| Power Bypass ON / OFF | `settings put system pass_through 1` / `0` |
| Game Tools, Game Launcher, GOS OFF | `pm disable-user com.samsung.android.game.{gametools,gamehome,gos}` |
| Restore | `pm enable <same packages>` |

---

## 🎯 Quick start

1. **Install [Shizuku](https://shizuku.rikka.app/download/)** on the phone
2. Settings → About phone → Software information → tap **Build number** 7 times
3. Settings → Developer options → turn on **Wireless debugging** (the phone must be on Wi-Fi)
4. In Shizuku: **Start via Wireless debugging**, pair with the code shown on screen, then Start
5. **Install the APK** from the [latest release](https://github.com/NyMaminiaina/galaxy-bypass-android/releases/latest) and open it
6. Tap **Allow** when the app asks to use Shizuku
7. Plug a **20 W+ USB-PD charger** and tap **Enable Power Bypass & Disable GOS**

> Shizuku (Wireless debugging mode) has to be started again after each reboot of the phone.

---

## 📱 Compatibility

**✅ Target:** Galaxy S20 / S21 / S22 / S23 / S24, Note 20, Z Fold / Z Flip (same list as the original project), Android 11 or newer
**❌ Not supported:** Galaxy A / M series, pre-2020 devices
**⚠️ Note:** the Galaxy S21 FE cannot physically enable power bypass charging.

> **Early release.** The command logic is unit-tested. The Shizuku layer is still being validated on real devices,
> so if something breaks on your model, please open an issue with your device and One UI version.

---

## ⚠️ Important notes

- 🌡️ **Watch the temperature.** GOS also protects the phone from overheating, so without it the device can get hot
- ❄️ A mobile cooler is recommended for long sessions
- 🔌 Power Bypass needs a **20 W+ USB-PD charger**
- 🔄 **Fully reversible**: *Normal Mode (Default)* puts everything back
- 🔋 **The battery stops around 20 % while bypass is active.** This is expected, not a bug. To charge normally again, switch back to Normal Mode (Default)

---

## 🛠️ Build from source

Android Studio (JDK 17+), open the folder, or:

```
./gradlew test               # unit tests (commands, allow-list, mode detection)
./gradlew assembleRelease    # app/build/outputs/apk/release/app-release.apk
```

The APK is signed with the debug key, which is fine for personal use.

### How it works

```
Compose UI ─ MainViewModel ─ ShizukuController ══(Shizuku)══▶ ShellService (shell uid)
                  │                                                │
              Bypass.kt  (commands, allow-list, mode)  ◀───────────┘ re-checks the allow-list
```

`ShellService` only runs the exact commands listed above, so it is not a generic shell.

---

## 🙏 Credits

Based on [MNDL-27/galaxy-bypass-utility](https://github.com/MNDL-27/galaxy-bypass-utility) (MIT).
The commands and package names come from that project.

## 📄 License

MIT, see the [LICENSE](LICENSE) file. Use at your own risk.