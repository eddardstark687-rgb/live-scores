package com.pitchpulse.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pitchpulse.ui.theme.AppAccent
import com.pitchpulse.ui.theme.AppSurface
import com.pitchpulse.ui.theme.TextMuted
import com.pitchpulse.ui.theme.TextSecondary

sealed class NavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : NavItem("home_screen", "Home", Icons.Default.Home)
    object Matches : NavItem("matches_screen", "Matches", Icons.AutoMirrored.Filled.List)
    object Search : NavItem("search_screen", "Search", Icons.Default.Search)
}

@Composable
fun BottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(NavItem.Home, NavItem.Matches, NavItem.Search)

    NavigationBar(
        containerColor = AppSurface,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppAccent,
                    selectedTextColor = AppAccent,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = AppSurface // No high-contrast indicator background
                )
            )
        }
    }
}


