package com.example.havenhub.remote

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.havenhub.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImgBBUploadManager @Inject constructor(
    private val context: Context
) {

    private val apiKey = "a7d0e9e8ea6147cef2d4648ac1c4fab9"

    // ── Uri → Base64 ──────────────────────────────────────────────────────────
    private fun uriToBase64(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes       = inputStream?.readBytes() ?: byteArrayOf()
        inputStream?.close()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    // ── Single image upload ───────────────────────────────────────────────────
    suspend fun uploadImage(imageUri: Uri): Resource<String> =
        withContext(Dispatchers.IO) {
            try {
                val base64 = uriToBase64(imageUri)
                val client = OkHttpClient()

                val body = FormBody.Builder()
                    .add("key", apiKey)
                    .add("image", base64)
                    .build()

                val request = Request.Builder()
                    .url("https://api.imgbb.com/1/upload")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val json     = JSONObject(response.body?.string() ?: "")
                val url      = json
                    .getJSONObject("data")
                    .getString("url")

                Resource.Success(url)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Image upload failed")
            }
        }

    // ── Multiple images upload ────────────────────────────────────────────────
    suspend fun uploadImages(imageUris: List<Uri>): Resource<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val urls = mutableListOf<String>()
                for (uri in imageUris) {
                    val result = uploadImage(uri)
                    if (result is Resource.Error)
                        return@withContext Resource.Error(
                            result.message ?: "Upload failed"
                        )
                    urls.add((result as Resource.Success).data ?: "")
                }
                Resource.Success(urls)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Images upload failed")
            }
        }
}