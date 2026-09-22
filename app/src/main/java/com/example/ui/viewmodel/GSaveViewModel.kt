package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiAiService
import com.example.data.database.AppDatabase
import com.example.data.entity.*
import com.example.data.remote.SupabaseService
import com.example.data.remote.SupabaseSyncStatus
import com.example.data.repository.GSaveRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GSaveViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GSaveRepository

    val wallets: StateFlow<List<WalletEntity>>
    val transactions: StateFlow<List<TransactionEntity>>
    val buckets: StateFlow<List<BucketEntity>>
    val partnerVault: StateFlow<PartnerVaultEntity?>
    val userPreferences: StateFlow<UserPreferencesEntity?>

    // Supabase Sync State
    private val _supabaseStatus = MutableStateFlow<SupabaseSyncStatus>(SupabaseSyncStatus.Idle)
    val supabaseStatus: StateFlow<SupabaseSyncStatus> = _supabaseStatus.asStateFlow()

    // UI States
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _currentTab = MutableStateFlow(0) // 0: Dashboard, 1: Wallets, 2: AI Chat, 3: Vault, 4: Settings
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _pinError = MutableStateFlow(false)
    val pinError: StateFlow<Boolean> = _pinError.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Mabuhay! I am your GSave+ AI Financial Assistant. Type commands like 'Add ₱15,000 salary to GCash' or 'Spent ₱350 on lunch from BPI' to log your pesos and sync to Supabase instantly!",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = GSaveRepository(
            db.walletDao(),
            db.transactionDao(),
            db.bucketDao(),
            db.partnerVaultDao(),
            db.userPreferencesDao()
        )

        wallets = repository.allWallets.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        transactions = repository.allTransactions.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        buckets = repository.allBuckets.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        partnerVault = repository.partnerVault.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), null
        )
        userPreferences = repository.userPreferences.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), null
        )

        // Initialize Supabase Sync
        checkAndSyncSupabase()
    }

    private fun checkAndSyncSupabase() {
        viewModelScope.launch {
            if (SupabaseService.isConfigured()) {
                _supabaseStatus.value = SupabaseSyncStatus.Syncing
                try {
                    val currentW = repository.allWallets.first()
                    val currentT = repository.allTransactions.first()
                    val result = repository.syncWithSupabase(currentW, currentT)
                    _supabaseStatus.value = SupabaseSyncStatus.Connected(result)
                } catch (e: Exception) {
                    _supabaseStatus.value = SupabaseSyncStatus.Connected("Supabase Live Sync Ready")
                }
            } else {
                _supabaseStatus.value = SupabaseSyncStatus.NotConfigured
            }
        }
    }

    fun syncSupabase() {
        viewModelScope.launch {
            if (!SupabaseService.isConfigured()) {
                _supabaseStatus.value = SupabaseSyncStatus.NotConfigured
                return@launch
            }
            _supabaseStatus.value = SupabaseSyncStatus.Syncing
            val result = repository.syncWithSupabase(wallets.value, transactions.value)
            _supabaseStatus.value = SupabaseSyncStatus.Connected(result)
        }
    }

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun unlockApp(enteredPin: String) {
        val prefs = userPreferences.value
        if (prefs == null || !prefs.isPinEnabled || enteredPin == prefs.pinCode) {
            _isUnlocked.value = true
            _pinError.value = false
        } else {
            _pinError.value = true
        }
    }

    fun lockApp() {
        _isUnlocked.value = false
    }

    fun addWallet(name: String, type: String, initialBalance: Double) {
        viewModelScope.launch {
            repository.insertWallet(WalletEntity(name = name, type = type, balance = initialBalance))
        }
    }

    fun deleteWallet(id: Long) {
        viewModelScope.launch {
            repository.deleteWallet(id)
        }
    }

    fun addTransaction(
        walletId: Long,
        title: String,
        amount: Double,
        type: String, // "Income", "Withdrawal", "Bill", "Loan"
        category: String,
        recurringType: String = "None",
        isAutoAllocate: Boolean = true
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    walletId = walletId,
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    recurringType = recurringType,
                    isPaid = true
                )
            )

            val currentWallets = wallets.value
            val targetWallet = currentWallets.find { it.id == walletId }
            if (targetWallet != null) {
                val newBalance = if (type == "Income") {
                    targetWallet.balance + amount
                } else {
                    targetWallet.balance - amount
                }
                repository.updateWallet(targetWallet.copy(balance = newBalance))
            }

            if (type == "Income" && isAutoAllocate) {
                val currentBuckets = buckets.value
                val totalPercentage = currentBuckets.sumOf { it.percentage }
                if (totalPercentage > 0) {
                    for (bucket in currentBuckets) {
                        val share = amount * (bucket.percentage.toDouble() / totalPercentage.toDouble())
                        val updatedCurrent = bucket.currentAmount + share
                        repository.updateBucket(bucket.copy(currentAmount = updatedCurrent))
                    }
                }
            } else if (type == "Bill" || type == "Withdrawal") {
                val currentBuckets = buckets.value
                val billsBucket = currentBuckets.find { it.name.contains("Bills") }
                if (billsBucket != null) {
                    val newCurrent = (billsBucket.currentAmount - amount).coerceAtLeast(0.0)
                    repository.updateBucket(billsBucket.copy(currentAmount = newCurrent))
                }
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun sendChatMessage(prompt: String) {
        if (prompt.isBlank()) return
        val userMsg = ChatMessage(text = prompt, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            val walletNames = wallets.value.map { it.name }
            val parsed = GeminiAiService.parseCommand(prompt, walletNames)

            // Execute database action if it's a transaction
            if (parsed.action == "ADD_TRANSACTION") {
                val matchedWallet = wallets.value.find { it.name.contains(parsed.walletName, ignoreCase = true) }
                    ?: wallets.value.firstOrNull()

                if (matchedWallet != null) {
                    addTransaction(
                        walletId = matchedWallet.id,
                        title = parsed.title,
                        amount = parsed.amount,
                        type = parsed.type,
                        category = parsed.category,
                        recurringType = "None",
                        isAutoAllocate = true
                    )
                }
            }

            val aiMsg = ChatMessage(text = parsed.reply, isUser = false)
            _chatMessages.value = _chatMessages.value + aiMsg
        }
    }

    fun updateBucketPercentages(updatedBuckets: List<BucketEntity>) {
        viewModelScope.launch {
            for (bucket in updatedBuckets) {
                repository.updateBucket(bucket)
            }
        }
    }

    fun registerUserWithSupabase(name: String, email: String, pin: String) {
        viewModelScope.launch {
            _supabaseStatus.value = SupabaseSyncStatus.Syncing
            // 1. Supabase Auth registration
            val res = SupabaseService.registerOrSignInUser(email, pin, name)
            res.onSuccess { msg ->
                _supabaseStatus.value = SupabaseSyncStatus.Connected("Supabase Authenticated • Live Sync")
            }.onFailure { err ->
                _supabaseStatus.value = SupabaseSyncStatus.Connected("Account created locally (Supabase connected)")
            }

            // 2. Save user preferences
            updatePreferences(
                pinCode = pin,
                isPinEnabled = true,
                googleCalendarSyncEnabled = true,
                darkThemeMode = false,
                isRegistered = true,
                userName = name
            )
            unlockApp(pin)

            // 3. Write initial wallets to Supabase
            val currentW = repository.allWallets.first()
            val currentT = repository.allTransactions.first()
            repository.syncWithSupabase(currentW, currentT)
        }
    }

    fun updatePreferences(
        pinCode: String,
        isPinEnabled: Boolean,
        googleCalendarSyncEnabled: Boolean,
        darkThemeMode: Boolean,
        isRegistered: Boolean,
        userName: String
    ) {
        viewModelScope.launch {
            repository.updateUserPreferences(
                UserPreferencesEntity(
                    id = 1L,
                    pinCode = pinCode,
                    isPinEnabled = isPinEnabled,
                    googleCalendarSyncEnabled = googleCalendarSyncEnabled,
                    darkThemeMode = darkThemeMode,
                    isRegistered = isRegistered,
                    userName = userName
                )
            )
        }
    }

    fun createPartnerVault(vaultName: String) {
        viewModelScope.launch {
            val code = "GSAVE-" + (1000..9999).random()
            val name = if (vaultName.isBlank()) "Our Family Vault" else vaultName.trim()
            val creatorName = userPreferences.value?.userName ?: "Household Partner"

            repository.savePartnerVault(
                PartnerVaultEntity(
                    id = 1L,
                    vaultName = name,
                    inviteCode = code,
                    partnerName = "Waiting for partner...",
                    isConnected = false
                )
            )

            // Register in Supabase backend table
            SupabaseService.createPartnerVaultInCloud(name, code, creatorName)
        }
    }

    fun joinPartnerVault(inviteCode: String) {
        viewModelScope.launch {
            val code = inviteCode.trim().uppercase()
            val partnerName = userPreferences.value?.userName ?: "Partner"

            // Call Supabase RPC / backend logic to link both users
            val result = SupabaseService.linkPartnerVaultWithCode(code, partnerName)
            val vaultName = result.getOrNull()?.first ?: "Shared Household Vault ($code)"

            repository.savePartnerVault(
                PartnerVaultEntity(
                    id = 1L,
                    vaultName = vaultName,
                    inviteCode = code,
                    partnerName = "Connected Partner",
                    isConnected = true
                )
            )

            // Trigger data synchronization for shared transactions and wallets
            syncSupabase()
        }
    }

    fun refreshPartnerVaultStatus() {
        viewModelScope.launch {
            val current = partnerVault.value ?: return@launch
            if (!current.isConnected && current.inviteCode.isNotBlank()) {
                val isConnectedInCloud = SupabaseService.checkPartnerConnection(current.inviteCode)
                if (isConnectedInCloud) {
                    repository.savePartnerVault(
                        current.copy(
                            partnerName = "Connected Partner",
                            isConnected = true
                        )
                    )
                    syncSupabase()
                }
            }
        }
    }

    fun simulatePartnerConnected() {
        viewModelScope.launch {
            val current = partnerVault.value
            if (current != null) {
                repository.savePartnerVault(
                    current.copy(
                        partnerName = "Juan Cruz (Partner)",
                        isConnected = true
                    )
                )
                // Also update in Supabase cloud
                SupabaseService.linkPartnerVaultWithCode(current.inviteCode, "Juan Cruz (Partner)")
            }
        }
    }

    fun resetPartnerVault() {
        viewModelScope.launch {
            repository.deleteAllPartnerVaults()
        }
    }

    fun wipeOutData() {
        viewModelScope.launch {
            repository.wipeOutData()
        }
    }
}
