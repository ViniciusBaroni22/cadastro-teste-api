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
import io.ktor.server.plugins.ContentTransformationException
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.and
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

fun Route.userRouting(jwtSecret: String, jwtIssuer: String, jwtAudience: String) {
    route("/api") {
        
        // --- ROTA DE REGISTRO (Igual) ---
        post("/register") {
            try {
                val userRequest = call.receive<UserRegisterRequest>()
                val passwordHash = BCrypt.hashpw(userRequest.password, BCrypt.gensalt())

                transaction {
                    Users.insert {
                        it[Users.email] = userRequest.email
                        it[Users.passwordHash] = passwordHash
                        it[Users.userType] = userRequest.userType
                        it[Users.name] = userRequest.name
                    }
                }
                call.respondText("Registro realizado com sucesso", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respondText("Erro: ${e.message}", status = HttpStatusCode.InternalServerError)
            }
        }

        // --- ROTA DE LOGIN (Igual) ---
        post("/login") {
            try {
                val userRequest = call.receive<UserLoginRequest>()
                val userFromDb = transaction {
                    Users.select { Users.email eq userRequest.email }.singleOrNull()
                }

                if (userFromDb != null && BCrypt.checkpw(userRequest.password, userFromDb[Users.passwordHash])) {
                    val userId = userFromDb[Users.id].toString()
                    val userName = userFromDb[Users.name]
                    val userType = userFromDb[Users.userType]
                    
                    val token = JWT.create()
                        .withAudience(jwtAudience)
                        .withIssuer(jwtIssuer)
                        .withClaim("email", userFromDb[Users.email])
                        .withClaim("id", userId)
                        .withClaim("userType", userType) // IMPORTANTE: Para frontend saber qual dashboard abrir
                        .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
                        .sign(Algorithm.HMAC256(jwtSecret))

                    call.respond(hashMapOf(
                        "token" to token, 
                        "name" to userName,
                        "userType" to userType,
                        "id" to userId
                    ))
                } else {
                    call.respondText("Credenciais inválidas", status = HttpStatusCode.Unauthorized)
                }
            } catch (e: Exception) {
                call.respondText("Erro no login", status = HttpStatusCode.InternalServerError)
            }
        }

        // --- ROTA DE VINCULAR PACIENTE (CORRIGIDA) ---
        authenticate("auth-jwt") {
            post("/patient/invite") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val nutritionistId = principal!!.payload.getClaim("id").asString()
                    val nutritionistUuid = UUID.fromString(nutritionistId)

                    val request = call.receive<PatientInviteRequest>()
                    
                    if (request.email.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "E-mail inválido")
                        return@post
                    }

                    transaction {
                        // Verifica se existe e pega o ID puro (.value)
                        var patientUuid: UUID? = Users.select { Users.email eq request.email }
                            .singleOrNull()?.get(Users.id)?.value

                        // Se não existir, cria e pega o ID puro (.value)
                        if (patientUuid == null) {
                            // --- AQUI ESTAVA O ERRO, AGORA CORRIGIDO COM .value ---
                            patientUuid = (Users.insert {
                                it[email] = request.email
                                it[passwordHash] = ""
                                it[userType] = "client"
                                it[name] = "Convidado"
                            } get Users.id).value
                        }

                        // Verifica vínculo
                        val linkExists = NutritionistPatientLinks.select {
                            (NutritionistPatientLinks.nutritionist eq nutritionistUuid) and
                            (NutritionistPatientLinks.patient eq patientUuid!!)
                        }.count() > 0

                        // Cria vínculo
                        if (!linkExists) {
                            NutritionistPatientLinks.insert {
                                it[nutritionist] = nutritionistUuid
                                it[patient] = patientUuid!!
                            }
                            println("✅ VÍNCULO SALVO: Nutri $nutritionistId <-> Paciente ${request.email}")
                        } else {
                            println("⚠️ Vínculo já existia.")
                        }
                    }

                    call.respond(HttpStatusCode.OK, mapOf("message" to "Paciente vinculado com sucesso!"))
                    
                } catch (e: Exception) {
                    println("❌ Erro ao vincular: ${e.message}")
                    e.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest, "Erro ao processar convite")
                }
            }
        }
    }
}
