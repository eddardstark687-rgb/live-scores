package com.pitchpulse.data.model

// Sample Data for UI Preview and Phase 1
object SampleData {
    val events = listOf(
        MatchEvent("Messi", 23, EventType.GOAL),
        MatchEvent("Ramos", 67, EventType.RED_CARD)
    )
    
    val matchLive = Match(
        id = 1,
        homeTeam = "Arsenal",
        homeTeamId = 42,
        homeTeamLogo = "",
        awayTeam = "Sporting CP",
        awayTeamId = 12,
        awayTeamLogo = "",
        homeScore = 2,
        awayScore = 1,
        time = "75'",
        isLive = true,
        competition = "UEFA Champions League",
        isFavoriteLeague = true
    )

    val matchFinished = Match(
        id = 2,
        homeTeam = "Real Madrid",
        homeTeamId = 541,
        homeTeamLogo = "",
        awayTeam = "Barcelona",
        awayTeamId = 529,
        awayTeamLogo = "",
        homeScore = 3,
        awayScore = 2,
        time = "FT",
        isLive = false,
        competition = "La Liga",
        isFavoriteLeague = true
    )
}
