package com.flightbooking.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.flightbooking.app.data.local.dao.AirportDao
import com.flightbooking.app.data.local.dao.BookingDao
import com.flightbooking.app.data.local.dao.FlightDao
import com.flightbooking.app.data.local.dao.NotificationDao
import com.flightbooking.app.data.local.entity.AirportEntity
import com.flightbooking.app.data.local.entity.BookingEntity
import com.flightbooking.app.data.local.entity.FlightEntity
import com.flightbooking.app.data.local.entity.NotificationEntity

@Database(
    entities = [
        FlightEntity::class,
        BookingEntity::class,
        AirportEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FlightBookingDatabase : RoomDatabase() {

    abstract fun flightDao(): FlightDao
    abstract fun bookingDao(): BookingDao
    abstract fun airportDao(): AirportDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val DATABASE_NAME = "flight_booking_db"
    }
}
