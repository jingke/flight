package com.flightbooking.app.ui.screens.map

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flightbooking.app.domain.model.Airport
import com.flightbooking.app.domain.model.Flight
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteMapScreen(
    onNavigateBack: () -> Unit,
    viewModel: RouteMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Route Map") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
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
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.errorMessage ?: "Flight not found",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    RouteMapContent(flight = uiState.flight!!)
                }
            }
        }
    }
}

@Composable
private fun RouteMapContent(flight: Flight) {
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
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = flight.flightNumber,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${flight.departureAirport.city} → ${flight.arrivalAirport.city}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        val routeColor = MaterialTheme.colorScheme.primary
        val departureColor = Color(0xFF4CAF50)
        val arrivalColor = Color(0xFFF44336)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            FlightRouteCanvas(
                departure = flight.departureAirport,
                arrival = flight.arrivalAirport,
                routeColor = routeColor,
                departureColor = departureColor,
                arrivalColor = arrivalColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AirportInfoCard(
                airport = flight.departureAirport,
                label = "Departure",
                icon = Icons.Default.FlightTakeoff,
                color = departureColor,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            AirportInfoCard(
                airport = flight.arrivalAirport,
                label = "Arrival",
                icon = Icons.Default.FlightLand,
                color = arrivalColor,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        DistanceCard(
            departure = flight.departureAirport,
            arrival = flight.arrivalAirport
        )
    }
}

@Composable
private fun FlightRouteCanvas(
    departure: Airport,
    arrival: Airport,
    routeColor: Color,
    departureColor: Color,
    arrivalColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val padding = 60f
        val canvasWidth = size.width - 2 * padding
        val canvasHeight = size.height - 2 * padding
        val allLats = listOf(departure.latitude, arrival.latitude)
        val allLongs = listOf(departure.longitude, arrival.longitude)
        val minLat = allLats.min()
        val maxLat = allLats.max()
        val minLong = allLongs.min()
        val maxLong = allLongs.max()
        val latRange = (maxLat - minLat).coerceAtLeast(1.0)
        val longRange = (maxLong - minLong).coerceAtLeast(1.0)
        fun toCanvasX(longitude: Double): Float {
            return padding + ((longitude - minLong) / longRange * canvasWidth).toFloat()
        }
        fun toCanvasY(latitude: Double): Float {
            return padding + ((maxLat - latitude) / latRange * canvasHeight).toFloat()
        }
        val depX = toCanvasX(departure.longitude)
        val depY = toCanvasY(departure.latitude)
        val arrX = toCanvasX(arrival.longitude)
        val arrY = toCanvasY(arrival.latitude)
        val midX = (depX + arrX) / 2f
        val midY = minOf(depY, arrY) - 60f
        val path = Path().apply {
            moveTo(depX, depY)
            quadraticBezierTo(midX, midY, arrX, arrY)
        }
        drawPath(
            path = path,
            color = routeColor.copy(alpha = 0.3f),
            style = Stroke(
                width = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f))
            )
        )
        drawPath(
            path = path,
            color = routeColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
        drawCircle(
            color = departureColor,
            radius = 14f,
            center = Offset(depX, depY)
        )
        drawCircle(
            color = Color.White,
            radius = 8f,
            center = Offset(depX, depY)
        )
        drawCircle(
            color = departureColor,
            radius = 5f,
            center = Offset(depX, depY)
        )
        drawCircle(
            color = arrivalColor,
            radius = 14f,
            center = Offset(arrX, arrY)
        )
        drawCircle(
            color = Color.White,
            radius = 8f,
            center = Offset(arrX, arrY)
        )
        drawCircle(
            color = arrivalColor,
            radius = 5f,
            center = Offset(arrX, arrY)
        )
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 32f
                isFakeBoldText = true
                color = android.graphics.Color.DKGRAY
            }
            drawText(departure.code, depX, depY - 24f, paint)
            drawText(arrival.code, arrX, arrY - 24f, paint)
        }
        val planeX = midX
        val planeY = midY + 10f
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 36f
                color = routeColor.hashCode()
            }
            drawText("✈", planeX, planeY, paint)
        }
    }
}

@Composable
private fun AirportInfoCard(
    airport: Airport,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = airport.code,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = airport.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "${airport.city}, ${airport.country}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "%.4f°, %.4f°".format(airport.latitude, airport.longitude),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DistanceCard(
    departure: Airport,
    arrival: Airport
) {
    val distanceKm = calculateHaversineDistance(
        lat1 = departure.latitude,
        lon1 = departure.longitude,
        lat2 = arrival.latitude,
        lon2 = arrival.longitude
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Distance",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "${String.format("%.0f", distanceKm)} km",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Est. Duration",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                val hours = (distanceKm / 850.0).toInt()
                val minutes = ((distanceKm / 850.0 - hours) * 60).toInt()
                Text(
                    text = "${hours}h ${minutes}m",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun calculateHaversineDistance(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusKm * c
}
