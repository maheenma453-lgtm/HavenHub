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

    // Firestore se aayega — manually set drawable name
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

    @get:PropertyName("isAvailable")
    @set:PropertyName("isAvailable")
    var available: Boolean = true,

    @get:PropertyName("isFeatured")
    @set:PropertyName("isFeatured")
    var featured: Boolean = false,

    @ServerTimestamp
    val createdAt: Timestamp? = null,

    val updatedAt: Timestamp? = null

) {
    constructor() : this(propertyId = "")

    @get:Exclude
    val coverImageUrl: String get() = imageUrls.firstOrNull() ?: ""

    @get:Exclude
    val formattedPrice: String get() = "PKR ${"%,.0f".format(pricePerNight)}"

    @get:Exclude
    val isAvailable: Boolean get() = available

    @get:Exclude
    val isFeatured: Boolean get() = featured

    val hasPt1Document: Boolean get() = pt1DocumentUrl.isNotBlank()

    // ✅ FIXED: Sab drawable file names ab actual files se match karte hain
    // Drawable folder mein jo exact naam hain wahi use kiye hain
    val resolvedDrawableName: String
        get() {
            // Agar Firestore mein drawableImageName set hai toh wahi use karo
            if (drawableImageName.isNotEmpty()) return drawableImageName

            val c = city.lowercase().trim()
            val t = propertyType.lowercase().trim()

            // Title bhi check karo — agar city field empty ho
            val titleLower = title.lowercase().trim()

            // City ya title se match karo
            val searchText = if (c.isNotEmpty()) c else titleLower

            return when {
                // ── Lahore ──────────────────────────────────────────────────
                searchText.contains("lahore") && t == "apartment"
                    -> "apartment_lahore"           // ✅ apartment_lahore.jpg

                // ── Rawalpindi ──────────────────────────────────────────────
                searchText.contains("rawalpindi") || searchText.contains("pindi")
                    -> "rawalpindi_apt"             // ✅ rawalpindi_apt.jpg  (was: apartment_rawalpindi)

                // ── Karachi ─────────────────────────────────────────────────
                searchText.contains("karachi")
                    -> "house_karachi"              // ✅ house_karachi.jpg

                // ── Kaghan ──────────────────────────────────────────────────
                searchText.contains("kaghan")
                    -> "kaghan_valleyhouse"         // ✅ kaghan_valleyhouse.jpg (was: house_kaghanvalley)

                // ── Hunza ───────────────────────────────────────────────────
                searchText.contains("hunza")
                    -> "farmhouse_hunza"            // ✅ farmhouse_hunza.jpg  (was: hunza_farmhouse)

                // ── Naran ───────────────────────────────────────────────────
                searchText.contains("naran")
                    -> "farmhouse_naran"            // ✅ farmhouse_naran.jpg  (was: naran_farmhouse)

                // ── Skardu ──────────────────────────────────────────────────
                searchText.contains("skardu")
                    -> "skardu_house"               // ✅ skardu_house.jpg     (was: skardu)

                // ── Swat ────────────────────────────────────────────────────
                searchText.contains("swat") && t == "villa"
                    -> "swat_vila"                  // ✅ swat_vila.jpg        (was: swat_villa)

                // ── Murree ──────────────────────────────────────────────────
                searchText.contains("murree") || searchText.contains("murri")
                    -> "vila_murree"                // ✅ vila_murree.jpg

                // ── Islamabad ───────────────────────────────────────────────
                searchText.contains("islamabad") && t == "room"
                    -> "room_islamabad"             // ✅ room_islamabad.jpg

                // ── Sialkot ─────────────────────────────────────────────────
                searchText.contains("sialkot")
                    -> "room_sialkot"               // ✅ room_sialkot.jpg

                // ── Faisalabad ──────────────────────────────────────────────
                searchText.contains("faisalabad") || searchText.contains("faislabad")
                    -> "studio_faislabad"           // ✅ studio_faislabad.jpg (note: typo in filename)

                else    -> ""
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