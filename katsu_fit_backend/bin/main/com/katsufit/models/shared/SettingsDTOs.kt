package com.katsufit.models.shared

import kotlinx.serialization.Serializable

/**
 * DTOs para Settings/Configurações
 */

// Resposta completa das configurações
@Serializable
data class UserSettingsResponse(
    val id: Int,
    val userId: String,
    val notificacoes: NotificacoesSettings,
    val aparencia: AparenciaSettings,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class NotificacoesSettings(
    val pushConsultas: Boolean,
    val emailConsumo: Boolean,
    val lembretesRenovacao: Boolean
)

@Serializable
data class AparenciaSettings(
    val tema: String, // "claro", "escuro", "sistema"
    val idioma: String // "pt-BR", "en-US", "es-ES"
)

// Update de notificações
@Serializable
data class UpdateNotificacoesRequest(
    val pushConsultas: Boolean? = null,
    val emailConsumo: Boolean? = null,
    val lembretesRenovacao: Boolean? = null
)

// Update de aparência
@Serializable
data class UpdateAparenciaRequest(
    val tema: String? = null,
    val idioma: String? = null
)

// Dados da assinatura (baseado no wallet existente)
@Serializable
data class SubscriptionInfoResponse(
    val plano: String, // "starter", "pro"
    val status: String, // "ativo", "inativo"
    val proximaCobranca: String?, // ISO 8601 ou null
    val valorMensal: Double,
    val alunosAtivos: Int,
    val limiteAlunos: Int // 16 é o break-even
)

// Histórico de pagamento
@Serializable
data class PaymentHistoryItem(
    val id: String,
    val data: String,
    val descricao: String,
    val valor: Double,
    val status: String // "pago", "pendente", "cancelado"
)

// Resposta de sucesso genérica
@Serializable
data class SettingsSuccessResponse(
    val success: Boolean,
    val message: String
)

// Erro
@Serializable
data class SettingsErrorResponse(
    val error: String,
    val code: String? = null
)
