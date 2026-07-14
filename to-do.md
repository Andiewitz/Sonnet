# Animation To-Do List

This is the actionable tracking list based on the `audit.md` findings. 

## High Priority
- [x] **MiniPlayer Transitions**: Wrap the `MiniPlayer` in `AnimatedVisibility` (slide in from bottom / slide out).
- [x] **Play/Pause Icon Scaling**: Add a scale animation (`animateFloatAsState` with a spring spec) to the main play/pause buttons on the `NowPlaying` screen and `MiniPlayer`.
- [x] **Track Art Crossfade**: Wrap the `AlbumArt` in `AnimatedContent` or `Crossfade` inside the `NowPlaying` screen so changing tracks feels seamless.

## Medium Priority
- [x] **Like Button Bounce**: Add a spring scale animation to the Favorite/Heart icon button.
- [x] **Progress Bar Smoothing**: Use `animateFloatAsState` for the `Slider` value in the `NowPlaying` screen to smooth out playback progress ticks.
- [x] **Dynamic Color Crossfade**: Animate the background color transitions in the `NowPlaying` screen when a new track loads and the extracted palette changes.

## Low Priority / Polish
- [x] **Staggered List Entrances**: Add initial load staggering to the `LazyColumn` and `LazyRow` items in the `HomeScreen` and `LibraryScreen`.
- [ ] **Shared Element Transitions**: Investigate using `SharedTransitionLayout` (Compose 1.7+) for seamless album art expansion from `MiniPlayer` to `NowPlayingScreen`.
- [ ] **Scrolling Parallax**: Add a slight parallax or scale effect to the album art when scrolling down the `NowPlaying` screen (if it becomes scrollable).
