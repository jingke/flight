package com.flightbooking.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flightbooking.app.ui.screens.auth.LoginScreen
import com.flightbooking.app.ui.screens.auth.RegisterScreen
import com.flightbooking.app.ui.screens.bookings.BookingDetailScreen
import com.flightbooking.app.ui.screens.bookings.MyBookingsScreen
import com.flightbooking.app.ui.screens.complaints.ComplaintsScreen
import com.flightbooking.app.ui.screens.flights.FlightDetailScreen
import com.flightbooking.app.ui.screens.home.HomeScreen
import com.flightbooking.app.ui.screens.loyalty.LoyaltyScreen
import com.flightbooking.app.ui.screens.map.RouteMapScreen
import com.flightbooking.app.ui.screens.notifications.NotificationsScreen
import com.flightbooking.app.ui.screens.search.SearchResultsScreen
import com.flightbooking.app.ui.screens.search.SearchScreen

@Composable
fun FlightBookingNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegistrationSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToBookings = { navController.navigate(Screen.MyBookings.route) },
                onNavigateToComplaints = { navController.navigate(Screen.Complaints.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                onNavigateToLoyalty = { navController.navigate(Screen.Loyalty.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onSearch = { departure, arrival, date, passengers ->
                    navController.navigate(
                        Screen.SearchResults.createRoute(departure, arrival, date, passengers)
                    )
                }
            )
        }

        composable(
            route = Screen.SearchResults.route,
            arguments = listOf(
                navArgument("departure") { type = NavType.StringType },
                navArgument("arrival") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType },
                navArgument("passengers") { type = NavType.IntType }
            )
        ) {
            SearchResultsScreen(
                onNavigateBack = { navController.popBackStack() },
                onFlightSelected = { flightId ->
                    navController.navigate(Screen.FlightDetail.createRoute(flightId))
                }
            )
        }

        composable(
            route = Screen.FlightDetail.route,
            arguments = listOf(navArgument("flightId") { type = NavType.IntType })
        ) {
            FlightDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onBookingCreated = { bookingId ->
                    navController.navigate(Screen.BookingDetail.createRoute(bookingId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onViewRouteMap = { flightId ->
                    navController.navigate(Screen.RouteMap.createRoute(flightId))
                }
            )
        }

        composable(Screen.MyBookings.route) {
            MyBookingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onBookingSelected = { bookingId ->
                    navController.navigate(Screen.BookingDetail.createRoute(bookingId))
                }
            )
        }

        composable(
            route = Screen.BookingDetail.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.IntType })
        ) {
            BookingDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onViewRouteMap = { flightId ->
                    navController.navigate(Screen.RouteMap.createRoute(flightId))
                }
            )
        }

        composable(Screen.Complaints.route) {
            ComplaintsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Loyalty.route) {
            LoyaltyScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.RouteMap.route,
            arguments = listOf(navArgument("flightId") { type = NavType.IntType })
        ) {
            RouteMapScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
