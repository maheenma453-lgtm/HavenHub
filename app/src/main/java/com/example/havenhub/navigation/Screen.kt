package com.example.havenhub.navigation

sealed class Screen(val route: String) {

    // ── 1. AUTH ──────────────────────────────────────────────────────────────
    object Splash         : Screen("splash")
    object Onboarding     : Screen("onboarding")
    object SignIn         : Screen("sign_in")
    object SignUp         : Screen("sign_up")
    object RoleSelection  : Screen("role_selection")
    object ForgotPassword : Screen("forgot_password")

    // ── 2. MAIN / CORE ───────────────────────────────────────────────────────
    object Home   : Screen("home")
    object Search : Screen("search")
    object Filter : Screen("filter")

    // ── 3. PROPERTY ──────────────────────────────────────────────────────────
    object PropertyList : Screen("property_list")

    object PropertyDetail : Screen("property_detail/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "property_detail/$propertyId"
    }

    object AddProperty  : Screen("add_property")
    object MyProperties : Screen("my_properties")

    object EditProperty : Screen("edit_property/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "edit_property/$propertyId"
    }

    // ── 4. BOOKING ───────────────────────────────────────────────────────────
    object Booking : Screen("booking/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "booking/$propertyId"
    }

    object BookingConfirmation : Screen("booking_confirmation/{bookingId}") {
        const val ARG_BOOKING_ID = "bookingId"
        fun createRoute(bookingId: String) = "booking_confirmation/$bookingId"
    }

    object MyBookings : Screen("my_bookings")

    object BookingDetails : Screen("booking_details/{bookingId}") {
        const val ARG_BOOKING_ID = "bookingId"
        fun createRoute(bookingId: String) = "booking_details/$bookingId"
    }

    // ── 5. PAYMENT ───────────────────────────────────────────────────────────
    object Payment : Screen("payment/{bookingId}") {
        const val ARG_BOOKING_ID = "bookingId"
        fun createRoute(bookingId: String) = "payment/$bookingId"
    }

    object PaymentMethod : Screen("payment_method/{bookingId}/{method}") {
        const val ARG_BOOKING_ID = "bookingId"
        const val ARG_METHOD     = "method"
        fun createRoute(bookingId: String, method: String) =
            "payment_method/$bookingId/$method"
    }

    object PaymentSuccess : Screen("payment_success/{bookingId}") {
        const val ARG_BOOKING_ID = "bookingId"
        fun createRoute(bookingId: String) = "payment_success/$bookingId"
    }

    // ── 6. REVIEW ────────────────────────────────────────────────────────────
    object AddReview : Screen("add_review/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "add_review/$propertyId"
    }

    object ViewReviews : Screen("view_reviews/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "view_reviews/$propertyId"
    }

    // ── 7. PROFILE ───────────────────────────────────────────────────────────
    object Profile     : Screen("profile")
    object EditProfile : Screen("edit_profile")

    // ── 8. SETTINGS ──────────────────────────────────────────────────────────
    object Settings             : Screen("settings")
    object AccountSettings      : Screen("account_settings")
    object NotificationSettings : Screen("notification_settings")
    object PrivacySettings      : Screen("privacy_settings")
    object About                : Screen("about")
    object HelpAndSupport       : Screen("help_and_support")

    // ── 9. NOTIFICATIONS ─────────────────────────────────────────────────────
    object Notifications : Screen("notifications")

    object NotificationDetail : Screen("notification_detail/{notificationId}") {
        const val ARG_NOTIFICATION_ID = "notificationId"
        fun createRoute(notificationId: String) =
            "notification_detail/$notificationId"
    }

    // ── 10. MESSAGING ────────────────────────────────────────────────────────
    object MessageList : Screen("message_list")

    object Chat : Screen("chat/{userId}") {
        const val ARG_USER_ID = "userId"
        fun createRoute(userId: String) = "chat/$userId"
    }

    // ── 11. VACATION ─────────────────────────────────────────────────────────
    object VacationRentals : Screen("vacation_rentals")
    object PreBooking      : Screen("pre_booking")

    object VacationCalendar : Screen("vacation_calendar/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "vacation_calendar/$propertyId"
    }

    // ── 12. ADMIN — DASHBOARD ────────────────────────────────────────────────
    object AdminDashboard : Screen("admin_dashboard")

    // ── 13. ADMIN — VERIFICATION ─────────────────────────────────────────────
    object VerifyProperties : Screen("verify_properties")
    object VerifyUsers      : Screen("verify_users")

    object PropertyVerificationDetail :
        Screen("property_verification_detail/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) =
            "property_verification_detail/$propertyId"
    }

    object UserVerificationDetail :
        Screen("user_verification_detail/{userId}") {
        const val ARG_USER_ID = "userId"
        fun createRoute(userId: String) =
            "user_verification_detail/$userId"
    }

    // ── 14. ADMIN — MANAGEMENT ───────────────────────────────────────────────
    object ManageUsers      : Screen("manage_users")
    object ManageProperties : Screen("manage_properties")
    object ManageBookings   : Screen("manage_bookings")

    // ── 15. ADMIN — REPORTS ──────────────────────────────────────────────────
    object Reports        : Screen("reports")
    object PaymentReports : Screen("payment_reports")
}
