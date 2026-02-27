package com.flightbooking.app.ui.screens.flights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flightbooking.app.domain.model.Flight
import com.flightbooking.app.domain.model.Seat
import com.flightbooking.app.domain.model.SeatClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailScreen(
    onNavigateBack: () -> Unit,
    onBookingCreated: (Int) -> Unit,
    onViewRouteMap: (Int) -> Unit = {},
    viewModel: FlightDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.bookingCreatedId) {
        uiState.bookingCreatedId?.let { onBookingCreated(it) }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(FlightDetailEvent.ClearError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flight Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.flight != null) {
                        IconButton(onClick = { onViewRouteMap(uiState.flight!!.id) }) {
                            Icon(Icons.Default.Map, contentDescription = "Route Map")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.flight == null -> {
                    Text(
                        text = "Flight not found",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    FlightDetailContent(
                        flight = uiState.flight!!,
                        seats = uiState.seats,
                        passengers = uiState.passengers,
                        activePassengerIndex = uiState.activePassengerIndex,
                        isBooking = uiState.isBooking,
                        onEvent = viewModel::onEvent
                    )
                }
            }
        }
    }
}

@Composable
private fun FlightDetailContent(
    flight: Flight,
    seats: List<Seat>,
    passengers: List<PassengerForm>,
    activePassengerIndex: Int,
    isBooking: Boolean,
    onEvent: (FlightDetailEvent) -> Unit
) {
    val selectedSeatIds = passengers.mapNotNull { it.selectedSeatId }.toSet()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        FlightInfoCard(flight = flight)
        Spacer(modifier = Modifier.height(16.dp))
        if (seats.isNotEmpty()) {
            SeatSelectionSection(
                seats = seats,
                selectedSeatIds = selectedSeatIds,
                activePassengerSeatId = passengers.getOrNull(activePassengerIndex)?.selectedSeatId,
                onSeatToggle = { seatId -> onEvent(FlightDetailEvent.ToggleSeat(seatId)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        PassengersSection(
            passengers = passengers,
            activePassengerIndex = activePassengerIndex,
            seats = seats,
            onEvent = onEvent
        )
        Spacer(modifier = Modifier.height(16.dp))
        PriceSummary(flight = flight, passengerCount = passengers.size)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onEvent(FlightDetailEvent.CreateBooking) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isBooking
        ) {
            if (isBooking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isBooking) "Booking..." else "Book Flight")
        }
    }
}

@Composable
private fun FlightInfoCard(flight: Flight) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = flight.flightNumber,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = flight.status.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = flight.departureAirport.code,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = flight.departureAirport.city,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDateTime(flight.departureTime),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(
                    imageVector = Icons.Default.AirplanemodeActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = flight.arrivalAirport.code,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = flight.arrivalAirport.city,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDateTime(flight.arrivalTime),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Price per person",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$${String.format("%.2f", flight.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SeatSelectionSection(
    seats: List<Seat>,
    selectedSeatIds: Set<Int>,
    activePassengerSeatId: Int?,
    onSeatToggle: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Select Seats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            SeatLegend()
            Spacer(modifier = Modifier.height(12.dp))
            SeatGrid(
                seats = seats,
                selectedSeatIds = selectedSeatIds,
                activePassengerSeatId = activePassengerSeatId,
                onSeatToggle = onSeatToggle
            )
        }
    }
}

@Composable
private fun SeatLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        LegendItem(color = Color(0xFF4CAF50), label = "Available")
        LegendItem(color = MaterialTheme.colorScheme.primary, label = "Selected")
        LegendItem(color = Color(0xFFBDBDBD), label = "Occupied")
        LegendItem(color = Color(0xFFFFD700), label = "Business")
        LegendItem(color = Color(0xFFE040FB), label = "First")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SeatGrid(
    seats: List<Seat>,
    selectedSeatIds: Set<Int>,
    activePassengerSeatId: Int?,
    onSeatToggle: (Int) -> Unit
) {
    val seatsByRow = seats.groupBy { it.row }.toSortedMap()
    val columns = seats.map { it.column }.distinct().sorted()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.width(28.dp))
            columns.forEach { col ->
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = col,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        seatsByRow.forEach { (row, rowSeats) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$row",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                columns.forEach { col ->
                    val seat = rowSeats.find { it.column == col }
                    if (seat != null) {
                        SeatItem(
                            seat = seat,
                            isSelected = seat.id in selectedSeatIds,
                            isActiveSelection = seat.id == activePassengerSeatId,
                            onToggle = { onSeatToggle(seat.id) }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SeatItem(
    seat: Seat,
    isSelected: Boolean,
    isActiveSelection: Boolean,
    onToggle: () -> Unit
) {
    val backgroundColor = when {
        !seat.isAvailable -> Color(0xFFBDBDBD)
        isActiveSelection -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        seat.seatClass == SeatClass.FIRST -> Color(0xFFE040FB).copy(alpha = 0.3f)
        seat.seatClass == SeatClass.BUSINESS -> Color(0xFFFFD700).copy(alpha = 0.3f)
        else -> Color(0xFF4CAF50).copy(alpha = 0.3f)
    }
    val textColor = when {
        isSelected || isActiveSelection -> Color.White
        !seat.isAvailable -> Color.Gray
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .then(
                if (isActiveSelection) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(6.dp)
                ) else Modifier
            )
            .clickable(enabled = seat.isAvailable) { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${seat.row}${seat.column}",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = textColor
        )
    }
}

@Composable
private fun PassengersSection(
    passengers: List<PassengerForm>,
    activePassengerIndex: Int,
    seats: List<Seat>,
    onEvent: (FlightDetailEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Passengers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(
                    onClick = { onEvent(FlightDetailEvent.AddPassenger) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            passengers.forEachIndexed { index, passenger ->
                PassengerFormCard(
                    index = index,
                    passenger = passenger,
                    isActive = index == activePassengerIndex,
                    canRemove = passengers.size > 1,
                    seatLabel = passenger.selectedSeatId?.let { seatId ->
                        seats.find { it.id == seatId }?.let { "${it.row}${it.column}" }
                    },
                    onEvent = onEvent
                )
                if (index < passengers.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PassengerFormCard(
    index: Int,
    passenger: PassengerForm,
    isActive: Boolean,
    canRemove: Boolean,
    seatLabel: String?,
    onEvent: (FlightDetailEvent) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEvent(FlightDetailEvent.SetActivePassenger(index)) },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Passenger ${index + 1}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                    if (seatLabel != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Seat $seatLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (canRemove) {
                    IconButton(
                        onClick = { onEvent(FlightDetailEvent.RemovePassenger(index)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = passenger.name,
                onValueChange = { onEvent(FlightDetailEvent.UpdatePassengerName(index, it)) },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = passenger.email,
                onValueChange = { onEvent(FlightDetailEvent.UpdatePassengerEmail(index, it)) },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PriceSummary(flight: Flight, passengerCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Price per person", style = MaterialTheme.typography.bodyMedium)
                Text("$${String.format("%.2f", flight.price)}", style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Passengers", style = MaterialTheme.typography.bodyMedium)
                Text("x$passengerCount", style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$${String.format("%.2f", flight.price * passengerCount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatDateTime(dateTimeString: String): String {
    return try {
        if (dateTimeString.contains("T")) {
            val parts = dateTimeString.split("T")
            "${parts[0]} ${parts[1].take(5)}"
        } else {
            dateTimeString
        }
    } catch (_: Exception) {
        dateTimeString
    }
}
