package com.katsufit.models.client

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import com.katsufit.models.shared.Users

/**
 * Tabela de vínculos entre Cliente e Profissional (Nutricionista ou Personal)
 * Cliente solicita, Profissional aceita/recusa
 */
object ClientProfessionalLinks : UUIDTable("client_professional_links") {
    val clientId = reference("client_id", Users, onDelete = ReferenceOption.CASCADE)
    val professionalId = reference("professional_id", Users, onDelete = ReferenceOption.CASCADE)
    val professionalType = varchar("professional_type", 20) // NUTRITIONIST ou PERSONAL
    val invitedBy = varchar("invited_by", 20) // CLIENT ou PROFESSIONAL
    val status = varchar("status", 20).default("PENDING") // PENDING, ACCEPTED, REJECTED, BLOCKED
    val invitationMessage = text("invitation_message").nullable()
    val linkedAt = timestamp("linked_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    
    init {
        uniqueIndex(clientId, professionalId, professionalType)
    }
}

@Serializable
data class ClientProfessionalLinkDTO(
    val id: String,
    val clientId: String,
    val professionalId: String,
    val professionalType: String,
    val professionalName: String? = null,
    val professionalEmail: String? = null,
    val invitedBy: String,
    val status: String,
    val invitationMessage: String? = null,
    val linkedAt: String? = null,
    val createdAt: String
)

@Serializable
data class LinkRequest(
    val professionalEmail: String,
    val professionalType: String, // NUTRITIONIST ou PERSONAL
    val message: String? = null
)

@Serializable
data class LinkInvitationRequest(
    val clientEmail: String,
    val message: String? = null
)

@Serializable
data class LinkResponse(
    val id: String,
    val status: String,
    val message: String
)
