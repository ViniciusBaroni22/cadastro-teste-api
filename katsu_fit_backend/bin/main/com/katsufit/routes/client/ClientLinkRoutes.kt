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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.clientLinkRoutes() {
    
    authenticate("auth-jwt") {
        
        // ============================================
        // CLIENTE: LISTAR SEUS VÍNCULOS
        // ============================================
        get("/api/client/links") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val clientId = principal!!.payload.getClaim("id").asString()
                val clientUuid = UUID.fromString(clientId)
                
                val links = transaction {
                    ClientProfessionalLinks
                        .join(Users, JoinType.INNER, ClientProfessionalLinks.professionalId, Users.id)
                        .select { ClientProfessionalLinks.clientId eq clientUuid }
                        .orderBy(ClientProfessionalLinks.createdAt, SortOrder.DESC)
                        .map { row ->
                            ClientProfessionalLinkDTO(
                                id = row[ClientProfessionalLinks.id].toString(),
                                clientId = row[ClientProfessionalLinks.clientId].toString(),
                                professionalId = row[ClientProfessionalLinks.professionalId].toString(),
                                professionalType = row[ClientProfessionalLinks.professionalType],
                                professionalName = row[Users.name],
                                professionalEmail = row[Users.email],
                                invitedBy = row[ClientProfessionalLinks.invitedBy],
                                status = row[ClientProfessionalLinks.status],
                                invitationMessage = row[ClientProfessionalLinks.invitationMessage],
                                linkedAt = row[ClientProfessionalLinks.linkedAt]?.toString(),
                                createdAt = row[ClientProfessionalLinks.createdAt].toString()
                            )
                        }
                }
                
                call.respond(HttpStatusCode.OK, links)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Erro ao buscar vínculos: ${e.message}")
            }
        }
        
        // ============================================
        // CLIENTE: SOLICITAR VÍNCULO COM PROFISSIONAL
        // ============================================
        post("/api/client/links/request") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val clientId = principal!!.payload.getClaim("id").asString()
                val clientUuid = UUID.fromString(clientId)
                
                val request = call.receive<LinkRequest>()
                
                // Buscar profissional pelo email
                val professional = transaction {
                    Users.select { 
                        (Users.email eq request.professionalEmail) and 
                        (Users.userType eq request.professionalType)
                    }.singleOrNull()
                }
                
                if (professional == null) {
                    call.respond(HttpStatusCode.NotFound, "Profissional não encontrado")
                    return@post
                }
                
                val professionalUuid = professional[Users.id]
                val professionalType = professional[Users.userType]
                
                // Verificar se já existe vínculo
                val existingLink = transaction {
                    ClientProfessionalLinks.select {
                        (ClientProfessionalLinks.clientId eq clientUuid) and
                        (ClientProfessionalLinks.professionalId eq professionalUuid) and
                        (ClientProfessionalLinks.professionalType eq professionalType)
                    }.singleOrNull()
                }
                
                if (existingLink != null) {
                    call.respond(HttpStatusCode.Conflict, "Vínculo já existe com status: ${existingLink[ClientProfessionalLinks.status]}")
                    return@post
                }
                
                // Criar novo vínculo
                val newLinkId = transaction {
                    ClientProfessionalLinks.insert {
                        it[ClientProfessionalLinks.clientId] = clientUuid
                        it[ClientProfessionalLinks.professionalId] = professionalUuid.value
                        it[ClientProfessionalLinks.professionalType] = request.professionalType
                        it[ClientProfessionalLinks.invitedBy] = "CLIENT"
                        it[ClientProfessionalLinks.status] = "PENDING"
                        it[ClientProfessionalLinks.invitationMessage] = request.message
                    } get ClientProfessionalLinks.id
                }
                
                call.respond(HttpStatusCode.Created, LinkResponse(
                    id = newLinkId.toString(),
                    status = "PENDING",
                    message = "Solicitação enviada com sucesso! Aguarde a aprovação do profissional."
                ))
                
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Erro ao solicitar vínculo: ${e.message}")
            }
        }
        
        // ============================================
        // CLIENTE: CANCELAR SOLICITAÇÃO PENDENTE
        // ============================================
        post("/api/client/links/{id}/cancel") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val clientId = principal!!.payload.getClaim("id").asString()
                val clientUuid = UUID.fromString(clientId)
                val linkId = UUID.fromString(call.parameters["id"])
                
                val updated = transaction {
                    ClientProfessionalLinks.update({ 
                        (ClientProfessionalLinks.id eq linkId) and 
                        (ClientProfessionalLinks.clientId eq clientUuid) and
                        (ClientProfessionalLinks.status eq "PENDING")
                    }) {
                        it[ClientProfessionalLinks.status] = "REJECTED"
                        it[ClientProfessionalLinks.updatedAt] = Clock.System.now()
                    }
                }
                
                if (updated > 0) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Solicitação cancelada"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Solicitação não encontrada ou já processada")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Erro ao cancelar: ${e.message}")
            }
        }
        
        // ============================================
        // CLIENTE: REMOVER VÍNCULO ACEITO
        // ============================================
        delete("/api/client/links/{id}") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val clientId = principal!!.payload.getClaim("id").asString()
                val clientUuid = UUID.fromString(clientId)
                val linkId = UUID.fromString(call.parameters["id"])
                
                val deleted = transaction {
                    ClientProfessionalLinks.deleteWhere { 
                        (ClientProfessionalLinks.id eq linkId) and 
                        (ClientProfessionalLinks.clientId eq clientUuid)
                    }
                }
                
                if (deleted > 0) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Vínculo removido"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Vínculo não encontrado")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Erro ao remover vínculo: ${e.message}")
            }
        }
        
        // ============================================
        // PROFISSIONAL: LISTAR CONVITES PENDENTES
        // ============================================
        get("/api/professional/links/pending") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val profId = principal!!.payload.getClaim("id").asString()
                val profUuid = UUID.fromString(profId)
                
                val pending = transaction {
                    ClientProfessionalLinks
                        .join(Users, JoinType.INNER, ClientProfessionalLinks.clientId, Users.id)
                        .select { 
                            (ClientProfessionalLinks.professionalId eq profUuid) and
                            (ClientProfessionalLinks.status eq "PENDING")
                        }
                        .orderBy(ClientProfessionalLinks.createdAt, SortOrder.DESC)
                        .map { row ->
                            mapOf(
                                "id" to row[ClientProfessionalLinks.id].toString(),
                                "clientId" to row[ClientProfessionalLinks.clientId].toString(),
                                "clientName" to row[Users.name],
                                "clientEmail" to row[Users.email],
                                "professionalType" to row[ClientProfessionalLinks.professionalType],
                                "invitedBy" to row[ClientProfessionalLinks.invitedBy],
                                "message" to row[ClientProfessionalLinks.invitationMessage],
                                "createdAt" to row[ClientProfessionalLinks.createdAt].toString()
                            )
                        }
                }
                
                call.respond(HttpStatusCode.OK, pending)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Erro ao buscar convites: ${e.message}")
            }
        }
        
        // ============================================
        // PROFISSIONAL: ACEITAR CONVITE
        // ============================================
        post("/api/professional/links/{id}/accept") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val profId = principal!!.payload.getClaim("id").asString()
                val profUuid = UUID.fromString(profId)
                val linkId = UUID.fromString(call.parameters["id"])
                
                val updated = transaction {
                    ClientProfessionalLinks.update({ 
                        (ClientProfessionalLinks.id eq linkId) and 
                        (ClientProfessionalLinks.professionalId eq profUuid) and
                        (ClientProfessionalLinks.status eq "PENDING")
                    }) {
                        it[ClientProfessionalLinks.status] = "ACCEPTED"
                        it[ClientProfessionalLinks.linkedAt] = Clock.System.now()
                        it[ClientProfessionalLinks.updatedAt] = Clock.System.now()
                    }
                }
                
                if (updated > 0) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Vínculo aceito com sucesso!"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Convite não encontrado ou já processado")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Erro ao aceitar: ${e.message}")
            }
        }
        
        // ============================================
        // PROFISSIONAL: RECUSAR CONVITE
        // ============================================
        post("/api/professional/links/{id}/reject") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val profId = principal!!.payload.getClaim("id").asString()
                val profUuid = UUID.fromString(profId)
                val linkId = UUID.fromString(call.parameters["id"])
                
                val updated = transaction {
                    ClientProfessionalLinks.update({ 
                        (ClientProfessionalLinks.id eq linkId) and 
                        (ClientProfessionalLinks.professionalId eq profUuid) and
                        (ClientProfessionalLinks.status eq "PENDING")
                    }) {
                        it[ClientProfessionalLinks.status] = "REJECTED"
                        it[ClientProfessionalLinks.updatedAt] = Clock.System.now()
                    }
                }
                
                if (updated > 0) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Convite recusado"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Convite não encontrado ou já processado")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Erro ao recusar: ${e.message}")
            }
        }
        
        // ============================================
        // PROFISSIONAL: CONVIDAR CLIENTE (Opção A)
        // ============================================
        post("/api/professional/clients/invite") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val profId = principal!!.payload.getClaim("id").asString()
                val profUuid = UUID.fromString(profId)
                val profType = principal.payload.getClaim("userType").asString()
                
                val request = call.receive<LinkInvitationRequest>()
                
                // Buscar ou criar cliente
                val clientUuid = transaction {
                    val existing = Users.select { Users.email eq request.clientEmail }.singleOrNull()
                    if (existing != null) {
                        existing[Users.id].value
                    } else {
                        // Criar usuário cliente
                        (Users.insert {
                            it[email] = request.clientEmail
                            it[passwordHash] = "" // Cliente define senha no primeiro login
                            it[userType] = "CLIENT"
                            it[name] = "Novo Cliente"
                        } get Users.id).value
                    }
                }
                
                // Verificar se vínculo já existe
                val existingLink = transaction {
                    ClientProfessionalLinks.select {
                        (ClientProfessionalLinks.clientId eq clientUuid) and
                        (ClientProfessionalLinks.professionalId eq profUuid)
                    }.singleOrNull()
                }
                
                if (existingLink != null) {
                    call.respond(HttpStatusCode.Conflict, "Vínculo já existe")
                    return@post
                }
                
                // Criar vínculo
                val newLinkId = transaction {
                    ClientProfessionalLinks.insert {
                        it[ClientProfessionalLinks.clientId] = clientUuid
                        it[ClientProfessionalLinks.professionalId] = profUuid
                        it[ClientProfessionalLinks.professionalType] = profType
                        it[ClientProfessionalLinks.invitedBy] = "PROFESSIONAL"
                        it[ClientProfessionalLinks.status] = "PENDING"
                        it[ClientProfessionalLinks.invitationMessage] = request.message
                    } get ClientProfessionalLinks.id
                }
                
                // TODO: Enviar email de convite para o cliente
                
                call.respond(HttpStatusCode.Created, mapOf(
                    "id" to newLinkId.toString(),
                    "message" to "Convite enviado! O cliente precisa aceitar para iniciar o vínculo."
                ))
                
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Erro ao convidar: ${e.message}")
            }
        }
    }
}
