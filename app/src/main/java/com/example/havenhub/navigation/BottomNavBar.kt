package com.example.havenhub.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.havenhub.ui.theme.BackgroundWhite
import com.example.havenhub.ui.theme.PrimaryBlue
import com.example.havenhub.ui.theme.TextSecondary

// ── Admin Bottom Navbar ───────────────────────────────────────────
@Composable
fun AdminBottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = BackgroundWhite,
        tonalElevation = 8.dp,
        modifier       = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val items = listOf(
            Triple(Screen.AdminDashboard.route,   "Dashboard", Icons.Filled.Dashboard),
            Triple(Screen.VerifyProperties.route, "Verify",    Icons.Filled.CheckCircle),
            Triple(Screen.ManageUsers.route,      "Users",     Icons.Filled.People),
            Triple(Screen.ManageBookings.route,   "Bookings",  Icons.Filled.CalendarMonth),
            Triple(Screen.Reports.route,          "Reports",   Icons.Filled.BarChart),
        )

        items.forEach { (route, label, icon) ->
            val isSelected = currentRoute == route
            NavigationBarItem(
                selected = isSelected,
                onClick  = {
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(Screen.AdminDashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                },
                icon   = {
                    Icon(
                        imageVector        = icon,
                        contentDescription = label,
                        modifier           = Modifier.size(24.dp)
                    )
                },
                label  = {
                    Text(
                        text       = label,
                        fontSize   = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = PrimaryBlue,
                    selectedTextColor   = PrimaryBlue,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor      = PrimaryBlue.copy(alpha = 0.1f)
                )
            )
        }
    }
}

// ── User Bottom Navbar ────────────────────────────────────────────
data class BottomNavItem(
    val route         : String,
    val label         : String,
    val selectedIcon  : ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route,        "Home",     Icons.Filled.Home,                 Icons.Outlined.Home),
    BottomNavItem(Screen.Search.route,      "Search",   Icons.Filled.Search,               Icons.Outlined.Search),
    BottomNavItem(Screen.MyBookings.route,  "Bookings", Icons.Filled.CalendarMonth,        Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.MessageList.route, "Messages", Icons.AutoMirrored.Filled.Message, Icons.AutoMirrored.Outlined.Message),
    BottomNavItem(Screen.Profile.route,     "Profile",  Icons.Filled.Person,               Icons.Outlined.Person)
)

@Composable
fun BottomNavBar(
    navController     : NavController,
    unreadMessageCount: Int = 0
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = BackgroundWhite,
        tonalElevation = 8.dp,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(64.dp)
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                },
                icon = {
                    if (item.route == Screen.MessageList.route && unreadMessageCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text(
                                        text     = if (unreadMessageCount > 9) "9+" else "$unreadMessageCount",
                                        fontSize = 9.sp,
                                        color    = BackgroundWhite
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
























