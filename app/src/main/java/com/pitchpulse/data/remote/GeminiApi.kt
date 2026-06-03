package com.pitchpulse.data.remote

import com.pitchpulse.data.remote.dto.GeminiGenerateRequest
import com.pitchpulse.data.remote.dto.GeminiGenerateResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApi {
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}
