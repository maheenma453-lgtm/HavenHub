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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.havenhub.ui.theme.GoldAccent
import com.example.havenhub.ui.theme.PrimaryNavy

// ── Admin Bottom Navbar — Navy background, Gold active ───────────────────────
@Composable
fun AdminBottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = PrimaryNavy,          // ✅ Navy background
        tonalElevation = 0.dp,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(68.dp)
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
                icon = {
                    Icon(
                        imageVector        = icon,
                        contentDescription = label,
                        modifier           = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text       = label,
                        fontSize   = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = GoldAccent,
                    selectedTextColor   = GoldAccent,
                    unselectedIconColor = Color.White.copy(alpha = 0.45f),  // ✅ White muted
                    unselectedTextColor = Color.White.copy(alpha = 0.45f),
                    indicatorColor      = Color.White.copy(alpha = 0.10f)   // ✅ Subtle indicator
                ),
                alwaysShowLabel = true
            )
        }
    }
}

// ── Nav Item Data Class ───────────────────────────────────────────────────────
data class BottomNavItem(
    val route         : String,
    val label         : String,
    val selectedIcon  : ImageVector,
    val unselectedIcon: ImageVector
)

// ── Tenant Nav Items ──────────────────────────────────────────────────────────
val tenantNavItems = listOf(
    BottomNavItem(Screen.Home.route,        "Home",       Icons.Filled.Home,                 Icons.Outlined.Home),
    BottomNavItem(Screen.Search.route,      "Search",     Icons.Filled.Search,               Icons.Outlined.Search),
    BottomNavItem(Screen.MyBookings.route,  "Bookings",   Icons.Filled.CalendarMonth,        Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.MessageList.route, "Messages",   Icons.AutoMirrored.Filled.Message, Icons.AutoMirrored.Outlined.Message),
    BottomNavItem(Screen.Favourites.route,  "Favourites", Icons.Filled.Favorite,             Icons.Filled.FavoriteBorder)
)

// ── Landlord Nav Items ────────────────────────────────────────────────────────
val landlordNavItems = listOf(
    BottomNavItem(Screen.Home.route,         "Home",       Icons.Filled.Home,                 Icons.Outlined.Home),
    BottomNavItem(Screen.MyProperties.route, "Properties", Icons.Filled.Home,                 Icons.Outlined.Home),
    BottomNavItem(Screen.MyBookings.route,   "Bookings",   Icons.Filled.CalendarMonth,        Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.MessageList.route,  "Messages",   Icons.AutoMirrored.Filled.Message, Icons.AutoMirrored.Outlined.Message),
    BottomNavItem(Screen.Profile.route,      "Profile",    Icons.Filled.Person,               Icons.Outlined.Person)
)

val bottomNavItems = tenantNavItems

// ── User Bottom Navbar — Navy background, Gold active ────────────────────────
@Composable
fun BottomNavBar(
    navController     : NavController,
    unreadMessageCount: Int    = 0,
    userRole          : String = "tenant"
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = if (userRole == "landlord") landlordNavItems else tenantNavItems

    NavigationBar(
        containerColor = PrimaryNavy,          // ✅ Navy background
        tonalElevation = 0.dp,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(68.dp)
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick  = {
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
                                Badge(containerColor = GoldAccent) {
                                    Text(
                                        text     = if (unreadMessageCount > 9) "9+" else "$unreadMessageCount",
                                        fontSize = 9.sp,
                                        color    = PrimaryNavy
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector        = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier           = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector        = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            modifier           = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text       = item.label,
                        fontSize   = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = GoldAccent,
                    selectedTextColor   = GoldAccent,
                    unselectedIconColor = Color.White.copy(alpha = 0.45f),  // ✅ White muted
                    unselectedTextColor = Color.White.copy(alpha = 0.45f),
                    indicatorColor      = Color.White.copy(alpha = 0.10f)
                ),
                alwaysShowLabel = true
            )
        }
    }
}



























