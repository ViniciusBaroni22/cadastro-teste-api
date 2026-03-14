package com.katsufit.routes.shared

import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.http.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SortOrder
import java.util.UUID

fun Route.clientDashboardRouting() {
    authenticate("auth-jwt") {
        route("/api/client/dashboard") {
            get {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val clientId = principal?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) }
                    if (clientId == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Token inválido.")
                        return@get
                    }

                    val nutritionPlans = transaction {
                        NutritionPlans.select { NutritionPlans.clientId eq clientId }
                            .map {
                                NutritionPlanResponse(
                                    id = it[NutritionPlans.id].value.toString(),
                                    professionalId = it[NutritionPlans.professionalId].toString(),
                                    name = it[NutritionPlans.name],
                                    startDate = it[NutritionPlans.startDate].toString(),
                                    endDate = it[NutritionPlans.endDate].toString(),
                                    description = it[NutritionPlans.description],
                                    createdAt = it[NutritionPlans.createdAt].toString()
                                )
                            }
                    }

                    val trainingPlans = transaction {
                        TrainingPlans.select { TrainingPlans.clientId eq clientId }
                            .map {
                                TrainingPlanResponse(
                                    id = it[TrainingPlans.id].value.toString(),
                                    professionalId = it[TrainingPlans.professionalId].toString(),
                                    name = it[TrainingPlans.name],
                                    description = it[TrainingPlans.description],
                                    createdAt = it[TrainingPlans.createdAt].toString()
                                )
                            }
                    }

                    val progress = transaction {
                        ProgressEntries.select { ProgressEntries.clientId eq clientId }
                            .orderBy(ProgressEntries.entryAt to SortOrder.DESC)
                            .map {
                                ProgressResponse(
                                    id = it[ProgressEntries.id].value.toString(),
                                    clientId = it[ProgressEntries.clientId].toString(),
                                    professionalId = it[ProgressEntries.professionalId].toString(),
                                    weight = it[ProgressEntries.weight],
                                    waist = it[ProgressEntries.waist],
                                    chest = it[ProgressEntries.chest],
                                    hips = it[ProgressEntries.hips],
                                    notes = it[ProgressEntries.notes],
                                    entryAt = it[ProgressEntries.entryAt].toString()
                                )
                            }
                    }

                    call.respond(
                        ClientDashboardResponse(
                            nutritionPlans = nutritionPlans,
                            trainingPlans = trainingPlans,
                            progress = progress
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Erro ao buscar informações do dashboard", "details" to (e.message ?: "Erro desconhecido"))
                    )
                }
            }
        }
    }
}
