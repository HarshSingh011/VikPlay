package com.example.vidplay.data.source.remote

import com.example.vidplay.BuildConfig
import com.example.vidplay.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton that builds and exposes the Retrofit instance.
 * Swap `Constants.BASE_URL` to point at a different environment.
 */
object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
    }

    private val urlLoggingInterceptor = ApiUrlLoggingInterceptor()

    val okHttpBuilder = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)

    init {
        if (BuildConfig.DEBUG) {
            okHttpBuilder.addInterceptor(loggingInterceptor)
            okHttpBuilder.addInterceptor(urlLoggingInterceptor)
        }
    }

    private val okHttpClient = okHttpBuilder.build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val streamApiService: StreamApiService by lazy {
        retrofit.create(StreamApiService::class.java)
    }

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
}
