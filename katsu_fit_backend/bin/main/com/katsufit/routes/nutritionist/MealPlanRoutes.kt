package com.katsufit.routes.nutritionist

import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.mealPlanRouting() {
    
    authenticate("auth-jwt") {
        
        // ============================
        // TEMPLATES DE PLANOS
        // ============================
        
        // LISTAR TODOS OS TEMPLATES DO NUTRICIONISTA
        get("/api/meal-plans") {
            val principal = call.principal<JWTPrincipal>()
            val nutritionistId = principal?.payload?.getClaim("id")?.asString()
            
            if (nutritionistId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                return@get
            }
            
            val templates = transaction {
                MealPlanTemplates.select { MealPlanTemplates.nutritionistId eq nutritionistId }
                    .orderBy(MealPlanTemplates.createdAt, SortOrder.DESC)
                    .map { row ->
                        val templateId = row[MealPlanTemplates.id]
                        val mealsCount = MealPlanMeals.select { MealPlanMeals.templateId eq templateId }.count()
                        
                        MealPlanTemplateResponse(
                            id = row[MealPlanTemplates.id],
                            name = row[MealPlanTemplates.name],
                            description = row[MealPlanTemplates.description],
                            totalCalories = row[MealPlanTemplates.totalCalories],
                            totalProtein = row[MealPlanTemplates.totalProtein],
                            totalCarbs = row[MealPlanTemplates.totalCarbs],
                            totalFat = row[MealPlanTemplates.totalFat],
                            totalFiber = row[MealPlanTemplates.totalFiber],
                            isActive = row[MealPlanTemplates.isActive],
                            mealsCount = mealsCount.toInt(),
                            createdAt = row[MealPlanTemplates.createdAt].toString(),
                            updatedAt = row[MealPlanTemplates.updatedAt].toString()
                        )
                    }
            }
            
            call.respond(templates)
        }
        
        // CRIAR NOVO TEMPLATE
        post("/api/meal-plans") {
            val principal = call.principal<JWTPrincipal>()
            val nutritionistId = principal?.payload?.getClaim("id")?.asString()
            
            if (nutritionistId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                return@post
            }
            
            val request = call.receive<CreateMealPlanTemplateRequest>()
            
            val templateId = transaction {
                MealPlanTemplates.insert {
                    it[MealPlanTemplates.nutritionistId] = nutritionistId
                    it[name] = request.name
                    it[description] = request.description
                    it[totalCalories] = 0.0
                    it[totalProtein] = 0.0
                    it[totalCarbs] = 0.0
                    it[totalFat] = 0.0
                    it[totalFiber] = 0.0
                    it[isActive] = true
                }[MealPlanTemplates.id]
            }
            
            call.respond(HttpStatusCode.Created, CreateMealPlanResponse(
                id = templateId,
                message = "Plano criado com sucesso!"
            ))
        }
        
        // BUSCAR TEMPLATE POR ID (COM REFEIÇÕES)
        get("/api/meal-plans/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val nutritionistId = principal?.payload?.getClaim("id")?.asString()
            
            if (nutritionistId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                return@get
            }
            
            val templateId = call.parameters["id"]?.toIntOrNull()
            if (templateId == null) {
                call.respond(HttpStatusCode.BadRequest, "ID inválido")
                return@get
            }
            
            val result = transaction {
                val template = MealPlanTemplates.select { 
                    (MealPlanTemplates.id eq templateId) and 
                    (MealPlanTemplates.nutritionistId eq nutritionistId) 
                }.singleOrNull()
                
                if (template == null) return@transaction null
                
                val meals = MealPlanMeals.select { MealPlanMeals.templateId eq templateId }
                    .orderBy(MealPlanMeals.orderIndex, SortOrder.ASC)
                    .map { mealRow ->
                        val foodsJson = mealRow[MealPlanMeals.foods]
                        val foodsList = try {
                            Json.decodeFromString<List<MealFoodItem>>(foodsJson)
                        } catch (e: Exception) {
                            emptyList()
                        }
                        
                        MealPlanMealDTO(
                            id = mealRow[MealPlanMeals.id],
                            templateId = mealRow[MealPlanMeals.templateId],
                            name = mealRow[MealPlanMeals.name],
                            time = mealRow[MealPlanMeals.time],
                            orderIndex = mealRow[MealPlanMeals.orderIndex],
                            foods = foodsList,
                            totalCalories = mealRow[MealPlanMeals.totalCalories],
                            totalProtein = mealRow[MealPlanMeals.totalProtein],
                            totalCarbs = mealRow[MealPlanMeals.totalCarbs],
                            totalFat = mealRow[MealPlanMeals.totalFat],
                            totalFiber = mealRow[MealPlanMeals.totalFiber],
                            createdAt = mealRow[MealPlanMeals.createdAt].toString(),
                            updatedAt = mealRow[MealPlanMeals.updatedAt].toString()
                        )
                    }
                
                MealPlanTemplateDTO(
                    id = template[MealPlanTemplates.id],
                    nutritionistId = template[MealPlanTemplates.nutritionistId],
                    name = template[MealPlanTemplates.name],
                    description = template[MealPlanTemplates.description],
                    totalCalories = template[MealPlanTemplates.totalCalories],
                    totalProtein = template[MealPlanTemplates.totalProtein],
                    totalCarbs = template[MealPlanTemplates.totalCarbs],
                    totalFat = template[MealPlanTemplates.totalFat],
                    totalFiber = template[MealPlanTemplates.totalFiber],
                    isActive = template[MealPlanTemplates.isActive],
                    meals = meals,
                    createdAt = template[MealPlanTemplates.createdAt].toString(),
                    updatedAt = template[MealPlanTemplates.updatedAt].toString()
                )
            }
            
            if (result == null) {
                call.respond(HttpStatusCode.NotFound, "Plano não encontrado")
            } else {
                call.respond(result)
            }
        }
        
        // DELETAR TEMPLATE
        delete("/api/meal-plans/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val nutritionistId = principal?.payload?.getClaim("id")?.asString()
            
            if (nutritionistId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                return@delete
            }
            
            val templateId = call.parameters["id"]?.toIntOrNull()
            if (templateId == null) {
                call.respond(HttpStatusCode.BadRequest, "ID inválido")
                return@delete
            }
            
            val deleted = transaction {
                MealPlanMeals.deleteWhere { MealPlanMeals.templateId eq templateId }
                MealPlanTemplates.deleteWhere { 
                    (MealPlanTemplates.id eq templateId) and 
                    (MealPlanTemplates.nutritionistId eq nutritionistId) 
                }
            }
            
            if (deleted > 0) {
                call.respond(HttpStatusCode.OK, SimpleMessageResponse("Plano excluído com sucesso"))
            } else {
                call.respond(HttpStatusCode.NotFound, "Plano não encontrado")
            }
        }
        
        // ============================
        // REFEIÇÕES DO PLANO
        // ============================
        
        // ADICIONAR REFEIÇÃO AO PLANO
        post("/api/meal-plans/{id}/meals") {
            val principal = call.principal<JWTPrincipal>()
            val nutritionistId = principal?.payload?.getClaim("id")?.asString()
            
            if (nutritionistId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                return@post
            }
            
            val templateId = call.parameters["id"]?.toIntOrNull()
            if (templateId == null) {
                call.respond(HttpStatusCode.BadRequest, "ID do plano inválido")
                return@post
            }
            
            val request = call.receive<CreateMealRequest>()
            
            val mealId = transaction {
                val template = MealPlanTemplates.select { 
                    (MealPlanTemplates.id eq templateId) and 
                    (MealPlanTemplates.nutritionistId eq nutritionistId) 
                }.singleOrNull()
                
                if (template == null) return@transaction null
                
                MealPlanMeals.insert {
                    it[MealPlanMeals.templateId] = templateId
                    it[name] = request.name
                    it[time] = request.time
                    it[orderIndex] = request.orderIndex
                    it[foods] = "[]"
                    it[totalCalories] = 0.0
                    it[totalProtein] = 0.0
                    it[totalCarbs] = 0.0
                    it[totalFat] = 0.0
                    it[totalFiber] = 0.0
                }[MealPlanMeals.id]
            }
            
            if (mealId == null) {
                call.respond(HttpStatusCode.NotFound, "Plano não encontrado")
            } else {
                call.respond(HttpStatusCode.Created, CreateMealResponse(
                    id = mealId,
                    message = "Refeição adicionada com sucesso!"
                ))
            }
        }
        
        // ATUALIZAR ALIMENTOS DE UMA REFEIÇÃO
        put("/api/meal-plans/meals/{mealId}/foods") {
            val principal = call.principal<JWTPrincipal>()
            val nutritionistId = principal?.payload?.getClaim("id")?.asString()
            
            if (nutritionistId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                return@put
            }
            
            val mealId = call.parameters["mealId"]?.toIntOrNull()
            if (mealId == null) {
                call.respond(HttpStatusCode.BadRequest, "ID da refeição inválido")
                return@put
            }
            
            val request = call.receive<UpdateMealFoodsRequest>()
            
            val updated = transaction {
                val meal = MealPlanMeals.select { MealPlanMeals.id eq mealId }.singleOrNull()
                    ?: return@transaction false
                
                val templateId = meal[MealPlanMeals.templateId]
                
                val template = MealPlanTemplates.select { 
                    (MealPlanTemplates.id eq templateId) and 
                    (MealPlanTemplates.nutritionistId eq nutritionistId) 
                }.singleOrNull() ?: return@transaction false
                
                val totalCal = request.foods.sumOf { it.calories ?: 0.0 }
                val totalProt = request.foods.sumOf { it.protein ?: 0.0 }
                val totalCarb = request.foods.sumOf { it.carbs ?: 0.0 }
                val totalFatVal = request.foods.sumOf { it.fat ?: 0.0 }
                val totalFib = request.foods.sumOf { it.fiber ?: 0.0 }
                
                MealPlanMeals.update({ MealPlanMeals.id eq mealId }) {
                    it[foods] = Json.encodeToString(request.foods)
                    it[totalCalories] = totalCal
                    it[totalProtein] = totalProt
                    it[totalCarbs] = totalCarb
                    it[totalFat] = totalFatVal
                    it[totalFiber] = totalFib
                }
                
                recalculateTemplateTotals(templateId)
                
                true
            }
            
            if (updated) {
                call.respond(HttpStatusCode.OK, SimpleMessageResponse("Alimentos atualizados com sucesso!"))
            } else {
                call.respond(HttpStatusCode.NotFound, "Refeição não encontrada")
            }
        }
        
        // DELETAR REFEIÇÃO
        delete("/api/meal-plans/meals/{mealId}") {
            val principal = call.principal<JWTPrincipal>()
            val nutritionistId = principal?.payload?.getClaim("id")?.asString()
            
            if (nutritionistId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                return@delete
            }
            
            val mealId = call.parameters["mealId"]?.toIntOrNull()
            if (mealId == null) {
                call.respond(HttpStatusCode.BadRequest, "ID da refeição inválido")
                return@delete
            }
            
            val deleted = transaction {
                val meal = MealPlanMeals.select { MealPlanMeals.id eq mealId }.singleOrNull()
                    ?: return@transaction false
                
                val templateId = meal[MealPlanMeals.templateId]
                
                val template = MealPlanTemplates.select { 
                    (MealPlanTemplates.id eq templateId) and 
                    (MealPlanTemplates.nutritionistId eq nutritionistId) 
                }.singleOrNull() ?: return@transaction false
                
                MealPlanMeals.deleteWhere { MealPlanMeals.id eq mealId }
                
                recalculateTemplateTotals(templateId)
                
                true
            }
            
            if (deleted) {
                call.respond(HttpStatusCode.OK, SimpleMessageResponse("Refeição excluída com sucesso!"))
            } else {
                call.respond(HttpStatusCode.NotFound, "Refeição não encontrada")
            }
        }
    }
}

private fun recalculateTemplateTotals(templateId: Int) {
    val allMeals = MealPlanMeals.select { MealPlanMeals.templateId eq templateId }
    val totalCal = allMeals.sumOf { it[MealPlanMeals.totalCalories] ?: 0.0 }
    val totalProt = allMeals.sumOf { it[MealPlanMeals.totalProtein] ?: 0.0 }
    val totalCarb = allMeals.sumOf { it[MealPlanMeals.totalCarbs] ?: 0.0 }
    val totalFatVal = allMeals.sumOf { it[MealPlanMeals.totalFat] ?: 0.0 }
    val totalFib = allMeals.sumOf { it[MealPlanMeals.totalFiber] ?: 0.0 }
    
    MealPlanTemplates.update({ MealPlanTemplates.id eq templateId }) {
        it[totalCalories] = totalCal
        it[totalProtein] = totalProt
        it[totalCarbs] = totalCarb
        it[totalFat] = totalFatVal
        it[totalFiber] = totalFib
    }
}
