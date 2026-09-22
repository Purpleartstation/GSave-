package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.GSaveTheme
import com.example.ui.viewmodel.GSaveViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GSaveViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
            val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
            val pinError by viewModel.pinError.collectAsStateWithLifecycle()

            val wallets by viewModel.wallets.collectAsStateWithLifecycle()
            val transactions by viewModel.transactions.collectAsStateWithLifecycle()
            val buckets by viewModel.buckets.collectAsStateWithLifecycle()
            val partnerVault by viewModel.partnerVault.collectAsStateWithLifecycle()
            val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
            val supabaseStatus by viewModel.supabaseStatus.collectAsStateWithLifecycle()

            val darkTheme = userPrefs?.darkThemeMode ?: false

            GSaveTheme(darkTheme = darkTheme) {
                if (userPrefs != null && !userPrefs!!.isRegistered) {
                    AuthRegistrationScreen(
                        onCompleteRegistration = { name, email, pin ->
                            viewModel.registerUserWithSupabase(name, email, pin)
                        }
                    )
                } else if (!isUnlocked && (userPrefs?.isPinEnabled == true)) {
                    PinLockScreen(
                        pinError = pinError,
                        onUnlock = { enteredPin ->
                            viewModel.unlockApp(enteredPin)
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar(
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                val items = listOf(
                                    Triple("Dashboard", Icons.Default.Dashboard, 0),
                                    Triple("Wallets", Icons.Default.AccountBalanceWallet, 1),
                                    Triple("AI Chat", Icons.Default.SmartToy, 2),
                                    Triple("Vault", Icons.Default.Group, 3),
                                    Triple("Settings", Icons.Default.Settings, 4)
                                )
                                items.forEach { (label, icon, index) ->
                                    NavigationBarItem(
                                        icon = { Icon(imageVector = icon, contentDescription = label) },
                                        label = { Text(label, fontSize = 10.sp) },
                                        selected = currentTab == index,
                                        onClick = { viewModel.setTab(index) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (currentTab) {
                                0 -> DashboardScreen(
                                    wallets = wallets,
                                    transactions = transactions,
                                    buckets = buckets,
                                    userName = userPrefs?.userName ?: "Maria",
                                    onNavigateTab = { viewModel.setTab(it) }
                                )
                                1 -> WalletsScreen(
                                    wallets = wallets,
                                    transactions = transactions,
                                    onAddWallet = { name, type, bal -> viewModel.addWallet(name, type, bal) },
                                    onDeleteWallet = { id -> viewModel.deleteWallet(id) },
                                    onAddTransaction = { wId, title, amt, type, cat, rec, auto ->
                                        viewModel.addTransaction(wId, title, amt, type, cat, rec, auto)
                                    },
                                    onDeleteTransaction = { id -> viewModel.deleteTransaction(id) }
                                )
                                2 -> AiChatScreen(
                                    messages = chatMessages,
                                    onSendMessage = { prompt -> viewModel.sendChatMessage(prompt) }
                                )
                                3 -> PartnerVaultScreen(
                                    vault = partnerVault,
                                    onCreateVault = { name -> viewModel.createPartnerVault(name) },
                                    onJoinVault = { code -> viewModel.joinPartnerVault(code) },
                                    onRefreshStatus = { viewModel.refreshPartnerVaultStatus() },
                                    onSimulatePartnerConnect = { viewModel.simulatePartnerConnected() },
                                    onResetVault = { viewModel.resetPartnerVault() }
                                )
                                4 -> SettingsScreen(
                                    userPrefs = userPrefs,
                                    buckets = buckets,
                                    supabaseStatus = supabaseStatus,
                                    onSyncSupabase = { viewModel.syncSupabase() },
                                    onUpdatePreferences = { pin, pinEn, cal, dark, reg, name ->
                                        viewModel.updatePreferences(pin, pinEn, cal, dark, reg, name)
                                    },
                                    onUpdateBuckets = { updatedBuckets ->
                                        viewModel.updateBucketPercentages(updatedBuckets)
                                    },
                                    onWipeOutData = { viewModel.wipeOutData() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
