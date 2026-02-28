package com.example.havenhub.di
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * FirebaseModule — Firebase SDK instances
 *
 * Provides singleton instances of Firebase services used across
 * the app. Only Firebase Realtime Database and Auth are included
 * since those are the active services in this project.
 *
 * All instances are @Singleton — Firebase itself is a singleton
 * under the hood, but wrapping in Hilt ensures consistent
 * injection everywhere.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Provides FirebaseAuth instance for login, signup,
     * logout and password reset operations.
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth =
        FirebaseAuth.getInstance()

    /**
     * Provides FirebaseDatabase instance (Realtime Database).
     * Used for chat messages, notifications and live data sync.
     */
    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase =
        FirebaseDatabase.getInstance()
}
