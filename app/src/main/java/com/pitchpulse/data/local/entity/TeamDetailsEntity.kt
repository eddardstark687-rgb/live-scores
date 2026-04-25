package com.pitchpulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pitchpulse.data.model.TeamDetails

@Entity(tableName = "team_details")
data class TeamDetailsEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val logo: String?,
    val country: String?,
    val founded: Int?,
    val isNational: Boolean,
    val venueName: String?,
    val venueCity: String?,
    val venueCapacity: Int?,
    val venueImage: String?
) {
    fun toDomainModel() = TeamDetails(
        id = id,
        name = name,
        logo = logo,
        country = country,
        founded = founded,
        isNational = isNational,
        venueName = venueName,
        venueCity = venueCity,
        venueCapacity = venueCapacity,
        venueImage = venueImage
    )

    companion object {
        fun fromDomain(domain: TeamDetails) = TeamDetailsEntity(
            id = domain.id,
            name = domain.name,
            logo = domain.logo,
            country = domain.country,
            founded = domain.founded,
            isNational = domain.isNational,
            venueName = domain.venueName,
            venueCity = domain.venueCity,
            venueCapacity = domain.venueCapacity,
            venueImage = domain.venueImage
        )
    }
}
