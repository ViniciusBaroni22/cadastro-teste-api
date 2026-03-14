package com.katsufit.models.personal.workout

import com.katsufit.models.personal.Professionals
import com.katsufit.models.personal.ProfessionalStudents
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

// ==========================================
// ENUM: Status do treino do aluno
// ==========================================
enum class WorkoutStatus {
    PENDING,    // Em staging, só personal vê
    ACTIVE,     // Publicado, aluno vê
    ARCHIVED    // Antigo, histórico
}

// ==========================================
// TABELA: Módulos da Biblioteca do Personal
// ==========================================
object WorkoutModules : IntIdTable("workout_modules") {
    val professionalId = reference("professional_id", Professionals, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val category = varchar("category", 50).nullable()
    val estimatedDuration = integer("estimated_duration_minutes").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

// ==========================================
// TABELA: Exercícios dentro de um Módulo
// ==========================================
object WorkoutModuleExercises : IntIdTable("workout_module_exercises") {
    val moduleId = reference("module_id", WorkoutModules, onDelete = ReferenceOption.CASCADE)
    val exerciseId = integer("exercise_id").nullable()
    val isDefaultExercise = bool("is_default_exercise").nullable()
    val exerciseName = varchar("exercise_name", 100)
    val exerciseGifUrl = varchar("exercise_gif_url", 255).nullable()
    val muscleGroup = varchar("muscle_group", 50).nullable()
    val sets = integer("sets")
    val reps = varchar("reps", 20)
    val restSeconds = integer("rest_seconds").nullable()
    val technique = varchar("technique", 100).nullable()
    val notes = text("notes").nullable()
    val orderIndex = integer("order_index")
}

// ==========================================
// TABELA: Treinos Vinculados aos Alunos
// ==========================================
object StudentWorkouts : IntIdTable("student_workouts") {
    val professionalId = reference("professional_id", Professionals, onDelete = ReferenceOption.CASCADE)
    val studentId = reference("student_id", ProfessionalStudents, onDelete = ReferenceOption.CASCADE)
    val moduleId = reference("module_id", WorkoutModules, onDelete = ReferenceOption.SET_NULL).nullable()
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val dayOfWeek = varchar("day_of_week", 10).nullable() // MONDAY, TUESDAY, etc
    val status = enumerationByName("status", 20, WorkoutStatus::class).default(WorkoutStatus.PENDING)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val publishedAt = datetime("published_at").nullable()
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

// ==========================================
// TABELA: Exercícios do Treino do Aluno
// ==========================================
object StudentWorkoutExercises : IntIdTable("student_workout_exercises") {
    val studentWorkoutId = reference("student_workout_id", StudentWorkouts, onDelete = ReferenceOption.CASCADE)
    val exerciseName = varchar("exercise_name", 100)
    val exerciseGifUrl = varchar("exercise_gif_url", 255).nullable()
    val muscleGroup = varchar("muscle_group", 50).nullable()
    val sets = integer("sets")
    val reps = varchar("reps", 20)
    val weight = varchar("weight", 20).nullable()
    val restSeconds = integer("rest_seconds").nullable()
    val technique = varchar("technique", 100).nullable()
    val notes = text("notes").nullable()
    val orderIndex = integer("order_index")
}

// ==========================================
// DAOs
// ==========================================

class WorkoutModule(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<WorkoutModule>(WorkoutModules)
    var professionalId by WorkoutModules.professionalId
    var name by WorkoutModules.name
    var description by WorkoutModules.description
    var category by WorkoutModules.category
    var estimatedDuration by WorkoutModules.estimatedDuration
    var createdAt by WorkoutModules.createdAt
    var updatedAt by WorkoutModules.updatedAt

    val exercises by WorkoutModuleExercise referrersOn WorkoutModuleExercises.moduleId
}

class WorkoutModuleExercise(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<WorkoutModuleExercise>(WorkoutModuleExercises)
    var moduleId by WorkoutModuleExercises.moduleId
    var exerciseId by WorkoutModuleExercises.exerciseId
    var isDefaultExercise by WorkoutModuleExercises.isDefaultExercise
    var exerciseName by WorkoutModuleExercises.exerciseName
    var exerciseGifUrl by WorkoutModuleExercises.exerciseGifUrl
    var muscleGroup by WorkoutModuleExercises.muscleGroup
    var sets by WorkoutModuleExercises.sets
    var reps by WorkoutModuleExercises.reps
    var restSeconds by WorkoutModuleExercises.restSeconds
    var technique by WorkoutModuleExercises.technique
    var notes by WorkoutModuleExercises.notes
    var orderIndex by WorkoutModuleExercises.orderIndex
}

class StudentWorkout(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StudentWorkout>(StudentWorkouts)
    var professionalId by StudentWorkouts.professionalId
    var studentId by StudentWorkouts.studentId
    var moduleId by StudentWorkouts.moduleId
    var name by StudentWorkouts.name
    var description by StudentWorkouts.description
    var dayOfWeek by StudentWorkouts.dayOfWeek
    var status by StudentWorkouts.status
    var createdAt by StudentWorkouts.createdAt
    var publishedAt by StudentWorkouts.publishedAt
    var updatedAt by StudentWorkouts.updatedAt

    val exercises by StudentWorkoutExercise referrersOn StudentWorkoutExercises.studentWorkoutId
}

class StudentWorkoutExercise(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StudentWorkoutExercise>(StudentWorkoutExercises)
    var studentWorkoutId by StudentWorkoutExercises.studentWorkoutId
    var exerciseName by StudentWorkoutExercises.exerciseName
    var exerciseGifUrl by StudentWorkoutExercises.exerciseGifUrl
    var muscleGroup by StudentWorkoutExercises.muscleGroup
    var sets by StudentWorkoutExercises.sets
    var reps by StudentWorkoutExercises.reps
    var weight by StudentWorkoutExercises.weight
    var restSeconds by StudentWorkoutExercises.restSeconds
    var technique by StudentWorkoutExercises.technique
    var notes by StudentWorkoutExercises.notes
    var orderIndex by StudentWorkoutExercises.orderIndex
}
