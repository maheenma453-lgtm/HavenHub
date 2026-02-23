package com.example.havenhub.remote
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.havenhub.utils.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FirebaseStorageManager
 *
 * Manages all file upload/download/delete operations via Firebase Storage.
 *
 * Storage Bucket Structure:
 * ├── profile_images/{uid}/profile.jpg          → User avatar images
 * ├── property_images/{propertyId}/{filename}   → Property listing photos
 * ├── property_documents/{propertyId}/{filename}→ Ownership/legal documents
 * └── id_documents/{uid}/{filename}             → User KYC/ID verification docs
 *
 * All operations return a [Resource] wrapper for safe error propagation.
 */
@Singleton
class FirebaseStorageManager @Inject constructor(
    private val storage: FirebaseStorage
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Storage References (path builders)
    // ─────────────────────────────────────────────────────────────────────────

    private fun profileImageRef(uid: String): StorageReference =
        storage.reference.child("profile_images/$uid/profile.jpg")

    private fun propertyImageRef(propertyId: String, fileName: String): StorageReference =
        storage.reference.child("property_images/$propertyId/$fileName")

    private fun propertyDocumentRef(propertyId: String, fileName: String): StorageReference =
        storage.reference.child("property_documents/$propertyId/$fileName")

    private fun idDocumentRef(uid: String, fileName: String): StorageReference =
        storage.reference.child("id_documents/$uid/$fileName")

    // ─────────────────────────────────────────────────────────────────────────
    // Profile Image
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Uploads a user's profile image to Firebase Storage.
     * Replaces any existing profile image for the given user.
     *
     * @param uid      The user's Firebase Auth UID.
     * @param imageUri The local [Uri] of the selected image file.
     * @return [Resource.Success] with the public download URL string,
     *         or [Resource.Error] on failure.
     */
    suspend fun uploadProfileImage(uid: String, imageUri: Uri): Resource<String> {
        return try {
            val ref = profileImageRef(uid)
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to upload profile image")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Property Images
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Uploads a single property image to Firebase Storage.
     *
     * @param propertyId The Firestore document ID of the property.
     * @param imageUri   The local [Uri] of the image file.
     * @param fileName   A unique filename (e.g., UUID-based) to avoid overwrites.
     * @return [Resource.Success] with the public download URL string,
     *         or [Resource.Error] on failure.
     */
    suspend fun uploadPropertyImage(
        propertyId: String,
        imageUri: Uri,
        fileName: String
    ): Resource<String> {
        return try {
            val ref = propertyImageRef(propertyId, fileName)
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to upload property image")
        }
    }

    /**
     * Uploads multiple property images sequentially and returns a list of URLs.
     * Stops and returns an error if any single upload fails.
     *
     * @param propertyId The Firestore document ID of the property.
     * @param imageUris  A list of local image [Uri]s to upload.
     * @return [Resource.Success] with a list of download URLs,
     *         or [Resource.Error] if any upload fails.
     */
    suspend fun uploadPropertyImages(
        propertyId: String,
        imageUris: List<Uri>
    ): Resource<List<String>> {
        return try {
            val downloadUrls = mutableListOf<String>()
            imageUris.forEachIndexed { index, uri ->
                // Use timestamp + index to guarantee unique file names
                val fileName = "${System.currentTimeMillis()}_$index.jpg"
                val ref = propertyImageRef(propertyId, fileName)
                ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                downloadUrls.add(url)
            }
            Resource.Success(downloadUrls)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to upload property images")
        }
    }

    /**
     * Deletes a property image from Firebase Storage by its full download URL.
     *
     * @param imageUrl The full Firebase Storage URL of the image to delete.
     * @return [Resource.Success] with Unit, or [Resource.Error] on failure.
     */
    suspend fun deletePropertyImage(imageUrl: String): Resource<Unit> {
        return try {
            storage.getReferenceFromUrl(imageUrl).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete image")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Property Documents (Ownership / Legal)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Uploads a legal/ownership document for a property listing.
     * Used during the property verification flow.
     *
     * @param propertyId The property's Firestore document ID.
     * @param docUri     The local [Uri] of the document file (PDF/image).
     * @param fileName   A unique filename for storage.
     * @return [Resource.Success] with the download URL, or [Resource.Error].
     */
    suspend fun uploadPropertyDocument(
        propertyId: String,
        docUri: Uri,
        fileName: String
    ): Resource<String> {
        return try {
            val ref = propertyDocumentRef(propertyId, fileName)
            ref.putFile(docUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to upload property document")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ID / KYC Documents
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Uploads a KYC/ID verification document for a user.
     * Used in the user identity verification flow.
     *
     * @param uid      The user's Firebase Auth UID.
     * @param docUri   The local [Uri] of the ID document.
     * @param fileName A unique filename for storage.
     * @return [Resource.Success] with the download URL, or [Resource.Error].
     */
    suspend fun uploadIdDocument(
        uid: String,
        docUri: Uri,
        fileName: String
    ): Resource<String> {
        return try {
            val ref = idDocumentRef(uid, fileName)
            ref.putFile(docUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to upload ID document")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Generic Delete (by URL)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Deletes any file from Firebase Storage using its full storage URL.
     *
     * @param fileUrl The full Firebase Storage URL of the file to delete.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun deleteFile(fileUrl: String): Resource<Unit> {
        return try {
            storage.getReferenceFromUrl(fileUrl).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete file")
        }
    }
}


