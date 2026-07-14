package com.example.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.presentation.theme.AccentPrimary
import com.example.presentation.theme.BgSecondary
import com.example.presentation.theme.BorderSubtle
import com.example.presentation.theme.TextTertiary

@Composable
fun AppBottomNavigationBar(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSecondary)
    ) {
        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            val isHomeSelected = currentDestination?.hierarchy?.any { it.route == "home" } == true
            NavItem(
                modifier = Modifier.weight(1f),
                selected = isHomeSelected,
                onClick = { navController.navigate("home") },
                icon = if (isHomeSelected) Icons.Filled.Home else Icons.Outlined.Home,
                label = "HOME"
            )
            
            val isSearchSelected = currentDestination?.hierarchy?.any { it.route == "search" } == true
            NavItem(
                modifier = Modifier.weight(1f),
                selected = isSearchSelected,
                onClick = { navController.navigate("search") },
                icon = if (isSearchSelected) Icons.Filled.Search else Icons.Outlined.Search,
                label = "SEARCH"
            )
            
            val isLibrarySelected = currentDestination?.hierarchy?.any { it.route == "library" } == true
            NavItem(
                modifier = Modifier.weight(1f),
                selected = isLibrarySelected,
                onClick = { navController.navigate("library") },
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) AccentPrimary else TextTertiary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) AccentPrimary else TextTertiary
        )
    }
}
