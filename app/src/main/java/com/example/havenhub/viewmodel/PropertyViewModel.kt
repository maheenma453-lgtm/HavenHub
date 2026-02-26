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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PropertyViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _propertyDetail = MutableStateFlow<Resource<Property>>(Resource.Loading)
    val propertyDetail: StateFlow<Resource<Property>> = _propertyDetail.asStateFlow()

    private val _myProperties = MutableStateFlow<Resource<List<Property>>>(Resource.Loading)
    val myProperties: StateFlow<Resource<List<Property>>> = _myProperties.asStateFlow()

    private val _addPropertyState = MutableStateFlow<Resource<String>>(Resource.Loading)
    val addPropertyState: StateFlow<Resource<String>> = _addPropertyState.asStateFlow()

    private val _updatePropertyState = MutableStateFlow<Resource<Unit>>(Resource.Loading)
    val updatePropertyState: StateFlow<Resource<Unit>> = _updatePropertyState.asStateFlow()

    private val _deletePropertyState = MutableStateFlow<Resource<Unit>>(Resource.Loading)
    val deletePropertyState: StateFlow<Resource<Unit>> = _deletePropertyState.asStateFlow()

    fun loadPropertyDetail(propertyId: String) {
        viewModelScope.launch {
            _propertyDetail.value = Resource.Loading
            _propertyDetail.value = propertyRepository.getPropertyById(propertyId)
        }
    }

    fun loadMyProperties() {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            _myProperties.value = Resource.Loading
            _myProperties.value = propertyRepository.getMyProperties(userId)
        }
    }

    fun addProperty(
        title: String,
        description: String,
        pricePerNight: Double,           // FIX: was 'price' → 'pricePerNight'
        address: String,
        city: String,
        propertyType: PropertyType,      // FIX: was String 'type' → PropertyType enum
        bedrooms: Int,
        bathrooms: Int,
        areaSqFt: Double? = null,        // FIX: was 'area' → 'areaSqFt' (nullable)
        amenities: List<String>,
        images: List<Uri>
    ) {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            _addPropertyState.value = Resource.Loading

            val property = Property(
                ownerId      = userId,
                title        = title,
                description  = description,
                pricePerNight = pricePerNight,
                address      = address,
                city         = city,
                propertyType = propertyType,
                bedrooms     = bedrooms,
                bathrooms    = bathrooms,
                areaSqFt     = areaSqFt,
                amenities    = amenities
            )
            _addPropertyState.value = propertyRepository.addProperty(property, images)
        }
    }

    fun updateProperty(property: Property, newImages: List<Uri> = emptyList()) {
        viewModelScope.launch {
            _updatePropertyState.value = Resource.Loading
            val fields = mutableMapOf<String, Any>(
                "title"        to property.title,
                "description"  to property.description,
                "pricePerNight" to property.pricePerNight,  // FIX: was 'price'
                "address"      to property.address,
                "city"         to property.city,
                "propertyType" to property.propertyType.name, // FIX: was 'type'
                "bedrooms"     to property.bedrooms,
                "bathrooms"    to property.bathrooms,
                "amenities"    to property.amenities
            )
            // FIX: areaSqFt is nullable — only add if not null
            property.areaSqFt?.let { fields["areaSqFt"] = it }

            _updatePropertyState.value = propertyRepository.updateProperty(property.propertyId, fields)

            if (newImages.isNotEmpty()) {
                propertyRepository.addPropertyImages(property.propertyId, newImages)
            }
        }
    }

    fun deleteProperty(propertyId: String) {
        viewModelScope.launch {
            _deletePropertyState.value = Resource.Loading
            _deletePropertyState.value = propertyRepository.deleteProperty(propertyId)
        }
    }

    fun resetStates() {
        _addPropertyState.value = Resource.Loading
        _updatePropertyState.value = Resource.Loading
        _deletePropertyState.value = Resource.Loading
    }
}