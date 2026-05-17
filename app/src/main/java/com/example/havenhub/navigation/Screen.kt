package com.example.havenhub.navigation

sealed class Screen(val route: String) {

    object Splash         : Screen("splash")
    object Onboarding     : Screen("onboarding")
    object SignIn         : Screen("sign_in")
    object SignUp         : Screen("sign_up/{role}") {
        const val ARG_ROLE = "role"
        fun createRoute(role: String) = "sign_up/$role"
    }
    object RoleSelection  : Screen("role_selection")
    object ForgotPassword : Screen("forgot_password")

    object Home       : Screen("home")
    object Search     : Screen("search")
    object Filter     : Screen("filter")
    object Favourites : Screen("favourites")

    // ✦ NEW: Global Reviews Tab (bottom navbar mein Search ki jagah)
    object GlobalReviews : Screen("global_reviews")

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

    object Booking : Screen("booking/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "booking/$propertyId"
    }
    object BookingConfirmation : Screen("booking_confirmation/{bookingId}") {
        const val ARG_BOOKING_ID = "bookingId"
        fun createRoute(bookingId: String) = "booking_confirmation/$bookingId"
    }
    object MyBookings : Screen("my_bookings?tab={tab}") {
        fun createRoute(tab: Int = 0) = "my_bookings?tab=$tab"
    }
    object BookingDetails : Screen("booking_details/{bookingId}") {
        const val ARG_BOOKING_ID = "bookingId"
        fun createRoute(bookingId: String) = "booking_details/$bookingId"
    }

    object ActiveTenants : Screen("active_tenants/{landlordId}") {
        const val ARG_LANDLORD_ID = "landlordId"
        fun createRoute(landlordId: String) = "active_tenants/$landlordId"
    }

    object Payment : Screen("payment/{bookingId}/{payerId}/{payeeId}/{payerName}/{payeeName}/{amount}") {
        fun createRoute(
            bookingId : String,
            payerId   : String,
            payeeId   : String,
            payerName : String,
            payeeName : String,
            amount    : Double
        ) = "payment/$bookingId/$payerId/$payeeId/$payerName/$payeeName/$amount"
    }
    object PaymentMethod : Screen("payment_method")
    object PaymentSuccess : Screen("payment_success/{bookingId}") {
        const val ARG_BOOKING_ID = "bookingId"
        fun createRoute(bookingId: String) = "payment_success/$bookingId"
    }

    // ✦ UPDATED: AddReview — propertyId optional ho gaya
    // PropertyDetailScreen se: createRoute("abc123") → "add_review/abc123"
    // GlobalReviewsScreen se:  createRoute("")       → "add_review/"  (search mode)
    object AddReview : Screen("add_review/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "add_review/$propertyId"
    }
    object ViewReviews : Screen("view_reviews/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "view_reviews/$propertyId"
    }

    object Profile     : Screen("profile")
    object EditProfile : Screen("edit_profile")

    object Settings             : Screen("settings")
    object AccountSettings      : Screen("account_settings")
    object NotificationSettings : Screen("notification_settings")
    object PrivacySettings      : Screen("privacy_settings")
    object About                : Screen("about")
    object HelpAndSupport       : Screen("help_and_support")

    object Notifications : Screen("notifications")
    object NotificationDetail : Screen("notification_detail/{notificationId}") {
        const val ARG_NOTIFICATION_ID = "notificationId"
        fun createRoute(notificationId: String) = "notification_detail/$notificationId"
    }

    object MessageList : Screen("message_list")
    object Chat : Screen("chat/{userId}/{ownerName}/{propertyId}") {
        const val ARG_USER_ID     = "userId"
        const val ARG_OWNER_NAME  = "ownerName"
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(
            userId    : String,
            ownerName : String = "Owner",
            propertyId: String = ""
        ): String {
            val encodedName = android.net.Uri.encode(ownerName.ifEmpty { "Owner" })
            val encodedPid  = android.net.Uri.encode(propertyId.ifEmpty { "none" })
            return "chat/$userId/$encodedName/$encodedPid"
        }
    }

    object VacationRentals : Screen("vacation_rentals")

    object PreBooking : Screen("pre_booking/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "pre_booking/$propertyId"
    }

    object VacationCalendar : Screen("vacation_calendar/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "vacation_calendar/$propertyId"
    }

    object AdminDashboard   : Screen("admin_dashboard")
    object ManageUsers      : Screen("manage_users")
    object ManageProperties : Screen("manage_properties")
    object ManageBookings   : Screen("manage_bookings")

    object VerifyProperties : Screen("verify_properties")
    object VerifyUsers      : Screen("verify_users")
    object PropertyVerificationDetail : Screen("property_verification_detail/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun createRoute(propertyId: String) = "property_verification_detail/$propertyId"
    }
    object UserVerificationDetail : Screen("user_verification_detail/{userId}") {
        const val ARG_USER_ID = "userId"
        fun createRoute(userId: String) = "user_verification_detail/$userId"
    }

    object Reports        : Screen("reports")
    object PaymentReports : Screen("payment_reports")
}
