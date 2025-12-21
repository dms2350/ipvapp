package com.dms2350.iptvapp.utils

object Constants {
    const val BASE_URL = "https://playtv-production.up.railway.app/"
    const val TIMEOUT_SECONDS = 30L
    
    // Player constants
    const val PLAYER_SEEK_BACK_INCREMENT = 10000L
    const val PLAYER_SEEK_FORWARD_INCREMENT = 10000L
    
    // Preferences keys
    const val PREF_USER_TOKEN = "user_token"
    const val PREF_USER_ID = "user_id"
    const val PREF_LAST_CHANNEL = "last_channel"
}
