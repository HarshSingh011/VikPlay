package com.example.vidplay.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Thin wrapper around SharedPreferences so every layer can read/write
 * app preferences without importing Android-framework classes directly.
 */
class PreferenceHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    var token: String
        get() = prefs.getString(Constants.KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_TOKEN, value).apply()

    var username: String
        get() = prefs.getString(Constants.KEY_USERNAME, "Viewer") ?: "Viewer"
        set(value) = prefs.edit().putString(Constants.KEY_USERNAME, value).apply()

    fun clearToken() {
        prefs.edit().remove(Constants.KEY_TOKEN).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
