package com.katsufit.models.personal

import kotlinx.serialization.Serializable

// ========================================
// DTOs para Cadastro de Alunos (Personal)
// ========================================

@Serializable
data class StudentRegistrationRequest(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val birthDate: String? = null,
    val gender: String? = null,
    val age: Int? = null,
    val weight: Double? = null,
    val height: Double? = null,
    val bodyFatPercentage: Double? = null,
    val objective: String? = null,
    val healthConditions: String? = null,
    val injuries: String? = null,
    val experience: String? = null,
    val trainingFrequency: Int? = null,
    val notes: String? = null
)

@Serializable
data class StudentRegistrationResponse(
    val success: Boolean,
    val message: String,
    val studentId: Int? = null
)

@Serializable
data class StudentDetailsDTO(
    val id: Int,
    val studentId: Int,
    val name: String,
    val email: String?,
    val phone: String?,
    val birthDate: String?,
    val gender: String?,
    val age: Int?,
    val weight: Double?,
    val height: Double?,
    val bodyFatPercentage: Double?,
    val imc: Double?,
    val objective: String?,
    val healthConditions: String?,
    val injuries: String?,
    val experience: String?,
    val trainingFrequency: Int?,
    val notes: String?,
    val isActive: Boolean,
    val hasActiveCredit: Boolean,
    val creditExpiresAt: String?,
    val createdAt: String
)

@Serializable
data class UpdateStudentRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val birthDate: String? = null,
    val gender: String? = null,
    val age: Int? = null,
    val weight: Double? = null,
    val height: Double? = null,
    val bodyFatPercentage: Double? = null,
    val objective: String? = null,
    val healthConditions: String? = null,
    val injuries: String? = null,
    val experience: String? = null,
    val trainingFrequency: Int? = null,
    val notes: String? = null
)
