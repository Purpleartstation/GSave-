package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.BucketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BucketDao {
    @Query("SELECT * FROM buckets")
    fun getAllBuckets(): Flow<List<BucketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBucket(bucket: BucketEntity)

    @Update
    suspend fun updateBucket(bucket: BucketEntity)

    @Query("DELETE FROM buckets")
    suspend fun deleteAllBuckets()
}
