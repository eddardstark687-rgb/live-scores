package com.pitchpulse.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.pitchpulse.data.model.Lineup
import com.pitchpulse.data.model.LineupPlayer
import com.pitchpulse.data.model.StatItem

import kotlinx.serialization.json.JsonElement

@Serializable
data class FootballResponse<T>(
    val response: List<T> = emptyList(),
    val errors: JsonElement? = null,
    val results: Int? = null
)

@Serializable
data class TeamsResponseDto(
    val response: List<TeamWrapperDto>,
    val errors: JsonElement? = null
)

@Serializable
data class TeamWrapperDto(
    val team: TeamDto,
    val venue: VenueDto? = null
)

@Serializable
data class FixturesResponseDto(
    val response: List<FixtureWrapperDto>,
    val errors: JsonElement? = null
)

@Serializable
data class FixtureWrapperDto(
    val fixture: FixtureDto,
    val league: LeagueDto,
    val teams: TeamsDto,
    val goals: GoalsDto,
    val events: List<EventDto> = emptyList()
)

@Serializable
data class EventDto(
    val time: EventTimeDto,
    val team: TeamDto,
    val player: EventPlayerDto,
    val type: String,
    val detail: String? = null
)

@Serializable
data class EventTimeDto(
    val elapsed: Int,
    val extra: Int? = null
)

@Serializable
data class EventPlayerDto(
    val id: Int? = null,
    val name: String? = null
)

@Serializable
data class FixtureDto(
    val id: Int,
    val date: String,
    val status: StatusDto
)

@Serializable
data class StatusDto(
    @SerialName("short") val short: String,
    val elapsed: Int? = null
)

@Serializable
data class LeagueDto(
    val id: Int,
    val name: String,
    val country: String? = null,
    val logo: String? = null,
    val season: Int? = null
)

@Serializable
data class TeamsDto(
    val home: TeamDto,
    val away: TeamDto
)

@Serializable
data class TeamDto(
    val id: Int,
    val name: String,
    val logo: String? = null,
    val country: String? = null,
    val winner: Boolean? = null,
    val founded: Int? = null,
    val national: Boolean = false
)

@Serializable
data class TeamFullDto(
    val id: Int,
    val name: String,
    val logo: String? = null,
    val country: String? = null,
    val founded: Int? = null,
    val national: Boolean = false
)

@Serializable
data class VenueDto(
    val id: Int? = null,
    val name: String? = null,
    val address: String? = null,
    val city: String? = null,
    val capacity: Int? = null,
    val surface: String? = null,
    val image: String? = null
)



@Serializable
data class GoalsDto(
    val home: Int? = null,
    val away: Int? = null
)

@Serializable
data class StandingResponse(
    val league: LeagueStandingsDto
)

@Serializable
data class LeagueStandingsDto(
    val id: Int,
    val name: String,
    val country: String,
    val logo: String,
    val standings: List<List<StandingDto>>
)

@Serializable
data class StandingDto(
    val rank: Int,
    val team: TeamDto,
    val points: Int,
    val goalsDiff: Int,
    val form: String? = null,
    val status: String? = null,
    val description: String? = null,
    val all: StatsDto
)

@Serializable
data class StatsDto(
    val played: Int,
    val win: Int,
    val draw: Int,
    val lose: Int
)

@Serializable
data class LineupDto(
    val team: TeamDto,
    val formation: String? = null,
    val startXI: List<LineupPlayerDto> = emptyList(),
    val substitutes: List<LineupPlayerDto> = emptyList(),
    val coach: CoachDto? = null
)

fun LineupDto.toDomain(): Lineup = Lineup(
    teamName = team.name,
    teamLogo = team.logo,
    formation = formation,
    startXI = startXI.map { it.toDomain() },
    substitutes = substitutes.map { it.toDomain() },
    coachName = coach?.name
)

@Serializable
data class LineupPlayerListDto(
    val player: LineupPlayerDto
)

@Serializable
data class LineupPlayerDto(
    val id: Int,
    val name: String,
    val number: Int? = null,
    val pos: String? = null,
    val grid: String? = null
)

fun LineupPlayerDto.toDomain(): LineupPlayer = LineupPlayer(
    id = id,
    name = name,
    number = number,
    position = pos,
    grid = grid
)

@Serializable
data class CoachDto(
    val id: Int? = null,
    val name: String? = null,
    val photo: String? = null
)

@Serializable
data class StatisticDto(
    val team: TeamDto,
    val statistics: List<StatItemDto> = emptyList()
)

fun StatisticDto.toDomainItems(): List<StatItem> = statistics.map { 
    StatItem(it.type, it.value?.toString()?.replace("\"", "") ?: "0") 
}

@Serializable
data class StatItemDto(
    val type: String,
    val value: kotlinx.serialization.json.JsonElement? = null
)
