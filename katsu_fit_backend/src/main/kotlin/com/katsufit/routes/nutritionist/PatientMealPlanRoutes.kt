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

fun Route.patientMealPlanRouting() {

    authenticate("auth-jwt") {

        // ============================
        // VINCULAR TEMPLATE AO PACIENTE (copia o template)
        // ============================
        post("/api/patients/{patientId}/meal-plan") {
            val principal = call.principal<JWTPrincipal>()
            val nutritionistId = principal?.payload?.getClaim("id")?.asString()

            if (nutritionistId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                return@post
            }

            val patientId = call.parameters["patientId"]
            if (patientId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "ID do paciente inválido")
                return@post
            }

            val request = call.receive<AssignMealPlanRequest>()

            try {
                val result = transaction {
                    // 1. Buscar o template original
                    val template = MealPlanTemplates.select {
                        (MealPlanTemplates.id eq request.templateId) and
                        (MealPlanTemplates.nutritionistId eq nutritionistId)
                    }.singleOrNull() ?: return@transaction null

                    // 2. Desativar planos anteriores do paciente
                    PatientMealPlans.update({
                        (PatientMealPlans.patientId eq patientId) and
                        (PatientMealPlans.nutritionistId eq nutritionistId) and
                        (PatientMealPlans.isActive eq true)
                    }) {
                        it[isActive] = false
                    }

                    // 3. Criar cópia do template como plano do paciente
                    val newPlanId = PatientMealPlans.insert {
                        it[PatientMealPlans.patientId] = patientId
                        it[PatientMealPlans.nutritionistId] = nutritionistId
                        it[sourceTemplateId] = request.templateId
                        it[name] = template[MealPlanTemplates.name]
                        it[description] = template[MealPlanTemplates.description]
                        it[totalCalories] = template[MealPlanTemplates.totalCalories]
                        it[totalProtein] = template[MealPlanTemplates.totalProtein]
                        it[totalCarbs] = template[MealPlanTemplates.totalCarbs]
                        it[totalFat] = template[MealPlanTemplates.totalFat]
                        it[totalFiber] = template[MealPlanTemplates.totalFiber]
                        it[isActive] = true
                    }[PatientMealPlans.id]

                    // 4. Copiar todas as refeições do template PARA CADA DIA especificado
                    val templateMeals = MealPlanMeals.select {
                        MealPlanMeals.templateId eq request.templateId
                    }.orderBy(MealPlanMeals.orderIndex, SortOrder.ASC)

                    for (day in request.days) {
                        for (meal in templateMeals) {
                            PatientMealPlanMeals.insert {
                                it[patientPlanId] = newPlanId
                                it[dayOfWeek] = day
                                it[name] = meal[MealPlanMeals.name]
                                it[time] = meal[MealPlanMeals.time]
                                it[orderIndex] = meal[MealPlanMeals.orderIndex]
                                it[foods] = meal[MealPlanMeals.foods]
                                it[totalCalories] = meal[MealPlanMeals.totalCalories]
                                it[totalProtein] = meal[MealPlanMeals.totalProtein]
                                it[totalCarbs] = meal[MealPlanMeals.totalCarbs]
                                it[totalFat] = meal[MealPlanMeals.totalFat]
                                it[totalFiber] = meal[MealPlanMeals.totalFiber]
                            }
                        }
                    }

                    newPlanId
                }

                if (result == null) {
                    call.respond(HttpStatusCode.NotFound, "Template não encontrado")
                } else {
                    call.respond(HttpStatusCode.Created, AssignMealPlanResponse(
                        id = result,
                        message = "Plano vinculado ao paciente com sucesso!"
                    ))
                }
            } catch (e: Exception) {
                call.application.environment.log.error("Erro ao vincular plano", e)
                call.respond(HttpStatusCode.InternalServerError, "Erro ao vincular plano: ${e.message}")
            }
        }

        // ============================
        // BUSCAR PLANO ATIVO DO PACIENTE
        // ============================
        get("/api/patients/{patientId}/meal-plan") {
            val principal = call.principal<JWTPrincipal>()
            val nutritionistId = principal?.payload?.getClaim("id")?.asString()

            if (nutritionistId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                return@get
            }

            val patientId = call.parameters["patientId"]
            if (patientId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "ID do paciente inválido")
                return@get
            }

            val result = transaction {
                val plan = PatientMealPlans.select {
                    (PatientMealPlans.patientId eq patientId) and
                    (PatientMealPlans.nutritionistId eq nutritionistId) and
                    (PatientMealPlans.isActive eq true)
                }.singleOrNull() ?: return@transaction null

                val planId = plan[PatientMealPlans.id]

                val meals = PatientMealPlanMeals.select {
                    PatientMealPlanMeals.patientPlanId eq planId
                }.orderBy(PatientMealPlanMeals.orderIndex, SortOrder.ASC)
                    .map { mealRow ->
                        val foodsJson = mealRow[PatientMealPlanMeals.foods]
                        val foodsList = try {
                            Json.decodeFromString<List<MealFoodItem>>(foodsJson)
                        } catch (e: Exception) {
                            emptyList()
                        }

                        PatientMealPlanMealDTO(
                            id = mealRow[PatientMealPlanMeals.id],
                            patientPlanId = mealRow[PatientMealPlanMeals.patientPlanId],
                            dayOfWeek = mealRow[PatientMealPlanMeals.dayOfWeek],
                            name = mealRow[PatientMealPlanMeals.name],
                            time = mealRow[PatientMealPlanMeals.time],
                            orderIndex = mealRow[PatientMealPlanMeals.orderIndex],
                            foods = foodsList,
                            totalCalories = mealRow[PatientMealPlanMeals.totalCalories],
                            totalProtein = mealRow[PatientMealPlanMeals.totalProtein],
                            totalCarbs = mealRow[PatientMealPlanMeals.totalCarbs],
                            totalFat = mealRow[PatientMealPlanMeals.totalFat],
                            totalFiber = mealRow[PatientMealPlanMeals.totalFiber],
                            createdAt = mealRow[PatientMealPlanMeals.createdAt].toString(),
                            updatedAt = mealRow[PatientMealPlanMeals.updatedAt].toString()
                        )
                    }

                PatientMealPlanDTO(
                    id = plan[PatientMealPlans.id],
                    patientId = plan[PatientMealPlans.patientId],
                    nutritionistId = plan[PatientMealPlans.nutritionistId],
                    sourceTemplateId = plan[PatientMealPlans.sourceTemplateId],
                    name = plan[PatientMealPlans.name],
                    description = plan[PatientMealPlans.description],
                    totalCalories = plan[PatientMealPlans.totalCalories],
                    totalProtein = plan[PatientMealPlans.totalProtein],
                    totalCarbs = plan[PatientMealPlans.totalCarbs],
                    totalFat = plan[PatientMealPlans.totalFat],
                    totalFiber = plan[PatientMealPlans.totalFiber],
                    isActive = plan[PatientMealPlans.isActive],
                    meals = meals,
                    createdAt = plan[PatientMealPlans.createdAt].toString(),
                    updatedAt = plan[PatientMealPlans.updatedAt].toString()
                )
            }

            if (result == null) {
                call.respond(HttpStatusCode.NotFound, "Nenhum plano ativo encontrado para este paciente")
            } else {
                call.respond(result)
            }
        }

        // ============================
        // ATUALIZAR ALIMENTOS DE UMA REFEIÇÃO DO PACIENTE
        // ============================
        put("/api/patients/{patientId}/meal-plan/meals/{mealId}/foods") {
            val principal = call.principal<JWTPrincipal>()
            val nutritionistId = principal?.payload?.getClaim("id")?.asString()

            if (nutritionistId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                return@put
            }

            val patientId = call.parameters["patientId"]
            val mealId = call.parameters["mealId"]?.toIntOrNull()

            if (patientId.isNullOrBlank() || mealId == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetros inválidos")
                return@put
            }

            val request = call.receive<UpdateMealFoodsRequest>()

            val updated = transaction {
                // Verificar que a refeição pertence ao plano ativo do paciente
                val meal = PatientMealPlanMeals.select {
                    PatientMealPlanMeals.id eq mealId
                }.singleOrNull() ?: return@transaction false

                val planId = meal[PatientMealPlanMeals.patientPlanId]

                val plan = PatientMealPlans.select {
                    (PatientMealPlans.id eq planId) and
                    (PatientMealPlans.patientId eq patientId) and
                    (PatientMealPlans.nutritionistId eq nutritionistId) and
                    (PatientMealPlans.isActive eq true)
                }.singleOrNull() ?: return@transaction false

                val totalCal = request.foods.sumOf { it.calories ?: 0.0 }
                val totalProt = request.foods.sumOf { it.protein ?: 0.0 }
                val totalCarb = request.foods.sumOf { it.carbs ?: 0.0 }
                val totalFatVal = request.foods.sumOf { it.fat ?: 0.0 }
                val totalFib = request.foods.sumOf { it.fiber ?: 0.0 }

                PatientMealPlanMeals.update({ PatientMealPlanMeals.id eq mealId }) {
                    it[foods] = Json.encodeToString(request.foods)
                    it[totalCalories] = totalCal
                    it[totalProtein] = totalProt
                    it[totalCarbs] = totalCarb
                    it[totalFat] = totalFatVal
                    it[totalFiber] = totalFib
                }

                // Recalcular totais do plano
                recalculatePatientPlanTotals(planId)

                true
            }

            if (updated) {
                call.respond(HttpStatusCode.OK, SimpleMessageResponse("Alimentos atualizados com sucesso!"))
            } else {
                call.respond(HttpStatusCode.NotFound, "Refeição não encontrada")
            }
        }

        // ============================
        // REMOVER PLANO DO PACIENTE (desativar)
        // ============================
        delete("/api/patients/{patientId}/meal-plan") {
            val principal = call.principal<JWTPrincipal>()
            val nutritionistId = principal?.payload?.getClaim("id")?.asString()

            if (nutritionistId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                return@delete
            }

            val patientId = call.parameters["patientId"]
            if (patientId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "ID do paciente inválido")
                return@delete
            }

            val deactivated = transaction {
                PatientMealPlans.update({
                    (PatientMealPlans.patientId eq patientId) and
                    (PatientMealPlans.nutritionistId eq nutritionistId) and
                    (PatientMealPlans.isActive eq true)
                }) {
                    it[isActive] = false
                }
            }

            if (deactivated > 0) {
                call.respond(HttpStatusCode.OK, SimpleMessageResponse("Plano removido do paciente com sucesso"))
            } else {
                call.respond(HttpStatusCode.NotFound, "Nenhum plano ativo encontrado")
            }
        }
    }
}

private fun recalculatePatientPlanTotals(planId: Int) {
    val allMeals = PatientMealPlanMeals.select { PatientMealPlanMeals.patientPlanId eq planId }
    val totalCal = allMeals.sumOf { it[PatientMealPlanMeals.totalCalories] ?: 0.0 }
    val totalProt = allMeals.sumOf { it[PatientMealPlanMeals.totalProtein] ?: 0.0 }
    val totalCarb = allMeals.sumOf { it[PatientMealPlanMeals.totalCarbs] ?: 0.0 }
    val totalFatVal = allMeals.sumOf { it[PatientMealPlanMeals.totalFat] ?: 0.0 }
    val totalFib = allMeals.sumOf { it[PatientMealPlanMeals.totalFiber] ?: 0.0 }

    PatientMealPlans.update({ PatientMealPlans.id eq planId }) {
        it[totalCalories] = totalCal
        it[totalProtein] = totalProt
        it[totalCarbs] = totalCarb
        it[totalFat] = totalFatVal
        it[totalFiber] = totalFib
    }
}
