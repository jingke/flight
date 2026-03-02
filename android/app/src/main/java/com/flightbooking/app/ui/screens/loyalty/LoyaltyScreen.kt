package com.flightbooking.app.ui.screens.loyalty

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flightbooking.app.domain.model.LoyaltyPoints

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyScreen(
    onNavigateBack: () -> Unit,
    viewModel: LoyaltyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val message = uiState.successMessage ?: uiState.errorMessage
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(LoyaltyEvent.ClearMessages)
        }
    }

    if (uiState.showRedeemDialog) {
        RedeemDialog(
            redeemAmount = uiState.redeemAmount,
            balance = uiState.loyaltyPoints?.balance ?: 0,
            onAmountChange = { viewModel.onEvent(LoyaltyEvent.UpdateRedeemAmount(it)) },
            onConfirm = { viewModel.onEvent(LoyaltyEvent.ConfirmRedeem) },
            onDismiss = { viewModel.onEvent(LoyaltyEvent.DismissRedeemDialog) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loyalty Points") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                uiState.loyaltyPoints == null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CardGiftcard,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No loyalty data", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.onEvent(LoyaltyEvent.LoadPoints) }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    LoyaltyContent(
                        loyaltyPoints = uiState.loyaltyPoints!!,
                        isRedeeming = uiState.isRedeeming,
                        onRedeem = { viewModel.onEvent(LoyaltyEvent.ShowRedeemDialog) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoyaltyContent(
    loyaltyPoints: LoyaltyPoints,
    isRedeeming: Boolean,
    onRedeem: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Stars,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Available Balance",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${loyaltyPoints.balance}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "points",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PointsSummaryCard(
                title = "Earned",
                points = loyaltyPoints.earned,
                icon = Icons.Default.CardGiftcard,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            PointsSummaryCard(
                title = "Redeemed",
                points = loyaltyPoints.redeemed,
                icon = Icons.Default.Redeem,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Points Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        LoyaltyChart(loyaltyPoints = loyaltyPoints)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRedeem,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRedeeming && loyaltyPoints.balance > 0
        ) {
            if (isRedeeming) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Default.Redeem, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Redeem Points")
        }
    }
}

@Composable
private fun PointsSummaryCard(
    title: String,
    points: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$points",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoyaltyChart(loyaltyPoints: LoyaltyPoints) {
    val earnedColor = MaterialTheme.colorScheme.primary
    val redeemedColor = MaterialTheme.colorScheme.tertiary
    val balanceColor = MaterialTheme.colorScheme.secondary
    val maxValue = maxOf(loyaltyPoints.earned, loyaltyPoints.redeemed, loyaltyPoints.balance, 1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val barWidth = size.width / 5f
                val spacing = barWidth / 2f
                val chartHeight = size.height - 40f
                val startX = (size.width - 3 * barWidth - 2 * spacing) / 2f
                drawBar(
                    startX = startX,
                    chartHeight = chartHeight,
                    barWidth = barWidth,
                    value = loyaltyPoints.earned,
                    maxValue = maxValue,
                    color = earnedColor,
                    label = "Earned"
                )
                drawBar(
                    startX = startX + barWidth + spacing,
                    chartHeight = chartHeight,
                    barWidth = barWidth,
                    value = loyaltyPoints.redeemed,
                    maxValue = maxValue,
                    color = redeemedColor,
                    label = "Redeemed"
                )
                drawBar(
                    startX = startX + 2 * (barWidth + spacing),
                    chartHeight = chartHeight,
                    barWidth = barWidth,
                    value = loyaltyPoints.balance,
                    maxValue = maxValue,
                    color = balanceColor,
                    label = "Balance"
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ChartLegendItem(color = earnedColor, label = "Earned")
                ChartLegendItem(color = redeemedColor, label = "Redeemed")
                ChartLegendItem(color = balanceColor, label = "Balance")
            }
        }
    }
}

private fun DrawScope.drawBar(
    startX: Float,
    chartHeight: Float,
    barWidth: Float,
    value: Int,
    maxValue: Int,
    color: Color,
    label: String
) {
    val barHeight = if (maxValue > 0) (value.toFloat() / maxValue) * chartHeight else 0f
    val topY = chartHeight - barHeight
    drawRoundRect(
        color = color,
        topLeft = Offset(startX, topY),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 28f
            this.color = android.graphics.Color.GRAY
        }
        drawText(
            "$value",
            startX + barWidth / 2,
            topY - 8f,
            paint
        )
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RedeemDialog(
    redeemAmount: String,
    balance: Int,
    onAmountChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redeem Points") },
        text = {
            Column {
                Text(
                    text = "Available balance: $balance points",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = redeemAmount,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) onAmountChange(newValue)
                    },
                    label = { Text("Points to Redeem") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Redeem") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
