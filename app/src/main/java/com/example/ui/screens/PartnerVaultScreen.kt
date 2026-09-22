package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PartnerVaultEntity
import com.example.ui.theme.GSaveBlue
import com.example.ui.theme.GSaveGreen
import kotlinx.coroutines.launch

@Composable
fun PartnerVaultScreen(
    vault: PartnerVaultEntity?,
    onCreateVault: (String) -> Unit,
    onJoinVault: (String) -> Unit,
    onSimulatePartnerConnect: () -> Unit = {},
    onResetVault: () -> Unit = {}
) {
    var vaultNameInput by remember { mutableStateOf("Our Family Vault") }
    var inviteCodeInput by remember { mutableStateOf("") }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showCreateNewDialog by remember { mutableStateOf(false) }
    var newVaultNameInput by remember { mutableStateOf("New Household Vault") }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Icon & Title
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(GSaveBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "Partner Vault",
                    tint = GSaveBlue,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Shared Household Partner Vault",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Collaborate with your partner in real-time. Share bills, manage joint expenses, and grow household savings together in Philippine Pesos (₱).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (vault != null) {
                // Active Vault Display Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("partner_vault_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title & Status Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = vault.vaultName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (vault.isConnected) "Joint Account Active" else "Ready to Link",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            if (vault.isConnected) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = GSaveGreen.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = GSaveGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Connected",
                                            color = GSaveGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = GSaveBlue.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassEmpty,
                                            contentDescription = null,
                                            tint = GSaveBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Waiting for Partner",
                                            color = GSaveBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Partner Details Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Household Partner:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = vault.partnerName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (vault.isConnected) GSaveBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }

                        // Generated Invite Code Box
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = GSaveGreen.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "YOUR PARTNER INVITE CODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp
                                )

                                Text(
                                    text = vault.inviteCode,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = GSaveGreen,
                                    modifier = Modifier.testTag("partner_invite_code")
                                )

                                Text(
                                    text = if (vault.isConnected) {
                                        "Share this code anytime to reconnect your household vault."
                                    } else {
                                        "Share this 6-digit code with your partner. When they enter it in GSave+, your vault links instantly!"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(vault.inviteCode))
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Invite code ${vault.inviteCode} copied to clipboard!")
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("copy_partner_code_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy Code")
                                    }

                                    Button(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Join my GSave+ Philippine Peso Shared Household Vault! Enter invite code: ${vault.inviteCode}"
                                                )
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Vault Code"))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GSaveBlue),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("share_partner_code_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share")
                                    }
                                }
                            }
                        }

                        // Simulation button if not connected yet
                        if (!vault.isConnected) {
                            OutlinedButton(
                                onClick = {
                                    onSimulatePartnerConnect()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Partner successfully connected to ${vault.vaultName}!")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("simulate_partner_connect_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = GSaveBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simulate Partner Joined (Test Connection)")
                            }
                        }

                        // Options row: Create New Vault or Join Different
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { showCreateNewDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Vault")
                            }

                            TextButton(
                                onClick = { showJoinDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Join Other")
                            }
                        }
                    }
                }
            } else {
                // Initial Create or Join Card (when no vault exists)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_vault_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Create or Join a Vault",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = vaultNameInput,
                            onValueChange = { vaultNameInput = it },
                            label = { Text("Vault Name") },
                            placeholder = { Text("e.g. Our Family Vault") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    val name = if (vaultNameInput.isBlank()) "Our Family Vault" else vaultNameInput.trim()
                                    onCreateVault(name)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                keyboardController?.hide()
                                val name = if (vaultNameInput.isBlank()) "Our Family Vault" else vaultNameInput.trim()
                                onCreateVault(name)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Vault '$name' created! Invite code generated.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GSaveGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("create_vault_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create Household Vault & Get Code",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        OutlinedButton(
                            onClick = { showJoinDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("join_vault_open_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = GSaveBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Join Partner Vault with Code")
                        }
                    }
                }
            }
        }
    }

    // Dialog for Joining Vault
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = GSaveBlue,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Join Partner Vault") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter the 6-character invite code provided by your partner:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inviteCodeInput,
                        onValueChange = { inviteCodeInput = it.uppercase() },
                        placeholder = { Text("e.g. GSAVE-4892") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (inviteCodeInput.isNotBlank()) {
                                    keyboardController?.hide()
                                    onJoinVault(inviteCodeInput.trim())
                                    showJoinDialog = false
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("join_invite_code_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inviteCodeInput.isNotBlank()) {
                            keyboardController?.hide()
                            onJoinVault(inviteCodeInput.trim())
                            showJoinDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Joined vault with code: ${inviteCodeInput.trim()}!")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GSaveGreen),
                    modifier = Modifier.testTag("confirm_join_vault_button")
                ) {
                    Text("Join Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog for Creating New Vault when one already exists
    if (showCreateNewDialog) {
        AlertDialog(
            onDismissRequest = { showCreateNewDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    tint = GSaveGreen,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Create New Household Vault") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This will generate a fresh new partner invite code in Philippine Pesos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newVaultNameInput,
                        onValueChange = { newVaultNameInput = it },
                        label = { Text("Vault Name") },
                        placeholder = { Text("e.g. Vacation Savings") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                val name = if (newVaultNameInput.isBlank()) "Our Household Vault" else newVaultNameInput.trim()
                                onCreateVault(name)
                                showCreateNewDialog = false
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_vault_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        val name = if (newVaultNameInput.isBlank()) "Our Household Vault" else newVaultNameInput.trim()
                        onCreateVault(name)
                        showCreateNewDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Created new vault '$name' with new code!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GSaveGreen),
                    modifier = Modifier.testTag("confirm_create_new_vault_button")
                ) {
                    Text("Create & Get Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateNewDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
