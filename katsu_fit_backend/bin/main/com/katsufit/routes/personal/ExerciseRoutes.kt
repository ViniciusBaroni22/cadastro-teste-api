package com.katsufit.routes.personal

import com.katsufit.models.personal.*
import com.katsufit.models.personal.exercise.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.exerciseRoutes() {

    // ==========================================
    // EXERCÍCIOS PADRÃO (Públicos, somente leitura)
    // ==========================================

    authenticate("auth-jwt") {

        // Listar exercícios padrão (com filtro opcional)
        get("/api/exercises/default") {
            val muscleGroup = call.request.queryParameters["muscleGroup"]
            val category = call.request.queryParameters["category"]
            val search = call.request.queryParameters["search"]

            val exercises = transaction {
                var query = DefaultExercises.selectAll()

                if (!muscleGroup.isNullOrBlank()) {
                    query = query.andWhere { DefaultExercises.muscleGroup eq muscleGroup }
                }
                if (!category.isNullOrBlank()) {
                    query = query.andWhere { DefaultExercises.category eq category }
                }
                if (!search.isNullOrBlank()) {
                    query = query.andWhere {
                        DefaultExercises.name.lowerCase() like "%${search.lowercase()}%"
                    }
                }

                query.orderBy(DefaultExercises.muscleGroup to SortOrder.ASC, DefaultExercises.name to SortOrder.ASC)
                    .map { row ->
                        ExerciseResponse(
                            id = row[DefaultExercises.id].value,
                            name = row[DefaultExercises.name],
                            description = row[DefaultExercises.description],
                            muscleGroup = row[DefaultExercises.muscleGroup],
                            category = row[DefaultExercises.category],
                            difficulty = row[DefaultExercises.difficulty],
                            videoUrl = row[DefaultExercises.videoUrl],
                            thumbnailUrl = row[DefaultExercises.thumbnailUrl],
                            instructions = row[DefaultExercises.instructions],
                            equipment = row[DefaultExercises.equipment],
                            isDefault = true
                        )
                    }
            }

            call.respond(exercises)
        }

        // Detalhe de um exercício padrão
        get("/api/exercises/default/{id}") {
            val exerciseId = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val exercise = transaction {
                DefaultExercise.findById(exerciseId)?.let { ex ->
                    ExerciseResponse(
                        id = ex.id.value,
                        name = ex.name,
                        description = ex.description,
                        muscleGroup = ex.muscleGroup,
                        category = ex.category,
                        difficulty = ex.difficulty,
                        videoUrl = ex.videoUrl,
                        thumbnailUrl = ex.thumbnailUrl,
                        instructions = ex.instructions,
                        equipment = ex.equipment,
                        isDefault = true
                    )
                }
            }

            if (exercise == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Exercício não encontrado"))
            } else {
                call.respond(exercise)
            }
        }

        // ==========================================
        // EXERCÍCIOS CUSTOM (Do Personal Trainer)
        // ==========================================

        // Listar exercícios custom do personal
        get("/api/personal/exercises") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val muscleGroup = call.request.queryParameters["muscleGroup"]
            val search = call.request.queryParameters["search"]

            val exercises = transaction {
                var query = CustomExercises.selectAll()
                    .andWhere { CustomExercises.professionalId eq professionalId }

                if (!muscleGroup.isNullOrBlank()) {
                    query = query.andWhere { CustomExercises.muscleGroup eq muscleGroup }
                }
                if (!search.isNullOrBlank()) {
                    query = query.andWhere {
                        CustomExercises.name.lowerCase() like "%${search.lowercase()}%"
                    }
                }

                query.orderBy(CustomExercises.createdAt to SortOrder.DESC)
                    .map { row ->
                        ExerciseResponse(
                            id = row[CustomExercises.id].value,
                            name = row[CustomExercises.name],
                            description = row[CustomExercises.description],
                            muscleGroup = row[CustomExercises.muscleGroup],
                            category = row[CustomExercises.category],
                            difficulty = row[CustomExercises.difficulty],
                            videoUrl = row[CustomExercises.videoUrl],
                            thumbnailUrl = row[CustomExercises.thumbnailUrl],
                            instructions = row[CustomExercises.instructions],
                            equipment = row[CustomExercises.equipment],
                            isDefault = false,
                            mediaType = row[CustomExercises.mediaType],
                            createdAt = row[CustomExercises.createdAt].toString()
                        )
                    }
            }

            call.respond(exercises)
        }

        // Detalhe de um exercício custom
        get("/api/personal/exercises/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val exerciseId = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val exercise = transaction {
                CustomExercise.find {
                    (CustomExercises.id eq exerciseId) and (CustomExercises.professionalId eq professionalId)
                }.firstOrNull()?.let { ex ->
                    ExerciseResponse(
                        id = ex.id.value,
                        name = ex.name,
                        description = ex.description,
                        muscleGroup = ex.muscleGroup,
                        category = ex.category,
                        difficulty = ex.difficulty,
                        videoUrl = ex.videoUrl,
                        thumbnailUrl = ex.thumbnailUrl,
                        instructions = ex.instructions,
                        equipment = ex.equipment,
                        isDefault = false,
                        mediaType = ex.mediaType,
                        createdAt = ex.createdAt.toString()
                    )
                }
            }

            if (exercise == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Exercício não encontrado"))
            } else {
                call.respond(exercise)
            }
        }

        // Criar exercício custom
        post("/api/personal/exercises") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val request = call.receive<CreateCustomExerciseRequest>()

            if (request.name.isBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Nome é obrigatório"))
            }
            if (request.muscleGroup.isBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Grupo muscular é obrigatório"))
            }

            val exerciseId = transaction {
                CustomExercise.new {
                    this.professionalId = org.jetbrains.exposed.dao.id.EntityID(professionalId, Professionals)
                    this.name = request.name
                    this.description = request.description
                    this.muscleGroup = request.muscleGroup
                    this.category = request.category
                    this.difficulty = request.difficulty
                    this.videoUrl = request.videoUrl
                    this.thumbnailUrl = request.thumbnailUrl
                    this.instructions = request.instructions
                    this.equipment = request.equipment
                    this.mediaType = request.mediaType
                }.id.value
            }

            call.respond(HttpStatusCode.Created, mapOf("id" to exerciseId, "message" to "Exercício criado com sucesso"))
        }

        // Atualizar exercício custom
        put("/api/personal/exercises/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val exerciseId = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val request = call.receive<UpdateCustomExerciseRequest>()

            val success = transaction {
                val exercise = CustomExercise.find {
                    (CustomExercises.id eq exerciseId) and (CustomExercises.professionalId eq professionalId)
                }.firstOrNull() ?: return@transaction false

                request.name?.let { exercise.name = it }
                request.description?.let { exercise.description = it }
                request.muscleGroup?.let { exercise.muscleGroup = it }
                request.category?.let { exercise.category = it }
                request.difficulty?.let { exercise.difficulty = it }
                request.videoUrl?.let { exercise.videoUrl = it }
                request.thumbnailUrl?.let { exercise.thumbnailUrl = it }
                request.instructions?.let { exercise.instructions = it }
                request.equipment?.let { exercise.equipment = it }
                request.mediaType?.let { exercise.mediaType = it }
                exercise.updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

                true
            }

            if (success) call.respond(HttpStatusCode.OK, mapOf("message" to "Exercício atualizado"))
            else call.respond(HttpStatusCode.NotFound, mapOf("error" to "Exercício não encontrado"))
        }

        // Deletar exercício custom
        delete("/api/personal/exercises/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val exerciseId = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val deleted = transaction {
                CustomExercise.find {
                    (CustomExercises.id eq exerciseId) and (CustomExercises.professionalId eq professionalId)
                }.firstOrNull()?.delete() != null
            }

            if (deleted) call.respond(HttpStatusCode.OK, mapOf("message" to "Exercício removido"))
            else call.respond(HttpStatusCode.NotFound, mapOf("error" to "Exercício não encontrado"))
        }
    }
}
