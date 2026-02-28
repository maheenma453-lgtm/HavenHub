package com.example.havenhub.di

import com.example.havenhub.remote.FirebaseAuthManager
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.remote.FirebaseMessagingManager
import com.example.havenhub.remote.FirebaseRealtimeListener
import com.example.havenhub.remote.FirebaseStorageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // ── Firebase SDK ─────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth =
        FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage =
        FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging =
        FirebaseMessaging.getInstance()

    // ── Firebase Managers ────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideFirebaseAuthManager(
        auth: FirebaseAuth
    ): FirebaseAuthManager = FirebaseAuthManager(auth)

    @Provides
    @Singleton
    fun provideFirebaseDataManager(
        firestore: FirebaseFirestore
    ): FirebaseDataManager = FirebaseDataManager(firestore)

    @Provides
    @Singleton
    fun provideFirebaseStorageManager(
        storage: FirebaseStorage
    ): FirebaseStorageManager = FirebaseStorageManager(storage)

    @Provides
    @Singleton
    fun provideFirebaseRealtimeListener(
        firestore: FirebaseFirestore
    ): FirebaseRealtimeListener = FirebaseRealtimeListener(firestore)

    @Provides
    @Singleton
    fun provideFirebaseMessagingManager(
        firestore: FirebaseFirestore,
        fcm: FirebaseMessaging
    ): FirebaseMessagingManager = FirebaseMessagingManager(firestore, fcm)
}






























