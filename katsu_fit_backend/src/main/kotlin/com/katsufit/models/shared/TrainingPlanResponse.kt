package com.katsufit.models.shared

import kotlinx.serialization.Serializable

@Serializable
data class TrainingPlanResponse(
    val id: String,
    val professionalId: String,
    val name: String,
    val description: String?,
    val createdAt: String
)
