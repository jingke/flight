package com.flightbooking.app.ui.navigation

sealed class Screen(val route: String) {

    data object Login : Screen("login")
    data object Register : Screen("register")

    data object Home : Screen("home")
    data object Search : Screen("search")
    data object SearchResults : Screen("search_results/{departure}/{arrival}/{date}/{passengers}") {
        fun createRoute(
            departure: String,
            arrival: String,
            date: String,
            passengers: Int
        ): String = "search_results/$departure/$arrival/$date/$passengers"
    }

    data object FlightDetail : Screen("flight/{flightId}") {
        fun createRoute(flightId: Int): String = "flight/$flightId"
    }

    data object SeatSelection : Screen("seat_selection/{flightId}") {
        fun createRoute(flightId: Int): String = "seat_selection/$flightId"
    }

    data object MyBookings : Screen("my_bookings")
    data object BookingDetail : Screen("booking/{bookingId}") {
        fun createRoute(bookingId: Int): String = "booking/$bookingId"
    }

    data object Complaints : Screen("complaints")
    data object Notifications : Screen("notifications")
    data object Loyalty : Screen("loyalty")
    data object RouteMap : Screen("route_map/{flightId}") {
        fun createRoute(flightId: Int): String = "route_map/$flightId"
    }
}
