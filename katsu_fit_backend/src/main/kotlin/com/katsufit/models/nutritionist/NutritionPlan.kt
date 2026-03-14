package com.katsufit.models.nutritionist

import com.katsufit.models.shared.*
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.kotlin.datetime.date

object NutritionPlans : UUIDTable("nutrition_plans") {
    val professionalId = uuid("professional_id").references(ProfessionalProfiles.id, onDelete = ReferenceOption.CASCADE)
    val clientId = uuid("client_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 255)
    val startDate = date("start_date")
    val endDate = date("end_date")
    val description = text("description").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}
