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
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

fun Route.settingsRoutes() {
    
    val dateFormatter = DateTimeFormatter.ISO_DATE_TIME
    
    authenticate("auth-jwt") {
        
        /**
         * GET /api/settings
         * Buscar configurações completas do usuário
         */
        get("/api/settings") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, SettingsErrorResponse("Token inválido"))
            
            val settings = transaction {
                UserSettingsTable
                    .select { UserSettingsTable.userId eq UUID.fromString(userId) }
                    .map { row ->
                        UserSettingsResponse(
                            id = row[UserSettingsTable.id].value,
                            userId = row[UserSettingsTable.userId].toString(),
                            notificacoes = NotificacoesSettings(
                                pushConsultas = row[UserSettingsTable.pushConsultas],
                                emailConsumo = row[UserSettingsTable.emailConsumo],
                                lembretesRenovacao = row[UserSettingsTable.lembretesRenovacao]
                            ),
                            aparencia = AparenciaSettings(
                                tema = row[UserSettingsTable.tema],
                                idioma = row[UserSettingsTable.idioma]
                            ),
                            createdAt = row[UserSettingsTable.createdAt].format(dateFormatter),
                            updatedAt = row[UserSettingsTable.updatedAt].format(dateFormatter)
                        )
                    }
                    .singleOrNull()
            }
            
            if (settings == null) {
                // Cria configurações padrão se não existir
                val newSettings = transaction {
                    val id = UserSettingsTable.insertAndGetId { stmt ->
                        stmt[UserSettingsTable.userId] = UUID.fromString(userId)
                        stmt[createdAt] = LocalDateTime.now()
                        stmt[updatedAt] = LocalDateTime.now()
                    }
                    
                    UserSettingsTable
                        .select { UserSettingsTable.id eq id }
                        .map { row ->
                            UserSettingsResponse(
                                id = row[UserSettingsTable.id].value,
                                userId = row[UserSettingsTable.userId].toString(),
                                notificacoes = NotificacoesSettings(
                                    pushConsultas = row[UserSettingsTable.pushConsultas],
                                    emailConsumo = row[UserSettingsTable.emailConsumo],
                                    lembretesRenovacao = row[UserSettingsTable.lembretesRenovacao]
                                ),
                                aparencia = AparenciaSettings(
                                    tema = row[UserSettingsTable.tema],
                                    idioma = row[UserSettingsTable.idioma]
                                ),
                                createdAt = row[UserSettingsTable.createdAt].format(dateFormatter),
                                updatedAt = row[UserSettingsTable.updatedAt].format(dateFormatter)
                            )
                        }
                        .single()
                }
                call.respond(HttpStatusCode.OK, newSettings)
            } else {
                call.respond(HttpStatusCode.OK, settings)
            }
        }
        
        /**
         * PUT /api/settings/notificacoes
         * Atualizar preferências de notificação
         */
        put("/api/settings/notificacoes") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asString()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, SettingsErrorResponse("Token inválido"))
            
            val request = call.receive<UpdateNotificacoesRequest>()
            
            transaction {
                // Garante que existe
                val exists = UserSettingsTable
                    .select { UserSettingsTable.userId eq UUID.fromString(userId) }
                    .empty().not()
                
                if (!exists) {
                    UserSettingsTable.insert { stmt ->
                        stmt[UserSettingsTable.userId] = UUID.fromString(userId)
                        stmt[createdAt] = LocalDateTime.now()
                        stmt[updatedAt] = LocalDateTime.now()
                    }
                }
                
                // Update
                UserSettingsTable.update({ UserSettingsTable.userId eq UUID.fromString(userId) }) { stmt ->
                    request.pushConsultas?.let { stmt[pushConsultas] = it }
                    request.emailConsumo?.let { stmt[emailConsumo] = it }
                    request.lembretesRenovacao?.let { stmt[lembretesRenovacao] = it }
                    stmt[updatedAt] = LocalDateTime.now()
                }
            }
            
            call.respond(
                HttpStatusCode.OK, 
                SettingsSuccessResponse(true, "Notificações atualizadas com sucesso")
            )
        }
        
        /**
         * PUT /api/settings/aparencia
         * Atualizar tema e idioma
         */
        put("/api/settings/aparencia") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asString()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, SettingsErrorResponse("Token inválido"))
            
            val request = call.receive<UpdateAparenciaRequest>()
            
            // Validação de tema
            request.tema?.let { tema ->
                val temasValidos = listOf("claro", "escuro", "sistema")
                if (tema !in temasValidos) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest, 
                        SettingsErrorResponse("Tema inválido. Use: claro, escuro, sistema")
                    )
                }
            }
            
            // Validação de idioma
            request.idioma?.let { idioma ->
                val idiomasValidos = listOf("pt-BR", "en-US", "es-ES")
                if (idioma !in idiomasValidos) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest, 
                        SettingsErrorResponse("Idioma inválido. Use: pt-BR, en-US, es-ES")
                    )
                }
            }
            
            transaction {
                val exists = UserSettingsTable
                    .select { UserSettingsTable.userId eq UUID.fromString(userId) }
                    .empty().not()
                
                if (!exists) {
                    UserSettingsTable.insert { stmt ->
                        stmt[UserSettingsTable.userId] = UUID.fromString(userId)
                        stmt[createdAt] = LocalDateTime.now()
                        stmt[updatedAt] = LocalDateTime.now()
                    }
                }
                
                UserSettingsTable.update({ UserSettingsTable.userId eq UUID.fromString(userId) }) { stmt ->
                    request.tema?.let { stmt[tema] = it }
                    request.idioma?.let { stmt[idioma] = it }
                    stmt[updatedAt] = LocalDateTime.now()
                }
            }
            
            call.respond(
                HttpStatusCode.OK, 
                SettingsSuccessResponse(true, "Aparência atualizada com sucesso")
            )
        }
        
        /**
         * GET /api/settings/subscription
         * Buscar dados da assinatura baseado no wallet existente
         */
        get("/api/settings/subscription") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, SettingsErrorResponse("Token inválido"))
            
            val subscription = transaction {
                // Busca dados do wallet
                val wallet = Wallets
                    .select { Wallets.nutritionistId eq UUID.fromString(userId) }
                    .singleOrNull()
                
                // Conta alunos ativos
                val alunosAtivos = NutritionistPatientLinks
                    .select { NutritionistPatientLinks.nutritionist eq UUID.fromString(userId) }
                    .count()
                    .toInt()
                
                if (wallet != null) {
                    val credits = wallet[Wallets.creditsBalance]
                    val plano = if (credits >= 60) "starter" else "starter" // Lógica simples por enquanto
                    
                    SubscriptionInfoResponse(
                        plano = plano,
                        status = "ativo",
                        proximaCobranca = null, // Sem assinatura recorrente por enquanto
                        valorMensal = 0.0,
                        alunosAtivos = alunosAtivos,
                        limiteAlunos = 16
                    )
                } else {
                    SubscriptionInfoResponse(
                        plano = "starter",
                        status = "ativo",
                        proximaCobranca = null,
                        valorMensal = 0.0,
                        alunosAtivos = alunosAtivos,
                        limiteAlunos = 16
                    )
                }
            }
            
            call.respond(HttpStatusCode.OK, subscription)
        }
        
        /**
         * GET /api/settings/payments/history
         * Histórico de pagamentos baseado em CreditTransactions
         */
        get("/api/settings/payments/history") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, SettingsErrorResponse("Token inválido"))
            
            val history = transaction {
                CreditTransactions
                    .select { CreditTransactions.nutritionistId eq UUID.fromString(userId) }
                    .orderBy(CreditTransactions.createdAt, SortOrder.DESC)
                    .limit(20)
                    .map { row ->
                        PaymentHistoryItem(
                            id = "pay_${row[CreditTransactions.id].value}",
                            data = row[CreditTransactions.createdAt].toString(),
                            descricao = row[CreditTransactions.description],
                            valor = row[CreditTransactions.amount].toDouble(),
                            status = "pago"
                        )
                    }
            }
            
            call.respond(HttpStatusCode.OK, history)
        }
    }
}
