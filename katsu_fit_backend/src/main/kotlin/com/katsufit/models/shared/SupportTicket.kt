package com.katsufit.models.shared

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object SupportTickets : IntIdTable("support_tickets") {
    val professionalId = varchar("professional_id", 36) // UUID como String
    val category = varchar("category", 50) // PRIMEIROS_PASSOS, FINANCEIRO, TECNICO, DUVIDA, SUGESTAO
    val subject = varchar("subject", 200)
    val description = text("description")
    val status = varchar("status", 20).default("ABERTO") // ABERTO, EM_ANALISE, RESOLVIDO
    val priority = varchar("priority", 20).default("MEDIA") // BAIXA, MEDIA, ALTA
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    val resolvedAt = datetime("resolved_at").nullable()
}
