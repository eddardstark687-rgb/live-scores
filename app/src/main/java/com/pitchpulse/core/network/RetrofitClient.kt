package com.pitchpulse.core.network

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pitchpulse.data.remote.FootballApi
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

import com.pitchpulse.BuildConfig

object RetrofitClient {
    private const val BASE_URL = "https://v3.football.api-sports.io/"
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val API_KEYS = listOf(
        BuildConfig.API_KEY_PRIMARY,
        BuildConfig.API_KEY_BACKUP_1,
        BuildConfig.API_KEY_BACKUP_2
    ).filter { it.isNotEmpty() }

    private var currentKeyIndex = 0

    fun getActiveKey(): String = API_KEYS[currentKeyIndex]
    
    fun getActiveKeyIndex(): Int = currentKeyIndex

    fun rotateKey() {
        if (currentKeyIndex < API_KEYS.size - 1) {
            currentKeyIndex++
            android.util.Log.w("RetrofitClient", "ROTATING API KEY to index $currentKeyIndex")
        } else {
            android.util.Log.e("RetrofitClient", "ALL API KEYS EXHAUSTED")
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("x-apisports-key", getActiveKey())
            .build()
        
        val response = chain.proceed(request)
        
        // Handle 429 or custom API-Sports quota error in response body
        // Note: Reading body here might require careful buffering if we want to check JSON.
        // For now, we'll rely on the Repository's checkQuota and explicit rotation triggers.
        
        response
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Use BASIC or NONE in production
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api: FootballApi = retrofit.create(FootballApi::class.java)
}
