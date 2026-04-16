package com.katsufit.models.nutritionist

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// ============================
// TABELA PATIENT_MEAL_PLANS
// Plano alimentar vinculado a um paciente (cópia independente do template)
// ============================
object PatientMealPlans : Table("patient_meal_plans") {
    val id = integer("id").autoIncrement()
    val patientId = varchar("patient_id", 36)        // UUID do paciente
    val nutritionistId = varchar("nutritionist_id", 36) // UUID do nutricionista
    val sourceTemplateId = integer("source_template_id").nullable() // Template de origem (referência)
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
// TABELA PATIENT_MEAL_PLAN_MEALS
// Refeições do plano do paciente
// ============================
object PatientMealPlanMeals : Table("patient_meal_plan_meals") {
    val id = integer("id").autoIncrement()
    val patientPlanId = integer("patient_plan_id").references(PatientMealPlans.id)
    val dayOfWeek = integer("day_of_week").default(1) // 1=Seg, 2=Ter, 3=Qua, 4=Qui, 5=Sex, 6=Sab, 7=Dom
    val name = varchar("name", 100)
    val time = varchar("time", 10).nullable()
    val orderIndex = integer("order_index").default(0)
    val foods = text("foods").default("[]")

    val totalCalories = double("total_calories").nullable()
    val totalProtein = double("total_protein").nullable()
    val totalCarbs = double("total_carbs").nullable()
    val totalFat = double("total_fat").nullable()
    val totalFiber = double("total_fiber").nullable()

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}

// ============================
// DTOs
// ============================

@Serializable
data class PatientMealPlanDTO(
    val id: Int? = null,
    val patientId: String? = null,
    val nutritionistId: String? = null,
    val sourceTemplateId: Int? = null,
    val name: String,
    val description: String? = null,
    val totalCalories: Double? = null,
    val totalProtein: Double? = null,
    val totalCarbs: Double? = null,
    val totalFat: Double? = null,
    val totalFiber: Double? = null,
    val isActive: Boolean = true,
    val meals: List<PatientMealPlanMealDTO>? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class PatientMealPlanMealDTO(
    val id: Int? = null,
    val patientPlanId: Int? = null,
    val dayOfWeek: Int = 1,
    val name: String,
    val time: String? = null,
    val orderIndex: Int = 0,
    val foods: List<MealFoodItem> = emptyList(),
    val totalCalories: Double? = null,
    val totalProtein: Double? = null,
    val totalCarbs: Double? = null,
    val totalFat: Double? = null,
    val totalFiber: Double? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class AssignMealPlanRequest(
    val templateId: Int,
    val days: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7) // padrão: todos os dias
)

@Serializable
data class AssignMealPlanResponse(
    val id: Int,
    val message: String
)

@Serializable
data class AddPatientMealRequest(
    val dayOfWeek: Int,          // 1=Seg, 2=Ter, 3=Qua, 4=Qui, 5=Sex, 6=Sab, 7=Dom
    val name: String,            // Nome da refeição (ex: "Café da Manhã")
    val time: String? = null,    // Horário (ex: "08:00")
    val foods: List<MealFoodItem>? = null  // Alimentos (opcional na criação)
)
