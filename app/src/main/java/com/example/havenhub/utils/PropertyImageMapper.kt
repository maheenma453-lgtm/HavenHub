package com.example.havenhub.utils

import com.example.havenhub.R

fun getPropertyImage(propertyId: String): Int {
    return when (propertyId) {

        // ── Prop IDs (Firestore manual) ──────────────────────────────────────
        "prop_001" -> R.drawable.apartment_lahore
        "prop_002" -> R.drawable.house_karachi
        "prop_003" -> R.drawable.room_islamabad
        "prop_004" -> R.drawable.rawalpindi_apt       // ✅ actual file name
        "prop_005 " -> R.drawable.studio_faislabad     // ✅ actual file name
        "prop_006 " -> R.drawable.room_sialkot
        "prop_007" -> R.drawable.vila_murree
        "prop_008" -> R.drawable.farmhouse_naran      // ✅ actual file name
        "prop_009" -> R.drawable.kaghan_valleyhouse   // ✅ actual file name
        "prop_010 " -> R.drawable.swat_vila            // ✅ actual file name
        "prop_011" -> R.drawable.farmhouse_hunza      // ✅ actual file name
        "prop_012" -> R.drawable.skardu_house         // ✅ actual file name

        // ── Drawable names (resolvedDrawableName se aata hai) ────────────────
        "apartment_lahore"     -> R.drawable.apartment_lahore
        "house_karachi"        -> R.drawable.house_karachi
        "room_islamabad"       -> R.drawable.room_islamabad
        "apartment_rawalpindi" -> R.drawable.rawalpindi_apt       // ✅ fix
        "studio_faisalabad"    -> R.drawable.studio_faislabad     // ✅ fix
        "room_sialkot"         -> R.drawable.room_sialkot
        "vila_murree"          -> R.drawable.vila_murree
        "naran_farmhouse"      -> R.drawable.farmhouse_naran      // ✅ fix
        "house_kaghanvalley"   -> R.drawable.kaghan_valleyhouse   // ✅ fix
        "swat_villa"           -> R.drawable.swat_vila            // ✅ fix
        "hunza_farmhouse"      -> R.drawable.farmhouse_hunza      // ✅ fix
        "skardu"               -> R.drawable.skardu_house         // ✅ fix

        else                   -> R.drawable.havenhub
    }
}