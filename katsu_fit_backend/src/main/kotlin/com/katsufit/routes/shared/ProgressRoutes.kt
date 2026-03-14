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

fun Route.progressRouting() {
    authenticate("auth-jwt") {
        route("/api/progress") {

            // Registrar novo progresso (profissional registra progresso do cliente)
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) }
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Token inválido.")
                        return@post
                    }

                    // Encontrar profile profissional do usuário autenticado
                    val professionalProfile = transaction {
                        ProfessionalProfiles.select { ProfessionalProfiles.userId eq userId }.singleOrNull()
                    }
                    if (professionalProfile == null) {
                        call.respond(HttpStatusCode.Forbidden, "Apenas profissionais podem registrar progresso.")
                        return@post
                    }
                    val professionalId = professionalProfile[ProfessionalProfiles.id]

                    val req = call.receive<ProgressRequest>()
                    val clientUUID = try {
                        UUID.fromString(req.clientId)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "clientId inválido.")
                        return@post
                    }

                    val newId = transaction {
                        ProgressEntries.insertAndGetId {
                            it[ProgressEntries.clientId] = clientUUID
                            it[ProgressEntries.professionalId] = professionalId.value
                            it[ProgressEntries.weight] = req.weight
                            it[ProgressEntries.waist] = req.waist
                            it[ProgressEntries.chest] = req.chest
                            it[ProgressEntries.hips] = req.hips
                            it[ProgressEntries.notes] = req.notes
                        }.value
                    }

                    val resp = transaction {
                        ProgressEntries.select { ProgressEntries.id eq newId }
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
                            }.first()
                    }

                    call.respond(HttpStatusCode.Created, resp)

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao registrar progresso", "details" to (e.message ?: "erro desconhecido")))
                }
            }

            // Buscar histórico de progresso
            // Comportamento:
            // - Se usuário for cliente (não possuir profile profissional), retorna os próprios registros.
            // - Se usuário for profissional, aceita query param clientId (obrigatório) e retorna registros desse cliente feitos pelo profissional autenticado.
            get {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) }
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Token inválido.")
                        return@get
                    }

                    // Verifica se é profissional
                    val professionalProfile = transaction {
                        ProfessionalProfiles.select { ProfessionalProfiles.userId eq userId }.singleOrNull()
                    }

                    if (professionalProfile == null) {
                        // Usuário é cliente -> retorna seus próprios registros
                        val clientId = userId
                        val list = transaction {
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
                        call.respond(list)
                        return@get
                    } else {
                        // Usuário é profissional -> deve informar clientId
                        val clientIdParam = call.request.queryParameters["clientId"]
                        if (clientIdParam.isNullOrBlank()) {
                            call.respond(HttpStatusCode.BadRequest, "Profissionais devem informar o parâmetro 'clientId'.")
                            return@get
                        }
                        val clientUUID = try {
                            UUID.fromString(clientIdParam)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, "clientId inválido.")
                            return@get
                        }
                        val professionalId = professionalProfile[ProfessionalProfiles.id]

                        val list = transaction {
                            ProgressEntries.select {
                                (ProgressEntries.clientId eq clientUUID) and
                                (ProgressEntries.professionalId eq professionalId.value)
                            }
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
                        call.respond(list)
                        return@get
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao buscar progresso", "details" to (e.message ?: "erro desconhecido")))
                }
            }
        }
    }
}
