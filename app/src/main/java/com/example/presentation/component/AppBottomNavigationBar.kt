package com.example.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.presentation.theme.AccentPrimary
import com.example.presentation.theme.BgSecondary
import com.example.presentation.theme.BorderSubtle
import com.example.presentation.theme.TextTertiary

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
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isHomeSelected = currentDestination?.route == "home"
            NavItem(
                modifier = Modifier.weight(1f),
                selected = isHomeSelected,
                onClick = { navigateToTab("home") },
                icon = if (isHomeSelected) Icons.Filled.Home else Icons.Outlined.Home,
                label = "HOME"
            )

            val isSearchSelected = currentDestination?.route == "search"
            NavItem(
                modifier = Modifier.weight(1f),
                selected = isSearchSelected,
                onClick = { navigateToTab("search") },
                icon = if (isSearchSelected) Icons.Filled.Search else Icons.Outlined.Search,
                label = "SEARCH"
            )

            val isLibrarySelected = currentDestination?.route == "library"
            NavItem(
                modifier = Modifier.weight(1f),
                selected = isLibrarySelected,
                onClick = { navigateToTab("library") },
                icon = if (isLibrarySelected) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                label = "LIBRARY"
            )
        }
    }
}

@Composable
private fun NavItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "nav_item_scale"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 28.dp),
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) AccentPrimary else TextTertiary,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) AccentPrimary else TextTertiary
        )
    }
}
