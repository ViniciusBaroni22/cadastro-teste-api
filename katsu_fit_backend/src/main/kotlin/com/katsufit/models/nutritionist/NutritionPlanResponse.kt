package com.katsufit.models.nutritionist

import com.katsufit.models.shared.*
import kotlinx.serialization.Serializable

@Serializable
data class NutritionPlanResponse(
    val id: String,
    val professionalId: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val description: String?,
    val createdAt: String
)
