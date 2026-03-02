package com.flightbooking.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flightbooking.app.domain.model.Airport

@Entity(tableName = "airports")
data class AirportEntity(
    @PrimaryKey val id: Int,
    val code: String,
    val name: String,
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
) {
    fun toDomain(): Airport = Airport(
        id = id,
        code = code,
        name = name,
        city = city,
        country = country,
        latitude = latitude,
        longitude = longitude
    )

    companion object {
        fun fromDomain(airport: Airport): AirportEntity = AirportEntity(
            id = airport.id,
            code = airport.code,
            name = airport.name,
            city = airport.city,
            country = airport.country,
            latitude = airport.latitude,
            longitude = airport.longitude
        )
    }
}
