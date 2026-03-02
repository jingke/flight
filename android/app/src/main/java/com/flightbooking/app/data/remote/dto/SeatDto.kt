package com.flightbooking.app.data.remote.dto

import com.flightbooking.app.domain.model.Seat
import com.flightbooking.app.domain.model.SeatClass
import com.google.gson.annotations.SerializedName

data class SeatResponse(
    val id: Int,
    @SerializedName("flight_id") val flightId: Int,
    val row: Int,
    val column: String,
    @SerializedName("seat_class") val seatClass: String,
    @SerializedName("is_available") val isAvailable: Boolean
) {
    fun toDomain(): Seat = Seat(
        id = id,
        flightId = flightId,
        row = row,
        column = column,
        seatClass = when (seatClass) {
            "business" -> SeatClass.BUSINESS
            "first" -> SeatClass.FIRST
            else -> SeatClass.ECONOMY
        },
        isAvailable = isAvailable
    )
}
