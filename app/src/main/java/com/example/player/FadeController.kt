package com.example.player

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages audio volume ramping, fade-in, fade-out, and cross-track transitions.
 * Ensures thread safety and crash resilience: whenever playback is paused, stopped,
 * or interrupted, volume is safely restored and state transitions gracefully.
 */
class FadeController(
    private val playerProvider: () -> ExoPlayer,
    private val scope: CoroutineScope
) {
    private var activeJob: Job? = null

    private val _isTransitioning = MutableStateFlow(false)
    val isTransitioning: StateFlow<Boolean> = _isTransitioning.asStateFlow()

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    /**
     * Safely applies volume on the main application looper required by ExoPlayer.
     * Guards against NaN, negative, out-of-bound values, and released player states.
     */
    private fun safeSetVolume(vol: Float) {
        val safeVol = if (vol.isNaN()) 1.0f else vol.coerceIn(0.0f, 1.0f)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                playerProvider().volume = safeVol
            } catch (t: Throwable) {
                // ExoPlayer may be released or uninitialized; silently catch
            }
        } else {
            mainHandler.post {
                try {
                    playerProvider().volume = safeVol
                } catch (t: Throwable) {
                    // ExoPlayer may be released or uninitialized; silently catch
                }
            }
        }
    }

    /**
     * Cancels any active fade or transition job and unconditionally restores
     * player volume to full 1.0f.
     */
    fun cancelAndRestore() {
        activeJob?.cancel()
        activeJob = null
        safeSetVolume(1.0f)
        _isTransitioning.value = false
    }

    /**
     * Starts a smooth fade-in from 0.0f to 1.0f over [durationMs].
     */
    fun startFadeIn(durationMs: Long) {
        cancelAndRestore()
        if (durationMs <= 100L) {
            safeSetVolume(1.0f)
            return
        }

        _isTransitioning.value = true
        safeSetVolume(0.0f)

        activeJob = scope.launch(Dispatchers.Main.immediate) {
            try {
                val startTime = System.currentTimeMillis()
                val totalMs = durationMs.toFloat()
                while (isActive) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val fraction = if (totalMs > 0f) (elapsed / totalMs).coerceIn(0f, 1f) else 1f
                    safeSetVolume(fraction)
                    if (fraction >= 1.0f) break
                    delay(25L)
                }
            } catch (e: CancellationException) {
                // Normal when cancelled by user action
            } catch (t: Throwable) {
                Log.e("FadeController", "Error during startFadeIn", t)
            } finally {
                withContext(NonCancellable) {
                    if (activeJob == coroutineContext[Job]) {
                        safeSetVolume(1.0f)
                        _isTransitioning.value = false
                        activeJob = null
                    }
                }
            }
        }
    }

    /**
     * Performs a smooth two-phase track transition:
     * Phase 1: Fade out current track to 0.0f over half duration.
     * Phase 2: Execute [onSwitch] (loads new track, prepares, starts playback).
     * Phase 3: Fade in next track from 0.0f to 1.0f over half duration.
     *
     * In case of any interruption or cancellation, volume is unconditionally
     * restored to 1.0f so playback is never muted.
     */
    fun transitionBetweenTracks(
        totalDurationMs: Long,
        onSwitch: suspend () -> Unit
    ) {
        cancelAndRestore()
        val safeTotal = totalDurationMs.coerceIn(400L, 12000L)
        val halfDuration = safeTotal / 2L

        _isTransitioning.value = true

        activeJob = scope.launch(Dispatchers.Main.immediate) {
            try {
                // Phase 1: Fade out
                val fadeOutStart = System.currentTimeMillis()
                val fadeOutTotal = halfDuration.toFloat()
                while (isActive) {
                    val elapsed = System.currentTimeMillis() - fadeOutStart
                    val fraction = if (fadeOutTotal > 0f) (elapsed / fadeOutTotal).coerceIn(0f, 1f) else 1f
                    safeSetVolume((1f - fraction).coerceIn(0f, 1f))
                    if (fraction >= 1.0f) break
                    delay(25L)
                }

                safeSetVolume(0.0f)

                // Phase 2: Switch track in player
                onSwitch()

                // Phase 3: Fade in
                val fadeInStart = System.currentTimeMillis()
                val fadeInTotal = halfDuration.toFloat()
                while (isActive) {
                    val elapsed = System.currentTimeMillis() - fadeInStart
                    val fraction = if (fadeInTotal > 0f) (elapsed / fadeInTotal).coerceIn(0f, 1f) else 1f
                    safeSetVolume(fraction.coerceIn(0f, 1f))
                    if (fraction >= 1.0f) break
                    delay(25L)
                }
            } catch (e: CancellationException) {
                // Normal when cancelled by user action
            } catch (t: Throwable) {
                Log.e("FadeController", "Error during transitionBetweenTracks", t)
            } finally {
                withContext(NonCancellable) {
                    if (activeJob == coroutineContext[Job]) {
                        safeSetVolume(1.0f)
                        _isTransitioning.value = false
                        activeJob = null
                    }
                }
            }
        }
    }
}
