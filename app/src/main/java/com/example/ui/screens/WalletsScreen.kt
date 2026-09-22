package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.*
import com.example.ui.theme.GSaveBlue
import com.example.ui.theme.GSaveGreen

@Composable
fun WalletsScreen(
    wallets: List<WalletEntity>,
    transactions: List<TransactionEntity>,
    onAddWallet: (String, String, Double) -> Unit,
    onDeleteWallet: (Long) -> Unit,
    onAddTransaction: (Long, String, Double, String, String, String, Boolean) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredTransactions = when (selectedFilter) {
        "Income" -> transactions.filter { it.type == "Income" }
        "Bills" -> transactions.filter { it.type == "Bill" }
        "Loans" -> transactions.filter { it.type == "Loan" }
        "Withdrawal" -> transactions.filter { it.type == "Withdrawal" }
        else -> transactions
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wallets & Vaults",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAddWalletDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GSaveGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Wallet")
                }
            }
        }

        // Wallets Horizontal Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(wallets) { wallet ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .width(200.dp)
                            .height(110.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = wallet.name,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { onDeleteWallet(wallet.id) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Text(
                                text = "₱ %.2f".format(wallet.balance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GSaveGreen
                            )
                            Text(
                                text = wallet.type,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAddTxDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GSaveBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Money / Bill")
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Income", "Bills", "Loans", "Withdrawal").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
        }

        items(filteredTransactions) { tx ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tx.title,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${tx.category} • ${tx.type}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        if (tx.recurringType != "None") {
                            Text(
                                text = "Recurring: ${tx.recurringType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GSaveBlue
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val isPositive = tx.type == "Income"
                        Text(
                            text = "${if (isPositive) "+" else "-"}₱ %.2f".format(tx.amount),
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) GSaveGreen else MaterialTheme.colorScheme.error
                        )
                        IconButton(
                            onClick = { onDeleteTransaction(tx.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddWalletDialog) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("E-Wallet") }
        var balance by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddWalletDialog = false },
            title = { Text("Create New Wallet / Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Wallet Name (e.g. Maya, BDO)") }
                    )
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Type (Cash, Bank, E-Wallet)") }
                    )
                    OutlinedTextField(
                        value = balance,
                        onValueChange = { balance = it },
                        label = { Text("Initial Amount (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amountVal = balance.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onAddWallet(name, type, amountVal)
                        showAddWalletDialog = false
                    }
                }) {
                    Text("Save Wallet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWalletDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddTxDialog) {
        var title by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Income") }
        var category by remember { mutableStateOf("Salary") }
        var recurringType by remember { mutableStateOf("None") }
        var isAutoAllocate by remember { mutableStateOf(true) }
        val selectedWalletId = wallets.firstOrNull()?.id ?: 1L

        AlertDialog(
            onDismissRequest = { showAddTxDialog = false },
            title = { Text("Add Transaction / Bill") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (e.g. Salary, Water Bill)") }
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount in Pesos (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Income", "Withdrawal", "Bill", "Loan").forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (Salary, Food, Utilities)") }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("None", "Daily", "Weekly", "Monthly").forEach { r ->
                            FilterChip(
                                selected = recurringType == r,
                                onClick = { recurringType = r },
                                label = { Text(r, fontSize = 11.sp) }
                            )
                        }
                    }
                    if (type == "Income") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isAutoAllocate, onCheckedChange = { isAutoAllocate = it })
                            Text("Auto-allocate 50/30/10/10 into buckets", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amtVal = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amtVal > 0) {
                        onAddTransaction(selectedWalletId, title, amtVal, type, category, recurringType, isAutoAllocate)
                        showAddTxDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTxDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
