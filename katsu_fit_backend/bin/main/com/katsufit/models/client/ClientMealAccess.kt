package com.katsufit.models.client

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import com.katsufit.models.shared.Users
import com.katsufit.models.nutritionist.PatientMealPlans

/**
 * Tabela de acesso do cliente aos planos alimentares atribuídos
 * Controla quais dietas o cliente pode visualizar no app
 */
object ClientMealAccess : UUIDTable("client_meal_access") {
    val clientId = reference("client_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val patientMealPlanId = integer("patient_meal_plan_id").references(PatientMealPlans.id, onDelete = ReferenceOption.CASCADE)
    val nutritionistId = reference("nutritionist_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val accessGrantedAt = timestamp("access_granted_at").defaultExpression(CurrentTimestamp)
    val validUntil = timestamp("valid_until").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

@Serializable
data class ClientMealAccessDTO(
    val id: String,
    val clientId: String,
    val patientMealPlanId: Int,
    val nutritionistId: String,
    val planName: String? = null,
    val accessGrantedAt: String,
    val validUntil: String? = null,
    val isActive: Boolean
)

@Serializable
data class ClientMealPlanResponse(
    val id: Int,
    val name: String,
    val description: String? = null,
    val nutritionistName: String,
    val totalDays: Int,
    val meals: List<ClientMealResponse>,
    val progress: MealPlanProgress? = null
)

@Serializable
data class ClientMealResponse(
    val id: Int,
    val name: String,
    val dayOfWeek: Int,
    val time: String,
    val foods: List<ClientMealFoodResponse>,
    val totalCalories: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double
)

@Serializable
data class ClientMealFoodResponse(
    val id: Int,
    val foodName: String,
    val quantity: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

@Serializable
data class MealPlanProgress(
    val totalMeals: Int,
    val completedMeals: Int,
    val percentage: Int
)

@Serializable
data class MealCompletionRequest(
    val mealId: Int,
    val completed: Boolean,
    val date: String // ISO date
)
