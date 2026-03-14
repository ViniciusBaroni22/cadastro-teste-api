package com.katsufit.routes.shared

import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

fun Route.authRoutes() {
    route("/api/auth") {
        
        authenticate("auth-jwt") {
            post("/logout") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                
                val userId = principal.payload.getClaim("id")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "ID do usuário não encontrado no token"))
                
                val expiresAt = principal.expiresAt?.toInstant()?.let {
                    LocalDateTime.ofInstant(it, java.time.ZoneId.systemDefault())
                } ?: LocalDateTime.now().plusHours(24)
                
                val authHeader = call.request.header(HttpHeaders.Authorization)
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Header de autorização não encontrado"))
                
                val token = authHeader.removePrefix("Bearer ").trim()
                
                if (token.isBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Token não fornecido"))
                }
                
                transaction {
                    BlacklistedTokens.insert {
                        it[BlacklistedTokens.token] = token
                        it[BlacklistedTokens.userId] = UUID.fromString(userId)
                        it[BlacklistedTokens.expiresAt] = expiresAt
                    }
                }
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "message" to "Logout realizado com sucesso",
                    "userId" to userId
                ))
            }
        }
        
        get("/check-token") {
            val authHeader = call.request.header(HttpHeaders.Authorization)
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Token não fornecido"))
            
            val token = authHeader.removePrefix("Bearer ").trim()
            
            val isBlacklisted = transaction {
                BlacklistedTokens.select { BlacklistedTokens.token eq token }.count() > 0
            }
            
            call.respond(HttpStatusCode.OK, mapOf(
                "isBlacklisted" to isBlacklisted
            ))
        }
    }
}
