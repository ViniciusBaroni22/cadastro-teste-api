package com.katsufit.routes.shared

import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import kotlinx.datetime.toJavaInstant

fun Route.growthRoutes() {
    authenticate("auth-jwt") {
        get("/api/growth/dashboard") {
            val principal = call.principal<JWTPrincipal>()
            val odUserId = principal?.payload?.getClaim("id")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

            val professionalId = UUID.fromString(odUserId)

            val dashboard = transaction {
                generateGrowthDashboard(professionalId)
            }

            call.respond(HttpStatusCode.OK, dashboard)
        }
    }
}

private fun generateGrowthDashboard(professionalId: UUID): GrowthDashboardDTO {
    val currentStudents = countActiveStudents(professionalId)
    val targetStudents = 16
    
    val score = calculateGrowthScore(currentStudents, professionalId)
    val financial = calculateFinancialMetrics(professionalId, currentStudents, targetStudents)
    val metrics = calculateDetailedMetrics(professionalId)
    val chartData = generateChartData(professionalId)
    val topCoupons = getTopCoupons(professionalId)
    val insight = generateInsight(currentStudents, metrics)

    return GrowthDashboardDTO(
        score = score,
        financial = financial,
        metrics = metrics,
        chartData = chartData,
        topCoupons = topCoupons,
        insight = insight
    )
}

// Conta pacientes vinculados ao nutricionista
private fun countActiveStudents(professionalId: UUID): Int {
    return NutritionistPatientLinks
        .select { NutritionistPatientLinks.nutritionist eq professionalId }
        .count()
        .toInt()
}

private fun calculateGrowthScore(currentStudents: Int, professionalId: UUID): GrowthScoreDTO {
    val studentsScore = ((currentStudents.toDouble() / 16.0) * 40).toInt().coerceAtMost(40)
    val retentionScore = calculateRetentionScore(professionalId)
    val engagementScore = calculateEngagementScore(professionalId)
    val financialScore = 10
    
    val totalScore = studentsScore + retentionScore + engagementScore + financialScore
    
    val category = when {
        totalScore >= 90 -> "katsu_master"
        totalScore >= 70 -> "pro_path"
        totalScore >= 40 -> "growing"
        else -> "needs_attention"
    }
    
    val message = when {
        totalScore >= 90 -> "Você é um Katsu Master! 🌟"
        totalScore >= 70 -> "Você está no caminho do Pro!"
        totalScore >= 40 -> "Continue crescendo!"
        else -> "Vamos acelerar seu crescimento!"
    }

    return GrowthScoreDTO(
        current = totalScore,
        category = category,
        message = message,
        currentStudents = currentStudents,
        breakdown = ScoreBreakdownDTO(
            studentsScore = studentsScore,
            retentionScore = retentionScore,
            engagementScore = engagementScore,
            financialScore = financialScore
        )
    )
}

private fun calculateRetentionScore(professionalId: UUID): Int {
    val avgRetention = getAverageRetentionMonths(professionalId)
    return when {
        avgRetention >= 6.0 -> 30
        avgRetention >= 4.0 -> 25
        avgRetention >= 2.0 -> 15
        else -> 10
    }
}

private fun calculateEngagementScore(professionalId: UUID): Int {
    val responseTime = getAverageResponseTime(professionalId)
    return when {
        responseTime <= 1.0 -> 20
        responseTime <= 2.0 -> 18
        responseTime <= 4.0 -> 15
        responseTime <= 8.0 -> 10
        else -> 5
    }
}

private fun calculateFinancialMetrics(
    professionalId: UUID, 
    currentStudents: Int, 
    targetStudents: Int
): GrowthFinancialDTO {
    val monthlyRevenue = getMonthlyRevenue(professionalId)
    val targetProgress = ((currentStudents.toDouble() / targetStudents.toDouble()) * 100).toInt()
    val couponsSavings = getTotalCouponsSavings(professionalId)

    return GrowthFinancialDTO(
        monthlyRevenue = monthlyRevenue,
        targetProgress = targetProgress,
        currentStudents = currentStudents,
        targetStudents = targetStudents,
        couponsSavings = couponsSavings
    )
}

// Receita mensal (compras de crédito do mês atual)
private fun getMonthlyRevenue(professionalId: UUID): Double {
    val startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay()
        .atZone(ZoneId.systemDefault()).toInstant()
    val kotlinStartOfMonth = kotlinx.datetime.Instant.fromEpochMilliseconds(startOfMonth.toEpochMilli())
    
    // Soma os valores de compra de créditos do mês
    val result = CreditTransactions
        .select { 
            (CreditTransactions.nutritionistId eq professionalId) and
            (CreditTransactions.type eq "purchase") and
            (CreditTransactions.createdAt greaterEq kotlinStartOfMonth)
        }
        .sumOf { it[CreditTransactions.amount] }
    
    // Converte créditos para valor em reais (assumindo R$60 = 60 créditos básico)
    return result.toDouble()
}

// Total de descontos dados por cupons (baseado no uso dos cupons)
private fun getTotalCouponsSavings(professionalId: UUID): Double {
    // Como não temos tabela de CouponUsages, vamos calcular baseado nos cupons usados
    val coupons = CouponsTable
        .select { CouponsTable.professionalId eq professionalId }
        .toList()
    
    var totalSavings = 0.0
    for (coupon in coupons) {
        val uses = coupon[CouponsTable.currentUses]
        val value = coupon[CouponsTable.value].toDouble()
        val type = coupon[CouponsTable.type]
        
        // Se é FIXED, o valor é direto. Se PERCENTAGE, estimamos baseado em R$60 médio
        val savingPerUse = if (type == CouponType.FIXED) value else (60.0 * value / 100.0)
        totalSavings += savingPerUse * uses
    }
    
    return totalSavings
}

private fun calculateDetailedMetrics(professionalId: UUID): GrowthMetricsDTO {
    return GrowthMetricsDTO(
        retention = RetentionMetricDTO(
            averageMonths = getAverageRetentionMonths(professionalId),
            trend = "up"
        ),
        messages = MessagesMetricDTO(
            total = getTotalMessages(professionalId),
            trend = "up"
        ),
        responseTime = ResponseTimeMetricDTO(
            averageHours = getAverageResponseTime(professionalId),
            trend = "down"
        ),
        renewalRate = RenewalMetricDTO(
            percentage = calculateRenewalRate(professionalId),
            trend = "stable"
        )
    )
}

// Tempo médio de retenção dos pacientes (em meses)
private fun getAverageRetentionMonths(professionalId: UUID): Double {
    val links = NutritionistPatientLinks
        .select { NutritionistPatientLinks.nutritionist eq professionalId }
        .toList()
    
    if (links.isEmpty()) return 0.0
    
    val now = java.time.Instant.now()
    val totalMonths = links.sumOf { row ->
        val createdAt = row[NutritionistPatientLinks.createdAt].toJavaInstant()
        ChronoUnit.DAYS.between(createdAt.atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now()) / 30.0
    }
    
    return totalMonths / links.size
}

// Total de mensagens enviadas pelo profissional
private fun getTotalMessages(professionalId: UUID): Int {
    return Messages
        .select { Messages.senderId eq professionalId }
        .count()
        .toInt()
}

// Tempo médio de resposta (mockado - implementar quando tiver lógica de resposta)
private fun getAverageResponseTime(professionalId: UUID): Double {
    // TODO: Implementar cálculo real baseado em timestamps de mensagens
    return 2.3
}

// Taxa de renovação (mockado - implementar quando tiver lógica de assinaturas)
private fun calculateRenewalRate(professionalId: UUID): Int {
    // TODO: Implementar quando tiver sistema de renovação mensal
    return 83
}

// Dados do gráfico de evolução de alunos (mockado - implementar histórico real)
private fun generateChartData(professionalId: UUID): ChartDataDTO {
    // TODO: Implementar com tabela de histórico mensal de alunos
    val labels = listOf("Ago", "Set", "Out", "Nov", "Dez", "Jan")
    val currentStudents = listOf(8, 10, 12, 11, 13, countActiveStudents(professionalId))
    val targetLine = List(6) { 16 }

    return ChartDataDTO(
        labels = labels,
        currentStudents = currentStudents,
        targetLine = targetLine
    )
}

// Top cupons por uso
private fun getTopCoupons(professionalId: UUID): List<CouponPerformanceDTO> {
    return CouponsTable
        .select { CouponsTable.professionalId eq professionalId }
        .orderBy(CouponsTable.currentUses, SortOrder.DESC)
        .limit(5)
        .map { row ->
            val value = row[CouponsTable.value].toDouble()
            val uses = row[CouponsTable.currentUses]
            val type = row[CouponsTable.type]
            val savingPerUse = if (type == CouponType.FIXED) value else (60.0 * value / 100.0)
            
            CouponPerformanceDTO(
                code = row[CouponsTable.code],
                uses = uses,
                totalSavings = savingPerUse * uses
            )
        }
}

private fun generateInsight(currentStudents: Int, metrics: GrowthMetricsDTO): GrowthInsightDTO {
    return when {
        currentStudents < 16 -> GrowthInsightDTO(
            icon = "trend",
            title = "Falta pouco para o Pro!",
            message = "Você tem ${String.format("%.1f", metrics.responseTime.averageHours)}h de tempo médio de resposta. Alunos respondidos em <2h têm 40% mais engajamento!"
        )
        else -> GrowthInsightDTO(
            icon = "star",
            title = "Você está voando!",
            message = "Com $currentStudents alunos, você já superou a meta! Considere aumentar seus preços."
        )
    }
}
