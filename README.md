# Anti-Doom-Scroll

Personal Android app that limits Instagram Reels to timed sessions with a cooldown (default 30 min, then a 90 min break). Feed, Stories and DMs stay untouched, and reels friends send in DMs are always allowed.

- Requirements: [docs/product/requirements.md](docs/product/requirements.md)
- Interaction design: [docs/product/interaction-design.md](docs/product/interaction-design.md)

## Build and install

Needs the Android SDK (build-tools 36.0.0, platform android-37.0) and a JDK. No Gradle.

```powershell
./app/build.ps1            # runs the session-rule tests, then builds app/build/anti-doom-scroll.apk
./app/build.ps1 -Install   # same, then installs on the USB-connected phone
```

After installing, open the app and follow the setup steps to turn on accessibility access.

## How detection works

Found with a throwaway probe (see requirements A-01 and A-04):

- Reels is showing when `clips_viewer_view_pager` is visible.
- A reel from a DM also shows the sender name and reply bar, so it is let through.

An Instagram update can rename these views; if Reels stops being limited, check them first.
