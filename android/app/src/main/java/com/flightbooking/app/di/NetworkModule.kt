package com.flightbooking.app.di

import com.flightbooking.app.BuildConfig
import com.flightbooking.app.data.local.TokenManager
import com.flightbooking.app.data.remote.api.AirportApi
import com.flightbooking.app.data.remote.api.AuthApi
import com.flightbooking.app.data.remote.api.BookingApi
import com.flightbooking.app.data.remote.api.ComplaintApi
import com.flightbooking.app.data.remote.api.FlightApi
import com.flightbooking.app.data.remote.api.LoyaltyApi
import com.flightbooking.app.data.remote.api.ModificationApi
import com.flightbooking.app.data.remote.api.NotificationApi
import com.flightbooking.app.data.remote.interceptor.AuthInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .create()

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideFlightApi(retrofit: Retrofit): FlightApi = retrofit.create(FlightApi::class.java)

    @Provides
    @Singleton
    fun provideBookingApi(retrofit: Retrofit): BookingApi = retrofit.create(BookingApi::class.java)

    @Provides
    @Singleton
    fun provideComplaintApi(retrofit: Retrofit): ComplaintApi = retrofit.create(ComplaintApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi = retrofit.create(NotificationApi::class.java)

    @Provides
    @Singleton
    fun provideLoyaltyApi(retrofit: Retrofit): LoyaltyApi = retrofit.create(LoyaltyApi::class.java)

    @Provides
    @Singleton
    fun provideAirportApi(retrofit: Retrofit): AirportApi = retrofit.create(AirportApi::class.java)

    @Provides
    @Singleton
    fun provideModificationApi(retrofit: Retrofit): ModificationApi =
        retrofit.create(ModificationApi::class.java)
}
