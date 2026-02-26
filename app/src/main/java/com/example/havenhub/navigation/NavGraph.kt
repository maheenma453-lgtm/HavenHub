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

// ── Auth ──────────────────────────────────────────────────────────────────
import com.example.havenhub.screens.splash.SplashScreen
import com.example.havenhub.screens.onboarding.OnboardingScreen
import com.example.havenhub.screens.auth.SignInScreen
import com.example.havenhub.screens.auth.SignUpScreen
import com.example.havenhub.screens.auth.RoleSelectionScreen
import com.example.havenhub.screens.auth.ForgotPasswordScreen

// ── Main ──────────────────────────────────────────────────────────────────
import com.example.havenhub.screens.home.HomeScreen
import com.example.havenhub.screens.search.SearchScreen
import com.example.havenhub.screens.search.FilterScreen

// ── Property ──────────────────────────────────────────────────────────────
import com.example.havenhub.screens.property.PropertyListScreen
import com.example.havenhub.screens.property.PropertyDetailScreen
import com.example.havenhub.screens.property.AddPropertyScreen
import com.example.havenhub.screens.property.MyPropertiesScreen
import com.example.havenhub.screens.property.EditPropertyScreen

// ── Booking ───────────────────────────────────────────────────────────────
import com.example.havenhub.screens.booking.BookingScreen
import com.example.havenhub.screens.booking.BookingConfirmationScreen
import com.example.havenhub.screens.booking.MyBookingsScreen
import com.example.havenhub.screens.booking.BookingDetailsScreen

// ── Payment ───────────────────────────────────────────────────────────────
import com.example.havenhub.screens.payment.PaymentScreen
import com.example.havenhub.screens.payment.PaymentMethodScreen
import com.example.havenhub.screens.payment.PaymentSuccessScreen

// ── Review ────────────────────────────────────────────────────────────────
import com.example.havenhub.screens.review.AddReviewScreen
import com.example.havenhub.screens.review.ViewReviewsScreen

// ── Profile ───────────────────────────────────────────────────────────────
import com.example.havenhub.screens.profile.ProfileScreen
import com.example.havenhub.screens.profile.EditProfileScreen

// ── Settings ──────────────────────────────────────────────────────────────
import com.example.havenhub.screens.settings.SettingsScreen
import com.example.havenhub.screens.settings.AccountSettingsScreen
import com.example.havenhub.screens.settings.NotificationSettingsScreen
import com.example.havenhub.screens.settings.PrivacySettingsScreen
import com.example.havenhub.screens.settings.AboutScreen
import com.example.havenhub.screens.settings.HelpAndSupportScreen

// ── Notifications ─────────────────────────────────────────────────────────
import com.example.havenhub.screens.notifications.NotificationsScreen
import com.example.havenhub.screens.notifications.NotificationDetailScreen

// ── Messaging ─────────────────────────────────────────────────────────────
import com.example.havenhub.screens.messaging.MessageListScreen
import com.example.havenhub.screens.messaging.ChatScreen

// ── Vacation ──────────────────────────────────────────────────────────────
import com.example.havenhub.screens.vacation.VacationRentalsScreen
import com.example.havenhub.screens.vacation.PreBookingScreen
import com.example.havenhub.screens.vacation.VacationCalendarScreen

// ── Admin ─────────────────────────────────────────────────────────────────
import com.example.havenhub.screens.admin.dashboard.AdminDashboardScreen
import com.example.havenhub.screens.admin.verification.VerifyPropertiesScreen
import com.example.havenhub.screens.admin.verification.VerifyUsersScreen
import com.example.havenhub.screens.admin.verification.PropertyVerificationDetailScreen
import com.example.havenhub.screens.admin.verification.UserVerificationDetailScreen
import com.example.havenhub.screens.admin.management.ManageUsersScreen
import com.example.havenhub.screens.admin.management.ManagePropertiesScreen
import com.example.havenhub.screens.admin.management.ManageBookingsScreen
import com.example.havenhub.screens.admin.reports.ReportsScreen
import com.example.havenhub.screens.admin.reports.PaymentReportsScreen

@Composable
fun HavenHubNavGraph(
    navController      : NavHostController,
    unreadMessageCount : Int = 0
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute      = navBackStackEntry?.destination?.route
    val showBottomBar     = currentRoute in bottomBarRoutes

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

            // 1. AUTH
            composable(Screen.Splash.route)         { SplashScreen(navController = navController) }
            composable(Screen.Onboarding.route)     { OnboardingScreen(navController = navController) }
            composable(Screen.SignIn.route)          { SignInScreen(navController = navController) }
            composable(Screen.SignUp.route)          { SignUpScreen(navController = navController) }
            composable(Screen.RoleSelection.route)  { RoleSelectionScreen(navController = navController) }
            composable(Screen.ForgotPassword.route) { ForgotPasswordScreen(navController = navController) }

            // 2. MAIN
            composable(Screen.Home.route)   { HomeScreen(navController = navController) }
            composable(Screen.Search.route) { SearchScreen(navController = navController) }
            composable(Screen.Filter.route) { FilterScreen(navController = navController) }

            // 3. PROPERTY
            composable(Screen.PropertyList.route)  { PropertyListScreen(navController = navController) }
            composable(Screen.AddProperty.route)   { AddPropertyScreen(navController = navController) }
            composable(Screen.MyProperties.route)  { MyPropertiesScreen(navController = navController) }

            composable(
                route     = Screen.PropertyDetail.route,
                arguments = listOf(navArgument(Screen.PropertyDetail.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString(Screen.PropertyDetail.ARG_PROPERTY_ID) ?: ""
                PropertyDetailScreen(navController = navController, propertyId = propertyId)
            }

            composable(
                route     = Screen.EditProperty.route,
                arguments = listOf(navArgument(Screen.EditProperty.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString(Screen.EditProperty.ARG_PROPERTY_ID) ?: ""
                EditPropertyScreen(navController = navController, propertyId = propertyId)
            }

            // 4. BOOKING
            composable(Screen.MyBookings.route) { MyBookingsScreen(navController = navController) }

            composable(
                route     = Screen.Booking.route,
                arguments = listOf(navArgument(Screen.Booking.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString(Screen.Booking.ARG_PROPERTY_ID) ?: ""
                BookingScreen(navController = navController, propertyId = propertyId)
            }

            composable(
                route     = Screen.BookingConfirmation.route,
                arguments = listOf(navArgument(Screen.BookingConfirmation.ARG_BOOKING_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString(Screen.BookingConfirmation.ARG_BOOKING_ID) ?: ""
                BookingConfirmationScreen(navController = navController, bookingId = bookingId)
            }

            composable(
                route     = Screen.BookingDetails.route,
                arguments = listOf(navArgument(Screen.BookingDetails.ARG_BOOKING_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString(Screen.BookingDetails.ARG_BOOKING_ID) ?: ""
                BookingDetailsScreen(navController = navController, bookingId = bookingId)
            }

            // 5. PAYMENT
            composable(
                route     = Screen.Payment.route,
                arguments = listOf(navArgument(Screen.Payment.ARG_BOOKING_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString(Screen.Payment.ARG_BOOKING_ID) ?: ""
                PaymentScreen(navController = navController, bookingId = bookingId)
            }

            composable(
                route     = Screen.PaymentMethod.route,
                arguments = listOf(
                    navArgument(Screen.PaymentMethod.ARG_BOOKING_ID) { type = NavType.StringType },
                    navArgument(Screen.PaymentMethod.ARG_METHOD)     { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString(Screen.PaymentMethod.ARG_BOOKING_ID) ?: ""
                val method    = backStackEntry.arguments?.getString(Screen.PaymentMethod.ARG_METHOD) ?: ""
                PaymentMethodScreen(navController = navController, bookingId = bookingId, method = method)
            }

            composable(
                route     = Screen.PaymentSuccess.route,
                arguments = listOf(navArgument(Screen.PaymentSuccess.ARG_BOOKING_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString(Screen.PaymentSuccess.ARG_BOOKING_ID) ?: ""
                PaymentSuccessScreen(navController = navController, bookingId = bookingId)
            }

            // 6. REVIEW
            composable(
                route     = Screen.AddReview.route,
                arguments = listOf(navArgument(Screen.AddReview.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString(Screen.AddReview.ARG_PROPERTY_ID) ?: ""
                AddReviewScreen(navController = navController, propertyId = propertyId)
            }

            composable(
                route     = Screen.ViewReviews.route,
                arguments = listOf(navArgument(Screen.ViewReviews.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString(Screen.ViewReviews.ARG_PROPERTY_ID) ?: ""
                ViewReviewsScreen(navController = navController, propertyId = propertyId)
            }

            // 7. PROFILE
            composable(Screen.Profile.route)     { ProfileScreen(navController = navController) }
            composable(Screen.EditProfile.route) { EditProfileScreen(navController = navController) }

            // 8. SETTINGS
            composable(Screen.Settings.route)             { SettingsScreen(navController = navController) }
            composable(Screen.AccountSettings.route)      { AccountSettingsScreen(navController = navController) }
            composable(Screen.NotificationSettings.route) { NotificationSettingsScreen(navController = navController) }
            composable(Screen.PrivacySettings.route)      { PrivacySettingsScreen(navController = navController) }
            composable(Screen.About.route)                { AboutScreen(navController = navController) }
            composable(Screen.HelpAndSupport.route)       { HelpAndSupportScreen(navController = navController) }

            // 9. NOTIFICATIONS
            composable(Screen.Notifications.route) { NotificationsScreen(navController = navController) }

            composable(
                route     = Screen.NotificationDetail.route,
                arguments = listOf(navArgument(Screen.NotificationDetail.ARG_NOTIFICATION_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val notificationId = backStackEntry.arguments?.getString(Screen.NotificationDetail.ARG_NOTIFICATION_ID) ?: ""
                NotificationDetailScreen(navController = navController, notificationId = notificationId)
            }

            // 10. MESSAGING
            composable(Screen.MessageList.route) { MessageListScreen(navController = navController) }

            composable(
                route     = Screen.Chat.route,
                arguments = listOf(navArgument(Screen.Chat.ARG_USER_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString(Screen.Chat.ARG_USER_ID) ?: ""
                ChatScreen(navController = navController, userId = userId)
            }

            // 11. VACATION
            composable(Screen.VacationRentals.route) { VacationRentalsScreen(navController = navController) }
            composable(Screen.PreBooking.route)      { PreBookingScreen(navController = navController) }

            composable(
                route     = Screen.VacationCalendar.route,
                arguments = listOf(navArgument(Screen.VacationCalendar.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString(Screen.VacationCalendar.ARG_PROPERTY_ID) ?: ""
                VacationCalendarScreen(navController = navController, propertyId = propertyId)
            }

            // 12. ADMIN — DASHBOARD
            composable(Screen.AdminDashboard.route) { AdminDashboardScreen(navController = navController) }

            // 13. ADMIN — VERIFICATION
            composable(Screen.VerifyProperties.route) { VerifyPropertiesScreen(navController = navController) }
            composable(Screen.VerifyUsers.route)      { VerifyUsersScreen(navController = navController) }

            composable(
                route     = Screen.PropertyVerificationDetail.route,
                arguments = listOf(navArgument(Screen.PropertyVerificationDetail.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString(Screen.PropertyVerificationDetail.ARG_PROPERTY_ID) ?: ""
                PropertyVerificationDetailScreen(navController = navController, propertyId = propertyId)
            }

            composable(
                route     = Screen.UserVerificationDetail.route,
                arguments = listOf(navArgument(Screen.UserVerificationDetail.ARG_USER_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString(Screen.UserVerificationDetail.ARG_USER_ID) ?: ""
                UserVerificationDetailScreen(navController = navController, userId = userId)
            }

            // 14. ADMIN — MANAGEMENT
            composable(Screen.ManageUsers.route)      { ManageUsersScreen(navController = navController) }
            composable(Screen.ManageProperties.route) { ManagePropertiesScreen(navController = navController) }
            composable(Screen.ManageBookings.route)   { ManageBookingsScreen(navController = navController) }

            // 15. ADMIN — REPORTS
            composable(Screen.Reports.route)        { ReportsScreen(navController = navController) }
            composable(Screen.PaymentReports.route) { PaymentReportsScreen(navController = navController) }

        }
    }
}