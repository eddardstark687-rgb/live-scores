package com.pitchpulse.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MatchStatistics(
    val homeStats: List<StatItem>,
    val awayStats: List<StatItem>
)

@Serializable
data class StatItem(
    val type: String,
    val value: String
)
