# Sonnet — Code Audit Report

**Scope:** `app/src/main/java/com/example` (8,673 LOC across 63 Kotlin files), `app/src/test`, `app/src/androidTest`, `app/build.gradle.kts`, `AndroidManifest.xml`, resources.
**Method:** Static review of every source file. No compilation, no execution, no device testing.
**Date:** 2026-09-29 · Commit `76d64f9` (main)

> **Caveat that bounds every finding below.** The Android SDK is not installed on the review machine, so nothing was built or run. Every claim here is derived from reading source. Line numbers are accurate as of `76d64f9`; findings marked **[verified]** were confirmed against the file by direct inspection. Runtime-only failures (actual crashes, measured frame rates, real query timings) are inferred from code and should be confirmed once the app can run.

---

## Executive summary

Sonnet is a competent Android music player — off-device local library, Media3/ExoPlayer playback, Room persistence, Compose UI, DataStore preferences. The **architectural bones are good**: clean domain/data/presentation separation, no blocking IO on the main thread, no destructive Room migration, correct permission model, and honest secrets handling.

The problems are concentrated and severe in three places:

1. **Playback logic is wrong in specific, user-visible ways.** Shuffle doesn't shuffle in the path that matters. "No repeat" is unreachable. The "crossfade" is a fade-to-silence, not a crossfade. Play counts are recorded on load, not on play.
2. **Two UI defects make features unusable or ruin performance.** The equalizer sheet dismisses when you tap non-interactive areas. The Home screen recomposes at ~60fps forever, forever, whether or not a skeleton is on screen.
3. **Nothing is tested.** 142 lines of tests against 8,673 lines of source — and two of the four test files are empty stub bodies that pass vacuously. One instrumented test is guaranteed to fail. The project has no CI, so nothing would catch a regression anyway.

There is also a **release-blocking build issue**: the release variant cannot be signed, and it ships unminified.

### Findings by severity

| Severity | Count | Theme |
|---|---|---|
| Critical | 3 | Feature-breaking correctness / permanent perf cost |
| High | 12 | Data integrity, unusable features, security, build blocked |
| Medium | 24 | Degradation, maintainability, correctness under edge conditions |
| Low | 22 | Dead code, inconsistencies, polish |

---

## Critical

### C1. Shuffle is not actually shuffling in the end-of-track path **[verified]**

`player/QueueManager.kt:84-90` vs `:118-125`

```kotlin
// peekNextTrack  (:84-90) — "preview" the next track
if (_shuffleModeEnabled.value) {
    if (list.size > 1) {
        val otherIndices = list.indices.filter { it != currentIdx }
        val nextIdx = otherIndices.firstOrNull() ?: currentIdx   // <-- always the FIRST
        return list[nextIdx]
    }

// getNextTrack (:118-125) — actually play it
if (_shuffleModeEnabled.value) {
    if (list.size > 1) {
        val otherIndices = list.indices.filter { it != currentIdx }
        val nextIdx = otherIndices.random()                      // <-- random
        currentTrackIndex = nextIdx
        return list[nextIdx]
    }
```

`peekNextTrack` is called from the progress-tracking loop (`AudioPlayer.kt:429`) to decide when to start a crossfade transition. It returns `otherIndices.first()`, which for any playlist is simply **the second track in the list**. So when shuffle is on, tracks auto-advance into the second item of the queue over and over instead of shuffling.

Worse, the two functions disagree by design: `peekNextTrack` previews track A, then `getNextTrack` (`AudioPlayer.kt:431`) rolls a *different* random track B, and the crossfade callback then loads B. The preview is always wrong.

**Fix:** Have `peekNextTrack` and `getNextTrack` share one selection function, and have the transition path *commit* the track it peeked rather than re-rolling. Also note shuffle has no history stack at all — `getPreviousTrack` (`:139-161`) completely ignores `_shuffleModeEnabled`, so "previous" after a shuffle is semantically meaningless.

### C2. The equalizer sheet closes when you tap anywhere non-interactive **[verified]**

`presentation/component/EqualizerBottomSheet.kt:78-79` and `:98`

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = scrimAlpha))
        .clickable(onClick = { dismissWithAnimation() }),   // :79  scrim — live handler
    contentAlignment = Alignment.BottomCenter
) {
    ...
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clickable(enabled = false) {}, // :98 "consume clicks inside sheet"
```

The comment states the intent, but `Modifier.clickable(enabled = false)` does not implement it. In Compose, `clickable` installs a `pointerInput` whose body begins with `if (!enabled) return` — a disabled clickable **completes immediately and never consumes**. The `Surface` is not scrollable or draggable, so it has no other hit path. Taps on the "PRESETS" label (`:185`), the "FREQUENCY BANDS (dB)" label (`:330`), the header (`:139-148`), and any blank gap between sliders fall straight through to the scrim and **dismiss the sheet**.

Only genuinely interactive children (`Switch`, `IconButton`, preset pills, `Slider`s) intercept the tap. The feature is effectively unusable.

**Fix:** `.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }` (enabled, no-op body) — that actually consumes. Or use `Modifier.pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent().changes.forEach { it.consume() } } } }`.

### C3. Home screen recomposes at ~60fps forever **[verified]**

`presentation/component/Skeleton.kt:20-35`, used at `presentation/screen/home/HomeScreen.kt:63`

```kotlin
// Skeleton.kt:20-35 — no `enabled` gate
@Composable
fun ProvideShimmer(content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "sharedPulse")
    val alpha by infiniteTransition.animateFloat(0.3f, 0.75f, infiniteRepeatable(...), ...)
    CompositionLocalProvider(LocalShimmerAlpha provides alpha) { content() }
}

// HomeScreen.kt:63-69 — wraps the entire LazyColumn, unconditionally
com.example.presentation.component.ProvideShimmer {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
        contentPadding = PaddingValues(top = 28.dp, bottom = 100.dp)
    ) { ... }
}
```

`alpha` is an infinite animation state that changes every frame. `CompositionLocalProvider` re-executes its `content` block when a provided value changes — so the `LazyColumn` and its entire content re-run at animation framerate, **permanently, even after the library has fully loaded and no skeleton is visible**.

It cannot be skipped, because both of `LazyColumn`'s parameters are freshly allocated on each re-execution: the `Modifier` chain at `:65-67` (3 new objects) and `PaddingValues` at `:69`. Downstream this re-runs, every frame:
- `HomeScreen.kt:145` — `listOf(listOf(1,2), listOf(3,4), listOf(5,6))` reallocated
- `HomeScreen.kt:159` — `recentTracks.take(6).chunked(2)` recomputed
- `HomeScreen.kt:160` — per-item String key `"rec_${...}_${...}"` rebuilt for every visible row

**Fix:** only provide shimmer while skeletons are on screen — `if (playlists.isEmpty() || recentTracks.isEmpty() || mostPlayedTracks.isEmpty()) ProvideShimmer { ... }` — and `remember` the `Modifier` chain, `PaddingValues`, and chunked lists.

---

## High

### H1. "No repeat" is unreachable — the mode silently coerces to "Repeat All"

`player/QueueManager.kt:43-58` **[verified]**

```kotlin
fun setRepeatMode(mode: Int) {
    _repeatMode.value = if (mode == Player.REPEAT_MODE_ONE) {
        Player.REPEAT_MODE_ONE
    } else {
        Player.REPEAT_MODE_ALL        // REPEAT_MODE_OFF (0) becomes ALL
    }
}

fun toggleRepeat(): Int {
    val next = if (_repeatMode.value == Player.REPEAT_MODE_ALL) REPEAT_MODE_ONE else REPEAT_MODE_ALL
    ...
}
```

The cycle is `ALL ↔ ONE` only. `REPEAT_MODE_OFF` can never be set, and a stored value of `OFF` is silently upgraded to `ALL` (`UserPreferencesDataStore.kt:59-66, 115-122` applies the same coercion on read). A user who wants a queue to stop at the end simply cannot express it.

### H2. Play history is recorded on *load*, not on play — inflating "Most Played"

`player/AudioPlayer.kt:318` (inside `loadAndStart`, 296-325) **[verified]**

```kotlin
_currentTrack.value = track
_currentPosition.value = 0L
onTrackPlayed?.invoke(track.id)     // <-- fired immediately, unconditionally
```

`onTrackPlayed` is wired to `recordPlayUseCase` in `di/AppContainer.kt:51-53`. Three problems:

- It fires **after `player.prepare()` but before `STATE_READY`** and before any audio is produced. A track that fails to load is still counted (the call sits *after* the `try/catch` at `:302-314`).
- The crossfade path (`AudioPlayer.kt:432-434`, driven by the 100ms poll loop) records the play **up to 12 seconds before the track is actually audible**.
- Tapping through a queue at speed records every track as played, making "Most Played" meaningless.

`getMostPlayedTracks` / `getRecentTracks` (`data/db/dao/TrackDao.kt:21,27`) are the home-screen queries built on this.

**Fix:** record on `Player.STATE_READY` plus a minimum-playtime threshold, or on the *completion* of a track — not on load.

### H3. The "crossfade" is a fade to silence, then a cold reload

`player/FadeController.kt:119-172` **[verified]**

```
Phase 1: fade out  1.0 → 0.0   (half duration)
Phase 2: onSwitch()            <-- player.stop(); clearMediaItems(); setMediaItem(); prepare()  ← full reload, seekTo(0)
Phase 3: fade in   0.0 → 1.0   (half duration)
```

There is a hard gap: the old track reaches silence, the player is torn down and rebuilt, then the new track fades up. Real crossfade requires the outgoing and incoming tracks to overlap, which needs two simultaneous players or ExoPlayer's `setCrossfadeEnabled()`.

Compounding it, `loadAndStart` (`AudioPlayer.kt:302-311`) calls `player.stop()` then `clearMediaItems()` on **every** track change — destroying ExoPlayer's preloading and guaranteeing a seek-and-buffer stall on every song. This is the primary cause of audible gaps between songs.

**Fix:** use ExoPlayer's native crossfade (`ExoPlayer.Builder.setCrossfadeEnabled(true)`), or drive two ExoPlayer instances. At minimum, use `setMediaItems(list, index, 0)` without clearing the queue.

### H4. A DB write failure crashes the app during playback

`di/AppContainer.kt:51-53` **[verified]**

```kotlin
audioPlayer.onTrackPlayed = { trackId ->
    GlobalScope.launch(Dispatchers.IO) { recordPlayUseCase(trackId) }
}
```

`GlobalScope` has no parent job and no owner. If the insert throws (disk full, DB closed/corrupted), the exception is unhandled and propagates to the default uncaught-exception handler — which is this app's own handler (`SonnetApplication.kt:19-28`). A disk-space hiccup becomes a process crash mid-song. The job also can never be cancelled or awaited.

**Fix:** a `CoroutineScope(SupervisorJob() + Dispatchers.IO)` owned by `AppContainer`, with a `try/catch` inside.

### H5. Deleted files stay in the library forever; playlists keep referencing them

`data/repository/TrackRepositoryImpl.kt:49-69` **[verified]**

`scanDeviceForTracks()` only ever calls `insertTracks`. It never deletes. A track deleted from the device, an uninstalled SD card, or a MediaStore re-index leaves its row in `tracks` **and** its `playlist_track` join row intact. The user sees unplayable entries; playing one throws `PlaybackException` (`AudioPlayer.kt:224-229`) and is only logged.

**Fix:** diff the scanned ID set against the DB and delete the difference inside the existing `Mutex`, cascading the join rows in the same transaction.

### H6. No Room migration path — the next schema bump bricks every install

`data/db/AppDatabase.kt:20-21`, `di/AppContainer.kt:22` **[verified]**

`version = 1`, `exportSchema = false`, no `addMigrations(...)`, no `fallbackToDestructiveMigration(...)`, no `room.schemaLocation` KSP argument, and no `app/schemas/` directory.

To be clear and fair: **the absence of `fallbackToDestructiveMigration` is the right call** — it is the most common Room data-loss footgun, and this app correctly refuses to silently wipe playlists and play history. But there is also no migration infrastructure. The first `version = 2` bump throws `IllegalStateException: A migration from 1 to 2 was required but not found` on every existing install, with no recovery path. And because no schema JSONs are exported, `MigrationTestHelper` tests are impossible to write — a schema that cannot be diffed cannot be safely migrated later.

**Fix:** set `exportSchema = true`, configure the KSP `room.schemaLocation` arg, commit `app/schemas/`, and write every migration as you add it.

### H7. Every foreground return triggers a full MediaStore rescan

`MainActivity.kt:369-372` (and `:107`) **[verified]**

```kotlin
override fun onResume() {
    super.onResume()
    triggerMediaScan()      // full cursor query over the entire media store
}
```

`onResume` fires on every foreground return: dismissing the permission dialog, returning from Settings, switching apps, pulling down the notification shade. There is no debounce and no staleness check. `onCreate:107` also calls `triggerMediaScan()`, and `onResume` fires immediately after `onCreate` — so **every cold start issues two full scans back-to-back**. `TrackRepositoryImpl.kt:50` uses `tryLock` so concurrent duplicates are dropped, but sequential resumes each pay full cost.

**Fix:** debounce with a minimum interval and a `lastScanAt` guard; drop the redundant `onCreate` call.

### H8. `MediaSessionService` hands control to any app, and widens the granted commands

`AndroidManifest.xml:27`, `player/PlaybackService.kt:153-155, 166-185`

`exported="true"` is *required* by Media3 for external controllers, so that part is correct. The defect is that `onGetSession` returns the session unconditionally, and `CustomCallback.onConnect` explicitly **adds** `COMMAND_SEEK_TO_PREVIOUS/NEXT` and the custom repeat/shuffle commands **for every controller**, with no `controller.packageName` check.

Any installed app can bind, seize playback, spam the notification, and call `onCustomCommand` (`:187-211`) to mutate persisted playback preferences.

**Fix:** gate `onGetSession` on an allowlist of system/Assistant/Auto packages, or add a `signature`-level `android:permission`.

### H9. Release build cannot be signed

`app/build.gradle.kts:28-32, 47`

`storeFile` falls back to `${rootDir}/my-upload-key.jks`, which does not exist. `STORE_PASSWORD`/`KEY_PASSWORD` read from `System.getenv` with no `require(...)`, so a missing env var yields `null` and fails late with an opaque error. There is no CI config to inject them and no unsigned fallback. `assembleRelease` fails at `validateSigningRelease`.

### H10. Release ships unminified and unobfuscated

`app/build.gradle.kts:45` — `isMinifyEnabled = false`, and `app/proguard-rules.pro` is 100% comments (zero rules). This is the largest available APK-size win, currently left entirely on the table.

### H11. Test coverage is effectively zero, and one test is guaranteed to fail

142 test LOC vs 8,673 source LOC = **1.6% by volume**. `app/build/` does not exist — the module has never been built, so **no test in this repo has ever been compiled or executed**.

- `androidTest/.../db/TrackDaoTest.kt:33-36` — `insertAndGetTrack()` is an empty body marked `// Stub`. No assertions; passes vacuously.
- `androidTest/.../ui/LibraryScreenTest.kt:15-18` — `libraryScreen_showsTracks()` is an empty body. `composeTestRule` is declared and spun up but never used.
- `androidTest/.../ExampleInstrumentedTest.kt:20` — asserts `appContext.packageName == "com.example"`, but `applicationId = "com.aistudio.sonnet.xwqrty"` (`app/build.gradle.kts:17`). **This test fails by construction** — stale template asserting `namespace` instead of `applicationId`.

The two real unit tests in `ExampleUnitTest.kt:28-45` and `:48-63` are genuine and well-targeted (peek-does-not-advance, fade-cancel-restores-volume) — the problem is purely quantity. Notably `advanceTimeBy` is imported at `:14` and never used: a coroutine-test rewrite was started and abandoned. This is the test that would have caught C1.

Zero coverage on: `QueueManager` shuffle/repeat, `FadeController` transitions, `MediaStoreScanner` cursor parsing, the three aggregate `TrackDao` queries, play-history recording, and all 7 ViewModels. `mockk` is a declared dependency with no usage.

There is **no CI** — no `.github/`, no YAML. Nothing runs the tests even if they were correct.

### H12. No `Modifier.semantics` anywhere in the app

Confirmed by repo-wide grep: zero occurrences of `Modifier.semantics` or `testTag` in `app/src/main/java/`.

- `AppBottomNavigationBar.kt:181` sets `contentDescription = label` on the `Icon` while `:191-198` renders the same `label` as a `Text` — TalkBack announces **"Home, Home"** for every tab. The wrapping `Column` (`:158-199`) is a bare `.clickable` with no `Role.Tab` and no `selected` semantics, so a screen-reader user cannot tell which tab is active.
- `AlbumArt.kt:58` sets `contentDescription = "Album Art"` on an image used in **19 places** including every list row. A 500-track list announces "Album Art" 500 times, duplicating what the adjacent text already says. The art is decorative; `null` is correct.
- Conversely, `HomeScreen.kt` has **zero** `contentDescription` occurrences — the equalizer `IconButton` at `:102-105` is unlabelled, as is the drag pill at `NowPlayingScreen.kt:120-128` (which is also only **4dp tall**, far below the 48dp minimum touch target).
- `EqualizerBottomSheet.kt:390-407` — band sliders are horizontal `Slider`s rotated 270° via `graphicsLayer`. That is a draw-time transform and does not update semantics, so TalkBack announces an unlabelled "Slider" with no indication of which frequency band it is.
- No `heading()` semantics on any section title, so screen-reader users cannot jump between sections.

### H13. `AudioPlayer.release()` is dead code guarding native handles

`player/AudioPlayer.kt:488-499` — zero call sites (verified repo-wide). `PlaybackService.onDestroy` (`:157-163`) releases only the `MediaSession`. The ExoPlayer instance, `LoudnessEnhancer`, and the `Equalizer`/`BassBoost`/`Virtualizer` native `AudioEffect` handles are never released. Process-scoped so not a hard leak, but the release path guarding native resources does not run.

---

## Medium

### Playback

- **`skipToNext` can lose a queue position.** `AudioPlayer.kt:429-434` — `peekNextTrack` is used to pick the track, then `getNextTrack(...)` is called at `:431` to advance the index, and the crossfade callback then loads the *peeked* track. The `getNextTrack` return value is discarded, and index state is advanced twice by two different functions.
- **`getPreviousTrack` ignores shuffle entirely** (`QueueManager.kt:139-161`) and falls through to `return list.firstOrNull()` at `:160` without updating `currentTrackIndex`. There is no previously-played history stack.
- **Progress tracker polls every 100ms on `Dispatchers.Main.immediate`** (`AudioPlayer.kt:405-443`), writing `_currentPosition` 10×/sec. This drives a full recomposition of the Now Playing screen 10×/sec, formatting two `String`s with `String.format` on each tick (`NowPlayingScreen.kt:494-495`) — including the duration string, which is a pure function of `duration` and is needlessly reformatted 10 times a second.
- **`AlwaysNavigablePlayer.hasNextMediaItem()` always returns `true`** (`AlwaysNavigablePlayer.kt:32-34`) and `isCommandAvailable` always returns `true` for seek-prev/next (`:22-29`). This lies to the system and to Bluetooth/Assistant controllers, so those UIs will always show an enabled next button even at the end of the queue.
- **`setGaplessPlayback` is a no-op stub** (`AudioPlayer.kt:257-259`) but is exposed to the user as a setting.
- **`onAudioSessionIdChanged` leaks the previous effect.** `AudioPlayer.kt:233-243` releases before creating, which is correct, but `EqualizerManager.bindAudioSession` is called on every session change and its own release path is only reached via the dead `AudioPlayer.release()`.
- **Loudness enhancer gain is a magic number** — `setTargetGain(800)` hardcoded at `AudioPlayer.kt:237, 249`, undocumented.

### Data & persistence

- **No foreign keys anywhere.** `PlaylistTrackCrossRef` (`:5-12`) and `PlayHistoryEntity` (`:6-11`) declare no `@ForeignKey`, so `addTrackToPlaylist(999, 1)` silently inserts an orphan. Related: `TrackDao.insertTracks` uses `OnConflictStrategy.REPLACE` (`TrackDao.kt:18`), which is DELETE+INSERT — that becomes destructive the moment FKs land. Switch to `@Upsert` first.
- **`play_history` is unbounded and unindexed.** `PlayHistoryEntity` declares no `indices`, so there is no index on `trackId` or `playedAt`, while three hot home-screen queries do `GROUP BY`/`ORDER BY COUNT()`/`MAX()` over it. Nothing prunes old rows.
- **N+1 query to build playlist thumbnails.** `PlaylistDao.kt:19-21` uses `@Relation` (correctly `@Transaction`-wrapped), but Room issues one extra query per playlist per emission, loading full 7-column `TrackEntity` rows for *every* track in *every* playlist — when `PlaylistRepositoryImpl.kt:22` only needs `albumArtUri.take(4)`. 20 playlists × 200 tracks ≈ 21 queries / 4,000 rows to render ~20 list items.
- **Scanner only sees primary storage.** `MediaStoreScanner.kt:14` uses `EXTERNAL_CONTENT_URI`; secondary/removable volumes need `getContentUri(MediaStore.VOLUME_EXTERNAL)` since API 29. SD card music is invisible.
- **Scanner swallows all exceptions** (`MediaStoreScanner.kt:77-79`), so a `SecurityException` from a revoked permission is indistinguishable from "no music found".
- **Deprecated, unused `DATA` column** (`MediaStoreScanner.kt:22, 40, 49`) — deprecated since API 29, redacted on API 30+, read into a `data` variable and never used.
- **Non-deterministic sort order defeats `distinctUntilChanged`.** `MediaStoreScanner.kt:32` sorts by `TITLE ASC` with no tiebreaker, so equal-titled tracks return in arbitrary order — meaning the `.distinctUntilChanged()` calls in `TrackRepositoryImpl.kt:22,28,34,40` never suppress an emission.
- **Whole library materialized twice, inserted in one unbounded transaction** (`MediaStoreScanner.kt:12-80` + `TrackRepositoryImpl.kt:53-64`) — two full `List<Track>`-sized allocations held simultaneously.
- **`playlists` backup is device-specific and breaks on restore.** `AndroidManifest.xml:15-17` sets `allowBackup="true"` while `backup_rules.xml` and `data_extraction_rules.xml` are entirely commented-out templates. Restoring `sonnet_db` to another device restores `playlist_track` rows pointing at `content://media/...` URIs minted on the old device — permanently broken playlists with no self-healing. `last_crash.txt` with full stack traces is backed up too.
- **Case-sensitive grouping vs case-insensitive filtering.** `LibraryViewModel.kt:44,59` groups with `groupBy { it.album }` (case-sensitive); `AlbumDetailViewModel.kt:22` and `ArtistDetailViewModel.kt:23` filter with `ignoreCase = true`. MediaStore tag casing is not consistent, so one album can appear as two entries in the library while its detail screen shows the union of both.
- **`DataStore` delegate keyed per-Context.** `UserPreferencesDataStore.kt:18` is a Context-receiver property delegate that caches one instance per Context. Constructing the class with two different Contexts yields two DataStores over `user_prefs` and the second throws `IllegalStateException: There are multiple DataStores active for the same file`. Currently safe (only the Application context is passed), but the class also holds the Context strongly rather than `applicationContext`.
- **Redundant scan triggers, silently dropped.** Three call sites — `MainActivity.kt:369`, `LibraryScreen.kt:97-103`, `SettingsScreen.kt:432-446` — but `tryLock` silently discards a scan requested while another is in flight, and `SettingsScreen.kt:440` then reports "Library scan complete" for a scan that never ran.

### UI

- **A DataStore write fires on every frame of the crossfade-duration drag.** `SettingsScreen.kt:334-340` calls `audioPlayer.setCrossfadeDurationSeconds()` from `Slider.onValueChange` — up to 60 disk commits/second. The codebase already has the correct pattern: `EqualizerManager.persistSettings` (`:277-301`) debounces with `cancel()` + `delay(300)`. The crossfade path has no equivalent.
- **The same slider snaps to integers mid-drag.** `SettingsScreen.kt:240-242` keys the local `remember` on the DataStore value it is derived from, so each round-trip re-initialises the in-progress drag. It also lives inside a `LazyColumn` `item { }`, so the edit is discarded when the card scrolls out of view.
- **Now Playing has no scroll container.** `NowPlayingScreen.kt:102-430` stacks fixed-height children with no `verticalScroll`. On a short window or at font scale > 1.0 the weighted spacers collapse and the play/pause and skip controls become **unreachable**.
- **Process death leaves the app in a dead-end state.** `MainActivity.kt:119,125,127,346` — `rememberSaveable` restores `isNowPlayingOpen = true`, but the player queue is never persisted (the `LAST_PLAYED_TRACK_ID` key at `UserPreferencesDataStore.kt:24` has no writer). So the overlay never appears, `showBottomBar` stays `false` (no mini player, **no bottom navigation**), and `BackHandler` is enabled and swallows the back gesture. The user has no way out.
- **NowPlayingViewModel is Activity-scoped and never cleared.** It is constructed outside a `NavHost` destination (`MainActivity.kt:345-363`), so `LocalViewModelStoreOwner` resolves to the Activity. `onCleared()` never runs.
- **Drag-to-dismiss uses per-frame delta, not accumulated distance.** `NowPlayingScreen.kt:131-141` — a fast flick dismisses on one frame; a slow deliberate 300px swipe never dismisses. The inverse of correct. `pointerInput(Unit)` also captures a stale lambda.
- **Add-to-playlist re-queries the DB on every recomposition and shows a success toast for a no-op.** `AddToPlaylistDialog.kt:29` builds a cold Room flow in composition and uses `collectAsState` — the only non-lifecycle collector in 31 sites. `:66-72` shows "Added…" even when `OnConflictStrategy.IGNORE` made it a silent no-op.
- **`AddToPlaylistDialog` aborts its DB write if dismissed mid-flight** (`:27, 66-72`) because `rememberCoroutineScope` is bound to the composition.
- **~90 hardcoded strings, zero `stringResource` calls.** `strings.xml` has 3 entries. Blocks i18n entirely. Plurals are hand-inlined ~10 times with inconsistent logic (`LibraryScreen.kt:383,425,493,550`, `SearchScreen.kt:486,526`, `ArtistDetailScreen.kt:131`). `SettingsScreen.kt:543` hardcodes `"Version 1.0.0"` while `build.gradle.kts:18` declares `versionName = "1.0"` — the About screen is already wrong, and `BuildConfig` is never imported anywhere in 8,673 lines.
- **Un-`remember`ed allocations in the draw phase.** `NowPlayingScreen.kt:107` allocates a `Brush` inside `drawBehind` (every frame); `:231-233` allocates another inside a continuous 2600ms pulse (~60/sec); `EqualizerIcon.kt:33,38` allocates two lists per `Canvas` redraw.
- **`SearchScreen` subscribes to the full track list and never reads it.** `SearchScreen.kt:69` — `allTracks` has exactly one occurrence in the file (the declaration). Because it is read in composable scope, any change to the whole track table recomposes the screen, including the `TextField` holding the user's in-progress query.
- **Hardcoded `100.dp` bottom padding in 8 screens**, decoupled from the real overlay height (64dp card + 12dp + 68dp nav + system inset) — the last list item is partially hidden on some devices and gapped on others. The overlay is drawn outside every `Scaffold`, so `innerPadding` cannot be used.
- **No drag-to-dismiss on the equalizer sheet**, though it draws a grabber bar (`EqualizerBottomSheet.kt:108-115`) that implies one. `dismissWithAnimation()` (`:57-63`) has no re-entrancy guard, so two fast scrim taps launch two competing coroutines.
- **Sub-minimum touch targets:** `LibraryScreen.kt:136,140,143` (36dp), `SettingsScreen.kt:272,308` (32dp), `EqualizerBottomSheet.kt:166-169` (32dp), `HomeScreen.kt:99` (42dp), `NowPlayingScreen.kt:120-128` (36×**4**dp).
- **Dead code that misleads:** `MiniPlayer.kt:164-307` contains two extra overloads duplicating the live implementation, and `MiniPlayerProgressBar` (`:337-360`) takes a `progressProvider: () -> Float` non-composable read that makes the bar permanently unskippable — a future refactor to the "nicer" API would silently reintroduce the recomposition storm the live path avoids. `TrackItem.kt:104` declares `isPlaying` that **no caller ever passes**, so ~60 lines of animating "now playing" indicator are unreachable. `MainActivity.kt:294-301` is a dead `now_playing/{trackId}` route nothing navigates to, with no `arguments` for its path param.
- **Unlabelled interactive controls:** `NowPlayingScreen.kt:306-308` renders a Share `IconButton` that does nothing at all.

### Theme

- **`Theme.kt:27-38` — `darkTheme` and `dynamicColor` are declared and never used.** The body unconditionally applies the hardcoded dark `AppColorScheme`; both parameters and the six related imports are dead. The app is permanently dark, ignoring `Configuration.UI_MODE_NIGHT_*`. Users who set a light system theme — or need reduced luminance for photosensitivity — have no way out. The signature actively misleads every call site.
- **`Type.kt:12-24` — font files registered at the wrong weight.** `bricolage_grotesque_bold.ttf` is declared as `FontWeight.Normal` (`:15`) and `syne_medium.ttf` as `FontWeight.Normal` (`:23`). Any W400 request in these families renders **bold**. Currently latent because `Typography` only uses W500-W800, but `SettingsScreen.kt:646-649` and `AppBottomNavigationBar.kt:193-196` compose these families with a runtime `fontWeight` expression.
- **11 raw `Color.Black`/`Color.White` references** bypass the theme's own `TextOnAccent` token (`theme/Color.kt:19`), which exists for exactly that case.

### Build & dependency hygiene

- **Firebase is entirely dead.** The `google-services` plugin, `firebase-ai`, `firebase-appcheck-recaptcha`, and the BOM are all declared, but there are **zero `com.google.firebase` imports** in main source and no `google-services.json`. `MissingGoogleServicesStrategy.WARN` and `googleServices.missing.passthrough=true` mask the failure, so a broken Firebase setup would ship silently. Note `PlaybackService.kt:22-23` imports Guava (`Futures`/`ListenableFuture`) **transitively through `firebase-ai`** — removing Firebase breaks that import.
- **Roborazzi is declared but unused** — plugin plus four test dependencies, zero Roborazzi tests, and a stale `app/src/test/screenshots/greeting.png` template artifact.
- **`.env` falls back to a placeholder key** (`app/build.gradle.kts:66-69` + `.env.example:5`), turning auth failures into runtime rather than build-time errors. Moot in practice since `GEMINI_API_KEY` is never read in source.
- **`metadata.json:5` declares a Gemini capability** that has no corresponding code or key usage anywhere.
- **`ExoPlayer` is constructed on the main thread during `Application.onCreate`.** `SonnetApplication.kt:31` eagerly forces `AppContainer`, which builds `ExoPlayer` (`AudioPlayer.kt:49-59`) and grabs `AudioManager`. This also runs when the process is started *only* for `PlaybackService`.
- **`themes.xml:4` extends a platform theme** (`android:Theme.DeviceDefault.NoActionBar`) app-wide. Fine for pure Compose today; it will fail the moment anything needs a Material/AppCompat context, and there is no `values-v31` variant for the Android 12 window attributes that `enableEdgeToEdge()` interacts with.
- **No `release { }` guard on missing signing env vars** — see H9.

---

## Low

- **Dead code with zero call sites** (all verified by repo-wide grep): `PlaylistDao.kt:36-37` `getTrackCountForPlaylist`; `PlaylistDao.kt:16-17` `getAllPlaylists`; the **entire play-history read path** (`PlayHistoryDao.kt:12-13` → `PlayHistoryRepositoryImpl.kt:13-17` → `GetPlayHistoryUseCase` → `AppContainer.kt:46`) is wired but never consumed — the history list has no UI; `UserPreferencesDataStore.kt:24,51,82` (`LAST_PLAYED_TRACK_ID`) and `:25,52,86-88` (`HIGH_QUALITY_AUDIO`) are declared, never read or written; `HomeViewModel.kt:26-28,53-54,62-64`; `LibraryViewModel.kt:74-76`; `SearchScreen.kt:9-11,46-51`; `Type.kt:34-35`.
- **Developer name shipped as a default.** `UserPreferencesDataStore.kt:58` defaults the greeting to `"Andrei"`, duplicated as a literal at `HomeScreen.kt:41` and `SettingsScreen.kt:44`, while `:111` falls back to `"User"`. Inconsistent, and one of them is personal.
- **`HomeScreen.kt:85` hardcodes `"Goodmorning,"`** and never varies with time of day.
- **Now Playing like state is in-memory only.** `NowPlayingScreen.kt:79` — `remember(track?.id) { mutableStateOf(false) }` resets whenever the overlay leaves composition and is never persisted. The heart button is a no-op beyond that local flag.
- **`remember` keyed on the wrong field.** `NowPlayingScreen.kt:86-90` keys on `track?.id` but reads `track?.title`; `:191-195` keys on `track?.id` but reads `track?.artist`. A title/artist change at a stable id won't update the colour.
- **Inconsistent state durability.** `LibraryScreen.kt:76-77` correctly uses `rememberSaveable` for `isGridView`/`selectedCategory`, but `:74-75` uses plain `remember` for `showAddDialog` and the **typed playlist name**. The manifest has `screenOrientation="portrait"` (ignored on large-screen/foldable) and no `configChanges`, so the Activity can be recreated and the typed name lost.
- **`SearchViewModel` has no `SavedStateHandle`** — the query and results are lost on process death.
- **Search has no debounce.** `SearchViewModel.kt:71-98` filters the entire library (3× `String.contains` per track) on every keystroke burst; `combine` bounds it, but a 200ms debounce is warranted at library scale.
- **Slow `contains` over deep-equal objects.** `SearchViewModel.kt:60-61` uses `it !in playedAlbums` on a `List<Album>` where `Album` carries a full `tracks: List<Track>`, so each comparison deep-compares track lists.
- **Permission re-requested on every Library tab visit.** `LibraryScreen.kt:87-103` re-launches the `READ_MEDIA_AUDIO` request on every fresh composition if permanently denied; `shouldShowRequestPermissionRationale` is never consulted, and `MainActivity.kt:104-108` already duplicates the request.
- **`selectedCategory` is a bare `String` compared with `==` against literals in 5 places** (`LibraryScreen.kt:77,135-192,256,340,435`) — an `enum class` would be exhaustive-checked and prevent silent fall-through on stale `rememberSaveable` restore.
- **Nine unconditional `as SonnetApplication` casts** (`MainActivity.kt:115` and 8 screen files) instead of one `CompositionLocal`. Will `ClassCastException` in instrumented tests, Robolectric, or a secondary process.
- **Dead route mutating nav state from a side effect.** `MainActivity.kt:294-301` — a `composable()` whose entire body is a `LaunchedEffect` calling `popBackStack()`; nothing navigates to it.
- **Crash handler does blocking I/O on the crashing thread.** `SonnetApplication.kt:19-28` — `stackTraceToString()` + `writeText()` on the main thread, and `defaultHandler?.uncaughtException(...)` uses `?.`, so a null default handler would leave the process hung with a corrupt frame stack.
- **`FadeController.safeSetVolume` swallows bare `Throwable`** (`:45, 52`), hiding real bugs including `CancellationException`. Should catch `IllegalStateException`/`IllegalArgumentException` specifically.
- **`EqualizerManager`'s default scope is never cancellable** (`EqualizerManager.kt:41`) — `CoroutineScope(Dispatchers.IO)` as a default parameter. Currently dead (AudioPlayer injects its own), but a trap.
- **Five separate DataStore reads at player startup** (`AudioPlayer.kt:156-177`) — five `.first()` calls, five readers, five cancellations at cold start. One `first()` on a `combine` would be a single read.
- **Dead Coil network config** — `SonnetApplication.kt:49-50` sets `networkCachePolicy(ENABLED)` + `respectCacheHeaders(false)`, but every image URI is a `content://media/...` URI; there is no network path.
- **11 raw Black/White refs;** `EqualizerBottomSheet.kt:196` is the only unkeyed lazy `items` call; `EqualizerBottomSheet.kt:369` uses `forEachIndexed` in a `Column` (correct for 5 bands — noted so it isn't "fixed"); `AlbumArt.kt:29` initialises `isLoading = true` so cached images flash a placeholder; `AlbumArt.kt:59` `matchParentSize()` contributes nothing and silently yields 0×0 for any caller passing only a background; `PlaylistArtwork.kt:25` un-remembered filter producing `List<String?>`; `Skeleton.kt:91` hardcoded alpha bypassing the file's own `LocalShimmerAlpha` mechanism; `SearchScreen.kt:164` uses a `🎧` emoji as illustration; `ExampleUnitTest.kt:9-14` has five unused imports from the abandoned coroutine-test rewrite; `ExampleUnitTest.kt:22` pins Robolectric to SDK 34 while `compileSdk`/`targetSdk` are 36; `build.gradle.kts:3-8` applies no `kotlin.android` plugin, relying on AGP 9's built-in Kotlin.
- **Route list allocated inside transition lambdas.** `MainActivity.kt:171-172, 188-189, 205-206, 222-223` — `listOf("home","library","search","settings")` twice per lambda, up to 8 allocations per navigation, during the transition animation.

---

## Done well

It would be unfair to read this list as only criticism. Several decisions here are better than most shipped apps:

1. **No `fallbackToDestructiveMigration()`** — verified absent repo-wide. This is the single most common Room data-loss footgun, and refusing it means the app crashes loudly on a schema bump rather than silently wiping playlists and history. Correct for data users cannot recreate.
2. **The permission model is correct end to end.** `AndroidManifest.xml:5-6` pairs `READ_EXTERNAL_STORAGE` with `maxSdkVersion="32"` against `READ_MEDIA_AUDIO`, and the runtime checks mirror the identical version branch — so the app never requests a permission it hasn't declared. `POST_NOTIFICATIONS` is requested only on API 33+. This is the part most apps get wrong.
3. **`FOREGROUND_SERVICE_MEDIA_PLAYBACK` is declared** alongside `foregroundServiceType="mediaPlayback"` — mandatory from API 34 and easy to omit.
4. **No SQL injection surface.** Every query uses `:named` bind args. The one dynamically composed string (`MediaStoreScanner.kt:24`) is built solely from `MediaStore` constants with `selectionArgs = null`.
5. **No unvalidated-URI path.** Track URIs are minted only via `ContentUris.withAppendedId` and read back with `Uri.parse`. There is no `Uri.fromFile`/`FileProvider` branch — correct under scoped storage, and explicitly reasoned about in comments at `AudioPlayer.kt:476-479`.
6. **No blocking DB on the main thread, anywhere.** Every read is a `Flow`; writes are `suspend`. `allowMainThreadQueries()` appears only in the instrumented test.
7. **No leaked Context in the data layer.** `AppContainer` is a true singleton built from the Application, and `AudioPlayer.kt:39` explicitly normalizes with `context.applicationContext`.
8. **State management is unusually disciplined** — `collectAsStateWithLifecycle` in **30 of 31** collection sites, `stateIn`/`WhileSubscribed` throughout, and correct `ViewModelStoreOwner` scoping for all 6 NavHost ViewModels. The one outlier is a real bug (H-above / `AddToPlaylistDialog`).
9. **Debouncing is done right on the hardest write path** — `EqualizerManager.persistSettings` (`:277-301`) coalesces the 30-60 writes/second a slider drag would produce. The crossfade path (Medium, above) should copy this.
10. **Preference values read from disk are never trusted** — crossfade clamps to 1..12, `setUserName` trims and rejects blanks, the repeat-mode read path re-coerces, and the band-gain CSV parser requires exactly 5 entries.
11. **A single shared `ExoPlayer`** for the whole app, with `WAKE_MODE_LOCAL` correctly paired to the `MODIFY_AUDIO_SETTINGS` declaration — the correct fix for double playback.
12. **Notification channel is created before the session** with an explicit small icon and a comment explaining *why* (`PlaybackService.kt:72-74`) — avoiding the Android 13+ `InvalidArgumentException` this codebase clearly already hit once.
13. **Secrets hygiene is correct.** Zero credentials in git history; `.gitignore:17-18` covers `.env` and `debug.keystore`; release passwords read from env only; `.env.example` ships a placeholder. The version catalog is disciplined — every alias resolves, no hardcoded versions.
14. **Gradle wrapper is now complete and reproducible** (pinned 9.8.0, `validateDistributionUrl=true`) — it was entirely absent before this audit.

---

## Recommended remediation order

**Before any release:**

1. H9 — fix release signing (or explicitly ship unsigned).
2. H11 — delete the two stub test bodies, fix the guaranteed-failing `packageName` assertion, add CI.
3. H1, H2 — shuffle and play-counting. These are the app's core promise.
4. C2 — the one-line equalizer fix; the feature is unusable until then.

**Next, for correctness and data safety:**

5. H5 (stale-track deletion) and H6 (Room migration infrastructure) — both get worse with every user.
6. H4 (GlobalScope) — a latent crash-on-disk-full.
7. H3 (real crossfade) — the most audible remaining quality problem.
8. H1–H2 (accessibility semantics) — the app is effectively unusable with a screen reader.

**Then, performance:**

9. C3 (`ProvideShimmer` gate) — one conditional removes a permanent 60fps recomposition.
10. M-list: the `remember` gaps in `LibraryScreen`, the DataStore-per-drag-frame write, `SearchScreen`'s dead subscription, and the draw-phase allocations.

**Ongoing:**

11. Establish a real test baseline around `QueueManager` and `FadeController` first — they hold the highest concentration of untested logic, they are pure enough to test without Android (though `FadeController` needs an injectable clock and scope, which it currently lacks), and a regression suite there would have caught C1 and H1 immediately.
