package com.katsufit.routes.nutritionist

import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.Users
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

fun Route.patientAppointmentRouting() {
    authenticate("auth-jwt") {
        route("/api/nutritionist") {
            
            // 1. Criar uma nova consulta
            post("/appointments") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val nutritionistIdStr = principal?.payload?.getClaim("id")?.asString()
                    if (nutritionistIdStr == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                        return@post
                    }
                    val nutritionistUUID = UUID.fromString(nutritionistIdStr)

                    val request = call.receive<CreatePatientAppointmentRequest>()
                    val patientUUID = UUID.fromString(request.patientId)

                    // Verificar se o paciente pertence ao nutricionista
                    val isLinked = transaction {
                        NutritionistPatientLinks.select {
                            (NutritionistPatientLinks.nutritionistId eq nutritionistUUID) and
                            (NutritionistPatientLinks.patientId eq patientUUID)
                        }.count() > 0
                    }

                    if (!isLinked) {
                        call.respond(HttpStatusCode.Forbidden, "Paciente não vinculado a este nutricionista.")
                        return@post
                    }

                    val scheduledDateTime = try {
                        LocalDateTime.parse(request.scheduledAt)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "Formato de data inválido. Use ISO-8601 (Ex: 2026-03-22T14:30:00).")
                        return@post
                    }

                    val newAppointmentId = transaction {
                        PatientAppointments.insertAndGetId {
                            it[patientId] = patientUUID
                            it[nutritionistId] = nutritionistUUID
                            it[scheduledAt] = scheduledDateTime
                            it[type] = request.type
                            it[status] = "SCHEDULED"
                            it[notes] = request.notes
                        }.value
                    }

                    val responseDTO = transaction {
                        PatientAppointments.select { PatientAppointments.id eq newAppointmentId }.single().let {
                            PatientAppointmentDTO(
                                id = it[PatientAppointments.id].value.toString(),
                                patientId = it[PatientAppointments.patientId].value.toString(),
                                nutritionistId = it[PatientAppointments.nutritionistId].value.toString(),
                                scheduledAt = it[PatientAppointments.scheduledAt].toString(),
                                type = it[PatientAppointments.type],
                                status = it[PatientAppointments.status],
                                notes = it[PatientAppointments.notes],
                                createdAt = it[PatientAppointments.createdAt].toString()
                            )
                        }
                    }

                    call.respond(HttpStatusCode.Created, responseDTO)

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao agendar consulta", "details" to e.message))
                }
            }

            // 2. Buscar próximas consultas (dashboard)
            get("/appointments/upcoming") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val nutritionistIdStr = principal?.payload?.getClaim("id")?.asString()
                    if (nutritionistIdStr == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                        return@get
                    }
                    val nutritionistUUID = UUID.fromString(nutritionistIdStr)

                    val now = LocalDateTime.now()

                    val upcoming = transaction {
                        PatientAppointments
                            .select { 
                                (PatientAppointments.nutritionistId eq nutritionistUUID) and 
                                (PatientAppointments.status eq "SCHEDULED") and
                                (PatientAppointments.scheduledAt greaterEq now)
                            }
                            .orderBy(PatientAppointments.scheduledAt to SortOrder.ASC)
                            .limit(10)
                            .map {
                                PatientAppointmentDTO(
                                    id = it[PatientAppointments.id].value.toString(),
                                    patientId = it[PatientAppointments.patientId].value.toString(),
                                    nutritionistId = it[PatientAppointments.nutritionistId].value.toString(),
                                    scheduledAt = it[PatientAppointments.scheduledAt].toString(),
                                    type = it[PatientAppointments.type],
                                    status = it[PatientAppointments.status],
                                    notes = it[PatientAppointments.notes],
                                    createdAt = it[PatientAppointments.createdAt].toString()
                                )
                            }
                    }

                    call.respond(HttpStatusCode.OK, upcoming)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao buscar próximas consultas", "details" to e.message))
                }
            }

            // 3. Buscar consultas de um paciente específico (histórico no prontuário)
            get("/patients/{patientId}/appointments") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val nutritionistIdStr = principal?.payload?.getClaim("id")?.asString()
                    if (nutritionistIdStr == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                        return@get
                    }
                    val nutritionistUUID = UUID.fromString(nutritionistIdStr)
                    val patientIdParam = call.parameters["patientId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val patientUUID = UUID.fromString(patientIdParam)

                    val appointments = transaction {
                        PatientAppointments
                            .select { 
                                (PatientAppointments.nutritionistId eq nutritionistUUID) and 
                                (PatientAppointments.patientId eq patientUUID) 
                            }
                            .orderBy(PatientAppointments.scheduledAt to SortOrder.DESC)
                            .map {
                                PatientAppointmentDTO(
                                    id = it[PatientAppointments.id].value.toString(),
                                    patientId = it[PatientAppointments.patientId].value.toString(),
                                    nutritionistId = it[PatientAppointments.nutritionistId].value.toString(),
                                    scheduledAt = it[PatientAppointments.scheduledAt].toString(),
                                    type = it[PatientAppointments.type],
                                    status = it[PatientAppointments.status],
                                    notes = it[PatientAppointments.notes],
                                    createdAt = it[PatientAppointments.createdAt].toString()
                                )
                            }
                    }

                    call.respond(HttpStatusCode.OK, appointments)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao buscar consultas do paciente", "details" to e.message))
                }
            }

            // 4. Atualizar status de uma consulta
            put("/appointments/{id}/status") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val nutritionistIdStr = principal?.payload?.getClaim("id")?.asString()
                    if (nutritionistIdStr == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                        return@put
                    }
                    val nutritionistUUID = UUID.fromString(nutritionistIdStr)
                    val appointmentIdParam = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                    val appointmentUUID = UUID.fromString(appointmentIdParam)

                    val request = call.receive<UpdatePatientAppointmentStatusRequest>()

                    val updatedCount = transaction {
                        PatientAppointments.update({ 
                            (PatientAppointments.id eq appointmentUUID) and 
                            (PatientAppointments.nutritionistId eq nutritionistUUID) 
                        }) {
                            it[status] = request.status
                        }
                    }

                    if (updatedCount > 0) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Status da consulta atualizado com sucesso"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Consulta não encontrada ou sem permissão")
                    }

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao atualizar status", "details" to e.message))
                }
            }
        }
    }
}
