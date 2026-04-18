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

                        if (linkExists == null) return@transaction null

                        // Buscar dados do usuário
                        val user = Users.select { Users.id eq patientId }.singleOrNull()
                            ?: return@transaction null

                        // Buscar patient_record
                        val record = PatientRecords.select {
                            (PatientRecords.patientId eq patientId.toString()) and
                            (PatientRecords.nutritionistId eq nutritionistId.toString())
                        }.singleOrNull()

                        // Buscar último progresso para peso atualizado
                        val lastProgress = ProgressEntries
                            .select { ProgressEntries.clientId eq patientId }
                            .orderBy(ProgressEntries.entryAt, SortOrder.DESC)
                            .limit(1)
                            .singleOrNull()

                        val weight = lastProgress?.get(ProgressEntries.weight)
                            ?: record?.get(PatientRecords.weight)
                        val height = record?.get(PatientRecords.height)
                        val imc = if (weight != null && height != null && height > 0) {
                            weight / ((height / 100) * (height / 100))
                        } else null

                        PatientDetailsDTO(
                            id = user[Users.id].value.toString(),
                            name = user[Users.name],
                            email = user[Users.email],
                            birthDate = record?.get(PatientRecords.birthDate),
                            phone = record?.get(PatientRecords.phone),
                            gender = record?.get(PatientRecords.gender),
                            weight = weight,
                            height = height,
                            imc = imc,
                            bodyFatPercentage = record?.get(PatientRecords.bodyFatPercentage),
                            waist = lastProgress?.get(ProgressEntries.waist),
                            chest = lastProgress?.get(ProgressEntries.chest),
                            hips = lastProgress?.get(ProgressEntries.hips),
                            goal = record?.get(PatientRecords.goal),
                            allergies = record?.get(PatientRecords.allergies),
                            medications = record?.get(PatientRecords.medications),
                            healthConditions = record?.get(PatientRecords.healthConditions),
                            notes = record?.get(PatientRecords.notes),
                            lastUpdate = lastProgress?.get(ProgressEntries.entryAt)?.toString()
                                ?: record?.get(PatientRecords.updatedAt)?.toString()
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

                    if (request.email.isBlank() || request.name.isBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            PatientRegistrationResponse(success = false, message = "Email e nome são obrigatórios", patientId = null)
                        )
                        return@post
                    }

                    val patientId = transaction {
                        val existingUser = Users.select { Users.email eq request.email }.singleOrNull()

                        val userId = if (existingUser != null) {
                            existingUser[Users.id].toString()
                        } else {
                            val newUserId = UUID.randomUUID()
                            Users.insert {
                                it[id] = newUserId
                                it[email] = request.email
                                it[passwordHash] = ""
                                it[name] = request.name
                                it[userType] = "CLIENT"
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
                                it[nutritionist] = UUID.fromString(nutritionistId)
                                it[patient] = UUID.fromString(userId)
                            }
                        }

                        // Salvar dados na tabela patient_records
                        val existingRecord = PatientRecords.select {
                            (PatientRecords.patientId eq userId) and
                            (PatientRecords.nutritionistId eq nutritionistId)
                        }.singleOrNull()

                        if (existingRecord == null) {
                            PatientRecords.insert {
                                it[PatientRecords.patientId] = userId
                                it[PatientRecords.nutritionistId] = nutritionistId
                                it[birthDate] = request.birthDate
                                it[phone] = request.phone
                                it[gender] = request.gender
                                it[height] = request.height
                                it[weight] = request.weight
                                it[bodyFatPercentage] = request.bodyFatPercentage
                                it[goal] = request.goal
                                it[allergies] = request.allergies
                                it[medications] = request.medications
                                it[healthConditions] = request.healthConditions
                                it[notes] = request.notes
                            }
                        } else {
                            PatientRecords.update({
                                (PatientRecords.patientId eq userId) and
                                (PatientRecords.nutritionistId eq nutritionistId)
                            }) {
                                it[birthDate] = request.birthDate
                                it[phone] = request.phone
                                it[gender] = request.gender
                                it[height] = request.height
                                it[weight] = request.weight
                                it[bodyFatPercentage] = request.bodyFatPercentage
                                it[goal] = request.goal
                                it[allergies] = request.allergies
                                it[medications] = request.medications
                                it[healthConditions] = request.healthConditions
                                it[notes] = request.notes
                            }
                        }

                        // Registro inicial de progresso (peso)
                        val professionalProfile = ProfessionalProfiles
                            .select { ProfessionalProfiles.userId eq UUID.fromString(nutritionistId) }
                            .singleOrNull()

                        if (professionalProfile != null) {
                            val professionalProfileId = professionalProfile[ProfessionalProfiles.id].value
                            ProgressEntries.insert {
                                it[id] = UUID.randomUUID()
                                it[clientId] = UUID.fromString(userId)
                                it[professionalId] = professionalProfileId
                                it[ProgressEntries.weight] = request.weight
                                it[waist] = null
                                it[chest] = null
                                it[hips] = null
                                it[ProgressEntries.notes] = "Cadastro inicial"
                            }
                        }

                        userId
                    }

                    call.respond(
                        HttpStatusCode.Created,
                        PatientRegistrationResponse(success = true, message = "Paciente cadastrado com sucesso!", patientId = patientId)
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        PatientRegistrationResponse(success = false, message = "Erro ao cadastrar paciente: ${e.message}", patientId = null)
                    )
                }
            }

            // ========================================
            // --- ROTA: ATUALIZAR DADOS DO PACIENTE ---
            // ========================================
            put("/patients/{patientId}/record") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val nutritionistId = principal?.payload?.getClaim("id")?.asString()

                    if (nutritionistId == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                        return@put
                    }

                    val patientId = call.parameters["patientId"]
                    if (patientId.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "ID do paciente inválido")
                        return@put
                    }

                    val request = call.receive<UpdatePatientRecordRequest>()

                    val updated = transaction {
                        val existing = PatientRecords.select {
                            (PatientRecords.patientId eq patientId) and
                            (PatientRecords.nutritionistId eq nutritionistId)
                        }.singleOrNull()

                        if (existing != null) {
                            PatientRecords.update({
                                (PatientRecords.patientId eq patientId) and
                                (PatientRecords.nutritionistId eq nutritionistId)
                            }) {
                                request.birthDate?.let { v -> it[birthDate] = v }
                                request.phone?.let { v -> it[phone] = v }
                                request.gender?.let { v -> it[gender] = v }
                                request.height?.let { v -> it[height] = v }
                                request.weight?.let { v -> it[weight] = v }
                                request.bodyFatPercentage?.let { v -> it[bodyFatPercentage] = v }
                                request.goal?.let { v -> it[goal] = v }
                                request.allergies?.let { v -> it[allergies] = v }
                                request.medications?.let { v -> it[medications] = v }
                                request.healthConditions?.let { v -> it[healthConditions] = v }
                                request.notes?.let { v -> it[notes] = v }
                            }
                            true
                        } else {
                            // Criar se não existir
                            PatientRecords.insert {
                                it[PatientRecords.patientId] = patientId
                                it[PatientRecords.nutritionistId] = nutritionistId
                                request.birthDate?.let { v -> it[birthDate] = v }
                                request.phone?.let { v -> it[phone] = v }
                                request.gender?.let { v -> it[gender] = v }
                                request.height?.let { v -> it[height] = v }
                                request.weight?.let { v -> it[weight] = v }
                                request.bodyFatPercentage?.let { v -> it[bodyFatPercentage] = v }
                                request.goal?.let { v -> it[goal] = v }
                                request.allergies?.let { v -> it[allergies] = v }
                                request.medications?.let { v -> it[medications] = v }
                                request.healthConditions?.let { v -> it[healthConditions] = v }
                                request.notes?.let { v -> it[notes] = v }
                            }
                            true
                        }
                    }

                    if (updated) {
                        call.respond(HttpStatusCode.OK, SimpleMessageResponse("Dados do paciente atualizados com sucesso!"))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Erro ao atualizar dados")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "Erro: ${e.message}")
                }
            }
        }
    }
}
