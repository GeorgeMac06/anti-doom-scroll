# Anti-Doom-Scroll — Product Requirements

Status: **Proposed**, decisions D-01 to D-04 accepted by owner 2026-10-08 · Created: 2026-10-08 · Owner: repository owner (sole user)

## 1. Problem and intent

The owner loses time to Instagram Reels on Android. A daily cap (SocialLite, 15 min/day) worked but felt rigid and cost too much. The owner wants to watch Reels whenever they like, in bounded sessions followed by a forced break.

- **Actor:** one person, the owner, on their own Android phone.
- **Intended outcome:** less Reels time per day, while Reels stays available in bounded sessions.
- **Horizon:** personal tool, first usable version.
- **Distribution:** sideloaded APK. No Play Store listing.

Source: owner statements in conversation, 2026-10-08. Background: opportunity evaluation (ln-11), verdict `RECOMMEND A` (personal sideloaded build).

## 2. Scope

### In scope (v1)
- Detect Reels viewing inside the official Instagram Android app.
- Session limit (X) and cooldown (Y), each configurable.
- When access is denied, push the user out of Reels.

### Protected existing behavior
Everything in Instagram outside Reels must keep working normally:
- Home feed, Stories, DMs, profiles, posting, search.

The app must not block or change these.

### Non-goals (v1)
- YouTube Shorts, TikTok, Snapchat, or any app other than Instagram.
- iOS, Play Store publishing, accounts, cloud sync, monetization.
- Daily total caps (the SocialLite model).
- Analytics dashboards beyond the minimal log in R-09.
- Tamper-proofing against a determined user, e.g. uninstalling or disabling the service.

### Optional ideas (not committed)
- Warning before a session ends (see R-07; proposed, but needs owner confirmation).
- Extending to YouTube Shorts later.

## 3. Definitions

| Term | Meaning |
|---|---|
| **Reels view** | Any screen in Instagram showing the full-screen vertical Reels player. Covers the Reels tab, a reel opened from the feed, and a reel opened from a profile. Excludes a **DM reel**. |
| **DM reel** | The single reel a person sent you, opened from a DM thread. It is always allowed (D-02). Swiping from it to further reels creates a normal Reels view. |
| **Session** | A period of allowed Reels viewing. It starts on the first Reels view while the app is Idle. |
| **X (session limit)** | Maximum session length. Default 30 min. |
| **Y (cooldown)** | Period after a session ends when Reels is blocked. Default 90 min. |

### States

```
Idle ──(enter Reels)──▶ InSession ──(X elapsed)──▶ Cooldown ──(Y elapsed)──▶ Idle
```

| State | Reels allowed? |
|---|---|
| Idle | Yes. Entering Reels starts a session. |
| InSession | Yes, until X has elapsed. |
| Cooldown | No. Every Reels view is exited immediately. |

## 4. Requirements

| ID | Requirement | Status |
|---|---|---|
| R-01 | Detect when the user is on a Reels view in the Instagram app. | Proposed |
| R-02 | In Idle, entering a Reels view starts a session and switches to InSession. | Proposed |
| R-03 | In InSession, Reels stays usable until the session reaches X. Elapsed time is counted per D-01. | Proposed |
| R-04 | When the session reaches X, exit the Reels view and switch to Cooldown. | Proposed |
| R-05 | In Cooldown, any Reels view is exited within 1 second of appearing. A short message shows the time until Reels unlocks. | Proposed |
| R-06 | When Y has elapsed after a session ended, switch to Idle. | Proposed |
| R-07 | Warn the user 2 minutes before the session ends (toast). | Proposed (D-04 accepted) |
| R-08 | X and Y can be changed in the app. Allowed range: 1–180 min each. Defaults: X = 30, Y = 90. | Proposed |
| R-09 | Keep a local log of sessions (start, end, minutes in Reels). Show today's total Reels minutes in the app. | Proposed |
| R-10 | Session and cooldown state survives app kill and phone restart. The countdown uses wall-clock time. | Proposed |
| R-11 | The main screen shows the current state and remaining time (session time left, or cooldown time left). | Proposed |
| R-12 | On first launch, explain why accessibility access is needed and guide the user to enable it. If access is off, show a clear "not protecting" warning. | Proposed |
| R-13 | All data stays on the device. The app sends nothing off the device and needs no internet permission. | Proposed |
| R-14 | Changes to X or Y made during Cooldown take effect only after the cooldown ends (D-03). | Proposed (D-03 accepted) |
| R-15 | A DM reel is always allowed, in every state. It does not start a session. Swiping on from it to another reel is a normal Reels view: it starts a session in Idle and is exited in Cooldown (D-02). | Proposed (D-02 accepted) |

### Failure and edge behavior

| ID | Situation | Expected behavior |
|---|---|---|
| E-01 | Instagram updates its UI and Reels is no longer detected. | The app must not block non-Reels screens by mistake. The user notices that Reels is not being limited, and fixing detection is a manual code update. |
| E-02 | Accessibility service is disabled by the system or the user. | Show the R-12 warning when the app is opened. While disabled, Reels is not limited. |
| E-03 | The user leaves Reels mid-session, e.g. to DMs, and returns. | Handled per D-01. |
| E-04 | The device clock is changed manually. | Best effort only. Not protected in v1 (tamper-proofing is a non-goal). |
| E-05 | The phone restarts during Cooldown. | Cooldown continues with the correct remaining time (R-10). |
| E-06 | Instagram is closed during a session. | Same as leaving Reels; handled per D-01. |

## 5. Acceptance criteria

Test with X and Y set to short values (e.g. X = 2 min, Y = 3 min) on the owner's phone with the current Instagram release.

| ID | Given | When | Then |
|---|---|---|---|
| AC-01 (R-01, R-02) | Idle | Open the Reels tab | The app shows InSession with a countdown starting at X. |
| AC-02 (R-01) | Idle | Open a reel from the home feed, and separately from a profile | Each one starts a session. |
| AC-03 (protected) | Any state | Use the feed, Stories, DMs, profile, search | Nothing is exited or blocked. |
| AC-04 (R-04) | InSession | Keep watching until X | Reels is exited and the state shows Cooldown with Y remaining. |
| AC-05 (R-05) | Cooldown | Open Reels from the tab, the feed and a profile | Each is exited within 1 s, and a message shows the time until unlock. |
| AC-06 (R-06) | Cooldown | Wait for Y | The state is Idle, and opening Reels starts a new session. |
| AC-07 (R-07) | InSession | Watch until 2 minutes before X | A warning appears once. |
| AC-08 (R-08) | Idle | Set X = 10, Y = 20, then start a session | The session lasts 10 min and the cooldown lasts 20 min. |
| AC-09 (R-10) | Cooldown with 2 min left | Force-stop the app, or restart the phone | Cooldown is still active, with about 2 min left. |
| AC-10 (R-12) | Accessibility off | Open the app | The "not protecting" warning shows, with a button to the accessibility settings. |
| AC-11 (R-13) | Installed | Inspect the manifest | No INTERNET permission. |
| AC-12 (R-14) | Cooldown | Set Y to 1 min | The current cooldown keeps its original end time. |
| AC-13 (R-09) | After two sessions today | Open the app | Today's Reels minutes equal the sum of both sessions (±1 min). |
| AC-14 (R-15) | Cooldown | Open a reel a friend sent in a DM | It plays and is not exited. The state stays Cooldown. |
| AC-15 (R-15) | Idle | Open a DM reel | It plays and the state stays Idle (no session starts). |
| AC-16 (R-15) | Cooldown, watching a DM reel | Swipe to the next reel | The next reel is exited back toward the DM thread, and the lock toast shows. |

## 6. Outcome measure

- **Intended effect:** less daily Reels time than before the app.
- **Baseline:** unknown. Android Digital Wellbeing reports total Instagram time, not Reels alone. Use about a week of Instagram time from Digital Wellbeing as a rough baseline before installing.
- **Measurement:** Reels minutes per day from R-09, plus Instagram time from Digital Wellbeing, over the first 2 weeks.
- **Target:** none set. The owner decides after 2 weeks whether X and Y need adjusting.

Functional acceptance (section 5) only shows that the app works. It does not show that Reels time went down.

## 7. Decisions

| ID | Question | Decision (owner, 2026-10-08) |
|---|---|---|
| D-01 | How is session time counted? | **(a) Wall-clock from session start.** Leaving Reels does not pause the timer. |
| D-02 | Are reels opened from a DM limited? | **(b) No.** A DM reel is always allowed (R-15). Swiping on to further reels counts as a normal Reels view. |
| D-03 | Can settings be changed during Cooldown? | **(b)** Yes, but changes apply only after the cooldown ends (R-14). |
| D-04 | Warning before the session ends (R-07)? | **Yes**, 2 min before. |

## 8. Assumptions and dependencies

- **A-01: Confirmed by probe, 2026-10-08** (Pixel 7 Pro, Android 17, Instagram 450.0.0.50.77; 2 runs; logs in `docs/product/evidence/`). Rule: a Reels view is showing when the node `com.instagram.android:id/clips_viewer_view_pager` is **visible to the user**.
  - The rule held for the Reels tab, a feed reel, a profile reel and an Explore reel.
  - Home, DM threads, Stories and Explore never matched. Stories use different `reel_viewer_*` IDs.
  - Caveat: Instagram keeps the hidden Reels tab in the accessibility tree, so the node merely existing is a false positive. Visibility must be checked.
  - Caveat: switching tabs produced a ~40 ms frame showing both feed and Reels. Detection needs a short debounce (e.g. ~300 ms).
  - Must be re-checked after Instagram updates (E-01).
- **A-02: Confirmed for adb installs, 2026-10-08.** The probe was installed with `adb install` and accessibility was turned on with no "restricted settings" step. An APK installed from a file manager or browser may still need "Allow restricted settings".
- **A-03:** The phone's battery optimization does not kill the accessibility service. Some vendors (e.g. Xiaomi, Samsung) may need a battery exemption.
- **A-04: Confirmed by probe, 2026-10-08** (2 samples, same device). Rule: a DM reel is a Reels view where `sender_username_or_fullname` (also `sender_timestamp`, `reply_bar_edittext`) is visible.
  - After swiping to the next reel, these IDs disappeared in both runs, so R-15's swipe rule can be detected.
  - Caveat: only 2 samples. Unverified whether the sender bar can auto-hide during long viewing. Fallback if it does: allow all reels while a DM thread was the last screen (owner to decide).
- **Dependency:** Instagram UI changes can break R-01 at any time (E-01).
