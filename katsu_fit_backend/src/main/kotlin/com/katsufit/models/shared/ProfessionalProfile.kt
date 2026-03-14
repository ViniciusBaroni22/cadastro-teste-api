package com.katsufit.models.shared

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object ProfessionalProfiles : UUIDTable("professional_profiles") {
    val userId = uuid("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val documentId = varchar("document_id", 20).uniqueIndex()
    val name = varchar("name", 255)
    val profession = varchar("profession", 255)
    val bio = text("bio").nullable()
    val specialty = varchar("specialty", 255).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}
