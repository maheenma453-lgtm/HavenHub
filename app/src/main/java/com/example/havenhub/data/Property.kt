package com.example.havenhub.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

// ══════════════════════════════════════════════════════════════════════════════
// Property.kt
// Central data model for a property listing in HavenHub.
// Includes Firestore mapping, coordinate resolution, and display helpers.
// ══════════════════════════════════════════════════════════════════════════════

data class Property(

    // ── Firestore document ID (auto-populated by Firestore) ───────────────────
    @DocumentId
    val propertyId: String = "",

    // ── Owner info ────────────────────────────────────────────────────────────
    val ownerId   : String = "",
    val ownerName : String = "",

    // ── Core listing details ──────────────────────────────────────────────────
    val title       : String = "",
    val description : String = "",

    // ── Property classification ───────────────────────────────────────────────
    // Stored as String in Firestore (e.g. "APARTMENT"), converted via propertyTypeEnum
    val propertyType : String = "APARTMENT",

    // ── Admin review status ───────────────────────────────────────────────────
    // Values: PENDING | UNDER_REVIEW | APPROVED | REJECTED | INACTIVE | BOOKED
    val status : String = "PENDING",

    // ── Location ──────────────────────────────────────────────────────────────
    val location : Location = Location(),
    val address  : String   = "",
    val city     : String   = "",

    // ── Map coordinates ───────────────────────────────────────────────────────
    // These are saved when a landlord submits a property via AddPropertyScreen.
    // PropertyViewModel resolves the city name to lat/lng before saving.
    // ExploreMapScreen reads resolvedLatitude / resolvedLongitude (see below).
    val latitude  : Double = 0.0,
    val longitude : Double = 0.0,

    // ── Pricing ───────────────────────────────────────────────────────────────
    val pricePerNight   : Double  = 0.0,
    val pricePerWeek    : Double? = null,
    val pricePerMonth   : Double? = null,
    val securityDeposit : Double  = 0.0,

    // ── Physical details ──────────────────────────────────────────────────────
    val bedrooms  : Int     = 1,
    val bathrooms : Int     = 1,
    val maxGuests : Int     = 2,
    val areaSqFt  : Double? = null,
    val floor     : Int?    = null,

    // ── Media ─────────────────────────────────────────────────────────────────
    // imageUrls: uploaded to ImgBB, stored as public URLs
    val imageUrls      : List<String> = emptyList(),
    // pt1DocumentUrl: PT-1 property tax document image URL
    val pt1DocumentUrl : String       = "",
    // drawableImageName: local res/drawable name (for manually seeded properties only)
    val drawableImageName : String    = "",

    // ── Amenities ─────────────────────────────────────────────────────────────
    val amenities : List<String> = emptyList(),

    // ── House rules ───────────────────────────────────────────────────────────
    val petsAllowed    : Boolean = false,
    val smokingAllowed : Boolean = false,
    val partiesAllowed : Boolean = false,
    val checkInTime    : String  = "14:00",
    val checkOutTime   : String  = "11:00",
    val minNights      : Int     = 1,

    // ── Ratings & reviews ─────────────────────────────────────────────────────
    val averageRating : Float = 0f,
    val reviewCount   : Int   = 0,

    // ── Admin note ────────────────────────────────────────────────────────────
    // Set by admin when approving or rejecting a property
    val adminNote : String = "",

    // ── Availability & featured flags ─────────────────────────────────────────
    // NOTE: `available` is the raw Firestore field ("isAvailable").
    // Use isAvailable (computed below) in UI — it also accounts for status.
    @get:PropertyName("isAvailable")
    @set:PropertyName("isAvailable")
    var available : Boolean = true,

    @get:PropertyName("isFeatured")
    @set:PropertyName("isFeatured")
    var featured : Boolean = false,

    // ── Timestamps ────────────────────────────────────────────────────────────
    @ServerTimestamp
    val createdAt : Timestamp? = null,
    val updatedAt : Timestamp? = null

) {

    // Required by Firestore for deserialization (no-arg constructor)
    constructor() : this(propertyId = "")

    // ════════════════════════════════════════════════════════════════════════
    // COMPUTED DISPLAY HELPERS  (@Exclude = not saved to Firestore)
    // ════════════════════════════════════════════════════════════════════════

    @get:Exclude
    val coverImageUrl: String
        get() = imageUrls.firstOrNull() ?: ""

    @get:Exclude
    val formattedPrice: String
        get() = "PKR ${"%,.0f".format(pricePerNight)}"

    // ════════════════════════════════════════════════════════════════════════
    // ✅ FIX: isAvailable — now checks BOTH the raw `available` field
    //          AND the admin-controlled `status`.
    //
    // Problem before: A landlord uploaded a new property. The `available`
    // field defaulted to `true`. The PropertyDetailScreen hero badge
    // showed "Available" (green) even though status was still "PENDING"
    // and admin hadn't approved it yet — misleading to the tenant.
    //
    // Fix: A property is only truly available when:
    //   1. The raw `available` flag is true  (landlord hasn't marked it unavailable)
    //   2. AND status is "APPROVED"          (admin has reviewed and approved it)
    //   3. AND status is NOT "BOOKED"        (no active booking occupying it)
    //
    // This single change fixes the hero badge in PropertyDetailScreen and
    // every other place that reads `property.isAvailable`.
    // ════════════════════════════════════════════════════════════════════════
    @get:Exclude
    val isAvailable: Boolean
        get() = available
                && status.equals(PropertyStatus.APPROVED.name, ignoreCase = true)
                && !status.equals("BOOKED", ignoreCase = true)

    @get:Exclude
    val isFeatured: Boolean get() = featured

    // True if landlord uploaded a PT-1 verification document
    val hasPt1Document: Boolean get() = pt1DocumentUrl.isNotBlank()

    // ════════════════════════════════════════════════════════════════════════
    // MAP COORDINATE RESOLUTION
    //
    // Priority 1: Use explicit lat/lng saved in Firestore (set by PropertyViewModel)
    // Priority 2: Fall back to city-name lookup (covers manually seeded properties)
    // Priority 3: Pakistan geographic center (30.3753, 69.3451) as last resort
    //
    // ExploreMapScreen filters out properties that land on the Pakistan center
    // fallback so they don't appear as a cluster of unresolved pins.
    // ════════════════════════════════════════════════════════════════════════

    @get:Exclude
    val resolvedLatitude: Double
        get() {
            if (latitude != 0.0) return latitude
            return CITY_LATITUDES[city.lowercase().trim()] ?: PAKISTAN_CENTER_LAT
        }

    @get:Exclude
    val resolvedLongitude: Double
        get() {
            if (longitude != 0.0) return longitude
            return CITY_LONGITUDES[city.lowercase().trim()] ?: PAKISTAN_CENTER_LNG
        }

    // ════════════════════════════════════════════════════════════════════════
    // LOCAL DRAWABLE RESOLUTION
    // Used for manually seeded properties that have a local res/drawable image.
    // App-submitted properties use imageUrls (ImgBB URLs) instead.
    // ════════════════════════════════════════════════════════════════════════

    val resolvedDrawableName: String
        get() {
            if (drawableImageName.isNotEmpty()) return drawableImageName

            val c          = city.lowercase().trim()
            val t          = propertyType.lowercase().trim()
            val titleLower = title.lowercase().trim()
            val search     = if (c.isNotEmpty()) c else titleLower

            return when {
                search.contains("lahore")      && t == "apartment" -> "apartment_lahore"
                search.contains("rawalpindi")  || search.contains("pindi") -> "rawalpindi_apt"
                search.contains("karachi")     -> "house_karachi"
                search.contains("kaghan")      -> "kaghan_valleyhouse"
                search.contains("hunza")       -> "farmhouse_hunza"
                search.contains("naran")       -> "farmhouse_naran"
                search.contains("skardu")      -> "skardu_house"
                search.contains("swat")        && t == "villa" -> "swat_vila"
                search.contains("murree")      || search.contains("murri") -> "vila_murree"
                search.contains("islamabad")   && t == "room" -> "room_islamabad"
                search.contains("sialkot")     -> "room_sialkot"
                search.contains("faisalabad")  || search.contains("faislabad") -> "studio_faislabad"
                else -> ""
            }
        }

    // ── Enum accessors ────────────────────────────────────────────────────────

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

    // ════════════════════════════════════════════════════════════════════════
    // COMPANION: city coordinate lookup tables
    // These are used as fallbacks when a property has no explicit lat/lng.
    // New cities can be added here without changing any other file.
    // ════════════════════════════════════════════════════════════════════════

    companion object {
        const val PAKISTAN_CENTER_LAT = 30.3753
        const val PAKISTAN_CENTER_LNG = 69.3451

        val CITY_LATITUDES = mapOf(
            // Northern areas
            "skardu"       to 35.2971,
            "hunza"        to 36.3167,
            "gilgit"       to 35.9221,
            "swat"         to 35.2227,
            "naran"        to 34.9008,
            "kaghan"       to 34.9167,
            "murree"       to 33.9071,
            "abbottabad"   to 34.1463,
            "mansehra"     to 34.3293,
            "chitral"      to 35.8517,

            // Punjab
            "islamabad"    to 33.7215,
            "rawalpindi"   to 33.6007,
            "lahore"       to 31.5204,
            "faisalabad"   to 31.4504,
            "gujranwala"   to 32.1877,
            "sialkot"      to 32.4927,
            "multan"       to 30.1575,
            "bahawalpur"   to 29.3956,
            "sargodha"     to 32.0836,
            "gujrat"       to 32.5744,
            "sheikhupura"  to 31.7167,

            // Sindh
            "karachi"      to 24.8607,
            "hyderabad"    to 25.3960,
            "sukkur"       to 27.7052,
            "larkana"      to 27.5570,

            // KPK
            "peshawar"     to 34.0151,
            "mardan"       to 34.1980,

            // Balochistan
            "quetta"       to 30.1798
        )

        val CITY_LONGITUDES = mapOf(
            // Northern areas
            "skardu"       to 75.6352,
            "hunza"        to 74.6500,
            "gilgit"       to 74.3090,
            "swat"         to 72.4258,
            "naran"        to 73.6511,
            "kaghan"       to 73.6333,
            "murree"       to 73.3943,
            "abbottabad"   to 73.2117,
            "mansehra"     to 73.1975,
            "chitral"      to 71.8360,

            // Punjab
            "islamabad"    to 73.0433,
            "rawalpindi"   to 73.0651,
            "lahore"       to 74.3587,
            "faisalabad"   to 73.1350,
            "gujranwala"   to 74.1945,
            "sialkot"      to 74.5311,
            "multan"       to 71.5249,
            "bahawalpur"   to 71.6839,
            "sargodha"     to 72.6689,
            "gujrat"       to 74.0775,
            "sheikhupura"  to 73.9850,

            // Sindh
            "karachi"      to 67.0011,
            "hyderabad"    to 68.3578,
            "sukkur"       to 68.8570,
            "larkana"      to 68.2150,

            // KPK
            "peshawar"     to 71.5249,
            "mardan"       to 72.0446,

            // Balochistan
            "quetta"       to 66.9750
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PropertyType enum
// All possible property categories shown in AddPropertyScreen filter chips.
// ══════════════════════════════════════════════════════════════════════════════

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

// ══════════════════════════════════════════════════════════════════════════════
// PropertyStatus enum
// Reflects the admin review lifecycle of a property.
// Only APPROVED properties are visible to tenants and shown on the map.
// ══════════════════════════════════════════════════════════════════════════════

enum class PropertyStatus {
    PENDING,        // Just submitted by landlord, awaiting admin review
    UNDER_REVIEW,   // Admin has opened and is reviewing the listing
    APPROVED,       // Approved — visible to tenants and shown on explore map
    REJECTED,       // Rejected — landlord notified via adminNote
    INACTIVE,       // Temporarily hidden by landlord or admin
    BOOKED;         // ✅ NEW: Currently occupied by an active booking

    fun displayName(): String = when (this) {
        PENDING      -> "Pending"
        UNDER_REVIEW -> "Under Review"
        APPROVED     -> "Approved"
        REJECTED     -> "Rejected"
        INACTIVE     -> "Inactive"
        BOOKED       -> "Booked"
    }
}
