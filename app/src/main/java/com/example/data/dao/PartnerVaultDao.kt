package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.PartnerVaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerVaultDao {
    @Query("SELECT * FROM partner_vault ORDER BY id DESC LIMIT 1")
    fun getPartnerVault(): Flow<PartnerVaultEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartnerVault(vault: PartnerVaultEntity)

    @Update
    suspend fun updatePartnerVault(vault: PartnerVaultEntity)

    @Query("DELETE FROM partner_vault")
    suspend fun deleteAllPartnerVaults()
}
