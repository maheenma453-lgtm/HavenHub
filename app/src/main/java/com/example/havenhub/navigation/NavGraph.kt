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
import com.example.havenhub.viewmodel.NotificationViewModel
import com.example.havenhub.viewmodel.MessagingViewModel

// ── Auth flow routes — no bottom bar shown on these ──────────────────────────
private val authRoutes = listOf(
    Screen.Splash.route,
    Screen.Onboarding.route,
    Screen.RoleSelection.route,
    "sign_up/{role}",
    Screen.SignIn.route,
    Screen.ForgotPassword.route
)

// ── Routes only accessible by admin role (both super_admin and sub_admin) ─────
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

// ── Routes accessible by all logged-in users ──────────────────────────────────
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
    Screen.GlobalReviews.route,
)

@Composable
fun HavenHubNavGraph(
    navController: NavHostController
) {
    val authViewModel   : AuthViewModel        = hiltViewModel()
    val uiState         by authViewModel.uiState.collectAsState()
    val notificationViewModel: NotificationViewModel = hiltViewModel()
    val messagingViewModel   : MessagingViewModel    = hiltViewModel()
    val messagingUiState by messagingViewModel.uiState.collectAsState()

    val currentUserId = uiState.currentUser?.uid ?: ""

    // ── Role flags ────────────────────────────────────────────────────────────
    val isCurrentUserLandlord : Boolean = uiState.userRole == "landlord"

    // isAdmin is true for both super_admin and sub_admin
    val isCurrentUserAdmin    : Boolean = uiState.userRole == "admin"

    // adminType determines super_admin vs sub_admin inside admin role
    val isSuperAdmin          : Boolean = isCurrentUserAdmin && uiState.adminType == "super_admin"
    val isSubAdmin            : Boolean = isCurrentUserAdmin && uiState.adminType == "sub_admin"

    // Redirect to SignIn if user is not logged in
    LaunchedEffect(uiState.isLoggedIn, uiState.isAuthReady) {
        if (uiState.isAuthReady && !uiState.isLoggedIn) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != Screen.Splash.route) {
                navController.navigate(Screen.SignIn.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            notificationViewModel.startListening(currentUserId)
            messagingViewModel.loadConversations(currentUserId)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isAuthRoute  = currentRoute in authRoutes

    val isAdminRoute = when {
        strictAdminRoutes.any { currentRoute == it }                     -> true
        currentRoute?.startsWith("property_verification_detail") == true -> true
        currentRoute?.startsWith("user_verification_detail") == true     -> true
        sharedRoutes.any { currentRoute == it }                          -> isCurrentUserAdmin
        currentRoute?.startsWith("notification_detail") == true          -> isCurrentUserAdmin
        else                                                             -> false
    }

    Scaffold(
        bottomBar = {
            when {
                isAuthRoute  -> { /* No bottom bar on auth screens */ }
                isAdminRoute -> AdminBottomNavBar(navController = navController)
                else         -> BottomNavBar(
                    navController      = navController,
                    unreadMessageCount = messagingUiState.unreadCount,
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

// ── Auth ──────────────────────────────────────────────────────────────────────
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

// ── Home / Search ─────────────────────────────────────────────────────────────
            composable(Screen.Home.route)   { HomeScreen(navController) }
            composable(Screen.Search.route) { SearchScreen(navController) }
            composable(Screen.Filter.route) { FilterScreen(navController) }

// ── Global Reviews ────────────────────────────────────────────────────────────
            composable(Screen.GlobalReviews.route) {
                GlobalReviewsScreen(navController = navController)
            }

// ── Property ──────────────────────────────────────────────────────────────────
            composable(Screen.PropertyList.route) { PropertyListScreen(navController) }

            composable(Screen.AddProperty.route) {
                val isVerified = uiState.isVerified
                when {
                    uiState.isLoading      -> Box(
                        Modifier.fillMaxSize(), Alignment.Center
                    ) { CircularProgressIndicator() }
                    !isCurrentUserLandlord -> {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
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
                arguments = listOf(
                    navArgument(Screen.PropertyDetail.ARG_PROPERTY_ID) {
                        type = NavType.StringType
                    }
                )
            ) { back ->
                PropertyDetailScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(
                        Screen.PropertyDetail.ARG_PROPERTY_ID
                    ) ?: "",
                )
            }

            composable(
                route     = Screen.EditProperty.route,
                arguments = listOf(
                    navArgument(Screen.EditProperty.ARG_PROPERTY_ID) {
                        type = NavType.StringType
                    }
                )
            ) { back ->
                EditPropertyScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(
                        Screen.EditProperty.ARG_PROPERTY_ID
                    ) ?: ""
                )
            }

            composable(
                route     = Screen.AddRentalPackage.route,
                arguments = listOf(
                    navArgument(Screen.AddRentalPackage.ARG_PROPERTY_ID) {
                        type = NavType.StringType
                    }
                )
            ) { back ->
                AddRentalPackageScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(
                        Screen.AddRentalPackage.ARG_PROPERTY_ID
                    ) ?: ""
                )
            }

// ── Bookings ──────────────────────────────────────────────────────────────────
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
                    userId        = currentUserId,
                    initialTab    = back.arguments?.getInt("tab") ?: 0,
                )
            }

            composable(
                route     = Screen.Booking.route,
                arguments = listOf(
                    navArgument(Screen.Booking.ARG_PROPERTY_ID) {
                        type = NavType.StringType
                    }
                )
            ) { back ->
                val isVerified = uiState.isVerified
                when {
                    uiState.isLoading     -> Box(
                        Modifier.fillMaxSize(), Alignment.Center
                    ) { CircularProgressIndicator() }
                    isCurrentUserLandlord -> {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                    !isVerified           -> UnverifiedAccessScreen(
                        message = "Your account needs to be verified by admin before you can book properties.",
                        onBack  = { navController.popBackStack() }
                    )
                    else -> BookingScreen(
                        navController = navController,
                        propertyId    = back.arguments?.getString(
                            Screen.Booking.ARG_PROPERTY_ID
                        ) ?: ""
                    )
                }
            }

            composable(
                route     = Screen.BookingConfirmation.route,
                arguments = listOf(
                    navArgument(Screen.BookingConfirmation.ARG_BOOKING_ID) {
                        type = NavType.StringType
                    }
                )
            ) { back ->
                BookingConfirmationScreen(
                    navController = navController,
                    bookingId     = back.arguments?.getString(
                        Screen.BookingConfirmation.ARG_BOOKING_ID
                    ) ?: ""
                )
            }

            composable(
                route     = Screen.BookingDetails.route,
                arguments = listOf(
                    navArgument(Screen.BookingDetails.ARG_BOOKING_ID) {
                        type = NavType.StringType
                    }
                )
            ) { back ->
                BookingDetailScreen(
                    navController = navController,
                    bookingId     = back.arguments?.getString(
                        Screen.BookingDetails.ARG_BOOKING_ID
                    ) ?: ""
                )
            }

// ── Payment ───────────────────────────────────────────────────────────────────
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
                    amount        = back.arguments?.getString("amount")    ?: ""
                )
            }

            composable(Screen.PaymentMethod.route) { PaymentMethodScreen(navController) }

            composable(
                route     = Screen.PaymentSuccess.route,
                arguments = listOf(
                    navArgument(Screen.PaymentSuccess.ARG_BOOKING_ID) {
                        type = NavType.StringType
                    }
                )
            ) { back ->
                PaymentSuccessScreen(
                    navController = navController,
                    bookingId     = back.arguments?.getString(
                        Screen.PaymentSuccess.ARG_BOOKING_ID
                    ) ?: ""
                )
            }

// ── Reviews ───────────────────────────────────────────────────────────────────
            composable(
                route     = Screen.AddReview.route,
                arguments = listOf(
                    navArgument(Screen.AddReview.ARG_PROPERTY_ID) {
                        type         = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { back ->
                val pid = back.arguments?.getString(Screen.AddReview.ARG_PROPERTY_ID) ?: ""
                AddReviewScreen(
                    navController = navController,
                    propertyId    = pid,
                    bookingId     = "",
                    propertyTitle = ""
                )
            }

            composable(
                route     = Screen.ViewReviews.route,
                arguments = listOf(
                    navArgument(Screen.ViewReviews.ARG_PROPERTY_ID) {
                        type = NavType.StringType
                    }
                )
            ) { back ->
                ViewReviewsScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(
                        Screen.ViewReviews.ARG_PROPERTY_ID
                    ) ?: ""
                )
            }

// ── Profile ───────────────────────────────────────────────────────────────────
            composable(Screen.Profile.route)     { ProfileScreen(navController) }
            composable(Screen.EditProfile.route) { EditProfileScreen(navController) }

// ── Favourites ────────────────────────────────────────────────────────────────
            composable(Screen.Favourites.route) { FavouritesScreen(navController) }

// ── Settings ──────────────────────────────────────────────────────────────────
            composable(Screen.Settings.route)             { SettingsScreen(navController) }
            composable(Screen.AccountSettings.route)      { AccountSettingsScreen(navController) }
            composable(Screen.NotificationSettings.route) { NotificationSettingsScreen(navController) }
            composable(Screen.PrivacySettings.route)      { PrivacySettingsScreen(navController) }
            composable(Screen.About.route)                { AboutScreen(navController) }
            composable(Screen.HelpAndSupport.route)       { HelpAndSupportScreen(navController) }

// ── Notifications ─────────────────────────────────────────────────────────────
            composable(Screen.Notifications.route) { NotificationsScreen(navController) }

            composable(
                route     = Screen.NotificationDetail.route,
                arguments = listOf(
                    navArgument(Screen.NotificationDetail.ARG_NOTIFICATION_ID) {
                        type = NavType.StringType
                    }
                )
            ) { back ->
                NotificationDetailScreen(
                    navController  = navController,
                    notificationId = back.arguments?.getString(
                        Screen.NotificationDetail.ARG_NOTIFICATION_ID
                    ) ?: ""
                )
            }

// ── Messaging ─────────────────────────────────────────────────────────────────
            composable(Screen.MessageList.route) { MessageListScreen(navController) }

            composable(
                route     = Screen.Chat.route,
                arguments = listOf(
                    navArgument(Screen.Chat.ARG_USER_ID)     { type = NavType.StringType },
                    navArgument(Screen.Chat.ARG_OWNER_NAME)  {
                        type = NavType.StringType; defaultValue = "Owner"
                    },
                    navArgument(Screen.Chat.ARG_PROPERTY_ID) {
                        type = NavType.StringType; defaultValue = "none"
                    }
                )
            ) { back ->
                val rawPropertyId = back.arguments?.getString(Screen.Chat.ARG_PROPERTY_ID) ?: ""
                ChatScreen(
                    navController = navController,
                    userId        = back.arguments?.getString(Screen.Chat.ARG_USER_ID) ?: "",
                    ownerName     = back.arguments?.getString(Screen.Chat.ARG_OWNER_NAME) ?: "Owner",
                    propertyId    = if (rawPropertyId == "none") "" else rawPropertyId,
                    currentUserId = currentUserId
                )
            }

// ── Vacation ──────────────────────────────────────────────────────────────────
            composable(Screen.VacationRentals.route) { VacationRentalsScreen(navController) }

            composable(
                route     = Screen.PreBooking.route,
                arguments = listOf(
                    navArgument(Screen.PreBooking.ARG_PROPERTY_ID) {
                        type         = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { back ->
                PreBookingScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(
                        Screen.PreBooking.ARG_PROPERTY_ID
                    ) ?: ""
                )
            }

            composable(
                route     = Screen.VacationCalendar.route,
                arguments = listOf(
                    navArgument(Screen.VacationCalendar.ARG_PROPERTY_ID) {
                        type = NavType.StringType
                    }
                )
            ) { back ->
                VacationCalendarScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(
                        Screen.VacationCalendar.ARG_PROPERTY_ID
                    ) ?: ""
                )
            }

// ── Admin ─────────────────────────────────────────────────────────────────────
// Both super_admin and sub_admin land on AdminDashboard
// AdminDashboard internally checks isSuperAdmin vs isSubAdmin
// and shows/hides options based on adminType and permissions

            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(
                    navController = navController,
                    isSuperAdmin  = isSuperAdmin   // dashboard uses this to show full vs limited UI
                )
            }

            // Manage Users — super_admin always, sub_admin only if canViewUsers == true
            composable(Screen.ManageUsers.route) {
                when {
                    isSuperAdmin -> ManageUsersScreen(navController)
                    isSubAdmin && uiState.permissions.canViewUsers -> ManageUsersScreen(navController)
                    else -> {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                }
            }

            composable(Screen.ManageProperties.route) {
                when {
                    isSuperAdmin -> ManagePropertiesScreen(navController)
                    isSubAdmin && uiState.permissions.canApproveProperties ->
                        ManagePropertiesScreen(navController)
                    else -> {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                }
            }

            // Manage Bookings — super_admin always, sub_admin only if canViewBookings == true
            composable(Screen.ManageBookings.route) {
                when {
                    isSuperAdmin -> ManageBookingsScreen(navController)
                    isSubAdmin && uiState.permissions.canViewBookings -> ManageBookingsScreen(navController)
                    else -> {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                }
            }

            // Verify Properties — super_admin always, sub_admin only if canApproveProperties == true
            composable(Screen.VerifyProperties.route) {
                when {
                    isSuperAdmin -> VerifyPropertiesScreen(navController)
                    isSubAdmin && uiState.permissions.canApproveProperties ->
                        VerifyPropertiesScreen(navController)
                    else -> {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                }
            }

            // Verify Users — super_admin only
            composable(Screen.VerifyUsers.route) {
                when {
                    isSuperAdmin -> VerifyUsersScreen(navController)
                    isSubAdmin && uiState.permissions.canVerifyUsers -> VerifyUsersScreen(navController)
                    else -> {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                }
            }

            composable(
                route     = Screen.PropertyVerificationDetail.route,
                arguments = listOf(
                    navArgument(Screen.PropertyVerificationDetail.ARG_PROPERTY_ID) {
                        type = NavType.StringType
                    }
                )
            ) { back ->
                PropertyVerificationDetailScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(
                        Screen.PropertyVerificationDetail.ARG_PROPERTY_ID
                    ) ?: ""
                )
            }

            composable(
                route     = Screen.UserVerificationDetail.route,
                arguments = listOf(
                    navArgument(Screen.UserVerificationDetail.ARG_USER_ID) {
                        type = NavType.StringType
                    }
                )
            ) { back ->
                UserVerificationDetailScreen(
                    navController = navController,
                    userId        = back.arguments?.getString(
                        Screen.UserVerificationDetail.ARG_USER_ID
                    ) ?: ""
                )
            }

            // Reports — super_admin always, sub_admin only if canViewPayments == true
            composable(Screen.Reports.route) {
                when {
                    isSuperAdmin -> ReportsScreen(navController)
                    isSubAdmin && uiState.permissions.canViewPayments -> ReportsScreen(navController)
                    else -> {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                }
            }

            composable(Screen.PaymentReports.route) {
                when {
                    isSuperAdmin -> PaymentReportsScreen(navController)
                    isSubAdmin && uiState.permissions.canViewPayments ->
                        PaymentReportsScreen(navController)
                    else -> {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                }
            }
        }
    }
}
