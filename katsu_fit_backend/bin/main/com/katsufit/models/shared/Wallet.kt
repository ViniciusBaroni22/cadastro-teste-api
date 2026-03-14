package com.katsufit.models.shared

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Wallets : IntIdTable("wallets") {
    val nutritionistId = reference("nutritionist_id", Users, onDelete = ReferenceOption.CASCADE)
    val creditsBalance = integer("credits_balance").default(0)
    val autoDeduct = bool("auto_deduct").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

@Serializable
data class WalletResponse(
    val id: Int,
    val nutritionistId: String,
    val creditsBalance: Int,
    val autoDeduct: Boolean,
    val activeClientsCount: Int,
    val nextConsumptionDate: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class WalletUpdateRequest(
    val autoDeduct: Boolean
)
