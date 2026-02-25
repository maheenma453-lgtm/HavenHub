package com.example.havenhub.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.example.havenhub.utils.Resource  // ✅ fixed
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageManager @Inject constructor(
    private val storage: FirebaseStorage
) {

    private fun profileImageRef(uid: String): StorageReference =
        storage.reference.child("profile_images/$uid/profile.jpg")

    private fun propertyImageRef(propertyId: String, fileName: String): StorageReference =
        storage.reference.child("property_images/$propertyId/$fileName")

    private fun propertyDocumentRef(propertyId: String, fileName: String): StorageReference =
        storage.reference.child("property_documents/$propertyId/$fileName")

    private fun idDocumentRef(uid: String, fileName: String): StorageReference =
        storage.reference.child("id_documents/$uid/$fileName")

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

    suspend fun uploadPropertyImages(
        propertyId: String,
        imageUris: List<Uri>
    ): Resource<List<String>> {
        return try {
            val downloadUrls = mutableListOf<String>()
            imageUris.forEachIndexed { index, uri ->
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

    suspend fun deletePropertyImage(imageUrl: String): Resource<Unit> {
        return try {
            storage.getReferenceFromUrl(imageUrl).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete image")
        }
    }

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

    suspend fun deleteFile(fileUrl: String): Resource<Unit> {
        return try {
            storage.getReferenceFromUrl(fileUrl).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete file")
        }
    }
}