package com.katsufit.routes.shared

import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.trainingPlanRouting() {
    authenticate("auth-jwt") {
        route("/api/plans/training") {

            // Criar plano de treino
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) }
                    if (userId == null) {
                        call.respondText("Token inválido.", status = HttpStatusCode.Unauthorized)
                        return@post
                    }
                    val req = call.receive<TrainingPlanRequest>()
                    val professionalProfile = transaction {
                        ProfessionalProfiles.select { ProfessionalProfiles.userId eq userId }.singleOrNull()
                    }
                    if (professionalProfile == null) {
                        call.respondText("Perfil profissional não encontrado.", status = HttpStatusCode.BadRequest)
                        return@post
                    }
                    val professionalId = professionalProfile[ProfessionalProfiles.id]
                    val newPlanId = transaction {
                        TrainingPlans.insertAndGetId {
                            it[TrainingPlans.professionalId] = professionalId.value
                            it[TrainingPlans.clientId] = UUID.fromString(req.clientId)
                            it[TrainingPlans.name] = req.name
                            it[TrainingPlans.description] = req.description
                        }.value
                    }
                    call.respondText("Plano de treino criado com sucesso com o ID: $newPlanId", status = HttpStatusCode.Created)
                } catch (e: Exception) {
                    call.respondText("Erro ao criar plano de treino: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }

            // Listar planos de treino do profissional autenticado
            get {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) }
                    if (userId == null) {
                        call.respondText("Token inválido.", status = HttpStatusCode.Unauthorized)
                        return@get
                    }
                    val plans = transaction {
                        val professionalProfile = ProfessionalProfiles.select { ProfessionalProfiles.userId eq userId }.singleOrNull()
                        if (professionalProfile == null) return@transaction emptyList<TrainingPlanResponse>()
                        val professionalId = professionalProfile[ProfessionalProfiles.id]
                        TrainingPlans.select { TrainingPlans.professionalId eq professionalId.value }.map {
                            TrainingPlanResponse(
                                id = it[TrainingPlans.id].value.toString(),
                                professionalId = professionalId.value.toString(),
                                name = it[TrainingPlans.name],
                                description = it[TrainingPlans.description],
                                createdAt = it[TrainingPlans.createdAt].toString()
                            )
                        }
                    }
                    call.respond(plans)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Erro ao buscar planos de treino", "details" to (e.message ?: "Erro desconhecido"))
                    )
                }
            }

            // Buscar plano de treino por ID (CORRIGIDO!)
            get("/{id}") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) }
                    if (userId == null) {
                        call.respondText("Token inválido.", status = HttpStatusCode.Unauthorized)
                        return@get
                    }
                    val planId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "ID não informado")
                    val plan = transaction {
                        val professionalProfile = ProfessionalProfiles.select { ProfessionalProfiles.userId eq userId }.singleOrNull()
                        if (professionalProfile == null) return@transaction null
                        val professionalId = professionalProfile[ProfessionalProfiles.id]
                        TrainingPlans.select {
                            (TrainingPlans.professionalId eq professionalId.value) and
                            (TrainingPlans.id eq UUID.fromString(planId))
                        }.singleOrNull()
                    }
                    if (plan == null) {
                        call.respond(HttpStatusCode.NotFound, "Plano não encontrado")
                    } else {
                        call.respond(
                            TrainingPlanResponse(
                                id = plan[TrainingPlans.id].value.toString(),
                                professionalId = plan[TrainingPlans.professionalId].toString(),
                                name = plan[TrainingPlans.name],
                                description = plan[TrainingPlans.description],
                                createdAt = plan[TrainingPlans.createdAt].toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Erro ao buscar plano de treino", "details" to (e.message ?: "Erro desconhecido"))
                    )
                }
            }
        }
    }
}
