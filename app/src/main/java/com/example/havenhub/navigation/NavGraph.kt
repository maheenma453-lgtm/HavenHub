package com.example.havenhub.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.havenhub.screens.*
import com.example.havenhub.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

private val authRoutes = listOf(
    Screen.Splash.route,
    Screen.Onboarding.route,
    Screen.RoleSelection.route,
    "sign_up/{role}",
    Screen.SignIn.route,
    Screen.ForgotPassword.route
)

private val strictAdminRoutes = listOf(
    Screen.AdminDashboard.route,
    Screen.ManageUsers.route,
    Screen.ManageProperties.route,
    Screen.ManageBookings.route,
    Screen.VerifyProperties.route,
    Screen.VerifyUsers.route,
    Screen.PropertyVerificationDetail.route,
    Screen.UserVerificationDetail.route,
    Screen.Reports.route,
    Screen.PaymentReports.route,
)

private val sharedRoutes = listOf(
    Screen.Notifications.route,
    Screen.NotificationDetail.route,
    Screen.Settings.route,
    Screen.AccountSettings.route,
    Screen.NotificationSettings.route,
    Screen.PrivacySettings.route,
    Screen.About.route,
    Screen.HelpAndSupport.route,
    Screen.Profile.route,
    Screen.EditProfile.route,
    Screen.Favourites.route,
)

@Composable
fun HavenHubNavGraph(
    navController     : NavHostController,
    unreadMessageCount: Int = 0
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val uiState by authViewModel.uiState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isCurrentUserLandlord: Boolean = uiState.userRole == "landlord"
    val isCurrentUserAdmin: Boolean = uiState.userRole == "admin"

    val isAuthRoute = currentRoute in authRoutes

    val isAdminRoute = when {
        strictAdminRoutes.any { currentRoute == it }                               -> true
        currentRoute?.startsWith("property_verification_detail") == true           -> true
        currentRoute?.startsWith("user_verification_detail") == true               -> true
        sharedRoutes.any { currentRoute == it }                                    -> isCurrentUserAdmin
        currentRoute?.startsWith("notification_detail") == true                    -> isCurrentUserAdmin
        else                                                                       -> false
    }

    Scaffold(
        bottomBar = {
            when {
                isAuthRoute  -> {}
                isAdminRoute -> AdminBottomNavBar(navController = navController)
                else         -> BottomNavBar(
                    navController      = navController,
                    unreadMessageCount = unreadMessageCount,
                    userRole           = uiState.userRole,
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Splash.route,
            modifier         = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {

// ── Auth ──────────────────────────────────────────────────────────
            composable(Screen.Splash.route)        { SplashScreen(navController) }
            composable(Screen.Onboarding.route)    { OnboardingScreen(navController) }
            composable(Screen.RoleSelection.route) { RoleSelectionScreen(navController) }

            composable(
                route     = Screen.SignUp.route,
                arguments = listOf(navArgument(Screen.SignUp.ARG_ROLE) {
                    type = NavType.StringType; defaultValue = ""
                })
            ) { back ->
                SignUpScreen(
                    navController = navController,
                    selectedRole  = back.arguments?.getString(Screen.SignUp.ARG_ROLE) ?: ""
                )
            }

            composable(Screen.SignIn.route)         { SignInScreen(navController) }
            composable(Screen.ForgotPassword.route) { ForgotPasswordScreen(navController) }

// ── Home / Search ─────────────────────────────────────────────────
            composable(Screen.Home.route)   { HomeScreen(navController) }
            composable(Screen.Search.route) { SearchScreen(navController) }
            composable(Screen.Filter.route) { FilterScreen(navController) }

// ── Property ──────────────────────────────────────────────────────
            composable(Screen.PropertyList.route) { PropertyListScreen(navController) }

            composable(Screen.AddProperty.route) {
                val isVerified = uiState.isVerified
                when {
                    uiState.isLoading      -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    !isCurrentUserLandlord -> { LaunchedEffect(Unit) { navController.popBackStack() } }
                    !isVerified            -> UnverifiedAccessScreen(
                        message = "Your account needs to be verified by admin before you can add properties.",
                        onBack  = { navController.popBackStack() }
                    )
                    else -> AddPropertyScreen(navController)
                }
            }

            composable(Screen.MyProperties.route) { MyPropertiesScreen(navController) }

            composable(
                route     = Screen.PropertyDetail.route,
                arguments = listOf(navArgument(Screen.PropertyDetail.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { back ->
                PropertyDetailScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(Screen.PropertyDetail.ARG_PROPERTY_ID) ?: "",
                )
            }

            composable(
                route     = Screen.EditProperty.route,
                arguments = listOf(navArgument(Screen.EditProperty.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { back ->
                EditPropertyScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(Screen.EditProperty.ARG_PROPERTY_ID) ?: ""
                )
            }

// ── Bookings ──────────────────────────────────────────────────────
            composable(
                route     = "my_bookings?tab={tab}",
                arguments = listOf(
                    navArgument("tab") {
                        type         = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { back ->
                MyBookingsScreen(
                    navController = navController,
                    userId        = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    initialTab    = back.arguments?.getInt("tab") ?: 0,
                )
            }

            composable(
                route     = Screen.Booking.route,
                arguments = listOf(navArgument(Screen.Booking.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { back ->
                val isVerified = uiState.isVerified
                when {
                    uiState.isLoading     -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    isCurrentUserLandlord -> { LaunchedEffect(Unit) { navController.popBackStack() } }
                    !isVerified           -> UnverifiedAccessScreen(
                        message = "Your account needs to be verified by admin before you can book properties.",
                        onBack  = { navController.popBackStack() }
                    )
                    else -> BookingScreen(
                        navController = navController,
                        propertyId    = back.arguments?.getString(Screen.Booking.ARG_PROPERTY_ID) ?: ""
                    )
                }
            }

            composable(
                route     = Screen.BookingConfirmation.route,
                arguments = listOf(navArgument(Screen.BookingConfirmation.ARG_BOOKING_ID) { type = NavType.StringType })
            ) { back ->
                BookingConfirmationScreen(
                    navController = navController,
                    bookingId     = back.arguments?.getString(Screen.BookingConfirmation.ARG_BOOKING_ID) ?: ""
                )
            }

            composable(
                route     = Screen.BookingDetails.route,
                arguments = listOf(navArgument(Screen.BookingDetails.ARG_BOOKING_ID) { type = NavType.StringType })
            ) { back ->
                BookingDetailScreen(
                    navController = navController,
                    bookingId     = back.arguments?.getString(Screen.BookingDetails.ARG_BOOKING_ID) ?: ""
                )
            }

// ── Payment ───────────────────────────────────────────────────────
            composable(
                route     = "payment/{bookingId}/{payerId}/{payeeId}/{payerName}/{payeeName}/{amount}",
                arguments = listOf(
                    navArgument("bookingId") { type = NavType.StringType },
                    navArgument("payerId")   { type = NavType.StringType },
                    navArgument("payeeId")   { type = NavType.StringType },
                    navArgument("payerName") { type = NavType.StringType },
                    navArgument("payeeName") { type = NavType.StringType },
                    navArgument("amount")    { type = NavType.StringType }
                )
            ) { back ->
                PaymentScreen(
                    navController = navController,
                    bookingId     = back.arguments?.getString("bookingId") ?: "",
                    payerId       = back.arguments?.getString("payerId")   ?: "",
                    payeeId       = back.arguments?.getString("payeeId")   ?: "",
                    payerName     = back.arguments?.getString("payerName") ?: "",
                    payeeName     = back.arguments?.getString("payeeName") ?: "",
                    amount        = back.arguments?.getString("amount")?.toDoubleOrNull() ?: 0.0
                )
            }

            composable(Screen.PaymentMethod.route) { PaymentMethodScreen(navController) }

            composable(
                route     = Screen.PaymentSuccess.route,
                arguments = listOf(navArgument(Screen.PaymentSuccess.ARG_BOOKING_ID) { type = NavType.StringType })
            ) { back ->
                PaymentSuccessScreen(
                    navController = navController,
                    bookingId     = back.arguments?.getString(Screen.PaymentSuccess.ARG_BOOKING_ID) ?: ""
                )
            }

// ── Reviews ───────────────────────────────────────────────────────
            composable(
                route     = Screen.AddReview.route,
                arguments = listOf(navArgument(Screen.AddReview.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { back ->
                AddReviewScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(Screen.AddReview.ARG_PROPERTY_ID) ?: "",
                    bookingId     = "",
                    propertyTitle = ""
                )
            }

            composable(
                route     = Screen.ViewReviews.route,
                arguments = listOf(navArgument(Screen.ViewReviews.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { back ->
                ViewReviewsScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(Screen.ViewReviews.ARG_PROPERTY_ID) ?: ""
                )
            }

// ── Profile ───────────────────────────────────────────────────────
            composable(Screen.Profile.route)     { ProfileScreen(navController) }
            composable(Screen.EditProfile.route) { EditProfileScreen(navController) }

// ── Favourites ────────────────────────────────────────────────────
            composable(Screen.Favourites.route) { FavouritesScreen(navController) }

// ── Settings ──────────────────────────────────────────────────────
            composable(Screen.Settings.route)             { SettingsScreen(navController) }
            composable(Screen.AccountSettings.route)      { AccountSettingsScreen(navController) }
            composable(Screen.NotificationSettings.route) { NotificationSettingsScreen(navController) }
            composable(Screen.PrivacySettings.route)      { PrivacySettingsScreen(navController) }
            composable(Screen.About.route)                { AboutScreen(navController) }
            composable(Screen.HelpAndSupport.route)       { HelpAndSupportScreen(navController) }

// ── Notifications ─────────────────────────────────────────────────
            composable(Screen.Notifications.route) { NotificationsScreen(navController) }

            composable(
                route     = Screen.NotificationDetail.route,
                arguments = listOf(navArgument(Screen.NotificationDetail.ARG_NOTIFICATION_ID) { type = NavType.StringType })
            ) { back ->
                NotificationDetailScreen(
                    navController  = navController,
                    notificationId = back.arguments?.getString(Screen.NotificationDetail.ARG_NOTIFICATION_ID) ?: ""
                )
            }

// ── Messaging ─────────────────────────────────────────────────────
            composable(Screen.MessageList.route) { MessageListScreen(navController) }

            composable(
                route     = Screen.Chat.route,
                arguments = listOf(
                    navArgument(Screen.Chat.ARG_USER_ID)     { type = NavType.StringType },
                    navArgument(Screen.Chat.ARG_OWNER_NAME)  { type = NavType.StringType; defaultValue = "Owner" },
                    navArgument(Screen.Chat.ARG_PROPERTY_ID) { type = NavType.StringType; defaultValue = "none" }
                )
            ) { back ->
                val rawPropertyId = back.arguments?.getString(Screen.Chat.ARG_PROPERTY_ID) ?: ""
                ChatScreen(
                    navController = navController,
                    userId        = back.arguments?.getString(Screen.Chat.ARG_USER_ID) ?: "",
                    ownerName     = back.arguments?.getString(Screen.Chat.ARG_OWNER_NAME) ?: "Owner",
                    propertyId    = if (rawPropertyId == "none") "" else rawPropertyId,
                    currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                )
            }

// ── Vacation ──────────────────────────────────────────────────────
            composable(Screen.VacationRentals.route) { VacationRentalsScreen(navController) }

            // ✦ UPDATED: propertyId argument ke saath
            composable(
                route     = Screen.PreBooking.route,
                arguments = listOf(
                    navArgument(Screen.PreBooking.ARG_PROPERTY_ID) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { back ->
                PreBookingScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(Screen.PreBooking.ARG_PROPERTY_ID) ?: ""
                )
            }

            composable(
                route     = Screen.VacationCalendar.route,
                arguments = listOf(navArgument(Screen.VacationCalendar.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { back ->
                VacationCalendarScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(Screen.VacationCalendar.ARG_PROPERTY_ID) ?: ""
                )
            }

// ── Admin ─────────────────────────────────────────────────────────
            composable(Screen.AdminDashboard.route)   { AdminDashboardScreen(navController) }
            composable(Screen.ManageUsers.route)      { ManageUsersScreen(navController) }
            composable(Screen.ManageProperties.route) { ManagePropertiesScreen(navController) }
            composable(Screen.ManageBookings.route)   { ManageBookingsScreen(navController) }
            composable(Screen.VerifyProperties.route) { VerifyPropertiesScreen(navController) }
            composable(Screen.VerifyUsers.route)      { VerifyUsersScreen(navController) }

            composable(
                route     = Screen.PropertyVerificationDetail.route,
                arguments = listOf(navArgument(Screen.PropertyVerificationDetail.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { back ->
                PropertyVerificationDetailScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(Screen.PropertyVerificationDetail.ARG_PROPERTY_ID) ?: ""
                )
            }

            composable(
                route     = Screen.UserVerificationDetail.route,
                arguments = listOf(navArgument(Screen.UserVerificationDetail.ARG_USER_ID) { type = NavType.StringType })
            ) { back ->
                UserVerificationDetailScreen(
                    navController = navController,
                    userId        = back.arguments?.getString(Screen.UserVerificationDetail.ARG_USER_ID) ?: ""
                )
            }

            composable(Screen.Reports.route)        { ReportsScreen(navController) }
            composable(Screen.PaymentReports.route) { PaymentReportsScreen(navController) }
        }
    }
}