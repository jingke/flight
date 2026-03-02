package com.flightbooking.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flightbooking.app.data.local.entity.FlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {

    @Query("SELECT * FROM flights ORDER BY departure_time ASC")
    fun observeAll(): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE id = :flightId")
    suspend fun getById(flightId: Int): FlightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(flights: List<FlightEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(flight: FlightEntity)

    @Query("DELETE FROM flights")
    suspend fun deleteAll()

    @Query("DELETE FROM flights WHERE cached_at < :timestamp")
    suspend fun deleteStale(timestamp: Long)
}
