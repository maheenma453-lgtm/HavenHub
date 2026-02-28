package com.example.havenhub.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument

// Ensure all your screen imports are exactly correct as per your folder structure
// Example: import com.example.havenhub.screens.auth.SignInScreen

@Composable
fun HavenHubNavGraph(
    navController: NavHostController,
    unreadMessageCount: Int = 0
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    navController = navController,
                    currentBackStack = navBackStackEntry,
                    unreadMessageCount = unreadMessageCount
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. AUTH
            composable(Screen.Splash.route) { SplashScreen(navController) }
            composable(Screen.Onboarding.route) { OnboardingScreen(navController) }
            composable(Screen.SignIn.route) { SignInScreen(navController) }
            composable(Screen.SignUp.route) { SignUpScreen(navController) }
            composable(Screen.RoleSelection.route) { RoleSelectionScreen(navController) }
            composable(Screen.ForgotPassword.route) { ForgotPasswordScreen(navController) }

            // 2. MAIN
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Search.route) { SearchScreen(navController) }
            composable(Screen.Filter.route) { FilterScreen(navController) }

            // 3. PROPERTY
            composable(Screen.PropertyList.route) { PropertyListScreen(navController) }
            composable(Screen.AddProperty.route) { AddPropertyScreen(navController) }
            composable(Screen.MyProperties.route) { MyPropertiesScreen(navController) }

            composable(
                route = Screen.PropertyDetail.route,
                arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
            ) { entry ->
                PropertyDetailScreen(navController, entry.arguments?.getString("propertyId") ?: "")
            }

            // 4. BOOKING
            composable(Screen.MyBookings.route) { MyBookingsScreen(navController) }
            composable(
                route = Screen.Booking.route,
                arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
            ) { entry ->
                BookingScreen(navController, entry.arguments?.getString("propertyId") ?: "")
            }

            // 5. MESSAGING (Common error source)
            composable(Screen.MessageList.route) { MessageListScreen(navController) }
            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { entry ->
                ChatScreen(navController, entry.arguments?.getString("userId") ?: "")
            }

            // 6. ADMIN SECTIONS (Often missed)
            composable(Screen.AdminDashboard.route) { AdminDashboardScreen(navController) }
            composable(Screen.VerifyProperties.route) { VerifyPropertiesScreen(navController) }

            composable(
                route = Screen.PropertyVerificationDetail.route,
                arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
            ) { entry ->
                PropertyVerificationDetailScreen(navController, entry.arguments?.getString("propertyId") ?: "")
            }

            // 7. PROFILE & SETTINGS
            composable(Screen.Profile.route) { ProfileScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
            composable(Screen.About.route) { AboutScreen(navController) }
        }
    }
}