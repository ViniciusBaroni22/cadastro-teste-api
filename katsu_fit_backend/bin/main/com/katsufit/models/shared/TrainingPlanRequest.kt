package com.katsufit.models.shared

import kotlinx.serialization.Serializable

@Serializable
data class TrainingPlanRequest(
    val clientId: String,
    val name: String,
    val description: String?
)
