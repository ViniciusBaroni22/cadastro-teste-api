package com.katsufit.routes.client

import com.katsufit.models.client.*
import com.katsufit.models.shared.Users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.clientRoutes() {
    
    // ============================================
    // PERFIL DO CLIENTE
    // ============================================
    authenticate("auth-jwt") {
        
        // Get perfil do cliente (apenas campos que existem na tabela)
        get("/api/client/profile") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val clientId = principal!!.payload.getClaim("id").asString()
                val clientUuid = UUID.fromString(clientId)
                
                val profile = transaction {
                    Users.select { Users.id eq clientUuid }.singleOrNull()?.let { row ->
                        mapOf(
                            "id" to row[Users.id].toString(),
                            "email" to row[Users.email],
                            "name" to row[Users.name],
                            "userType" to row[Users.userType]
                            // Campos opcionais removidos - podem não existir no banco
                        )
                    }
                }
                
                if (profile != null) {
                    call.respond(HttpStatusCode.OK, profile)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Perfil não encontrado")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Erro ao buscar perfil: ${e.message}")
            }
        }
        
        // Atualizar perfil (apenas campos básicos)
        put("/api/client/profile") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val clientId = principal!!.payload.getClaim("id").asString()
                val clientUuid = UUID.fromString(clientId)
                val update = call.receive<Map<String, String?>>()
                
                transaction {
                    Users.update({ Users.id eq clientUuid }) {
                        update["name"]?.let { name -> it[Users.name] = name }
                        // Campos opcionais removidos
                    }
                }
                
                call.respond(HttpStatusCode.OK, "Perfil atualizado com sucesso")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Erro ao atualizar perfil: ${e.message}")
            }
        }
    }
}
