package com.example.havenhub.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.data.User
import com.example.havenhub.repository.NotificationRepository
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.utils.NotificationHelper
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

data class VerificationUiState(
    val isLoading           : Boolean        = false,
    val pendingProperties   : List<Property> = emptyList(),
    val pendingUsers        : List<User>     = emptyList(),
    val selectedUser        : User?          = null,     // Direct fetch result for user detail screen
    val selectedProperty    : Property?      = null,     // ✅ NEW: Direct fetch result for property detail screen
    val errorMessage        : String?        = null,
    val successMessage      : String?        = null,
    val actionSuccess       : Boolean        = false
)

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val propertyRepository     : PropertyRepository,
    private val notificationRepository : NotificationRepository,
    private val notificationHelper     : NotificationHelper,
    private val firestore              : FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    private val usersCol      = firestore.collection("users")
    private val propertiesCol = firestore.collection("properties")

    // ════════════════════════════════════════════════════════════════════════
    // RESET / CLEAR HELPERS
    // ════════════════════════════════════════════════════════════════════════

    fun resetActionState() {
        _uiState.update {
            it.copy(
                actionSuccess  = false,
                errorMessage   = null,
                successMessage = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // LOAD PENDING PROPERTIES (list screen)
    // ════════════════════════════════════════════════════════════════════════

    fun loadPendingProperties() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = propertyRepository.getPendingProperties()) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, pendingProperties = result.data)
                }
                is Resource.Error   -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                Resource.Loading    -> Unit
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // ✅ NEW: LOAD SINGLE PROPERTY BY ID (detail screen direct open fix)
    //
    // Problem: PropertyVerificationDetailScreen property ko pendingProperties
    // list mein dhundta tha. Agar admin directly detail screen pe navigate
    // kare (notification tap, deep link) toh list empty hoti hai → "not found".
    //
    // Fix: Pehle pendingProperties check karo, agar nahi mila toh Firestore
    // se direct fetch karo aur selectedProperty mein store karo.
    // Screen dono sources se property read karta hai.
    // ════════════════════════════════════════════════════════════════════════

    fun loadPropertyById(propertyId: String) {
        if (propertyId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Invalid property ID") }
            return
        }

        // Step 1: already list mein hai?
        val cached = _uiState.value.pendingProperties.find { it.propertyId == propertyId }
        if (cached != null) {
            Log.d("VERIFY_VM", "✅ loadPropertyById: found in pendingProperties cache — $propertyId")
            _uiState.update { it.copy(selectedProperty = cached) }
            return
        }

        // Step 2: Firestore se direct fetch
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val doc = propertiesCol.document(propertyId).get().await()
                if (!doc.exists()) {
                    Log.e("VERIFY_VM", "❌ loadPropertyById: document does not exist — $propertyId")
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Property document not found in Firestore")
                    }
                    return@launch
                }

                // Manual parse ─ same fields PropertyRepository uses
                val property = parsePropertyFromDoc(doc)
                if (property != null) {
                    Log.d("VERIFY_VM", "✅ loadPropertyById Firestore fetch success — $propertyId status=${property.status}")
                    _uiState.update {
                        it.copy(
                            isLoading        = false,
                            selectedProperty = property,
                            // Also inject into pendingProperties so approve/reject
                            // list filter works even if list was empty
                            pendingProperties = if (it.pendingProperties.none { p -> p.propertyId == propertyId })
                                it.pendingProperties + property
                            else
                                it.pendingProperties
                        )
                    }
                } else {
                    Log.e("VERIFY_VM", "❌ loadPropertyById: parse returned null — $propertyId")
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Could not parse property data")
                    }
                }
            } catch (e: Exception) {
                Log.e("VERIFY_VM", "loadPropertyById error: ${e.localizedMessage}")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage)
                }
            }
        }
    }

    // ── Safe Firestore document → Property parser ─────────────────────────────
    // Mirrors FirebaseDataManager.parseProperty() without the dependency.
    private fun parsePropertyFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot): Property? {
        return try {
            com.example.havenhub.data.Property(
                propertyId        = doc.id,
                ownerId           = doc.getString("ownerId")           ?: "",
                ownerName         = doc.getString("ownerName")         ?: "",
                title             = doc.getString("title")             ?: "",
                description       = doc.getString("description")       ?: "",
                propertyType      = doc.getString("propertyType")      ?: "APARTMENT",
                status            = doc.getString("status")            ?: "PENDING",
                address           = doc.getString("address")           ?: "",
                city              = doc.getString("city")              ?: "",
                pricePerNight     = doc.getDouble("pricePerNight")     ?: 0.0,
                pricePerWeek      = doc.getDouble("pricePerWeek"),
                pricePerMonth     = doc.getDouble("pricePerMonth"),
                securityDeposit   = doc.getDouble("securityDeposit")   ?: 0.0,
                bedrooms          = (doc.getLong("bedrooms")           ?: 1L).toInt(),
                bathrooms         = (doc.getLong("bathrooms")          ?: 1L).toInt(),
                maxGuests         = (doc.getLong("maxGuests")          ?: 2L).toInt(),
                areaSqFt          = doc.getDouble("areaSqFt"),
                imageUrls         = (doc.get("imageUrls") as? List<*>)
                    ?.filterIsInstance<String>()                       ?: emptyList(),
                pt1DocumentUrl    = doc.getString("pt1DocumentUrl")    ?: "",
                drawableImageName = doc.getString("drawableImageName") ?: "",
                amenities         = (doc.get("amenities") as? List<*>)
                    ?.filterIsInstance<String>()                       ?: emptyList(),
                petsAllowed       = doc.getBoolean("petsAllowed")      ?: false,
                smokingAllowed    = doc.getBoolean("smokingAllowed")   ?: false,
                partiesAllowed    = doc.getBoolean("partiesAllowed")   ?: false,
                checkInTime       = doc.getString("checkInTime")       ?: "14:00",
                checkOutTime      = doc.getString("checkOutTime")      ?: "11:00",
                minNights         = (doc.getLong("minNights")          ?: 1L).toInt(),
                averageRating     = (doc.getDouble("averageRating")    ?: 0.0).toFloat(),
                reviewCount       = (doc.getLong("reviewCount")        ?: 0L).toInt(),
                adminNote         = doc.getString("adminNote")         ?: "",
                available         = doc.getBoolean("isAvailable")      ?: true,
                featured          = doc.getBoolean("isFeatured")       ?: false,
                createdAt         = doc.getTimestamp("createdAt"),
                updatedAt         = doc.getTimestamp("updatedAt")
            )
        } catch (e: Exception) {
            Log.e("VERIFY_VM", "parsePropertyFromDoc FAIL ${doc.id}: ${e.localizedMessage}")
            null
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // LOAD PENDING USERS (user verification list screen)
    // ════════════════════════════════════════════════════════════════════════

    fun loadPendingUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val snapshot = usersCol
                    .whereEqualTo("verificationStatus", "PENDING")
                    .get().await()
                val users = snapshot.toObjects(User::class.java)
                _uiState.update { it.copy(isLoading = false, pendingUsers = users) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage)
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // LOAD SINGLE USER BY ID (user verification detail screen)
    // ════════════════════════════════════════════════════════════════════════

    fun loadUserById(userId: String) {
        // Check cache first
        val existing = _uiState.value.pendingUsers.find { it.userId == userId }
        if (existing != null) {
            _uiState.update { it.copy(selectedUser = existing) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val doc  = usersCol.document(userId).get().await()
                val user = doc.toObject(User::class.java)
                if (user != null) {
                    _uiState.update { it.copy(isLoading = false, selectedUser = user) }
                    Log.d("VERIFY_VM", "✅ loadUserById success: $userId")
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "User document not found in Firestore")
                    }
                    Log.e("VERIFY_VM", "❌ loadUserById: toObject returned null")
                }
            } catch (e: Exception) {
                Log.e("VERIFY_VM", "loadUserById error: ${e.localizedMessage}")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage)
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PROPERTY VERIFICATION — APPROVE
    // ════════════════════════════════════════════════════════════════════════

    fun approveProperty(property: Property, adminNote: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val updateResult = propertyRepository.approveProperty(property.propertyId, adminNote)
                if (updateResult is Resource.Error) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = updateResult.message)
                    }
                    return@launch
                }

                val ownerId = property.ownerId
                if (ownerId.isNotEmpty()) {
                    notificationRepository.sendPropertyApprovedNotification(
                        ownerId       = ownerId,
                        propertyId    = property.propertyId,
                        propertyTitle = property.title,
                        adminNote     = adminNote
                    )
                    Log.d("VERIFY_VM", "✅ Approval notification sent to landlord: $ownerId")
                } else {
                    Log.e("VERIFY_VM", "❌ ownerId empty — notification not sent")
                }

                notificationHelper.showPropertyApproved(
                    propertyTitle = property.title,
                    propertyId    = property.propertyId,
                    adminNote     = adminNote
                )

                _uiState.update { state ->
                    state.copy(
                        isLoading         = false,
                        actionSuccess     = true,
                        successMessage    = "\"${property.title}\" approved! Landlord ko notification bhej di.",
                        selectedProperty  = null, // ✅ Clear selected so detail screen pops
                        pendingProperties = state.pendingProperties.filter {
                            it.propertyId != property.propertyId
                        }
                    )
                }

            } catch (e: Exception) {
                Log.e("VERIFY_VM", "approveProperty error: ${e.localizedMessage}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PROPERTY VERIFICATION — REJECT
    // ════════════════════════════════════════════════════════════════════════

    fun rejectProperty(property: Property, adminNote: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val updateResult = propertyRepository.rejectProperty(property.propertyId, adminNote)
                if (updateResult is Resource.Error) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = updateResult.message)
                    }
                    return@launch
                }

                val ownerId = property.ownerId
                if (ownerId.isNotEmpty()) {
                    notificationRepository.sendPropertyRejectedNotification(
                        ownerId       = ownerId,
                        propertyId    = property.propertyId,
                        propertyTitle = property.title,
                        adminNote     = adminNote
                    )
                    Log.d("VERIFY_VM", "✅ Rejection notification sent to: $ownerId")
                }

                notificationHelper.showPropertyRejected(
                    propertyTitle = property.title,
                    propertyId    = property.propertyId,
                    adminNote     = adminNote
                )

                _uiState.update { state ->
                    state.copy(
                        isLoading         = false,
                        actionSuccess     = true,
                        successMessage    = "\"${property.title}\" rejected. Landlord ko reason bhej diya.",
                        selectedProperty  = null, // ✅ Clear selected so detail screen pops
                        pendingProperties = state.pendingProperties.filter {
                            it.propertyId != property.propertyId
                        }
                    )
                }

            } catch (e: Exception) {
                Log.e("VERIFY_VM", "rejectProperty error: ${e.localizedMessage}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // USER VERIFICATION — VERIFY
    // ════════════════════════════════════════════════════════════════════════

    @Suppress("unused")
    fun verifyUser(user: User) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                usersCol.document(user.userId).update(
                    mapOf(
                        "verificationStatus" to "VERIFIED",
                        "isVerified"         to true,
                        "updatedAt"          to FieldValue.serverTimestamp()
                    )
                ).await()

                notificationRepository.sendUserVerifiedNotification(
                    userId   = user.userId,
                    userName = user.fullName
                )
                Log.d("VERIFY_VM", "✅ User verified notification sent: ${user.userId}")

                notificationHelper.showUserVerified(userName = user.fullName)

                _uiState.update { state ->
                    state.copy(
                        isLoading      = false,
                        actionSuccess  = true,
                        successMessage = "${user.fullName} verified! User ko notification bhej di.",
                        selectedUser   = null,
                        pendingUsers   = state.pendingUsers.filter { it.userId != user.userId }
                    )
                }

            } catch (e: Exception) {
                Log.e("VERIFY_VM", "verifyUser error: ${e.localizedMessage}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // USER VERIFICATION — REJECT
    // ════════════════════════════════════════════════════════════════════════

    fun rejectUser(user: User, reason: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                usersCol.document(user.userId).update(
                    mapOf(
                        "verificationStatus" to "REJECTED",
                        "isVerified"         to false,
                        "updatedAt"          to FieldValue.serverTimestamp()
                    )
                ).await()

                notificationRepository.sendUserRejectedNotification(
                    userId = user.userId,
                    reason = reason
                )
                Log.d("VERIFY_VM", "✅ User rejected notification sent: ${user.userId}")

                notificationHelper.showUserRejected(
                    userName = user.fullName,
                    reason   = reason
                )

                _uiState.update { state ->
                    state.copy(
                        isLoading      = false,
                        actionSuccess  = true,
                        successMessage = "${user.fullName} ka verification reject kiya.",
                        selectedUser   = null,
                        pendingUsers   = state.pendingUsers.filter { it.userId != user.userId }
                    )
                }

            } catch (e: Exception) {
                Log.e("VERIFY_VM", "rejectUser error: ${e.localizedMessage}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }
}