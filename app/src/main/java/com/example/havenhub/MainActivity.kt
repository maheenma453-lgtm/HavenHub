package com.example.havenhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.havenhub.navigation.HavenHubNavGraph
import com.example.havenhub.ui.theme.HavenHubTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HavenHubTheme {
                val navController = rememberNavController()
                HavenHubNavGraph(navController = navController)
            }
        }
    }
}