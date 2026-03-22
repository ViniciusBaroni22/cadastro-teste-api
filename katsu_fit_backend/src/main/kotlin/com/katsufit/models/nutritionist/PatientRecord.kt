package com.katsufit.models.nutritionist

import com.katsufit.models.shared.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// ============================
// TABELA PATIENT_RECORDS
// Dados pessoais e de saúde do paciente (1 por paciente)
// ============================
object PatientRecords : Table("patient_records") {
    val id = integer("id").autoIncrement()
    val patientId = varchar("patient_id", 36)
    val nutritionistId = varchar("nutritionist_id", 36)

    // Dados Pessoais
    val birthDate = varchar("birth_date", 20).nullable()
    val phone = varchar("phone", 20).nullable()
    val gender = varchar("gender", 10).nullable()

    // Dados Antropométricos (valores atuais)
    val height = double("height").nullable()           // cm
    val weight = double("weight").nullable()            // kg
    val bodyFatPercentage = double("body_fat_percentage").nullable() // %

    // Objetivo
    val goal = varchar("goal", 100).nullable()

    // Saúde
    val allergies = text("allergies").nullable()
    val medications = text("medications").nullable()
    val healthConditions = text("health_conditions").nullable()

    // Observações
    val notes = text("notes").nullable()

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}

// ============================
// DTOs
// ============================

@Serializable
data class PatientRecordDTO(
    val id: Int? = null,
    val patientId: String? = null,
    val nutritionistId: String? = null,

    // Pessoais
    val birthDate: String? = null,
    val phone: String? = null,
    val gender: String? = null,

    // Antropométricos
    val height: Double? = null,
    val weight: Double? = null,
    val bodyFatPercentage: Double? = null,
    val imc: Double? = null,  // calculado

    // Objetivo
    val goal: String? = null,

    // Saúde
    val allergies: String? = null,
    val medications: String? = null,
    val healthConditions: String? = null,

    // Obs
    val notes: String? = null,

    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class UpdatePatientRecordRequest(
    val birthDate: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    val height: Double? = null,
    val weight: Double? = null,
    val bodyFatPercentage: Double? = null,
    val goal: String? = null,
    val allergies: String? = null,
    val medications: String? = null,
    val healthConditions: String? = null,
    val notes: String? = null
)
