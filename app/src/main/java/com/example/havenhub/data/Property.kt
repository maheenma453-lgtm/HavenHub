package com.example.havenhub.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Property(

    @DocumentId
    val propertyId: String = "",

    val ownerId: String = "",
    val ownerName: String = "",
    val title: String = "",
    val description: String = "",

    val propertyType: String = "APARTMENT",
    val status: String = "PENDING",

    val location: Location = Location(),
    val address: String = "",
    val city: String = "",

    val pricePerNight: Double = 0.0,
    val pricePerWeek: Double? = null,
    val pricePerMonth: Double? = null,
    val securityDeposit: Double = 0.0,

    val bedrooms: Int = 1,
    val bathrooms: Int = 1,
    val maxGuests: Int = 2,
    val areaSqFt: Double? = null,
    val floor: Int? = null,

    val imageUrls: List<String> = emptyList(),
    val pt1DocumentUrl: String = "",        // ✅ NEW: Admin ke liye PT-1 imgbb link
    val amenities: List<String> = emptyList(),

    val petsAllowed: Boolean = false,
    val smokingAllowed: Boolean = false,
    val partiesAllowed: Boolean = false,

    val checkInTime: String = "14:00",
    val checkOutTime: String = "11:00",
    val minNights: Int = 1,

    val averageRating: Float = 0f,
    val reviewCount: Int = 0,

    val adminNote: String = "",
    val isAvailable: Boolean = true,
    val isFeatured: Boolean = false,

    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null

) {
    constructor() : this(propertyId = "")

    val coverImageUrl: String get() = imageUrls.firstOrNull() ?: ""

    val formattedPrice: String get() = "PKR ${"%,.0f".format(pricePerNight)}"

    val isLive: Boolean get() = status == "APPROVED" && isAvailable

    // ✅ PT-1 uploaded hai ya nahi
    val hasPt1Document: Boolean get() = pt1DocumentUrl.isNotBlank()

    val propertyStatusEnum: PropertyStatus
        get() = try {
            PropertyStatus.valueOf(status)
        } catch (e: Exception) {
            PropertyStatus.PENDING
        }

    val propertyTypeEnum: PropertyType
        get() = try {
            PropertyType.valueOf(propertyType)
        } catch (e: Exception) {
            PropertyType.APARTMENT
        }
}

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class PropertyType {
    APARTMENT, HOUSE, VILLA, STUDIO, ROOM, HOSTEL, PENTHOUSE, FARMHOUSE;

    fun displayName(): String = when (this) {
        APARTMENT -> "Apartment"
        HOUSE     -> "House"
        VILLA     -> "Villa"
        STUDIO    -> "Studio"
        ROOM      -> "Room"
        HOSTEL    -> "Hostel"
        PENTHOUSE -> "Penthouse"
        FARMHOUSE -> "Farmhouse"
    }
}

enum class PropertyStatus {
    PENDING, UNDER_REVIEW, APPROVED, REJECTED, INACTIVE;

    fun displayName(): String = when (this) {
        PENDING      -> "Pending"
        UNDER_REVIEW -> "Under Review"
        APPROVED     -> "Approved"
        REJECTED     -> "Rejected"
        INACTIVE     -> "Inactive"
    }
}