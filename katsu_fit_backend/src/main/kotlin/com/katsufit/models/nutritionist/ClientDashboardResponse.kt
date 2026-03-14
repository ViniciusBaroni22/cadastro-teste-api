package com.katsufit.models.nutritionist

import kotlinx.serialization.Serializable
import com.katsufit.models.shared.ProgressResponse
import com.katsufit.models.shared.TrainingPlanResponse

@Serializable
data class ClientDashboardResponse(
    val nutritionPlans: List<NutritionPlanResponse>,
    val trainingPlans: List<TrainingPlanResponse>,
    val progress: List<ProgressResponse> // CORRETO!
)
