package com.flightbooking.app.data.remote.dto

import com.flightbooking.app.domain.model.Airport
import com.flightbooking.app.domain.model.Booking
import com.flightbooking.app.domain.model.BookingStatus
import com.flightbooking.app.domain.model.Flight
import com.flightbooking.app.domain.model.FlightStatus
import com.flightbooking.app.domain.model.Passenger
import com.flightbooking.app.domain.model.PaymentStatus
import com.google.gson.annotations.SerializedName

data class BookingResponse(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("flight_id") val flightId: Int,
    val status: String,
    @SerializedName("total_price") val totalPrice: Double,
    @SerializedName("payment_status") val paymentStatus: String,
    val passengers: List<PassengerResponse> = emptyList(),
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("flight_number") val flightNumber: String? = null,
    @SerializedName("departure_airport") val departureAirport: String? = null,
    @SerializedName("arrival_airport") val arrivalAirport: String? = null,
    @SerializedName("departure_time") val departureTime: String? = null,
    @SerializedName("arrival_time") val arrivalTime: String? = null
) {
    fun toDomain(): Booking = Booking(
        id = id,
        userId = userId,
        flight = Flight(
            id = flightId,
            flightNumber = flightNumber ?: "N/A",
            departureAirport = parseAirportString(departureAirport),
            arrivalAirport = parseAirportString(arrivalAirport),
            departureTime = departureTime ?: "",
            arrivalTime = arrivalTime ?: "",
            price = totalPrice / maxOf(passengers.size, 1),
            totalSeats = 0,
            status = FlightStatus.SCHEDULED
        ),
        status = when (status) {
            "cancelled" -> BookingStatus.CANCELLED
            "pending" -> BookingStatus.PENDING
            else -> BookingStatus.CONFIRMED
        },
        totalPrice = totalPrice,
        paymentStatus = when (paymentStatus) {
            "pending" -> PaymentStatus.PENDING
            "refunded" -> PaymentStatus.REFUNDED
            else -> PaymentStatus.PAID
        },
        passengers = passengers.map { it.toDomain() },
        createdAt = createdAt
    )
}

private fun parseAirportString(airport: String?): Airport {
    if (airport == null) return Airport(
        id = 0, code = "???", name = "Unknown",
        city = "Unknown", country = "", latitude = 0.0, longitude = 0.0
    )
    val parts = airport.split(" - ", limit = 2)
    val code = parts.getOrElse(0) { "???" }.trim()
    val city = parts.getOrElse(1) { "Unknown" }.trim()
    return Airport(
        id = 0, code = code, name = code,
        city = city, country = "", latitude = 0.0, longitude = 0.0
    )
}

data class PassengerResponse(
    val id: Int,
    @SerializedName("booking_id") val bookingId: Int = 0,
    val name: String,
    val email: String,
    @SerializedName("seat_assignment") val seatAssignment: String?
) {
    fun toDomain(): Passenger = Passenger(
        id = id,
        bookingId = bookingId,
        name = name,
        email = email,
        seatAssignment = seatAssignment
    )
}

data class CreateBookingRequest(
    @SerializedName("flight_id") val flightId: Int,
    val passengers: List<PassengerRequest>
)

data class PassengerRequest(
    val name: String,
    val email: String,
    @SerializedName("seat_id") val seatId: Int?
)
