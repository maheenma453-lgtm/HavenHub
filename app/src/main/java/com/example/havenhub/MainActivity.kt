package com.example.havenhub

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase initialize
        FirebaseApp.initializeApp(this)

        // Firebase test
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("test")
        ref.setValue("Firebase Connected Successfully")
            .addOnSuccessListener {
                Log.d("FirebaseTest", "Data set successfully")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseTest", "Error: ${e.message}")
            }
    }
    }
