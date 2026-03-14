package com.katsufit.models.nutritionist

import com.katsufit.models.shared.*
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlinx.datetime.LocalDate

@Serializable
data class NutritionPlanRequest(
    val clientId: String,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val description: String?
)
