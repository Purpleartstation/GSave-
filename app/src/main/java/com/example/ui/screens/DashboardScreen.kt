package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.*
import com.example.ui.theme.GSaveBlue
import com.example.ui.theme.GSaveGreen

@Composable
fun DashboardScreen(
    wallets: List<WalletEntity>,
    transactions: List<TransactionEntity>,
    buckets: List<BucketEntity>,
    userName: String,
    onNavigateTab: (Int) -> Unit
) {
    val totalBalance = wallets.sumOf { it.balance }
    val totalIncome = transactions.filter { it.type == "Income" }.sumOf { it.amount }
    val totalExpenses = transactions.filter { it.type == "Withdrawal" || it.type == "Bill" }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mabuhay, $userName 👋",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "GSave+ Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(
                    onClick = { onNavigateTab(3) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GSaveGreen.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = GSaveGreen
                    )
                }
            }
        }

        // Total Balance Card (Peso Signature)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GSaveGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Total Vault Balance (PHP)",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₱ %.2f".format(totalBalance),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Income", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text("₱ %.2f".format(totalIncome), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column {
                            Text("Total Expenses", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text("₱ %.2f".format(totalExpenses), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Google Calendar Sync Reminder Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GSaveBlue.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = GSaveBlue,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Calendar Sync Active",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "2 upcoming bills (Meralco & Water) synced automatically.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Money Flow Chart Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Money Flow Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxVal = maxOf(totalIncome, totalExpenses, 1000.0).toFloat()
                            val width = size.width
                            val height = size.height
                            val barWidth = width * 0.25f

                            val incomeHeight = (totalIncome.toFloat() / maxVal) * (height - 30f)
                            val expenseHeight = (totalExpenses.toFloat() / maxVal) * (height - 30f)

                            drawRect(
                                color = GSaveGreen,
                                topLeft = Offset(width * 0.2f - barWidth / 2f, height - incomeHeight),
                                size = androidx.compose.ui.geometry.Size(barWidth, incomeHeight)
                            )

                            drawRect(
                                color = GSaveBlue,
                                topLeft = Offset(width * 0.7f - barWidth / 2f, height - expenseHeight),
                                size = androidx.compose.ui.geometry.Size(barWidth, expenseHeight)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("Income (₱ %.0f)".format(totalIncome), color = GSaveGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Expenses (₱ %.0f)".format(totalExpenses), color = GSaveBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Automated Income Allocation Buckets
        item {
            Text(
                text = "Automated Savings Buckets (50/30/10/10)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(buckets.size) { index ->
            val bucket = buckets[index]
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = bucket.name,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${bucket.percentage}% Allocation",
                            style = MaterialTheme.typography.bodySmall,
                            color = GSaveGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val progress = if (bucket.targetAmount > 0) (bucket.currentAmount / bucket.targetAmount).coerceIn(0.0, 1.0).toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = GSaveGreen,
                        trackColor = GSaveGreen.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("₱ %.2f".format(bucket.currentAmount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text("Target: ₱ %.2f".format(bucket.targetAmount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}
