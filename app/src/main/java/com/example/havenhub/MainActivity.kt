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
import com.example.havenhub.utils.Constants
import com.example.havenhub.utils.NotificationHelper
import com.example.havenhub.utils.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    companion object {
        val darkModeFlow = MutableStateFlow(false)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
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

        // ── Create notification channels ──────────────────────────────────────
        notificationHelper.createNotificationChannels()

        // ── Request POST_NOTIFICATIONS on Android 13+ ─────────────────────────
        askNotificationPermission()

        // ── FCM Token fetch + Firestore mein save ─────────────────────────────
        fetchAndSaveFcmToken()

        setContent {
            val isDarkMode by darkModeFlow.collectAsState()
            HavenHubTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                HavenHubNavGraph(navController = navController)
            }
        }
    }

    // ── FCM Token: Firestore mein save karo ───────────────────────────────────
    private fun fetchAndSaveFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                android.util.Log.d("FCM_TOKEN", "✅ Token: $token")

                // SharedPreferences mein bhi save karo (HavenHubMessagingService use karta hai)
                getSharedPreferences("haven_prefs", MODE_PRIVATE)
                    .edit().putString(Constants.PREF_FCM_TOKEN, token).apply()

                // Firestore mein save karo — user logged in ho tab
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    saveFcmTokenToFirestore(userId, token)
                } else {
                    // User abhi logged in nahi — login hone par save hoga
                    FirebaseAuth.getInstance().addAuthStateListener { auth ->
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            saveFcmTokenToFirestore(uid, token)

                            // Role-based topic subscribe karo
                            subscribeToRoleTopic(uid, token)
                        }
                    }
                }

                // Agar user already logged in hai to role topic bhi subscribe karo
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUid != null) {
                    subscribeToRoleTopic(currentUid, token)
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FCM_TOKEN", "❌ Token fetch failed: ${e.message}")
            }
    }

    // ── Firestore mein fcmToken field update karo ─────────────────────────────
    private fun saveFcmTokenToFirestore(userId: String, token: String) {
        FirebaseFirestore.getInstance()
            .collection(Constants.COLLECTION_USERS)
            .document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                android.util.Log.d("FCM_TOKEN", "✅ Token Firestore mein save ho gaya — userId=$userId")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FCM_TOKEN", "❌ Firestore save failed: ${e.message}")
            }
    }

    // ── Role ke hisaab se FCM topic subscribe karo ────────────────────────────
    // Taake server ek hi call mein sare tenants/landlords/admins ko notify kar sake
    private fun subscribeToRoleTopic(userId: String, token: String) {
        FirebaseFirestore.getInstance()
            .collection(Constants.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role")?.uppercase()?.trim() ?: Constants.ROLE_TENANT

                // SharedPreferences mein role save karo (HavenHubMessagingService use karta hai)
                getSharedPreferences("haven_prefs", MODE_PRIVATE)
                    .edit()
                    .putString(Constants.PREF_USER_ROLE, role)
                    .putString(Constants.PREF_USER_ID, userId)
                    .apply()

                val roleTopic = when (role) {
                    Constants.ROLE_ADMIN    -> Constants.TOPIC_ADMIN
                    Constants.ROLE_LANDLORD -> Constants.TOPIC_LANDLORD
                    else                    -> Constants.TOPIC_TENANT
                }

                // Role-specific topic subscribe
                FirebaseMessaging.getInstance().subscribeToTopic(roleTopic)
                    .addOnSuccessListener {
                        android.util.Log.d("FCM_TOKEN", "✅ Topic subscribe: $roleTopic")
                    }

                // Sab ko jaane wali notifications ke liye
                FirebaseMessaging.getInstance().subscribeToTopic(Constants.TOPIC_ALL)
                    .addOnSuccessListener {
                        android.util.Log.d("FCM_TOKEN", "✅ Topic subscribe: ${Constants.TOPIC_ALL}")
                    }
            }
    }

    // ── Notification Permission (Android 13+) ─────────────────────────────────
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
    }
}