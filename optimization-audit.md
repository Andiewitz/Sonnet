# Performance Optimization Audit

This document identifies areas where the application suffers from sluggishness and drops frames, proposing zero-compromise fixes that maintain the current UI/UX while significantly boosting performance.

## Identified Bottlenecks

### 1. Expensive Rendering (`Modifier.blur`)
- **Location:** `NowPlayingScreen.kt` (Dynamic color background glow).
- **Issue:** `Modifier.blur(40.dp)` is extremely computationally expensive. On many Android devices, it forces software rendering or heavy GPU overdraw, causing severe jank, especially when combined with our animated color crossfades.
- **Fix:** Replace `Modifier.blur` with a `Brush.radialGradient()`. This achieves the exact same glowing orb aesthetic using standard gradient rendering, which is heavily optimized and hardware-accelerated.

### 2. Missing `key` in Lazy Lists
- **Location:** `HomeScreen.kt`, `LibraryScreen.kt`.
- **Issue:** Our `LazyColumn` and `LazyRow` components use `items` and `itemsIndexed` without providing a `key` parameter. When the list data changes or the parent recomposes, Compose destroys and recreates the list items because it cannot track their identities.
- **Fix:** Pass a unique identifier (e.g., `key = { it.id }` or `key = { _, item -> item.id }`) to every lazy list builder.

### 3. State Thrashing from Playback Progress
- **Location:** `NowPlayingScreen.kt`, `MiniPlayer.kt`, `MainActivity.kt`.
- **Issue:** We are reading `currentPosition` directly at the top level of the screen composables. Because this value updates constantly during playback (often multiple times a second), it forces the *entire screen* (including album art, background, and lists) to recompose on every tick.
- **Fix:** 
  - **Defer State Reading:** Pass `currentPosition` as a lambda `() -> Long` or wrap the Slider/Progress elements in a smaller, isolated Composable so *only* the progress bar recomposes.
  - **Use `derivedStateOf`:** Calculate the `sliderPosition` fraction inside a `derivedStateOf` block to prevent unnecessary downstream recalculations.

### 4. Unoptimized Staggered Animations
- **Location:** `HomeScreen.kt`, `LibraryScreen.kt`.
- **Issue:** The recently added staggered list entrances use `LaunchedEffect` with `delay` and a local `isVisible` state per item. In large lists, this creates hundreds of coroutines and `AnimatedVisibility` instances, causing immediate layout sluggishness when the screen opens.
- **Fix:** Remove the manual `LaunchedEffect` delay approach. Instead, upgrade to Compose 1.7's `Modifier.animateItem()` or use standard `Modifier.animateItemPlacement()`. If initial entrance is strictly required, manage a single shared animation state in the ViewModel rather than per-item coroutines.

### 5. `SubcomposeAsyncImage` Overhead
- **Location:** `AlbumArt.kt`.
- **Issue:** `SubcomposeAsyncImage` is significantly slower than `AsyncImage` because it requires subcomposition passes. We are using it just to show a fallback icon.
- **Fix:** Switch to `AsyncImage` and use a `placeholder` and `error` painter (or standard Coil `ImageRequest` configuration) for a much faster first-paint.

### 6. Heavy Object Reallocation in Recomposition
- **Location:** Various UI components (e.g., `AlbumArt.kt` fallback lambda, `Color` instantiations in `NowPlayingScreen`).
- **Issue:** We are instantiating new objects (like lambdas or complex UI configurations) on every recomposition pass without `remember`.
- **Fix:** Wrap expensive calculations or static lambdas in `remember { ... }` to cache them across recompositions.
