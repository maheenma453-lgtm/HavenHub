package com.example.havenhub

import android.app.Application
import com.example.havenhub.utils.NotificationHelper
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HavenApp : Application() {

    @Inject
    lateinit var notificationHelper: NotificationHelper  // ✅ ADD

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        notificationHelper.createNotificationChannels()  // ✅ ADD — yeh missing tha!
    }
}