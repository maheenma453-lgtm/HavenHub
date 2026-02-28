package com.example.havenhub.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.havenhub.R

data class BottomNavItem(
    val label: String,
    val route: String,
    val iconRes: Int
)

val bottomNavItems = listOf(
    BottomNavItem("Home",     Screen.Home.route,        R.drawable.ic_home),
    BottomNavItem("Search",   Screen.Search.route,      R.drawable.ic_search),
    BottomNavItem("Bookings", Screen.MyBookings.route,  R.drawable.ic_bookings),
    BottomNavItem("Messages", Screen.MessageList.route, R.drawable.ic_message),
    BottomNavItem("Profile",  Screen.Profile.route,     R.drawable.ic_profile)
)

@Composable
fun BottomNavBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}