package com.example.havenhub.utils
import com.example.havenhub.R

fun getPropertyImage(propertyId: String): Int {
    return when (propertyId) {

        // ── Prop IDs (Firestore manual) ──────────────────────
        "prop_001" -> R.drawable.apartment_lahore
        "prop_002" -> R.drawable.house_karachi
        "prop_003" -> R.drawable.room_islamabad
        "prop_004" -> R.drawable.apartment_rawalpindi
        "prop_005" -> R.drawable.studio_faisalabad
        "prop_006" -> R.drawable.room_sialkot
        "prop_007" -> R.drawable.vila_murree
        "prop_008" -> R.drawable.naran_farmhouse
        "prop_009" -> R.drawable.house_kaghanvalley
        "prop_010" -> R.drawable.swat_villa
        "prop_011" -> R.drawable.hunza_farmhouse
        "prop_012" -> R.drawable.skardu

        // ── Drawable names (resolvedDrawableName se aata hai) ─
        "apartment_lahore"     -> R.drawable.apartment_lahore
        "house_karachi"        -> R.drawable.house_karachi
        "room_islamabad"       -> R.drawable.room_islamabad
        "apartment_rawalpindi" -> R.drawable.apartment_rawalpindi
        "studio_faisalabad"    -> R.drawable.studio_faisalabad
        "room_sialkot"         -> R.drawable.room_sialkot
        "vila_murree"          -> R.drawable.vila_murree
        "naran_farmhouse"      -> R.drawable.naran_farmhouse
        "house_kaghanvalley"   -> R.drawable.house_kaghanvalley
        "swat_villa"           -> R.drawable.swat_villa
        "hunza_farmhouse"      -> R.drawable.hunza_farmhouse
        "skardu"               -> R.drawable.skardu

        else -> R.drawable.havenhub
    }
}