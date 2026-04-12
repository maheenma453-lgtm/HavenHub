package com.example.havenhub.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.data.PropertyType
import com.example.havenhub.repository.AuthRepository
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PropertyUiState(
    val isLoading      : Boolean          = false,
    val propertyDetail : Property?        = null,
    val myProperties   : List<Property>   = emptyList(),
    val errorMessage   : String?          = null,
    val actionSuccess  : Boolean          = false,
    val successMessage : String?          = null
)

@HiltViewModel
class PropertyViewModel @Inject constructor(
    private val propertyRepository : PropertyRepository,
    private val authRepository     : AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertyUiState())
    val uiState: StateFlow<PropertyUiState> = _uiState.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Fetch
    // ─────────────────────────────────────────────────────────────────────────

    fun loadPropertyDetail(propertyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = propertyRepository.getPropertyById(propertyId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, propertyDetail = result.data)
                }
                is Resource.Error   -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    fun loadMyProperties() {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = propertyRepository.getMyProperties(userId)) {
                is Resource.Success -> _uiState.update {
                    // ✅ Fix: data nullable safely handle karo
                    it.copy(isLoading = false, myProperties = result.data ?: emptyList())
                }
                is Resource.Error   -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Create
    // ─────────────────────────────────────────────────────────────────────────

    fun addProperty(
        title         : String,
        description   : String,
        pricePerNight : Double,
        address       : String,
        city          : String,
        propertyType  : PropertyType,
        bedrooms      : Int,
        bathrooms     : Int,
        areaSqFt      : Double?      = null,
        amenities     : List<String>,
        images        : List<Uri>,
        status        : String       = PropertyStatus.PENDING.name  // ✅ default PENDING
    ) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser ?: return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionSuccess = false) }

            // ✅ ownerName bhi save karo
            val property = Property(
                ownerId       = currentUser.uid,
                ownerName     = currentUser.displayName ?: "",
                title         = title,
                description   = description,
                pricePerNight = pricePerNight,
                address       = address,
                city          = city,
                propertyType  = propertyType.name,
                status        = status,               // ✅ PENDING set ho raha hai
                bedrooms      = bedrooms,
                bathrooms     = bathrooms,
                areaSqFt      = areaSqFt,
                amenities     = amenities
            )

            val result = propertyRepository.addProperty(property, images)
            handleActionResult(result, "Property submit ho gayi! Admin approve karega.")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────────────────────────────────

    fun updateProperty(property: Property, newImages: List<Uri> = emptyList()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionSuccess = false) }

            val fields = mutableMapOf<String, Any>(
                "title"         to property.title,
                "description"   to property.description,
                "pricePerNight" to property.pricePerNight,
                "address"       to property.address,
                "city"          to property.city,
                "propertyType"  to property.propertyType,
                "bedrooms"      to property.bedrooms,
                "bathrooms"     to property.bathrooms,
                "amenities"     to property.amenities,
                "updatedAt"     to System.currentTimeMillis()
            )
            property.areaSqFt?.let { fields["areaSqFt"] = it }

            val result = propertyRepository.updateProperty(property.propertyId, fields)

            if (result is Resource.Success && newImages.isNotEmpty()) {
                propertyRepository.addPropertyImages(property.propertyId, newImages)
            }

            handleActionResult(result, "Property update ho gayi!")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete
    // ─────────────────────────────────────────────────────────────────────────

    fun deleteProperty(propertyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionSuccess = false) }
            val result = propertyRepository.deleteProperty(propertyId)
            handleActionResult(result, "Property delete ho gayi!")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin Actions
    // ─────────────────────────────────────────────────────────────────────────

    fun approveProperty(propertyId: String, adminNote: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionSuccess = false) }
            val result = propertyRepository.approveProperty(propertyId, adminNote)
            handleActionResult(result, "Property approve ho gayi!")
        }
    }

    fun rejectProperty(propertyId: String, adminNote: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionSuccess = false) }
            val result = propertyRepository.rejectProperty(propertyId, adminNote)
            handleActionResult(result, "Property reject ho gayi!")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun handleActionResult(result: Resource<*>, successMsg: String) {
        when (result) {
            is Resource.Success -> _uiState.update {
                it.copy(isLoading = false, actionSuccess = true, successMessage = successMsg)
            }
            is Resource.Error   -> _uiState.update {
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