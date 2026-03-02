package com.flightbooking.app.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onSearch: (departure: String, arrival: String, date: String, passengers: Int) -> Unit
) {
    var departure by rememberSaveable { mutableStateOf("") }
    var arrival by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var passengers by rememberSaveable { mutableStateOf("1") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Flights") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Where would you like to go?",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = departure,
                onValueChange = { departure = it },
                label = { Text("From (Airport Code)") },
                leadingIcon = { Icon(Icons.Default.FlightTakeoff, contentDescription = null) },
                placeholder = { Text("e.g. JFK") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = arrival,
                onValueChange = { arrival = it },
                label = { Text("To (Airport Code)") },
                leadingIcon = { Icon(Icons.Default.FlightLand, contentDescription = null) },
                placeholder = { Text("e.g. LAX") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = passengers,
                onValueChange = { passengers = it },
                label = { Text("Passengers") },
                leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    onSearch(
                        departure.ifBlank { "any" },
                        arrival.ifBlank { "any" },
                        date.ifBlank { "any" },
                        passengers.toIntOrNull() ?: 1
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Search Flights")
            }
        }
    }
}
