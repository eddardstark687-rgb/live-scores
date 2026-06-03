package com.pitchpulse.data.repository

import android.util.Log
import com.pitchpulse.core.network.GeminiClient
import com.pitchpulse.data.home.HomeFallbackContent
import com.pitchpulse.data.local.dao.FootballDao
import com.pitchpulse.data.model.FootballQuote
import com.pitchpulse.data.model.LeagueTodaySummary
import com.pitchpulse.data.model.Match
import com.pitchpulse.data.model.QuizQuestion
import com.pitchpulse.data.remote.GeminiApi
import com.pitchpulse.data.remote.dto.GeminiContent
import com.pitchpulse.data.remote.dto.GeminiGenerateRequest
import com.pitchpulse.data.remote.dto.GeminiGenerationConfig
import com.pitchpulse.data.remote.dto.GeminiPart
import com.pitchpulse.data.remote.dto.HomeAiPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "HomeContentRepository"

class HomeContentRepository(
    private val dao: FootballDao,
    private val geminiApi: GeminiApi = GeminiClient.api,
    private val geminiApiKey: String = GeminiClient.apiKey()
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val trackedLeagues = listOf(
        LeagueMatcher("Premier League", listOf("premier league")),
        LeagueMatcher("La Liga", listOf("la liga", "laliga")),
        LeagueMatcher("Bundesliga", listOf("bundesliga"))
    )

    fun observeLeagueSummariesToday(): Flow<List<LeagueTodaySummary>> {
        val today = todayString()
        return dao.getDailyMatchesFlow(today).map { entities ->
            val matches = entities.map { it.toDomainModel() }
            buildLeagueSummaries(matches)
        }
    }

    suspend fun fetchAiHomeContent(): AiHomeBundle = withContext(Dispatchers.IO) {
        if (geminiApiKey.isBlank()) {
            Log.w(TAG, "GEMINI_API_KEY missing — using bundled home content")
            return@withContext fallbackBundle()
        }
        try {
            val prompt = """
                You are a football expert. Respond with JSON only, no markdown, matching this schema:
                {
                  "quizzes": [
                    {"question": "string", "options": ["A","B","C","D"], "correctIndex": 0}
                  ],
                  "quote": {"text": "string", "author": "string"},
                  "fact": "string"
                }
                Provide exactly 3 varied football trivia quiz questions (4 options each, correctIndex 0-3),
                one short inspirational football quote with a real or attributed author,
                and one surprising football fact under 120 characters.
            """.trimIndent()

            val response = geminiApi.generateContent(
                apiKey = geminiApiKey,
                request = GeminiGenerateRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
                    generationConfig = GeminiGenerationConfig()
                )
            )

            val rawText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?: return@withContext fallbackBundle()

            val payload = json.decodeFromString<HomeAiPayload>(rawText.trim())
            val quizzes = payload.quizzes
                .mapNotNull { dto ->
                    if (dto.options.size < 2) return@mapNotNull null
                    val idx = dto.correctIndex.coerceIn(0, dto.options.lastIndex)
                    QuizQuestion(dto.question, dto.options, idx)
                }
                .ifEmpty { HomeFallbackContent.quizPool }

            AiHomeBundle(
                quizzes = quizzes,
                quote = payload.quote?.let { FootballQuote(it.text, it.author) }
                    ?: HomeFallbackContent.defaultQuote,
                fact = payload.fact?.takeIf { it.isNotBlank() }
                    ?: HomeFallbackContent.defaultFact,
                fromNetwork = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gemini fetch failed: ${e.message}", e)
            fallbackBundle()
        }
    }

    private fun fallbackBundle() = AiHomeBundle(
        quizzes = HomeFallbackContent.quizPool,
        quote = HomeFallbackContent.defaultQuote,
        fact = HomeFallbackContent.defaultFact,
        fromNetwork = false
    )

    fun buildLeagueSummaries(matches: List<Match>): List<LeagueTodaySummary> =
        trackedLeagues.map { league ->
            val count = matches.count { match ->
                league.patterns.any { pattern ->
                    match.competition.contains(pattern, ignoreCase = true)
                }
            }
            LeagueTodaySummary(league.displayName, count)
        }

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private data class LeagueMatcher(val displayName: String, val patterns: List<String>)
}

data class AiHomeBundle(
    val quizzes: List<QuizQuestion>,
    val quote: FootballQuote,
    val fact: String,
    val fromNetwork: Boolean
)
