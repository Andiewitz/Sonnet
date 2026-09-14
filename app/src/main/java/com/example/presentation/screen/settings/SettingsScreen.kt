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
    val isCrossfadeEnabled by appContainer.userPreferencesDataStore.crossfadeEnabled
        .collectAsStateWithLifecycle(initialValue = true)
    val crossfadeDurationSeconds by appContainer.userPreferencesDataStore.crossfadeDurationSeconds
        .collectAsStateWithLifecycle(initialValue = 3)
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
            // Header
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            // Profile
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
                        Box(
                            modifier = Modifier
                                .size(52.dp)
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
                                text = "Tap to change name",
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

            // Audio & Playback
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
                        // Equalizer
                        SettingsNavRow(
                            customIcon = {
                                com.example.presentation.component.EqualizerIcon(
                                    tint = AccentPrimary,
                                    size = 20.dp
                                )
                            },
                            title = "Equalizer",
                            subtitle = "Adjust audio frequencies & presets",
                            onClick = { showEqualizer = true }
                        )

                        SettingsCardDivider()

                        // Song Transition
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
                                        .background(BgTertiary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.GraphicEq,
                                        contentDescription = null,
                                        tint = if (isCrossfadeEnabled) AccentPrimary else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Song Transition (Fade)",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isCrossfadeEnabled) "${crossfadeDurationSeconds}s fade between tracks" else "Off",
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
                                        checkedThumbColor = TextOnAccent,
                                        checkedTrackColor = AccentPrimary,
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = BgTertiary,
                                        uncheckedBorderColor = BorderSubtle
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
                                        color = BorderSubtle.copy(alpha = 0.4f),
                                        thickness = 1.dp
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Fade Duration",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "${crossfadeDurationSeconds}s",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentPrimary
                                        )
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

                                    Spacer(modifier = Modifier.height(6.dp))

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

                        // Normalize Volume
                        SettingsSwitchRow(
                            icon = Icons.AutoMirrored.Outlined.VolumeUp,
                            title = "Normalize Volume",
                            subtitle = "Consistent loudness across songs",
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

            // Library
            item {
                SettingsSectionHeader(title = "Library")
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
                            .clickable(enabled = !isScanningMedia) {
                                coroutineScope.launch {
                                    isScanningMedia = true
                                    try {
                                        android.widget.Toast.makeText(context, "Scanning for songs...", android.widget.Toast.LENGTH_SHORT).show()
                                        withContext(Dispatchers.IO) {
                                            appContainer.trackRepository.scanDeviceForTracks()
                                        }
                                        android.widget.Toast.makeText(context, "Library scan complete", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Scan error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
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
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = AccentPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Scan",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scan for songs",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isScanningMedia) "Scanning files..." else "Search device storage for newly added songs",
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

            // About
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
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sonnet Music",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Version 1.0.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
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
                        text = "Edit Name",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { newNameInput = it },
                        singleLine = true,
                        placeholder = { Text("Name", color = TextTertiary) },
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
private fun SettingsNavRow(
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
private fun SettingsSwitchRow(
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
