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
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

fun Route.couponRoutes() {
    route("/api/coupons") {
        
        // Todas as rotas precisam de autenticação JWT
        authenticate("auth-jwt") {
            
            // POST /api/coupons - Criar novo cupom
            post {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = UUID.fromString(principal!!.payload.getClaim("id").asString())
                
                val request = call.receive<CreateCouponRequest>()
                
                // Validações
                if (request.code.isBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Código do cupom é obrigatório"))
                }
                
                if (request.value <= 0) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Valor do desconto deve ser maior que zero"))
                }
                
                if (request.type == CouponType.PERCENTAGE && request.value > 100) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Desconto percentual não pode ser maior que 100%"))
                }
                
                // Normaliza código: maiúsculas, sem espaços
                val normalizedCode = request.code.uppercase().trim().replace("\\s+".toRegex(), "")
                
                // Verifica se já existe cupom com mesmo código para este profissional
                val exists = transaction {
                    CouponEntity.find { 
                        (CouponsTable.professionalId eq professionalId) and 
                        (CouponsTable.code eq normalizedCode) 
                    }.firstOrNull() != null
                }
                
                if (exists) {
                    return@post call.respond(HttpStatusCode.Conflict, mapOf("error" to "Você já possui um cupom com este código"))
                }
                
                // Parse da data de expiração se fornecida
                val expiresAt = request.expiresAt?.let {
                    try {
                        LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
                    } catch (e: Exception) {
                        return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Formato de data inválido. Use ISO 8601 (YYYY-MM-DDTHH:mm:ss)"))
                    }
                }
                
                // Cria o cupom
                val coupon = transaction {
                    CouponEntity.new {
                        this.professionalId = professionalId
                        this.code = normalizedCode
                        this.description = request.description?.take(255)
                        this.type = request.type
                        this.value = request.value.toBigDecimal()
                        this.minPurchaseAmount = request.minPurchaseAmount?.toBigDecimal()
                        this.maxUses = request.maxUses
                        this.currentUses = 0
                        this.expiresAt = expiresAt
                        this.isActive = true
                        this.createdAt = LocalDateTime.now()
                        this.updatedAt = LocalDateTime.now()
                    }.toResponseDTO()
                }
                
                call.respond(HttpStatusCode.Created, coupon)
            }
            
            // GET /api/coupons - Listar cupons do profissional
            get {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = UUID.fromString(principal!!.payload.getClaim("id").asString())
                
                val showInactive = call.request.queryParameters["showInactive"]?.toBoolean() ?: false
                
                val coupons = transaction {
                    CouponEntity.find { CouponsTable.professionalId eq professionalId }
                        .filter { showInactive || it.isActive }
                        .sortedByDescending { it.createdAt }
                        .map { it.toResponseDTO() }
                }
                
                call.respond(CouponListResponse(coupons, coupons.size))
            }
            
            // GET /api/coupons/{id} - Detalhes de um cupom específico
            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = UUID.fromString(principal!!.payload.getClaim("id").asString())
                val couponId = call.parameters["id"]?.toIntOrNull()
                
                if (couponId == null) {
                    return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                }
                
                val coupon = transaction {
                    CouponEntity.findById(couponId)?.takeIf { it.professionalId == professionalId }?.toResponseDTO()
                }
                
                if (coupon == null) {
                    return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Cupom não encontrado"))
                }
                
                call.respond(coupon)
            }
            
            // PUT /api/coupons/{id} - Atualizar cupom (descrição, status, validade, limite)
            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = UUID.fromString(principal!!.payload.getClaim("id").asString())
                val couponId = call.parameters["id"]?.toIntOrNull()
                
                if (couponId == null) {
                    return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                }
                
                val request = call.receive<UpdateCouponRequest>()
                
                val updated = transaction {
                    val coupon = CouponEntity.findById(couponId)?.takeIf { it.professionalId == professionalId }
                        ?: return@transaction null
                    
                    // Atualiza campos permitidos
                    request.description?.let { coupon.description = it.take(255) }
                    request.isActive?.let { coupon.isActive = it }
                    
                    request.expiresAt?.let {
                        coupon.expiresAt = try {
                            LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
                        } catch (e: Exception) {
                            return@transaction Pair(null, "Formato de data inválido")
                        }
                    }
                    
                    // Só permite aumentar o limite, nunca diminuir abaixo do já usado
                    request.maxUses?.let { newLimit ->
                        if (newLimit < coupon.currentUses) {
                            return@transaction Pair(null, "Novo limite não pode ser menor que usos atuais (${coupon.currentUses})")
                        }
                        coupon.maxUses = newLimit
                    }
                    
                    coupon.updatedAt = LocalDateTime.now()
                    Pair(coupon.toResponseDTO(), null)
                }
                
                if (updated == null) {
                    return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Cupom não encontrado"))
                }
                
                if (updated.second != null) {
                    return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to updated.second))
                }
                
                call.respond(updated.first!!)
            }
            
            // DELETE /api/coupons/{id} - Desativar cupom (soft delete lógico)
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = UUID.fromString(principal!!.payload.getClaim("id").asString())
                val couponId = call.parameters["id"]?.toIntOrNull()
                
                if (couponId == null) {
                    return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                }
                
                val success = transaction {
                    val coupon = CouponEntity.findById(couponId)?.takeIf { it.professionalId == professionalId }
                    if (coupon != null) {
                        coupon.isActive = false
                        coupon.updatedAt = LocalDateTime.now()
                        true
                    } else {
                        false
                    }
                }
                
                if (!success) {
                    return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Cupom não encontrado"))
                }
                
                call.respond(HttpStatusCode.OK, mapOf("message" to "Cupom desativado com sucesso"))
            }
            
            // POST /api/coupons/validate - Validar cupom (usado no checkout)
            post("/validate") {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = UUID.fromString(principal!!.payload.getClaim("id").asString())
                
                val request = call.receive<ValidateCouponRequest>()
                
                if (request.code.isBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, CouponValidationResponse(
                        valid = false, message = "Código do cupom é obrigatório"
                    ))
                }
                
                val normalizedCode = request.code.uppercase().trim().replace("\\s+".toRegex(), "")
                
                val result = transaction {
                    val coupon = CouponEntity.find {
                        (CouponsTable.professionalId eq professionalId) and
                        (CouponsTable.code eq normalizedCode)
                    }.firstOrNull()
                    
                    if (coupon == null) {
                        return@transaction CouponValidationResponse(valid = false, message = "Cupom não encontrado")
                    }
                    
                    // Verifica se está ativo
                    if (!coupon.isActive) {
                        return@transaction CouponValidationResponse(valid = false, message = "Cupom está inativo")
                    }
                    
                    // Verifica expiração
                    coupon.expiresAt?.let {
                        if (it.isBefore(LocalDateTime.now())) {
                            return@transaction CouponValidationResponse(valid = false, message = "Cupom expirado")
                        }
                    }
                    
                    // Verifica limite de usos
                    coupon.maxUses?.let {
                        if (coupon.currentUses >= it) {
                            return@transaction CouponValidationResponse(valid = false, message = "Limite de usos atingido")
                        }
                    }
                    
                    // Verifica valor mínimo de compra
                    coupon.minPurchaseAmount?.let {
                        if (request.purchaseAmount < it.toDouble()) {
                            return@transaction CouponValidationResponse(
                                valid = false, 
                                message = "Valor mínimo para este cupom é R$ ${it.toDouble()}"
                            )
                        }
                    }
                    
                    // Calcula desconto
                    val discountAmount = when (coupon.type) {
                        CouponType.PERCENTAGE -> {
                            val discount = request.purchaseAmount * (coupon.value.toDouble() / 100)
                            discount
                        }
                        CouponType.FIXED -> {
                            val discount = coupon.value.toDouble()
                            if (discount > request.purchaseAmount) request.purchaseAmount else discount
                        }
                    }
                    
                    val finalAmount = request.purchaseAmount - discountAmount
                    
                    CouponValidationResponse(
                        valid = true,
                        coupon = coupon.toResponseDTO(),
                        discountAmount = discountAmount,
                        finalAmount = finalAmount,
                        message = "Cupom válido!"
                    )
                }
                
                call.respond(HttpStatusCode.OK, result)
            }
            
            // POST /api/coupons/{id}/apply - Aplicar cupom (incrementa contador de uso)
            // Chamado APÓS confirmação de pagamento para registrar o uso real
            post("/{id}/apply") {
                val principal = call.principal<JWTPrincipal>()
                val professionalId = UUID.fromString(principal!!.payload.getClaim("id").asString())
                val couponId = call.parameters["id"]?.toIntOrNull()
                
                if (couponId == null) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                }
                
                val success = transaction {
                    val coupon = CouponEntity.findById(couponId)?.takeIf { it.professionalId == professionalId }
                        ?: return@transaction false
                    
                    // Só incrementa se ainda estiver válido
                    if (!coupon.isActive) return@transaction false
                    coupon.expiresAt?.let { if (it.isBefore(LocalDateTime.now())) return@transaction false }
                    coupon.maxUses?.let { if (coupon.currentUses >= it) return@transaction false }
                    
                    coupon.currentUses += 1
                    coupon.updatedAt = LocalDateTime.now()
                    true
                }
                
                if (!success) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Não foi possível aplicar o cupom"))
                }
                
                call.respond(HttpStatusCode.OK, mapOf("message" to "Cupom aplicado com sucesso"))
            }
        }
    }
}
