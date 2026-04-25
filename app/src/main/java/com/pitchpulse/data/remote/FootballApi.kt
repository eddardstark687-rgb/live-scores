package com.pitchpulse.data.remote

import com.pitchpulse.data.remote.dto.FixturesResponseDto
import com.pitchpulse.data.remote.dto.FootballResponse
import com.pitchpulse.data.remote.dto.LineupDto
import com.pitchpulse.data.remote.dto.StandingResponse
import com.pitchpulse.data.remote.dto.StatisticDto
import com.pitchpulse.data.remote.dto.TeamsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface FootballApi {
    @GET("fixtures")
    suspend fun getFixtures(
        @Query("date") date: String,
        @Query("league") leagueId: Int? = null
    ): FixturesResponseDto

    @GET("standings")
    suspend fun getStandings(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): FootballResponse<StandingResponse>

    @GET("fixtures")
    suspend fun getFixtureDetails(
        @Query("id") id: Int
    ): FixturesResponseDto

    @GET("fixtures/lineups")
    suspend fun getLineups(
        @Query("fixture") fixtureId: Int
    ): FootballResponse<LineupDto>

    @GET("fixtures/statistics")
    suspend fun getStatistics(
        @Query("fixture") fixtureId: Int
    ): FootballResponse<StatisticDto>

    @GET("teams")
    suspend fun getTeamInfo(
        @Query("id") id: Int
    ): TeamsResponseDto

    @GET("fixtures")
    suspend fun getTeamFixtures(
        @Query("team") teamId: Int,
        @Query("next") next: Int? = null,
        @Query("last") last: Int? = null,
        @Query("season") season: Int? = null
    ): FixturesResponseDto

    @GET("teams")
    suspend fun searchTeams(
        @Query("search") query: String
    ): TeamsResponseDto
}
