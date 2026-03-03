package com.example.havenhub.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
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

// ─────────────────────────────────────────────────────────────────
//  Property UI State
// ─────────────────────────────────────────────────────────────────
data class PropertyUiState(
    val isLoading: Boolean = false,
    val propertyDetail: Property? = null,
    val myProperties: List<Property> = emptyList(),
    val errorMessage: String? = null,
    val actionSuccess: Boolean = false,
    val successMessage: String? = null
)

@HiltViewModel
class PropertyViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertyUiState())
    val uiState: StateFlow<PropertyUiState> = _uiState.asStateFlow()

    // ─────────────────────────────────────────────────────────────
    //  Fetch Logics
    // ─────────────────────────────────────────────────────────────

    fun loadPropertyDetail(propertyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = propertyRepository.getPropertyById(propertyId)

            when (result) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, propertyDetail = result.data)
                }
                is Resource.Error -> _uiState.update {
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

            val result = propertyRepository.getMyProperties(userId)
            when (result) {
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

    // ─────────────────────────────────────────────────────────────
    //  CRUD Operations
    // ─────────────────────────────────────────────────────────────

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
        images: List<Uri>
    ) {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionSuccess = false) }

            val property = Property(
                ownerId       = userId,
                title         = title,
                description   = description,
                pricePerNight = pricePerNight,
                address       = address,
                city          = city,
                propertyType  = propertyType,
                bedrooms      = bedrooms,
                bathrooms     = bathrooms,
                areaSqFt      = areaSqFt,
                amenities     = amenities
            )

            val result = propertyRepository.addProperty(property, images)
            handleActionResult(result, "Property added successfully!")
        }
    }

    fun updateProperty(property: Property, newImages: List<Uri> = emptyList()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionSuccess = false) }

            val fields = mutableMapOf<String, Any>(
                "title"         to property.title,
                "description"   to property.description,
                "pricePerNight" to property.pricePerNight,
                "address"       to property.address,
                "city"          to property.city,
                "propertyType"  to property.propertyType.name,
                "bedrooms"      to property.bedrooms,
                "bathrooms"     to property.bathrooms,
                "amenities"     to property.amenities
            )
            property.areaSqFt?.let { fields["areaSqFt"] = it }

            val result = propertyRepository.updateProperty(property.propertyId, fields)

            // Agar text update ho gaya aur new images hain, toh unhe upload karein
            if (result is Resource.Success && newImages.isNotEmpty()) {
                propertyRepository.addPropertyImages(property.propertyId, newImages)
            }

            handleActionResult(result, "Property updated successfully!")
        }
    }

    fun deleteProperty(propertyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionSuccess = false) }
            val result = propertyRepository.deleteProperty(propertyId)
            handleActionResult(result, "Property deleted successfully!")
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Utilities
    // ─────────────────────────────────────────────────────────────

    private fun <T> handleActionResult(result: Resource<T>, successMsg: String) {
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

    fun clearMessages() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                successMessage = null,
                actionSuccess = false
            )
        }
    }
}