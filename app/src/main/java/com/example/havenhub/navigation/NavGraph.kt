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

// FIX: all screens are in com.example.havenhub.screens (no sub-packages)
import com.example.havenhub.screens.SplashScreen
import com.example.havenhub.screens.OnboardingScreen
import com.example.havenhub.screens.SignInScreen
import com.example.havenhub.screens.SignUpScreen
import com.example.havenhub.screens.RoleSelectionScreen
import com.example.havenhub.screens.ForgotPasswordScreen
import com.example.havenhub.screens.HomeScreen
import com.example.havenhub.screens.SearchScreen
import com.example.havenhub.screens.FilterScreen
import com.example.havenhub.screens.PropertyListScreen
import com.example.havenhub.screens.PropertyDetailScreen
import com.example.havenhub.screens.AddPropertyScreen
import com.example.havenhub.screens.MyPropertiesScreen
import com.example.havenhub.screens.EditPropertyScreen
import com.example.havenhub.screens.BookingScreen
import com.example.havenhub.screens.BookingConfirmationScreen
import com.example.havenhub.screens.MyBookingsScreen
import com.example.havenhub.screens.BookingDetailScreen
import com.example.havenhub.screens.PaymentScreen
import com.example.havenhub.screens.PaymentMethodScreen
import com.example.havenhub.screens.PaymentSuccessScreen
import com.example.havenhub.screens.AddReviewScreen
import com.example.havenhub.screens.ViewReviewsScreen
import com.example.havenhub.screens.ProfileScreen
import com.example.havenhub.screens.EditProfileScreen
import com.example.havenhub.screens.SettingsScreen
import com.example.havenhub.screens.AccountSettingsScreen
import com.example.havenhub.screens.NotificationSettingsScreen
import com.example.havenhub.screens.PrivacySettingsScreen
import com.example.havenhub.screens.AboutScreen
import com.example.havenhub.screens.HelpAndSupportScreen
import com.example.havenhub.screens.NotificationsScreen
import com.example.havenhub.screens.NotificationDetailScreen
import com.example.havenhub.screens.MessageListScreen
import com.example.havenhub.screens.ChatScreen
import com.example.havenhub.screens.VacationRentalsScreen
import com.example.havenhub.screens.PreBookingScreen
import com.example.havenhub.screens.VacationCalendarScreen
import com.example.havenhub.screens.AdminDashboardScreen
import com.example.havenhub.screens.VerifyPropertiesScreen
import com.example.havenhub.screens.VerifyUsersScreen
import com.example.havenhub.screens.PropertyVerificationDetailScreen
import com.example.havenhub.screens.UserVerificationDetailScreen
import com.example.havenhub.screens.ManageUsersScreen
import com.example.havenhub.screens.ManagePropertiesScreen
import com.example.havenhub.screens.ManageBookingsScreen
import com.example.havenhub.screens.ReportsScreen
import com.example.havenhub.screens.PaymentReportsScreen

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
                // FIX: actual BottomNavBar only takes navController
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->

        NavHost(
            navController    = navController,
            startDestination = Screen.Splash.route,
            modifier         = Modifier.padding(innerPadding)
        ) {

            // 1. AUTH
            composable(Screen.Splash.route)         { SplashScreen(navController) }
            composable(Screen.Onboarding.route)     { OnboardingScreen(navController) }
            composable(Screen.SignIn.route)         { SignInScreen(navController) }
            composable(Screen.SignUp.route)         { SignUpScreen(navController) }
            composable(Screen.RoleSelection.route)  { RoleSelectionScreen(navController) }
            composable(Screen.ForgotPassword.route) { ForgotPasswordScreen(navController) }

            // 2. MAIN
            composable(Screen.Home.route)   { HomeScreen(navController) }
            composable(Screen.Search.route) { SearchScreen(navController) }
            composable(Screen.Filter.route) { FilterScreen(navController) }

            // 3. PROPERTY
            composable(Screen.PropertyList.route) { PropertyListScreen(navController) }
            composable(Screen.AddProperty.route)  { AddPropertyScreen(navController) }
            composable(Screen.MyProperties.route) { MyPropertiesScreen(navController) }

            composable(
                route     = Screen.PropertyDetail.route,
                arguments = listOf(navArgument(Screen.PropertyDetail.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString(Screen.PropertyDetail.ARG_PROPERTY_ID) ?: ""
                PropertyDetailScreen(navController, propertyId)
            }

            composable(
                route     = Screen.EditProperty.route,
                arguments = listOf(navArgument(Screen.EditProperty.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString(Screen.EditProperty.ARG_PROPERTY_ID) ?: ""
                EditPropertyScreen(navController, propertyId)
            }

            // 4. BOOKING
            composable(Screen.MyBookings.route) { MyBookingsScreen(navController) }

            composable(
                route     = Screen.Booking.route,
                arguments = listOf(navArgument(Screen.Booking.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString(Screen.Booking.ARG_PROPERTY_ID) ?: ""
                BookingScreen(navController, propertyId)
            }

            composable(
                route     = Screen.BookingConfirmation.route,
                arguments = listOf(navArgument(Screen.BookingConfirmation.ARG_BOOKING_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString(Screen.BookingConfirmation.ARG_BOOKING_ID) ?: ""
                BookingConfirmationScreen(navController, bookingId)
            }

            composable(
                route     = Screen.BookingDetails.route,
                arguments = listOf(navArgument(Screen.BookingDetails.ARG_BOOKING_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString(Screen.BookingDetails.ARG_BOOKING_ID) ?: ""
                BookingDetailScreen(navController, bookingId)
            }

            // 5. PAYMENT
            composable(
                route     = Screen.Payment.route,
                arguments = listOf(navArgument(Screen.Payment.ARG_BOOKING_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString(Screen.Payment.ARG_BOOKING_ID) ?: ""
                PaymentScreen(navController, bookingId)
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
                PaymentMethodScreen(navController, bookingId, method)
            }

            composable(
                route     = Screen.PaymentSuccess.route,
                arguments = listOf(navArgument(Screen.PaymentSuccess.ARG_BOOKING_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString(Screen.PaymentSuccess.ARG_BOOKING_ID) ?: ""
                PaymentSuccessScreen(navController, bookingId)
            }

            // 6. REVIEW
            composable(
                route     = Screen.AddReview.route,
                arguments = listOf(navArgument(Screen.AddReview.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString(Screen.AddReview.ARG_PROPERTY_ID) ?: ""
                AddReviewScreen(navController, propertyId)
            }

            composable(
                route     = Screen.ViewReviews.route,
                arguments = listOf(navArgument(Screen.ViewReviews.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString(Screen.ViewReviews.ARG_PROPERTY_ID) ?: ""
                ViewReviewsScreen(navController, propertyId)
            }

            // 7. PROFILE
            composable(Screen.Profile.route)     { ProfileScreen(navController) }
            composable(Screen.EditProfile.route) { EditProfileScreen(navController) }

            // 8. SETTINGS
            composable(Screen.Settings.route)             { SettingsScreen(navController) }
            composable(Screen.AccountSettings.route)      { AccountSettingsScreen(navController) }
            composable(Screen.NotificationSettings.route) { NotificationSettingsScreen(navController) }
            composable(Screen.PrivacySettings.route)      { PrivacySettingsScreen(navController) }
            composable(Screen.About.route)                { AboutScreen(navController) }
            composable(Screen.HelpAndSupport.route)       { HelpAndSupportScreen(navController) }

            // 9. NOTIFICATIONS
            composable(Screen.Notifications.route) { NotificationsScreen(navController) }

            composable(
                route     = Screen.NotificationDetail.route,
                arguments = listOf(navArgument(Screen.NotificationDetail.ARG_NOTIFICATION_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val notificationId = backStackEntry.arguments?.getString(Screen.NotificationDetail.ARG_NOTIFICATION_ID) ?: ""
                NotificationDetailScreen(navController, notificationId)
            }

            // 10. MESSAGING
            composable(Screen.MessageList.route) { MessageListScreen(navController) }

            composable(
                route     = Screen.Chat.route,
                arguments = listOf(navArgument(Screen.Chat.ARG_USER_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString(Screen.Chat.ARG_USER_ID) ?: ""
                ChatScreen(navController, userId)
            }

            // 11. VACATION
            composable(Screen.VacationRentals.route) { VacationRentalsScreen(navController) }
            composable(Screen.PreBooking.route)      { PreBookingScreen(navController) }

            composable(
                route     = Screen.VacationCalendar.route,
                arguments = listOf(navArgument(Screen.VacationCalendar.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString(Screen.VacationCalendar.ARG_PROPERTY_ID) ?: ""
                VacationCalendarScreen(navController, propertyId)
            }

            // 12. ADMIN — DASHBOARD
            composable(Screen.AdminDashboard.route) { AdminDashboardScreen(navController) }

            // 13. ADMIN — VERIFICATION
            composable(Screen.VerifyProperties.route) { VerifyPropertiesScreen(navController) }
            composable(Screen.VerifyUsers.route)      { VerifyUsersScreen(navController) }

            composable(
                route     = Screen.PropertyVerificationDetail.route,
                arguments = listOf(navArgument(Screen.PropertyVerificationDetail.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString(Screen.PropertyVerificationDetail.ARG_PROPERTY_ID) ?: ""
                PropertyVerificationDetailScreen(navController, propertyId)
            }

            composable(
                route     = Screen.UserVerificationDetail.route,
                arguments = listOf(navArgument(Screen.UserVerificationDetail.ARG_USER_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString(Screen.UserVerificationDetail.ARG_USER_ID) ?: ""
                UserVerificationDetailScreen(navController, userId)
            }

            // 14. ADMIN — MANAGEMENT
            composable(Screen.ManageUsers.route)      { ManageUsersScreen(navController) }
            composable(Screen.ManageProperties.route) { ManagePropertiesScreen(navController) }
            composable(Screen.ManageBookings.route)   { ManageBookingsScreen(navController) }

            // 15. ADMIN — REPORTS
            composable(Screen.Reports.route)        { ReportsScreen(navController) }
            composable(Screen.PaymentReports.route) { PaymentReportsScreen(navController) }
        }
    }
}