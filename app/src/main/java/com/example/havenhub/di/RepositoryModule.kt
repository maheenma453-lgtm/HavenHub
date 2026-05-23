package com.example.havenhub.di

import android.content.Context
import com.example.havenhub.remote.FirebaseAuthManager
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.remote.FirebaseMessagingManager
import com.example.havenhub.remote.FirebaseRealtimeListener
import com.example.havenhub.remote.ImgBBUploadManager
import com.example.havenhub.repository.AdminRepository
import com.example.havenhub.repository.AuthRepository
import com.example.havenhub.repository.BookingRepository
import com.example.havenhub.repository.MessagingRepository
import com.example.havenhub.repository.NotificationRepository
import com.example.havenhub.repository.PaymentRepository
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.repository.ReviewRepository
import com.example.havenhub.repository.SeasonalAlertRepository   // ✦ NEW
import com.example.havenhub.repository.SettingsRepository
import com.example.havenhub.utils.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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

    // ══════════════════════════════════════════════════════════════════════════
    // FIREBASE CORE
    //
    // FirebaseAuth, FirebaseFirestore, FirebaseMessaging provided here.
    // FirebaseDatabase (Realtime DB) is provided in FirebaseModule.
    // Hilt merges both modules automatically so all are injectable everywhere.
    // ══════════════════════════════════════════════════════════════════════════

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
    fun provideFirebaseMessaging(): FirebaseMessaging =
        FirebaseMessaging.getInstance()

    // ══════════════════════════════════════════════════════════════════════════
    // FIREBASE REMOTE MANAGERS
    // ══════════════════════════════════════════════════════════════════════════

    @Provides
    @Singleton
    fun provideImgBBUploadManager(
        @ApplicationContext context: Context
    ): ImgBBUploadManager = ImgBBUploadManager(context)

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

    // FirebaseRealtimeListener needs both Firestore and Realtime DB.
    // Hilt injects FirebaseDatabase from FirebaseModule automatically.
    @Provides
    @Singleton
    fun provideFirebaseRealtimeListener(
        firestore       : FirebaseFirestore,
        realtimeDatabase: FirebaseDatabase
    ): FirebaseRealtimeListener = FirebaseRealtimeListener(firestore, realtimeDatabase)

    @Provides
    @Singleton
    fun provideFirebaseMessagingManager(
        firestore: FirebaseFirestore,
        fcm      : FirebaseMessaging
    ): FirebaseMessagingManager = FirebaseMessagingManager(firestore, fcm)

    // ══════════════════════════════════════════════════════════════════════════
    // UTILS
    // ══════════════════════════════════════════════════════════════════════════

    @Provides
    @Singleton
    fun providePreferenceManager(
        @ApplicationContext context: Context
    ): PreferenceManager = PreferenceManager(context)

    // ══════════════════════════════════════════════════════════════════════════
    // REPOSITORIES
    // ══════════════════════════════════════════════════════════════════════════

    @Provides
    @Singleton
    fun provideNotificationRepository(
        firestore       : FirebaseFirestore,
        realtimeListener: FirebaseRealtimeListener
    ): NotificationRepository = NotificationRepository(firestore, realtimeListener)

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth     : FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository = AuthRepository(auth, firestore)

    @Provides
    @Singleton
    fun providePropertyRepository(
        dataManager           : FirebaseDataManager,
        imgBBManager          : ImgBBUploadManager,
        firestore             : FirebaseFirestore,
        notificationRepository: NotificationRepository
    ): PropertyRepository = PropertyRepository(
        dataManager,
        imgBBManager,
        firestore,
        notificationRepository
    )

    @Provides
    @Singleton
    fun provideBookingRepository(
        dataManager           : FirebaseDataManager,
        realtimeListener      : FirebaseRealtimeListener,
        firestore             : FirebaseFirestore,
        notificationRepository: NotificationRepository
    ): BookingRepository = BookingRepository(
        dataManager,
        realtimeListener,
        firestore,
        notificationRepository
    )

    @Provides
    @Singleton
    fun providePaymentRepository(
        firestore             : FirebaseFirestore,
        notificationRepository: NotificationRepository
    ): PaymentRepository = PaymentRepository(firestore, notificationRepository)

    @Provides
    @Singleton
    fun provideReviewRepository(
        dataManager           : FirebaseDataManager,
        firestore             : FirebaseFirestore,
        notificationRepository: NotificationRepository
    ): ReviewRepository = ReviewRepository(dataManager, firestore, notificationRepository)

    @Provides
    @Singleton
    fun provideAdminRepository(
        firestore             : FirebaseFirestore,
        notificationRepository: NotificationRepository
    ): AdminRepository = AdminRepository(firestore, notificationRepository)

    @Provides
    @Singleton
    fun provideMessagingRepository(
        firestore          : FirebaseFirestore,
        firebaseDataManager: FirebaseDataManager
    ): MessagingRepository = MessagingRepository(firestore, firebaseDataManager)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        preferenceManager: PreferenceManager,
        firestore        : FirebaseFirestore
    ): SettingsRepository = SettingsRepository(preferenceManager, firestore)

    // ✦ NEW ── Seasonal Alert Repository ─────────────────────────────────────
    // Only needs Firestore — reads/writes from seasonal_alerts collection.
    // No NotificationRepository dependency needed here because seasonal alerts
    // are fetched directly from Firestore by the UI layer, not pushed as
    // individual notifications to each user.
    @Provides
    @Singleton
    fun provideSeasonalAlertRepository(
        firestore: FirebaseFirestore
    ): SeasonalAlertRepository = SeasonalAlertRepository(firestore)
}