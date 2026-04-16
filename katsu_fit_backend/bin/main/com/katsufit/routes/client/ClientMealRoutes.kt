package com.katsufit.routes.client

import com.katsufit.models.client.*
import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.Users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.UUID

fun Route.clientMealRoutes() {
    
    authenticate("auth-jwt") {
        
        // ============================================
        // CLIENTE: LISTAR PLANOS ALIMENTARES ACESSÍVEIS
        // ============================================
        get("/api/client/meal-plans") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val clientId = principal!!.payload.getClaim("id").asString()
                val clientUuid = UUID.fromString(clientId)
                
                val mealPlans = transaction {
                    ClientMealAccess
                        .join(PatientMealPlans, JoinType.INNER, ClientMealAccess.patientMealPlanId, PatientMealPlans.id)
                        .join(Users, JoinType.INNER, ClientMealAccess.nutritionistId, Users.id)
                        .select { 
                            (ClientMealAccess.clientId eq clientUuid) and
                            (ClientMealAccess.isActive eq true)
                        }
                        .orderBy(ClientMealAccess.accessGrantedAt, SortOrder.DESC)
                        .map { row ->
                            mapOf(
                                "id" to row[PatientMealPlans.id],
                                "name" to row[PatientMealPlans.name],
                                "description" to row[PatientMealPlans.description],
                                "nutritionistName" to row[Users.name],
                                "accessGrantedAt" to row[ClientMealAccess.accessGrantedAt].toString(),
                                "validUntil" to row[ClientMealAccess.validUntil]?.toString()
                            )
                        }
                }
                
                call.respond(HttpStatusCode.OK, mealPlans)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Erro ao buscar planos: ${e.message}")
            }
        }
        
        // ============================================
        // CLIENTE: DETALHE COMPLETO DO PLANO
        // ============================================
        get("/api/client/meal-plans/{id}") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val clientId = principal!!.payload.getClaim("id").asString()
                val clientUuid = UUID.fromString(clientId)
                val planId = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "ID inválido")
                
                // Verificar se cliente tem acesso
                val hasAccess = transaction {
                    ClientMealAccess.select {
                        (ClientMealAccess.clientId eq clientUuid) and
                        (ClientMealAccess.patientMealPlanId eq planId) and
                        (ClientMealAccess.isActive eq true)
                    }.count() > 0
                }
                
                if (!hasAccess) {
                    call.respond(HttpStatusCode.Forbidden, "Você não tem acesso a este plano")
                    return@get
                }
                
                val planDetail = transaction {
                    // Buscar dados do plano
                    val plan = PatientMealPlans
                        .join(Users, JoinType.INNER, PatientMealPlans.nutritionistId, Users.id)
                        .select { PatientMealPlans.id eq planId }
                        .singleOrNull()
                        ?: return@transaction null
                    
                    val nutritionistName = plan[Users.name]
                    
                    // Buscar refeições do plano
                    val meals = PatientMealPlanMeals
                        .select { PatientMealPlanMeals.patientPlanId eq planId }
                        .orderBy(PatientMealPlanMeals.dayOfWeek to SortOrder.ASC, PatientMealPlanMeals.time to SortOrder.ASC)
                        .map { mealRow ->
                            // Parsear JSON dos alimentos
                            val foodsJson = mealRow[PatientMealPlanMeals.foods]
                            val foods: List<MealFoodItem> = try {
                                Json.decodeFromString<List<MealFoodItem>>(foodsJson)
                            } catch (e: Exception) {
                                emptyList()
                            }
                            
                            val totalCalories = foods.sumOf { it.calories ?: 0.0 }
                            val totalProtein = foods.sumOf { it.protein ?: 0.0 }
                            val totalCarbs = foods.sumOf { it.carbs ?: 0.0 }
                            val totalFat = foods.sumOf { it.fat ?: 0.0 }
                            
                            mapOf(
                                "id" to mealRow[PatientMealPlanMeals.id],
                                "name" to mealRow[PatientMealPlanMeals.name],
                                "dayOfWeek" to mealRow[PatientMealPlanMeals.dayOfWeek],
                                "time" to (mealRow[PatientMealPlanMeals.time] ?: "--:--"),
                                "foods" to foods.map { food ->
                                    mapOf(
                                        "id" to food.foodId,
                                        "foodName" to food.foodName,
                                        "quantity" to "${food.quantity.toInt()}${food.unit}",
                                        "calories" to (food.calories ?: 0.0),
                                        "protein" to (food.protein ?: 0.0),
                                        "carbs" to (food.carbs ?: 0.0),
                                        "fat" to (food.fat ?: 0.0)
                                    )
                                },
                                "totalCalories" to totalCalories,
                                "totalProtein" to totalProtein,
                                "totalCarbs" to totalCarbs,
                                "totalFat" to totalFat
                            )
                        }
                    
                    mapOf(
                        "id" to planId,
                        "name" to plan[PatientMealPlans.name],
                        "description" to plan[PatientMealPlans.description],
                        "nutritionistName" to nutritionistName,
                        "meals" to meals,
                        "progress" to null
                    )
                }
                
                if (planDetail != null) {
                    call.respond(HttpStatusCode.OK, planDetail)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Plano não encontrado")
                }
                
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Erro ao buscar plano: ${e.message}")
            }
        }
        
        // ============================================
        // CLIENTE: REFEIÇÕES DO DIA ATUAL
        // ============================================
        get("/api/client/meal-plans/{id}/today") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val clientId = principal!!.payload.getClaim("id").asString()
                val clientUuid = UUID.fromString(clientId)
                val planId = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "ID inválido")
                
                // Verificar acesso
                val hasAccess = transaction {
                    ClientMealAccess.select {
                        (ClientMealAccess.clientId eq clientUuid) and
                        (ClientMealAccess.patientMealPlanId eq planId) and
                        (ClientMealAccess.isActive eq true)
                    }.count() > 0
                }
                
                if (!hasAccess) {
                    call.respond(HttpStatusCode.Forbidden, "Sem acesso")
                    return@get
                }
                
                // Dia da semana atual (1=Segunda, 7=Domingo)
                val today = LocalDate.now().dayOfWeek.value
                
                val todayMeals = transaction {
                    PatientMealPlanMeals
                        .select { 
                            (PatientMealPlanMeals.patientPlanId eq planId) and
                            (PatientMealPlanMeals.dayOfWeek eq today)
                        }
                        .orderBy(PatientMealPlanMeals.time)
                        .map { mealRow ->
                            val foodsJson = mealRow[PatientMealPlanMeals.foods]
                            val foods: List<MealFoodItem> = try {
                                Json.decodeFromString<List<MealFoodItem>>(foodsJson)
                            } catch (e: Exception) {
                                emptyList()
                            }
                            
                            mapOf(
                                "id" to mealRow[PatientMealPlanMeals.id],
                                "name" to mealRow[PatientMealPlanMeals.name],
                                "dayOfWeek" to today,
                                "time" to (mealRow[PatientMealPlanMeals.time] ?: "--:--"),
                                "foods" to foods.map { food ->
                                    mapOf(
                                        "id" to food.foodId,
                                        "foodName" to food.foodName,
                                        "quantity" to "${food.quantity.toInt()}${food.unit}",
                                        "calories" to (food.calories ?: 0.0),
                                        "protein" to (food.protein ?: 0.0),
                                        "carbs" to (food.carbs ?: 0.0),
                                        "fat" to (food.fat ?: 0.0)
                                    )
                                },
                                "totalCalories" to foods.sumOf { it.calories ?: 0.0 },
                                "totalProtein" to foods.sumOf { it.protein ?: 0.0 },
                                "totalCarbs" to foods.sumOf { it.carbs ?: 0.0 },
                                "totalFat" to foods.sumOf { it.fat ?: 0.0 }
                            )
                        }
                }
                
                // Calcular totais do dia
                val totalCalories = todayMeals.sumOf { (it["totalCalories"] as? Double) ?: 0.0 }
                val totalProtein = todayMeals.sumOf { (it["totalProtein"] as? Double) ?: 0.0 }
                val totalCarbs = todayMeals.sumOf { (it["totalCarbs"] as? Double) ?: 0.0 }
                val totalFat = todayMeals.sumOf { (it["totalFat"] as? Double) ?: 0.0 }
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "meals" to todayMeals,
                    "totalCalories" to totalCalories,
                    "totalProtein" to totalProtein,
                    "totalCarbs" to totalCarbs,
                    "totalFat" to totalFat
                ))
                
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Erro: ${e.message}")
            }
        }
    }
}
