package com.flightbooking.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "flight_number") val flightNumber: String,
    @ColumnInfo(name = "departure_airport_code") val departureAirportCode: String,
    @ColumnInfo(name = "departure_airport_name") val departureAirportName: String,
    @ColumnInfo(name = "departure_city") val departureCity: String,
    @ColumnInfo(name = "arrival_airport_code") val arrivalAirportCode: String,
    @ColumnInfo(name = "arrival_airport_name") val arrivalAirportName: String,
    @ColumnInfo(name = "arrival_city") val arrivalCity: String,
    @ColumnInfo(name = "departure_time") val departureTime: String,
    @ColumnInfo(name = "arrival_time") val arrivalTime: String,
    val price: Double,
    @ColumnInfo(name = "total_seats") val totalSeats: Int,
    val status: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis()
)
