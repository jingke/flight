package com.flightbooking.app

import com.flightbooking.app.domain.model.Airport
import com.flightbooking.app.domain.model.Booking
import com.flightbooking.app.domain.model.BookingStatus
import com.flightbooking.app.domain.model.Complaint
import com.flightbooking.app.domain.model.ComplaintStatus
import com.flightbooking.app.domain.model.Flight
import com.flightbooking.app.domain.model.FlightStatus
import com.flightbooking.app.domain.model.LoyaltyPoints
import com.flightbooking.app.domain.model.ModificationRequest
import com.flightbooking.app.domain.model.ModificationStatus
import com.flightbooking.app.domain.model.ModificationType
import com.flightbooking.app.domain.model.Notification
import com.flightbooking.app.domain.model.Passenger
import com.flightbooking.app.domain.model.PaymentStatus
import com.flightbooking.app.domain.model.Seat
import com.flightbooking.app.domain.model.SeatClass
import com.flightbooking.app.domain.model.User
import com.flightbooking.app.domain.model.UserRole

object TestFixtures {

    fun createAirport(
        id: Int = 1,
        code: String = "JFK",
        name: String = "John F. Kennedy International",
        city: String = "New York",
        country: String = "USA",
        latitude: Double = 40.6413,
        longitude: Double = -73.7781
    ): Airport = Airport(id, code, name, city, country, latitude, longitude)

    fun createFlight(
        id: Int = 1,
        flightNumber: String = "FL100",
        departureAirport: Airport = createAirport(),
        arrivalAirport: Airport = createAirport(
            id = 2, code = "LAX", name = "Los Angeles International",
            city = "Los Angeles", latitude = 33.9425, longitude = -118.4081
        ),
        departureTime: String = "2026-03-15T10:00:00",
        arrivalTime: String = "2026-03-15T13:00:00",
        price: Double = 299.99,
        totalSeats: Int = 180,
        status: FlightStatus = FlightStatus.SCHEDULED
    ): Flight = Flight(
        id, flightNumber, departureAirport, arrivalAirport,
        departureTime, arrivalTime, price, totalSeats, status
    )

    fun createBooking(
        id: Int = 1,
        userId: Int = 1,
        flight: Flight = createFlight(),
        status: BookingStatus = BookingStatus.CONFIRMED,
        totalPrice: Double = 299.99,
        paymentStatus: PaymentStatus = PaymentStatus.PAID,
        passengers: List<Passenger> = listOf(createPassenger()),
        createdAt: String = "2026-03-10T08:00:00"
    ): Booking = Booking(id, userId, flight, status, totalPrice, paymentStatus, passengers, createdAt)

    fun createPassenger(
        id: Int = 1,
        bookingId: Int = 1,
        name: String = "John Doe",
        email: String = "john@example.com",
        seatAssignment: String? = "12A"
    ): Passenger = Passenger(id, bookingId, name, email, seatAssignment)

    fun createSeat(
        id: Int = 1,
        flightId: Int = 1,
        row: Int = 1,
        column: String = "A",
        seatClass: SeatClass = SeatClass.ECONOMY,
        isAvailable: Boolean = true
    ): Seat = Seat(id, flightId, row, column, seatClass, isAvailable)

    fun createUser(
        id: Int = 1,
        email: String = "test@example.com",
        name: String = "Test User",
        role: UserRole = UserRole.CUSTOMER,
        createdAt: String = "2026-01-01T00:00:00"
    ): User = User(id, email, name, role, createdAt)

    fun createComplaint(
        id: Int = 1,
        userId: Int = 1,
        bookingId: Int? = 1,
        subject: String = "Delayed flight",
        description: String = "My flight was delayed by 3 hours",
        status: ComplaintStatus = ComplaintStatus.OPEN,
        adminResponse: String? = null,
        createdAt: String = "2026-03-10T10:00:00"
    ): Complaint = Complaint(id, userId, bookingId, subject, description, status, adminResponse, createdAt)

    fun createLoyaltyPoints(
        id: Int = 1,
        userId: Int = 1,
        earned: Int = 500,
        redeemed: Int = 100,
        balance: Int = 400
    ): LoyaltyPoints = LoyaltyPoints(id, userId, earned, redeemed, balance)

    fun createNotification(
        id: Int = 1,
        userId: Int = 1,
        title: String = "Booking Confirmed",
        message: String = "Your booking has been confirmed",
        isRead: Boolean = false,
        createdAt: String = "2026-03-10T10:00:00"
    ): Notification = Notification(id, userId, title, message, isRead, createdAt)

    fun createModificationRequest(
        id: Int = 1,
        userId: Int = 1,
        bookingId: Int = 1,
        type: ModificationType = ModificationType.DATE_CHANGE,
        details: String = "Change to March 20",
        status: ModificationStatus = ModificationStatus.PENDING,
        createdAt: String = "2026-03-10T10:00:00"
    ): ModificationRequest = ModificationRequest(id, userId, bookingId, type, details, status, createdAt)
}
