package com.example.havenhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraph
import com.example.havenhub.ui.theme.HavenHubTheme
//import com.example.havenhub.navigation.NavGraph  // tumhare navigation folder se
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HavenHubTheme {
                NavGraph()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    HavenHubTheme {
        NavGraph()
    }
}