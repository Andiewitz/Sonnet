# Sonnet — Ticket Breakdown 001

**Source:** [`docs/AUDIT.md`](./AUDIT.md) (audit of commit `76d64f9`)
**Created:** 2026-09-29
**Baseline:** `main` @ `76d64f9`

Each ticket is scoped to be an independently reviewable and mergeable change. Where a ticket is a pure-logic fix, a regression test is **required** — that is the point of the ticket, not an optional extra.

Status legend: ⬜ open · 🟡 in progress · ✅ done

---

## Wave 1 — Correctness (Critical)

These are the findings that make a shipped feature not do what its name says. All are small, all are high-impact.

### ⬜ T-001 — Shuffle does not shuffle
**Severity:** Critical · **Audit ref:** C1 · **Estimate:** S

`QueueManager.peekNextTrack()` returns `otherIndices.firstOrNull()` (`QueueManager.kt:84-90`) — always the *second track in the list* — while `getNextTrack()` returns `otherIndices.random()` (`:118-125`). The peek drives the end-of-track crossfade (`AudioPlayer.kt:429-434`), so with shuffle on, tracks advance into queue position 2 repeatedly. The two functions also disagree, so the transition previews one track and plays a different one.

**Acceptance**
- A single selection function is used by both `peekNextTrack` and `getNextTrack`.
- With shuffle on, N auto-advances over a 10-track queue never return the same track twice in a row.
- The track the transition previews is the track it plays.
- No regression in non-shuffle ordering.

**Tests (required)** — `QueueManagerShuffleTest`
- `shuffle_doesNotAlwaysReturnSecondTrackInQueue`
- `peekAndGet_agree_whenShuffleEnabled`
- `sequentialShuffle_doesNotRepeatTrackConsecutively`
- `nonShuffle_preservesQueueOrder`

### ⬜ T-002 — "No repeat" mode is unreachable
**Severity:** Critical · **Audit ref:** H1 · **Estimate:** S

`QueueManager.setRepeatMode()` coerces anything that isn't `REPEAT_MODE_ONE` to `REPEAT_MODE_ALL` (`:43-49`), and `toggleRepeat()` only cycles `ALL ↔ ONE` (`:51-59`). `UserPreferencesDataStore` applies the same coercion on read (`:59-66, :115-122`). A user cannot stop the queue at the end of the last track.

**Acceptance**
- Cycle is `OFF → ALL → ONE → OFF`.
- A stored `REPEAT_MODE_OFF` round-trips as `OFF` and is no longer silently upgraded.
- `getNextTrack` returns `null` past the end of the queue when `OFF`.
- `PlaybackService.updateCustomLayout` shows a distinct "off" affordance.

**Tests (required)** — `QueueManagerRepeatTest`
- `toggleRepeat_cyclesAllThreeModes`
- `repeatOff_getNextTrack_pastEnd_returnsNull`
- `setRepeatMode(REPEAT_MODE_OFF)_isPreserved`

### ⬜ T-003 — Equalizer sheet dismisses on any non-interactive tap
**Severity:** Critical · **Audit ref:** C2 · **Estimate:** S

`EqualizerBottomSheet.kt:98` uses `.clickable(enabled = false) {}` with the comment *"consume clicks inside sheet"*. A disabled clickable returns immediately from its `pointerInput` and never consumes, so taps on the "PRESETS" label (`:185`), the "FREQUENCY BANDS (dB)" label (`:330`), the header (`:139-148`) and blank gaps fall through to the live scrim handler at `:79` and dismiss the sheet.

**Acceptance**
- Taps on non-interactive regions of the sheet do **not** dismiss it.
- Taps on the scrim still dismiss it.
- All interactive children (switch, preset pills, band sliders, close button) still work.
- The misleading comment is replaced with one describing actual behaviour.

**Tests (required)** — instrumented `EqualizerBottomSheetTest`
- `tapOnNonInteractiveArea_doesNotDismiss`
- `tapOnScrim_dismisses`
- `tapOnPreset_appliesPreset`

### ⬜ T-004 — Home screen recomposes at ~60fps permanently
**Severity:** Critical · **Audit ref:** C3 · **Estimate:** S

`ProvideShimmer` (`Skeleton.kt:20-35`) has no `enabled` gate and drives a `rememberInfiniteTransition`. `HomeScreen.kt:63` wraps the entire `LazyColumn` in it unconditionally, so the whole screen re-executes every frame even after loading completes and no skeleton is visible. It cannot be skipped because the `Modifier` chain (`:65-67`) and `PaddingValues` (`:69`) are reallocated each time.

**Acceptance**
- Shimmer animation only runs while at least one skeleton is on screen.
- Once loaded, the Home screen stops recomposing on animation frames.
- The `Modifier` chain, `PaddingValues` and chunked lists are `remember`ed.

**Tests (required)** — instrumented + manual profile
- `homeScreen_stopsRecomposing_afterContentLoads`
- Manual: confirm no `~60fps` recomposition in Layout Inspector once loaded.

### ⬜ T-005 — Play history recorded on load, not on play
**Severity:** High · **Audit ref:** H2 · **Estimate:** M

`AudioPlayer.kt:318` fires `onTrackPlayed` inside `loadAndStart` — *after* `prepare()` but before `STATE_READY`, and *after* the `try/catch`, so a track that fails to load is still counted. The crossfade path (`:432-434`) records up to 12s before the track is audible. "Most Played" and "Recent" are therefore meaningless.

**Acceptance**
- A play is recorded only once actual audible playback begins (`STATE_READY`) and has lasted a minimum threshold.
- A track that fails to load is never recorded.
- Skipping through a queue rapidly does not record every skipped track.

**Tests (required)** — `AudioPlayerPlayHistoryTest`
- `play_isNotRecorded_beforeStateReady`
- `play_isNotRecorded_whenLoadThrows`
- `play_isRecorded_once_afterMinimumPlaytime`

### ⬜ T-006 — Previous-track semantics are wrong under shuffle and repeat-off
**Severity:** High · **Audit ref:** C1 · **Estimate:** M

`QueueManager.getPreviousTrack()` (`:139-161`) ignores `_shuffleModeEnabled` entirely — there is no previously-played history stack — and falls through to `return list.firstOrNull()` at `:160` without updating `currentTrackIndex`.

**Acceptance**
- Shuffle maintains a history stack; "previous" walks it back.
- Under `REPEAT_MODE_OFF` at queue position 0, "previous" does not silently restart the current track.
- `currentTrackIndex` is left consistent with what was returned.

**Tests (required)** — `QueueManagerPreviousTest`
- `previous_afterShuffle_usesHistoryStack`
- `previous_atQueueStart_repeatOff_doesNotRestartCurrentTrack`
- `previous_updatesCurrentIndex`

---

## Wave 2 — Playback quality

### ⬜ T-007 — Crossfade is a fade-to-silence, not a crossfade
**Severity:** High · **Audit ref:** H3 · **Estimate:** L

`FadeController.transitionBetweenTracks` (`:119-172`) fades out, then `onSwitch()` runs `player.stop(); clearMediaItems(); setMediaItem(); prepare()` (`AudioPlayer.kt:302-311`), then fades in. There is a hard gap and a cold reload on **every** track change, destroying ExoPlayer preloading.

**Acceptance** — two accepted outcomes, pick one and document it:
- **(a)** ExoPlayer native crossfade via `ExoPlayer.Builder.setCrossfadeEnabled(true)`, or
- **(b)** queue all tracks with `setMediaItems(list, index, 0)` and never clear the queue on a normal advance.

Either way the audible gap between tracks is eliminated and `loadAndStart` stops tearing down the player on every track.

### ⬜ T-008 — Progress tracker drives 10Hz full-screen recomposition
**Severity:** Medium · **Audit ref:** Medium/Playback · **Estimate:** M

`AudioPlayer.startProgressTracker` (`:405-443`) writes `_currentPosition` every 100ms on `Dispatchers.Main.immediate`. `NowPlayingScreen` recomposes on each tick and re-formats two `String.format` calls (`:494-495`) per tick — including the duration string, which is a pure function of `duration`.

**Acceptance**
- Position updates are throttled or derived without a full-screen recomposition.
- The duration string is `remember`ed on `duration`, not recomputed per tick.
- `String.format` is given an explicit `Locale`.

### ⬜ T-009 — `AlwaysNavigablePlayer` lies to the system
**Severity:** Medium · **Audit ref:** Medium/Playback · **Estimate:** S

`hasNextMediaItem()` / `hasPreviousMediaItem()` always return `true` and `isCommandAvailable` always returns `true` for seek-prev/next (`:22-34`). Bluetooth, Assistant and Wear OS controllers therefore show enabled navigation buttons at the end of the queue.

**Acceptance** — availability reflects the real queue state while custom `onNext`/`onPrevious` callbacks still receive a chance to run.

### ⬜ T-010 — `AudioPlayer.release()` is never called
**Severity:** High · **Audit ref:** H13 · **Estimate:** S

`AudioPlayer.release()` (`:488-499`) has zero call sites. `PlaybackService.onDestroy` (`:157-163`) releases only the `MediaSession`. ExoPlayer, `LoudnessEnhancer` and the `Equalizer`/`BassBoost`/`Virtualizer` native handles are never released.

**Acceptance** — a real teardown path exists and runs; `AudioPlayer.scope` is cancelled; native `AudioEffect` handles are released.

### ⬜ T-011 — `GlobalScope` write can crash the app during playback
**Severity:** High · **Audit ref:** H4 · **Estimate:** S

`AppContainer.kt:51-53` launches `recordPlayUseCase` in `GlobalScope` with no error handling. A failing insert reaches the app's own crash handler (`SonnetApplication.kt:19-28`) and kills the process mid-song.

**Acceptance** — replaced with a `SupervisorJob`-backed scope owned by `AppContainer` and cancelled in `close()`; exceptions are caught and logged, never propagated to the crash handler.

---

## Wave 3 — Data integrity

### ⬜ T-012 — Deleted tracks persist forever
**Severity:** High · **Audit ref:** H5 · **Estimate:** M

`TrackRepositoryImpl.scanDeviceForTracks()` (`:49-69`) only inserts. Deleted files, uninstalled SD cards and MediaStore re-indexes leave orphan rows in `tracks` *and* their `playlist_track` join rows. The user sees unplayable entries; playback throws and is only logged.

**Acceptance** — the rescan diffs the scanned ID set against the DB and deletes the difference, cascading join rows in the same transaction.

**Tests (required)** — `TrackRepositoryRescanTest`
- `rescan_removesTracksNoLongerOnDevice`
- `rescan_preservesTracksStillPresent`
- `rescan_cascadesPlaylistJoinsForRemovedTracks`

### ⬜ T-013 — No Room migration infrastructure
**Severity:** High · **Audit ref:** H6 · **Estimate:** M

`version = 1`, `exportSchema = false`, no `addMigrations`, no schema location, no `app/schemas/`. The next `version` bump bricks every install. (Note: the *absence* of `fallbackToDestructiveMigration` is correct and must stay.)

**Acceptance**
- `exportSchema = true` with the KSP `room.schemaLocation` arg configured.
- `app/schemas/` committed.
- `MigrationTestHelper` is usable, demonstrated by a `1→2` migration test.

### ⬜ T-014 — No foreign keys; `REPLACE` is destructive
**Severity:** Medium · **Audit ref:** Medium/Data · **Estimate:** M

`PlaylistTrackCrossRef` and `PlayHistoryEntity` declare no `@ForeignKey`, so orphans insert silently. `TrackDao.insertTracks` uses `OnConflictStrategy.REPLACE` (`:18`), which is DELETE+INSERT and becomes destructive once FKs land.

**Acceptance** — FKs with `onDelete = CASCADE`; `insertTracks` switched to `@Upsert` **before** FKs are added.

### ⬜ T-015 — `play_history` is unbounded and unindexed
**Severity:** Medium · **Audit ref:** Medium/Data · **Estimate:** S

No `indices` on `trackId` or `playedAt`; three hot home-screen queries do `GROUP BY`/`ORDER BY COUNT()`/`MAX()` over a monotonically growing table. Nothing prunes.

**Acceptance** — indices added; a retention sweep removes rows older than a defined window.

### ⬜ T-016 — N+1 query to build playlist thumbnails
**Severity:** Medium · **Audit ref:** Medium/Data · **Estimate:** M

`PlaylistDao.getPlaylistsWithTracks()` uses `@Relation` (`:19-21`), issuing one query per playlist per emission and loading full `TrackEntity` rows when `PlaylistRepositoryImpl.kt:22` only needs `albumArtUri.take(4)`.

**Acceptance** — a single JOIN projecting `(playlistId, albumArtUri)` limited to 4 per playlist, or art denormalised onto `PlaylistEntity`.

---

## Wave 4 — Build & release

### ⬜ T-017 — Release build cannot be signed
**Severity:** High · **Audit ref:** H9 · **Estimate:** S

`app/build.gradle.kts:28-32` falls back to a nonexistent `my-upload-key.jks`; `STORE_PASSWORD`/`KEY_PASSWORD` read from env with no `require(...)`. `assembleRelease` fails at `validateSigningRelease` with an opaque error.

**Acceptance** — config fails fast with an actionable message at configuration time when signing env vars are absent; an unsigned debug/release path is available for local builds.

### ⬜ T-018 — Release ships unminified; proguard rules are empty
**Severity:** High · **Audit ref:** H10 · **Estimate:** M

`isMinifyEnabled = false` (`:45`) and `proguard-rules.pro` is 100% comments.

**Acceptance** — minify+shrink enabled for release with real keep rules for Moshi codegen, Media3, Room and the Firebase-absent reflection paths; APK size delta recorded.

### ⬜ T-019 — Remove dead Firebase dependency
**Severity:** Medium · **Audit ref:** Medium/Build · **Estimate:** S

`google-services` plugin, `firebase-ai`, `firebase-appcheck-recaptcha` and the BOM are declared with **zero** `com.google.firebase` imports in source. `WARN` + `missing.passthrough` mask the failure. Note `PlaybackService.kt:22-23` imports Guava *transitively via `firebase-ai`* — removing this breaks that import.

**Acceptance** — Firebase removed; the Guava import replaced with a `ListenableFuture` alternative or Guava declared explicitly.

### ⬜ T-020 — No CI
**Severity:** Medium · **Audit ref:** Medium/Build · **Estimate:** M

No `.github/`, no workflows. Nothing runs the tests.

**Acceptance** — workflow running lint + unit tests on PRs; instrumented tests at least on a manual/dispatched basis.

---

## Wave 5 — Test baseline

Nothing below can be validated without this first. T-001 and T-002 are unmergeable without it.

### ⬜ T-021 — Delete stub tests and fix the guaranteed failure
**Severity:** High · **Audit ref:** H11 · **Estimate:** S

- `TrackDaoTest.kt:33-36` — empty body, `// Stub`, no assertions.
- `LibraryScreenTest.kt:15-18` — empty body; `composeTestRule` spun up but unused.
- `ExampleInstrumentedTest.kt:20` — asserts `packageName == "com.example"` but `applicationId` is `com.aistudio.sonnet.xwqrty`. **Fails by construction.**

**Acceptance** — stubs deleted or implemented; the packageName assertion corrected; `ExampleUnitTest`'s five unused imports (`:9-14`, an abandoned coroutine-test rewrite) cleaned up.

### ⬜ T-022 — `QueueManager` test suite
**Severity:** High · **Audit ref:** H11 · **Estimate:** M

Zero coverage on the highest-density pure logic in the app. `QueueManager` is pure Kotlin apart from `Player` int constants and is directly unit-testable.

**Acceptance** — covers shuffle, repeat (all three modes), previous, index consistency, and empty/single-item playlists. This suite must fail against the pre-T-001/T-002 code.

### ⬜ T-023 — DAO aggregate query tests
**Severity:** High · **Audit ref:** H11 · **Estimate:** M

`getMostPlayedTracks`, `getLeastPlayedTracks`, `getRecentTracks` (`TrackDao.kt:21,24,27`) are the highest-bug-density SQL in the app and the only DAO test is a stub.

**Acceptance** — real assertions over seeded data covering ties, empty history, and tracks with no history.

### ⬜ T-024 — `MediaStoreScanner` tests
**Severity:** Medium · **Audit ref:** H11 · **Estimate:** M

Zero coverage on cursor parsing, `getColumnIndexOrThrow` (`:34-40`) and null-metadata fallbacks.

**Acceptance** — `ContentResolver` faked with a `MatrixCursor`; covers missing columns, null title/artist/album, and zero-duration filtering.

### ⬜ T-025 — ViewModel tests
**Severity:** Medium · **Audit ref:** H11 · **Estimate:** L

All 7 ViewModels untested despite `mockk` being a declared dependency with no usage.

---

## Wave 6 — Performance

### ⬜ T-026 — DataStore write on every drag frame
**Severity:** Medium · **Audit ref:** Medium/UI · **Estimate:** S

`SettingsScreen.kt:334-340` calls `setCrossfadeDurationSeconds` from `Slider.onValueChange` — up to 60 disk commits/sec. `EqualizerManager.persistSettings` (`:277-301`) already has the correct debounce pattern.

**Acceptance** — debounced like the equalizer path, or written only in `onValueChangeFinished`.

### ⬜ T-027 — Slider `remember` key fights the DataStore it reads
**Severity:** Medium · **Audit ref:** Medium/UI · **Estimate:** S

`SettingsScreen.kt:240-242` keys the local state on the value it's derived from, so each round-trip re-initialises the in-progress drag. It also lives inside a `LazyColumn` `item { }`, discarding the edit on scroll.

**Acceptance** — drag is smooth and continuous; external changes still sync in; state survives scrolling out of view.

### ⬜ T-028 — Un-`remember`ed list allocations in list bodies
**Severity:** Medium · **Audit ref:** Medium/UI · **Estimate:** S

`chunked(2)` and per-item String key interpolation recomputed on the composition thread at `LibraryScreen.kt:259,348,443` and `HomeScreen.kt:159`. The key lambdas walk `Album.tracks` to build Strings per item per frame.

**Acceptance** — chunking is `remember`ed; items key on a stable id rather than string interpolation.

### ⬜ T-029 — Draw-phase allocations
**Severity:** Medium · **Audit ref:** Medium/UI · **Estimate:** S

`Brush` allocated inside `drawBehind` (`NowPlayingScreen.kt:107`) and inside a continuous pulse (`:231-233`); two `listOf` per `Canvas` redraw (`EqualizerIcon.kt:33,38`).

**Acceptance** — brushes `remember`ed on their inputs; `EqualizerIcon` uses scalar locals.

### ⬜ T-030 — Dead subscription recomposes Search
**Severity:** Medium · **Audit ref:** Medium/UI · **Estimate:** S

`SearchScreen.kt:69` subscribes to the full track list and never reads it. Any change to the track table recomposes the screen including the `TextField` holding the in-progress query.

### ⬜ T-031 — Debounce the MediaStore rescan
**Severity:** High · **Audit ref:** H7 · **Estimate:** S

`MainActivity.kt:369-372` rescans on every `onResume`; `:107` also fires on `onCreate`, so every cold start scans twice.

**Acceptance** — minimum interval + `lastScanAt` guard; redundant `onCreate` call removed; a scan skipped by the mutex is *reported* rather than silently dropped (`TrackRepositoryImpl.kt:50`).

### ⬜ T-032 — N+1 / full-materialisation in the scan path
**Severity:** Low · **Audit ref:** Medium/Data · **Estimate:** S

`MediaStoreScanner` builds a full `List<Track>`, which `TrackRepositoryImpl.kt:54-64` re-maps to a second `List<TrackEntity>` before one batched insert. Batch in chunks inside the existing `Mutex`.

---

## Wave 7 — Accessibility

The app is currently unusable with a screen reader: zero `Modifier.semantics` in the entire source tree.

### ⬜ T-033 — Navigation bar semantics
**Severity:** High · **Audit ref:** H12 · **Estimate:** S

`AppBottomNavigationBar.kt:181` sets `contentDescription = label` on the `Icon` while `:191-198` renders the same label as `Text` → TalkBack announces "Home, Home". The `Column` (`:158-199`) has no `Role.Tab` and no `selected` semantics.

**Acceptance** — `contentDescription = null` on the icon; `Modifier.selectable(selected, role = Role.Tab)` on the item; the active tab is announced.

### ⬜ T-034 — Album art announced 19× redundantly
**Severity:** Medium · **Audit ref:** H12 · **Estimate:** S

`AlbumArt.kt:58` sets `contentDescription = "Album Art"` on an image used in 19 places including every list row. A 500-track list announces it 500 times, duplicating adjacent text. It is decorative — `null` is correct.

### ⬜ T-035 — Unlabelled interactive controls
**Severity:** Medium · **Audit ref:** H12 · **Estimate:** S

`HomeScreen.kt` has **zero** `contentDescription` occurrences; the equalizer button (`:102-105`) and the Now Playing drag pill (`:120-128`) are unlabelled. `NowPlayingScreen.kt:306-308` renders a Share `IconButton` that does nothing.

### ⬜ T-036 — Touch targets below 48dp
**Severity:** Medium · **Audit ref:** Medium/UI · **Estimate:** S

36dp (`LibraryScreen.kt:136,140,143`), 32dp (`SettingsScreen.kt:272,308`, `EqualizerBottomSheet.kt:166-169`), 42dp (`HomeScreen.kt:99`), and 36×**4dp** (`NowPlayingScreen.kt:120-128`).

**Acceptance** — ≥48dp touch bounds via `minimumInteractiveComponentSize` / `touchableInExpandedBounds` without inflating the visual design.

### ⬜ T-037 — Rotated equalizer sliders are inaccessible
**Severity:** Medium · **Audit ref:** H12 · **Estimate:** M

Band sliders are horizontal `Slider`s rotated 270° via `graphicsLayer` (`:390-407`) — a draw-time transform that does not update semantics. TalkBack announces an unlabelled "Slider" with no frequency or dB value.

### ⬜ T-038 — Section headings and switch rows
**Severity:** Low · **Audit ref:** H12 · **Estimate:** S

No `heading()` semantics on any section title. `SettingsScreen.kt:733` puts `.clickable` on a `Row` that also contains a `Switch`, so TalkBack announces it twice with no role or state.

---

## Wave 8 — Robustness & polish

### ⬜ T-039 — Process death leaves a dead-end screen
**Severity:** High · **Audit ref:** Medium/UI · **Estimate:** M

`MainActivity.kt:119,125,127,346` — `rememberSaveable` restores `isNowPlayingOpen = true` but the queue is never persisted, so the overlay never appears, `showBottomBar` stays false (no mini player **and no bottom nav**), and `BackHandler` swallows the back gesture. The user is stuck.

**Acceptance** — `BackHandler` and `showBottomBar` are gated on the same condition as the overlay, so a restored-but-unsatisfiable state can never trap the user.

### ⬜ T-040 — Now Playing has no scroll container
**Severity:** High · **Audit ref:** Medium/UI · **Estimate:** S

`NowPlayingScreen.kt:102-430` stacks fixed-height children with no `verticalScroll`. On a short window or at font scale > 1.0 the transport controls become unreachable.

### ⬜ T-041 — Draggable now playing uses per-frame delta
**Severity:** Medium · **Audit ref:** Medium/UI · **Estimate:** S

`NowPlayingScreen.kt:131-141` compares a single frame's `dragAmount` to a threshold: a fast flick dismisses, a slow deliberate swipe does not. Also `pointerInput(Unit)` captures a stale lambda.

### ⬜ T-042 — MediaSession controller allowlist
**Severity:** High · **Audit ref:** H8 · **Estimate:** M

`PlaybackService.onGetSession` (`:153-155`) returns the session unconditionally and `onConnect` (`:166-185`) grants extra commands to *every* controller with no package check. Any installed app can seize playback and mutate persisted preferences via `onCustomCommand`.

**Acceptance** — `onGetSession` gated on an allowlist of system/Assistant/Auto packages, or a `signature`-level permission on the service.

### ⬜ T-043 — `allowBackup` restores device-specific URIs
**Severity:** Medium · **Audit ref:** Medium/Data · **Estimate:** S

`backup_rules.xml` and `data_extraction_rules.xml` are 100% commented-out templates while `allowBackup="true"`. Restoring `sonnet_db` to another device restores `playlist_track` rows pointing at `content://media/...` URIs minted on the old device — permanently broken playlists.

### ⬜ T-044 — Scanner misses secondary storage
**Severity:** Medium · **Audit ref:** Medium/Data · **Estimate:** S

`MediaStoreScanner.kt:14` uses `EXTERNAL_CONTENT_URI` (primary only). API 29+ needs `getContentUri(MediaStore.VOLUME_EXTERNAL)`. SD-card music is invisible.

### ⬜ T-045 — Scanner swallows permission failures
**Severity:** Medium · **Audit ref:** Medium/Data · **Estimate:** S

`MediaStoreScanner.kt:77-79` catches `Exception`, so a revoked `READ_MEDIA_AUDIO` is indistinguishable from "no music found".

### ⬜ T-046 — Theme is permanently dark
**Severity:** Medium · **Audit ref:** Medium/Theme · **Estimate:** S

`Theme.kt:27-38` declares `darkTheme` and `dynamicColor` and **never uses either**; the body unconditionally applies the hardcoded dark scheme. `Configuration.UI_MODE_NIGHT_*` is ignored and users have no override.

### ⬜ T-047 — Font weights registered against the wrong TTF
**Severity:** Medium · **Audit ref:** Medium/Theme · **Estimate:** S

`Type.kt:15,23` register the *bold* and *medium* font files as `FontWeight.Normal`. Any W400 request renders bold. Latent today; `SettingsScreen.kt:646-649` and `AppBottomNavigationBar.kt:193-196` use runtime `fontWeight`.

### ⬜ T-048 — Hardcoded strings block i18n
**Severity:** Medium · **Audit ref:** Medium/UI · **Estimate:** L

~90 string literals, `strings.xml` has 3 entries, zero `stringResource` calls. Plurals hand-inlined ~10× with inconsistent logic. `SettingsScreen.kt:543` hardcodes `"Version 1.0.0"` while `versionName` is `"1.0"`, and `BuildConfig` is never imported anywhere.

### ⬜ T-049 — Remove dead code and duplicate implementations
**Severity:** Low · **Audit ref:** Low · **Estimate:** M

`MiniPlayer.kt:164-307` duplicates the live implementation twice over; `MiniPlayerProgressBar` (`:337-360`) takes a non-composable `progressProvider` that makes the bar permanently unskippable. `TrackItem.kt:104` `isPlaying` is never passed by any caller. `MainActivity.kt:294-301` is a dead route nothing navigates to. The entire play-history read path is wired but unconsumed. `UserPreferencesDataStore` has four unused keys.

### ⬜ T-050 — Crash handler does blocking I/O on the crashing thread
**Severity:** Low · **Audit ref:** Low · **Estimate:** S

`SonnetApplication.kt:19-28` — `stackTraceToString()` + `writeText()` on the main thread; `defaultHandler?.uncaughtException(...)` uses `?.`, so a null handler would hang the process.

---

## Suggested sequencing

1. **T-021** (unblock testing) → **T-022** (the suite that catches Wave 1)
2. **Wave 1** — T-001, T-002, T-003, T-004 each land with their tests green
3. **T-006**, **T-005**, **T-010**, **T-011** — playback correctness
4. **Wave 4** — T-017, T-018, T-020, so releases are actually shippable and verified
5. Wave 3, 6, 7, 8 as capacity allows

**Rule for this backlog:** any ticket touching `QueueManager`, `FadeController` or a DAO query ships with a test, or it does not merge. The audit's single most expensive finding is that 1.6% coverage let a shuffle bug, an unreachable repeat mode, and three stub tests survive in a repo with no CI.
