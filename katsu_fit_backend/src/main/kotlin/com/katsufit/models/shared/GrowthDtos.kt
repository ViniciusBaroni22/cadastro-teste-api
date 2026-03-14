package com.katsufit.models.shared

import kotlinx.serialization.Serializable

// =====================================
// DTOs PARA MEU CRESCIMENTO (GROWTH)
// =====================================

@Serializable
data class GrowthDashboardDTO(
    val score: GrowthScoreDTO,
    val financial: GrowthFinancialDTO,
    val metrics: GrowthMetricsDTO,
    val chartData: ChartDataDTO,
    val topCoupons: List<CouponPerformanceDTO>,
    val insight: GrowthInsightDTO
)

@Serializable
data class GrowthScoreDTO(
    val current: Int,
    val max: Int = 100,
    val category: String,
    val message: String,
    val targetStudents: Int = 16,
    val currentStudents: Int,
    val breakdown: ScoreBreakdownDTO
)

@Serializable
data class ScoreBreakdownDTO(
    val studentsScore: Int,
    val retentionScore: Int,
    val engagementScore: Int,
    val financialScore: Int
)

@Serializable
data class GrowthFinancialDTO(
    val monthlyRevenue: Double,
    val targetProgress: Int,
    val currentStudents: Int,
    val targetStudents: Int,
    val couponsSavings: Double
)

@Serializable
data class GrowthMetricsDTO(
    val retention: RetentionMetricDTO,
    val messages: MessagesMetricDTO,
    val responseTime: ResponseTimeMetricDTO,
    val renewalRate: RenewalMetricDTO
)

@Serializable
data class RetentionMetricDTO(
    val averageMonths: Double,
    val trend: String
)

@Serializable
data class MessagesMetricDTO(
    val total: Int,
    val trend: String
)

@Serializable
data class ResponseTimeMetricDTO(
    val averageHours: Double,
    val trend: String
)

@Serializable
data class RenewalMetricDTO(
    val percentage: Int,
    val trend: String
)

@Serializable
data class ChartDataDTO(
    val labels: List<String>,
    val currentStudents: List<Int>,
    val targetLine: List<Int>
)

@Serializable
data class CouponPerformanceDTO(
    val code: String,
    val uses: Int,
    val totalSavings: Double
)

@Serializable
data class GrowthInsightDTO(
    val icon: String,
    val title: String,
    val message: String
)
