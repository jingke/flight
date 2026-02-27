package com.flightbooking.app.ui.screens.bookings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flightbooking.app.domain.model.Booking
import com.flightbooking.app.domain.model.BookingStatus
import com.flightbooking.app.domain.model.PaymentStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    onNavigateBack: () -> Unit,
    onViewRouteMap: (Int) -> Unit = {},
    viewModel: BookingDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val message = uiState.successMessage ?: uiState.errorMessage
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(BookingDetailEvent.ClearMessages)
        }
    }

    if (uiState.showCancelDialog) {
        CancelConfirmationDialog(
            onConfirm = { viewModel.onEvent(BookingDetailEvent.ConfirmCancel) },
            onDismiss = { viewModel.onEvent(BookingDetailEvent.DismissCancelDialog) }
        )
    }

    if (uiState.showModifyDialog) {
        ModifyRequestDialog(
            modifyType = uiState.modifyType,
            modifyDetails = uiState.modifyDetails,
            onTypeChange = { viewModel.onEvent(BookingDetailEvent.UpdateModifyType(it)) },
            onDetailsChange = { viewModel.onEvent(BookingDetailEvent.UpdateModifyDetails(it)) },
            onSubmit = { viewModel.onEvent(BookingDetailEvent.SubmitModification) },
            onDismiss = { viewModel.onEvent(BookingDetailEvent.DismissModifyDialog) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.booking?.let { booking ->
                        IconButton(onClick = { onViewRouteMap(booking.flight.id) }) {
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
                uiState.booking == null -> {
                    Text(
                        text = "Booking not found",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    BookingDetailContent(
                        booking = uiState.booking!!,
                        isCancelling = uiState.isCancelling,
                        isSubmittingModification = uiState.isSubmittingModification,
                        onShowCancelDialog = { viewModel.onEvent(BookingDetailEvent.ShowCancelDialog) },
                        onShowModifyDialog = { viewModel.onEvent(BookingDetailEvent.ShowModifyDialog) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingDetailContent(
    booking: Booking,
    isCancelling: Boolean,
    isSubmittingModification: Boolean,
    onShowCancelDialog: () -> Unit,
    onShowModifyDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
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
                        text = "Booking #${booking.id}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    StatusChip(status = booking.status)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Booked on ${formatDate(booking.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = booking.flight.flightNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = booking.flight.departureAirport.code,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = booking.flight.departureAirport.city,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.AirplanemodeActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = booking.flight.arrivalAirport.code,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = booking.flight.arrivalAirport.city,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDateTime(booking.flight.departureTime),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatDateTime(booking.flight.arrivalTime),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Passengers (${booking.passengers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                booking.passengers.forEachIndexed { index, passenger ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = passenger.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = passenger.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (passenger.seatAssignment != null) {
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "Seat ${passenger.seatAssignment}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                    if (index < booking.passengers.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(start = 32.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
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
                    Text("Total Price", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "$${String.format("%.2f", booking.totalPrice)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Payment", style = MaterialTheme.typography.bodySmall)
                    val paymentLabel = when (booking.paymentStatus) {
                        PaymentStatus.PAID -> "Paid"
                        PaymentStatus.PENDING -> "Pending"
                        PaymentStatus.REFUNDED -> "Refunded"
                    }
                    Text(
                        paymentLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        if (booking.status != BookingStatus.CANCELLED) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onShowModifyDialog,
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmittingModification
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modify")
                }
                Button(
                    onClick = onShowCancelDialog,
                    modifier = Modifier.weight(1f),
                    enabled = !isCancelling,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: BookingStatus) {
    val (label, containerColor) = when (status) {
        BookingStatus.CONFIRMED -> "Confirmed" to MaterialTheme.colorScheme.primaryContainer
        BookingStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.errorContainer
        BookingStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.tertiaryContainer
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = containerColor)
    )
}

@Composable
private fun CancelConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel Booking") },
        text = { Text("Are you sure you want to cancel this booking? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Cancel Booking")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep Booking") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModifyRequestDialog(
    modifyType: String,
    modifyDetails: String,
    onTypeChange: (String) -> Unit,
    onDetailsChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val modificationTypes = listOf(
        "date_change" to "Date Change",
        "seat_change" to "Seat Change",
        "passenger_change" to "Passenger Change"
    )
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = modificationTypes.find { it.first == modifyType }?.second ?: "Date Change"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Modification") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        modificationTypes.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onTypeChange(value)
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = modifyDetails,
                    onValueChange = onDetailsChange,
                    label = { Text("Details") },
                    placeholder = { Text("Describe the changes you need...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSubmit) { Text("Submit Request") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatDate(dateTimeString: String): String {
    return try {
        dateTimeString.substringBefore("T")
    } catch (_: Exception) {
        dateTimeString
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
