package com.example.havenhub.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookOnline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookOnline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
// FIX: com.havenhub.ui.theme → com.example.havenhub.ui.theme
import com.example.havenhub.ui.theme.BackgroundWhite
import com.example.havenhub.ui.theme.BorderGray
import com.example.havenhub.ui.theme.PrimaryBlue
import com.example.havenhub.ui.theme.TextSecondary

data class BottomNavItem(
    val route          : String,
    val label          : String,
    val selectedIcon   : ImageVector,
    val unselectedIcon : ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route          = Screen.Home.route,
        label          = "Home",
        selectedIcon   = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavItem(
        route          = Screen.Search.route,
        label          = "Search",
        selectedIcon   = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    ),
    BottomNavItem(
        route          = Screen.MyBookings.route,
        label          = "Bookings",
        selectedIcon   = Icons.Filled.BookOnline,
        unselectedIcon = Icons.Outlined.BookOnline
    ),
    BottomNavItem(
        route          = Screen.MessageList.route,
        label          = "Messages",
        selectedIcon   = Icons.Filled.Message,
        unselectedIcon = Icons.Outlined.Message
    ),
    BottomNavItem(
        route          = Screen.Profile.route,
        label          = "Profile",
        selectedIcon   = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
)

val bottomBarRoutes = setOf(
    Screen.Home.route,
    Screen.Search.route,
    Screen.MyBookings.route,
    Screen.MessageList.route,
    Screen.Profile.route
)

@Composable
fun BottomNavBar(
    navController      : NavController,
    currentBackStack   : NavBackStackEntry?,
    unreadMessageCount : Int = 0
) {
    val currentRoute = currentBackStack?.destination?.route

    NavigationBar(
        containerColor = BackgroundWhite,
        tonalElevation = 8.dp,
        modifier       = Modifier.height(64.dp)
    ) {
        bottomNavItems.forEach { item ->

            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick  = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                icon = {
                    if (item.route == Screen.MessageList.route && unreadMessageCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor   = BackgroundWhite
                                ) {
                                    Text(
                                        text     = if (unreadMessageCount > 9) "9+" else "$unreadMessageCount",
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector        = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier           = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector        = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            modifier           = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text       = item.label,
                        fontSize   = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = PrimaryBlue,
                    selectedTextColor   = PrimaryBlue,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor      = PrimaryBlue.copy(alpha = 0.1f)
                ),
                alwaysShowLabel = true
            )
        }
    }
}