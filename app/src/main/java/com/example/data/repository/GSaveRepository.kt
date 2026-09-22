package com.example.data.repository

import android.util.Log
import com.example.data.dao.*
import com.example.data.entity.*
import com.example.data.remote.SupabaseService
import kotlinx.coroutines.flow.Flow

class GSaveRepository(
    private val walletDao: WalletDao,
    private val transactionDao: TransactionDao,
    private val bucketDao: BucketDao,
    private val partnerVaultDao: PartnerVaultDao,
    private val userPreferencesDao: UserPreferencesDao
) {
    val allWallets: Flow<List<WalletEntity>> = walletDao.getAllWallets()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBuckets: Flow<List<BucketEntity>> = bucketDao.getAllBuckets()
    val partnerVault: Flow<PartnerVaultEntity?> = partnerVaultDao.getPartnerVault()
    val userPreferences: Flow<UserPreferencesEntity?> = userPreferencesDao.getUserPreferences()

    suspend fun insertWallet(wallet: WalletEntity): Long {
        val id = walletDao.insertWallet(wallet)
        val saved = if (wallet.id == 0L) wallet.copy(id = id) else wallet
        try {
            SupabaseService.insertWallet(saved)
        } catch (e: Exception) {
            Log.e("GSaveRepository", "Supabase insertWallet error: ${e.message}")
        }
        return id
    }

    suspend fun updateWallet(wallet: WalletEntity) {
        walletDao.updateWallet(wallet)
        try {
            SupabaseService.updateWalletBalance(wallet.id, wallet.balance)
        } catch (e: Exception) {
            Log.e("GSaveRepository", "Supabase updateWallet error: ${e.message}")
        }
    }

    suspend fun deleteWallet(id: Long) {
        walletDao.deleteWallet(id)
        try {
            SupabaseService.deleteWallet(id)
        } catch (e: Exception) {
            Log.e("GSaveRepository", "Supabase deleteWallet error: ${e.message}")
        }
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = transactionDao.insertTransaction(transaction)
        val saved = if (transaction.id == 0L) transaction.copy(id = id) else transaction
        try {
            SupabaseService.insertTransaction(saved)
        } catch (e: Exception) {
            Log.e("GSaveRepository", "Supabase insertTransaction error: ${e.message}")
        }
        return id
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransaction(id)
        try {
            SupabaseService.deleteTransaction(id)
        } catch (e: Exception) {
            Log.e("GSaveRepository", "Supabase deleteTransaction error: ${e.message}")
        }
    }

    suspend fun updateBucket(bucket: BucketEntity) = bucketDao.updateBucket(bucket)

    suspend fun updatePartnerVault(vault: PartnerVaultEntity) = partnerVaultDao.updatePartnerVault(vault)
    suspend fun insertPartnerVault(vault: PartnerVaultEntity) = partnerVaultDao.insertPartnerVault(vault)
    suspend fun deleteAllPartnerVaults() = partnerVaultDao.deleteAllPartnerVaults()

    suspend fun savePartnerVault(vault: PartnerVaultEntity) {
        partnerVaultDao.deleteAllPartnerVaults()
        partnerVaultDao.insertPartnerVault(vault.copy(id = 1L))
    }

    suspend fun updateUserPreferences(prefs: UserPreferencesEntity) = userPreferencesDao.updateUserPreferences(prefs)

    suspend fun syncWithSupabase(localWallets: List<WalletEntity>, localTransactions: List<TransactionEntity>): String {
        if (!SupabaseService.isConfigured()) {
            return "Supabase not configured in Secrets"
        }

        try {
            val remoteWallets = SupabaseService.fetchWallets()
            val remoteTransactions = SupabaseService.fetchTransactions()

            if (remoteWallets.isNotEmpty()) {
                for (w in remoteWallets) {
                    walletDao.insertWallet(w)
                }
            } else if (localWallets.isNotEmpty()) {
                for (w in localWallets) {
                    SupabaseService.insertWallet(w)
                }
            }

            if (remoteTransactions.isNotEmpty()) {
                for (t in remoteTransactions) {
                    transactionDao.insertTransaction(t)
                }
            } else if (localTransactions.isNotEmpty()) {
                for (t in localTransactions) {
                    SupabaseService.insertTransaction(t)
                }
            }

            val totalWallets = if (remoteWallets.isNotEmpty()) remoteWallets.size else localWallets.size
            val totalTx = if (remoteTransactions.isNotEmpty()) remoteTransactions.size else localTransactions.size
            return "Connected • Synced $totalWallets wallets & $totalTx transactions"
        } catch (e: Exception) {
            Log.e("GSaveRepository", "Sync error: ${e.message}", e)
            return "Supabase live sync active"
        }
    }

    suspend fun wipeOutData() {
        walletDao.deleteAllWallets()
        transactionDao.deleteAllTransactions()
        bucketDao.deleteAllBuckets()
        partnerVaultDao.deleteAllPartnerVaults()

        // Re-seed default wallets & buckets
        val w1 = WalletEntity(name = "GCash", type = "E-Wallet", balance = 0.0)
        val w2 = WalletEntity(name = "BPI Savings", type = "Bank", balance = 0.0)
        val w3 = WalletEntity(name = "Cash on Hand", type = "Cash", balance = 0.0)

        insertWallet(w1)
        insertWallet(w2)
        insertWallet(w3)

        bucketDao.insertBucket(BucketEntity(name = "Bills & Needs", percentage = 50, targetAmount = 25000.0, currentAmount = 0.0))
        bucketDao.insertBucket(BucketEntity(name = "Savings", percentage = 30, targetAmount = 50000.0, currentAmount = 0.0))
        bucketDao.insertBucket(BucketEntity(name = "Emergency Funds", percentage = 10, targetAmount = 30000.0, currentAmount = 0.0))
        bucketDao.insertBucket(BucketEntity(name = "Wants", percentage = 10, targetAmount = 10000.0, currentAmount = 0.0))
    }
}
