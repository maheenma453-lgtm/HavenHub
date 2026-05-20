package com.example.havenhub.di

import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ══════════════════════════════════════════════════════════════════════════════
// FirebaseModule
//
// This module provides Firebase services that are NOT already in RepositoryModule.
//
// Already provided in RepositoryModule — do NOT duplicate:
//   • FirebaseAuth       → provideFirebaseAuth()
//   • FirebaseFirestore  → provideFirebaseFirestore()
//   • FirebaseMessaging  → provideFirebaseMessaging()
//
// Only addition here:
//   • FirebaseDatabase (Realtime DB) → for user online presence only
//
// Why a separate Realtime Database?
//   Firestore has no .onDisconnect() support. If the app crashes or loses
//   network, Firestore cannot auto-mark the user offline. Realtime Database
//   runs .onDisconnect() server-side — even on unexpected disconnections —
//   so the user is never stuck showing "Online" after a crash.
// ══════════════════════════════════════════════════════════════════════════════

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    // ── Firebase Realtime Database ────────────────────────────────────────────
    // Used exclusively for: /status/{userId}/isOnline + lastSeen
    // Everything else (messages, bookings, properties) stays in Firestore.
    //
    // setPersistenceEnabled(true) → local disk cache so brief network drops
    // don't immediately flip the user to offline on the listener side.
    //
    // @Singleton guarantees this runs exactly once — calling
    // setPersistenceEnabled() twice on the same instance throws an exception.
    @Provides
    @Singleton
    fun provideFirebaseRealtimeDatabase(): FirebaseDatabase {
        val database = FirebaseDatabase.getInstance()
        try {
            database.setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Safe to ignore — already enabled if getInstance() was called
            // before Hilt ran this provider (e.g. during app startup)
        }
        return database
    }
}