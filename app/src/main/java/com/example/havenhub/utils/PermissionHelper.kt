package com.example.havenhub.utils
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class PermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ── Check single permission ───────────────────────────────────────
    fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    // ── Camera ────────────────────────────────────────────────────────
    fun hasCameraPermission(): Boolean = isGranted(Manifest.permission.CAMERA)

    // ── Storage / Gallery ─────────────────────────────────────────────
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // ── Location ──────────────────────────────────────────────────────
    fun hasLocationPermission(): Boolean {
        return isGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
                isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    // ── Notifications (Android 13+) ───────────────────────────────────
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    // ── Image picker permissions list ─────────────────────────────────
    fun getImagePickerPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // ── Location permissions list ─────────────────────────────────────
    fun getLocationPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    // ── Check multiple permissions ────────────────────────────────────
    fun allGranted(permissions: Array<String>): Boolean {
        return permissions.all { isGranted(it) }
    }

    // ── Open app settings (when permission permanently denied) ────────
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}