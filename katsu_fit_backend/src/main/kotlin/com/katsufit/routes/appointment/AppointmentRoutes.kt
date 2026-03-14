package com.katsufit.routes.appointment

import com.katsufit.models.appointment.*
import com.katsufit.models.personal.ProfessionalStudent
import com.katsufit.models.personal.ProfessionalStudents
import com.katsufit.models.personal.Professional
import com.katsufit.models.personal.Professionals
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun Route.appointmentRoutes() {
    authenticate("auth-jwt") {
        
        // ==================== CRUD CONSULTAS ====================
        
        // Criar nova consulta (Personal agendando para aluno)
        post("/api/appointments") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                
                val request = call.receive<CreateAppointmentRequest>()
                
                // Validar data futura
                val startDateTime = try {
                    LocalDateTime.parse(request.startDateTime)
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Data inválida. Use ISO 8601")
                }
                
                if (startDateTime.isBefore(LocalDateTime.now())) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Não é possível agendar no passado")
                }
                
                val endDateTime = startDateTime.plusMinutes(request.durationMinutes.toLong())
                
                transaction {
                    // Verificar se aluno pertence ao profissional
                    val student = ProfessionalStudent.findById(request.studentId)
                        ?: return@transaction null to "Aluno não encontrado"
                    
                    if (student.professional.id.value != professionalId) {
                        return@transaction null to "Aluno não pertence a este profissional"
                    }
                    
                    // Verificar conflito de horário
                    val conflict = Appointment.find {
                        (Appointments.professionalId eq professionalId) and
                        (Appointments.status neq AppointmentStatus.CANCELLED) and
                        (
                            (Appointments.startDateTime lessEq endDateTime) and
                            (Appointments.endDateTime greaterEq startDateTime)
                        )
                    }.firstOrNull()
                    
                    if (conflict != null) {
                        return@transaction null to "Conflito de horário com outra consulta"
                    }
                    
                    val appointment = Appointment.new {
                        this.professionalId = EntityID(professionalId, Professionals)
                        this.studentId = EntityID(request.studentId, ProfessionalStudents)
                        this.startDateTime = startDateTime
                        this.durationMinutes = request.durationMinutes
                        this.endDateTime = endDateTime
                        this.type = AppointmentType.valueOf(request.type)
                        this.status = AppointmentStatus.PENDING // Aguarda confirmação do aluno
                        this.location = request.location
                        this.meetLink = request.meetLink
                        this.notes = request.notes
                        this.createdBy = UserType.PROFESSIONAL
                        this.professionalConfirmedAt = LocalDateTime.now() // Personal já confirmou ao criar
                    }
                    
                    appointment.toResponse(UserType.PROFESSIONAL) to null
                }.let { (response, error) ->
                    if (error != null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to error))
                    } else if (response != null) {
                        // TODO: Enviar notificação push para o aluno
                        call.respond(HttpStatusCode.Created, response)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro interno: ${e.message}"))
            }
        }
        
        // Listar consultas do profissional (com filtros)
        get("/api/appointments") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val statusFilter = call.request.queryParameters["status"]?.let { 
                    try { AppointmentStatus.valueOf(it) } catch(e: Exception) { null }
                }
                val dateFrom = call.request.queryParameters["from"]?.let { 
                    try { LocalDate.parse(it) } catch(e: Exception) { null }
                }
                val dateTo = call.request.queryParameters["to"]?.let { 
                    try { LocalDate.parse(it) } catch(e: Exception) { null }
                }
                val studentId = call.request.queryParameters["studentId"]?.toIntOrNull()
                
                val appointments = transaction {
                    var query = Appointments.select { Appointments.professionalId eq professionalId }
                    
                    statusFilter?.let { query = query.andWhere { Appointments.status eq it } }
                    dateFrom?.let { query = query.andWhere { Appointments.startDateTime greaterEq it.atStartOfDay() } }
                    dateTo?.let { query = query.andWhere { Appointments.startDateTime lessEq it.plusDays(1).atStartOfDay() } }
                    studentId?.let { query = query.andWhere { Appointments.studentId eq it } }
                    
                    query.orderBy(Appointments.startDateTime to SortOrder.ASC)
                        .map {
                            val appointment = Appointment.wrapRow(it)
                            appointment.toResponse(UserType.PROFESSIONAL)
                        }
                }
                
                val counts = transaction {
                    mapOf(
                        "total" to Appointment.find { Appointments.professionalId eq professionalId }.count(),
                        "pending" to Appointment.find { 
                            (Appointments.professionalId eq professionalId) and 
                            (Appointments.status eq AppointmentStatus.PENDING) 
                        }.count(),
                        "confirmed" to Appointment.find { 
                            (Appointments.professionalId eq professionalId) and 
                            (Appointments.status eq AppointmentStatus.CONFIRMED) 
                        }.count(),
                        "today" to Appointment.find {
                            (Appointments.professionalId eq professionalId) and
                            (Appointments.startDateTime greaterEq LocalDate.now().atStartOfDay()) and
                            (Appointments.startDateTime lessEq LocalDate.now().plusDays(1).atStartOfDay())
                        }.count()
                    )
                }
                
                call.respond(AppointmentListResponse(
                    appointments = appointments,
                    totalCount = counts["total"]?.toInt() ?: 0,
                    pendingCount = counts["pending"]?.toInt() ?: 0,
                    confirmedCount = counts["confirmed"]?.toInt() ?: 0,
                    todayCount = counts["today"]?.toInt() ?: 0
                ))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao listar consultas: ${e.message}"))
            }
        }
        
        // Detalhes de uma consulta
        get("/api/appointments/{id}") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val appointmentId = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "ID inválido")
                
                val appointment = transaction {
                    val app = Appointment.findById(appointmentId)
                        ?: return@transaction null
                    
                    if (app.professionalId.value != professionalId) return@transaction null
                    
                    app.toResponse(UserType.PROFESSIONAL)
                }
                
                if (appointment == null) {
                    call.respond(HttpStatusCode.NotFound, "Consulta não encontrada")
                } else {
                    call.respond(appointment)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao obter detalhes: ${e.message}"))
            }
        }
        
        // Atualizar consulta (só se PENDING ou CONFIRMED e data futura)
        put("/api/appointments/{id}") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)
                
                val appointmentId = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "ID inválido")
                
                val request = call.receive<UpdateAppointmentRequest>()
                
                transaction {
                    val appointment = Appointment.findById(appointmentId)
                        ?: return@transaction false to "Consulta não encontrada"
                    
                    if (appointment.professionalId.value != professionalId) {
                        return@transaction false to "Sem permissão"
                    }
                    
                    if (appointment.status == AppointmentStatus.CANCELLED || 
                        appointment.status == AppointmentStatus.COMPLETED ||
                        appointment.startDateTime.isBefore(LocalDateTime.now())) {
                        return@transaction false to "Não é possível editar consultas passadas ou finalizadas"
                    }
                    
                    request.startDateTime?.let { newDateStr ->
                        val newDate = LocalDateTime.parse(newDateStr)
                        if (newDate.isBefore(LocalDateTime.now())) {
                            return@transaction false to "Não é possível agendar no passado"
                        }
                        appointment.startDateTime = newDate
                        request.durationMinutes?.let { mins ->
                            appointment.endDateTime = newDate.plusMinutes(mins.toLong())
                        } ?: run {
                            appointment.endDateTime = newDate.plusMinutes(appointment.durationMinutes.toLong())
                        }
                    }
                    
                    request.durationMinutes?.let { 
                        appointment.durationMinutes = it
                        // Recalcular endTime se não mudou startTime
                        if (request.startDateTime == null) {
                            val start = appointment.startDateTime
                            appointment.endDateTime = start.plusMinutes(it.toLong())
                        }
                    }
                    
                    request.type?.let { appointment.type = AppointmentType.valueOf(it) }
                    request.location?.let { appointment.location = it }
                    request.meetLink?.let { appointment.meetLink = it }
                    request.notes?.let { appointment.notes = it }
                    
                    appointment.updatedAt = LocalDateTime.now()
                    
                    true to null
                }.let { (success, error) ->
                    if (success) {
                        call.respond(HttpStatusCode.OK, "Consulta atualizada")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to error))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao atualizar: ${e.message}"))
            }
        }
        
        // Confirmar consulta (quando aluno criou e profissional precisa confirmar)
        post("/api/appointments/{id}/confirm") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                
                val appointmentId = call.parameters["id"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "ID inválido")
                
                transaction {
                    val appointment = Appointment.findById(appointmentId)
                        ?: return@transaction false to "Consulta não encontrada"
                    
                    if (appointment.professionalId.value != professionalId) {
                        return@transaction false to "Sem permissão"
                    }
                    
                    if (appointment.status != AppointmentStatus.PENDING) {
                        return@transaction false to "Consulta não está pendente"
                    }
                    
                    appointment.professionalConfirmedAt = LocalDateTime.now()
                    
                    // Se aluno já confirmou (criado por aluno), muda para CONFIRMED
                    if (appointment.studentConfirmedAt != null || appointment.createdBy == UserType.PROFESSIONAL) {
                        appointment.status = AppointmentStatus.CONFIRMED
                    }
                    
                    appointment.updatedAt = LocalDateTime.now()
                    
                    true to null
                }.let { (success, error) ->
                    if (success) {
                        // TODO: Notificar aluno que profissional confirmou
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Consulta confirmada", "status" to "CONFIRMED"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to error))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao confirmar: ${e.message}"))
            }
        }
        
        // Iniciar consulta (mudar para IN_PROGRESS)
        post("/api/appointments/{id}/start") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                
                val appointmentId = call.parameters["id"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "ID inválido")
                
                val request = try { call.receiveNullable<StartAppointmentRequest>() } catch(e: Exception) { null }
                
                transaction {
                    val appointment = Appointment.findById(appointmentId)
                        ?: return@transaction false to "Consulta não encontrada"
                    
                    if (appointment.professionalId.value != professionalId) {
                        return@transaction false to "Sem permissão"
                    }
                    
                    if (appointment.status !in listOf(AppointmentStatus.CONFIRMED, AppointmentStatus.PENDING)) {
                        return@transaction false to "Status inválido para iniciar"
                    }
                    
                    // Só pode iniciar se estiver dentro do horário (15min antes até duração + 15min depois)
                    val now = LocalDateTime.now()
                    val start = appointment.startDateTime
                    val end = appointment.endDateTime
                    
                    if (now.isBefore(start.minusMinutes(15)) || now.isAfter(end.plusMinutes(15))) {
                        return@transaction false to "Fora do horário permitido para iniciar"
                    }
                    
                    appointment.status = AppointmentStatus.IN_PROGRESS
                    request?.meetLink?.let { appointment.meetLink = it }
                    appointment.updatedAt = now
                    
                    true to null
                }.let { (success, error) ->
                    if (success) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Consulta iniciada", "status" to "IN_PROGRESS"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to error))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao iniciar: ${e.message}"))
            }
        }
        
        // Finalizar consulta (adicionar notas)
        post("/api/appointments/{id}/complete") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                
                val appointmentId = call.parameters["id"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "ID inválido")
                
                val request = try { call.receiveNullable<QuickNoteRequest>() } catch(e: Exception) { null }
                
                transaction {
                    val appointment = Appointment.findById(appointmentId)
                        ?: return@transaction false to "Consulta não encontrada"
                    
                    if (appointment.professionalId.value != professionalId) {
                        return@transaction false to "Sem permissão"
                    }
                    
                    appointment.status = AppointmentStatus.COMPLETED
                    request?.notes?.let { appointment.notes = it }
                    appointment.updatedAt = LocalDateTime.now()
                    
                    true to null
                }.let { (success, error) ->
                    if (success) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Consulta finalizada"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to error))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao finalizar: ${e.message}"))
            }
        }
        
        // Cancelar consulta
        post("/api/appointments/{id}/cancel") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                
                val appointmentId = call.parameters["id"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "ID inválido")
                
                val request = call.receive<CancelAppointmentRequest>()
                
                transaction {
                    val appointment = Appointment.findById(appointmentId)
                        ?: return@transaction false to "Consulta não encontrada"
                    
                    if (appointment.professionalId.value != professionalId) {
                        return@transaction false to "Sem permissão"
                    }
                    
                    if (appointment.status in listOf(AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED, AppointmentStatus.IN_PROGRESS)) {
                        return@transaction false to "Não é possível cancelar esta consulta"
                    }
                    
                    appointment.status = AppointmentStatus.CANCELLED
                    appointment.cancelledBy = UserType.PROFESSIONAL
                    appointment.cancellationReason = request.reason
                    appointment.cancelledAt = LocalDateTime.now()
                    appointment.updatedAt = LocalDateTime.now()
                    
                    true to null
                }.let { (success, error) ->
                    if (success) {
                        // TODO: Notificar aluno do cancelamento
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Consulta cancelada"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to error))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao cancelar: ${e.message}"))
            }
        }
        
        // Verificar disponibilidade de horários
        get("/api/appointments/availability") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val date = call.request.queryParameters["date"]?.let { 
                    try { LocalDate.parse(it) } catch(e: Exception) { null }
                } ?: return@get call.respond(HttpStatusCode.BadRequest, "Data obrigatória (YYYY-MM-DD)")
                
                val duration = call.request.queryParameters["duration"]?.toIntOrNull() ?: 60
                
                val slots = generateTimeSlots(date, duration, professionalId)
                call.respond(slots)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao verificar disponibilidade: ${e.message}"))
            }
        }
        
        // Próximas consultas (dashboard)
        get("/api/appointments/upcoming") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = principal?.payload?.getClaim("professionalId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 5
                
                val appointments = transaction {
                    val now = LocalDateTime.now()
                    
                    var query = Appointments.select { Appointments.professionalId eq professionalId }
                    query = query.andWhere { Appointments.status neq AppointmentStatus.CANCELLED }
                    query = query.andWhere { Appointments.startDateTime greaterEq now }
                    
                    query.orderBy(Appointments.startDateTime to SortOrder.ASC)
                        .limit(limit)
                        .map { Appointment.wrapRow(it) }
                        .map { it.toResponse(UserType.PROFESSIONAL) }
                }
                
                call.respond(appointments)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao buscar próximas consultas: ${e.message}"))
            }
        }
    }
}

// ==================== FUNÇÕES AUXILIARES ====================

private fun Appointment.toResponse(viewerType: UserType): AppointmentResponse {
    // Busca o aluno com base no ID
    val student = ProfessionalStudent.findById(this.studentId.value)!!
    
    return AppointmentResponse(
        id = this.id.value,
        professionalId = this.professionalId.value,
        studentId = this.studentId.value,
        studentName = student.studentName,
        studentAvatar = student.studentAvatar,
        startDateTime = this.startDateTime.toString(),
        endDateTime = this.endDateTime.toString(),
        durationMinutes = this.durationMinutes,
        type = this.type.name,
        status = this.status.name,
        location = this.location,
        meetLink = this.meetLink,
        notes = if (this.status == AppointmentStatus.COMPLETED) this.notes else null, // Só mostra notas se finalizada
        createdBy = this.createdBy.name,
        waitingFor = this.waitingFor()?.name,
        canEdit = this.status !in listOf(AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED) &&
                  this.startDateTime.isAfter(LocalDateTime.now()),
        canCancel = this.status !in listOf(AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED, AppointmentStatus.IN_PROGRESS) &&
                   this.startDateTime.isAfter(LocalDateTime.now()),
        isPast = this.startDateTime.isBefore(LocalDateTime.now())
    )
}

private fun generateTimeSlots(date: LocalDate, durationMinutes: Int, professionalId: Int): List<AvailableSlotResponse> {
    // Horário comercial padrão: 08h às 20h
    val startHour = 8
    val endHour = 20
    val slotInterval = 30 // intervalos de 30min
    
    val slots = mutableListOf<AvailableSlotResponse>()
    
    transaction {
        for (hour in startHour until endHour) {
            for (minute in listOf(0, slotInterval)) {
                val slotStart = date.atTime(hour, minute)
                val slotEnd = slotStart.plusMinutes(durationMinutes.toLong())
                
                // Verificar conflito
                val conflict = Appointment.find {
                    (Appointments.professionalId eq professionalId) and
                    (Appointments.status neq AppointmentStatus.CANCELLED) and
                    (
                        (Appointments.startDateTime lessEq slotEnd) and
                        (Appointments.endDateTime greaterEq slotStart)
                    )
                }.firstOrNull()
                
                slots.add(AvailableSlotResponse(
                    time = String.format("%02d:%02d", hour, minute),
                    available = conflict == null,
                    conflictingAppointmentId = conflict?.id?.value
                ))
            }
        }
    }
    
    return slots
}
