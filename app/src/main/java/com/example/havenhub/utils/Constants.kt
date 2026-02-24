package com.example.havenhub.utils
object Constants {

    // ── Firebase Collections ──────────────────────────────────────────
    const val COLLECTION_USERS = "users"
    const val COLLECTION_PROPERTIES = "properties"
    const val COLLECTION_BOOKINGS = "bookings"
    const val COLLECTION_REVIEWS = "reviews"
    const val COLLECTION_PAYMENTS = "payments"
    const val COLLECTION_NOTIFICATIONS = "notifications"
    const val COLLECTION_MESSAGES = "messages"
    const val COLLECTION_CONVERSATIONS = "conversations"
    const val COLLECTION_VERIFICATIONS = "verifications"

    // ── Firebase Storage Paths ────────────────────────────────────────
    const val STORAGE_USERS = "users"
    const val STORAGE_PROPERTIES = "properties"
    const val STORAGE_DOCUMENTS = "documents"

    // ── User Roles ────────────────────────────────────────────────────
    const val ROLE_TENANT = "TENANT"
    const val ROLE_OWNER = "OWNER"
    const val ROLE_ADMIN = "ADMIN"

    // ── User Status ───────────────────────────────────────────────────
    const val STATUS_ACTIVE = "Active"
    const val STATUS_SUSPENDED = "Suspended"
    const val STATUS_BANNED = "Banned"

    // ── Booking Status ────────────────────────────────────────────────
    const val BOOKING_PENDING = "Pending"
    const val BOOKING_CONFIRMED = "Confirmed"
    const val BOOKING_COMPLETED = "Completed"
    const val BOOKING_CANCELLED = "Cancelled"

    // ── Payment Status ────────────────────────────────────────────────
    const val PAYMENT_SUCCESS = "Success"
    const val PAYMENT_PENDING = "Pending"
    const val PAYMENT_FAILED = "Failed"

    // ── Property Types ────────────────────────────────────────────────
    const val TYPE_APARTMENT = "Apartment"
    const val TYPE_HOUSE = "House"
    const val TYPE_VILLA = "Villa"
    const val TYPE_STUDIO = "Studio"
    const val TYPE_ROOM = "Room"

    // ── Property Status ───────────────────────────────────────────────
    const val PROPERTY_ACTIVE = "Active"
    const val PROPERTY_INACTIVE = "Inactive"
    const val PROPERTY_SUSPENDED = "Suspended"

    // ── Notification Types ────────────────────────────────────────────
    const val NOTIF_BOOKING = "booking"
    const val NOTIF_PAYMENT = "payment"
    const val NOTIF_MESSAGE = "message"
    const val NOTIF_SYSTEM = "system"

    // ── Verification Status ───────────────────────────────────────────
    const val VERIFICATION_PENDING = "Pending"
    const val VERIFICATION_APPROVED = "Approved"
    const val VERIFICATION_REJECTED = "Rejected"

    // ── SharedPreferences Keys ────────────────────────────────────────
    const val PREF_USER_ID = "pref_user_id"
    const val PREF_USER_ROLE = "pref_user_role"
    const val PREF_IS_LOGGED_IN = "pref_is_logged_in"
    const val PREF_ONBOARDING_DONE = "pref_onboarding_done"
    const val PREF_DARK_MODE = "pref_dark_mode"
    const val PREF_LANGUAGE = "pref_language"
    const val PREF_PUSH_ENABLED = "pref_push_enabled"
    const val PREF_NAME = "pref_name"

    // ── Pagination ────────────────────────────────────────────────────
    const val PAGE_SIZE = 20

    // ── Image ─────────────────────────────────────────────────────────
    const val MAX_IMAGE_SIZE_KB = 500
    const val MAX_PROPERTY_IMAGES = 10

    // ── Validation ────────────────────────────────────────────────────
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_REVIEW_LENGTH = 500
    const val MAX_BIO_LENGTH = 200
    const val MIN_PROPERTY_PRICE = 500.0
}