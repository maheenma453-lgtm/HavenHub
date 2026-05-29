package com.example.havenhub.data

import com.google.firebase.Timestamp

// ─────────────────────────────────────────────────────────────────────────────
// SeasonalAlert.kt
//
// Represents a seasonal / promotional alert created by Admin in Firestore.
// Collection: "seasonal_alerts"
//
// NEW FIELDS added for clickable card navigation:
//   filterTag      → matches season tag on VacationRentalsScreen filter chips
//                    e.g. "Eid", "Winter", "Summer"
//                    Falls back to `season` field if empty (backward compatible)
//   targetLocation → optional city pre-filter on VacationRentalsScreen
//                    e.g. "North Areas", "Hunza", "Naran"
//                    Empty = show all cities (no pre-filter)
// ─────────────────────────────────────────────────────────────────────────────

data class SeasonalAlert(
    val alertId        : String     = "",       // Firestore document ID
    val title          : String     = "",       // e.g. "Eid ul Adha Special!"
    val message        : String     = "",       // Full alert message body
    val season         : String     = "",       // e.g. "Eid", "Summer", "Winter", "Holidays"
    val iconEmoji      : String     = "🎉",     // Emoji shown on the alert card
    val targetRole     : String     = "both",   // "landlord" | "tenant" | "both"
    val isActive       : Boolean    = true,     // Admin can deactivate without deleting
    val startDate      : Timestamp? = null,     // Alert visible from this date
    val endDate        : Timestamp? = null,     // Alert stops showing after this date
    val createdAt      : Timestamp? = null,     // When admin created this alert
    val updatedAt      : Timestamp? = null,     // Last modification timestamp

    // ── Navigation fields (NEW) ───────────────────────────────────────────────
    // filterTag: season tag used to pre-filter VacationRentalsScreen
    // Falls back to `season` if left empty — existing Firestore docs still work
    val filterTag      : String     = "",       // e.g. "Eid", "Winter"

    // targetLocation: city/area to pre-select in VacationRentalsScreen city chips
    // Empty string = no city pre-filter (shows "All")
    val targetLocation : String     = ""        // e.g. "Hunza", "Naran", "North Areas"
)











