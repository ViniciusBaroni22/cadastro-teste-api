package com.katsufit.models.shared

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

// TABELA EXPOSED
object CouponsTable : IntIdTable("coupons") {
    val professionalId = uuid("professional_id").index() // UUID do profissional dono
    val code = varchar("code", 50).index() // Código do cupom (ex: BLACKFRIDAY20)
    val description = varchar("description", 255).nullable() // Descrição opcional
    val type = enumerationByName("type", 20, CouponType::class) // PERCENTAGE ou FIXED
    val value = decimal("value", 10, 2) // Valor do desconto (ex: 20.00 = 20% ou R$20,00)
    val minPurchaseAmount = decimal("min_purchase_amount", 10, 2).nullable() // Valor mínimo para aplicar (ex: R$60,00)
    val maxUses = integer("max_uses").nullable() // Limite de usos (null = ilimitado)
    val currentUses = integer("current_uses").default(0) // Contador de usos atuais
    val expiresAt = datetime("expires_at").nullable() // Data de expiração
    val isActive = bool("is_active").default(true) // Ativo/Inativo
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    
    init {
        // Código único por profissional (não pode ter 2 cupons com mesmo código para o mesmo profissional)
        uniqueIndex("idx_professional_code", professionalId, code)
    }
}

// ENTITY DAO
class CouponEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CouponEntity>(CouponsTable)
    
    var professionalId by CouponsTable.professionalId
    var code by CouponsTable.code
    var description by CouponsTable.description
    var type by CouponsTable.type
    var value by CouponsTable.value
    var minPurchaseAmount by CouponsTable.minPurchaseAmount
    var maxUses by CouponsTable.maxUses
    var currentUses by CouponsTable.currentUses
    var expiresAt by CouponsTable.expiresAt
    var isActive by CouponsTable.isActive
    var createdAt by CouponsTable.createdAt
    var updatedAt by CouponsTable.updatedAt
    
    fun toResponseDTO() = CouponResponse(
        id = id.value,
        professionalId = professionalId.toString(),
        code = code.uppercase(),
        description = description,
        type = type,
        value = value.toDouble(),
        minPurchaseAmount = minPurchaseAmount?.toDouble(),
        maxUses = maxUses,
        currentUses = currentUses,
        expiresAt = expiresAt?.toString(),
        isActive = isActive,
        isExpired = expiresAt?.isBefore(LocalDateTime.now()) ?: false,
        isUsageLimitReached = maxUses?.let { currentUses >= it } ?: false,
        createdAt = createdAt.toString()
    )
}

// ENUMS
enum class CouponType {
    PERCENTAGE, // Desconto em % (0-100)
    FIXED       // Desconto em valor fixo R$
}

// DTOs DE REQUEST (entrada)
@Serializable
data class CreateCouponRequest(
    val code: String,
    val description: String? = null,
    val type: CouponType,
    val value: Double, // Se PERCENTAGE: 0-100 | Se FIXED: valor em reais
    val minPurchaseAmount: Double? = null, // Ex: 60.00 para pacote starter
    val maxUses: Int? = null, // null = ilimitado
    val expiresAt: String? = null // ISO 8601 (YYYY-MM-DDTHH:mm:ss) ou null = nunca expira
)

@Serializable
data class UpdateCouponRequest(
    val description: String? = null,
    val isActive: Boolean? = null,
    val expiresAt: String? = null,
    val maxUses: Int? = null // Só permite aumentar, nunca diminuir abaixo do currentUses
)

@Serializable
data class ValidateCouponRequest(
    val code: String,
    val purchaseAmount: Double // Valor da compra para validar minPurchaseAmount
)

// DTOs DE RESPONSE (saída)
@Serializable
data class CouponResponse(
    val id: Int,
    val professionalId: String, // Changed from UUID to String for serialization
    val code: String,
    val description: String?,
    val type: CouponType,
    val value: Double,
    val minPurchaseAmount: Double?,
    val maxUses: Int?,
    val currentUses: Int,
    val expiresAt: String?,
    val isActive: Boolean,
    val isExpired: Boolean,
    val isUsageLimitReached: Boolean,
    val createdAt: String
)

@Serializable
data class CouponValidationResponse(
    val valid: Boolean,
    val coupon: CouponResponse? = null,
    val discountAmount: Double? = null, // Valor calculado do desconto
    val finalAmount: Double? = null,    // Valor final após desconto
    val message: String? = null         // Mensagem de erro se inválido
)

@Serializable
data class CouponListResponse(
    val coupons: List<CouponResponse>,
    val total: Int
)
