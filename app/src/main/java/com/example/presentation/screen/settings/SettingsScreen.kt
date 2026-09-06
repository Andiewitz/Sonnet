package com.example.presentation.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.presentation.theme.*

@Composable
fun SettingsScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appContainer = (context.applicationContext as com.example.SonnetApplication).container
    var isGaplessPlayback by remember { mutableStateOf(true) }
    var isNormalizeVolume by remember { mutableStateOf(false) }
    var isHighQualityAudio by remember { mutableStateOf(true) }
    var showEqualizer by remember { mutableStateOf(false) }

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
                        text = "Customize your listening experience",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Audio & Playback section
            item {
                SettingsSectionHeader(title = "Audio & Playback")
            }

            item {
                SettingsToggleRow(
                    customIcon = {
                        com.example.presentation.component.EqualizerIcon(
                            tint = TextPrimary,
                            size = 20.dp
                        )
                    },
                    title = "Equalizer",
                    subtitle = "Adjust audio frequencies & sound profiles",
                    onClick = { showEqualizer = true }
                )
            }

            item {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Tune,
                    title = "High Quality Audio",
                    subtitle = "Stream and playback at highest fidelity",
                    checked = isHighQualityAudio,
                    onCheckedChange = { isHighQualityAudio = it }
                )
            }

            item {
                SettingsSwitchRow(
                    icon = Icons.Outlined.MusicNote,
                    title = "Gapless Playback",
                    subtitle = "Seamlessly transition between album tracks",
                    checked = isGaplessPlayback,
                    onCheckedChange = { isGaplessPlayback = it }
                )
            }

            item {
                SettingsSwitchRow(
                    icon = Icons.Outlined.VolumeUp,
                    title = "Normalize Volume",
                    subtitle = "Keep constant loudness across different tracks",
                    checked = isNormalizeVolume,
                    onCheckedChange = { isNormalizeVolume = it }
                )
            }

            // Library & Storage section
            item {
                SettingsSectionHeader(title = "Library & Storage")
            }

            item {
                SettingsToggleRow(
                    icon = Icons.Outlined.Folder,
                    title = "Music Folders",
                    subtitle = "Choose device folders to include in library",
                    onClick = { /* open folder picker */ }
                )
            }

            item {
                SettingsToggleRow(
                    icon = Icons.Outlined.Refresh,
                    title = "Rescan Media",
                    subtitle = "Scan device storage for new songs and albums",
                    onClick = { /* trigger scan */ }
                )
            }

            // About section
            item {
                SettingsSectionHeader(title = "About")
            }

            item {
                SettingsToggleRow(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    subtitle = "Sonnet v1.0.0 (Sophisticated Dark edition)",
                    onClick = {}
                )
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
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsToggleRow(
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
            .padding(horizontal = 20.dp, vertical = 14.dp),
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
            .padding(horizontal = 20.dp, vertical = 14.dp),
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
