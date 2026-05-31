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
import com.example.havenhub.viewmodel.SearchViewModel

// ── Route groupings ───────────────────────────────────────────────────────────

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
    Screen.ManageSeasonalAlerts.route,
    Screen.CreateSeasonalAlert.route,
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
    Screen.GlobalReviews.route,
)

// ── Sub-admin permission model ────────────────────────────────────────────────

private fun hasPermission(
    role: String?,
    permissions: List<String>,
    permission: String
): Boolean = when (role) {
    "admin"     -> true
    "sub_admin" -> permissions.contains(permission)
    else        -> false
}

object SubAdminPermission {
    const val MANAGE_USERS         = "manage_users"
    const val MANAGE_PROPERTIES    = "manage_properties"
    const val MANAGE_BOOKINGS      = "manage_bookings"
    const val VERIFY_PROPERTIES    = "verify_properties"
    const val VERIFY_USERS         = "verify_users"
    const val VIEW_REPORTS         = "view_reports"
    const val VIEW_PAYMENT_REPORTS = "view_payment_reports"
}

// ── Nav graph ─────────────────────────────────────────────────────────────────

@Composable
fun HavenHubNavGraph(
    navController: NavHostController
) {
    val authViewModel        : AuthViewModel         = hiltViewModel()
    val uiState              by authViewModel.uiState.collectAsState()
    val notificationViewModel: NotificationViewModel = hiltViewModel()
    val messagingViewModel   : MessagingViewModel    = hiltViewModel()
    val messagingUiState      by messagingViewModel.uiState.collectAsState()

    val currentUserId       = uiState.currentUser?.uid ?: ""
    val userRole            = uiState.userRole
    val subAdminPermissions = uiState.subAdminPermissions ?: emptyList()

    val isCurrentUserLandlord = userRole == "landlord"
    val isCurrentUserAdmin    = userRole == "admin"
    val isCurrentUserSubAdmin = userRole == "sub_admin"
    val isCurrentUserAnyAdmin = isCurrentUserAdmin || isCurrentUserSubAdmin

    val canManageUsers        = hasPermission(userRole, subAdminPermissions, SubAdminPermission.MANAGE_USERS)
    val canManageProperties   = hasPermission(userRole, subAdminPermissions, SubAdminPermission.MANAGE_PROPERTIES)
    val canManageBookings     = hasPermission(userRole, subAdminPermissions, SubAdminPermission.MANAGE_BOOKINGS)
    val canVerifyProperties   = hasPermission(userRole, subAdminPermissions, SubAdminPermission.VERIFY_PROPERTIES)
    val canVerifyUsers        = hasPermission(userRole, subAdminPermissions, SubAdminPermission.VERIFY_USERS)
    val canViewReports        = hasPermission(userRole, subAdminPermissions, SubAdminPermission.VIEW_REPORTS)
    val canViewPaymentReports = hasPermission(userRole, subAdminPermissions, SubAdminPermission.VIEW_PAYMENT_REPORTS)

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
    val isExploreMap = currentRoute == Screen.ExploreMap.route

    val isAdminRoute = when {
        strictAdminRoutes.any { currentRoute == it }                     -> true
        currentRoute?.startsWith("property_verification_detail") == true -> true
        currentRoute?.startsWith("user_verification_detail") == true     -> true
        sharedRoutes.any { currentRoute == it }                          -> isCurrentUserAnyAdmin
        currentRoute?.startsWith("notification_detail") == true          -> isCurrentUserAnyAdmin
        else                                                             -> false
    }

    Scaffold(
        bottomBar = {
            when {
                isAuthRoute || isExploreMap -> { /* No Bottom Bar */ }
                isAdminRoute -> AdminBottomNavBar(navController = navController)
                else         -> BottomNavBar(
                    navController      = navController,
                    unreadMessageCount = messagingUiState.unreadCount,
                    userRole           = userRole,
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
            composable(Screen.Home.route) { HomeScreen(navController) }

            composable(Screen.Search.route) { searchEntry ->
                val searchViewModel: SearchViewModel = hiltViewModel(searchEntry)
                SearchScreen(
                    navController = navController,
                    viewModel     = searchViewModel
                )
            }

            composable(Screen.Filter.route) { filterEntry ->   // ← FIXED: named lambda param
                val searchEntry = remember(filterEntry) {    // ← FIXED: keyed on NavBackStackEntry
                    navController.getBackStackEntry(Screen.Search.route)
                }
                val searchViewModel: SearchViewModel = hiltViewModel(searchEntry)
                FilterScreen(
                    navController = navController,
                    viewModel     = searchViewModel
                )
            }

// ── Explore Map ───────────────────────────────────────────────────────────────
            composable(Screen.ExploreMap.route) {
                ExploreMapScreen(navController = navController)
            }

// ── Global Reviews ────────────────────────────────────────────────────────────
            composable(Screen.GlobalReviews.route) {
                GlobalReviewsScreen(
                    navController = navController,
                    isLandlord    = isCurrentUserLandlord,
                    currentUserId = currentUserId
                )
            }

// ── Property ──────────────────────────────────────────────────────────────────
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

            composable(
                route     = Screen.AddRentalPackage.route,
                arguments = listOf(navArgument(Screen.AddRentalPackage.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { back ->
                AddRentalPackageScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(Screen.AddRentalPackage.ARG_PROPERTY_ID) ?: ""
                )
            }

// ── Bookings ──────────────────────────────────────────────────────────────────
            composable(
                route     = "my_bookings?tab={tab}",
                arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
            ) { back ->
                MyBookingsScreen(
                    navController = navController,
                    userId        = currentUserId,
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

// ── Payment ───────────────────────────────────────────────────────────────────
            composable(
                route = Screen.Payment.route,
                arguments = listOf(
                    navArgument(Screen.Payment.ARG_BOOKING_ID)   { type = NavType.StringType },
                    navArgument(Screen.Payment.ARG_PAYER_ID)     { type = NavType.StringType },
                    navArgument(Screen.Payment.ARG_PAYEE_ID)     { type = NavType.StringType },
                    navArgument(Screen.Payment.ARG_PAYER_NAME)   { type = NavType.StringType },
                    navArgument(Screen.Payment.ARG_PAYEE_NAME)   { type = NavType.StringType },
                    navArgument(Screen.Payment.ARG_AMOUNT)       { type = NavType.StringType },
                    navArgument(Screen.Payment.ARG_PAYMENT_TYPE) { type = NavType.StringType; defaultValue = "FULL" },
                    navArgument(Screen.Payment.ARG_PACKAGE_ID)   { type = NavType.StringType; defaultValue = "none" },
                )
            ) { back ->
                PaymentScreen(
                    navController = navController,
                    bookingId     = back.arguments?.getString(Screen.Payment.ARG_BOOKING_ID)    ?: "",
                    payerId       = back.arguments?.getString(Screen.Payment.ARG_PAYER_ID)      ?: "",
                    payeeId       = back.arguments?.getString(Screen.Payment.ARG_PAYEE_ID)      ?: "",
                    payerName     = back.arguments?.getString(Screen.Payment.ARG_PAYER_NAME)    ?: "",
                    payeeName     = back.arguments?.getString(Screen.Payment.ARG_PAYEE_NAME)    ?: "",
                    amount        = back.arguments?.getString(Screen.Payment.ARG_AMOUNT)        ?: "0",
                    paymentType   = back.arguments?.getString(Screen.Payment.ARG_PAYMENT_TYPE)  ?: "FULL",
                    packageId     = back.arguments?.getString(Screen.Payment.ARG_PACKAGE_ID)    ?: "none",
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

// ── Reviews ───────────────────────────────────────────────────────────────────
            composable(
                route     = Screen.AddReview.route,
                arguments = listOf(navArgument(Screen.AddReview.ARG_PROPERTY_ID) {
                    type = NavType.StringType; defaultValue = ""
                })
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
                arguments = listOf(navArgument(Screen.NotificationDetail.ARG_NOTIFICATION_ID) { type = NavType.StringType })
            ) { back ->
                NotificationDetailScreen(
                    navController  = navController,
                    notificationId = back.arguments?.getString(Screen.NotificationDetail.ARG_NOTIFICATION_ID) ?: ""
                )
            }

// ── Messaging ─────────────────────────────────────────────────────────────────
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
                    userId        = back.arguments?.getString(Screen.Chat.ARG_USER_ID)    ?: "",
                    ownerName     = back.arguments?.getString(Screen.Chat.ARG_OWNER_NAME) ?: "Owner",
                    propertyId    = if (rawPropertyId == "none") "" else rawPropertyId,
                    currentUserId = currentUserId
                )
            }

// ── Vacation / Pre-booking ────────────────────────────────────────────────────
            composable(Screen.VacationRentals.route) { VacationRentalsScreen(navController) }

            composable(
                route     = Screen.PreBooking.route,
                arguments = listOf(navArgument(Screen.PreBooking.ARG_PROPERTY_ID) {
                    type = NavType.StringType; defaultValue = ""
                })
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

// ── Tenants (landlord only) ───────────────────────────────────────────────────
            composable(Screen.Tenants.route) {
                if (isCurrentUserLandlord) {
                    TenantsScreen(navController = navController)
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

// ── Admin / Sub-admin screens ─────────────────────────────────────────────────
            composable(Screen.AdminDashboard.route) {
                if (isCurrentUserAnyAdmin) AdminDashboardScreen(navController = navController)
                else LaunchedEffect(Unit) { navController.popBackStack() }
            }

            composable(Screen.ManageUsers.route) {
                when { canManageUsers -> ManageUsersScreen(navController); else -> LaunchedEffect(Unit) { navController.popBackStack() } }
            }

            composable(Screen.ManageProperties.route) {
                when { canManageProperties -> ManagePropertiesScreen(navController); else -> LaunchedEffect(Unit) { navController.popBackStack() } }
            }

            composable(Screen.ManageBookings.route) {
                when { canManageBookings -> ManageBookingsScreen(navController); else -> LaunchedEffect(Unit) { navController.popBackStack() } }
            }

            composable(Screen.VerifyProperties.route) {
                when { canVerifyProperties -> VerifyPropertiesScreen(navController); else -> LaunchedEffect(Unit) { navController.popBackStack() } }
            }

            composable(Screen.VerifyUsers.route) {
                when { canVerifyUsers -> VerifyUsersScreen(navController); else -> LaunchedEffect(Unit) { navController.popBackStack() } }
            }

            composable(
                route     = Screen.PropertyVerificationDetail.route,
                arguments = listOf(navArgument(Screen.PropertyVerificationDetail.ARG_PROPERTY_ID) { type = NavType.StringType })
            ) { back ->
                when {
                    canVerifyProperties -> PropertyVerificationDetailScreen(
                        navController = navController,
                        propertyId    = back.arguments?.getString(Screen.PropertyVerificationDetail.ARG_PROPERTY_ID) ?: ""
                    )
                    else -> LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            composable(
                route     = Screen.UserVerificationDetail.route,
                arguments = listOf(navArgument(Screen.UserVerificationDetail.ARG_USER_ID) { type = NavType.StringType })
            ) { back ->
                when {
                    canVerifyUsers -> UserVerificationDetailScreen(
                        navController = navController,
                        userId        = back.arguments?.getString(Screen.UserVerificationDetail.ARG_USER_ID) ?: ""
                    )
                    else -> LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            composable(Screen.Reports.route) {
                when { canViewReports -> ReportsScreen(navController); else -> LaunchedEffect(Unit) { navController.popBackStack() } }
            }

            composable(Screen.PaymentReports.route) {
                when { canViewPaymentReports -> PaymentReportsScreen(navController); else -> LaunchedEffect(Unit) { navController.popBackStack() } }
            }

            composable(Screen.ManageSeasonalAlerts.route) {
                if (isCurrentUserAnyAdmin) ManageSeasonalAlertsScreen(navController)
                else LaunchedEffect(Unit) { navController.popBackStack() }
            }

            composable(Screen.CreateSeasonalAlert.route) {
                if (isCurrentUserAnyAdmin) CreateSeasonalAlertScreen(navController)
                else LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
    }
}