package com.katsufit.routes.shared

import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.messageRouting() {
    authenticate("auth-jwt") {
        route("/api/messages") {

            // Enviar mensagem (POST)
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val senderId = principal?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) }
                    if (senderId == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Token inválido.")
                        return@post
                    }

                    val req = call.receive<MessageRequest>()
                    val receiverId = UUID.fromString(req.receiverId)

                    val messageId = transaction {
                        Messages.insertAndGetId {
                            it[Messages.senderId] = senderId
                            it[Messages.receiverId] = receiverId
                            it[Messages.content] = req.content
                            it[Messages.attachmentUrl] = req.attachmentUrl
                        }.value
                    }

                    val message = transaction {
                        Messages.select { Messages.id eq messageId }
                            .map {
                                MessageResponse(
                                    id = it[Messages.id].value.toString(),
                                    senderId = it[Messages.senderId].toString(),
                                    receiverId = it[Messages.receiverId].toString(),
                                    content = it[Messages.content],
                                    timestamp = it[Messages.timestamp].toString(),
                                    read = it[Messages.read],
                                    attachmentUrl = it[Messages.attachmentUrl]
                                )
                            }.first()
                    }

                    call.respond(HttpStatusCode.Created, message)

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao enviar mensagem", "details" to (e.message ?: "Erro desconhecido")))
                }
            }

            // Buscar histórico de mensagens (GET)
            // Query params: otherId (UUID do outro usuário na conversa), opcional: limit, offset
            get {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) }
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Token inválido.")
                        return@get
                    }

                    val otherIdParam = call.request.queryParameters["otherId"]
                    if (otherIdParam.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Informe o parâmetro 'otherId' para buscar a conversa.")
                        return@get
                    }
                    val otherId = UUID.fromString(otherIdParam)
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                    val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                    val messages = transaction {
                        Messages.select {
                            ((Messages.senderId eq userId) and (Messages.receiverId eq otherId)) or
                            ((Messages.senderId eq otherId) and (Messages.receiverId eq userId))
                        }
                        .orderBy(Messages.timestamp to SortOrder.DESC)
                        .limit(limit, offset = offset.toLong())
                        .map {
                            MessageResponse(
                                id = it[Messages.id].value.toString(),
                                senderId = it[Messages.senderId].toString(),
                                receiverId = it[Messages.receiverId].toString(),
                                content = it[Messages.content],
                                timestamp = it[Messages.timestamp].toString(),
                                read = it[Messages.read],
                                attachmentUrl = it[Messages.attachmentUrl]
                            )
                        }
                    }

                    call.respond(messages)

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro ao buscar mensagens", "details" to (e.message ?: "Erro desconhecido")))
                }
            }
        }
    }
}
