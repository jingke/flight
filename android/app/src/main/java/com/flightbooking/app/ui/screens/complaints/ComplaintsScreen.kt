package com.flightbooking.app.ui.screens.complaints

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flightbooking.app.domain.model.Complaint
import com.flightbooking.app.domain.model.ComplaintStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ComplaintsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val message = uiState.successMessage ?: uiState.errorMessage
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(ComplaintsEvent.ClearMessages)
        }
    }

    if (uiState.showNewComplaintDialog) {
        NewComplaintDialog(
            bookings = uiState.bookings.map { it.id to "Booking #${it.id} - ${it.flight.flightNumber}" },
            selectedBookingId = uiState.selectedBookingId,
            subject = uiState.subject,
            description = uiState.description,
            isSubmitting = uiState.isSubmitting,
            onBookingSelected = { viewModel.onEvent(ComplaintsEvent.UpdateSelectedBooking(it)) },
            onSubjectChange = { viewModel.onEvent(ComplaintsEvent.UpdateSubject(it)) },
            onDescriptionChange = { viewModel.onEvent(ComplaintsEvent.UpdateDescription(it)) },
            onSubmit = { viewModel.onEvent(ComplaintsEvent.SubmitComplaint) },
            onDismiss = { viewModel.onEvent(ComplaintsEvent.DismissNewComplaintDialog) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complaints") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(ComplaintsEvent.ShowNewComplaintDialog) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Complaint")
            }
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
                uiState.complaints.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Report,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No complaints", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tap + to submit a new complaint",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                    ) {
                        items(uiState.complaints, key = { it.id }) { complaint ->
                            ComplaintCard(complaint = complaint)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComplaintCard(complaint: Complaint) {
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
                    text = complaint.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ComplaintStatusChip(status = complaint.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = complaint.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
            if (complaint.bookingId != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Booking #${complaint.bookingId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (complaint.adminResponse != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Admin Response",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = complaint.adminResponse,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatDate(complaint.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ComplaintStatusChip(status: ComplaintStatus) {
    val (label, icon, containerColor) = when (status) {
        ComplaintStatus.OPEN -> Triple("Open", Icons.Default.Info, MaterialTheme.colorScheme.tertiaryContainer)
        ComplaintStatus.IN_PROGRESS -> Triple("In Progress", Icons.Default.HourglassEmpty, MaterialTheme.colorScheme.secondaryContainer)
        ComplaintStatus.RESOLVED -> Triple("Resolved", Icons.Default.CheckCircle, MaterialTheme.colorScheme.primaryContainer)
        ComplaintStatus.CLOSED -> Triple("Closed", Icons.Default.CheckCircle, MaterialTheme.colorScheme.surfaceVariant)
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = containerColor)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewComplaintDialog(
    bookings: List<Pair<Int, String>>,
    selectedBookingId: Int?,
    subject: String,
    description: String,
    isSubmitting: Boolean,
    onBookingSelected: (Int?) -> Unit,
    onSubjectChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    var isBookingExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = if (selectedBookingId != null) {
        bookings.find { it.first == selectedBookingId }?.second ?: "None"
    } else {
        "None (General)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Complaint") },
        text = {
            Column {
                if (bookings.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = isBookingExpanded,
                        onExpandedChange = { isBookingExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Related Booking (Optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBookingExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isBookingExpanded,
                            onDismissRequest = { isBookingExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (General)") },
                                onClick = {
                                    onBookingSelected(null)
                                    isBookingExpanded = false
                                }
                            )
                            bookings.forEach { (id, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        onBookingSelected(id)
                                        isBookingExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = subject,
                    onValueChange = onSubjectChange,
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    placeholder = { Text("Describe your complaint...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSubmit, enabled = !isSubmitting) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Submit")
            }
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
