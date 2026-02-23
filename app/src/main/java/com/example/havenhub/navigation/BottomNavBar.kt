package com.example.havenhub.navigation
// ═══════════════════════════════════════════════════════════════════════════
// FILE     : BottomNavBar.kt
// PACKAGE  : com.havenhub.ui.navigation
//
// PURPOSE  : Material3 NavigationBar shown at the bottom of the app.
//            Displays 5 tabs for the main sections of HavenHub.
//            Each tab has an icon, a label, and an unread-badge.
//
// TABS     :
//   1. Home       → Screen.Home
//   2. Search     → Screen.Search
//   3. Bookings   → Screen.MyBookings
//   4. Messages   → Screen.MessageList  (shows unread count badge)
//   5. Profile    → Screen.Profile
//
// HOW IT WORKS:
//   • BottomNavBar receives the current NavBackStackEntry so it can
//     highlight the correct tab based on the active route.
//   • Single-top navigation prevents duplicate screens on the back stack
//     when the user taps the same tab twice.
//   • saveState / restoreState preserve scroll positions when switching tabs.
//   • The bar is only visible on the 5 root-level screens (see NavGraph.kt
//     where Scaffold wraps child NavHost).
//
// USAGE (in NavGraph.kt / MainActivity):
//   val navBackStackEntry by navController.currentBackStackEntryAsState()
//   BottomNavBar(
//       navController      = navController,
//       currentBackStack   = navBackStackEntry,
//       unreadMessageCount = unreadCount   // from MessagingViewModel
//   )
// ═══════════════════════════════════════════════════════════════════════════

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.havenhub.ui.theme.BackgroundWhite
import com.havenhub.ui.theme.BorderGray
import com.havenhub.ui.theme.PrimaryBlue
import com.havenhub.ui.theme.TextSecondary

// ── Data model for a single tab item ─────────────────────────────────────

/**
 * Holds all display properties for a single bottom-nav tab.
 *
 * @param route         The [Screen] route this tab navigates to.
 * @param label         Short text shown below the icon.
 * @param selectedIcon  Filled icon when this tab is active.
 * @param unselectedIcon Outlined icon when this tab is inactive.
 */
data class BottomNavItem(
    val route          : String,
    val label          : String,
    val selectedIcon   : ImageVector,
    val unselectedIcon : ImageVector
)

// ── Tab definitions ───────────────────────────────────────────────────────

/**
 * The 5 tabs shown in [BottomNavBar].
 * Order here determines left-to-right display order.
 */
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

// ── Routes where BottomNavBar should be VISIBLE ───────────────────────────

/**
 * BottomNavBar is only shown when the current route is one of these.
 * All other screens (detail views, forms, admin, etc.) hide the bar.
 */
val bottomBarRoutes = setOf(
    Screen.Home.route,
    Screen.Search.route,
    Screen.MyBookings.route,
    Screen.MessageList.route,
    Screen.Profile.route
)

// ── Main composable ───────────────────────────────────────────────────────

/**
 * HavenHub's bottom navigation bar.
 *
 * @param navController       Used to perform navigation on tab tap.
 * @param currentBackStack    The current [NavBackStackEntry]; drives active-tab highlight.
 * @param unreadMessageCount  If > 0 a red badge is shown on the Messages tab.
 */
@Composable
fun BottomNavBar(
    navController      : NavController,
    currentBackStack   : NavBackStackEntry?,
    unreadMessageCount : Int = 0
) {
    // Current destination route (may be null before first compose)
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
                        // Pop to the start destination to avoid large back stacks
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Prevent multiple copies of the same destination
                        launchSingleTop = true
                        // Restore scroll/list state when re-selecting a tab
                        restoreState = true
                    }
                },
                icon = {
                    // Messages tab gets an unread count badge
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


