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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

fun Route.walletRouting() {
    authenticate("auth-jwt") {
        route("/api/wallet") {
            
            get {
                val principal = call.principal<JWTPrincipal>()
                val odUserId = principal?.payload?.getClaim("id")?.asString()

                if (odUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                    return@get
                }

                val odUserUUID = UUID.fromString(odUserId)

                val wallet = transaction {
                    val existingWallet = Wallets.selectAll()
                        .where { Wallets.nutritionistId eq odUserUUID }
                        .singleOrNull()

                    if (existingWallet == null) {
                        Wallets.insert {
                            it[nutritionistId] = odUserUUID
                            it[creditsBalance] = 0
                            it[autoDeduct] = true
                        }
                        Wallets.selectAll()
                            .where { Wallets.nutritionistId eq odUserUUID }
                            .single()
                    } else {
                        existingWallet
                    }
                }

                val activeClientsCount = transaction {
                    NutritionistPatientLinks.selectAll()
                        .where { NutritionistPatientLinks.nutritionist eq odUserUUID }
                        .count()
                        .toInt()
                }

                val nextConsumptionDate = LocalDate.now()
                    .with(TemporalAdjusters.firstDayOfNextMonth())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

                val response = WalletResponse(
                    id = wallet[Wallets.id].value,
                    nutritionistId = wallet[Wallets.nutritionistId].toString(),
                    creditsBalance = wallet[Wallets.creditsBalance],
                    autoDeduct = wallet[Wallets.autoDeduct],
                    activeClientsCount = activeClientsCount,
                    nextConsumptionDate = nextConsumptionDate,
                    createdAt = wallet[Wallets.createdAt].toString(),
                    updatedAt = wallet[Wallets.updatedAt].toString()
                )

                call.respond(HttpStatusCode.OK, response)
            }

            put("/auto-deduct") {
                val principal = call.principal<JWTPrincipal>()
                val odUserId = principal?.payload?.getClaim("id")?.asString()

                if (odUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                    return@put
                }

                val odUserUUID = UUID.fromString(odUserId)
                val request = call.receive<WalletUpdateRequest>()

                val updated = transaction {
                    val exists = Wallets.selectAll()
                        .where { Wallets.nutritionistId eq odUserUUID }
                        .singleOrNull()

                    if (exists == null) {
                        Wallets.insert {
                            it[nutritionistId] = odUserUUID
                            it[creditsBalance] = 0
                            it[autoDeduct] = request.autoDeduct
                        }
                    } else {
                        Wallets.update({ Wallets.nutritionistId eq odUserUUID }) {
                            it[autoDeduct] = request.autoDeduct
                        }
                    }

                    Wallets.selectAll()
                        .where { Wallets.nutritionistId eq odUserUUID }
                        .single()
                }

                val activeClientsCount = transaction {
                    NutritionistPatientLinks.selectAll()
                        .where { NutritionistPatientLinks.nutritionist eq odUserUUID }
                        .count()
                        .toInt()
                }

                val nextConsumptionDate = LocalDate.now()
                    .with(TemporalAdjusters.firstDayOfNextMonth())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

                val response = WalletResponse(
                    id = updated[Wallets.id].value,
                    nutritionistId = updated[Wallets.nutritionistId].toString(),
                    creditsBalance = updated[Wallets.creditsBalance],
                    autoDeduct = updated[Wallets.autoDeduct],
                    activeClientsCount = activeClientsCount,
                    nextConsumptionDate = nextConsumptionDate,
                    createdAt = updated[Wallets.createdAt].toString(),
                    updatedAt = updated[Wallets.updatedAt].toString()
                )

                call.respond(HttpStatusCode.OK, response)
            }

            get("/transactions") {
                val principal = call.principal<JWTPrincipal>()
                val odUserId = principal?.payload?.getClaim("id")?.asString()

                if (odUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                    return@get
                }

                val odUserUUID = UUID.fromString(odUserId)
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

                val transactions = transaction {
                    CreditTransactions.selectAll()
                        .where { CreditTransactions.nutritionistId eq odUserUUID }
                        .orderBy(CreditTransactions.createdAt, SortOrder.DESC)
                        .limit(limit)
                        .map { row ->
                            val patientId = row[CreditTransactions.patientId]
                            val patientName = if (patientId != null) {
                                Users.selectAll()
                                    .where { Users.id eq patientId }
                                    .singleOrNull()
                                    ?.get(Users.name)
                            } else null

                            CreditTransactionResponse(
                                id = row[CreditTransactions.id].value,
                                nutritionistId = row[CreditTransactions.nutritionistId].toString(),
                                type = row[CreditTransactions.type],
                                amount = row[CreditTransactions.amount],
                                description = row[CreditTransactions.description],
                                patientId = patientId?.toString(),
                                patientName = patientName,
                                createdAt = row[CreditTransactions.createdAt].toString()
                            )
                        }
                }

                val totals = transaction {
                    val allTransactions = CreditTransactions.selectAll()
                        .where { CreditTransactions.nutritionistId eq odUserUUID }
                        .toList()
                    
                    val purchased = allTransactions
                        .filter { it[CreditTransactions.type] == "PURCHASE" }
                        .sumOf { it[CreditTransactions.amount] }

                    val consumed = allTransactions
                        .filter { it[CreditTransactions.type] == "CONSUMPTION" }
                        .sumOf { it[CreditTransactions.amount] }

                    Pair(purchased, consumed)
                }

                val response = TransactionHistoryResponse(
                    transactions = transactions,
                    totalPurchased = totals.first,
                    totalConsumed = totals.second
                )

                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
