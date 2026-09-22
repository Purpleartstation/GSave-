package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val walletId: Long,
    val title: String,
    val amount: Double,
    val type: String, // "Income", "Withdrawal", "Bill", "Loan"
    val category: String, // "Salary", "Food", "Utilities", "Savings", etc.
    val date: Long = System.currentTimeMillis(),
    val recurringType: String = "None", // "None", "Daily", "Weekly", "Monthly"
    val recurringCount: Int = 0,
    val isPaid: Boolean = true
)
