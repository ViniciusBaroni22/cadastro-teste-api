package com.katsufit.models.nutritionist

import com.katsufit.models.shared.Users
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object PatientAppointments : UUIDTable("patient_appointments") {
    val patientId = reference("patient_id", Users, onDelete = ReferenceOption.CASCADE)
    val nutritionistId = reference("nutritionist_id", Users, onDelete = ReferenceOption.CASCADE)
    val scheduledAt = datetime("scheduled_at")
    val type = varchar("type", 50) // e.g., ONLINE, PRESENCIAL, RETORNO
    val status = varchar("status", 50).default("SCHEDULED") // SCHEDULED, COMPLETED, CANCELLED
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

@Serializable
data class PatientAppointmentDTO(
    val id: String,
    val patientId: String,
    val nutritionistId: String,
    val scheduledAt: String,
    val type: String,
    val status: String,
    val notes: String?,
    val createdAt: String
)

@Serializable
data class CreatePatientAppointmentRequest(
    val patientId: String,
    val scheduledAt: String, // ISO-8601 format
    val type: String,
    val notes: String? = null
)

@Serializable
data class UpdatePatientAppointmentStatusRequest(
    val status: String
)
