# Anti-Doom-Scroll

An Android app that limits Instagram Reels to timed sessions with a cooldown, so Reels stays available but can't eat your evening.

- Opening Reels asks whether to start a session (default 30 min). Say "Not now" and nothing is counted.
- When the session ends you're taken out of Reels, and it stays locked for a cooldown (default 90 min).
- Feed, Stories and DMs are never touched. A reel a friend sends you in a DM always plays.
- Everything stays on your phone. The app has no internet permission.

> Not affiliated with, endorsed by, or connected to Instagram or Meta. It works by reading Instagram's screen through Android's accessibility service, so an Instagram update can break detection at any time.

## Install

There is no prebuilt download yet, so you build it yourself. The build script is **Windows-only** (PowerShell).

**You need:**
- [Android Studio](https://developer.android.com/studio) or the Android SDK, with **build-tools 36.0.0** and **platform android-37** installed (SDK Manager).
- A JDK (17 or newer) on your `PATH`.
- An Android 10+ phone with **USB debugging** on (Settings → About phone → tap Build number 7 times → Developer options → USB debugging), connected by USB.

**Then, from the repo folder:**

```powershell
./app/build.ps1 -Install   # runs the tests, builds the APK, installs it on the phone
```

Without `-Install`, the APK is written to `app/build/anti-doom-scroll.apk`.

Open **Anti-Doom-Scroll** on the phone and follow the setup steps to turn on accessibility access.

## Docs

- [Requirements](docs/product/requirements.md): what the app must do, with acceptance tests.
- [Interaction design](docs/product/interaction-design.md): screens, flows and messages.

## How detection works

Found with a throwaway probe (requirements A-01 and A-04; logs in `docs/product/evidence/`):

- Reels is showing when `clips_viewer_view_pager` is visible.
- A reel from a DM also shows the sender name and reply bar, so it is let through.

If Reels stops being limited after an Instagram update, check these view IDs first.

## License

[MIT](LICENSE)
