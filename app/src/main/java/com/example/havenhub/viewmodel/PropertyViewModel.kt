package com.example.havenhub.viewmodel
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.havenhub.data.model.Property
import com.havenhub.data.repository.AuthRepository
import com.havenhub.data.repository.PropertyRepository
import com.havenhub.utils.Resource
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

    private val _propertyDetail = MutableStateFlow<Resource<Property>>(Resource.Idle())
    val propertyDetail: StateFlow<Resource<Property>> = _propertyDetail.asStateFlow()

    private val _myProperties = MutableStateFlow<Resource<List<Property>>>(Resource.Idle())
    val myProperties: StateFlow<Resource<List<Property>>> = _myProperties.asStateFlow()

    private val _addPropertyState = MutableStateFlow<Resource<Property>>(Resource.Idle())
    val addPropertyState: StateFlow<Resource<Property>> = _addPropertyState.asStateFlow()

    private val _updatePropertyState = MutableStateFlow<Resource<Property>>(Resource.Idle())
    val updatePropertyState: StateFlow<Resource<Property>> = _updatePropertyState.asStateFlow()

    private val _deletePropertyState = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val deletePropertyState: StateFlow<Resource<Boolean>> = _deletePropertyState.asStateFlow()

    fun loadPropertyDetail(propertyId: String) {
        viewModelScope.launch {
            _propertyDetail.value = Resource.Loading()
            _propertyDetail.value = propertyRepository.getPropertyById(propertyId)
        }
    }

    fun loadMyProperties() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: return@launch
            _myProperties.value = Resource.Loading()
            _myProperties.value = propertyRepository.getPropertiesByOwner(userId)
        }
    }

    fun addProperty(
        title: String,
        description: String,
        price: Double,
        address: String,
        city: String,
        type: String,
        bedrooms: Int,
        bathrooms: Int,
        area: Double,
        amenities: List<String>,
        images: List<Uri>
    ) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: return@launch
            _addPropertyState.value = Resource.Loading()
            _addPropertyState.value = propertyRepository.addProperty(
                ownerId = userId,
                title = title,
                description = description,
                price = price,
                address = address,
                city = city,
                type = type,
                bedrooms = bedrooms,
                bathrooms = bathrooms,
                area = area,
                amenities = amenities,
                images = images
            )
        }
    }

    fun updateProperty(property: Property, newImages: List<Uri> = emptyList()) {
        viewModelScope.launch {
            _updatePropertyState.value = Resource.Loading()
            _updatePropertyState.value = propertyRepository.updateProperty(property, newImages)
        }
    }

    fun deleteProperty(propertyId: String) {
        viewModelScope.launch {
            _deletePropertyState.value = Resource.Loading()
            _deletePropertyState.value = propertyRepository.deleteProperty(propertyId)
        }
    }

    fun resetStates() {
        _addPropertyState.value = Resource.Idle()
        _updatePropertyState.value = Resource.Idle()
        _deletePropertyState.value = Resource.Idle()
    }
}