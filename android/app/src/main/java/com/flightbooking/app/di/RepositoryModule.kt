package com.flightbooking.app.di

import com.flightbooking.app.data.repository.AirportRepositoryImpl
import com.flightbooking.app.data.repository.AuthRepositoryImpl
import com.flightbooking.app.data.repository.BookingRepositoryImpl
import com.flightbooking.app.data.repository.ComplaintRepositoryImpl
import com.flightbooking.app.data.repository.FlightRepositoryImpl
import com.flightbooking.app.data.repository.LoyaltyRepositoryImpl
import com.flightbooking.app.data.repository.ModificationRepositoryImpl
import com.flightbooking.app.data.repository.NotificationRepositoryImpl
import com.flightbooking.app.domain.repository.AirportRepository
import com.flightbooking.app.domain.repository.AuthRepository
import com.flightbooking.app.domain.repository.BookingRepository
import com.flightbooking.app.domain.repository.ComplaintRepository
import com.flightbooking.app.domain.repository.FlightRepository
import com.flightbooking.app.domain.repository.LoyaltyRepository
import com.flightbooking.app.domain.repository.ModificationRepository
import com.flightbooking.app.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindFlightRepository(impl: FlightRepositoryImpl): FlightRepository

    @Binds
    @Singleton
    abstract fun bindBookingRepository(impl: BookingRepositoryImpl): BookingRepository

    @Binds
    @Singleton
    abstract fun bindComplaintRepository(impl: ComplaintRepositoryImpl): ComplaintRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindLoyaltyRepository(impl: LoyaltyRepositoryImpl): LoyaltyRepository

    @Binds
    @Singleton
    abstract fun bindAirportRepository(impl: AirportRepositoryImpl): AirportRepository

    @Binds
    @Singleton
    abstract fun bindModificationRepository(impl: ModificationRepositoryImpl): ModificationRepository
}
