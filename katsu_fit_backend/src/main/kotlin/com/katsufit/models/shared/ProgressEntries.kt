package com.katsufit.models.shared

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import java.util.UUID

object ProgressEntries : UUIDTable("progress_entries") {
    // client (usuário que recebe o acompanhamento)
    val clientId = uuid("client_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    // professional profile id (perfil do profissional que registrou)
    val professionalId = uuid("professional_id").references(ProfessionalProfiles.id, onDelete = ReferenceOption.CASCADE)
    val weight = double("weight")
    val waist = double("waist").nullable()
    val chest = double("chest").nullable()
    val hips = double("hips").nullable()
    val notes = text("notes").nullable()
    val entryAt = timestamp("entry_at").defaultExpression(CurrentTimestamp)
}

@Serializable
data class ProgressRequest(
    val clientId: String,
    val weight: Double,
    val waist: Double? = null,
    val chest: Double? = null,
    val hips: Double? = null,
    val notes: String? = null
)

@Serializable
data class ProgressResponse(
    val id: String,
    val clientId: String,
    val professionalId: String,
    val weight: Double,
    val waist: Double? = null,
    val chest: Double? = null,
    val hips: Double? = null,
    val notes: String? = null,
    val entryAt: String
)
