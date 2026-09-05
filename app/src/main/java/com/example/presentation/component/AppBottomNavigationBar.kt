package com.example.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.presentation.theme.*

@Composable
fun AppBottomNavigationBar(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSecondary)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val isHomeSelected = currentDestination?.route == "home"
            NavItem(
                modifier = Modifier.weight(1f),
                selected = isHomeSelected,
                onClick = { navigateToTab("home") },
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                label = "Home"
            )

            val isSearchSelected = currentDestination?.route == "search"
            NavItem(
                modifier = Modifier.weight(1f),
                selected = isSearchSelected,
                onClick = { navigateToTab("search") },
                selectedIcon = Icons.Filled.Search,
                unselectedIcon = Icons.Outlined.Search,
                label = "Search"
            )

            val isLibrarySelected = currentDestination?.route == "library"
            NavItem(
                modifier = Modifier.weight(1f),
                selected = isLibrarySelected,
                onClick = { navigateToTab("library") },
                selectedIcon = Icons.Filled.LibraryMusic,
                unselectedIcon = Icons.Outlined.LibraryMusic,
                label = "Library"
            )

            val isSettingsSelected = currentDestination?.route == "settings"
            NavItem(
                modifier = Modifier.weight(1f),
                selected = isSettingsSelected,
                onClick = { navigateToTab("settings") },
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                label = "Settings"
            )
        }
    }
}

@Composable
private fun NavItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String
) {
    val interactionSource = remember { MutableInteractionSource() }

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "nav_item_scale"
    )

    val pillWidth by animateDpAsState(
        targetValue = if (selected) 48.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "nav_pill_width"
    )

    val pillColor by animateColorAsState(
        targetValue = if (selected) AccentPrimary.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(200),
        label = "nav_pill_color"
    )

    val labelColor by animateColorAsState(
        targetValue = if (selected) AccentPrimary else TextSecondary,
        animationSpec = tween(200),
        label = "nav_label_color"
    )

    val iconColor by animateColorAsState(
        targetValue = if (selected) AccentPrimary else TextTertiary,
        animationSpec = tween(200),
        label = "nav_icon_color"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 32.dp),
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with animated pill highlight backing
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(if (pillWidth > 0.dp) pillWidth else 32.dp)
                .clip(CircleShape)
                .background(pillColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = subheaderFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = labelColor
        )
    }
}

