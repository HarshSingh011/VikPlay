package com.example.vidplay.util

object Constants {

    /** Base URL for all VidPlay API calls. Trailing slash is required by Retrofit. */
    const val BASE_URL = "https://api.vidplay.com/v1/"

    /** SharedPreferences file name */
    const val PREFS_NAME = "vidplay_prefs"

    /** Key used to persist the bearer token */
    const val KEY_TOKEN = "saved_token"
}
