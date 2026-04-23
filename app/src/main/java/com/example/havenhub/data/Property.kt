package com.example.havenhub.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
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
    val pt1DocumentUrl: String = "",
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

    // ✅ Firestore mein "isAvailable" field hai, isliye yahan mapping zaruri hai
    @get:PropertyName("isAvailable")
    @set:PropertyName("isAvailable")
    var available: Boolean = true,

    // ✅ Firestore mein "isFeatured" field hai, isliye yahan mapping zaruri hai
    @get:PropertyName("isFeatured")
    @set:PropertyName("isFeatured")
    var featured: Boolean = false,

    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    // Firestore serialization ke liye empty constructor
    constructor() : this(propertyId = "")

    // --- UI Helpers (Marked @Exclude so Firestore doesn't try to save them) ---

    @get:Exclude
    val coverImageUrl: String get() = imageUrls.firstOrNull() ?: ""

    @get:Exclude
    val formattedPrice: String get() = "PKR ${"%,.0f".format(pricePerNight)}"

    @get:Exclude
    val isAvailable: Boolean get() = available

    @get:Exclude
    val isFeatured: Boolean get() = featured

    @get:Exclude
    val isLive: Boolean get() = status == "APPROVED" && available

    @get:Exclude
    val hasPt1Document: Boolean get() = pt1DocumentUrl.isNotBlank()

    @get:Exclude
    val propertyStatusEnum: PropertyStatus
        get() = try {
            PropertyStatus.valueOf(status)
        } catch (e: Exception) {
            PropertyStatus.PENDING
        }

    @get:Exclude
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