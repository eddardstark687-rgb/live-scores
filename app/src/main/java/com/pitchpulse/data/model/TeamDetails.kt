package com.pitchpulse.data.model

data class TeamDetails(
    val id: Int,
    val name: String,
    val logo: String?,
    val country: String?,
    val founded: Int?,
    val isNational: Boolean,
    val venueName: String?,
    val venueCity: String?,
    val venueCapacity: Int?,
    val venueImage: String?
)
