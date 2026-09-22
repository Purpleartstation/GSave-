package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.*
import com.example.ui.theme.GSaveBlue
import com.example.ui.theme.GSaveGreen

@Composable
fun SettingsScreen(
    userPrefs: UserPreferencesEntity?,
    buckets: List<BucketEntity>,
    onUpdatePreferences: (String, Boolean, Boolean, Boolean, Boolean, String) -> Unit,
    onUpdateBuckets: (List<BucketEntity>) -> Unit,
    onWipeOutData: () -> Unit
) {
    var calendarSync by remember(userPrefs) { mutableStateOf(userPrefs?.googleCalendarSyncEnabled ?: true) }
    var pinEnabled by remember(userPrefs) { mutableStateOf(userPrefs?.isPinEnabled ?: true) }
    var darkMode by remember(userPrefs) { mutableStateOf(userPrefs?.darkThemeMode ?: false) }
    var userName by remember(userPrefs) { mutableStateOf(userPrefs?.userName ?: "Maria Santos") }
    var pinCode by remember(userPrefs) { mutableStateOf(userPrefs?.pinCode ?: "1234") }

    var showPinDialog by remember { mutableStateOf(false) }
    var showWipeDialog by remember { mutableStateOf(false) }
    var showConfirmWipeDialog by remember { mutableStateOf(false) }

    var bucketList by remember(buckets) { mutableStateOf(buckets) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "GSave+ Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Account Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Account Management", fontWeight = FontWeight.Bold, color = GSaveGreen)
                    OutlinedTextField(
                        value = userName,
                        onValueChange = {
                            userName = it
                            onUpdatePreferences(pinCode, pinEnabled, calendarSync, darkMode, true, userName)
                        },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { showPinDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = GSaveBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change 4-Digit Security PIN")
                    }
                }
            }
        }

        // Preferences Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("App Preferences & Sync", fontWeight = FontWeight.Bold, color = GSaveGreen)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Google Calendar Reminders & Dues")
                        Switch(
                            checked = calendarSync,
                            onCheckedChange = {
                                calendarSync = it
                                onUpdatePreferences(pinCode, pinEnabled, calendarSync, darkMode, true, userName)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Require PIN on Startup")
                        Switch(
                            checked = pinEnabled,
                            onCheckedChange = {
                                pinEnabled = it
                                onUpdatePreferences(pinCode, pinEnabled, calendarSync, darkMode, true, userName)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Mode Theme")
                        Switch(
                            checked = darkMode,
                            onCheckedChange = {
                                darkMode = it
                                onUpdatePreferences(pinCode, pinEnabled, calendarSync, darkMode, true, userName)
                            }
                        )
                    }
                }
            }
        }

        // Custom Percentage Allocation
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Custom Bucket Percentages", fontWeight = FontWeight.Bold, color = GSaveGreen)
                    Text("Adjust how automatic income split allocates your funds:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                    bucketList.forEachIndexed { index, bucket ->
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(bucket.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("${bucket.percentage}%", fontSize = 13.sp, color = GSaveBlue)
                            }
                            Slider(
                                value = bucket.percentage.toFloat(),
                                onValueChange = { newVal ->
                                    val updated = bucketList.toMutableList()
                                    updated[index] = bucket.copy(percentage = newVal.toInt())
                                    bucketList = updated
                                    onUpdateBuckets(bucketList)
                                },
                                valueRange = 0f..100f,
                                steps = 20
                            )
                        }
                    }
                }
            }
        }

        // Wipe Out Data Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Danger Zone", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("Clear all user-added transactions, wallets, and custom data for a fresh start.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { showWipeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wipe Out All Data")
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Change 4-Digit PIN") },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) newPin = it },
                    label = { Text("New 4-Digit PIN") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newPin.length == 4) {
                        pinCode = newPin
                        onUpdatePreferences(pinCode, pinEnabled, calendarSync, darkMode, true, userName)
                        showPinDialog = false
                    }
                }) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = { Text("Wipe Out Data?") },
            text = { Text("Are you sure you want to delete all wallets, transactions, and savings buckets? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeDialog = false
                        showConfirmWipeDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showConfirmWipeDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmWipeDialog = false },
            title = { Text("Final Confirmation ⚠️") },
            text = { Text("This is your last warning. All Philippine Peso tracking data will be completely wiped out for a fresh start.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmWipeDialog = false
                        onWipeOutData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Wipe Everything Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmWipeDialog = false }) { Text("Cancel") }
            }
        )
    }
}
