package com.katsufit.models.personal.workout

import kotlinx.serialization.Serializable

// ==================== BIBLIOTECA (MÓDULOS) ====================

@Serializable
data class CreateModuleRequest(
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val estimatedDuration: Int? = null,
    val exercises: List<ModuleExerciseRequest>
)

@Serializable
data class ModuleExerciseRequest(
    val exerciseId: Int? = null,
    val isDefaultExercise: Boolean? = null,
    val exerciseName: String,
    val exerciseGifUrl: String? = null,
    val muscleGroup: String? = null,
    val sets: Int,
    val reps: String,
    val restSeconds: Int? = null,
    val technique: String? = null,
    val notes: String? = null,
    val orderIndex: Int
)

@Serializable
data class WorkoutModuleResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val category: String?,
    val estimatedDuration: Int?,
    val exercisesCount: Int,
    val createdAt: String
)

@Serializable
data class WorkoutModuleDetailResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val category: String?,
    val estimatedDuration: Int?,
    val exercises: List<ModuleExerciseResponse>,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ModuleExerciseResponse(
    val id: Int,
    val exerciseId: Int? = null,
    val isDefaultExercise: Boolean? = null,
    val exerciseName: String,
    val exerciseGifUrl: String?,
    val muscleGroup: String?,
    val sets: Int,
    val reps: String,
    val restSeconds: Int?,
    val technique: String?,
    val notes: String?,
    val orderIndex: Int
)

// ==================== VINCULAÇÃO/STAGING ====================

@Serializable
data class AssignWorkoutRequest(
    val moduleId: Int,
    val studentIds: List<Int>,
    val dayOfWeek: String? = null,   // "MONDAY", "TUESDAY", etc
    val customName: String? = null
)

@Serializable
data class UpdateStudentWorkoutRequest(
    val name: String? = null,
    val description: String? = null,
    val dayOfWeek: String? = null,
    val exercises: List<UpdateStudentExerciseRequest>? = null
)

@Serializable
data class UpdateStudentExerciseRequest(
    val id: Int,
    val sets: Int? = null,
    val reps: String? = null,
    val weight: String? = null,
    val restSeconds: Int? = null,
    val technique: String? = null,
    val notes: String? = null
)

@Serializable
data class StudentWorkoutResponse(
    val id: Int,
    val studentId: Int,
    val studentName: String,
    val moduleId: Int?,
    val name: String,
    val description: String?,
    val dayOfWeek: String?,
    val status: String,
    val createdAt: String,
    val publishedAt: String?
)

@Serializable
data class StudentWorkoutDetailResponse(
    val id: Int,
    val studentId: Int,
    val studentName: String,
    val studentAvatar: String?,
    val moduleId: Int?,
    val name: String,
    val description: String?,
    val dayOfWeek: String?,
    val status: String,
    val exercises: List<StudentExerciseResponse>,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class StudentExerciseResponse(
    val id: Int,
    val exerciseName: String,
    val exerciseGifUrl: String?,
    val muscleGroup: String?,
    val sets: Int,
    val reps: String,
    val weight: String?,
    val restSeconds: Int?,
    val technique: String?,
    val notes: String?,
    val orderIndex: Int
)

// ==================== RESPOSTAS BATCH ====================

@Serializable
data class BatchAssignResponse(
    val success: List<Int>,
    val failed: List<BatchFailure>
)

@Serializable
data class BatchFailure(
    val studentId: Int,
    val error: String
)

@Serializable
data class PublishWorkoutRequest(
    val notifyStudent: Boolean = true
)
