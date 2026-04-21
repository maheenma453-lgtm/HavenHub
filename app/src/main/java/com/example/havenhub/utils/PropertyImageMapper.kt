package com.example.havenhub.utils
import com.example.havenhub.R
fun getPropertyImage(propertyId: String): Int {
    return when (propertyId) {
        "prop_001" -> R.drawable.apartment_lahore
        "prop_002" -> R.drawable.house_karachi
        "prop_003" -> R.drawable.room_islamabad
        "prop_004" -> R.drawable.rawalpindi_apt
        "prop_005 " -> R.drawable.studio_faislabad
        "prop_006 " -> R.drawable.room_sialkot
        "prop_007" -> R.drawable.vila_murree
        "prop_008" -> R.drawable.farmhouse_naran
        "prop_009" -> R.drawable.kaghan_valleyhouse
        "prop_010 " -> R.drawable.swat_vila
        "prop_011" -> R.drawable.farmhouse_hunza
        "prop_012" -> R.drawable.skardu_house
        else -> R.drawable.havenhub
    }
}
