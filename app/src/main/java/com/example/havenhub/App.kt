package com.example.havenhub

import android.app.Application
import com.google.firebase.FirebaseApp

class HavenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase initialize
        FirebaseApp.initializeApp(this)
    }
}
