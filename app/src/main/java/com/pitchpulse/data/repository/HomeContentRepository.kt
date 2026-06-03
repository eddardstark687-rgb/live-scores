package com.pitchpulse.data.repository

import android.util.Log
import com.pitchpulse.core.network.GeminiClient
import com.pitchpulse.data.home.HomeFallbackContent
import com.pitchpulse.data.local.dao.FootballDao
import com.pitchpulse.data.local.entity.HomeDailyContentEntity
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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

    /**
     * Returns today's home content. Uses Room cache for the current date so quiz, quote,
     * and fact stay stable for the day and refresh automatically after midnight.
     */
    suspend fun getTodayHomeContent(): AiHomeBundle = withContext(Dispatchers.IO) {
        val today = todayString()
        loadCachedBundle(today)?.let { return@withContext it }

        val bundle = if (geminiApiKey.isBlank()) {
            Log.w(TAG, "GEMINI_API_KEY missing — using daily rotated fallback content")
            fallbackBundleForDate(today)
        } else {
            fetchFromGemini(today) ?: fallbackBundleForDate(today)
        }

        cacheBundle(today, bundle)
        bundle
    }

    private suspend fun loadCachedBundle(date: String): AiHomeBundle? {
        val entity = dao.getHomeDailyContent(date) ?: return null
        return runCatching {
            entity.contentJson.let { json.decodeFromString<CachedHomePayload>(it).toBundle() }
        }.onFailure {
            Log.w(TAG, "Failed to read cached home content for $date: ${it.message}")
        }.getOrNull()
    }

    private suspend fun cacheBundle(date: String, bundle: AiHomeBundle) {
        val payload = CachedHomePayload.fromBundle(bundle)
        dao.insertHomeDailyContent(
            HomeDailyContentEntity(
                dateString = date,
                contentJson = json.encodeToString(payload)
            )
        )
        dao.clearOldHomeDailyContent(date)
    }

    private suspend fun fetchFromGemini(today: String): AiHomeBundle? {
        try {
            val prompt = """
                You are a football expert. Today is $today.
                Respond with JSON only, no markdown, matching this schema:
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
                Make the content feel fresh and different from generic repeated trivia.
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
                ?: return null

            return parseGeminiPayload(rawText)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini fetch failed: ${e.message}", e)
            return null
        }
    }

    private fun parseGeminiPayload(rawText: String): AiHomeBundle? {
        val payload = json.decodeFromString<HomeAiPayload>(rawText.trim())
        val quizzes = payload.quizzes
            .mapNotNull { dto ->
                if (dto.options.size < 2) return@mapNotNull null
                val idx = dto.correctIndex.coerceIn(0, dto.options.lastIndex)
                QuizQuestion(dto.question, dto.options, idx)
            }
            .take(3)
        if (quizzes.isEmpty()) return null

        val quote = payload.quote?.let { FootballQuote(it.text, it.author) } ?: return null
        val fact = payload.fact?.takeIf { it.isNotBlank() } ?: return null

        return AiHomeBundle(
            quizzes = quizzes,
            quote = quote,
            fact = fact,
            fromNetwork = true
        )
    }

    private fun fallbackBundleForDate(date: String): AiHomeBundle {
        val fallback = HomeFallbackContent.bundleForDate(date)
        return AiHomeBundle(
            quizzes = fallback.quizzes,
            quote = fallback.quote,
            fact = fallback.fact,
            fromNetwork = false
        )
    }

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

@Serializable
private data class CachedHomePayload(
    val quizzes: List<CachedQuiz>,
    val quoteText: String,
    val quoteAuthor: String,
    val fact: String,
    val fromNetwork: Boolean
) {
    fun toBundle() = AiHomeBundle(
        quizzes = quizzes.map { QuizQuestion(it.question, it.options, it.correctIndex) },
        quote = FootballQuote(quoteText, quoteAuthor),
        fact = fact,
        fromNetwork = fromNetwork
    )

    companion object {
        fun fromBundle(bundle: AiHomeBundle) = CachedHomePayload(
            quizzes = bundle.quizzes.map {
                CachedQuiz(it.question, it.options, it.correctIndex)
            },
            quoteText = bundle.quote.text,
            quoteAuthor = bundle.quote.author,
            fact = bundle.fact,
            fromNetwork = bundle.fromNetwork
        )
    }
}

@Serializable
private data class CachedQuiz(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

data class AiHomeBundle(
    val quizzes: List<QuizQuestion>,
    val quote: FootballQuote,
    val fact: String,
    val fromNetwork: Boolean
)
