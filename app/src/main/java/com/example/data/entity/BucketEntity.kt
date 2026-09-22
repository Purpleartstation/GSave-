package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buckets")
data class BucketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String, // "Bills & Needs", "Savings", "Emergency Funds", "Wants"
    val percentage: Int, // e.g. 50, 30, 10, 10
    val targetAmount: Double,
    val currentAmount: Double
)
