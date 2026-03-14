package com.katsufit.models.nutritionist

import com.katsufit.models.shared.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// ============================
// TABELA MEAL_PLAN_TEMPLATES
// ============================
object MealPlanTemplates : Table("meal_plan_templates") {
    val id = integer("id").autoIncrement()
    val nutritionistId = varchar("nutritionist_id", 36)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    
    val totalCalories = double("total_calories").nullable()
    val totalProtein = double("total_protein").nullable()
    val totalCarbs = double("total_carbs").nullable()
    val totalFat = double("total_fat").nullable()
    val totalFiber = double("total_fiber").nullable()
    
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    
    override val primaryKey = PrimaryKey(id)
}

// ============================
// DTOs
// ============================

@Serializable
data class MealPlanTemplateDTO(
    val id: Int? = null,
    val nutritionistId: String? = null,
    val name: String,
    val description: String? = null,
    val totalCalories: Double? = null,
    val totalProtein: Double? = null,
    val totalCarbs: Double? = null,
    val totalFat: Double? = null,
    val totalFiber: Double? = null,
    val isActive: Boolean = true,
    val meals: List<MealPlanMealDTO>? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class CreateMealPlanTemplateRequest(
    val name: String,
    val description: String? = null
)

@Serializable
data class MealPlanTemplateResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val totalCalories: Double?,
    val totalProtein: Double?,
    val totalCarbs: Double?,
    val totalFat: Double?,
    val totalFiber: Double?,
    val isActive: Boolean,
    val mealsCount: Int = 0,
    val createdAt: String,
    val updatedAt: String
)

// ============================
// RESPOSTAS SIMPLES
// ============================

@Serializable
data class CreateMealPlanResponse(
    val id: Int,
    val message: String
)

@Serializable
data class CreateMealResponse(
    val id: Int,
    val message: String
)

@Serializable
data class SimpleMessageResponse(
    val message: String
)
