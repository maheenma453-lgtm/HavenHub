package com.example.havenhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.example.havenhub.navigation.HavenHubNavGraph
import com.example.havenhub.ui.theme.HavenHubTheme
import com.example.havenhub.utils.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    // FIX: Dark mode ka global state flow
    // SettingsViewModel yahan se value update karega — poori app instantly react karegi
    companion object {
        val darkModeFlow = MutableStateFlow(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // App open hone pe SharedPreferences se saved value restore karo
        darkModeFlow.value = preferenceManager.isDarkMode()

        setContent {
            // FIX: darkModeFlow observe karo — toggle pe instantly theme change hogi
            val isDarkMode by darkModeFlow.collectAsState()

            HavenHubTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                HavenHubNavGraph(navController = navController)
            }
        }
    }
}