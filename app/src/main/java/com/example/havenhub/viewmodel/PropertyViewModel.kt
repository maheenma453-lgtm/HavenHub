package com.example.havenhub.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.data.PropertyType
import com.example.havenhub.data.RentalPackage
import com.example.havenhub.repository.AuthRepository
import com.example.havenhub.repository.BookingRepository
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.utils.Resource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// ══════════════════════════════════════════════════════════════════════════════
// UI STATE
// Holds all data that the UI screens observe.
// ══════════════════════════════════════════════════════════════════════════════
data class PropertyUiState(
    val isLoading                : Boolean             = false,
    val propertyDetail           : Property?           = null,
    val myProperties             : List<Property>      = emptyList(),
    val allProperties            : List<Property>      = emptyList(),
    val rentalPackages           : List<RentalPackage> = emptyList(),
    val errorMessage             : String?             = null,
    val actionSuccess            : Boolean             = false,
    val successMessage           : String?             = null,
    val isPropertyCurrentlyBooked: Boolean?            = null,
    val isCheckingBooking        : Boolean             = false
)

@HiltViewModel
class PropertyViewModel @Inject constructor(
    private val propertyRepository : PropertyRepository,
    private val authRepository     : AuthRepository,
    private val bookingRepository  : BookingRepository,
    private val firestore          : FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertyUiState())
    val uiState: StateFlow<PropertyUiState> = _uiState.asStateFlow()

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 1: LOAD OPERATIONS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Load a single property by its ID.
     * After loading, if ownerName is blank, fetches it separately from Firestore users collection.
     */
    fun loadPropertyDetail(propertyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = propertyRepository.getPropertyById(propertyId)) {
                is Resource.Success -> {
                    val property = result.data
                    _uiState.update { it.copy(isLoading = false, propertyDetail = property) }

                    // If ownerName is missing in Firestore doc, fetch from users collection
                    if (property != null) {
                        if (property.ownerName.isBlank() && property.ownerId.isNotBlank()) {
                            fetchAndPatchOwnerName(property)
                        }
                    }
                }

                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }

                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    /**
     * Fetch owner's display name from the users collection and patch it
     * into the current propertyDetail state without a full reload.
     */
    private fun fetchAndPatchOwnerName(property: Property) {
        viewModelScope.launch {
            try {
                val doc = firestore
                    .collection("users")
                    .document(property.ownerId)
                    .get()
                    .await()

                // Try multiple possible field names for the owner's name
                val name = (
                        doc.getString("fullName")
                            ?: doc.getString("name")
                            ?: doc.getString("displayName")
                            ?: doc.getString("firstName")?.let { first ->
                                val last = doc.getString("lastName") ?: ""
                                "$first $last".trim()
                            }
                            ?: ""
                        ).ifBlank { "" }

                if (name.isNotBlank()) {
                    _uiState.update { state ->
                        state.copy(
                            propertyDetail = state.propertyDetail?.copy(ownerName = name)
                        )
                    }
                }
            } catch (_: Exception) {
                // Non-critical — if this fails, just show blank owner name
            }
        }
    }

    /**
     * Load all properties owned by the currently logged-in user.
     * Shows all statuses: PENDING, APPROVED, REJECTED.
     */
    fun loadMyProperties() {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = propertyRepository.getMyProperties(userId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, myProperties = result.data)
                }

                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }

                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    /**
     * Load all APPROVED properties (used by HomeScreen, SearchScreen, etc.)
     */
    fun loadAllProperties() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = propertyRepository.getAllProperties()) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, allProperties = result.data)
                }

                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }

                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 1B — BOOKING STATUS CHECK (DATE-AWARE)
    //
    // Flow:
    //   1. checkPropertyBookingStatus() is called from LaunchedEffect in detail screen
    //   2. markExpiredBookingsCompleted() runs silently in background
    //      → Expired bookings get marked COMPLETED in Firestore
    //   3. isPropertyBooked() runs a fresh Firestore query
    //      → Expired bookings are no longer in CONFIRMED/PENDING/CHECKED_IN
    //      → If no active booking found → shows "Available"
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Check if a property is currently booked.
     * First auto-completes any expired bookings, then checks active status.
     */
    fun checkPropertyBookingStatus(propertyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingBooking = true) }

            // Step 1: Auto-complete expired bookings silently
            try {
                bookingRepository.markExpiredBookingsCompleted()
            } catch (_: Exception) {
                // Non-critical — status check will continue regardless
            }

            // Step 2: Check for any active booking on this property
            val booked = bookingRepository.isPropertyBooked(propertyId)

            _uiState.update {
                it.copy(
                    isCheckingBooking = false,
                    isPropertyCurrentlyBooked = booked
                )
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 2: COORDINATE RESOLUTION
    // Used when adding/updating a property to set lat/lng from city name.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Get latitude for a given city name.
     * Falls back to Pakistan's center if city is not found.
     */
    private fun resolveCityLatitude(city: String): Double {
        val key = city.lowercase().trim()
        Property.CITY_LATITUDES[key]?.let { return it }
        Property.CITY_LATITUDES.entries
            .firstOrNull { (k, _) -> key.contains(k) }
            ?.let { return it.value }
        return Property.PAKISTAN_CENTER_LAT
    }

    /**
     * Get longitude for a given city name.
     * Falls back to Pakistan's center if city is not found.
     */
    private fun resolveCityLongitude(city: String): Double {
        val key = city.lowercase().trim()
        Property.CITY_LONGITUDES[key]?.let { return it }
        Property.CITY_LONGITUDES.entries
            .firstOrNull { (k, _) -> key.contains(k) }
            ?.let { return it.value }
        return Property.PAKISTAN_CENTER_LNG
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 3: ADD PROPERTY
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Create a new property listing.
     * Resolves city coordinates, fetches owner name if not in Auth,
     * then submits to Firebase via PropertyRepository.
     * Status is always set to PENDING on creation — admin must approve.
     */
    fun addProperty(
        title: String,
        description: String,
        pricePerNight: Double,
        address: String,
        city: String,
        propertyType: PropertyType,
        bedrooms: Int,
        bathrooms: Int,
        areaSqFt: Double? = null,
        amenities: List<String>,
        images: List<Uri>,
        pt1DocumentUri: Uri? = null,
        petsAllowed: Boolean = false,
        smokingAllowed: Boolean = false,
        partiesAllowed: Boolean = false,
        checkInTime: String = "14:00",
        checkOutTime: String = "11:00",
        maxGuests: Int = 2,
        pricePerWeek: Double? = null,
        pricePerMonth: Double? = null,
        isPremium: Boolean = false,
        status: String = PropertyStatus.PENDING.name
    ) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser ?: return@launch
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, actionSuccess = false)
            }

            // Resolve city name to GPS coordinates
            val resolvedLat = resolveCityLatitude(city)
            val resolvedLng = resolveCityLongitude(city)

            // Fetch owner name — try Firebase Auth displayName first, then Firestore
            val ownerName: String = (currentUser.displayName?.trim() ?: "").ifBlank {
                try {
                    val doc = firestore.collection("users").document(currentUser.uid).get().await()
                    doc.getString("fullName")
                        ?: doc.getString("name")
                        ?: doc.getString("displayName")
                        ?: doc.getString("firstName")?.let { first ->
                            val last = doc.getString("lastName") ?: ""
                            "$first $last".trim()
                        }
                        ?: ""
                } catch (_: Exception) {
                    ""
                }
            }

            val property = Property(
                ownerId = currentUser.uid,
                ownerName = ownerName,
                title = title,
                description = description,
                pricePerNight = pricePerNight,
                pricePerWeek = pricePerWeek,
                pricePerMonth = pricePerMonth,
                address = address,
                city = city,
                latitude = resolvedLat,
                longitude = resolvedLng,
                propertyType = propertyType.toString(),
                bedrooms = bedrooms,
                bathrooms = bathrooms,
                maxGuests = maxGuests,
                areaSqFt = areaSqFt,
                amenities = amenities,
                petsAllowed = petsAllowed,
                smokingAllowed = smokingAllowed,
                partiesAllowed = partiesAllowed,
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                premium = isPremium,
                status = status
            )

            val result = propertyRepository.addProperty(property, images, pt1DocumentUri)
            handleActionResult(result, "Property submitted! Admin will review it.")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 4: EDIT / UPDATE PROPERTY
    //
    // FIX APPLIED:
    //   - updatedAt now uses FieldValue.serverTimestamp() instead of
    //     System.currentTimeMillis() — prevents Firestore field type mismatch.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Update an existing property with new field values.
     * If new images are provided, they are uploaded and appended to existing ones.
     *
     * FIX: updatedAt uses FieldValue.serverTimestamp() (was System.currentTimeMillis())
     */
    fun updateProperty(property: Property, newImages: List<Uri> = emptyList()) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, actionSuccess = false)
            }

            // Resolve coordinates from the (possibly updated) city name
            val lat = resolveCityLatitude(property.city)
            val lng = resolveCityLongitude(property.city)

            // Build the fields map for Firestore partial update
            val fields = mutableMapOf<String, Any>(
                "title" to property.title,
                "description" to property.description,
                "pricePerNight" to property.pricePerNight,
                "address" to property.address,
                "city" to property.city,
                "latitude" to lat,
                "longitude" to lng,
                "propertyType" to property.propertyType,
                "bedrooms" to property.bedrooms,
                "bathrooms" to property.bathrooms,
                "amenities" to property.amenities,
                "petsAllowed" to property.petsAllowed,
                "smokingAllowed" to property.smokingAllowed,
                "partiesAllowed" to property.partiesAllowed,
                "checkInTime" to property.checkInTime,
                "checkOutTime" to property.checkOutTime,
                "isPremium" to property.premium,
                // FIX: Use Firestore server timestamp — System.currentTimeMillis()
                // caused a Long vs Timestamp type mismatch in Firestore documents.
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Only include optional fields if they have a value
            property.areaSqFt?.let { fields["areaSqFt"] = it }
            property.pricePerWeek?.let { fields["pricePerWeek"] = it }
            property.pricePerMonth?.let { fields["pricePerMonth"] = it }

            // Also update the imageUrls field in Firestore with the current list
            // (some images may have been removed by the user in EditPropertyScreen)
            fields["imageUrls"] = property.imageUrls

            val result = propertyRepository.updateProperty(property.propertyId, fields)

            // If update succeeded and user added new images, upload and append them
            if (result is Resource.Success && newImages.isNotEmpty()) {
                propertyRepository.addPropertyImages(property.propertyId, newImages)
            }

            handleActionResult(result, "Property updated successfully!")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 5: DELETE PROPERTY
    //
    // FIX APPLIED:
    //   - After a successful delete, the property is immediately removed from
    //     the local myProperties list in UiState — no need to wait for reload.
    //   - actionSuccess is still set to true so MyPropertiesScreen can also
    //     trigger a background reload for fresh data.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Delete a property from Firestore.
     *
     * FIX: On success, the property is instantly removed from the local
     * myProperties list so the UI updates immediately without waiting for
     * a full reload from Firebase.
     */
    fun deleteProperty(propertyId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, actionSuccess = false)
            }

            val result = propertyRepository.deleteProperty(propertyId)

            if (result is Resource.Success) {
                // FIX: Remove deleted property from local list immediately
                // This gives instant UI feedback — the card disappears right away.
                // MyPropertiesScreen will also do a background reload via LaunchedEffect.
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        actionSuccess = true,
                        successMessage = "Property deleted!",
                        myProperties = state.myProperties.filterNot {
                            it.propertyId == propertyId
                        }
                    )
                }
            } else {
                // On failure, show the error message
                handleActionResult(result, "Property deleted!")
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 6: ADMIN ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Approve a property (admin only).
     * Sends approval notification to the landlord.
     */
    @Suppress("unused")
    fun approveProperty(propertyId: String, adminNote: String = "") {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, actionSuccess = false)
            }
            val result = propertyRepository.approveProperty(propertyId, adminNote)
            handleActionResult(result, "Property approved!")
        }
    }

    /**
     * Reject a property (admin only).
     * Sends rejection notification with admin note to the landlord.
     */
    @Suppress("unused")
    fun rejectProperty(propertyId: String, adminNote: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, actionSuccess = false)
            }
            val result = propertyRepository.rejectProperty(propertyId, adminNote)
            handleActionResult(result, "Property rejected!")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 7: RENTAL PACKAGES
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Create a new rental package for a property.
     */
    fun addRentalPackage(pkg: RentalPackage) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, actionSuccess = false)
            }
            val result = propertyRepository.addRentalPackage(pkg)
            handleActionResult(result, "Package created successfully!")
        }
    }

    /**
     * Load all rental packages associated with a specific property.
     */
    @Suppress("unused")
    fun loadPackagesByProperty(propertyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = propertyRepository.getPackagesByProperty(propertyId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, rentalPackages = result.data)
                }

                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }

                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    /**
     * Delete a rental package by its ID.
     */
    @Suppress("unused")
    fun deleteRentalPackage(packageId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, actionSuccess = false)
            }
            val result = propertyRepository.deleteRentalPackage(packageId)
            handleActionResult(result, "Package deleted!")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 8: HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Generic handler for action results (add, update, delete, approve, reject).
     * Sets actionSuccess = true and shows successMsg on success,
     * or sets errorMessage on failure.
     */
    private fun handleActionResult(result: Resource<*>, successMsg: String) {
        when (result) {
            is Resource.Success -> _uiState.update {
                it.copy(
                    isLoading = false,
                    actionSuccess = true,
                    successMessage = successMsg
                )
            }

            is Resource.Error -> _uiState.update {
                it.copy(isLoading = false, errorMessage = result.message)
            }

            is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
        }
    }

    /**
     * Clear all transient messages and reset actionSuccess flag.
     * Call this AFTER navigating away or showing the snackbar — not before.
     */
    fun clearMessages() {
        _uiState.update {
            it.copy(errorMessage = null, successMessage = null, actionSuccess = false)
        }
    }
}