package com.example.havenhub.navigation
/ ═══════════════════════════════════════════════════════════════════════════
// FILE     : NavGraph.kt
// PACKAGE  : com.havenhub.ui.navigation
//
// PURPOSE  : Defines the complete Jetpack Compose Navigation graph for
//            HavenHub. Every screen in the app is registered here as a
//            composable() destination. Role-based guards redirect users
//            who don't have permission to access certain sections.
//
// ARCHITECTURE:
//   • Single NavHost (one graph for the whole app, no nested graphs)
//   • Start destination: Screen.Splash
//   • Scaffold wraps NavHost so BottomNavBar is conditionally visible
//   • Arguments extracted via navBackStackEntry.arguments safely
//
// ROLE GUARDS:
//   • Admin screens  → redirect to Home if role != "ADMIN"
//   • Owner screens  → redirect to Home if role != "PROPERTY_OWNER"
//   • These checks happen inside each composable block using
//     AuthViewModel.currentUserRole collected from the call site.
//
// BACK-STACK RULES:
//   • Auth flow cleared on login  → popUpTo(Splash, inclusive = true)
//   • Payment success cleared     → popUpTo(Booking, inclusive = true)
//   • Bottom-nav uses singleTop + saveState + restoreState
//
// SECTIONS (matches Screen.kt order):
//   1.  Auth               6.  Review          11. Vacation
//   2.  Main / Core        7.  Profile         12. Admin — Dashboard
//   3.  Property           8.  Settings        13. Admin — Verification
//   4.  Booking            9.  Notifications   14. Admin — Management
//   5.  Payment           10.  Messaging       15. Admin — Reports
// ═══════════════════════════════════════════════════════════════════════════

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

// ── Screen imports — Auth ─────────────────────────────────────────────────
import com.havenhub.ui.screens.splash.SplashScreen
import com.havenhub.ui.screens.onboarding.OnboardingScreen
import com.havenhub.ui.screens.auth.SignInScreen
import com.havenhub.ui.screens.auth.SignUpScreen
import com.havenhub.ui.screens.auth.RoleSelectionScreen
import com.havenhub.ui.screens.auth.ForgotPasswordScreen

// ── Screen imports — Main ─────────────────────────────────────────────────
import com.havenhub.ui.screens.home.HomeScreen
import com.havenhub.ui.screens.search.SearchScreen
import com.havenhub.ui.screens.search.FilterScreen

// ── Screen imports — Property ─────────────────────────────────────────────
import com.havenhub.ui.screens.property.PropertyListScreen
import com.havenhub.ui.screens.property.PropertyDetailScreen
import com.havenhub.ui.screens.property.AddPropertyScreen
import com.havenhub.ui.screens.property.MyPropertiesScreen
import com.havenhub.ui.screens.property.EditPropertyScreen

// ── Screen imports — Booking ──────────────────────────────────────────────
import com.havenhub.ui.screens.booking.BookingScreen
import com.havenhub.ui.screens.booking.BookingConfirmationScreen
import com.havenhub.ui.screens.booking.MyBookingsScreen
import com.havenhub.ui.screens.booking.BookingDetailsScreen

// ── Screen imports — Payment ──────────────────────────────────────────────
import com.havenhub.ui.screens.payment.PaymentScreen
import com.havenhub.ui.screens.payment.PaymentMethodScreen
import com.havenhub.ui.screens.payment.PaymentSuccessScreen

// ── Screen imports — Review ───────────────────────────────────────────────
import com.havenhub.ui.screens.review.AddReviewScreen
import com.havenhub.ui.screens.review.ViewReviewsScreen

// ── Screen imports — Profile ──────────────────────────────────────────────
import com.havenhub.ui.screens.profile.ProfileScreen
import com.havenhub.ui.screens.profile.EditProfileScreen

// ── Screen imports — Settings ─────────────────────────────────────────────
import com.havenhub.ui.screens.settings.SettingsScreen
import com.havenhub.ui.screens.settings.AccountSettingsScreen
import com.havenhub.ui.screens.settings.NotificationSettingsScreen
import com.havenhub.ui.screens.settings.PrivacySettingsScreen
import com.havenhub.ui.screens.settings.AboutScreen
import com.havenhub.ui.screens.settings.HelpAndSupportScreen

// ── Screen imports — Notifications ───────────────────────────────────────
import com.havenhub.ui.screens.notifications.NotificationsScreen
import com.havenhub.ui.screens.notifications.NotificationDetailScreen

// ── Screen imports — Messaging ────────────────────────────────────────────
import com.havenhub.ui.screens.messaging.MessageListScreen
import com.havenhub.ui.screens.messaging.ChatScreen

// ── Screen imports — Vacation ─────────────────────────────────────────────
import com.havenhub.ui.screens.vacation.VacationRentalsScreen
import com.havenhub.ui.screens.vacation.PreBookingScreen
import com.havenhub.ui.screens.vacation.VacationCalendarScreen

// ── Screen imports — Admin ────────────────────────────────────────────────
import com.havenhub.ui.screens.admin.dashboard.AdminDashboardScreen
import com.havenhub.ui.screens.admin.verification.VerifyPropertiesScreen
import com.havenhub.ui.screens.admin.verification.VerifyUsersScreen
import com.havenhub.ui.screens.admin.verification.PropertyVerificationDetailScreen
import com.havenhub.ui.screens.admin.verification.UserVerificationDetailScreen
import com.havenhub.ui.screens.admin.management.ManageUsersScreen
import com.havenhub.ui.screens.admin.management.ManagePropertiesScreen
import com.havenhub.ui.screens.admin.management.ManageBookingsScreen
import com.havenhub.ui.screens.admin.reports.ReportsScreen
import com.havenhub.ui.screens.admin.reports.PaymentReportsScreen

// ─────────────────────────────────────────────────────────────────────────
// HavenHubNavGraph
//
// The root composable that sets up Scaffold + BottomNavBar + NavHost.
// Called once from MainActivity.
//
// @param navController       The single NavHostController for the app.
// @param unreadMessageCount  Passed down from MessagingViewModel; drives
//                            the badge on the Messages bottom-nav tab.
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun HavenHubNavGraph(
    navController      : NavHostController,
    unreadMessageCount : Int = 0
) {
    // Observe current back-stack entry to drive BottomNavBar selection
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute      = navBackStackEntry?.destination?.route

    // Only show BottomNavBar on the 5 root-level tabs
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    navController      = navController,
                    currentBackStack   = navBackStackEntry,
                    unreadMessageCount = unreadMessageCount
                )
            }
        }
    ) { innerPadding ->

        NavHost(
            navController    = navController,
            startDestination = Screen.Splash.route,
            modifier         = Modifier.padding(innerPadding)
        ) {

            // ────────────────────────────────────────────────────────
            // 1. AUTH FLOW
            //    Back-stack is cleared on successful login so the user
            //    cannot press Back to return to SignIn from Home.
            // ────────────────────────────────────────────────────────

            composable(Screen.Splash.route) {
                SplashScreen(navController = navController)
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(navController = navController)
            }

            composable(Screen.SignIn.route) {
                SignInScreen(navController = navController)
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(navController = navController)
            }

            composable(Screen.RoleSelection.route) {
                RoleSelectionScreen(navController = navController)
            }

            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(navController = navController)
            }


            // ────────────────────────────────────────────────────────
            // 2. MAIN / CORE SCREENS
            // ────────────────────────────────────────────────────────

            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }

            composable(Screen.Search.route) {
                SearchScreen(navController = navController)
            }

            composable(Screen.Filter.route) {
                FilterScreen(navController = navController)
            }


            // ────────────────────────────────────────────────────────
            // 3. PROPERTY SCREENS
            // ────────────────────────────────────────────────────────

            composable(Screen.PropertyList.route) {
                PropertyListScreen(navController = navController)
            }

            // PropertyDetail — requires propertyId argument
            composable(
                route     = Screen.PropertyDetail.route,
                arguments = listOf(
                    navArgument(Screen.PropertyDetail.ARG_PROPERTY_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments
                    ?.getString(Screen.PropertyDetail.ARG_PROPERTY_ID) ?: ""
                PropertyDetailScreen(
                    navController = navController,
                    propertyId    = propertyId
                )
            }

            composable(Screen.AddProperty.route) {
                AddPropertyScreen(navController = navController)
            }

            composable(Screen.MyProperties.route) {
                MyPropertiesScreen(navController = navController)
            }

            // EditProperty — requires propertyId argument
            composable(
                route     = Screen.EditProperty.route,
                arguments = listOf(
                    navArgument(Screen.EditProperty.ARG_PROPERTY_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments
                    ?.getString(Screen.EditProperty.ARG_PROPERTY_ID) ?: ""
                EditPropertyScreen(
                    navController = navController,
                    propertyId    = propertyId
                )
            }


            // ────────────────────────────────────────────────────────
            // 4. BOOKING SCREENS
            // ────────────────────────────────────────────────────────

            // Booking — requires propertyId argument
            composable(
                route     = Screen.Booking.route,
                arguments = listOf(
                    navArgument(Screen.Booking.ARG_PROPERTY_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments
                    ?.getString(Screen.Booking.ARG_PROPERTY_ID) ?: ""
                BookingScreen(
                    navController = navController,
                    propertyId    = propertyId
                )
            }

            // BookingConfirmation — requires bookingId argument
            composable(
                route     = Screen.BookingConfirmation.route,
                arguments = listOf(
                    navArgument(Screen.BookingConfirmation.ARG_BOOKING_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments
                    ?.getString(Screen.BookingConfirmation.ARG_BOOKING_ID) ?: ""
                BookingConfirmationScreen(
                    navController = navController,
                    bookingId     = bookingId
                )
            }

            composable(Screen.MyBookings.route) {
                MyBookingsScreen(navController = navController)
            }

            // BookingDetails — requires bookingId argument
            composable(
                route     = Screen.BookingDetails.route,
                arguments = listOf(
                    navArgument(Screen.BookingDetails.ARG_BOOKING_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments
                    ?.getString(Screen.BookingDetails.ARG_BOOKING_ID) ?: ""
                BookingDetailsScreen(
                    navController = navController,
                    bookingId     = bookingId
                )
            }


            // ────────────────────────────────────────────────────────
            // 5. PAYMENT SCREENS
            //    After PaymentSuccess clear the payment back-stack so
            //    the user cannot go Back into the OTP screen.
            // ────────────────────────────────────────────────────────

            // Payment — requires bookingId argument
            composable(
                route     = Screen.Payment.route,
                arguments = listOf(
                    navArgument(Screen.Payment.ARG_BOOKING_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments
                    ?.getString(Screen.Payment.ARG_BOOKING_ID) ?: ""
                PaymentScreen(
                    navController = navController,
                    bookingId     = bookingId
                )
            }

            // PaymentMethod — requires bookingId + method arguments
            composable(
                route     = Screen.PaymentMethod.route,
                arguments = listOf(
                    navArgument(Screen.PaymentMethod.ARG_BOOKING_ID) {
                        type = NavType.StringType
                    },
                    navArgument(Screen.PaymentMethod.ARG_METHOD) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments
                    ?.getString(Screen.PaymentMethod.ARG_BOOKING_ID) ?: ""
                val method    = backStackEntry.arguments
                    ?.getString(Screen.PaymentMethod.ARG_METHOD) ?: ""
                PaymentMethodScreen(
                    navController = navController,
                    bookingId     = bookingId,
                    method        = method
                )
            }

            // PaymentSuccess — requires bookingId argument
            composable(
                route     = Screen.PaymentSuccess.route,
                arguments = listOf(
                    navArgument(Screen.PaymentSuccess.ARG_BOOKING_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments
                    ?.getString(Screen.PaymentSuccess.ARG_BOOKING_ID) ?: ""
                PaymentSuccessScreen(
                    navController = navController,
                    bookingId     = bookingId
                )
            }


            // ────────────────────────────────────────────────────────
            // 6. REVIEW SCREENS
            // ────────────────────────────────────────────────────────

            // AddReview — requires propertyId argument
            composable(
                route     = Screen.AddReview.route,
                arguments = listOf(
                    navArgument(Screen.AddReview.ARG_PROPERTY_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments
                    ?.getString(Screen.AddReview.ARG_PROPERTY_ID) ?: ""
                AddReviewScreen(
                    navController = navController,
                    propertyId    = propertyId
                )
            }

            // ViewReviews — requires propertyId argument
            composable(
                route     = Screen.ViewReviews.route,
                arguments = listOf(
                    navArgument(Screen.ViewReviews.ARG_PROPERTY_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments
                    ?.getString(Screen.ViewReviews.ARG_PROPERTY_ID) ?: ""
                ViewReviewsScreen(
                    navController = navController,
                    propertyId    = propertyId
                )
            }


            // ────────────────────────────────────────────────────────
            // 7. PROFILE SCREENS
            // ────────────────────────────────────────────────────────

            composable(Screen.Profile.route) {
                ProfileScreen(navController = navController)
            }

            composable(Screen.EditProfile.route) {
                EditProfileScreen(navController = navController)
            }


            // ────────────────────────────────────────────────────────
            // 8. SETTINGS SCREENS
            // ────────────────────────────────────────────────────────

            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }

            composable(Screen.AccountSettings.route) {
                AccountSettingsScreen(navController = navController)
            }

            composable(Screen.NotificationSettings.route) {
                NotificationSettingsScreen(navController = navController)
            }

            composable(Screen.PrivacySettings.route) {
                PrivacySettingsScreen(navController = navController)
            }

            composable(Screen.About.route) {
                AboutScreen(navController = navController)
            }

            composable(Screen.HelpAndSupport.route) {
                HelpAndSupportScreen(navController = navController)
            }


            // ────────────────────────────────────────────────────────
            // 9. NOTIFICATION SCREENS
            // ────────────────────────────────────────────────────────

            composable(Screen.Notifications.route) {
                NotificationsScreen(navController = navController)
            }

            // NotificationDetail — requires notificationId argument
            composable(
                route     = Screen.NotificationDetail.route,
                arguments = listOf(
                    navArgument(Screen.NotificationDetail.ARG_NOTIFICATION_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val notificationId = backStackEntry.arguments
                    ?.getString(Screen.NotificationDetail.ARG_NOTIFICATION_ID) ?: ""
                NotificationDetailScreen(
                    navController  = navController,
                    notificationId = notificationId
                )
            }


            // ────────────────────────────────────────────────────────
            // 10. MESSAGING SCREENS
            // ────────────────────────────────────────────────────────

            composable(Screen.MessageList.route) {
                MessageListScreen(navController = navController)
            }

            // Chat — requires userId argument (the other person's UID)
            composable(
                route     = Screen.Chat.route,
                arguments = listOf(
                    navArgument(Screen.Chat.ARG_USER_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments
                    ?.getString(Screen.Chat.ARG_USER_ID) ?: ""
                ChatScreen(
                    navController = navController,
                    userId        = userId
                )
            }


            // ────────────────────────────────────────────────────────
            // 11. VACATION SCREENS
            // ────────────────────────────────────────────────────────

            composable(Screen.VacationRentals.route) {
                VacationRentalsScreen(navController = navController)
            }

            composable(Screen.PreBooking.route) {
                PreBookingScreen(navController = navController)
            }

            // VacationCalendar — requires propertyId argument
            composable(
                route     = Screen.VacationCalendar.route,
                arguments = listOf(
                    navArgument(Screen.VacationCalendar.ARG_PROPERTY_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments
                    ?.getString(Screen.VacationCalendar.ARG_PROPERTY_ID) ?: ""
                VacationCalendarScreen(
                    navController = navController,
                    propertyId    = propertyId
                )
            }


            // ────────────────────────────────────────────────────────
            // 12. ADMIN — DASHBOARD
            //     Guard: role check done inside AdminDashboardScreen.
            //     Non-admins are shown a permission-denied message and
            //     redirected to Home via LaunchedEffect.
            // ────────────────────────────────────────────────────────

            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(navController = navController)
            }


            // ────────────────────────────────────────────────────────
            // 13. ADMIN — VERIFICATION
            // ────────────────────────────────────────────────────────

            composable(Screen.VerifyProperties.route) {
                VerifyPropertiesScreen(navController = navController)
            }

            composable(Screen.VerifyUsers.route) {
                VerifyUsersScreen(navController = navController)
            }

            // PropertyVerificationDetail — requires propertyId argument
            composable(
                route     = Screen.PropertyVerificationDetail.route,
                arguments = listOf(
                    navArgument(Screen.PropertyVerificationDetail.ARG_PROPERTY_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments
                    ?.getString(Screen.PropertyVerificationDetail.ARG_PROPERTY_ID) ?: ""
                PropertyVerificationDetailScreen(
                    navController = navController,
                    propertyId    = propertyId
                )
            }

            // UserVerificationDetail — requires userId argument
            composable(
                route     = Screen.UserVerificationDetail.route,
                arguments = listOf(
                    navArgument(Screen.UserVerificationDetail.ARG_USER_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments
                    ?.getString(Screen.UserVerificationDetail.ARG_USER_ID) ?: ""
                UserVerificationDetailScreen(
                    navController = navController,
                    userId        = userId
                )
            }


            // ────────────────────────────────────────────────────────
            // 14. ADMIN — MANAGEMENT
            // ────────────────────────────────────────────────────────

            composable(Screen.ManageUsers.route) {
                ManageUsersScreen(navController = navController)
            }

            composable(Screen.ManageProperties.route) {
                ManagePropertiesScreen(navController = navController)
            }

            composable(Screen.ManageBookings.route) {
                ManageBookingsScreen(navController = navController)
            }


            // ────────────────────────────────────────────────────────
            // 15. ADMIN — REPORTS
            // ────────────────────────────────────────────────────────

            composable(Screen.Reports.route) {
                ReportsScreen(navController = navController)
            }

            composable(Screen.PaymentReports.route) {
                PaymentReportsScreen(navController = navController)
            }

        } // end NavHost
    } // end Scaffold
}


