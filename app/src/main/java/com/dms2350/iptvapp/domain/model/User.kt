package com.dms2350.iptvapp.domain.model

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val subscriptionType: String = "free",
    val subscriptionExpiresAt: String?,
    val isActive: Boolean = true
)
