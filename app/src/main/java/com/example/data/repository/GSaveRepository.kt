package com.example.data.repository

import com.example.data.dao.*
import com.example.data.entity.*
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

    suspend fun insertWallet(wallet: WalletEntity) = walletDao.insertWallet(wallet)
    suspend fun updateWallet(wallet: WalletEntity) = walletDao.updateWallet(wallet)
    suspend fun deleteWallet(id: Long) = walletDao.deleteWallet(id)

    suspend fun insertTransaction(transaction: TransactionEntity) = transactionDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: TransactionEntity) = transactionDao.updateTransaction(transaction)
    suspend fun deleteTransaction(id: Long) = transactionDao.deleteTransaction(id)

    suspend fun updateBucket(bucket: BucketEntity) = bucketDao.updateBucket(bucket)

    suspend fun updatePartnerVault(vault: PartnerVaultEntity) = partnerVaultDao.updatePartnerVault(vault)
    suspend fun insertPartnerVault(vault: PartnerVaultEntity) = partnerVaultDao.insertPartnerVault(vault)
    suspend fun deleteAllPartnerVaults() = partnerVaultDao.deleteAllPartnerVaults()

    suspend fun savePartnerVault(vault: PartnerVaultEntity) {
        partnerVaultDao.deleteAllPartnerVaults()
        partnerVaultDao.insertPartnerVault(vault.copy(id = 1L))
    }

    suspend fun updateUserPreferences(prefs: UserPreferencesEntity) = userPreferencesDao.updateUserPreferences(prefs)

    suspend fun wipeOutData() {
        walletDao.deleteAllWallets()
        transactionDao.deleteAllTransactions()
        bucketDao.deleteAllBuckets()
        partnerVaultDao.deleteAllPartnerVaults()

        // Re-seed default wallets & buckets
        walletDao.insertWallet(WalletEntity(name = "GCash", type = "E-Wallet", balance = 0.0))
        walletDao.insertWallet(WalletEntity(name = "BPI Savings", type = "Bank", balance = 0.0))
        walletDao.insertWallet(WalletEntity(name = "Cash on Hand", type = "Cash", balance = 0.0))

        bucketDao.insertBucket(BucketEntity(name = "Bills & Needs", percentage = 50, targetAmount = 25000.0, currentAmount = 0.0))
        bucketDao.insertBucket(BucketEntity(name = "Savings", percentage = 30, targetAmount = 50000.0, currentAmount = 0.0))
        bucketDao.insertBucket(BucketEntity(name = "Emergency Funds", percentage = 10, targetAmount = 30000.0, currentAmount = 0.0))
        bucketDao.insertBucket(BucketEntity(name = "Wants", percentage = 10, targetAmount = 10000.0, currentAmount = 0.0))
    }
}
