package com.katsufit.routes.shared

import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

@Serializable
data class ProfileAndPlanRequest(
    val documentId: String,
    val name: String,
    val profession: String,
    val bio: String? = null,
    val specialty: String? = null,
    val clientId: String,
    val planName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val description: String? = null
)

fun Route.professionalProfileRouting() {
    authenticate("auth-jwt") {
        route("/api/profile") {
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) }
                    if (userId == null) {
                        call.respondText("Token de usuário inválido", status = HttpStatusCode.Unauthorized)
                        return@post
                    }

                    val request = call.receive<ProfileAndPlanRequest>()

                    transaction {
                        // Se já existe profile para este user, usa ele; senão cria
                        val profileId = ProfessionalProfiles
                            .select { ProfessionalProfiles.userId eq userId }
                            .singleOrNull()
                            ?.get(ProfessionalProfiles.id)
                            ?: ProfessionalProfiles.insertAndGetId {
                                it[ProfessionalProfiles.userId] = userId
                                it[ProfessionalProfiles.documentId] = request.documentId
                                it[ProfessionalProfiles.name] = request.name
                                it[ProfessionalProfiles.profession] = request.profession
                                it[ProfessionalProfiles.bio] = request.bio
                                it[ProfessionalProfiles.specialty] = request.specialty
                            }

                        val profileUuid = profileId.value

                        // Cria o plano apropriado conforme profession
                        if (!request.planName.isNullOrBlank()) {
                            when (request.profession.lowercase()) {
                                "personal" -> {
                                    TrainingPlans.insert {
                                        it[TrainingPlans.professionalId] = profileUuid
                                        it[TrainingPlans.clientId] = UUID.fromString(request.clientId)
                                        it[TrainingPlans.name] = request.planName
                                        it[TrainingPlans.description] = request.description
                                    }
                                }
                                "nutri" -> {
                                    NutritionPlans.insert {
                                        it[NutritionPlans.professionalId] = profileUuid
                                        it[NutritionPlans.clientId] = UUID.fromString(request.clientId)
                                        it[NutritionPlans.name] = request.planName
                                        it[NutritionPlans.startDate] = request.startDate
                                        it[NutritionPlans.endDate] = request.endDate
                                        it[NutritionPlans.description] = request.description
                                    }
                                }
                                else -> {
                                    // não cria plano se profissão desconhecida
                                }
                            }
                        }
                    }

                    call.respondText("Perfil e plano criados com sucesso!", status = HttpStatusCode.OK)
                } catch (e: Exception) {
                    call.respondText("Erro ao criar perfil e plano: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }
        }
    }
}
