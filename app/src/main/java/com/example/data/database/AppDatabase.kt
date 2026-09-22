package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WalletEntity::class,
        TransactionEntity::class,
        BucketEntity::class,
        PartnerVaultEntity::class,
        UserPreferencesEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun transactionDao(): TransactionDao
    abstract fun bucketDao(): BucketDao
    abstract fun partnerVaultDao(): PartnerVaultDao
    abstract fun userPreferencesDao(): UserPreferencesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gsave_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            suspend fun populateInitialData(database: AppDatabase) {
                // Default Wallets
                database.walletDao().insertWallet(WalletEntity(name = "GCash", type = "E-Wallet", balance = 12500.00))
                database.walletDao().insertWallet(WalletEntity(name = "BPI Savings", type = "Bank", balance = 45000.00))
                database.walletDao().insertWallet(WalletEntity(name = "Cash on Hand", type = "Cash", balance = 2500.00))

                // Default Buckets (50% / 30% / 10% / 10%)
                database.bucketDao().insertBucket(BucketEntity(name = "Bills & Needs", percentage = 50, targetAmount = 25000.00, currentAmount = 18500.00))
                database.bucketDao().insertBucket(BucketEntity(name = "Savings", percentage = 30, targetAmount = 50000.00, currentAmount = 28000.00))
                database.bucketDao().insertBucket(BucketEntity(name = "Emergency Funds", percentage = 10, targetAmount = 30000.00, currentAmount = 12000.00))
                database.bucketDao().insertBucket(BucketEntity(name = "Wants", percentage = 10, targetAmount = 10000.00, currentAmount = 4500.00))

                // Default Preferences
                database.userPreferencesDao().insertUserPreferences(
                    UserPreferencesEntity(
                        pinCode = "1234",
                        isPinEnabled = true,
                        googleCalendarSyncEnabled = true,
                        darkThemeMode = false,
                        isRegistered = false,
                        userName = "Maria Santos"
                    )
                )

                // Default Partner Vault
                database.partnerVaultDao().insertPartnerVault(
                    PartnerVaultEntity(
                        vaultName = "Santos-Cruz Household Vault",
                        inviteCode = "GSAVE-PH99",
                        partnerName = "Juan Cruz",
                        isConnected = true
                    )
                )

                // Initial Transactions
                database.transactionDao().insertTransaction(
                    TransactionEntity(walletId = 1, title = "Monthly Salary", amount = 35000.0, type = "Income", category = "Salary")
                )
                database.transactionDao().insertTransaction(
                    TransactionEntity(walletId = 1, title = "Electric Bill (Meralco)", amount = 2800.0, type = "Bill", category = "Utilities", isPaid = true)
                )
                database.transactionDao().insertTransaction(
                    TransactionEntity(walletId = 2, title = "Grocery Run (SM)", amount = 4500.0, type = "Withdrawal", category = "Food")
                )
            }
        }
    }
}
