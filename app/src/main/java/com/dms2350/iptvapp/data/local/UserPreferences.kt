package com.dms2350.iptvapp.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "iptv_user_prefs"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_CEDULA = "user_cedula"
        private const val KEY_REGISTRATION_COMPLETED = "registration_completed"
        private const val KEY_REGISTRATION_SKIPPED = "registration_skipped"
    }

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userCedula: String?
        get() = prefs.getString(KEY_USER_CEDULA, null)
        set(value) = prefs.edit().putString(KEY_USER_CEDULA, value).apply()

    var isRegistrationCompleted: Boolean
        get() = prefs.getBoolean(KEY_REGISTRATION_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_REGISTRATION_COMPLETED, value).apply()

    var isRegistrationSkipped: Boolean
        get() = prefs.getBoolean(KEY_REGISTRATION_SKIPPED, false)
        set(value) = prefs.edit().putBoolean(KEY_REGISTRATION_SKIPPED, value).apply()

    fun saveUserInfo(name: String, cedula: String) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_CEDULA, cedula)
            putBoolean(KEY_REGISTRATION_COMPLETED, true)
            putBoolean(KEY_REGISTRATION_SKIPPED, false)
            apply()
        }
    }

    fun skipRegistration() {
        prefs.edit().apply {
            putBoolean(KEY_REGISTRATION_SKIPPED, true)
            putBoolean(KEY_REGISTRATION_COMPLETED, true)
            apply()
        }
    }

    fun clearUserInfo() {
        prefs.edit().clear().apply()
    }

    fun hasCompletedRegistration(): Boolean {
        return isRegistrationCompleted
    }
}

