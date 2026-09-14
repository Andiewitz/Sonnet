package com.example.presentation.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.presentation.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appContainer = (context.applicationContext as com.example.SonnetApplication).container
    val coroutineScope = rememberCoroutineScope()

    val userName by appContainer.userPreferencesDataStore.userName
        .collectAsStateWithLifecycle(initialValue = "Andrei")
    val isHighQualityAudio by appContainer.userPreferencesDataStore.isHighQualityAudio
        .collectAsStateWithLifecycle(initialValue = true)
    val isCrossfadeEnabled by appContainer.userPreferencesDataStore.crossfadeEnabled
        .collectAsStateWithLifecycle(initialValue = true)
    val crossfadeDurationSeconds by appContainer.userPreferencesDataStore.crossfadeDurationSeconds
        .collectAsStateWithLifecycle(initialValue = 3)
    val isGaplessPlayback by appContainer.userPreferencesDataStore.isGaplessPlayback
        .collectAsStateWithLifecycle(initialValue = true)
    val isNormalizeVolume by appContainer.userPreferencesDataStore.isNormalizeVolume
        .collectAsStateWithLifecycle(initialValue = false)

    var showEqualizer by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var isScanningMedia by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BgPrimary,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // Screen Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Preferences, audio engine, and profile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Section 1: Profile & Account
            item {
                SettingsSectionHeader(title = "Profile")
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEditNameDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar with Initial
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(AccentPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AccentPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap to change display name",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        FilledTonalButton(
                            onClick = { showEditNameDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = BgTertiary,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit Name",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Section 2: Audio & Playback
            item {
                SettingsSectionHeader(title = "Audio & Playback")
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Equalizer Row
                        SettingsToggleRowInsideCard(
                            customIcon = {
                                com.example.presentation.component.EqualizerIcon(
                                    tint = AccentPrimary,
                                    size = 20.dp
                                )
                            },
                            title = "Equalizer",
                            subtitle = "Adjust audio frequencies & sound profiles",
                            onClick = { showEqualizer = true }
                        )

                        SettingsCardDivider()

                        // High Quality Audio Switch
                        SettingsSwitchRowInsideCard(
                            icon = Icons.Outlined.Tune,
                            title = "High Quality Audio",
                            subtitle = "Stream and playback at highest fidelity",
                            checked = isHighQualityAudio,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    appContainer.userPreferencesDataStore.setHighQualityAudio(checked)
                                }
                            }
                        )

                        SettingsCardDivider()

                        // Song Transition (Fade In & Out)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(BgTertiary, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.GraphicEq,
                                        contentDescription = "Song Transition",
                                        tint = if (isCrossfadeEnabled) AccentPrimary else TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Song Transition (Fade In & Out)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isCrossfadeEnabled) {
                                            "${crossfadeDurationSeconds}s transition • Fade out, then fade in"
                                        } else {
                                            "Off • Gapless transitions"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = isCrossfadeEnabled,
                                    onCheckedChange = { checked ->
                                        coroutineScope.launch {
                                            appContainer.userPreferencesDataStore.setCrossfadeEnabled(checked)
                                            appContainer.audioPlayer.setCrossfadeEnabled(checked)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AccentPrimary,
                                        uncheckedThumbColor = TextTertiary,
                                        uncheckedTrackColor = BgTertiary
                                    )
                                )
                            }

                            AnimatedVisibility(
                                visible = isCrossfadeEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                ) {
                                    HorizontalDivider(
                                        color = BorderSubtle.copy(alpha = 0.5f),
                                        thickness = 1.dp
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Transition Duration",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = TextSecondary
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AccentPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${crossfadeDurationSeconds} seconds",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentPrimary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Slider(
                                        value = crossfadeDurationSeconds.toFloat(),
                                        onValueChange = { newValue ->
                                            val rounded = newValue.roundToInt().coerceIn(1, 12)
                                            if (rounded != crossfadeDurationSeconds) {
                                                coroutineScope.launch {
                                                    appContainer.userPreferencesDataStore.setCrossfadeDurationSeconds(rounded)
                                                    appContainer.audioPlayer.setCrossfadeDurationSeconds(rounded)
                                                }
                                            }
                                        },
                                        valueRange = 1f..12f,
                                        steps = 10,
                                        colors = SliderDefaults.colors(
                                            thumbColor = AccentPrimary,
                                            activeTrackColor = AccentPrimary,
                                            inactiveTrackColor = BorderSubtle
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        listOf(1, 2, 3, 4, 6, 8).forEach { presetSec ->
                                            val isSelected = crossfadeDurationSeconds == presetSec
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) AccentPrimary else BgTertiary,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelected) AccentPrimary else BorderSubtle
                                                ),
                                                modifier = Modifier
                                                    .clickable {
                                                        coroutineScope.launch {
                                                            appContainer.userPreferencesDataStore.setCrossfadeDurationSeconds(presetSec)
                                                            appContainer.audioPlayer.setCrossfadeDurationSeconds(presetSec)
                                                        }
                                                    }
                                            ) {
                                                Text(
                                                    text = "${presetSec}s",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color.White else TextSecondary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SettingsCardDivider()

                        // Gapless Playback Switch
                        SettingsSwitchRowInsideCard(
                            icon = Icons.Outlined.MusicNote,
                            title = "Gapless Playback",
                            subtitle = "Seamlessly transition between album tracks",
                            checked = isGaplessPlayback,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    appContainer.userPreferencesDataStore.setGaplessPlayback(checked)
                                    appContainer.audioPlayer.setGaplessPlayback(checked)
                                }
                            }
                        )

                        SettingsCardDivider()

                        // Normalize Volume Switch
                        SettingsSwitchRowInsideCard(
                            icon = Icons.AutoMirrored.Outlined.VolumeUp,
                            title = "Normalize Volume",
                            subtitle = "Keep constant loudness across different tracks",
                            checked = isNormalizeVolume,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    appContainer.userPreferencesDataStore.setNormalizeVolume(checked)
                                    appContainer.audioPlayer.setNormalizeVolume(checked)
                                }
                            }
                        )
                    }
                }
            }

            // Section 3: Library & Storage (Auto-Scanning + Backup Scan Button)
            item {
                SettingsSectionHeader(title = "Library & Storage")
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Auto-Scan Information Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(BgTertiary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Automatic Library Sync",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Songs are automatically detected on startup",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AccentPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        SettingsCardDivider()

                        // Manual Backup Scan Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isScanningMedia) {
                                    coroutineScope.launch {
                                        isScanningMedia = true
                                        try {
                                            android.widget.Toast.makeText(context, "Scanning device storage for songs...", android.widget.Toast.LENGTH_SHORT).show()
                                            withContext(Dispatchers.IO) {
                                                appContainer.trackRepository.scanDeviceForTracks()
                                            }
                                            android.widget.Toast.makeText(context, "Library updated successfully", android.widget.Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Scan failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isScanningMedia = false
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(BgTertiary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isScanningMedia) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = AccentPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Manual Rescan",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Manual Rescan (Backup)",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isScanningMedia) "Scanning files now..." else "Scan now if newly downloaded files are missing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Section 4: About & System
            item {
                SettingsSectionHeader(title = "About")
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsInfoRow(
                            icon = Icons.Outlined.Info,
                            title = "Sonnet Music",
                            subtitle = "Version 1.0.0 (Sophisticated Dark Edition)"
                        )
                        SettingsCardDivider()
                        SettingsInfoRow(
                            icon = Icons.Outlined.Storage,
                            title = "Database Engine",
                            subtitle = "Room SQLite Local Cache"
                        )
                    }
                }
            }
        }
    }

    // Change Name Dialog
    if (showEditNameDialog) {
        var newNameInput by remember { mutableStateOf(userName) }

        Dialog(onDismissRequest = { showEditNameDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Edit Profile Name",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter the name you'd like displayed on the Home screen greeting.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { newNameInput = it },
                        singleLine = true,
                        placeholder = { Text("Your name", color = TextTertiary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = BgTertiary,
                            unfocusedContainerColor = BgTertiary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showEditNameDialog = false },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmed = newNameInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    coroutineScope.launch {
                                        appContainer.userPreferencesDataStore.setUserName(trimmed)
                                        showEditNameDialog = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentPrimary,
                                contentColor = TextOnAccent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showEqualizer) {
        com.example.presentation.component.EqualizerBottomSheet(
            equalizerManager = appContainer.audioPlayer.equalizerManager,
            onDismiss = { showEqualizer = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = subheaderFontFamily,
            fontWeight = FontWeight.Bold
        ),
        color = AccentPrimary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCardDivider() {
    HorizontalDivider(
        color = BorderSubtle.copy(alpha = 0.4f),
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SettingsToggleRowInsideCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    customIcon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(BgTertiary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (customIcon != null) {
                customIcon()
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SettingsSwitchRowInsideCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(BgTertiary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextOnAccent,
                checkedTrackColor = AccentPrimary,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = BgTertiary,
                uncheckedBorderColor = BorderSubtle
            )
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(BgTertiary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
