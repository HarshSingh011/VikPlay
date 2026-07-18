package com.example.vidplay.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    private val tokenValidityMillis = 30L * 24 * 60 * 60 * 1000

    var token: String
        get() {
            val savedAt = prefs.getLong(Constants.KEY_TOKEN_SAVED_AT, 0L)
            val storedToken = prefs.getString(Constants.KEY_TOKEN, "") ?: ""

            if (storedToken.isBlank()) {
                clearSession()
                return ""
            }

            if (savedAt == 0L || System.currentTimeMillis() - savedAt > tokenValidityMillis) {
                clearSession()
                return ""
            }

            return storedToken
        }
        set(value) {
            if (value.isBlank()) {
                clearSession()
                return
            }

            prefs.edit()
                .putString(Constants.KEY_TOKEN, value)
                .putLong(Constants.KEY_TOKEN_SAVED_AT, System.currentTimeMillis())
                .apply()
        }

    var username: String
        get() = prefs.getString(Constants.KEY_USERNAME, "Viewer") ?: "Viewer"
        set(value) = prefs.edit().putString(Constants.KEY_USERNAME, value).apply()

    fun hasValidToken(): Boolean = token.isNotEmpty()

    fun clearToken() {
        clearSession()
    }

    fun clearSession() {
        prefs.edit()
            .remove(Constants.KEY_TOKEN)
            .remove(Constants.KEY_TOKEN_SAVED_AT)
            .remove(Constants.KEY_USERNAME)
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
