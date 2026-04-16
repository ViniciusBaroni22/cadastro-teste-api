package com.katsufit.models.shared

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Users : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val userType = varchar("user_type", 50)
    val name = varchar("name", 255)
    // Campos adicionais para clientes
    val phone = varchar("phone", 50).nullable()
    val birthDate = date("birth_date").nullable()
    val gender = varchar("gender", 20).nullable()
    val heightCm = integer("height_cm").nullable()
    val currentWeightKg = decimal("current_weight_kg", 5, 2).nullable()
    val goal = varchar("goal", 50).nullable() // EMAGRECER, GANHAR_MASSA, MANUTENCAO
    val avatarUrl = text("avatar_url").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

// UserLoginRequest continua aqui, sem problemas
@Serializable
data class UserLoginRequest(
    val email: String,
    val password: String
)

