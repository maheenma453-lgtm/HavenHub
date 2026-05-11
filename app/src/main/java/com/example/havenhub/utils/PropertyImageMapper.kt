package com.example.havenhub.utils

import com.example.havenhub.R

fun getPropertyImage(key: String): Int {
    // ✅ FIX: trim() — trailing spaces kabhi match fail nahi karenge
    return when (key.trim()) {

        // ── Prop IDs (Firestore document IDs) ────────────────────────────────
        "prop_001"  -> R.drawable.apartment_lahore
        "prop_002"  -> R.drawable.house_karachi
        "prop_003"  -> R.drawable.room_islamabad
        "prop_004"  -> R.drawable.rawalpindi_apt
        "prop_005"  -> R.drawable.studio_faislabad   // ✅ trailing space fix
        "prop_006"  -> R.drawable.room_sialkot       // ✅ trailing space fix
        "prop_007"  -> R.drawable.vila_murree
        "prop_008"  -> R.drawable.farmhouse_naran
        "prop_009"  -> R.drawable.kaghan_valleyhouse
        "prop_010"  -> R.drawable.swat_vila          // ✅ trailing space fix
        "prop_011"  -> R.drawable.farmhouse_hunza
        "prop_012"  -> R.drawable.skardu_house

        // ── Drawable names (resolvedDrawableName / drawableImageName) ─────────
        "apartment_lahore"     -> R.drawable.apartment_lahore
        "house_karachi"        -> R.drawable.house_karachi
        "room_islamabad"       -> R.drawable.room_islamabad
        "rawalpindi_apt"       -> R.drawable.rawalpindi_apt
        "apartment_rawalpindi" -> R.drawable.rawalpindi_apt
        "studio_faislabad"     -> R.drawable.studio_faislabad
        "studio_faisalabad"    -> R.drawable.studio_faislabad   // ✅ spelling variant
        "room_sialkot"         -> R.drawable.room_sialkot
        "vila_murree"          -> R.drawable.vila_murree
        "farmhouse_naran"      -> R.drawable.farmhouse_naran
        "naran_farmhouse"      -> R.drawable.farmhouse_naran    // ✅ old name variant
        "kaghan_valleyhouse"   -> R.drawable.kaghan_valleyhouse
        "house_kaghanvalley"   -> R.drawable.kaghan_valleyhouse // ✅ old name variant
        "swat_vila"            -> R.drawable.swat_vila
        "swat_villa"           -> R.drawable.swat_vila          // ✅ spelling variant
        "farmhouse_hunza"      -> R.drawable.farmhouse_hunza
        "hunza_farmhouse"      -> R.drawable.farmhouse_hunza    // ✅ old name variant
        "skardu_house"         -> R.drawable.skardu_house
        "skardu"               -> R.drawable.skardu_house       // ✅ old name variant

        else -> R.drawable.havenhub
    }
}