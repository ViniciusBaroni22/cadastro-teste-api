package com.katsufit.routes.nutritionist

import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.*
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.and
import java.util.UUID

fun Route.nutritionPlanRouting() {
    authenticate("auth-jwt") {
        route("/api/plans/nutrition") {
            
            // ROTA 1: CRIAR NOVO PLANO (JÁ EXISTIA)
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val professionalUserId = principal?.payload?.getClaim("id")?.asString()?.let {
                        UUID.fromString(it)
                    }

                    if (professionalUserId == null) {
                        call.respondText("Token de profissional inválido.", status = HttpStatusCode.Unauthorized)
                        return@post
                    }

                    val nutritionPlanRequest = call.receive<NutritionPlanRequest>()

                    val newPlanId = transaction {
                        // procurar profile pelo userId (UUID do usuário presente no JWT)
                        val professionalProfileRow = ProfessionalProfiles.select { ProfessionalProfiles.userId eq professionalUserId }.singleOrNull()

                        if (professionalProfileRow == null) {
                            throw IllegalArgumentException("Perfil de profissional não encontrado.")
                        }

                        // id do profile (referenciado pela FK nutrition_plans.professional_id)
                        val professionalProfileId = professionalProfileRow[ProfessionalProfiles.id] // EntityID<UUID>
                        val professionalProfileUuid: UUID = professionalProfileId.value

                        NutritionPlans.insert {
                            it[NutritionPlans.professionalId] = professionalProfileUuid
                            it[NutritionPlans.clientId] = UUID.fromString(nutritionPlanRequest.clientId)
                            it[NutritionPlans.name] = nutritionPlanRequest.name
                            it[NutritionPlans.startDate] = nutritionPlanRequest.startDate
                            it[NutritionPlans.endDate] = nutritionPlanRequest.endDate
                            it[NutritionPlans.description] = nutritionPlanRequest.description
                        } get NutritionPlans.id
                    }

                    call.respondText("Plano de nutrição criado com sucesso com o ID: $newPlanId", status = HttpStatusCode.Created)
                } catch (e: IllegalArgumentException) {
                    call.respondText(e.message ?: "Erro no corpo da requisição.", status = HttpStatusCode.BadRequest)
                } catch (e: Exception) {
                    call.application.environment.log.error("Erro criando plano de nutrição", e)
                    call.respondText("Erro ao criar plano de nutrição: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }

            // ROTA 2: LISTAR PLANOS DE UM CLIENTE ESPECÍFICO (NOVA)
            get {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val professionalUserId = principal?.payload?.getClaim("id")?.asString()?.let {
                        UUID.fromString(it)
                    }

                    if (professionalUserId == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Token inválido.")
                        return@get
                    }

                    // Pega o ID do cliente que veio na URL (ex: ?clientId=123-abc...)
                    val clientIdParam = call.request.queryParameters["clientId"]
                    if (clientIdParam.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Parâmetro 'clientId' é obrigatório.")
                        return@get
                    }
                    val clientUuid = UUID.fromString(clientIdParam)

                    val plansList = transaction {
                        // 1. Achar o perfil do profissional logado
                        val professionalProfileRow = ProfessionalProfiles.select { ProfessionalProfiles.userId eq professionalUserId }.singleOrNull()
                        
                        if (professionalProfileRow == null) {
                            return@transaction emptyList<NutritionPlanResponse>()
                        }
                        
                        val professionalProfileId = professionalProfileRow[ProfessionalProfiles.id].value

                        // 2. Buscar planos filtrando pelo Profissional E pelo Cliente
                        NutritionPlans.select {
                            (NutritionPlans.professionalId eq professionalProfileId) and
                            (NutritionPlans.clientId eq clientUuid)
                        }.map {
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

                    call.respond(plansList)

                } catch (e: Exception) {
                    call.application.environment.log.error("Erro ao buscar planos", e)
                    call.respond(HttpStatusCode.InternalServerError, "Erro ao buscar planos: ${e.message}")
                }
            }
        }
    }
}
