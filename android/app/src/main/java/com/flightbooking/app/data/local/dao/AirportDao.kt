package com.flightbooking.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flightbooking.app.data.local.entity.AirportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AirportDao {

    @Query("SELECT * FROM airports ORDER BY code ASC")
    fun observeAll(): Flow<List<AirportEntity>>

    @Query("SELECT * FROM airports WHERE id = :airportId")
    suspend fun getById(airportId: Int): AirportEntity?

    @Query("SELECT * FROM airports WHERE code = :code")
    suspend fun getByCode(code: String): AirportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(airports: List<AirportEntity>)

    @Query("DELETE FROM airports")
    suspend fun deleteAll()
}
