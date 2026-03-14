package com.katsufit.models.shared

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Users : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val userType = varchar("user_type", 50)
    val name = varchar("name", 255) // <-- Linha importante adicionada/confirmada
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

// UserLoginRequest continua aqui, sem problemas
@Serializable
data class UserLoginRequest(
    val email: String,
    val password: String
)

