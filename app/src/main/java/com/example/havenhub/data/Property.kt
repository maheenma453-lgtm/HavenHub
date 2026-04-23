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

    // ✅ NEW: Drawable image name field (Firestore se aayega)
    val drawableImageName: String = "",

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

    val updatedAt: Long? = null

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

    val hasPt1Document: Boolean get() = pt1DocumentUrl.isNotBlank()

    // ✅ NEW: Auto drawable resolve — agar drawableImageName empty ho
    // toh city aur propertyType se guess karo
    val resolvedDrawableName: String

        get() {
            if (drawableImageName.isNotEmpty()) return drawableImageName
            // City + Type se match karo
            val c = city.lowercase().trim()
            val t = propertyType.lowercase().trim()
            return when {
                c.contains("lahore")     && t == "apartment"  -> "apartment_lahore"
                c.contains("rawalpindi") && t == "apartment"  -> "apartment_rawalpindi"
                c.contains("karachi")    && t == "house"      -> "house_karachi"
                c.contains("kaghan")                          -> "house_kaghanvalley"
                c.contains("hunza")                           -> "hunza_farmhouse"
                c.contains("naran")                           -> "naran_farmhouse"
                c.contains("skardu")                          -> "skardu"
                c.contains("swat")       && t == "villa"      -> "swat_villa"
                c.contains("murree")     || c.contains("murri") -> "vila_murree"
                c.contains("islamabad")  && t == "room"       -> "room_islamabad"
                c.contains("sialkot")    && t == "room"       -> "room_sialkot"
                c.contains("faisalabad") && t == "studio"     -> "studio_faisalabad"
                else                                          -> ""
            }
        }

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