package com.example.havenhub

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

// ✅ ZAROORI: Ye annotation Hilt ko initialize karne ke liye lazmi hai
@HiltAndroidApp
class HavenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase initialize (Perfectly placed here)
        FirebaseApp.initializeApp(this)
    }
}
