package com.katsufit.routes.personal

import com.katsufit.models.personal.*
import com.katsufit.models.personal.workout.*
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

fun Route.workoutRoutes() {
    authenticate("auth-jwt") {

        // ==========================================
        // BIBLIOTECA DO PERSONAL (MÓDULOS/TEMPLATES)
        // ==========================================

        // Criar novo módulo na biblioteca
        post("/api/personal/workout/modules") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val request = call.receive<CreateModuleRequest>()

            val moduleId = transaction {
                val module = WorkoutModule.new {
                    this.professionalId = org.jetbrains.exposed.dao.id.EntityID(professionalId, Professionals)
                    this.name = request.name
                    this.description = request.description
                    this.category = request.category
                    this.estimatedDuration = request.estimatedDuration
                }

                request.exercises.forEach { ex ->
                    WorkoutModuleExercise.new {
                        this.moduleId = module.id
                        this.exerciseId = ex.exerciseId
                        this.isDefaultExercise = ex.isDefaultExercise
                        this.exerciseName = ex.exerciseName
                        this.exerciseGifUrl = ex.exerciseGifUrl
                        this.muscleGroup = ex.muscleGroup
                        this.sets = ex.sets
                        this.reps = ex.reps
                        this.restSeconds = ex.restSeconds
                        this.technique = ex.technique
                        this.notes = ex.notes
                        this.orderIndex = ex.orderIndex
                    }
                }
                module.id.value
            }

            call.respond(HttpStatusCode.Created, mapOf("id" to moduleId, "message" to "Módulo criado com sucesso"))
        }

        // Listar módulos da biblioteca
        get("/api/personal/workout/modules") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val modules = transaction {
                WorkoutModule.find { WorkoutModules.professionalId eq professionalId }
                    .orderBy(WorkoutModules.updatedAt to SortOrder.DESC)
                    .map {
                        WorkoutModuleResponse(
                            id = it.id.value,
                            name = it.name,
                            description = it.description,
                            category = it.category,
                            estimatedDuration = it.estimatedDuration,
                            exercisesCount = it.exercises.count().toInt(),
                            createdAt = it.createdAt.toString()
                        )
                    }
            }

            call.respond(modules)
        }

        // Detalhes de um módulo
        get("/api/personal/workout/modules/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val moduleId = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val module = transaction {
                val mod = WorkoutModule.find {
                    (WorkoutModules.id eq moduleId) and (WorkoutModules.professionalId eq professionalId)
                }.firstOrNull() ?: return@transaction null

                WorkoutModuleDetailResponse(
                    id = mod.id.value,
                    name = mod.name,
                    description = mod.description,
                    category = mod.category,
                    estimatedDuration = mod.estimatedDuration,
                    exercises = mod.exercises.sortedBy { it.orderIndex }.map { ex ->
                        ModuleExerciseResponse(
                            id = ex.id.value,
                            exerciseId = ex.exerciseId,
                            isDefaultExercise = ex.isDefaultExercise,
                            exerciseName = ex.exerciseName,
                            exerciseGifUrl = ex.exerciseGifUrl,
                            muscleGroup = ex.muscleGroup,
                            sets = ex.sets,
                            reps = ex.reps,
                            restSeconds = ex.restSeconds,
                            technique = ex.technique,
                            notes = ex.notes,
                            orderIndex = ex.orderIndex
                        )
                    },
                    createdAt = mod.createdAt.toString(),
                    updatedAt = mod.updatedAt.toString()
                )
            }

            if (module == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Módulo não encontrado"))
            } else {
                call.respond(module)
            }
        }

        // Atualizar módulo da biblioteca
        put("/api/personal/workout/modules/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val moduleId = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val request = call.receive<CreateModuleRequest>()

            val success = transaction {
                val module = WorkoutModule.find {
                    (WorkoutModules.id eq moduleId) and (WorkoutModules.professionalId eq professionalId)
                }.firstOrNull() ?: return@transaction false

                module.name = request.name
                module.description = request.description
                module.category = request.category
                module.estimatedDuration = request.estimatedDuration
                module.updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

                // Deletar exercícios antigos e criar novos
                WorkoutModuleExercises.deleteWhere { WorkoutModuleExercises.moduleId eq moduleId }

                request.exercises.forEach { ex ->
                    WorkoutModuleExercise.new {
                        this.moduleId = module.id
                        this.exerciseId = ex.exerciseId
                        this.isDefaultExercise = ex.isDefaultExercise
                        this.exerciseName = ex.exerciseName
                        this.exerciseGifUrl = ex.exerciseGifUrl
                        this.muscleGroup = ex.muscleGroup
                        this.sets = ex.sets
                        this.reps = ex.reps
                        this.restSeconds = ex.restSeconds
                        this.technique = ex.technique
                        this.notes = ex.notes
                        this.orderIndex = ex.orderIndex
                    }
                }
                true
            }

            if (success) call.respond(HttpStatusCode.OK, mapOf("message" to "Módulo atualizado"))
            else call.respond(HttpStatusCode.NotFound, mapOf("error" to "Módulo não encontrado"))
        }

        // Deletar módulo da biblioteca
        delete("/api/personal/workout/modules/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val moduleId = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val deleted = transaction {
                WorkoutModule.find {
                    (WorkoutModules.id eq moduleId) and (WorkoutModules.professionalId eq professionalId)
                }.firstOrNull()?.delete() != null
            }

            if (deleted) call.respond(HttpStatusCode.OK, mapOf("message" to "Módulo deletado"))
            else call.respond(HttpStatusCode.NotFound, mapOf("error" to "Módulo não encontrado"))
        }

        // ==========================================
        // VINCULAÇÃO AOS ALUNOS (STAGING)
        // ==========================================

        // Vincular módulo a alunos (cria PENDING)
        post("/api/personal/workout/assign") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val request = call.receive<AssignWorkoutRequest>()

            val result = transaction {
                // Buscar módulo
                val module = WorkoutModule.find {
                    (WorkoutModules.id eq request.moduleId) and
                            (WorkoutModules.professionalId eq professionalId)
                }.firstOrNull() ?: return@transaction null

                val successList = mutableListOf<Int>()
                val failedList = mutableListOf<BatchFailure>()

                request.studentIds.forEach { studentId ->
                    try {
                        val student = ProfessionalStudent.findById(studentId)
                        if (student == null) {
                            failedList.add(BatchFailure(studentId, "Aluno não encontrado"))
                            return@forEach
                        }

                        if (student.professional.id.value != professionalId) {
                            failedList.add(BatchFailure(studentId, "Aluno não pertence a este profissional"))
                            return@forEach
                        }

                        // Criar StudentWorkout (cópia do módulo)
                        val studentWorkout = StudentWorkout.new {
                            this.professionalId = org.jetbrains.exposed.dao.id.EntityID(professionalId, Professionals)
                            this.studentId = org.jetbrains.exposed.dao.id.EntityID(studentId, ProfessionalStudents)
                            this.moduleId = module.id
                            this.name = request.customName ?: module.name
                            this.description = module.description
                            this.dayOfWeek = request.dayOfWeek
                            this.status = WorkoutStatus.PENDING
                        }

                        // Copiar exercícios (independente do módulo)
                        module.exercises.forEach { ex ->
                            StudentWorkoutExercise.new {
                                this.studentWorkoutId = studentWorkout.id
                                this.exerciseName = ex.exerciseName
                                this.exerciseGifUrl = ex.exerciseGifUrl
                                this.muscleGroup = ex.muscleGroup
                                this.sets = ex.sets
                                this.reps = ex.reps
                                this.restSeconds = ex.restSeconds
                                this.technique = ex.technique
                                this.notes = ex.notes
                                this.orderIndex = ex.orderIndex
                            }
                        }

                        successList.add(studentWorkout.id.value)
                    } catch (e: Exception) {
                        failedList.add(BatchFailure(studentId, e.message ?: "Erro desconhecido"))
                    }
                }

                BatchAssignResponse(successList, failedList)
            }

            if (result == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Módulo não encontrado"))
            } else {
                call.respond(HttpStatusCode.Created, result)
            }
        }

        // Listar treinos de um aluno
        get("/api/personal/workout/student/{studentId}") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val studentId = call.parameters["studentId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val statusFilter = call.request.queryParameters["status"]?.uppercase()?.let {
                try { WorkoutStatus.valueOf(it) } catch (e: Exception) { null }
            }

            val workouts = transaction {
                val student = ProfessionalStudent.findById(studentId)
                    ?: return@transaction null

                if (student.professional.id.value != professionalId) return@transaction null

                val query = StudentWorkout.find {
                    (StudentWorkouts.studentId eq studentId) and
                            (StudentWorkouts.professionalId eq professionalId)
                }.let { base ->
                    if (statusFilter != null) base.filter { it.status == statusFilter }
                    else base.toList()
                }

                query.map { w ->
                    StudentWorkoutResponse(
                        id = w.id.value,
                        studentId = studentId,
                        studentName = student.studentName,
                        moduleId = w.moduleId?.value,
                        name = w.name,
                        description = w.description,
                        dayOfWeek = w.dayOfWeek,
                        status = w.status.name,
                        createdAt = w.createdAt.toString(),
                        publishedAt = w.publishedAt?.toString()
                    )
                }
            }

            if (workouts == null) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Sem permissão para ver este aluno"))
            } else {
                call.respond(workouts)
            }
        }

        // Detalhes de um treino do aluno
        get("/api/personal/workout/student-workout/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val workoutId = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val workout = transaction {
                val w = StudentWorkout.findById(workoutId) ?: return@transaction null
                if (w.professionalId.value != professionalId) return@transaction null

                val student = ProfessionalStudent.findById(w.studentId.value)
                    ?: return@transaction null

                StudentWorkoutDetailResponse(
                    id = w.id.value,
                    studentId = w.studentId.value,
                    studentName = student.studentName,
                    studentAvatar = student.studentAvatar,
                    moduleId = w.moduleId?.value,
                    name = w.name,
                    description = w.description,
                    dayOfWeek = w.dayOfWeek,
                    status = w.status.name,
                    exercises = w.exercises.sortedBy { it.orderIndex }.map { ex ->
                        StudentExerciseResponse(
                            id = ex.id.value,
                            exerciseName = ex.exerciseName,
                            exerciseGifUrl = ex.exerciseGifUrl,
                            muscleGroup = ex.muscleGroup,
                            sets = ex.sets,
                            reps = ex.reps,
                            weight = ex.weight,
                            restSeconds = ex.restSeconds,
                            technique = ex.technique,
                            notes = ex.notes,
                            orderIndex = ex.orderIndex
                        )
                    },
                    createdAt = w.createdAt.toString(),
                    updatedAt = w.updatedAt.toString()
                )
            }

            if (workout == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Treino não encontrado"))
            } else {
                call.respond(workout)
            }
        }

        // Editar treino do aluno (ajustes no staging)
        put("/api/personal/workout/student-workout/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val workoutId = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val request = call.receive<UpdateStudentWorkoutRequest>()

            val success = transaction {
                val workout = StudentWorkout.findById(workoutId)
                    ?: return@transaction false
                if (workout.professionalId.value != professionalId) return@transaction false

                request.name?.let { workout.name = it }
                request.description?.let { workout.description = it }
                request.dayOfWeek?.let { workout.dayOfWeek = it }
                workout.updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

                request.exercises?.forEach { exReq ->
                    val ex = StudentWorkoutExercise.findById(exReq.id) ?: return@forEach
                    if (ex.studentWorkoutId.value != workoutId) return@forEach

                    exReq.sets?.let { ex.sets = it }
                    exReq.reps?.let { ex.reps = it }
                    exReq.weight?.let { ex.weight = it }
                    exReq.restSeconds?.let { ex.restSeconds = it }
                    exReq.technique?.let { ex.technique = it }
                    exReq.notes?.let { ex.notes = it }
                }

                true
            }

            if (success) call.respond(HttpStatusCode.OK, mapOf("message" to "Treino atualizado"))
            else call.respond(HttpStatusCode.NotFound, mapOf("error" to "Treino não encontrado ou sem permissão"))
        }

        // PUBLICAR treino (PENDING -> ACTIVE)
        post("/api/personal/workout/student-workout/{id}/publish") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val workoutId = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val published = transaction {
                val workout = StudentWorkout.findById(workoutId)
                    ?: return@transaction false
                if (workout.professionalId.value != professionalId) return@transaction false
                if (workout.status != WorkoutStatus.PENDING) return@transaction false

                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                workout.status = WorkoutStatus.ACTIVE
                workout.publishedAt = now
                workout.updatedAt = now
                true
            }

            if (published) {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Treino publicado com sucesso", "status" to "ACTIVE"))
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Não foi possível publicar. Verifique se o treino existe e está pendente."))
            }
        }

        // ARQUIVAR treino (ACTIVE -> ARCHIVED)
        post("/api/personal/workout/student-workout/{id}/archive") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val workoutId = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val success = transaction {
                val workout = StudentWorkout.findById(workoutId)
                    ?: return@transaction false
                if (workout.professionalId.value != professionalId) return@transaction false

                workout.status = WorkoutStatus.ARCHIVED
                workout.updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                true
            }

            if (success) call.respond(HttpStatusCode.OK, mapOf("message" to "Treino arquivado"))
            else call.respond(HttpStatusCode.NotFound, mapOf("error" to "Treino não encontrado"))
        }

        // Deletar treino (só PENDING ou ARCHIVED)
        delete("/api/personal/workout/student-workout/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val workoutId = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val deleted = transaction {
                val workout = StudentWorkout.findById(workoutId)
                    ?: return@transaction false
                if (workout.professionalId.value != professionalId) return@transaction false
                if (workout.status == WorkoutStatus.ACTIVE) return@transaction false

                workout.delete()
                true
            }

            if (deleted) call.respond(HttpStatusCode.OK, mapOf("message" to "Treino removido"))
            else call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Não foi possível remover. Treinos ativos não podem ser deletados."))
        }
    }
}
