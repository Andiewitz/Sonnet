package com.example.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * High-fidelity, unmistakably authentic Audio Equalizer / Mixing Console Fader Icon.
 * Features:
 * - 3 vertical equalizer slider channels
 * - Recessed slider channel guide tracks
 * - Proportional physical mixing fader caps/knobs at staggered band positions (Bass, Mid, Treble)
 * - Distinct horizontal center grip/indicator slot etched into each fader knob
 */
@Composable
fun EqualizerIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // 3 slider channels spaced horizontally
        val xPositions = listOf(w * 0.22f, w * 0.50f, w * 0.78f)
        // Staggered knob positions representing dynamic EQ bands:
        // Left slider (Bass): near top (32% from top)
        // Mid slider (Mids): near lower-middle (64% from top)
        // Right slider (Treble): at upper-middle (44% from top)
        val knobYPositions = listOf(h * 0.32f, h * 0.64f, h * 0.44f)

        val trackWidth = (w * 0.085f).coerceAtLeast(1.5f)
        val trackRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)

        val topMargin = h * 0.10f
        val bottomMargin = h * 0.90f
        val trackLength = bottomMargin - topMargin

        val knobWidth = w * 0.30f
        val knobHeight = h * 0.18f
        val knobCorner = CornerRadius(knobHeight * 0.28f, knobHeight * 0.28f)

        for (i in 0..2) {
            val cx = xPositions[i]
            val cy = knobYPositions[i]

            // 1. Channel slot / guide track (subtle background line)
            drawRoundRect(
                color = tint.copy(alpha = 0.35f),
                topLeft = Offset(cx - trackWidth / 2f, topMargin),
                size = Size(trackWidth, trackLength),
                cornerRadius = trackRadius
            )

            // 2. Active level illumination below the fader knob
            val activeTop = cy + knobHeight * 0.1f
            if (bottomMargin > activeTop) {
                drawRoundRect(
                    color = tint.copy(alpha = 0.85f),
                    topLeft = Offset(cx - trackWidth / 2f, activeTop),
                    size = Size(trackWidth, bottomMargin - activeTop),
                    cornerRadius = trackRadius
                )
            }

            // 3. Fader Knob / Slider Cap (prominent, solid mixing console thumb)
            drawRoundRect(
                color = tint,
                topLeft = Offset(cx - knobWidth / 2f, cy - knobHeight / 2f),
                size = Size(knobWidth, knobHeight),
                cornerRadius = knobCorner
            )

            // 4. Center Grip Notch / Etched Indicator line across the knob cap
            val notchWidth = knobWidth * 0.68f
            val notchHeight = (knobHeight * 0.18f).coerceAtLeast(1.2f)
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.65f),
                topLeft = Offset(cx - notchWidth / 2f, cy - notchHeight / 2f),
                size = Size(notchWidth, notchHeight),
                cornerRadius = CornerRadius(notchHeight / 2f, notchHeight / 2f)
            )
        }
    }
}
