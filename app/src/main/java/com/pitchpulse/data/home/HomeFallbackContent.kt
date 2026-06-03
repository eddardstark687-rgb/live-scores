package com.pitchpulse.data.home

import com.pitchpulse.data.model.FootballQuote
import com.pitchpulse.data.model.QuizQuestion
import kotlin.math.abs

object HomeFallbackContent {
    val defaultQuote = FootballQuote(
        text = "Football is the ballet of the masses.",
        author = "Dmitri Shostakovich"
    )

    val defaultFact =
        "Pelé is the only player to have won three FIFA World Cups (1958, 1962, 1970)."

    private val quotePool = listOf(
        defaultQuote,
        FootballQuote(
            text = "Some people think football is a matter of life and death. I assure you, it's much more serious than that.",
            author = "Bill Shankly"
        ),
        FootballQuote(
            text = "I learned all about life with a ball at my feet.",
            author = "Ronaldinho"
        ),
        FootballQuote(
            text = "The ball is round, the game lasts ninety minutes, and everything else is pure theory.",
            author = "Sepp Herberger"
        ),
        FootballQuote(
            text = "Football is simple, but it is hard to play simple.",
            author = "Johan Cruyff"
        ),
        FootballQuote(
            text = "Talent without working hard is nothing.",
            author = "Cristiano Ronaldo"
        ),
        FootballQuote(
            text = "You have to fight to reach your dream. You have to sacrifice and work hard for it.",
            author = "Lionel Messi"
        )
    )

    private val factPool = listOf(
        defaultFact,
        "The fastest red card in professional football was after 2 seconds (Lee Todd, 2000).",
        "Cristiano Ronaldo has scored in five different World Cups — a men's record.",
        "The original World Cup trophy was called the Jules Rimet Trophy.",
        "AC Milan and Inter Milan share the same stadium: San Siro / Giuseppe Meazza.",
        "The offside rule was introduced in 1863, though it has changed many times since.",
        "Lionel Messi has won more Ballon d'Or awards than any other player."
    )

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
        ),
        QuizQuestion(
            question = "Which nation hosted the 2010 FIFA World Cup?",
            options = listOf("Brazil", "Germany", "South Africa", "Russia"),
            correctIndex = 2
        ),
        QuizQuestion(
            question = "Who scored the 'Hand of God' goal in 1986?",
            options = listOf("Diego Maradona", "Pelé", "Gabriel Batistuta", "Hernán Crespo"),
            correctIndex = 0
        ),
        QuizQuestion(
            question = "How many players are on the pitch per team during normal play?",
            options = listOf("9", "10", "11", "12"),
            correctIndex = 2
        )
    )

    fun bundleForDate(date: String): FallbackBundle {
        val seed = abs(date.hashCode())
        val quote = quotePool[seed % quotePool.size]
        val fact = factPool[seed % factPool.size]
        val quizzes = (0 until quizPool.size)
            .map { offset -> quizPool[(seed + offset) % quizPool.size] }
            .distinctBy { it.question }
            .take(3)
            .ifEmpty { quizPool.take(3) }
        return FallbackBundle(quizzes, quote, fact)
    }

    data class FallbackBundle(
        val quizzes: List<QuizQuestion>,
        val quote: FootballQuote,
        val fact: String
    )
}
