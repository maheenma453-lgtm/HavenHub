package com.example.havenhub.di

import android.content.Context
import com.example.havenhub.remote.FirebaseAuthManager
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.remote.FirebaseMessagingManager
import com.example.havenhub.remote.FirebaseRealtimeListener
import com.example.havenhub.remote.ImgBBUploadManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance()

    // ✅ FirebaseStorage HATA DIYA
    // ✅ ImgBBUploadManager ADD kiya
    @Provides
    @Singleton
    fun provideImgBBUploadManager(
        @ApplicationContext context: Context
    ): ImgBBUploadManager = ImgBBUploadManager(context)

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging =
        FirebaseMessaging.getInstance()

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
    fun provideFirebaseRealtimeListener(
        firestore: FirebaseFirestore
    ): FirebaseRealtimeListener = FirebaseRealtimeListener(firestore)

    @Provides
    @Singleton
    fun provideFirebaseMessagingManager(
        firestore: FirebaseFirestore,
        fcm      : FirebaseMessaging
    ): FirebaseMessagingManager = FirebaseMessagingManager(firestore, fcm)

}