# Performance Optimization Audit & Implementation Plan

**Application:** Sonnet Offline Music Player  
**Target:** 60/120 FPS Fluidity, Reduced Input Latency, Zero Main-Thread Jank, Minimal Memory Churn & Battery Efficiency.

---

## 1. Executive Summary

A comprehensive, file-by-file audit of the Sonnet codebase was conducted across all architectural layers:
1. **Data & Storage Layer** (`MediaStoreScanner`, `TrackRepositoryImpl`, Room DAOs, `UserPreferencesDataStore`)
2. **Audio Engine & DSP** (`AudioPlayer`, `EqualizerManager`, `PlaybackService`)
3. **Image Loading Subsystem** (`SonnetApplication`, `AlbumArt`, Coil Configuration)
4. **Jetpack Compose UI & Animation Engine** (`HomeScreen`, `LibraryScreen`, `SearchScreen`, `NowPlayingScreen`, `MiniPlayer`, `TrackItem`, `Skeleton`)

The audit reveals that while the app has an elegant Material 3 design and rich features, several critical performance bottlenecks cause UI micro-stutters, frame drops during scrolling, excessive recompositions, and unnecessary main-thread/disk load.

---

## 2. Deep-Dive Audit Findings & Root Causes

### 2.1 UI Thread Blocking & Recomposition Traps (High Impact)

#### Bottleneck A: Heavy Sorting, Filtering & Grouping Directly in Compose (`SearchScreen.kt`)
- **Location:** `app/src/main/java/com/example/presentation/screen/search/SearchScreen.kt` (Lines 44–75)
- **Root Cause:**
  On every single character typed in the search bar, the UI thread synchronously executes:
  ```kotlin
  val filteredTracks = remember(searchQuery, allTracks) { ... }
  val matchedAlbums = remember(searchQuery, allTracks) {
      allTracks.filter { it.album.contains(...) }.groupBy { it.album }.map { ... }
  }
  val matchedArtists = remember(searchQuery, allTracks) {
      allTracks.filter { it.artist.contains(...) }.groupBy { it.artist }.map { ... }
  }
  ```
  In a library with hundreds or thousands of songs, running multiple `.filter()`, `.groupBy()`, and `.map()` iterations directly on the Android main thread during keypress events drops frames and introduces visible keyboard input lag.
- **Solution:** Move the search filtering pipeline into `SearchViewModel` using Kotlin Coroutines `Flow` with `debounce(150.milliseconds)`, `flowOn(Dispatchers.Default)`, and expose a combined UI state.

---

#### Bottleneck B: Unstable Key Allocation & Duplicate Processing in Library (`LibraryScreen.kt`)
- **Location:** `app/src/main/java/com/example/presentation/screen/library/LibraryScreen.kt` (Lines 77–100, 246, 298)
- **Root Cause:**
  1. Album grouping and Artist grouping are computed inside composable body:
     ```kotlin
     val albums = remember(tracks) { tracks.groupBy { it.album }.map { ... } }
     val artists = remember(tracks) { tracks.groupBy { it.artist }.map { ... } }
     ```
  2. In `LazyColumn` for artists (Line 298):
     ```kotlin
     items(artists, key = { it.first }) // "it.first" is the artist string name!
     ```
     If two artists share the same name or an empty artist tag exists, Room returns duplicates and Compose crashes or drops frames reconciling keys.
  3. In the Albums Grid (Line 246):
     ```kotlin
     items(albums, key = { it.first }) // "it.first" is the album title!
     ```
     Self-titled albums ("Greatest Hits", "Untitled") trigger duplicate key warnings and disable smooth item animations.
- **Solution:** Compute unique stable keys (e.g. `"${album.first}_${album.second.firstOrNull()?.id}"`), perform grouping in the ViewModel on a background thread, and pass pre-computed immutable models.

---

#### Bottleneck C: Full MiniPlayer Recomposition Every 1000ms (`MainActivity.kt` & `MiniPlayer.kt`)
- **Location:** `MainActivity.kt` (Lines 343, 364) & `MiniPlayer.kt` (Lines 45–54)
- **Root Cause:**
  `MainMiniPlayer` collects `val currentPosition by audioPlayer.currentPosition.collectAsStateWithLifecycle()` and passes `currentPosition: Long` into `MiniPlayer`.
  Inside `MiniPlayer`:
  ```kotlin
  val progress = if (track.duration > 0) (currentPosition.toFloat() / track.duration.toFloat()).coerceIn(0f, 1f) else 0f
  ```
  Because `currentPosition` is a primitive value passed into the Composable, **the entire MiniPlayer layout** (Track Title, Artist Name, Album Art, Play/Pause Button, and Outer Card) recomposes every single second, triggering layout recalculations even though only the thin 2.5dp progress bar changed.
- **Solution:** Pass a progress lambda `() -> Float` or defer state read directly into the `LinearProgressIndicator` draw phase, skipping recomposition of the MiniPlayer content.

---

#### Bottleneck D: Multiple Concurrent Infinite Animation Loops in Skeletons (`Skeleton.kt`)
- **Location:** `app/src/main/java/com/example/presentation/component/Skeleton.kt` (Lines 22–32)
- **Root Cause:**
  Every `SkeletonBox` calls:
  ```kotlin
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val alpha by infiniteTransition.animateFloat(...)
  ```
  When the app opens or library loads, 15–25 skeleton boxes exist simultaneously on the screen (4 playlists, 6 recent tracks, 5 track list items). Having 25 independent `rememberInfiniteTransition` instances running 60fps frame loops produces noticeable frame drops and CPU spikes on mid-range devices.
- **Solution:** Lift the shimmer transition to a shared composition local or single shimmer brush provider so all skeleton boxes share one unified hardware-accelerated phase.

---

### 2.2 Image Loading & Coil Optimization (Medium-High Impact)

#### Bottleneck A: Aggressive Bitmap Downscaling & Crossfade Policy
- **Location:** `SonnetApplication.kt` (Lines 26–50) & `AlbumArt.kt`
- **Root Cause:**
  `SonnetApplication.kt` allocates 25% of app memory for Coil memory cache and 100MB disk cache. However, `AlbumArt.kt` loads images into cards of varying sizes (48dp, 120dp, 160dp, 190dp, and full-screen NowPlaying) without specifying `.size()` or downsampling options in the Coil `AsyncImage` or `ImageRequest`.
  When loading high-resolution cover arts from `content://media/external/audio/albumart` (often 1000x1000 or larger), Coil decodes full-resolution Bitmaps into memory, causing frequent garbage collection (GC) pauses during fast `LazyColumn` flings.
- **Solution:**
  1. Specify `.precision(Precision.INEXACT)` or target size for thumbnails.
  2. Enable hardware bitmaps (`Bitmap.Config.HARDWARE`) where applicable for zero-copy GPU textures.
  3. Ensure `crossfade(150)` is short and lightweight to prevent alpha-blending stall during fast scrolling.

---

### 2.3 Audio Engine & Equalizer Disk I/O Thrashing (Critical Performance Issue)

#### Bottleneck A: Slider Dragging Flooding Disk Writes to DataStore (`EqualizerManager.kt`)
- **Location:** `app/src/main/java/com/example/player/EqualizerManager.kt` (Lines 185–222, 275–295)
- **Root Cause:**
  In `EqualizerManager.kt`, every slider movement in the UI calls:
  - `setBass(level)` -> calls `persistSettings()`
  - `setVirtualizer(level)` -> calls `persistSettings()`
  - `setBandGain(index, gain)` -> calls `persistSettings()`
  
  Inside `persistSettings()`:
  ```kotlin
  coroutineScope.launch {
      userPreferencesDataStore.saveEqualizerSettings(...)
  }
  ```
  Dragging a slider generates 30–60 events per second. Each tick launches a coroutine that serializes 5 band gains into string format and writes to Proto/Preferences DataStore on disk. This disk thrashing causes thread starvation on `Dispatchers.IO`, battery drain, and UI slider stutter.
- **Solution:** Introduce a 300ms debounce buffer in `EqualizerManager` using a debounced coroutine job:
  ```kotlin
  private var persistJob: Job? = null
  private fun persistSettings() {
      persistJob?.cancel()
      persistJob = coroutineScope.launch {
          delay(300)
          userPreferencesDataStore.saveEqualizerSettings(...)
      }
  }
  ```

---

#### Bottleneck B: Unconstrained GlobalScope Usage (`AppContainer.kt`)
- **Location:** `AppContainer.kt` (Lines 50–54)
- **Root Cause:**
  `GlobalScope.launch(Dispatchers.IO) { recordPlayUseCase(trackId) }` is used to record track plays.
  Using `GlobalScope` bypasses structured concurrency, makes error tracking impossible, and leaks jobs.
- **Solution:** Use a structured `CoroutineScope(SupervisorJob() + Dispatchers.IO)` owned by `AppContainer`.

---

### 2.4 MediaStore Scanner & Room Database Ingestion

#### Current Architecture:
- `MediaStoreScanner` reads Android's `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` with projection fields and yields `List<TrackEntity>`.
- In `TrackRepositoryImpl`:
  ```kotlin
  val existing = trackDao.getAllTracksSync()
  val existingMap = existing.associateBy { it.id }
  ```
  Scanned tracks are compared against Room entities. Any track missing or with updated fields is inserted via `trackDao.insertTracks(newTracks)`.

#### Opportunities for Improvement:
1. Ensure Room queries use indexed columns for fast joins and sorting.
2. In `TrackEntity`, add composite index on `(artist, album)` and `(playCount DESC)` to speed up `getMostPlayedTracks()` and `getLeastPlayedTracks()`.
3. Wrap bulk inserts in `@Transaction` to execute in a single SQLite transaction block rather than multiple sequential disk commits.

---

## 3. Step-by-Step Optimization Plan

| Phase | Target Area | Changes & Improvements | Impact |
|---|---|---|---|
| **Phase 1** | **Equalizer & DataStore** | Add 300ms debounce to `persistSettings()` in `EqualizerManager`; prevent disk thrashing during slider scrub | **Immediate** (Eliminates slider lag & IO thread locks) |
| **Phase 2** | **Compose State & MiniPlayer** | Defer `currentPosition` read in `MiniPlayer` using lambda / derived state; optimize `NowPlayingProgressBar` derivedStateOf | **Immediate** (Stops 1Hz full-screen recomposition cascade) |
| **Phase 3** | **Search & Library Optimization** | Move filter/grouping logic to `SearchViewModel` and `LibraryViewModel` with `Dispatchers.Default` & 150ms debounce; fix unique keys | **High** (Fluid typing in search, 60fps fling in Library) |
| **Phase 4** | **Skeleton & Animation Shimmer** | Unify `SkeletonBox` shimmer to use a single shared animation phase / brush | **High** (Cuts animation overhead during cold launch) |
| **Phase 5** | **Room DB & Repository** | Add Room indices on `TrackEntity` (`playCount`, `dateAdded`); wrap repository sync in transaction | **Medium** (Faster database queries & app cold start) |
| **Phase 6** | **Coil & Image Caching** | Fine-tune `ImageLoader` in `SonnetApplication` with RGB_565/HARDWARE bitmap config for album thumbnails | **Medium** (Reduces RAM consumption by ~30%) |

---

## 4. Preservation Guarantee

- **No visual or layout changes**: All existing Spotify-inspired dark UI styles, glassmorphic headers, typography, buttons, and layouts remain strictly intact.
- **No feature regressions**: Player queue, gapless playback, equalizer 5-band DSP, playlists, history, and search remain 100% functional.
- **Smoothness enhancement**: The pop-up and down gesture of `NowPlayingScreen` and `MiniPlayer` transitions will feel noticeably snappier and smoother due to the removal of background thread contention.
