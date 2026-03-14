package com.katsufit.routes.shared

import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun Route.supportRoutes() {
    val dateFormatter = DateTimeFormatter.ISO_DATE_TIME
    
    authenticate("auth-jwt") {
        
        // POST /api/support/tickets - Criar chamado
        post("/api/support/tickets") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("id")?.asString()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, "Token inválido")
            
            val request = call.receive<CreateTicketRequest>()
            
            // Validações
            if (request.subject.isBlank() || request.subject.length < 5) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Assunto deve ter no mínimo 5 caracteres"))
            }
            if (request.description.isBlank() || request.description.length < 10) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Descrição deve ter no mínimo 10 caracteres"))
            }
            
            val validCategories = listOf("PRIMEIROS_PASSOS", "FINANCEIRO", "TECNICO", "DUVIDA", "SUGESTAO")
            if (request.category !in validCategories) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Categoria inválida"))
            }
            
            val validPriorities = listOf("BAIXA", "MEDIA", "ALTA")
            val priority = if (request.priority in validPriorities) request.priority else "MEDIA"
            
            val now = LocalDateTime.now()
            
            val ticketId = transaction {
                SupportTickets.insertAndGetId {
                    it[SupportTickets.professionalId] = professionalId
                    it[SupportTickets.category] = request.category
                    it[SupportTickets.subject] = request.subject
                    it[SupportTickets.description] = request.description
                    it[SupportTickets.status] = "ABERTO"
                    it[SupportTickets.priority] = priority
                    it[SupportTickets.createdAt] = now
                    it[SupportTickets.updatedAt] = now
                    it[SupportTickets.resolvedAt] = null
                }.value
            }
            
            call.respond(HttpStatusCode.Created, mapOf(
                "id" to ticketId,
                "message" to "Chamado aberto com sucesso",
                "protocol" to "SUP-${ticketId.toString().padStart(6, '0')}"
            ))
        }
        
        // GET /api/support/tickets - Listar meus chamados
        get("/api/support/tickets") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("id")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Token inválido")
            
            val tickets = transaction {
                SupportTickets
                    .select { SupportTickets.professionalId eq professionalId }
                    .orderBy(SupportTickets.createdAt to SortOrder.DESC)
                    .map {
                        TicketListItem(
                            id = it[SupportTickets.id].value,
                            subject = it[SupportTickets.subject],
                            status = it[SupportTickets.status],
                            createdAt = it[SupportTickets.createdAt].format(dateFormatter)
                        )
                    }
            }
            
            call.respond(tickets)
        }
        
        // GET /api/support/tickets/{id} - Detalhe do chamado
        get("/api/support/tickets/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val professionalId = principal?.payload?.getClaim("id")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Token inválido")
            
            val ticketId = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            
            val ticket = transaction {
                SupportTickets
                    .select { (SupportTickets.id eq ticketId) and (SupportTickets.professionalId eq professionalId) }
                    .map {
                        TicketResponse(
                            id = it[SupportTickets.id].value,
                            professionalId = it[SupportTickets.professionalId],
                            category = it[SupportTickets.category],
                            subject = it[SupportTickets.subject],
                            description = it[SupportTickets.description],
                            status = it[SupportTickets.status],
                            priority = it[SupportTickets.priority],
                            createdAt = it[SupportTickets.createdAt].format(dateFormatter),
                            updatedAt = it[SupportTickets.updatedAt].format(dateFormatter),
                            resolvedAt = it[SupportTickets.resolvedAt]?.format(dateFormatter)
                        )
                    }.singleOrNull()
            }
            
            if (ticket == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Chamado não encontrado"))
            } else {
                call.respond(ticket)
            }
        }
    }
}
