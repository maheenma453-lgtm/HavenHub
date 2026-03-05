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

// ✅ Sabhi screens ke imports
import com.example.havenhub.screens.*

@Composable
fun HavenHubNavGraph(
    navController: NavHostController,
    unreadMessageCount: Int = 0
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Bottom Bar Logic
    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.MyBookings.route,
        Screen.MessageList.route,
        Screen.Profile.route
    )
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController, unreadMessageCount = unreadMessageCount)
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── 1. AUTH FLOW ──
            composable(Screen.Splash.route) { SplashScreen(navController) }
            composable(Screen.Onboarding.route) { OnboardingScreen(navController) }
            composable(Screen.SignIn.route) { SignInScreen(navController) }
            composable(Screen.SignUp.route) { SignUpScreen(navController) }
            composable(Screen.RoleSelection.route) { RoleSelectionScreen(navController) }
            composable(Screen.ForgotPassword.route) { ForgotPasswordScreen(navController) }

            // ── 2. MAIN CORE ──
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Search.route) { SearchScreen(navController) }
            composable(Screen.Profile.route) { ProfileScreen(navController) }
            composable(Screen.MessageList.route) { MessageListScreen(navController) }

            // ── 3. MY BOOKINGS (Fixed for your new code) ──
            composable(Screen.MyBookings.route) {
                // ✅ FIX: Aapki screen userId maang rahi hai.
                // Yahan aapko logged-in user ki ID deni hogi.
                MyBookingsScreen(
                    navController = navController,
                    userId = "current_logged_in_user_id" // Replace with actual Auth logic
                )
            }

            // ── 4. BOOKING DETAILS ──
            composable(
                route = Screen.BookingDetails.route,
                arguments = listOf(navArgument(Screen.BookingDetails.ARG_BOOKING_ID) { type = NavType.StringType })
            ) { b ->
                val id = b.arguments?.getString(Screen.BookingDetails.ARG_BOOKING_ID) ?: ""
                BookingDetailsScreen(navController = navController, bookingId = id)
            }

            // ── 5. CHAT (Fixed with Named Arguments) ──
            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument(Screen.Chat.ARG_USER_ID) { type = NavType.StringType })
            ) { b ->
                val uId = b.arguments?.getString(Screen.Chat.ARG_USER_ID) ?: ""
                ChatScreen(
                    navController = navController,
                    userId = uId,
                    currentUserId = "user_me",
                    chatId = "active_chat"
                )
            }

            // ── 6. PAYMENT ──
            composable(Screen.PaymentMethod.route) {
                PaymentMethodScreen(navController = navController)
            }

            // ── 7. PROPERTY & OTHERS ──
            composable(Screen.PropertyList.route) { PropertyListScreen(navController) }
            composable(
                route = Screen.PropertyDetail.route,
                arguments = listOf(navArgument(Screen.PropertyDetail.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { b ->
                val id = b.arguments?.getString(Screen.PropertyDetail.ARG_PROPERTY_ID) ?: ""
                PropertyDetailScreen(navController = navController, propertyId = id)
            }

            // ── 8. ADMIN SECTIONS ──
            composable(Screen.AdminDashboard.route) { AdminDashboardScreen(navController) }
            composable(
                route = Screen.UserVerificationDetail.route,
                arguments = listOf(navArgument(Screen.UserVerificationDetail.ARG_USER_ID) { type = NavType.StringType })
            ) { b ->
                val id = b.arguments?.getString(Screen.UserVerificationDetail.ARG_USER_ID) ?: ""
                UserVerificationDetailScreen(navController = navController, userId = id)
            }
        }
    }
}