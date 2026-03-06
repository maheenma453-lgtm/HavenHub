package com.example.havenhub.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.havenhub.screens.*

@Composable
fun HavenHubNavGraph(
    navController: NavHostController,
    unreadMessageCount: Int = 0
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route, // ✅ Start Point
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── SEQUENCE: Splash -> Onboarding -> RoleSelection -> SignUp ──

            composable(Screen.Splash.route) {
                SplashScreen(navController)
            }

            composable(Screen.Onboarding.route) {
                // Note: Is screen ke button par RoleSelection ka rasta hona chahiye
                OnboardingScreen(navController)
            }

            composable(Screen.RoleSelection.route) {
                // Note: Iske Continue par SignUp ka rasta hona chahiye
                RoleSelectionScreen(navController)
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(navController)
            }

            composable(Screen.SignIn.route) {
                SignInScreen(navController)
            }

            composable(Screen.Home.route) {
                HomeScreen(navController)
            }

            // ... (Baaki saari screens: Search, Profile, Chat etc.)
        }
    }
}