package com.katsufit.routes.nutritionist

import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.nutritionistRouting() {
    route("/api/nutritionist") {
        authenticate("auth-jwt") {
            
            // --- ROTA: LISTAR MEUS PACIENTES ---
            get("/patients") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val nutritionistId = UUID.fromString(principal!!.payload.getClaim("id").asString())

                    val patientsList = transaction {
                        NutritionistPatientLinks
                            .innerJoin(Users, { NutritionistPatientLinks.patient }, { Users.id })
                            .slice(Users.id, Users.name, Users.email)
                            .select { NutritionistPatientLinks.nutritionist eq nutritionistId }
                            .map { row ->
                                PatientDTO(
                                    id = row[Users.id].value.toString(),
                                    name = row[Users.name],
                                    email = row[Users.email]
                                )
                            }
                    }

                    call.respond(patientsList)
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "Erro ao buscar pacientes")
                }
            }

            // ========================================
            // --- ROTA: DETALHES COMPLETOS DO PACIENTE ---
            // ========================================
            get("/patients/{patientId}/details") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val nutritionistId = UUID.fromString(principal!!.payload.getClaim("id").asString())
                    
                    val patientId = call.parameters["patientId"]?.let { UUID.fromString(it) }
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "ID do paciente inválido")

                    val patientDetails = transaction {
                        // Verificar se o vínculo existe
                        val linkExists = NutritionistPatientLinks.select {
                            (NutritionistPatientLinks.nutritionist eq nutritionistId) and
                            (NutritionistPatientLinks.patient eq patientId)
                        }.singleOrNull()

                        if (linkExists == null) {
                            return@transaction null
                        }

                        // Buscar dados do usuário
                        val user = Users.select { Users.id eq patientId }.singleOrNull()
                            ?: return@transaction null

                        // Buscar último registro de progresso (dados antropométricos)
                        val lastProgress = ProgressEntries
                            .select { ProgressEntries.clientId eq patientId }
                            .orderBy(ProgressEntries.entryAt, SortOrder.DESC)
                            .limit(1)
                            .singleOrNull()

                        // Extrair informações das notas (onde salvamos no cadastro)
                        val notes = lastProgress?.get(ProgressEntries.notes) ?: ""
                        
                        // Função auxiliar para extrair dados das notas
                        fun extractFromNotes(field: String): String? {
                            val regex = "$field: (.+)".toRegex()
                            return regex.find(notes)?.groupValues?.get(1)?.trim()
                        }

                        // Calcular IMC se tiver peso e altura
                        val weight = lastProgress?.get(ProgressEntries.weight)
                        val heightText = extractFromNotes("Altura")
                        val height = heightText?.replace(" cm", "")?.toDoubleOrNull()
                        val imc = if (weight != null && height != null && height > 0) {
                            weight / ((height / 100) * (height / 100))
                        } else null

                        // Montar resposta
                        PatientDetailsDTO(
                            id = user[Users.id].value.toString(),
                            name = user[Users.name],
                            email = user[Users.email],
                            birthDate = extractFromNotes("Data Nascimento"),
                            phone = extractFromNotes("Telefone"),
                            gender = extractFromNotes("Gênero"),
                            weight = weight,
                            height = height,
                            imc = imc,
                            bodyFatPercentage = extractFromNotes("% Gordura")?.replace("%", "")?.toDoubleOrNull(),
                            waist = lastProgress?.get(ProgressEntries.waist),
                            chest = lastProgress?.get(ProgressEntries.chest),
                            hips = lastProgress?.get(ProgressEntries.hips),
                            goal = extractFromNotes("Objetivo"),
                            allergies = extractFromNotes("Alergias"),
                            medications = extractFromNotes("Medicamentos"),
                            healthConditions = extractFromNotes("Condições"),
                            notes = extractFromNotes("Obs"),
                            lastUpdate = lastProgress?.get(ProgressEntries.entryAt)?.toString()
                        )
                    }

                    if (patientDetails != null) {
                        call.respond(HttpStatusCode.OK, patientDetails)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Paciente não encontrado ou sem vínculo"))
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "Erro ao buscar detalhes do paciente: ${e.message}")
                }
            }

            // --- ROTA: EXCLUIR VÍNCULO COM PACIENTE ---
            delete("/patients/{patientId}") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val nutritionistId = UUID.fromString(principal!!.payload.getClaim("id").asString())
                    
                    val patientId = call.parameters["patientId"]?.let { UUID.fromString(it) }
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, "ID do paciente inválido")

                    val deleted = transaction {
                        NutritionistPatientLinks.deleteWhere {
                            (NutritionistPatientLinks.nutritionist eq nutritionistId) and
                            (NutritionistPatientLinks.patient eq patientId)
                        }
                    }

                    if (deleted > 0) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Paciente removido com sucesso"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Vínculo não encontrado")
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "Erro ao excluir paciente")
                }
            }

            // --- ROTA: ATIVAR PACIENTE ---
            patch("/patients/{patientId}/activate") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val nutritionistId = UUID.fromString(principal!!.payload.getClaim("id").asString())
                    
                    val patientId = call.parameters["patientId"]?.let { UUID.fromString(it) }
                        ?: return@patch call.respond(HttpStatusCode.BadRequest, "ID do paciente inválido")

                    val exists = transaction {
                        NutritionistPatientLinks.select {
                            (NutritionistPatientLinks.nutritionist eq nutritionistId) and
                            (NutritionistPatientLinks.patient eq patientId)
                        }.count() > 0
                    }

                    if (exists) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Paciente ativado", "active" to true))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Vínculo não encontrado")
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "Erro ao ativar paciente")
                }
            }

            // --- ROTA: INATIVAR PACIENTE ---
            patch("/patients/{patientId}/deactivate") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val nutritionistId = UUID.fromString(principal!!.payload.getClaim("id").asString())
                    
                    val patientId = call.parameters["patientId"]?.let { UUID.fromString(it) }
                        ?: return@patch call.respond(HttpStatusCode.BadRequest, "ID do paciente inválido")

                    val exists = transaction {
                        NutritionistPatientLinks.select {
                            (NutritionistPatientLinks.nutritionist eq nutritionistId) and
                            (NutritionistPatientLinks.patient eq patientId)
                        }.count() > 0
                    }

                    if (exists) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Paciente inativado", "active" to false))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Vínculo não encontrado")
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "Erro ao inativar paciente")
                }
            }

            // ========================================
            // --- ROTA NOVA: CADASTRO COMPLETO DE PACIENTE ---
            // ========================================
            post("/patients/register") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val nutritionistId = principal?.payload?.getClaim("id")?.asString()

                    if (nutritionistId == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                        return@post
                    }

                    val request = call.receive<PatientRegistrationRequest>()

                    // Validações
                    if (request.email.isBlank() || request.name.isBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            PatientRegistrationResponse(
                                success = false,
                                message = "Email e nome são obrigatórios",
                                patientId = null
                            )
                        )
                        return@post
                    }

                    val patientId = transaction {
                        // Verificar se o email já está cadastrado
                        val existingUser = Users.select { Users.email eq request.email }.singleOrNull()

                        val userId = if (existingUser != null) {
                            existingUser[Users.id].toString()
                        } else {
                            // Criar novo usuário (paciente)
                            val newUserId = UUID.randomUUID()
                            Users.insert {
                                it[id] = newUserId
                                it[email] = request.email
                                it[passwordHash] = "" // <<< CORRIGIDO: passwordHash
                                it[name] = request.name
                                it[userType] = "client"
                            }
                            newUserId.toString()
                        }

                        // Criar vínculo nutricionista-paciente
                        val linkExists = NutritionistPatientLinks.select {
                            (NutritionistPatientLinks.nutritionist eq UUID.fromString(nutritionistId)) and
                            (NutritionistPatientLinks.patient eq UUID.fromString(userId))
                        }.singleOrNull()

                        if (linkExists == null) {
                            NutritionistPatientLinks.insert {
                                it[nutritionist] = UUID.fromString(nutritionistId) // <<< CORRIGIDO
                                it[patient] = UUID.fromString(userId) // <<< CORRIGIDO
                            }
                        }

                        // Buscar o ProfessionalProfile do nutricionista
                        val professionalProfile = ProfessionalProfiles
                            .select { ProfessionalProfiles.userId eq UUID.fromString(nutritionistId) }
                            .singleOrNull()

                        if (professionalProfile != null) {
                            val professionalProfileId = professionalProfile[ProfessionalProfiles.id].value // <<< ADICIONA .value

                            // Salvar dados antropométricos iniciais
                            ProgressEntries.insert {
                                it[id] = UUID.randomUUID()
                                it[clientId] = UUID.fromString(userId)
                                it[professionalId] = professionalProfileId
                                it[weight] = request.weight
                                it[waist] = null // <<< CORRIGIDO
                                it[chest] = null // <<< CORRIGIDO
                                it[hips] = null // <<< CORRIGIDO
                                it[notes] = buildString {
                                    append("CADASTRO INICIAL\n")
                                    append("Data Nascimento: ${request.birthDate}\n")
                                    append("Telefone: ${request.phone}\n")
                                    append("Gênero: ${request.gender}\n")
                                    append("Altura: ${request.height} cm\n")
                                    if (request.bodyFatPercentage != null) append("% Gordura: ${request.bodyFatPercentage}%\n")
                                    append("Objetivo: ${request.goal}\n")
                                    if (!request.allergies.isNullOrBlank()) append("Alergias: ${request.allergies}\n")
                                    if (!request.medications.isNullOrBlank()) append("Medicamentos: ${request.medications}\n")
                                    if (!request.healthConditions.isNullOrBlank()) append("Condições: ${request.healthConditions}\n")
                                    if (!request.notes.isNullOrBlank()) append("Obs: ${request.notes}\n")
                                }
                            }
                        }

                        userId
                    }

                    call.respond(
                        HttpStatusCode.Created,
                        PatientRegistrationResponse(
                            success = true,
                            message = "Paciente cadastrado com sucesso!",
                            patientId = patientId
                        )
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        PatientRegistrationResponse(
                            success = false,
                            message = "Erro ao cadastrar paciente: ${e.message}",
                            patientId = null
                        )
                    )
                }
            }
        }
    }
}
