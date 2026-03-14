package com.katsufit.models.shared

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object CreditTransactions : IntIdTable("credit_transactions") {
    val nutritionistId = reference("nutritionist_id", Users, onDelete = ReferenceOption.CASCADE)
    val type = varchar("type", 20)
    val amount = integer("amount")
    val description = varchar("description", 255)
    val patientId = reference("patient_id", Users, onDelete = ReferenceOption.SET_NULL).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

@Serializable
data class CreditTransactionResponse(
    val id: Int,
    val nutritionistId: String,
    val type: String,
    val amount: Int,
    val description: String,
    val patientId: String? = null,
    val patientName: String? = null,
    val createdAt: String
)

@Serializable
data class TransactionHistoryResponse(
    val transactions: List<CreditTransactionResponse>,
    val totalPurchased: Int,
    val totalConsumed: Int
)
