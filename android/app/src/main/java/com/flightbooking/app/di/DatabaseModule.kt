package com.flightbooking.app.di

import android.content.Context
import androidx.room.Room
import com.flightbooking.app.data.local.FlightBookingDatabase
import com.flightbooking.app.data.local.dao.AirportDao
import com.flightbooking.app.data.local.dao.BookingDao
import com.flightbooking.app.data.local.dao.FlightDao
import com.flightbooking.app.data.local.dao.NotificationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FlightBookingDatabase =
        Room.databaseBuilder(
            context,
            FlightBookingDatabase::class.java,
            FlightBookingDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFlightDao(database: FlightBookingDatabase): FlightDao = database.flightDao()

    @Provides
    fun provideBookingDao(database: FlightBookingDatabase): BookingDao = database.bookingDao()

    @Provides
    fun provideAirportDao(database: FlightBookingDatabase): AirportDao = database.airportDao()

    @Provides
    fun provideNotificationDao(database: FlightBookingDatabase): NotificationDao = database.notificationDao()
}
