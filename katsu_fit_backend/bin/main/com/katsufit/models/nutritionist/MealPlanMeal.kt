package com.katsufit.models.nutritionist

import com.katsufit.models.shared.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// ============================
// TABELA MEAL_PLAN_MEALS
// Refeições de cada template (café, almoço, jantar, etc)
// ============================
object MealPlanMeals : Table("meal_plan_meals") {
    val id = integer("id").autoIncrement()
    val templateId = integer("template_id").references(MealPlanTemplates.id) // Qual plano essa refeição pertence
    val name = varchar("name", 100) // Ex: "Café da Manhã", "Almoço", "Jantar"
    val time = varchar("time", 10).nullable() // Ex: "08:00", "12:30"
    val orderIndex = integer("order_index").default(0) // Ordem de exibição (1, 2, 3...)
    
    // Alimentos da refeição em JSON
    // Formato: [{"foodId": 31, "foodName": "Arroz", "quantity": 100, "unit": "g", "calories": 123, "protein": 2.5, ...}, ...]
    val foods = text("foods").default("[]")
    
    // Totais calculados dessa refeição
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
// DTOs - Data Transfer Objects
// ============================

@Serializable
data class MealPlanMealDTO(
    val id: Int? = null,
    val templateId: Int? = null,
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
data class MealFoodItem(
    val foodId: Int,
    val foodName: String,
    val quantity: Double, // Quantidade em gramas
    val unit: String = "g",
    // Valores calculados baseados na quantidade
    val calories: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val fiber: Double? = null
)

@Serializable
data class CreateMealRequest(
    val name: String,
    val time: String? = null,
    val orderIndex: Int = 0
)

@Serializable
data class AddFoodToMealRequest(
    val mealId: Int,
    val foodId: Int,
    val quantity: Double, // Quantidade em gramas
    val unit: String = "g"
)

@Serializable
data class UpdateMealFoodsRequest(
    val foods: List<MealFoodItem>
)
