package com.katsufit.models.personal.exercise

import kotlinx.serialization.Serializable

// ==========================================
// RESPONSE: Exercício (padrão ou custom)
// ==========================================
@Serializable
data class ExerciseResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val muscleGroup: String,
    val category: String?,
    val difficulty: String?,
    val videoUrl: String?,
    val thumbnailUrl: String?,
    val instructions: String?,
    val equipment: String?,
    val isDefault: Boolean,
    val mediaType: String? = null,
    val createdAt: String? = null
)

// ==========================================
// REQUEST: Criar Exercício Custom
// ==========================================
@Serializable
data class CreateCustomExerciseRequest(
    val name: String,
    val description: String? = null,
    val muscleGroup: String,
    val category: String? = null,
    val difficulty: String? = null,
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val instructions: String? = null,
    val equipment: String? = null,
    val mediaType: String? = null // VIDEO_UPLOAD, VIDEO_LINK
)

// ==========================================
// REQUEST: Atualizar Exercício Custom
// ==========================================
@Serializable
data class UpdateCustomExerciseRequest(
    val name: String? = null,
    val description: String? = null,
    val muscleGroup: String? = null,
    val category: String? = null,
    val difficulty: String? = null,
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val instructions: String? = null,
    val equipment: String? = null,
    val mediaType: String? = null
)
