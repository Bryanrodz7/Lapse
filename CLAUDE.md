# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

**Lapse** is an Android app that tracks expiration and renewal dates (license, passport, registration, insurance, certs, warranties) and reminds you before they lapse. Local-only: no accounts, no cloud, no network beyond what AdMob requires.

The repo is currently the Android Studio **Empty Activity template** — `MainActivity` still renders the template `Greeting`, and `ui/theme/` still holds the default purple Material palette. None of the app described below has been written yet. Do not assume any of it exists; check before referencing it.

## Commands

Run from the project root. The Bash tool gets `./gradlew`; from PowerShell use `.\gradlew.bat`.

```bash
./gradlew :app:assembleDebug          # build debug APK
./gradlew :app:installDebug           # build + install to connected device
./gradlew :app:testDebugUnitTest      # JVM unit tests (app/src/test)
./gradlew :app:connectedDebugAndroidTest  # instrumented tests (app/src/androidTest), needs a device
./gradlew :app:lintDebug              # Android Lint
```

Single tests:

```bash
# one JVM test class or method
./gradlew :app:testDebugUnitTest --tests "dev.randyapps.lapse.ExampleUnitTest.addition_isCorrect"

# one instrumented class
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.randyapps.lapse.ExampleInstrumentedTest
```

The SDK path lives in `local.properties` (git-ignored, machine-specific). This directory is **not** a git repository.

## Toolchain gotchas

The build uses very new tooling — AGP **9.2.1**, Gradle **9.4.1**, Kotlin **2.2.10**, Compose BOM **2026.02.01**, JDK 21 toolchain. Several things differ from what most Android docs and older AGP examples show:

- **No `kotlin-android` plugin, by design.** AGP 9 applies Kotlin itself. `app/build.gradle.kts` declares only `com.android.application` and `kotlin.plugin.compose`. Do not add `org.jetbrains.kotlin.android` — it is unnecessary and will conflict.
- **`compileSdk` must be 37.** Compose BOM 2026.02.01 and `lifecycle-runtime-compose:2.11.0` both refuse to be consumed by a project compiling against anything lower; the build fails at `checkDebugAarMetadata`. It uses the new block DSL, not the old integer property:
  ```kotlin
  compileSdk { version = release(37) }
  ```
  AGP auto-downloads the platform (`android-37` is not in the local SDK dir, only `build-tools/37.0.0`).
- **R8 config lives in `app/src/main/keepRules/`**, not `proguard-rules.pro`. AGP 9 merges every keep-rule file in that directory. The release build currently sets `optimization { enable = false }`.
- **Configuration cache is on** (`org.gradle.configuration-cache=true` in `gradle.properties`). Build logic that reads mutable state at execution time will fail the cache — keep `build.gradle.kts` free of `Date.now()`-style or environment-dependent reads.
- **`minSdk` is 24, so `java.time` is not available without help.** The data layer models dates with `LocalDate`/`Instant`, so core library desugaring is enabled (`isCoreLibraryDesugaringEnabled = true` + `coreLibraryDesugaring(...)`, `compileOptions` at Java 17). The `l8DexDesugarLibDebug` task in build output confirms it is active.
- **Use KSP 2.3.x or newer — do not downgrade to the `<kotlin>-<ksp>` versioning scheme.** KSP versions like `2.2.10-2.0.2` register generated sources through the `kotlin.sourceSets` DSL, which built-in Kotlin rejects outright: *"Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin."* KSP 2.3.x moved to independent versioning and uses AGP's variant API instead, so it just works. **KSP `2.3.11` + Room `2.8.4` are verified building and generating on this exact toolchain, with no opt-out flags of any kind.**
  - Do **not** "fix" this with `android.disallowKotlinSourceSets=false`. That property is undocumented, suppresses an AGP correctness check rather than fixing the cause, and is unnecessary at KSP 2.3.x.
  - The documented alternative — `android.builtInKotlin=false` plus `org.jetbrains.kotlin.android` — also requires `android.newDsl=false`, which would break this project's `compileSdk { }` and `optimization { }` blocks. Don't go down that path without a specific reason.
- **Hilt 2.60.1 works on AGP 9.2.1 with no workarounds.** `hiltCollectClassesDebug`, `hiltAggregateDepsDebug`, and the ASM class transform all run normally. Its annotation processor goes through the same KSP that Room uses.
- **`SwipeToDismissBoxState` is saved against the LazyColumn item key.** An item removed by a swipe and then restored under the same key comes back *still dismissed* — in the tree, rendered off-screen, invisible. `HomeScreen` snaps the state back to `Settled` in a `LaunchedEffect(item.id)`. ViewModel tests cannot see this class of bug; `HomeScreenTest` covers it.
- **`animateFloatAsState` starts at its target on first composition.** Animating straight to `1f` produces no animation at all. Flip a `remember`ed flag in a `LaunchedEffect` so the value has somewhere to travel from (see `StaggeredEntry`).
- **WorkManager's auto-initializer is removed in the manifest** so `LapseApp` can supply the Hilt worker factory. Two consequences that both fail at runtime, not compile time:
  - **Anything injected into `LapseApp` must not resolve a `WorkManager` during injection.** `WorkManager.getInstance()` calls back into `LapseApp.workManagerConfiguration`, which reads the not-yet-injected `workerFactory` and crashes on launch with `UninitializedPropertyAccessException`. `WorkManagerReminderScheduler` takes a `Provider<WorkManager>` to defer the lookup. Don't "simplify" that to a direct `WorkManager`.
  - **Instrumented tests must call `WorkManagerTestInitHelper.initializeTestWorkManager` themselves.** `HiltTestApplication` is not a `Configuration.Provider`, so resolving the graph otherwise throws *"WorkManager is not initialized properly"*.
- **Instrumented tests pass while the app is dead.** The launch crash above did not fail a single test, because tests run against `HiltTestApplication` and initialize WorkManager explicitly. After any change to `LapseApp`, DI, or the manifest, install and launch the app and check `pidof` plus logcat for `FATAL EXCEPTION`. `connectedAndroidTest` also *uninstalls* the APK when it finishes, so reinstall before manual checks.
- **Instrumented tests run under a custom `HiltTestRunner`**, set as `testInstrumentationRunner` in `defaultConfig`. `@EntryPoint` interfaces declared in `androidTest` are *not* installed into the app's real component — using `EntryPointAccessors` against the app graph fails at runtime with `ClassCastException: Cannot cast ...SingletonCImpl to <your entry point>`. Use `@HiltAndroidTest` + `HiltAndroidRule` + `@Inject` instead.

## Intended architecture

Written down so a future instance resuming mid-build knows the target. **None of this is implemented yet.**

- **MVVM.** ViewModel + `StateFlow`; screens are stateless composables taking state and lambdas. Repository sits between Room DAO and ViewModel.
- **One Room entity**, `ItemEntity`: name, `Category` enum, `expiryDate`, `reminderDaysBefore: List<Int>`, optional note, optional internal-storage photo path, `createdAt`. DAO exposes `Flow<List<ItemEntity>>`.
- **`daysRemaining` and status (ACTIVE / SOON / URGENT / EXPIRED) are derived at read time, never stored** — so they cannot go stale between app launches. SOON is 30 days out, URGENT is 7.
- **Three screens only** — Home (list grouped into "This month" / "Next 3 months" / "Later" / "Expired"), Add/Edit (one form, both modes), Settings. Navigation Compose, no bottom nav. A fourth screen is out of scope.
- **WorkManager** schedules one worker per reminder offset per item; reschedule on create/edit/delete and on `BOOT_COMPLETED`, cancelling orphans. `POST_NOTIFICATIONS` is requested when the user saves their **first** item, not at launch.
- **Ads: adaptive anchored banner on Home only.** No interstitial, rewarded, or app-open units — do not create them. Banner height is reserved so it never shifts layout, and its visibility is gated on a single boolean so a future one-time "remove ads" purchase can hide it. Use Google's test ad unit IDs; the real ID goes in a constant with a TODO.

### Design constraints that are requirements, not suggestions

- Serif (Instrument Serif / Fraunces / Newsreader, bundled locally in `res/font` — not downloadable fonts) for item names and the days-remaining number; sans for body and labels. That mix is the app's whole personality.
- Warm off-white background in light mode, warm near-black in dark — never pure white or black, never blue-gray. Surfaces barely differ from background; separation comes from spacing, not cards, borders, or elevation.
- Status colors are the **only** saturation in the app, and they are muted and earthy.
- Dark mode is hand-tuned, not inverted.
- The days number is the hero; the calendar date is secondary.
- Deleting is swipe + undo snackbar, never a confirmation dialog. All animation under 300ms.
- No analytics, no crash reporting, no third-party trackers. Photos in app-internal storage only.
- Full TalkBack content descriptions on every interactive element, and layouts must survive 200% font scaling.
- No `com.example` anywhere; the package is `dev.randyapps.lapse`.
