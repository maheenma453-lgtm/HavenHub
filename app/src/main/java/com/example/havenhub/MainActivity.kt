package com.example.havenhub

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.havenhub.navigation.HavenHubNavGraph
import com.example.havenhub.ui.theme.HavenHubTheme
import com.example.havenhub.utils.NotificationHelper
import com.example.havenhub.utils.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var notificationHelper: NotificationHelper   // ✅ Hilt inject

    // Global dark mode state — SettingsViewModel yahan se update karega
    companion object {
        val darkModeFlow = MutableStateFlow(false)
    }

    // ── Android 13+ notification permission launcher ──────────────────────────
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted — channels already created below, nothing extra needed
            android.util.Log.d("MainActivity", "POST_NOTIFICATIONS permission granted")
        } else {
            android.util.Log.w("MainActivity", "POST_NOTIFICATIONS permission denied — push notifications won't show")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── Restore saved dark mode preference ───────────────────────────────
        darkModeFlow.value = preferenceManager.isDarkMode()

        // ── Create notification channels (safe to call every launch) ─────────
        // FIX: Without this, pop-up and lock screen notifications never show.
        // Must be called before any notification is posted.
        notificationHelper.createNotificationChannels()

        // ── Request POST_NOTIFICATIONS on Android 13+ ────────────────────────
        // FIX: Android 13 (API 33) requires explicit user permission.
        // Without this, NotificationHelper silently skips posting notifications.
        askNotificationPermission()

        setContent {
            val isDarkMode by darkModeFlow.collectAsState()

            HavenHubTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                HavenHubNavGraph(navController = navController)
            }
        }
    }

    // ── Permission helper ─────────────────────────────────────────────────────
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!alreadyGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        // Below Android 13 — permission not needed, notifications work automatically
    }
}
