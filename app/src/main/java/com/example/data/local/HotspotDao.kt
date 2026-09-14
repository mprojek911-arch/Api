package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HotspotDao {
    @Query("SELECT * FROM fire_hotspots ORDER BY observationEpochMs DESC, acqDate DESC, acqTime DESC")
    fun getAllHotspotsFlow(): Flow<List<FireHotspotEntity>>

    @Query("SELECT * FROM fire_hotspots ORDER BY observationEpochMs DESC, acqDate DESC, acqTime DESC")
    suspend fun getAllHotspots(): List<FireHotspotEntity>

    @Query("SELECT * FROM fire_hotspots WHERE observationEpochMs >= :cutoffEpochMs ORDER BY observationEpochMs DESC")
    suspend fun getHotspotsSince(cutoffEpochMs: Long): List<FireHotspotEntity>

    @Query("SELECT * FROM fire_hotspots WHERE province = :province ORDER BY observationEpochMs DESC")
    fun getHotspotsByProvince(province: String): Flow<List<FireHotspotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hotspots: List<FireHotspotEntity>)

    @Query("DELETE FROM fire_hotspots")
    suspend fun clearAll()

    @Query("DELETE FROM fire_hotspots WHERE observationEpochMs < :cutoffEpochMs")
    suspend fun deleteOlderThan(cutoffEpochMs: Long)

    @Query("SELECT COUNT(*) FROM fire_hotspots")
    suspend fun getCount(): Int

    @Query("SELECT MAX(retrievedAt) FROM fire_hotspots")
    suspend fun getLastRetrievedAt(): Long?
}
