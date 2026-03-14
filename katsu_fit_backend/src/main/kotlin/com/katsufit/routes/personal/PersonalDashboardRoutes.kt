package com.katsufit.routes.personal

import com.katsufit.models.personal.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

fun Route.personalDashboardRoutes() {
    authenticate("auth-jwt") {
        get("/api/personal/dashboard") {
            val principal = call.principal<JWTPrincipal>()
            val email = principal?.payload?.getClaim("email")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            
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
                val studentsWithoutCredit = students.filter { !it.hasValidCredit() }
                
                val today = LocalDate.now()
                val workoutsToday = 4 // Mock - integrar com tabela de agendamentos quando existir
                
                DashboardResponse(
                    professional = ProfessionalSummary(
                        id = professional.id.value,
                        fullName = professional.fullName,
                        avatarUrl = professional.avatarUrl,
                        cref = professional.cref,
                        planType = professional.planType.name,
                        creditsAvailable = if (professional.planType == PlanType.STARTER) professional.creditsAvailable else null,
                        creditsFrozen = if (professional.planType == PlanType.PRO) professional.creditsFrozen else null,
                        proRenewsAt = professional.proRenewsAt?.toString()
                    ),
                    metrics = DashboardMetrics(
                        activeStudentsCount = activeStudents.size,
                        workoutsToday = workoutsToday,
                        weeklyVolume = "12.450 kg", // Mock
                        attendance = "85%" // Mock
                    ),
                    creditStatus = if (professional.planType == PlanType.STARTER) {
                        CreditStatus(
                            available = professional.creditsAvailable,
                            activeCredits = activeStudents.size,
                            studentsWithCredits = activeStudents.map { 
                                StudentCreditInfo(
                                    id = it.studentId,
                                    name = it.studentName,
                                    avatar = it.studentAvatar,
                                    daysRemaining = it.daysUntilExpiry(),
                                    objective = it.objective
                                )
                            }
                        )
                    } else null,
                    proStatus = if (professional.planType == PlanType.PRO) {
                        ProStatus(
                            isActive = true,
                            renewsAt = professional.proRenewsAt?.toString() ?: "N/A",
                            studentsLimit = "Ilimitado"
                        )
                    } else null,
                    walletBalance = professional.creditsAvailable,
                    suggestKatsuPro = professional.planType == PlanType.STARTER && activeStudents.size >= 16,
                    alerts = buildList {
                        if (professional.planType == PlanType.STARTER && professional.creditsAvailable < 5) {
                            add(DashboardAlert(
                                type = "warning",
                                title = "Créditos baixos",
                                message = "Você tem apenas ${professional.creditsAvailable} créditos restantes",
                                action = "Comprar créditos"
                            ))
                        }
                        if (studentsWithoutCredit.isNotEmpty()) {
                            add(DashboardAlert(
                                type = "info",
                                title = "Alunos sem crédito",
                                message = "${studentsWithoutCredit.size} aluno(s) aguardando ativação",
                                action = "Ver alunos"
                            ))
                        }
                        if (professional.planType == PlanType.STARTER && activeStudents.size >= 16) {
                            add(DashboardAlert(
                                type = "upgrade",
                                title = "Migre para o Katsu Pro!",
                                message = "Com ${activeStudents.size} alunos, o Katsu Pro (R\$ 79,90/mês) sai mais em conta e com alunos ilimitados!",
                                action = "Ver Katsu Pro"
                            ))
                        }
                    }
                )
            }
            
            if (response == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Profissional não encontrado"))
            } else {
                call.respond(HttpStatusCode.OK, response)
            }
        }
        
        get("/api/personal/dashboard/students") {
            val principal = call.principal<JWTPrincipal>()
            val email = principal?.payload?.getClaim("email")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            
            val students = transaction {
                val professional = Professional.find { Professionals.email eq email }.singleOrNull()
                    ?: Professional.findById(1) // Fallback for testing generic logins
                    ?: return@transaction emptyList()
                
                val professionalId = professional.id.value
                
                ProfessionalStudent.find {
                    ProfessionalStudents.professionalId eq professionalId
                }.toList()
            }
            
            call.respond(students.map { 
                StudentDashboardItem(
                    id = it.studentId,
                    name = it.studentName,
                    avatar = it.studentAvatar,
                    age = it.age,
                    objective = it.objective,
                    hasActiveCredit = it.hasValidCredit(),
                    creditExpiresAt = it.creditExpiresAt?.toString(),
                    daysRemaining = it.daysUntilExpiry(),
                    status = if (it.hasValidCredit()) "active" else "inactive"
                )
            })
        }
    }
}

// DTOs
@Serializable
data class DashboardResponse(
    val professional: ProfessionalSummary,
    val metrics: DashboardMetrics,
    val creditStatus: CreditStatus?,
    val proStatus: ProStatus?,
    val walletBalance: Int = 0,
    val suggestKatsuPro: Boolean = false,
    val alerts: List<DashboardAlert>
)

@Serializable
data class ProfessionalSummary(
    val id: Int,
    val fullName: String,
    val avatarUrl: String?,
    val cref: String,
    val planType: String,
    val creditsAvailable: Int?,
    val creditsFrozen: Int?,
    val proRenewsAt: String?
)

@Serializable
data class DashboardMetrics(
    val activeStudentsCount: Int,
    val workoutsToday: Int,
    val weeklyVolume: String,
    val attendance: String
)

@Serializable
data class CreditStatus(
    val available: Int,
    val activeCredits: Int,
    val studentsWithCredits: List<StudentCreditInfo>
)

@Serializable
data class StudentCreditInfo(
    val id: Int,
    val name: String,
    val avatar: String?,
    val daysRemaining: Int,
    val objective: String?
)

@Serializable
data class ProStatus(
    val isActive: Boolean,
    val renewsAt: String,
    val studentsLimit: String
)

@Serializable
data class DashboardAlert(
    val type: String,
    val title: String,
    val message: String,
    val action: String
)

@Serializable
data class StudentDashboardItem(
    val id: Int,
    val name: String,
    val avatar: String?,
    val age: Int?,
    val objective: String?,
    val hasActiveCredit: Boolean,
    val creditExpiresAt: String?,
    val daysRemaining: Int,
    val status: String
)
