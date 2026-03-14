package com.katsufit.models.shared

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Tabela de configurações do usuário
 * One-to-One com Users (cada usuário tem uma config)
 */
object UserSettingsTable : IntIdTable("user_settings") {
    val userId = uuid("user_id").uniqueIndex().references(Users.id, onDelete = ReferenceOption.CASCADE)
    
    // Notificações
    val pushConsultas = bool("push_consultas").default(true)
    val emailConsumo = bool("email_consumo").default(true)
    val lembretesRenovacao = bool("lembretes_renovacao").default(true)
    
    // Aparência
    val tema = varchar("tema", 20).default("claro") // claro, escuro, sistema
    val idioma = varchar("idioma", 10).default("pt-BR") // pt-BR, en-US, es-ES
    
    // Timestamps
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
}
