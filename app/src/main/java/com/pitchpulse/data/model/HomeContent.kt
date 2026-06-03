package com.pitchpulse.data.model

data class LeagueTodaySummary(
    val name: String,
    val matchCount: Int
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

data class FootballQuote(
    val text: String,
    val author: String
)
