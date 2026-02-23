package com.example.havenhub.navigation
// ═══════════════════════════════════════════════════════════════════════════
// FILE     : Screen.kt
// PACKAGE  : com.havenhub.ui.navigation
//
// PURPOSE  : Single source of truth for ALL navigation routes in HavenHub.
//            Every screen across the app is declared as a sealed-class object
//            so routes are type-safe, refactor-friendly, and impossible to
//            mistype as raw strings anywhere in the codebase.
//
// PATTERN  :
//   ┌─────────────────────────────────────────────────────────────────┐
//   │  No parameters  →  plain object with route string               │
//   │  Has parameters →  {argName} in route + createRoute() helper    │
//   │  ARG_* consts   →  reused in NavGraph navArgument(key) calls    │
//   └─────────────────────────────────────────────────────────────────┘
//
// HOW TO USE:
//   // Navigate to a screen with no arguments
//   navController.navigate(Screen.Home.route)
//
//   // Navigate to a screen with arguments
//   navController.navigate(Screen.PropertyDetail.createRoute("abc123"))
//
//   // Declare argument in NavGraph composable block
//   navArgument(Screen.PropertyDetail.ARG_PROPERTY_ID) {
//       type = NavType.StringType
//   }
//
// SECTIONS (15 groups):
//   1.  Auth               6.  Review          11. Vacation
//   2.  Main / Core        7.  Profile         12. Admin — Dashboard
//   3.  Property           8.  Settings        13. Admin — Verification
//   4.  Booking            9.  Notifications   14. Admin — Management
//   5.  Payment           10.  Messaging       15. Admin — Reports
// ═══════════════════════════════════════════════════════════════════════════

sealed class Screen(val route: String) {

    // ───────────────────────────────────────────────────────────────────────
    // 1. AUTH FLOW
    //
    //  Back-stack rule: after the user reaches Home, the full auth back-stack
    //  is cleared (popUpTo(Screen.Splash.route) { inclusive = true }) so the
    //  Back button never returns to a login screen.
    //
    //  Flow:
    //    Cold start → Splash → Onboarding (first time only)
    //              → SignIn ←→ SignUp → RoleSelection → Home
    //              → ForgotPassword  (side-branch from SignIn)
    // ───────────────────────────────────────────────────────────────────────

    /** Animated logo screen. Auto-navigates after 2.5 s. */
    object Splash : Screen("splash")

    /** 3-page HorizontalPager intro. Shown only when onboarding flag = false. */
    object Onboarding : Screen("onboarding")

    /** Email + password login. Role-aware redirect after success. */
    object SignIn : Screen("sign_in")

    /** New account form — name, email, phone, password, confirm password. */
    object SignUp : Screen("sign_up")

    /** Post-signup role picker: USER or PROPERTY_OWNER. */
    object RoleSelection : Screen("role_selection")

    /** Sends Firebase password-reset link to user's email. */
    object ForgotPassword : Screen("forgot_password")


    // ───────────────────────────────────────────────────────────────────────
    // 2. MAIN / CORE SCREENS
    //
    //  These are the root-level destinations shown in BottomNavBar.
    //  Filter is a full-screen destination launched from SearchScreen,
    //  not a bottom-bar item.
    // ───────────────────────────────────────────────────────────────────────

    /** Dashboard — featured properties, category chips, owner shortcuts. */
    object Home : Screen("home")

    /** Live search with recent searches and popular city chips. */
    object Search : Screen("search")

    /** Advanced filter — city, type, price slider, amenities, duration. */
    object Filter : Screen("filter")


    // ───────────────────────────────────────────────────────────────────────
    // 3. PROPERTY SCREENS
    //
    //  All roles  : browse PropertyList, view PropertyDetail
    //  OWNER only : AddProperty, MyProperties, EditProperty
    // ───────────────────────────────────────────────────────────────────────

    /** Scrollable listing with sort dropdown and grid/list toggle. */
    object PropertyList : Screen("property_list")

    /** Gallery, amenities, owner card, sticky Book Now button. */
    object PropertyDetail : Screen("property_detail/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "property_detail/$propertyId"
    }

    /** Owner form to list a new property with photos and pricing. */
    object AddProperty : Screen("add_property")

    /** Owner's own listings with verification status badges. */
    object MyProperties : Screen("my_properties")

    /** Edit an existing property — pre-fills all fields from Firestore. */
    object EditProperty : Screen("edit_property/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "edit_property/$propertyId"
    }


    // ───────────────────────────────────────────────────────────────────────
    // 4. BOOKING SCREENS
    //
    //  Flow:
    //    PropertyDetail → Booking → BookingConfirmation
    //                             ├── Pay Now   → Payment
    //                             └── Pay Later → MyBookings
    //    MyBookings → BookingDetails (read-only)
    // ───────────────────────────────────────────────────────────────────────

    /** Package selection, date pickers, guest count, live price breakdown. */
    object Booking : Screen("booking/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "booking/$propertyId"
    }

    /** Animated checkmark + booking summary + Pay Now / Pay Later buttons. */
    object BookingConfirmation : Screen("booking_confirmation/{bookingId}") {
        const val ARG_BOOKING_ID = "bookingId"
        fun createRoute(bookingId: String) = "booking_confirmation/$bookingId"
    }

    /** Tabbed list: Active | Pending | Completed | Cancelled. */
    object MyBookings : Screen("my_bookings")

    /** Read-only detail of a single booking with all metadata. */
    object BookingDetails : Screen("booking_details/{bookingId}") {
        const val ARG_BOOKING_ID = "bookingId"
        fun createRoute(bookingId: String) = "booking_details/$bookingId"
    }


    // ───────────────────────────────────────────────────────────────────────
    // 5. PAYMENT SCREENS
    //
    //  Flow:
    //    BookingConfirmation
    //      → Payment       (method selection: JazzCash / EasyPaisa / Cash)
    //      → PaymentMethod (OTP entry — skipped for Cash on Arrival)
    //      → PaymentSuccess (receipt + transaction ID)
    // ───────────────────────────────────────────────────────────────────────

    /** Payment method selection with booking summary banner and amount. */
    object Payment : Screen("payment/{bookingId}") {
        const val ARG_BOOKING_ID = "bookingId"
        fun createRoute(bookingId: String) = "payment/$bookingId"
    }

    /**
     * Phone number input + OTP verification for mobile wallets.
     * [method] must be "JAZZCASH" or "EASYPAISA"
     */
    object PaymentMethod : Screen("payment_method/{bookingId}/{method}") {
        const val ARG_BOOKING_ID = "bookingId"
        const val ARG_METHOD     = "method"
        fun createRoute(bookingId: String, method: String) =
            "payment_method/$bookingId/$method"
    }

    /** Animated success screen with receipt card and transaction ID. */
    object PaymentSuccess : Screen("payment_success/{bookingId}") {
        const val ARG_BOOKING_ID = "bookingId"
        fun createRoute(bookingId: String) = "payment_success/$bookingId"
    }


    // ───────────────────────────────────────────────────────────────────────
    // 6. REVIEW SCREENS
    //
    //  AddReview: only shown when ReviewViewModel.canReview == true,
    //  meaning the current user has a COMPLETED booking for that property.
    // ───────────────────────────────────────────────────────────────────────

    /** Star rating per category + 500-char written review textarea. */
    object AddReview : Screen("add_review/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "add_review/$propertyId"
    }

    /** All reviews with distribution bars and Write-Review FAB. */
    object ViewReviews : Screen("view_reviews/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "view_reviews/$propertyId"
    }


    // ───────────────────────────────────────────────────────────────────────
    // 7. PROFILE SCREENS
    // ───────────────────────────────────────────────────────────────────────

    /** Stats, role badge, menu links, logout with confirmation dialog. */
    object Profile : Screen("profile")

    /** Edit name, phone, city, bio and profile photo. Email is read-only. */
    object EditProfile : Screen("edit_profile")


    // ───────────────────────────────────────────────────────────────────────
    // 8. SETTINGS SCREENS
    //
    //  SettingsScreen is the hub. Each sub-screen handles one concern and
    //  is reachable only by navigating from SettingsScreen.
    // ───────────────────────────────────────────────────────────────────────

    /** Main settings hub with grouped navigation rows. */
    object Settings : Screen("settings")

    /** Change password + Two-Factor Authentication toggle. */
    object AccountSettings : Screen("account_settings")

    /** Push, booking alerts, chat alerts, promo emails, SMS toggles. */
    object NotificationSettings : Screen("notification_settings")

    /** Profile visibility, location sharing, analytics opt-out, data export. */
    object PrivacySettings : Screen("privacy_settings")

    /** App version, Terms of Service, Privacy Policy, Open Source Licenses. */
    object About : Screen("about")

    /** FAQ accordion + email/live-chat support contact options. */
    object HelpAndSupport : Screen("help_and_support")


    // ───────────────────────────────────────────────────────────────────────
    // 9. NOTIFICATION SCREENS
    // ───────────────────────────────────────────────────────────────────────

    /** Tabbed inbox: All | Bookings | Payments | Messages + Mark All Read. */
    object Notifications : Screen("notifications")

    /** Full notification body with deep-link action button. */
    object NotificationDetail : Screen("notification_detail/{notificationId}") {
        const val ARG_NOTIFICATION_ID = "notificationId"
        fun createRoute(notificationId: String) =
            "notification_detail/$notificationId"
    }


    // ───────────────────────────────────────────────────────────────────────
    // 10. MESSAGING SCREENS
    //
    //  Real-time chat powered by Firebase Realtime Database.
    //  Supports text messages, image attachments, timestamps, read receipts.
    // ───────────────────────────────────────────────────────────────────────

    /** All conversations with unread count badges and online-status dots. */
    object MessageList : Screen("message_list")

    /** One-to-one chat — right bubbles = sent (blue), left = received (gray). */
    object Chat : Screen("chat/{userId}") {
        const val ARG_USER_ID = "userId"
        fun createRoute(userId: String) = "chat/$userId"
    }


    // ───────────────────────────────────────────────────────────────────────
    // 11. VACATION SCREENS
    //
    //  Northern areas advance pre-booking (up to 6 months ahead).
    //  Users pay 20% deposit now; remainder is collected on arrival.
    // ───────────────────────────────────────────────────────────────────────

    /** Destination cards (Murree, Swat, Hunza …) + available property list. */
    object VacationRentals : Screen("vacation_rentals")

    /** Advance booking form — destination, dates, type, guests, deposit. */
    object PreBooking : Screen("pre_booking")

    /** Monthly calendar with booked (red) / available (green) day cells. */
    object VacationCalendar : Screen("vacation_calendar/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "vacation_calendar/$propertyId"
    }


    // ───────────────────────────────────────────────────────────────────────
    // 12. ADMIN — DASHBOARD
    //
    //  Guard: NavGraph checks Firebase role == "ADMIN" before composing this
    //  destination. Non-admins are redirected to Home.
    // ───────────────────────────────────────────────────────────────────────

    /** KPI stat cards, pending-verification alert, quick-action grid (6 tiles). */
    object AdminDashboard : Screen("admin_dashboard")


    // ───────────────────────────────────────────────────────────────────────
    // 13. ADMIN — VERIFICATION
    //
    //  Property verification: owners upload PT-1 document.
    //  User verification:     owners upload CNIC image.
    //  Admin can Approve, Reject, or request more information.
    // ───────────────────────────────────────────────────────────────────────

    /** Tabbed property list: Pending | Approved | Rejected + inline actions. */
    object VerifyProperties : Screen("verify_properties")

    /** Tabbed user list: Pending | Verified | Rejected + inline actions. */
    object VerifyUsers : Screen("verify_users")

    /** Full property verification detail — document preview + action buttons. */
    object PropertyVerificationDetail :
        Screen("property_verification_detail/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) =
            "property_verification_detail/$propertyId"
    }

    /** Full user verification detail — CNIC image + verify/reject buttons. */
    object UserVerificationDetail :
        Screen("user_verification_detail/{userId}") {
        const val ARG_USER_ID = "userId"
        fun createRoute(userId: String) =
            "user_verification_detail/$userId"
    }


    // ───────────────────────────────────────────────────────────────────────
    // 14. ADMIN — MANAGEMENT
    //
    //  Full CRUD control over platform content.
    // ───────────────────────────────────────────────────────────────────────

    /** Search + role-filter all users; ban / unban / reset password menu. */
    object ManageUsers : Screen("manage_users")

    /** Search all listings; remove or toggle listing visibility. */
    object ManageProperties : Screen("manage_properties")

    /** All bookings with 5 status tabs; admin can cancel any booking. */
    object ManageBookings : Screen("manage_bookings")


    // ───────────────────────────────────────────────────────────────────────
    // 15. ADMIN — REPORTS
    // ───────────────────────────────────────────────────────────────────────

    /** Analytics hub: KPI cards, monthly bar chart, top cities, period filter. */
    object Reports : Screen("reports")

    /** Payment transaction history filterable by method and status. */
    object PaymentReports : Screen("payment_reports")
}


