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
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

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
    private val firestore          : FirebaseFirestore   // ✅ NEW inject
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertyUiState())
    val uiState: StateFlow<PropertyUiState> = _uiState.asStateFlow()

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 1: LOAD OPERATIONS
    // ══════════════════════════════════════════════════════════════════════════

    fun loadPropertyDetail(propertyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = propertyRepository.getPropertyById(propertyId)) {
                is Resource.Success -> {
                    val property = result.data
                    _uiState.update { it.copy(isLoading = false, propertyDetail = property) }

                    // ✅ FIX: Agar ownerName empty hai toh Firestore users collection se fetch karo
                    // Yeh auto-added properties ka issue fix karta hai jahan displayName null tha
                    if (property != null && property.ownerName.isBlank() && property.ownerId.isNotBlank()) {
                        fetchAndPatchOwnerName(property)
                    }
                }

                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }

                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    // ✅ NEW: Firestore users collection se owner ka naam fetch karo
    // aur propertyDetail mein patch karo (Firestore document update nahi hoga,
    // sirf in-memory UI state update hoga)
    private fun fetchAndPatchOwnerName(property: Property) {
        viewModelScope.launch {
            try {
                val doc = firestore
                    .collection("users")
                    .document(property.ownerId)
                    .get()
                    .await()

                // Firestore users collection mein naam in fields mein se koi bhi ho sakta hai
                val name = doc.getString("fullName")
                    ?: doc.getString("name")
                    ?: doc.getString("displayName")
                    ?: doc.getString("firstName")?.let { first ->
                        val last = doc.getString("lastName") ?: ""
                        "$first $last".trim()
                    }
                    ?: ""

                if (name.isNotBlank()) {
                    // Sirf UI state mein patch karo — Firestore document nahi badlega
                    _uiState.update { state ->
                        state.copy(
                            propertyDetail = state.propertyDetail?.copy(ownerName = name)
                        )
                    }
                }
            } catch (e: Exception) {
                // Silently ignore — naam na milna critical error nahi hai
                // UI mein "Property Owner" fallback dikhega
            }
        }
    }

    fun loadMyProperties() {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = propertyRepository.getMyProperties(userId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, myProperties = result.data ?: emptyList())
                }

                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }

                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

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
    // SECTION 1B — BOOKING STATUS CHECK
    // ══════════════════════════════════════════════════════════════════════════

    fun checkPropertyBookingStatus(propertyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingBooking = true) }
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
    // ══════════════════════════════════════════════════════════════════════════

    private fun resolveCityLatitude(city: String): Double {
        val key = city.lowercase().trim()
        Property.CITY_LATITUDES[key]?.let { return it }
        Property.CITY_LATITUDES.entries
            .firstOrNull { (k, _) -> key.contains(k) }
            ?.let { return it.value }
        return Property.PAKISTAN_CENTER_LAT
    }

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
        status: String = PropertyStatus.PENDING.name
    ) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser ?: return@launch
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, actionSuccess = false)
            }

            val resolvedLat = resolveCityLatitude(city)
            val resolvedLng = resolveCityLongitude(city)

            // ✅ FIX: ownerName ke liye sirf displayName pe rely nahi karo
            // Pehle displayName try karo, agar null/blank hai toh Firestore se fetch karo
            val ownerNameFromAuth = currentUser.displayName?.trim() ?: ""
            val ownerName = if (ownerNameFromAuth.isNotBlank()) {
                ownerNameFromAuth
            } else {
                // Firestore se naam fetch karo
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
                } catch (e: Exception) {
                    ""
                }
            }

            val property = Property(
                ownerId = currentUser.uid,
                ownerName = ownerName,          // ✅ Fixed naam yahan store hoga
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
                status = status
            )

            val result = propertyRepository.addProperty(property, images, pt1DocumentUri)
            handleActionResult(result, "Property submitted! Admin will review it.")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 4: EDIT / UPDATE PROPERTY
    // ══════════════════════════════════════════════════════════════════════════

    fun updateProperty(property: Property, newImages: List<Uri> = emptyList()) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, actionSuccess = false)
            }

            val lat = resolveCityLatitude(property.city)
            val lng = resolveCityLongitude(property.city)

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
                "updatedAt" to System.currentTimeMillis()
            )

            property.areaSqFt?.let { fields["areaSqFt"] = it }
            property.pricePerWeek?.let { fields["pricePerWeek"] = it }
            property.pricePerMonth?.let { fields["pricePerMonth"] = it }

            val result = propertyRepository.updateProperty(property.propertyId, fields)

            if (result is Resource.Success && newImages.isNotEmpty()) {
                propertyRepository.addPropertyImages(property.propertyId, newImages)
            }

            handleActionResult(result, "Property updated successfully!")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 5: DELETE PROPERTY
    // ══════════════════════════════════════════════════════════════════════════

    fun deleteProperty(propertyId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, actionSuccess = false)
            }
            val result = propertyRepository.deleteProperty(propertyId)
            handleActionResult(result, "Property deleted!")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 6: ADMIN ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    fun approveProperty(propertyId: String, adminNote: String = "") {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, actionSuccess = false)
            }
            val result = propertyRepository.approveProperty(propertyId, adminNote)
            handleActionResult(result, "Property approved!")
        }
    }

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

    fun addRentalPackage(pkg: RentalPackage) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, actionSuccess = false)
            }
            val result = propertyRepository.addRentalPackage(pkg)
            handleActionResult(result, "Package created successfully!")
        }
    }

    fun loadPackagesByProperty(propertyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = propertyRepository.getPackagesByProperty(propertyId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        rentalPackages = result.data ?: emptyList()
                    )
                }

                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }

                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

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

    private fun handleActionResult(result: Resource<*>, successMsg: String) {
        when (result) {
            is Resource.Success -> _uiState.update {
                it.copy(isLoading = false, actionSuccess = true, successMessage = successMsg)
            }

            is Resource.Error -> _uiState.update {
                it.copy(isLoading = false, errorMessage = result.message)
            }

            is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(errorMessage = null, successMessage = null, actionSuccess = false)
        }
    }
}