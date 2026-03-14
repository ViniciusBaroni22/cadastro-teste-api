package com.katsufit.models.appointment

import com.katsufit.models.personal.ProfessionalStudents
import com.katsufit.models.personal.Professionals
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

// ========================================
// TABELA DE CONSULTAS/AGENDAMENTOS
// ========================================
object Appointments : IntIdTable("appointments") {
    val professionalId = reference("professional_id", Professionals, onDelete = ReferenceOption.CASCADE)
    val studentId = reference("student_id", ProfessionalStudents, onDelete = ReferenceOption.CASCADE)
    
    val startDateTime = datetime("start_datetime")
    val durationMinutes = integer("duration_minutes").default(60)
    val endDateTime = datetime("end_datetime")
    
    val type = enumeration("type", AppointmentType::class)
    val status = enumeration("status", AppointmentStatus::class).default(AppointmentStatus.PENDING)
    
    val location = varchar("location", 255).nullable()
    val meetLink = varchar("meet_link", 500).nullable()
    val notes = text("notes").nullable()
    
    val createdBy = enumeration("created_by", UserType::class)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
    
    // Confirmações
    val professionalConfirmedAt = datetime("professional_confirmed_at").nullable()
    val studentConfirmedAt = datetime("student_confirmed_at").nullable()
    
    // Cancelamento
    val cancelledBy = enumeration("cancelled_by", UserType::class).nullable()
    val cancellationReason = text("cancellation_reason").nullable()
    val cancelledAt = datetime("cancelled_at").nullable()
    
    // Lembretes
    val reminderSent24h = bool("reminder_sent_24h").default(false)
    val reminderSent1h = bool("reminder_sent_1h").default(false)
    
    init {
        index("idx_appointments_time", false, professionalId, startDateTime, endDateTime)
    }
}

// ========================================
// ENUMS
// ========================================
enum class AppointmentType {
    ONLINE_VIDEO,
    ONLINE_AUDIO,
    PRESENCIAL,
    HIBRIDO
}

enum class AppointmentStatus {
    PENDING,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}

enum class UserType {
    PROFESSIONAL,
    STUDENT
}

// ========================================
// TABELA DE DISPONIBILIDADE (futuro)
// ========================================
object ProfessionalAvailabilities : IntIdTable("professional_availabilities") {
    val professionalId = reference("professional_id", Professionals, onDelete = ReferenceOption.CASCADE)
    val dayOfWeek = integer("day_of_week") // 1=Seg, 7=Dom
    val startTime = varchar("start_time", 5) // "08:00"
    val endTime = varchar("end_time", 5)     // "18:00"
    val isAvailable = bool("is_available").default(true)
}

// ========================================
// DAO
// ========================================
class Appointment(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Appointment>(Appointments)
    
    var professionalId by Appointments.professionalId
    var studentId by Appointments.studentId
    var startDateTime by Appointments.startDateTime
    var durationMinutes by Appointments.durationMinutes
    var endDateTime by Appointments.endDateTime
    var type by Appointments.type
    var status by Appointments.status
    var location by Appointments.location
    var meetLink by Appointments.meetLink
    var notes by Appointments.notes
    var createdBy by Appointments.createdBy
    var createdAt by Appointments.createdAt
    var updatedAt by Appointments.updatedAt
    var professionalConfirmedAt by Appointments.professionalConfirmedAt
    var studentConfirmedAt by Appointments.studentConfirmedAt
    var cancelledBy by Appointments.cancelledBy
    var cancellationReason by Appointments.cancellationReason
    var cancelledAt by Appointments.cancelledAt
    var reminderSent24h by Appointments.reminderSent24h
    var reminderSent1h by Appointments.reminderSent1h
    
    fun isConfirmed(): Boolean = status == AppointmentStatus.CONFIRMED || 
                                status == AppointmentStatus.COMPLETED ||
                                status == AppointmentStatus.IN_PROGRESS
                                
    fun waitingFor(): UserType? {
        return when {
            status != AppointmentStatus.PENDING -> null
            createdBy == UserType.PROFESSIONAL && studentConfirmedAt == null -> UserType.STUDENT
            createdBy == UserType.STUDENT && professionalConfirmedAt == null -> UserType.PROFESSIONAL
            else -> null
        }
    }
}
