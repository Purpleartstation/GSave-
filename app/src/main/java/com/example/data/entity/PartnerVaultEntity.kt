package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "partner_vault")
data class PartnerVaultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val vaultName: String,
    val inviteCode: String,
    val partnerName: String,
    val isConnected: Boolean
)
