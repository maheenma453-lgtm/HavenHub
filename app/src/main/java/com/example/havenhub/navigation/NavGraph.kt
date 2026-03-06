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

// ✅ Sabhi screens ke sahi imports
import com.example.havenhub.screens.*

@Composable
fun HavenHubNavGraph(
    navController: NavHostController,
    unreadMessageCount: Int = 0
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Bottom Bar sirf in Main screens par dikhegi
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
            startDestination = Screen.Splash.route, // ✅ Start hamesha Splash se
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── 1. AUTH & INITIAL FLOW (Sahi Sequence) ──

            // Step 1: Splash
            composable(Screen.Splash.route) {
                SplashScreen(navController)
            }

            // Step 2: Onboarding (New users ke liye)
            composable(Screen.Onboarding.route) {
                OnboardingScreen(navController)
            }

            // Step 3: Role Selection (Sign-Up se pehle lazmi hai)
            composable(Screen.RoleSelection.route) {
                RoleSelectionScreen(navController)
            }

            // Step 4: Login/Register
            composable(Screen.SignUp.route) { SignUpScreen(navController) }
            composable(Screen.SignIn.route) { SignInScreen(navController) }
            composable(Screen.ForgotPassword.route) { ForgotPasswordScreen(navController) }

            // ── 2. MAIN APP CORE ──
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Search.route) { SearchScreen(navController) }
            composable(Screen.Profile.route) { ProfileScreen(navController) }
            composable(Screen.MessageList.route) { MessageListScreen(navController) }

            // ── 3. BOOKINGS SECTION ──
            composable(Screen.MyBookings.route) {
                // ✅ FIX: UserId pass karna zaroori hai
                MyBookingsScreen(
                    navController = navController,
                    userId = "current_user_id" // Isse actual Firebase Auth ID se replace karein
                )
            }

            composable(
                route = Screen.BookingDetails.route,
                arguments = listOf(navArgument(Screen.BookingDetails.ARG_BOOKING_ID) { type = NavType.StringType })
            ) { b ->
                val id = b.arguments?.getString(Screen.BookingDetails.ARG_BOOKING_ID) ?: ""
                BookingDetailsScreen(navController = navController, bookingId = id)
            }

            // ── 4. CHAT SYSTEM (Corrected Parameters) ──
            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument(Screen.Chat.ARG_USER_ID) { type = NavType.StringType })
            ) { b ->
                val uId = b.arguments?.getString(Screen.Chat.ARG_USER_ID) ?: ""
                ChatScreen(
                    navController = navController,
                    userId = uId,
                    currentUserId = "my_id",
                    chatId = "active_chat_session"
                )
            }

            // ── 5. PROPERTY & PAYMENT ──
            composable(Screen.PaymentMethod.route) {
                PaymentMethodScreen(navController = navController)
            }
            composable(Screen.PropertyList.route) { PropertyListScreen(navController) }
            composable(
                route = Screen.PropertyDetail.route,
                arguments = listOf(navArgument(Screen.PropertyDetail.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { b ->
                val id = b.arguments?.getString(Screen.PropertyDetail.ARG_PROPERTY_ID) ?: ""
                PropertyDetailScreen(navController = navController, propertyId = id)
            }

            // ── 6. ADMIN PANEL ──
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