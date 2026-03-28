package com.example.vidplay.util

object Constants {

    /** Base URL for all VidPlay API calls. Trailing slash is required by Retrofit. */
    const val BASE_URL = "https://vikplay-backend.onrender.com/"

    /** SharedPreferences file name */
    const val PREFS_NAME = "vidplay_prefs"

    /** Key used to persist the bearer token */
    const val KEY_TOKEN = "saved_token"

    /** Key used to persist the username */
    const val KEY_USERNAME = "saved_username"
}
