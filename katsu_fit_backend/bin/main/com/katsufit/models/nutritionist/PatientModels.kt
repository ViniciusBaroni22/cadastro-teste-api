package com.katsufit.models.nutritionist

import com.katsufit.models.shared.*
import kotlinx.serialization.Serializable

// ========== MODELOS EXISTENTES (mantém tudo) ==========

@Serializable
data class PatientInviteRequest(
    val email: String
)

@Serializable
data class PatientDTO(
    val id: String,
    val name: String,
    val email: String
)

@Serializable
data class PatientRegistrationRequest(
    val email: String,
    val name: String,
    val birthDate: String,
    val phone: String,
    val gender: String,
    val weight: Double,
    val height: Double,
    val bodyFatPercentage: Double?,
    val goal: String,
    val allergies: String?,
    val medications: String?,
    val healthConditions: String?,
    val notes: String?
)

@Serializable
data class PatientRegistrationResponse(
    val success: Boolean,
    val message: String,
    val patientId: String?
)

// ========== NOVO: MODELO PARA DETALHES COMPLETOS DO PACIENTE ==========

@Serializable
data class PatientDetailsDTO(
    // Dados Pessoais
    val id: String,
    val name: String,
    val email: String,
    val birthDate: String?,
    val phone: String?,
    val gender: String?,
    
    // Dados Antropométricos (últimos registrados)
    val weight: Double?,
    val height: Double?,
    val imc: Double?,           // Calculado automaticamente
    val bodyFatPercentage: Double?,
    val waist: Double?,
    val chest: Double?,
    val hips: Double?,
    
    // Objetivo
    val goal: String?,
    
    // Saúde
    val allergies: String?,
    val medications: String?,
    val healthConditions: String?,
    
    // Observações
    val notes: String?,
    
    // Data do último registro
    val lastUpdate: String?
)
