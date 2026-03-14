package com.katsufit.routes.personal

import com.katsufit.models.personal.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDateTime as KLocalDateTime

// =============================================
// CREDIT PACKAGES — Pacotes de Créditos Avulsos
// R$5/crédito base, desconto progressivo
// =============================================
val CREDIT_PACKAGES = listOf(
    CreditPackageDTO(id = 1, credits = 10, priceInCents = 5000, label = "10 créditos", priceLabel = "R$ 50,00", pricePerCredit = "R$ 5,00", savings = null),
    CreditPackageDTO(id = 2, credits = 20, priceInCents = 9000, label = "20 créditos", priceLabel = "R$ 90,00", pricePerCredit = "R$ 4,50", savings = "10%"),
    CreditPackageDTO(id = 3, credits = 50, priceInCents = 20000, label = "50 créditos", priceLabel = "R$ 200,00", pricePerCredit = "R$ 4,00", savings = "20%", popular = true),
    CreditPackageDTO(id = 4, credits = 100, priceInCents = 35000, label = "100 créditos", priceLabel = "R$ 350,00", pricePerCredit = "R$ 3,50", savings = "30%")
)

fun Route.personalWalletRoutes() {
    authenticate("auth-jwt") {
        

        // =============================================
        // GET /api/personal/wallet
        // Retorna saldo, alunos ativos, transações recentes
        // =============================================
        get("/api/personal/wallet") {
            val principal = call.principal<JWTPrincipal>()
            val email = principal?.payload?.getClaim("email")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
            
            val response = transaction {
                val professional = Professional.find { Professionals.email eq email }.singleOrNull()
                    ?: Professional.findById(1) // Fallback for testing generic logins
                    ?: return@transaction null
                
                val professionalId = professional.id.value
                
                val students = ProfessionalStudent.find {
                    ProfessionalStudents.professionalId eq professionalId and
                    (ProfessionalStudents.isActive eq true)
                }.toList()
                
                val activeStudents = students.filter { it.hasValidCredit() }
                
                // Últimas 10 transações
                val recentTransactions = PersonalCreditTransaction.find {
                    PersonalCreditTransactions.professionalId eq professionalId
                }.orderBy(PersonalCreditTransactions.createdAt to SortOrder.DESC)
                    .limit(10)
                    .map { tx ->
                        WalletTransactionDTO(
                            id = tx.id.value,
                            description = tx.description,
                            amount = tx.amount,
                            type = tx.type.name,
                            date = tx.createdAt.toString().take(10), // yyyy-MM-dd
                            studentName = tx.studentId?.let { sid ->
                                students.find { it.studentId == sid }?.studentName
                            }
                        )
                    }
                
                // Calcular próximo consumo (dia 1 do próximo mês)
                val now = Clock.System.now().toLocalDateTime(TimeZone.of("America/Sao_Paulo"))
                val nextMonth = if (now.monthNumber == 12) {
                    KLocalDateTime(now.year + 1, 1, 1, 0, 0)
                } else {
                    KLocalDateTime(now.year, now.monthNumber + 1, 1, 0, 0)
                }
                val nextBilling = "%02d/%02d/%04d".format(nextMonth.dayOfMonth, nextMonth.monthNumber, nextMonth.year)
                
                // Gatilho de sugestão Katsu Pro (≥16 alunos)
                val suggestPro = professional.planType == PlanType.STARTER && activeStudents.size >= 16
                
                WalletResponseDTO(
                    balance = professional.creditsAvailable,
                    activeStudents = activeStudents.size,
                    totalStudents = students.size,
                    nextBillingDate = nextBilling,
                    planType = professional.planType.name,
                    suggestKatsuPro = suggestPro,
                    transactions = recentTransactions,
                    packages = CREDIT_PACKAGES
                )
            }
            
            if (response == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Profissional não encontrado"))
            } else {
                call.respond(HttpStatusCode.OK, response)
            }
        }
        
        // =============================================
        // GET /api/personal/wallet/transactions
        // Histórico completo com filtros
        // ?type=PURCHASE|DEBIT&search=nome&limit=20&offset=0
        // =============================================
        get("/api/personal/wallet/transactions") {
            val principal = call.principal<JWTPrincipal>()
            val email = principal?.payload?.getClaim("email")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
            
            val typeFilter = call.request.queryParameters["type"]
            val search = call.request.queryParameters["search"]
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0L
            
            val result = transaction {
                val professional = Professional.find { Professionals.email eq email }.singleOrNull()
                    ?: Professional.findById(1) // Fallback for testing generic logins
                    ?: return@transaction null
                
                val professionalId = professional.id.value
                
                val students = ProfessionalStudent.find {
                    ProfessionalStudents.professionalId eq professionalId
                }.toList()
                
                var baseCondition = PersonalCreditTransactions.professionalId eq professionalId
                
                if (typeFilter != null) {
                    val txType = try { TransactionType.valueOf(typeFilter) } catch (e: Exception) { null }
                    if (txType != null) {
                        baseCondition = baseCondition and (PersonalCreditTransactions.type eq txType)
                    }
                }
                
                val query = PersonalCreditTransactions
                    .select(PersonalCreditTransactions.columns)
                    .where { baseCondition }
                
                val allResults = query
                    .orderBy(PersonalCreditTransactions.createdAt to SortOrder.DESC)
                    .map { row ->
                        val studentId = row[PersonalCreditTransactions.studentId]
                        val studentName = studentId?.let { sid ->
                            students.find { it.studentId == sid }?.studentName
                        }
                        WalletTransactionDTO(
                            id = row[PersonalCreditTransactions.id].value,
                            description = row[PersonalCreditTransactions.description],
                            amount = row[PersonalCreditTransactions.amount],
                            type = row[PersonalCreditTransactions.type].name,
                            date = row[PersonalCreditTransactions.createdAt].toString().take(10),
                            studentName = studentName
                        )
                    }
                
                // Filtro por busca de nome (client-side para simplicidade)
                val filtered = if (search.isNullOrBlank()) {
                    allResults
                } else {
                    allResults.filter { tx ->
                        tx.description.contains(search, ignoreCase = true) ||
                        (tx.studentName?.contains(search, ignoreCase = true) ?: false)
                    }
                }
                
                // Totais
                val totalPurchased = allResults.filter { it.type == "PURCHASE" }.sumOf { it.amount }
                val totalConsumed = allResults.filter { it.type == "DEBIT" || it.type == "EXPIRE" }.sumOf { it.amount }
                
                TransactionsResponseDTO(
                    totalPurchased = totalPurchased,
                    totalConsumed = totalConsumed,
                    transactions = filtered.drop(offset.toInt()).take(limit),
                    total = filtered.size
                )
            }
            
            if (result == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Profissional não encontrado"))
            } else {
                call.respond(HttpStatusCode.OK, result)
            }
        }
        
        // =============================================
        // POST /api/personal/wallet/buy-credits
        // Compra de créditos avulsos
        // =============================================
        post("/api/personal/wallet/buy-credits") {
            val principal = call.principal<JWTPrincipal>()
            val email = principal?.payload?.getClaim("email")?.asString()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
            
            val request = call.receive<BuyCreditsRequestDTO>()
            
            // Validar quantidade mínima
            if (request.quantity < 10) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "A compra mínima é de 10 créditos"))
            }
            
            val result = transaction {
                val professional = Professional.find { Professionals.email eq email }.singleOrNull()
                    ?: Professional.findById(1) // Fallback for testing generic logins
                    ?: return@transaction null
                
                // Creditar
                professional.creditsAvailable += request.quantity
                
                // Registrar transação
                PersonalCreditTransaction.new {
                    this.professional = professional
                    this.type = TransactionType.PURCHASE
                    this.amount = request.quantity
                    this.description = "Compra avulsa de ${request.quantity} créditos"
                }
                
                BuyCreditsResponseDTO(
                    success = true,
                    message = "Compra realizada com sucesso! ${request.quantity} créditos adicionados.",
                    newBalance = professional.creditsAvailable,
                    creditsAdded = request.quantity,
                    expiresIn = "5 meses"
                )
            }
            
            if (result == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Profissional não encontrado"))
            } else {
                call.respond(HttpStatusCode.OK, result)
            }
        }
    }
}

// =============================================
// DTOs
// =============================================

@Serializable
data class WalletResponseDTO(
    val balance: Int,
    val activeStudents: Int,
    val totalStudents: Int,
    val nextBillingDate: String,
    val planType: String,
    val suggestKatsuPro: Boolean,
    val transactions: List<WalletTransactionDTO>,
    val packages: List<CreditPackageDTO>
)

@Serializable
data class WalletTransactionDTO(
    val id: Int,
    val description: String,
    val amount: Int,
    val type: String, // PURCHASE, DEBIT, FREEZE, UNFREEZE, EXPIRE
    val date: String,
    val studentName: String? = null
)

@Serializable
data class CreditPackageDTO(
    val id: Int,
    val credits: Int,
    val priceInCents: Int,
    val label: String,
    val priceLabel: String,
    val pricePerCredit: String,
    val savings: String? = null,
    val popular: Boolean = false
)

@Serializable
data class TransactionsResponseDTO(
    val totalPurchased: Int,
    val totalConsumed: Int,
    val transactions: List<WalletTransactionDTO>,
    val total: Int
)

@Serializable
data class BuyCreditsRequestDTO(
    val quantity: Int,
    val paymentMethod: String // PIX, CARTAO
)

@Serializable
data class BuyCreditsResponseDTO(
    val success: Boolean,
    val message: String,
    val newBalance: Int,
    val creditsAdded: Int,
    val expiresIn: String
)
