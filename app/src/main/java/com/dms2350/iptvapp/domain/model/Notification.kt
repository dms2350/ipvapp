package com.dms2350.iptvapp.domain.model

data class Notification(
    val id: Int,
    val title: String,
    val message: String,
    val displayDuration: Int, // Duración en segundos
    val isBlocked: Boolean, // true = bloqueado (naranja), false = normal (azul)
    val isActive: Boolean,
    val createdAt: String?
)


