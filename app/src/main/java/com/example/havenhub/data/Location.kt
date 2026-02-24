package com.example.havenhub.data
// Location.kt
// Model: Geographic coordinates and address details for a property listing.
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * # Location
 *
 * Stores the full geographic and postal address information for a property listing.
 * This model is embedded as a **nested object** inside the [Property] data class
 * rather than being stored as a separate Firestore collection — since a location
 * has no meaning independently of a property.
 *
 * ## Firestore Storage
 * Embedded directly inside the parent property document:
 * ```
 * properties/{propertyId}
 *   └── location: { address, suburb, city, province, ... }
 * ```
 *
 * ## Map Integration
 * The [latitude] and [longitude] fields enable:
 * - Rendering property pins on a map (Google Maps / MapBox)
 * - Geo-proximity filtering (e.g., "properties within 5 km")
 * - Distance calculations from the user's current location
 *
 * ## Usage Example
 * ```kotlin
 * val location = Location(
 *     address    = "12 Oak Avenue",
 *     suburb     = "Sandton",
 *     city       = "Johannesburg",
 *     province   = "Gauteng",
 *     postalCode = "2196",
 *     latitude   = -26.1076,
 *     longitude  = 28.0567
 * )
 *
 * println(location.toDisplayString())  // "Sandton, Johannesburg"
 * println(location.toFullAddress())    // "12 Oak Avenue, Sandton, Johannesburg, 2196"
 * ```
 *
 * @property address     Full street address line (e.g., "12 Oak Avenue, Apt 3A").
 * @property suburb      Suburb or neighbourhood name (e.g., "Sandton").
 * @property city        City or town name (e.g., "Johannesburg").
 * @property province    Province or state (e.g., "Gauteng", "Western Cape").
 * @property country     Country name. Defaults to "South Africa" for HavenHub's primary market.
 * @property postalCode  Postal or ZIP code (e.g., "2196").
 * @property latitude    GPS latitude in decimal degrees. Used for map rendering and geo-queries.
 * @property longitude   GPS longitude in decimal degrees. Used for map rendering and geo-queries.
 */
data class Location(

    /** Full street address, including unit or apartment number if applicable. */
    val address: String = "",

    /** Suburb or neighbourhood name within the city. */
    val suburb: String = "",

    /** City or town where the property is located. */
    val city: String = "",

    /** Province, state, or region (e.g., "Gauteng", "Western Cape", "KwaZulu-Natal"). */
    val province: String = "",

    /**
     * Country name.
     * Defaults to "South Africa" as HavenHub primarily serves the South African market.
     */
    val country: String = "South Africa",

    /** Postal code / ZIP code for mail delivery and search indexing. */
    val postalCode: String = "",

    /**
     * GPS latitude in decimal degrees (WGS84 coordinate system).
     * Negative values indicate the Southern Hemisphere.
     * Example: -26.1076 (Sandton, Johannesburg)
     */
    val latitude: Double = 0.0,

    /**
     * GPS longitude in decimal degrees (WGS84 coordinate system).
     * Positive values indicate East of the Prime Meridian.
     * Example: 28.0567 (Sandton, Johannesburg)
     */
    val longitude: Double = 0.0

) {

    // ─────────────────────────────────────────────────────────────────────────
    // Display Formatters
    // Used by the UI layer to present location information to users.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a short, human-readable location label for property cards and list items.
     * Combines suburb and city for a concise location description.
     *
     * Example output: `"Sandton, Johannesburg"`
     *
     * Falls back gracefully:
     * - If [suburb] is empty, returns just [city].
     * - If both are empty, returns [province] or "Unknown Location".
     */
    fun toDisplayString(): String = when {
        suburb.isNotBlank() && city.isNotBlank() -> "$suburb, $city"
        city.isNotBlank()                         -> city
        province.isNotBlank()                     -> province
        else                                      -> "Unknown Location"
    }

    /**
     * Returns the complete address as a single formatted line.
     * Suitable for use in maps search, sharing, or detailed property views.
     *
     * Example output: `"12 Oak Avenue, Sandton, Johannesburg, 2196"`
     *
     * Only non-blank components are included to avoid trailing commas.
     */
    fun toFullAddress(): String = listOf(address, suburb, city, postalCode)
        .filter { it.isNotBlank() }
        .joinToString(", ")

    /**
     * Returns the full address including province and country.
     * Suitable for formal addresses or postal purposes.
     *
     * Example output: `"12 Oak Avenue, Sandton, Johannesburg, Gauteng, 2196, South Africa"`
     */
    fun toPostalAddress(): String = listOf(address, suburb, city, province, postalCode, country)
        .filter { it.isNotBlank() }
        .joinToString(", ")

    // ─────────────────────────────────────────────────────────────────────────
    // Geo Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if this location has valid GPS coordinates.
     * A location is considered valid if both [latitude] and [longitude]
     * are non-zero (i.e., not the default unset values of 0.0, 0.0).
     *
     * Note: The point (0.0, 0.0) is in the Gulf of Guinea and is never a valid
     * South African property location.
     */
    val hasCoordinates: Boolean
        get() = latitude != 0.0 && longitude != 0.0

    /**
     * Returns true if the location has at minimum a city name.
     * Used to validate that a [Property] has sufficient location data
     * before being submitted for verification.
     */
    val isComplete: Boolean
        get() = city.isNotBlank() && province.isNotBlank()

    /**
     * Calculates the approximate distance in kilometres between this location
     * and another [Location] using the Haversine formula.
     * Useful for "properties near me" proximity filtering.
     *
     * @param other The target location to measure distance to.
     * @return Distance in kilometres, or null if either location lacks coordinates.
     */
    fun distanceTo(other: Location): Double? {
        if (!hasCoordinates || !other.hasCoordinates) return null

        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(latitude)) *
                Math.cos(Math.toRadians(other.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadiusKm * c
    }
}

