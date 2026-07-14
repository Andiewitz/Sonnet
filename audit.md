# Animation Audit

This document tracks the current state of animations within the application, identifies areas where motion design is lacking, and proposes enhancements for a more fluid and premium user experience.

## Current State
- **Navigation Transitions**: 
  - Standard screens (Home, Library, Search) use horizontal slide and fade transitions.
  - The `NowPlaying` screen uses a vertical slide up/down transition to simulate a modal sheet opening.
- **Loading States**: 
  - Shimmer/pulse animations are implemented via the `SkeletonBox` component for loading placeholders across the app.

## Identified Gaps & Missing Animations

1. **MiniPlayer Entrance/Exit**
   - **Issue**: The `MiniPlayer` appears abruptly when a track starts playing.
   - **Fix**: Add an `AnimatedVisibility` wrapper with a vertical slide-up and fade-in transition when the player becomes active, and slide-down when dismissed or stopped.

2. **List Item Staggering**
   - **Issue**: Track lists, recent playlists, and recommended items populate instantly when data loads, which can feel jarring.
   - **Fix**: Implement staggered entrance animations for `LazyColumn` and `LazyRow` items so they cascade into view sequentially.

3. **Play/Pause Button Feedback**
   - **Issue**: The play/pause button state changes instantaneously.
   - **Fix**: Add a scale/bounce effect when the button is tapped, and ideally a crossfade or morphing animation between the Play and Pause icons.

4. **Shared Element Transitions (Album Art)**
   - **Issue**: Tapping a track or the MiniPlayer opens the NowPlaying screen, but the album art doesn't feel connected between the two screens.
   - **Fix**: While true shared element transitions are still experimental in Compose, we can simulate the effect or implement `SharedTransitionLayout` to make the album art seamlessly expand from the thumbnail to the full-screen view.

5. **Like/Favorite Interaction**
   - **Issue**: Tapping the heart icon is static.
   - **Fix**: Add a lively "spring" or "bounce" animation (e.g., scaling up to 1.3x and back) when a user likes a track, accompanied by a color crossfade.

6. **Track Change Crossfades**
   - **Issue**: Album art and background colors switch instantly when skipping tracks.
   - **Fix**: Implement `Crossfade` or `AnimatedContent` for the album artwork and the dynamically extracted background colors in the `NowPlaying` screen.

7. **Now Playing Progress Bar**
   - **Issue**: The progress bar thumb might jump depending on polling frequency.
   - **Fix**: Ensure the progress indicator value is smoothly animated (`animateFloatAsState`) between updates.
