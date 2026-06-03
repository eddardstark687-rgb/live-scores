package com.pitchpulse.data.home

import com.pitchpulse.data.model.FootballQuote
import com.pitchpulse.data.model.QuizQuestion

object HomeFallbackContent {
    val defaultQuote = FootballQuote(
        text = "Football is the ballet of the masses.",
        author = "Dmitri Shostakovich"
    )

    val defaultFact =
        "Pelé is the only player to have won three FIFA World Cups (1958, 1962, 1970)."

    val quizPool = listOf(
        QuizQuestion(
            question = "Which country has won the most FIFA World Cups?",
            options = listOf("Germany", "Italy", "Brazil", "Argentina"),
            correctIndex = 2
        ),
        QuizQuestion(
            question = "Who holds the record for most goals in a single Premier League season (38 games)?",
            options = listOf("Mohamed Salah", "Erling Haaland", "Alan Shearer", "Thierry Henry"),
            correctIndex = 1
        ),
        QuizQuestion(
            question = "In which year was the first UEFA Champions League final held (as the rebranded competition)?",
            options = listOf("1990", "1992", "1994", "1996"),
            correctIndex = 1
        ),
        QuizQuestion(
            question = "Which club is nicknamed 'The Old Lady' (La Vecchia Signora)?",
            options = listOf("AC Milan", "Inter Milan", "Juventus", "Roma"),
            correctIndex = 2
        )
    )
}
