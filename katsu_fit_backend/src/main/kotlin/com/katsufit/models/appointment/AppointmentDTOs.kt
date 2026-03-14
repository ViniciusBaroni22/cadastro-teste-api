package com.katsufit.models.appointment

import kotlinx.serialization.Serializable

// ========================================
// REQUEST DTOs
// ========================================

@Serializable
data class CreateAppointmentRequest(
    val studentId: Int,
    val startDateTime: String, // ISO 8601: "2024-02-20T14:30:00"
    val durationMinutes: Int = 60,
    val type: String, // "ONLINE_VIDEO", "PRESENCIAL", etc
    val location: String? = null,
    val meetLink: String? = null,
    val notes: String? = null
)

@Serializable
data class UpdateAppointmentRequest(
    val startDateTime: String? = null,
    val durationMinutes: Int? = null,
    val type: String? = null,
    val location: String? = null,
    val meetLink: String? = null,
    val notes: String? = null
)

@Serializable
data class ConfirmAppointmentRequest(
    val confirmed: Boolean
)

@Serializable
data class CancelAppointmentRequest(
    val reason: String? = null
)

@Serializable
data class QuickNoteRequest(
    val notes: String
)

@Serializable
data class StartAppointmentRequest(
    val meetLink: String? = null
)

// ========================================
// RESPONSE DTOs
// ========================================

@Serializable
data class AppointmentResponse(
    val id: Int,
    val professionalId: Int,
    val studentId: Int,
    val studentName: String,
    val studentAvatar: String?,
    val startDateTime: String,
    val endDateTime: String,
    val durationMinutes: Int,
    val type: String,
    val status: String,
    val location: String?,
    val meetLink: String?,
    val notes: String?,
    val createdBy: String,
    val waitingFor: String?,
    val canEdit: Boolean,
    val canCancel: Boolean,
    val isPast: Boolean
)

@Serializable
data class AppointmentListResponse(
    val appointments: List<AppointmentResponse>,
    val totalCount: Int,
    val pendingCount: Int,
    val confirmedCount: Int,
    val todayCount: Int
)

@Serializable
data class AvailableSlotResponse(
    val time: String,
    val available: Boolean,
    val conflictingAppointmentId: Int? = null
)
