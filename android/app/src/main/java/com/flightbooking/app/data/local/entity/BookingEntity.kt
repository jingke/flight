package com.flightbooking.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "flight_id") val flightId: Int,
    @ColumnInfo(name = "flight_number") val flightNumber: String,
    val status: String,
    @ColumnInfo(name = "total_price") val totalPrice: Double,
    @ColumnInfo(name = "payment_status") val paymentStatus: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis()
)
