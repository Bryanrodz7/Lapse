# Store screenshots — shot list and setup

Six phone screenshots, in listing order. Everything below is verified against this project's
debug build and the emulator, not copied from a template.

---

## Play's actual requirements

| Asset | Rule |
|---|---|
| **Phone screenshots** | **Minimum 2, maximum 8.** At least **4** are needed for the app to be eligible for Play's featured/promotional surfaces — shoot 6. |
| Format | PNG or JPEG |
| Size | Each side **320–3840 px** |
| Aspect ratio | **The longest side may not be more than 2× the shortest.** This is the one that bites — see below. |
| **Feature graphic** | **1024 × 500**, PNG or JPEG, **no alpha/transparency**. Required — the listing will not publish without it. |
| App icon | 512 × 512 PNG, 32-bit with alpha, ≤ 1 MB — already generated at `store/out/play-icon-512.png` |
| Tablet (7" & 10") | Optional, up to 8 each. Without them Play tags the listing "not designed for tablets" and drops it from tablet ranking. Skip for v1 if you want. |

### The aspect-ratio trap

The emulator's native resolution is **1080 × 2400**. Ratio 2.222 — `2400 > 2 × 1080`.
**Raw screenshots from this AVD get rejected by Play.**

I already resized the running emulator to **1080 × 1920** (ratio 1.778) and captured a test frame
to confirm it comes out at exactly that size. Nothing more to do — just don't reboot the emulator
before shooting, or re-run:

```powershell
adb shell wm size 1080x1920
adb shell wm density 420
```

To put it back afterwards: `adb shell wm size reset; adb shell wm density reset`

---

## One-time setup before shooting

### 1. Turn the ad banner off

The debug build shows a "Test Ad" banner on Home. Both Home screenshots would show it.

In `app/src/main/java/dev/randyapps/lapse/ads/AdsState.kt`, change the one line in
`DefaultAdsState`:

```kotlin
override val adsEnabled: Flow<Boolean> = flowOf(false)   // was flowOf(true) — REVERT AFTER
```

The banner *and* its reserved height both disappear (this is the same path the HomeViewModel
tests cover), so the list sits flush to the bottom. **Revert this before committing.**

### 2. Install, grant notifications, seed

Granting the permission up front stops the runtime dialog from appearing mid-shoot.

```powershell
.\gradlew installDebug
adb shell pm clear dev.randyapps.lapse
adb shell pm grant dev.randyapps.lapse android.permission.POST_NOTIFICATIONS
adb shell am start -n dev.randyapps.lapse/.MainActivity
```

`pm clear` wipes the seeded-once flag, so `DemoSeed` re-seeds on this launch. It creates 8 items
relative to *today*, covering every status:

| Item | Days | Shows as |
|---|---|---|
| Driver's License | 0 | **Expires today** |
| Vehicle Inspection | +4 | Urgent |
| Car Insurance | +23 | Soon |
| Passport | +61 | Later |
| Dentist Check-up | +88 | Later |
| First Aid Certificate | +240 | Later |
| Boiler Service | −12 | Expired |
| Gym Membership | −95 | Expired |

**Swipe-delete "Gym Membership" before shooting.** Two expired rows makes the lead screenshot
read as a neglected app; one keeps the quiet-expired treatment visible without that.

### 3. Clean status bar

Already applied to the running emulator (fixed 9:30 clock, full battery, full wifi, no
notification icons). It does not survive a reboot. To reapply:

```powershell
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0930
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
```

Exit demo mode after: `adb shell am broadcast -a com.android.systemui.demo -e command exit`

### 4. Capture command

```powershell
adb exec-out screencap -p > store\screenshots\01-home-light.png
```

---

## The six shots

Order matters — Play shows the first 2–3 in search results and most people never scroll.

### 1 — `01-home-light.png` · Home, populated · **Light**

The hero. The whole pitch is in this one frame: serif names, the days number as the hero,
status colour as the only saturation, expired sinking away.

- Seeded list, Gym Membership deleted (7 rows)
- Scrolled to the very top — wordmark and the settings gear visible
- FAB visible bottom-right
- No ad banner

### 2 — `02-add-quickpicks-light.png` · Add screen, quick picks · **Light**

Sells the speed. This is the "add something in four seconds" claim made visible.

- Tap the FAB
- **Do not type anything** — the quick-pick chips must be on screen, name field empty with its
  placeholder showing
- Keyboard: dismiss it (back gesture once) so the chips and the date row are both visible

### 3 — `03-home-dark.png` · Home, populated · **Dark**

Dark mode is a real reason people pick one utility over another, and the warm near-black is
some of the best-looking work in the app.

```powershell
adb shell cmd uimode night yes
```

(Works because the app's own theme setting defaults to *System*. Undo with `night no`.)

- Same list and scroll position as shot 1, so the pair reads as one app not two

### 4 — `04-edit-renewed-light.png` · Edit an item, renew action · **Light**

The differentiator. Every other reminder app makes you delete and re-add.

- Back to light mode first
- Tap **Car Insurance**
- The edit screen with the renewal period set and the **"Renewed — move to …"** action visible
- If the action sits below the fold, scroll so it's in frame — this is the point of the shot

### 5 — `05-settings-privacy-light.png` · Settings · **Light**

The positioning shot. Backs up the short description's privacy claim with the actual About copy.

- Settings gear → scroll so the default lead times *and* the About/privacy text are both visible
- If they don't fit in one frame, favour the About text

### 6 — `06-empty-state-light.png` · Empty state · **Light**

First-run charm, and it shows the quick picks a second time in context.

```powershell
adb shell pm clear dev.randyapps.lapse
adb shell pm grant dev.randyapps.lapse android.permission.POST_NOTIFICATIONS
```

Then launch and **immediately delete all 8 seeded rows**, or set the seeded flag first so nothing
seeds:

```powershell
adb shell "run-as dev.randyapps.lapse mkdir -p shared_prefs"
```

Simpler: just swipe-delete the 8 rows. The empty state appears with its copy and quick-pick chips,
and the FAB correctly hides.

---

## After shooting

1. Revert `DefaultAdsState` to `flowOf(true)`
2. `adb shell cmd uimode night no`
3. `adb shell am broadcast -a com.android.systemui.demo -e command exit`
4. `adb shell wm size reset; adb shell wm density reset`
5. Verify every file: `python -c "from PIL import Image;import glob;[print(f, Image.open(f).size) for f in glob.glob('store/screenshots/*.png')]"` — every one should read `(1080, 1920)`

## Still outstanding for the listing

- ~~Feature graphic 1024 × 500~~ — done: `store/feature-graphic.png`, regenerate with
  `python store/generate_feature_graphic.py`
- **Real AdMob app ID + unit ID** — still the test IDs (`AdIds.kt` and the manifest `meta-data`)
- ~~Privacy policy URL~~ — done: <https://bryanrodz7.github.io/Lapse/privacy-policy/>
- **Upload keystore** — you create it; command is in `store/play-listing.md`
