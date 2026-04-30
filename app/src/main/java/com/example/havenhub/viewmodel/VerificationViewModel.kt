package com.example.havenhub.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.data.User
import com.example.havenhub.repository.NotificationRepository
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.utils.Constants
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
    val isLoading         : Boolean        = false,
    val pendingProperties : List<Property> = emptyList(),
    val pendingUsers      : List<User>     = emptyList(),
    val errorMessage      : String?        = null,
    val successMessage    : String?        = null,
    val actionSuccess     : Boolean        = false
)

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val propertyRepository     : PropertyRepository,
    private val notificationRepository : NotificationRepository,
    private val notificationHelper     : NotificationHelper,   // ✅ KEPT — device bell notification ke liye
    private val firestore              : FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    private val usersCol = firestore.collection("users")

    // ── Reset action state ────────────────────────────────────────────────────
    fun resetActionState() {
        _uiState.update {
            it.copy(
                actionSuccess  = false,
                errorMessage   = null,
                successMessage = null
            )
        }
    }

    // ── Clear messages ────────────────────────────────────────────────────────
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // ── Load pending properties ───────────────────────────────────────────────
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

    // ── Load pending users ────────────────────────────────────────────────────
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

    // ══════════════════════════════════════════════════════════════════════════
    // PROPERTY VERIFICATION
    // ══════════════════════════════════════════════════════════════════════════

    fun approveProperty(property: Property, adminNote: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Step 1: Firestore status update
                val updateResult = propertyRepository.approveProperty(property.propertyId, adminNote)
                if (updateResult is Resource.Error) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = updateResult.message)
                    }
                    return@launch
                }

                // Step 2: Landlord ko in-app Firestore notification
                // ✅ FIX: Property.kt mein sirf 'ownerId' hai — 'landlordId' exist nahi karta
                val ownerId = property.ownerId
                if (ownerId.isNotEmpty()) {
                    notificationRepository.sendPropertyApprovedNotification(
                        ownerId       = ownerId,
                        propertyId    = property.propertyId,
                        propertyTitle = property.title,
                        adminNote     = adminNote
                    )
                    Log.d("VERIFY_VM", "✅ In-app notification sent to landlord: $ownerId")
                } else {
                    Log.e("VERIFY_VM", "❌ ownerId empty — in-app notification not sent")
                }

                // Step 3: Device bell notification (landlord ka device)
                // ✅ KEPT: notificationHelper use ho raha hai — unused warning fix
                notificationHelper.showPropertyApproved(
                    propertyTitle = property.title,
                    propertyId    = property.propertyId,
                    adminNote     = adminNote
                )

                // Step 4: UI update
                _uiState.update { state ->
                    state.copy(
                        isLoading         = false,
                        actionSuccess     = true,
                        successMessage    = "\"${property.title}\" approved! Landlord ko notification bhej di.",
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

    fun rejectProperty(property: Property, adminNote: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Step 1: Firestore status update
                val updateResult = propertyRepository.rejectProperty(property.propertyId, adminNote)
                if (updateResult is Resource.Error) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = updateResult.message)
                    }
                    return@launch
                }

                // Step 2: Landlord ko in-app notification
                // ✅ FIX: sirf ownerId — landlordId nahi
                val ownerId = property.ownerId
                if (ownerId.isNotEmpty()) {
                    notificationRepository.sendPropertyRejectedNotification(
                        ownerId       = ownerId,
                        propertyId    = property.propertyId,
                        propertyTitle = property.title,
                        adminNote     = adminNote
                    )
                    Log.d("VERIFY_VM", "✅ In-app rejection notification sent to: $ownerId")
                }

                // Step 3: Device bell notification
                notificationHelper.showPropertyRejected(
                    propertyTitle = property.title,
                    propertyId    = property.propertyId,
                    adminNote     = adminNote
                )

                // Step 4: UI update
                _uiState.update { state ->
                    state.copy(
                        isLoading         = false,
                        actionSuccess     = true,
                        successMessage    = "\"${property.title}\" rejected. Landlord ko reason bhej diya.",
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

    // ══════════════════════════════════════════════════════════════════════════
    // USER VERIFICATION
    // ══════════════════════════════════════════════════════════════════════════

    // ✅ NOTE: "verifyUser is never used" — ye sirf IDE warning hai (yellow, error nahi).
    //    UserVerificationDetailScreen se call hota hai. Suppress karo ya ignore karo.
    @Suppress("unused")
    fun verifyUser(user: User) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Step 1: Firestore update
                usersCol.document(user.userId).update(
                    mapOf(
                        "verificationStatus" to "VERIFIED",
                        "isVerified"         to true,
                        "updatedAt"          to FieldValue.serverTimestamp()
                    )
                ).await()

                // Step 2: In-app notification
                notificationRepository.sendUserVerifiedNotification(
                    userId   = user.userId,
                    userName = user.fullName
                )
                Log.d("VERIFY_VM", "✅ User verified notification sent: ${user.userId}")

                // Step 3: Device bell notification
                notificationHelper.showUserVerified(userName = user.fullName)

                // Step 4: UI update
                _uiState.update { state ->
                    state.copy(
                        isLoading      = false,
                        actionSuccess  = true,
                        successMessage = "${user.fullName} verified! User ko notification bhej di.",
                        pendingUsers   = state.pendingUsers.filter { it.userId != user.userId }
                    )
                }

            } catch (e: Exception) {
                Log.e("VERIFY_VM", "verifyUser error: ${e.localizedMessage}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun rejectUser(user: User, reason: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Step 1: Firestore update
                usersCol.document(user.userId).update(
                    mapOf(
                        "verificationStatus" to "REJECTED",
                        "isVerified"         to false,
                        "updatedAt"          to FieldValue.serverTimestamp()
                    )
                ).await()

                // Step 2: In-app notification
                notificationRepository.sendUserRejectedNotification(
                    userId = user.userId,
                    reason = reason
                )
                Log.d("VERIFY_VM", "✅ User rejected notification sent: ${user.userId}")

                // Step 3: Device bell notification
                notificationHelper.showUserRejected(
                    userName = user.fullName,
                    reason   = reason
                )

                // Step 4: UI update
                _uiState.update { state ->
                    state.copy(
                        isLoading      = false,
                        actionSuccess  = true,
                        successMessage = "${user.fullName} ka verification reject kiya.",
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