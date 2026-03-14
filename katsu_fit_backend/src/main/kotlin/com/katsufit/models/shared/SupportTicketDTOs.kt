package com.katsufit.models.shared

import kotlinx.serialization.Serializable

@Serializable
data class CreateTicketRequest(
    val category: String,
    val subject: String,
    val description: String,
    val priority: String = "MEDIA"
)

@Serializable
data class TicketResponse(
    val id: Int,
    val professionalId: String,
    val category: String,
    val subject: String,
    val description: String,
    val status: String,
    val priority: String,
    val createdAt: String, // ISO 8601
    val updatedAt: String,
    val resolvedAt: String?
)

@Serializable
data class TicketListItem(
    val id: Int,
    val subject: String,
    val status: String,
    val createdAt: String
)
