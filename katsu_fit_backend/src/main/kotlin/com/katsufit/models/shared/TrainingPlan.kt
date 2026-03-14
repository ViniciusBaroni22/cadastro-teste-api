package com.katsufit.models.shared

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object TrainingPlans : UUIDTable("training_plans") {
    val professionalId = uuid("professional_id").references(ProfessionalProfiles.id, onDelete = ReferenceOption.CASCADE)
    val clientId = uuid("client_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}
