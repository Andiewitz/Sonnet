package com.example.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.player.EqualizerManager
import com.example.player.EqualizerPreset
import com.example.presentation.theme.*

@Composable
fun EqualizerBottomSheet(
    equalizerManager: EqualizerManager,
    onDismiss: () -> Unit
) {
    val isEnabled by equalizerManager.isEnabled.collectAsStateWithLifecycle()
    val currentPreset by equalizerManager.currentPreset.collectAsStateWithLifecycle()
    val bassLevel by equalizerManager.bassLevel.collectAsStateWithLifecycle()
    val virtualizerLevel by equalizerManager.virtualizerLevel.collectAsStateWithLifecycle()
    val bandGains by equalizerManager.bandGains.collectAsStateWithLifecycle()

    val bandFrequencies = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(enabled = false) {}, // consume clicks inside sheet
                color = BgSecondary,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Grabber bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(BorderSubtle)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Header row with Title & Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(BgTertiary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                EqualizerIcon(
                                    tint = if (isEnabled) AccentPrimary else TextSecondary,
                                    size = 20.dp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Equalizer",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isEnabled) "Active sound enhancements" else "Audio bypass",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isEnabled) AccentPrimary else TextSecondary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { equalizerManager.setEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextOnAccent,
                                    checkedTrackColor = AccentPrimary,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = BgTertiary,
                                    uncheckedBorderColor = BorderSubtle
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(BgTertiary, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Presets horizontal pills
                    Text(
                        text = "PRESETS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(EqualizerPreset.values()) { preset ->
                            val isSelected = isEnabled && (currentPreset == preset)
                            val bg by animateColorAsState(
                                targetValue = if (isSelected) AccentPrimary else BgTertiary,
                                label = "preset_bg"
                            )
                            val textColor by animateColorAsState(
                                targetValue = if (isSelected) TextOnAccent else TextPrimary,
                                label = "preset_text"
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) AccentPrimary else BorderSubtle,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (!isEnabled) equalizerManager.setEnabled(true)
                                        equalizerManager.selectPreset(preset)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = preset.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = textColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sound Effects Sliders: Bass Boost & 3D Surround
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Bass Boost Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = BgTertiary,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Bass Boost",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "$bassLevel%",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = AccentPrimary
                                    )
                                }
                                Slider(
                                    value = bassLevel.toFloat(),
                                    onValueChange = {
                                        if (!isEnabled) equalizerManager.setEnabled(true)
                                        equalizerManager.setBass(it.toInt())
                                    },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentPrimary,
                                        activeTrackColor = AccentPrimary,
                                        inactiveTrackColor = BorderStrong
                                    )
                                )
                            }
                        }

                        // 3D Spatializer Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = BgTertiary,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "3D Surround",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "$virtualizerLevel%",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = AccentPrimary
                                    )
                                }
                                Slider(
                                    value = virtualizerLevel.toFloat(),
                                    onValueChange = {
                                        if (!isEnabled) equalizerManager.setEnabled(true)
                                        equalizerManager.setVirtualizer(it.toInt())
                                    },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentPrimary,
                                        activeTrackColor = AccentPrimary,
                                        inactiveTrackColor = BorderStrong
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 5-Band Equalizer Frequency Sliders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FREQUENCY BANDS (dB)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )

                        TextButton(
                            onClick = { equalizerManager.selectPreset(EqualizerPreset.FLAT) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset Flat",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = BgTertiary,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bandGains.forEachIndexed { index, gain ->
                                val label = bandFrequencies.getOrElse(index) { "Band $index" }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Gain readout
                                    val dbValue = (gain / 10f)
                                    val formattedDb = if (dbValue > 0) "+${dbValue.toInt()}dB" else "${dbValue.toInt()}dB"
                                    Text(
                                        text = formattedDb,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (gain != 0 && isEnabled) AccentPrimary else TextSecondary
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Vertical slider simulation with standard compose Slider or stepped increments
                                    Slider(
                                        value = gain.toFloat(),
                                        onValueChange = { newGain ->
                                            if (!isEnabled) equalizerManager.setEnabled(true)
                                            equalizerManager.setBandGain(index, newGain.toInt())
                                        },
                                        valueRange = -100f..100f,
                                        modifier = Modifier
                                            .height(110.dp)
                                            .graphicsLayer {
                                                rotationZ = 270f
                                            },
                                        colors = SliderDefaults.colors(
                                            thumbColor = AccentPrimary,
                                            activeTrackColor = AccentPrimary,
                                            inactiveTrackColor = BorderStrong
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
