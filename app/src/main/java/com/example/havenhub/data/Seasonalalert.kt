package com.example.havenhub.data

import com.google.firebase.Timestamp

// ─────────────────────────────────────────────────────────────────────────────
// SeasonalAlert.kt
//
// Represents a seasonal / promotional alert created by the Admin in Firestore.
// These alerts are pushed to landlords, tenants, or both based on targetRole.
//
// Firestore collection: "seasonal_alerts"
//
// Example documents:
//   eid_ul_adha_2026  → Eid holidays alert for landlords to list properties
//   summer_2026       → Summer vacation alert for tenants to book now
// ─────────────────────────────────────────────────────────────────────────────

data class SeasonalAlert(
    val alertId      : String    = "",          // Firestore document ID
    val title        : String    = "",          // e.g. "Eid ul Adha Special!"
    val message      : String    = "",          // Full alert message body
    val season       : String    = "",          // e.g. "Eid", "Summer", "Winter", "Holidays"
    val iconEmoji    : String    = "🎉",        // Emoji shown in notification card
    val targetRole   : String    = "both",      // "landlord" | "tenant" | "both"
    val isActive     : Boolean   = true,        // Admin can deactivate without deleting
    val startDate    : Timestamp? = null,       // Alert becomes visible from this date
    val endDate      : Timestamp? = null,       // Alert stops showing after this date
    val createdAt    : Timestamp? = null,       // When admin created this alert
    val updatedAt    : Timestamp? = null        // Last modification timestamp
)

// ─────────────────────────────────────────────────────────────────────────────
// NotificationType extension — add SEASONAL_ALERT to your existing enum:
//
// In your NotificationType enum file, add:
//   SEASONAL_ALERT
//
// This file is just the model — the enum addition must go in the file
// where NotificationType is defined (likely Notification.kt or a separate enum file).
// ────────────────────────────────────────────────