package com.flightbooking.app.domain.model

data class Notification(
    val id: Int,
    val userId: Int,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: String
)
