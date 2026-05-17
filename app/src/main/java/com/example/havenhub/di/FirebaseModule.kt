package com.example.havenhub.di

import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * FirebaseModule — Firebase SDK instances
 *
 * Note: FirebaseAuth aur FirebaseFirestore already RepositoryModule
 * mein provide ho rahe hain — yahan sirf FirebaseDatabase hai
 * jo RepositoryModule mein nahi tha.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Provides FirebaseDatabase instance (Realtime Database).
     * Used for chat messages, notifications and live data sync.
     */
    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase =
        FirebaseDatabase.getInstance()
}