package com.pitchpulse.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiGenerationConfig(
    @SerialName("responseMimeType") val responseMimeType: String = "application/json"
)

@Serializable
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null
)

@Serializable
data class HomeAiPayload(
    val quizzes: List<QuizQuestionDto> = emptyList(),
    val quote: QuoteDto? = null,
    val fact: String? = null
)

@Serializable
data class QuizQuestionDto(
    val question: String,
    val options: List<String>,
    @SerialName("correctIndex") val correctIndex: Int
)

@Serializable
data class QuoteDto(
    val text: String,
    val author: String
)
