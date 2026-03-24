package com.example.vidplay.data.source.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that logs the full API URL being called.
 * Useful for debugging API requests.
 */
class ApiUrlLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val method = request.method

        Log.i("API_CALL", "$method $url")

        return chain.proceed(request)
    }
}
