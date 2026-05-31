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

// ── Route Groupings ───────────────────────────────────────────────────────────
// Auth-only routes: no bottom navigation bar shown on these screens.
private val authRoutes = listOf(
    Screen.Splash.route,
    Screen.Onboarding.route,
    Screen.RoleSelection.route,
    "sign_up/{role}",
    Screen.SignIn.route,
    Screen.ForgotPassword.route
)

// Routes that always render the Admin bottom nav bar regardless of user role.
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

// Routes shared between tenants and admins/sub-admins.
// When an admin navigates to these routes, the Admin bottom nav bar is used;
// when a tenant navigates to them, the regular bottom nav bar is used.
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

// ── Sub-Admin Permission Helper ───────────────────────────────────────────────
/**
 * Returns true if the given [role] has access to [permission].
 * - "admin"     → always has every permission.
 * - "sub_admin" → only if [permissions] list explicitly contains [permission].
 * - Any other role → denied.
 */
private fun hasPermission(
    role       : String?,
    permissions: List<String>,
    permission : String
): Boolean = when (role) {
    "admin"     -> true
    "sub_admin" -> permissions.contains(permission)
    else        -> false
}

/** Constant keys for sub-admin permission strings stored in Firestore. */
object SubAdminPermission {
    const val MANAGE_USERS         = "manage_users"
    const val MANAGE_PROPERTIES    = "manage_properties"
    const val MANAGE_BOOKINGS      = "manage_bookings"
    const val VERIFY_PROPERTIES    = "verify_properties"
    const val VERIFY_USERS         = "verify_users"
    const val VIEW_REPORTS         = "view_reports"
    const val VIEW_PAYMENT_REPORTS = "view_payment_reports"
}

// ── Root Nav Graph ────────────────────────────────────────────────────────────
/**
 * HavenHubNavGraph — single-activity Navigation component for the entire app.
 *
 * Responsibilities:
 *  • Observes auth state and redirects to Sign-In when the session ends.
 *  • Starts real-time Firestore listeners (notifications, conversations) once
 *    the authenticated user UID is available.
 *  • Decides which bottom navigation bar to render based on the current route
 *    and the authenticated user's role.
 *  • Declares every composable destination in the app, enforcing role-based
 *    access control inline (popBackStack for unauthorised access).
 */
@Composable
fun HavenHubNavGraph(
    navController: NavHostController
) {
    // ── ViewModels ────────────────────────────────────────────────────────────
    val authViewModel        : AuthViewModel         = hiltViewModel()
    val uiState              by authViewModel.uiState.collectAsState()
    val notificationViewModel: NotificationViewModel = hiltViewModel()
    val messagingViewModel   : MessagingViewModel    = hiltViewModel()
    val messagingUiState     by messagingViewModel.uiState.collectAsState()

    // ── Derived Auth State ────────────────────────────────────────────────────
    val currentUserId       = uiState.currentUser?.uid ?: ""
    val userRole            = uiState.userRole
    val subAdminPermissions = uiState.subAdminPermissions ?: emptyList()

    val isCurrentUserLandlord = userRole == "landlord"
    val isCurrentUserAdmin    = userRole == "admin"
    val isCurrentUserSubAdmin = userRole == "sub_admin"
    // Convenience flag covering both full admin and sub-admin roles.
    val isCurrentUserAnyAdmin = isCurrentUserAdmin || isCurrentUserSubAdmin

    // ── Sub-Admin Permission Flags ────────────────────────────────────────────
    val canManageUsers        = hasPermission(userRole, subAdminPermissions, SubAdminPermission.MANAGE_USERS)
    val canManageProperties   = hasPermission(userRole, subAdminPermissions, SubAdminPermission.MANAGE_PROPERTIES)
    val canManageBookings     = hasPermission(userRole, subAdminPermissions, SubAdminPermission.MANAGE_BOOKINGS)
    val canVerifyProperties   = hasPermission(userRole, subAdminPermissions, SubAdminPermission.VERIFY_PROPERTIES)
    val canVerifyUsers        = hasPermission(userRole, subAdminPermissions, SubAdminPermission.VERIFY_USERS)
    val canViewReports        = hasPermission(userRole, subAdminPermissions, SubAdminPermission.VIEW_REPORTS)
    val canViewPaymentReports = hasPermission(userRole, subAdminPermissions, SubAdminPermission.VIEW_PAYMENT_REPORTS)

    // ── Auth Session Guard ────────────────────────────────────────────────────
    // When auth is ready and the user is no longer logged in, pop the entire
    // back-stack and navigate to Sign-In (except from the Splash screen which
    // handles its own initial routing).
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

    // ── Real-Time Listeners ───────────────────────────────────────────────────
    // Start notification and messaging listeners as soon as the UID is known.
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            notificationViewModel.startListening(currentUserId)
            messagingViewModel.loadConversations(currentUserId)
        }
    }

    // ── Current Route Tracking ────────────────────────────────────────────────
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isAuthRoute  = currentRoute in authRoutes
    // ExploreMap hides the bottom bar so the map fills the entire screen.
    val isExploreMap = currentRoute == Screen.ExploreMap.route

    // Determine whether the Admin bottom nav bar should be shown.
    val isAdminRoute = when {
        // Strict admin-only routes always use the admin nav bar.
        strictAdminRoutes.any { currentRoute == it }                     -> true
        // Dynamic detail routes checked by prefix.
        currentRoute?.startsWith("property_verification_detail") == true -> true
        currentRoute?.startsWith("user_verification_detail") == true     -> true
        // Shared routes use the admin nav bar only when the user is an admin.
        sharedRoutes.any { currentRoute == it }                          -> isCurrentUserAnyAdmin
        currentRoute?.startsWith("notification_detail") == true          -> isCurrentUserAnyAdmin
        else                                                             -> false
    }

    // ── Scaffold with Dynamic Bottom Bar ──────────────────────────────────────
    Scaffold(
        bottomBar = {
            when {
                // No bottom bar on auth screens or the full-screen map.
                isAuthRoute || isExploreMap -> { /* intentionally empty */ }

                isAdminRoute -> AdminBottomNavBar(navController = navController)

                else -> BottomNavBar(
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

            // ── Auth Screens ──────────────────────────────────────────────────
            composable(Screen.Splash.route)        { SplashScreen(navController) }
            composable(Screen.Onboarding.route)    { OnboardingScreen(navController) }
            composable(Screen.RoleSelection.route) { RoleSelectionScreen(navController) }

            // Sign-Up accepts a "role" argument pre-filled from the Role Selection screen.
            composable(
                route     = Screen.SignUp.route,
                arguments = listOf(navArgument(Screen.SignUp.ARG_ROLE) {
                    type         = NavType.StringType
                    defaultValue = ""
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
            composable(Screen.Home.route) { HomeScreen(navController) }

            // SearchViewModel is scoped to this back-stack entry so it is shared
            // with the Filter screen (see below).
            composable(Screen.Search.route) { searchEntry ->
                val searchViewModel: SearchViewModel = hiltViewModel(searchEntry)
                SearchScreen(
                    navController = navController,
                    viewModel     = searchViewModel
                )
            }

            // Filter screen retrieves the SearchViewModel from the Search entry
            // so that both screens share the same filter/search state.
            composable(Screen.Filter.route) { filterEntry ->
                // Key on filterEntry so the remembered value recomputes if the
                // back-stack entry changes (e.g. after popping and re-entering).
                val searchEntry = remember(filterEntry) {
                    navController.getBackStackEntry(Screen.Search.route)
                }
                val searchViewModel: SearchViewModel = hiltViewModel(searchEntry)
                FilterScreen(
                    navController = navController,
                    viewModel     = searchViewModel
                )
            }

            // ── Explore Map ───────────────────────────────────────────────────
            // Full-screen map with no bottom bar (isExploreMap guard above).
            composable(Screen.ExploreMap.route) {
                ExploreMapScreen(navController = navController)
            }

            // ── Global Reviews ────────────────────────────────────────────────
            // Passes role flags so the screen can show landlord/admin-specific
            // controls (e.g. long-press delete for landlords).
            composable(Screen.GlobalReviews.route) {
                GlobalReviewsScreen(
                    navController = navController,
                    isLandlord    = isCurrentUserLandlord,
                    isAdmin       = isCurrentUserAdmin,
                    currentUserId = currentUserId
                )
            }

            // ── Property Screens ──────────────────────────────────────────────
            composable(Screen.PropertyList.route) { PropertyListScreen(navController) }

            // AddProperty is landlord-only and requires admin verification.
            composable(Screen.AddProperty.route) {
                val isVerified = uiState.isVerified
                when {
                    // Show a spinner while auth state is still loading.
                    uiState.isLoading -> Box(
                        Modifier.fillMaxSize(), Alignment.Center
                    ) { CircularProgressIndicator() }

                    // Non-landlords cannot add properties — pop back immediately.
                    !isCurrentUserLandlord -> {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }

                    // Unverified landlords see an informative gate screen.
                    !isVerified -> UnverifiedAccessScreen(
                        message = "Your account needs to be verified by admin before you can add properties.",
                        onBack  = { navController.popBackStack() }
                    )

                    else -> AddPropertyScreen(navController)
                }
            }

            composable(Screen.MyProperties.route) { MyPropertiesScreen(navController) }

            composable(
                route     = Screen.PropertyDetail.route,
                arguments = listOf(navArgument(Screen.PropertyDetail.ARG_PROPERTY_ID) {
                    type = NavType.StringType
                })
            ) { back ->
                PropertyDetailScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(Screen.PropertyDetail.ARG_PROPERTY_ID) ?: "",
                )
            }

            composable(
                route     = Screen.EditProperty.route,
                arguments = listOf(navArgument(Screen.EditProperty.ARG_PROPERTY_ID) {
                    type = NavType.StringType
                })
            ) { back ->
                EditPropertyScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(Screen.EditProperty.ARG_PROPERTY_ID) ?: ""
                )
            }

            // AddRentalPackage — landlord creates a time-limited deal for a property.
            composable(
                route     = Screen.AddRentalPackage.route,
                arguments = listOf(navArgument(Screen.AddRentalPackage.ARG_PROPERTY_ID) {
                    type = NavType.StringType
                })
            ) { back ->
                AddRentalPackageScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(Screen.AddRentalPackage.ARG_PROPERTY_ID) ?: ""
                )
            }

            // ── Booking Screens ───────────────────────────────────────────────

            // MyBookings supports a "tab" query parameter so that other screens
            // can deep-link directly to a specific booking status tab (e.g. tab=1
            // for "Deposit Paid" after a deposit payment).
            composable(
                route     = "my_bookings?tab={tab}",
                arguments = listOf(navArgument("tab") {
                    type         = NavType.IntType
                    defaultValue = 0
                })
            ) { back ->
                MyBookingsScreen(
                    navController = navController,
                    userId        = currentUserId,
                    initialTab    = back.arguments?.getInt("tab") ?: 0,
                )
            }

            // Booking screen is tenant-only and requires admin verification.
            composable(
                route     = Screen.Booking.route,
                arguments = listOf(navArgument(Screen.Booking.ARG_PROPERTY_ID) {
                    type = NavType.StringType
                })
            ) { back ->
                val isVerified = uiState.isVerified
                when {
                    uiState.isLoading -> Box(
                        Modifier.fillMaxSize(), Alignment.Center
                    ) { CircularProgressIndicator() }

                    // Landlords cannot book properties — pop back.
                    isCurrentUserLandlord -> {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }

                    // Unverified tenants see an informative gate screen.
                    !isVerified -> UnverifiedAccessScreen(
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
                arguments = listOf(navArgument(Screen.BookingConfirmation.ARG_BOOKING_ID) {
                    type = NavType.StringType
                })
            ) { back ->
                BookingConfirmationScreen(
                    navController = navController,
                    bookingId     = back.arguments?.getString(Screen.BookingConfirmation.ARG_BOOKING_ID) ?: ""
                )
            }

            composable(
                route     = Screen.BookingDetails.route,
                arguments = listOf(navArgument(Screen.BookingDetails.ARG_BOOKING_ID) {
                    type = NavType.StringType
                })
            ) { back ->
                BookingDetailScreen(
                    navController         = navController,
                    bookingId             = back.arguments?.getString(Screen.BookingDetails.ARG_BOOKING_ID) ?: "",
                    isCurrentUserLandlord = isCurrentUserLandlord  // Enables landlord-specific actions in the detail screen.
                )
            }

            // ── Payment Screens ───────────────────────────────────────────────
            // Payment route carries all necessary booking/payer/payee details as
            // path segments so they survive process death without a ViewModel.
            //
            // paymentType values:
            //   "FULL"      → tenant pays 100% for a standard booking
            //   "DEPOSIT"   → tenant pays 20% deposit for a pre-booking
            //   "REMAINING" → tenant pays the remaining 80% at check-in
            //
            // packageId is only relevant for the DEPOSIT flow; pass "none" otherwise.
            composable(
                route     = Screen.Payment.route,
                arguments = listOf(
                    navArgument(Screen.Payment.ARG_BOOKING_ID)   { type = NavType.StringType },
                    navArgument(Screen.Payment.ARG_PAYER_ID)     { type = NavType.StringType },
                    navArgument(Screen.Payment.ARG_PAYEE_ID)     { type = NavType.StringType },
                    navArgument(Screen.Payment.ARG_PAYER_NAME)   { type = NavType.StringType },
                    navArgument(Screen.Payment.ARG_PAYEE_NAME)   { type = NavType.StringType },
                    navArgument(Screen.Payment.ARG_AMOUNT)       { type = NavType.StringType },
                    navArgument(Screen.Payment.ARG_PAYMENT_TYPE) {
                        type         = NavType.StringType
                        defaultValue = "FULL"
                    },
                    navArgument(Screen.Payment.ARG_PACKAGE_ID)   {
                        type         = NavType.StringType
                        defaultValue = "none"
                    },
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
                arguments = listOf(navArgument(Screen.PaymentSuccess.ARG_BOOKING_ID) {
                    type = NavType.StringType
                })
            ) { back ->
                PaymentSuccessScreen(
                    navController = navController,
                    bookingId     = back.arguments?.getString(Screen.PaymentSuccess.ARG_BOOKING_ID) ?: ""
                )
            }

            // ── Review Screens ────────────────────────────────────────────────
            composable(
                route     = Screen.AddReview.route,
                arguments = listOf(navArgument(Screen.AddReview.ARG_PROPERTY_ID) {
                    type         = NavType.StringType
                    defaultValue = ""
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
                arguments = listOf(navArgument(Screen.ViewReviews.ARG_PROPERTY_ID) {
                    type = NavType.StringType
                })
            ) { back ->
                ViewReviewsScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(Screen.ViewReviews.ARG_PROPERTY_ID) ?: ""
                )
            }

            // ── Profile Screens ───────────────────────────────────────────────
            composable(Screen.Profile.route)     { ProfileScreen(navController) }
            composable(Screen.EditProfile.route) { EditProfileScreen(navController) }

            // ── Favourites ────────────────────────────────────────────────────
            composable(Screen.Favourites.route) { FavouritesScreen(navController) }

            // ── Settings Screens ──────────────────────────────────────────────
            composable(Screen.Settings.route)             { SettingsScreen(navController) }
            composable(Screen.AccountSettings.route)      { AccountSettingsScreen(navController) }
            composable(Screen.NotificationSettings.route) { NotificationSettingsScreen(navController) }
            composable(Screen.PrivacySettings.route)      { PrivacySettingsScreen(navController) }
            composable(Screen.About.route)                { AboutScreen(navController) }
            composable(Screen.HelpAndSupport.route)       { HelpAndSupportScreen(navController) }

            // ── Notification Screens ──────────────────────────────────────────
            composable(Screen.Notifications.route) { NotificationsScreen(navController) }

            composable(
                route     = Screen.NotificationDetail.route,
                arguments = listOf(navArgument(Screen.NotificationDetail.ARG_NOTIFICATION_ID) {
                    type = NavType.StringType
                })
            ) { back ->
                NotificationDetailScreen(
                    navController  = navController,
                    notificationId = back.arguments?.getString(Screen.NotificationDetail.ARG_NOTIFICATION_ID) ?: ""
                )
            }

            // ── Messaging Screens ─────────────────────────────────────────────
            composable(Screen.MessageList.route) { MessageListScreen(navController) }

            // Chat screen encodes the property ID as "none" when there is no
            // associated property; the sentinel is decoded back to an empty string
            // before being forwarded to the screen.
            composable(
                route     = Screen.Chat.route,
                arguments = listOf(
                    navArgument(Screen.Chat.ARG_USER_ID)     { type = NavType.StringType },
                    navArgument(Screen.Chat.ARG_OWNER_NAME)  {
                        type         = NavType.StringType
                        defaultValue = "Owner"
                    },
                    navArgument(Screen.Chat.ARG_PROPERTY_ID) {
                        type         = NavType.StringType
                        defaultValue = "none"
                    }
                )
            ) { back ->
                val rawPropertyId = back.arguments?.getString(Screen.Chat.ARG_PROPERTY_ID) ?: ""
                ChatScreen(
                    navController = navController,
                    userId        = back.arguments?.getString(Screen.Chat.ARG_USER_ID)    ?: "",
                    ownerName     = back.arguments?.getString(Screen.Chat.ARG_OWNER_NAME) ?: "Owner",
                    // Convert the "none" sentinel back to an empty string.
                    propertyId    = if (rawPropertyId == "none") "" else rawPropertyId,
                    currentUserId = currentUserId
                )
            }

            // ── Vacation Rentals / Pre-Booking ────────────────────────────────
            // VacationRentals accepts optional `season` and `location` query
            // parameters so that Seasonal Alert cards can deep-link with
            // pre-applied filters.
            //
            // Both parameters default to "none" so that existing navigation
            // (BottomNavBar, HomeScreen) continues to work without any changes.
            composable(
                route     = Screen.VacationRentals.route,
                arguments = listOf(
                    navArgument(Screen.VacationRentals.ARG_SEASON) {
                        type         = NavType.StringType
                        defaultValue = "none"   // No season filter by default.
                    },
                    navArgument(Screen.VacationRentals.ARG_LOCATION) {
                        type         = NavType.StringType
                        defaultValue = "none"   // No city pre-filter by default.
                    }
                )
            ) { back ->
                // Decode the "none" sentinel back to an empty string so that
                // VacationRentalsScreen can simply check `initialSeason.isEmpty()`.
                val rawSeason   = back.arguments?.getString(Screen.VacationRentals.ARG_SEASON)   ?: "none"
                val rawLocation = back.arguments?.getString(Screen.VacationRentals.ARG_LOCATION) ?: "none"

                VacationRentalsScreen(
                    navController   = navController,
                    initialSeason   = if (rawSeason   == "none") "" else rawSeason,
                    initialLocation = if (rawLocation == "none") "" else rawLocation
                )
            }

            // PreBooking — tenant selects a rental package and pays a 20% deposit.
            composable(
                route     = Screen.PreBooking.route,
                arguments = listOf(navArgument(Screen.PreBooking.ARG_PROPERTY_ID) {
                    type         = NavType.StringType
                    defaultValue = ""
                })
            ) { back ->
                PreBookingScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(Screen.PreBooking.ARG_PROPERTY_ID) ?: ""
                )
            }

            // VacationCalendar — shows availability calendar for a specific property.
            composable(
                route     = Screen.VacationCalendar.route,
                arguments = listOf(navArgument(Screen.VacationCalendar.ARG_PROPERTY_ID) {
                    type = NavType.StringType
                })
            ) { back ->
                VacationCalendarScreen(
                    navController = navController,
                    propertyId    = back.arguments?.getString(Screen.VacationCalendar.ARG_PROPERTY_ID) ?: ""
                )
            }

            // ── Tenants (Landlord Only) ───────────────────────────────────────
            // Only landlords can view their tenant list; all other roles are
            // immediately popped back to avoid a blank screen.
            composable(Screen.Tenants.route) {
                if (isCurrentUserLandlord) {
                    TenantsScreen(navController = navController)
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // ── Admin / Sub-Admin Screens ─────────────────────────────────────

            // Admin Dashboard — accessible to both full admin and sub-admin roles.
            composable(Screen.AdminDashboard.route) {
                if (isCurrentUserAnyAdmin) AdminDashboardScreen(navController = navController)
                else LaunchedEffect(Unit) { navController.popBackStack() }
            }

            // Manage Users — guarded by the MANAGE_USERS sub-admin permission.
            composable(Screen.ManageUsers.route) {
                when {
                    canManageUsers -> ManageUsersScreen(navController)
                    else           -> LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // Manage Properties — guarded by the MANAGE_PROPERTIES sub-admin permission.
            composable(Screen.ManageProperties.route) {
                when {
                    canManageProperties -> ManagePropertiesScreen(navController)
                    else                -> LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // Manage Bookings — guarded by the MANAGE_BOOKINGS sub-admin permission.
            composable(Screen.ManageBookings.route) {
                when {
                    canManageBookings -> ManageBookingsScreen(navController)
                    else              -> LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // Verify Properties — guarded by the VERIFY_PROPERTIES sub-admin permission.
            composable(Screen.VerifyProperties.route) {
                when {
                    canVerifyProperties -> VerifyPropertiesScreen(navController)
                    else                -> LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // Verify Users — guarded by the VERIFY_USERS sub-admin permission.
            composable(Screen.VerifyUsers.route) {
                when {
                    canVerifyUsers -> VerifyUsersScreen(navController)
                    else           -> LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // Property Verification Detail — shows full property details for review.
            composable(
                route     = Screen.PropertyVerificationDetail.route,
                arguments = listOf(navArgument(Screen.PropertyVerificationDetail.ARG_PROPERTY_ID) {
                    type = NavType.StringType
                })
            ) { back ->
                when {
                    canVerifyProperties -> PropertyVerificationDetailScreen(
                        navController = navController,
                        propertyId    = back.arguments?.getString(Screen.PropertyVerificationDetail.ARG_PROPERTY_ID) ?: ""
                    )
                    else -> LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // User Verification Detail — shows full user profile for KYC review.
            composable(
                route     = Screen.UserVerificationDetail.route,
                arguments = listOf(navArgument(Screen.UserVerificationDetail.ARG_USER_ID) {
                    type = NavType.StringType
                })
            ) { back ->
                when {
                    canVerifyUsers -> UserVerificationDetailScreen(
                        navController = navController,
                        userId        = back.arguments?.getString(Screen.UserVerificationDetail.ARG_USER_ID) ?: ""
                    )
                    else -> LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // Reports — platform-level booking/revenue reports.
            composable(Screen.Reports.route) {
                when {
                    canViewReports -> ReportsScreen(navController)
                    else           -> LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // Payment Reports — detailed payment/payout reports.
            composable(Screen.PaymentReports.route) {
                when {
                    canViewPaymentReports -> PaymentReportsScreen(navController)
                    else                  -> LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // Manage Seasonal Alerts — admin creates/edits seasonal promotion alerts.
            composable(Screen.ManageSeasonalAlerts.route) {
                if (isCurrentUserAnyAdmin) ManageSeasonalAlertsScreen(navController)
                else LaunchedEffect(Unit) { navController.popBackStack() }
            }

            // Create Seasonal Alert — form to publish a new seasonal alert.
            composable(Screen.CreateSeasonalAlert.route) {
                if (isCurrentUserAnyAdmin) CreateSeasonalAlertScreen(navController)
                else LaunchedEffect(Unit) { navController.popBackStack() }
            }

        } // end NavHost
    } // end Scaffold
}
