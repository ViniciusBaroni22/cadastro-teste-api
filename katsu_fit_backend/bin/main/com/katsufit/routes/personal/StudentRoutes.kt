package com.katsufit.routes.personal

import com.katsufit.models.personal.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

fun Route.studentRoutes() {
    authenticate("auth-jwt") {
        route("/api/personal/students") {

            // ========================================
            // POST /api/personal/students/register
            // Cadastrar novo aluno
            // ========================================
            post("/register") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                        ?: return@post call.respond(
                            HttpStatusCode.Unauthorized,
                            StudentRegistrationResponse(false, "Token inválido")
                        )

                    val request = call.receive<StudentRegistrationRequest>()

                    // Validação
                    if (request.name.isBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            StudentRegistrationResponse(false, "Nome é obrigatório")
                        )
                        return@post
                    }

                    val student = transaction {
                        // Verificar se profissional existe
                        Professional.findById(professionalId)
                            ?: return@transaction null

                        // Criar aluno vinculado ao profissional
                        ProfessionalStudent.new {
                            this.professional = Professional[professionalId]
                            this.studentId = Random.nextInt(10000, 99999) // ID temporário único
                            this.studentName = request.name.trim()
                            this.isActive = true
                            this.objective = request.objective
                            this.age = request.age
                            this.email = request.email?.trim()
                            this.phone = request.phone?.trim()
                            this.birthDate = request.birthDate
                            this.gender = request.gender
                            this.weight = request.weight
                            this.height = request.height
                            this.bodyFatPercentage = request.bodyFatPercentage
                            this.healthConditions = request.healthConditions
                            this.injuries = request.injuries
                            this.experience = request.experience
                            this.trainingFrequency = request.trainingFrequency
                            this.notes = request.notes
                        }
                    }

                    if (student == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            StudentRegistrationResponse(false, "Profissional não encontrado")
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.Created,
                            StudentRegistrationResponse(true, "Aluno cadastrado com sucesso!", student.id.value)
                        )
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        StudentRegistrationResponse(false, "Erro ao cadastrar aluno: ${e.message}")
                    )
                }
            }

            // ========================================
            // GET /api/personal/students
            // Listar todos os alunos do profissional
            // ========================================
            get {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

                    val students = transaction {
                        ProfessionalStudent.find {
                            ProfessionalStudents.professionalId eq professionalId
                        }.map { student ->
                            val imc = if (student.weight != null && student.height != null && student.height!! > 0) {
                                student.weight!! / ((student.height!! / 100) * (student.height!! / 100))
                            } else null

                            StudentDetailsDTO(
                                id = student.id.value,
                                studentId = student.studentId,
                                name = student.studentName,
                                email = student.email,
                                phone = student.phone,
                                birthDate = student.birthDate,
                                gender = student.gender,
                                age = student.age,
                                weight = student.weight,
                                height = student.height,
                                bodyFatPercentage = student.bodyFatPercentage,
                                imc = imc,
                                objective = student.objective,
                                healthConditions = student.healthConditions,
                                injuries = student.injuries,
                                experience = student.experience,
                                trainingFrequency = student.trainingFrequency,
                                notes = student.notes,
                                isActive = student.isActive,
                                hasActiveCredit = student.hasValidCredit(),
                                creditExpiresAt = student.creditExpiresAt?.toString(),
                                createdAt = student.createdAt.toString()
                            )
                        }
                    }

                    call.respond(HttpStatusCode.OK, students)

                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao listar alunos: ${e.message}"))
                }
            }

            // ========================================
            // GET /api/personal/students/{id}
            // Detalhes de um aluno específico
            // ========================================
            get("/{id}") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

                    val studentDbId = call.parameters["id"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

                    val student = transaction {
                        ProfessionalStudent.findById(studentDbId)?.let { s ->
                            if (s.professional.id.value != professionalId) return@let null

                            val imc = if (s.weight != null && s.height != null && s.height!! > 0) {
                                s.weight!! / ((s.height!! / 100) * (s.height!! / 100))
                            } else null

                            StudentDetailsDTO(
                                id = s.id.value,
                                studentId = s.studentId,
                                name = s.studentName,
                                email = s.email,
                                phone = s.phone,
                                birthDate = s.birthDate,
                                gender = s.gender,
                                age = s.age,
                                weight = s.weight,
                                height = s.height,
                                bodyFatPercentage = s.bodyFatPercentage,
                                imc = imc,
                                objective = s.objective,
                                healthConditions = s.healthConditions,
                                injuries = s.injuries,
                                experience = s.experience,
                                trainingFrequency = s.trainingFrequency,
                                notes = s.notes,
                                isActive = s.isActive,
                                hasActiveCredit = s.hasValidCredit(),
                                creditExpiresAt = s.creditExpiresAt?.toString(),
                                createdAt = s.createdAt.toString()
                            )
                        }
                    }

                    if (student != null) {
                        call.respond(HttpStatusCode.OK, student)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Aluno não encontrado"))
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro: ${e.message}"))
                }
            }

            // ========================================
            // PUT /api/personal/students/{id}
            // Atualizar dados do aluno
            // ========================================
            put("/{id}") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                        ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

                    val studentDbId = call.parameters["id"]?.toIntOrNull()
                        ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

                    val request = call.receive<UpdateStudentRequest>()

                    val updated = transaction {
                        val student = ProfessionalStudent.findById(studentDbId)
                            ?: return@transaction false

                        if (student.professional.id.value != professionalId) return@transaction false

                        request.name?.let { student.studentName = it.trim() }
                        request.email?.let { student.email = it.trim() }
                        request.phone?.let { student.phone = it.trim() }
                        request.birthDate?.let { student.birthDate = it }
                        request.gender?.let { student.gender = it }
                        request.age?.let { student.age = it }
                        request.weight?.let { student.weight = it }
                        request.height?.let { student.height = it }
                        request.bodyFatPercentage?.let { student.bodyFatPercentage = it }
                        request.objective?.let { student.objective = it }
                        request.healthConditions?.let { student.healthConditions = it }
                        request.injuries?.let { student.injuries = it }
                        request.experience?.let { student.experience = it }
                        request.trainingFrequency?.let { student.trainingFrequency = it }
                        request.notes?.let { student.notes = it }

                        true
                    }

                    if (updated) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Aluno atualizado com sucesso"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Aluno não encontrado"))
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro: ${e.message}"))
                }
            }

            // ========================================
            // PATCH /api/personal/students/{id}/deactivate
            // Inativar aluno
            // ========================================
            patch("/{id}/deactivate") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                        ?: return@patch call.respond(HttpStatusCode.Unauthorized)

                    val studentDbId = call.parameters["id"]?.toIntOrNull()
                        ?: return@patch call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

                    val updated = transaction {
                        val student = ProfessionalStudent.findById(studentDbId)
                            ?: return@transaction false
                        if (student.professional.id.value != professionalId) return@transaction false
                        student.isActive = false
                        true
                    }

                    if (updated) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Aluno inativado"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Aluno não encontrado"))
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro: ${e.message}"))
                }
            }

            // ========================================
            // PATCH /api/personal/students/{id}/activate
            // Reativar aluno
            // ========================================
            patch("/{id}/activate") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                        ?: return@patch call.respond(HttpStatusCode.Unauthorized)

                    val studentDbId = call.parameters["id"]?.toIntOrNull()
                        ?: return@patch call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

                    val updated = transaction {
                        val student = ProfessionalStudent.findById(studentDbId)
                            ?: return@transaction false
                        if (student.professional.id.value != professionalId) return@transaction false
                        student.isActive = true
                        true
                    }

                    if (updated) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Aluno reativado"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Aluno não encontrado"))
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro: ${e.message}"))
                }
            }

            // ========================================
            // DELETE /api/personal/students/{id}
            // Excluir aluno (permanente)
            // ========================================
            delete("/{id}") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                        ?: return@delete call.respond(HttpStatusCode.Unauthorized)

                    val studentDbId = call.parameters["id"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

                    val deleted = transaction {
                        val student = ProfessionalStudent.findById(studentDbId)
                            ?: return@transaction false
                        if (student.professional.id.value != professionalId) return@transaction false
                        student.delete()
                        true
                    }

                    if (deleted) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Aluno excluído com sucesso"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Aluno não encontrado"))
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro: ${e.message}"))
                }
            }
        }
    }
}
