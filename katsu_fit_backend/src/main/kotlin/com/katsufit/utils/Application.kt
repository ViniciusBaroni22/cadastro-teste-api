package com.katsufit

import com.katsufit.models.shared.*
import com.katsufit.models.nutritionist.*
import com.katsufit.models.personal.*
import com.katsufit.models.personal.workout.*
import com.katsufit.models.personal.exercise.*

// ========================================
// ROTAS SHARED (Compartilhadas entre módulos)
// ========================================
import com.katsufit.routes.shared.trainingPlanRouting
import com.katsufit.routes.shared.progressRouting
import com.katsufit.routes.shared.clientDashboardRouting
import com.katsufit.routes.shared.messageRouting
import com.katsufit.routes.shared.userRouting
import com.katsufit.routes.shared.professionalProfileRouting
import com.katsufit.routes.shared.walletRouting
import com.katsufit.routes.shared.couponRoutes
import com.katsufit.routes.shared.growthRoutes
import com.katsufit.routes.shared.settingsRoutes
import com.katsufit.routes.shared.supportRoutes
import com.katsufit.routes.shared.authRoutes

// ========================================
// ROTAS NUTRITIONIST (Específicas do Nutricionista)
// ========================================
import com.katsufit.routes.nutritionist.nutritionPlanRouting
import com.katsufit.routes.nutritionist.nutritionistRouting
import com.katsufit.routes.nutritionist.foodRouting
import com.katsufit.routes.nutritionist.mealPlanRouting
import com.katsufit.routes.nutritionist.patientMealPlanRouting
import com.katsufit.routes.nutritionist.anamnesisRoutes
import com.katsufit.routes.nutritionist.documentRouting

// ========================================
// ROTAS PERSONAL (Específicas do Personal Trainer)
// ========================================
import com.katsufit.routes.personal.personalDashboardRoutes
import com.katsufit.routes.personal.personalWalletRoutes
import com.katsufit.routes.personal.workoutRoutes
import com.katsufit.routes.personal.studentRoutes
import com.katsufit.routes.personal.exerciseRoutes
import com.katsufit.routes.appointment.appointmentRoutes
import com.katsufit.models.appointment.Appointments
import com.katsufit.models.appointment.ProfessionalAvailabilities

import com.katsufit.seeds.AnamnesisSeeds
import com.katsufit.seeds.ExerciseSeeds
import com.katsufit.seeds.ProfessionalSeeds
import com.katsufit.seeds.FoodSeeds

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.http.content.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import java.io.File

fun main() {
    val jwtSecret = System.getenv("JWT_SECRET") ?: "minhachavesecreta123"
    val jwtIssuer = "emissor"
    val jwtAudience = "audiencia"

    // ========================================
    // CONEXÃO COM BANCO DE DADOS (Render + Local)
    // ========================================
    val dbHost = System.getenv("DB_HOST") ?: "localhost"
    val dbPort = System.getenv("DB_PORT") ?: "5432"
    val dbName = System.getenv("DB_NAME") ?: "katsu_fit"
    val dbUser = System.getenv("DB_USER") ?: "postgres"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "PatyFoxPng"
    
    val jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"
    
    Database.connect(
        url = jdbcUrl,
        driver = "org.postgresql.Driver",
        user = dbUser,
        password = dbPassword
    )

    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json()
        }

        // Configuração do CORS para o celular conseguir acessar
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            anyHost()
        }

        install(Authentication) {
            jwt("auth-jwt") {
                realm = "Acesso Restrito ao Katsu Fit"
                authHeader { call ->
                    if (call.request.headers["Authorization"] != null) {
                        try {
                            val header = call.request.headers["Authorization"]
                            val parts = header?.split(" ")
                            if (parts?.size == 2 && parts[0] == "Bearer") {
                                val token = parts[1]
                                
                                // Verificar se token está na blacklist
                                val isBlacklisted = transaction {
                                    BlacklistedTokens.select { BlacklistedTokens.token eq token }.count() > 0
                                }
                                
                                if (isBlacklisted) {
                                    return@authHeader null
                                }
                                
                                return@authHeader HttpAuthHeader.Single("Bearer", token)
                            }
                        } catch (e: Exception) {
                            call.application.environment.log.warn("Invalid Authorization header", e)
                        }
                    }
                    null
                }
                verifier(
                    JWT.require(Algorithm.HMAC256(jwtSecret))
                        .withAudience(jwtAudience)
                        .withIssuer(jwtIssuer)
                        .build()
                )
                validate { credential ->
                    val email = credential.payload.getClaim("email").asString()
                    val userId = credential.payload.getClaim("id").asString()

                    if (email != null && userId != null) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
            }
        }

        // Criar as tabelas no banco de dados e aplicar workaround
               // Criar as tabelas no banco de dados
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users, 
                ProfessionalProfiles, 
                NutritionPlans, 
                TrainingPlans, 
                Messages, 
                ProgressEntries,
                NutritionistPatientLinks,
                PatientRecords,
                Foods,
                MealPlanTemplates,
                MealPlanMeals,
                AnamnesisTemplates,
                AnamnesisTemplateSections,
                AnamnesisTemplateQuestions,
                AnamnesisQuestionOptions,
                PatientAnamnesis,
                PatientAnamnesisAnswers,
                Appointments,
                ProfessionalAvailabilities,
                Documents,
                SharedDocuments,
                Wallets,
                CreditTransactions,
                CouponsTable,
                UserSettingsTable,
                SupportTickets,
                BlacklistedTokens,
                Professionals,
                ProfessionalStudents,
                PersonalCreditTransactions,
                WorkoutModules,
                WorkoutModuleExercises,
                StudentWorkouts,
                StudentWorkoutExercises,
                DefaultExercises,
                CustomExercises,
                PatientMealPlans,
                PatientMealPlanMeals
            )
        }
        
        // Workaround em transação separada (se falhar, não quebra nada)
        transaction {
            try {
                exec("UPDATE users SET name = 'Sem Nome' WHERE name IS NULL")
            } catch (e: Exception) {
                // Ignore
            }
        }

        // ========================================
        // SEED: Criar templates padrão de anamnese e profissionais
        // ========================================
        AnamnesisSeeds.seedDefaultTemplates()
        ExerciseSeeds.seedDefaultExercises()
        ProfessionalSeeds.seedDefaultProfessional()
        FoodSeeds.seedDefaultFoods()

        routing {
            // ========================================
            // SERVIR ARQUIVOS ESTÁTICOS (uploads)
            // ========================================
            staticFiles("/uploads", File("uploads"))

            userRouting(jwtSecret, jwtIssuer, jwtAudience)
            trainingPlanRouting()
            clientDashboardRouting()
            messageRouting()
            progressRouting()
            nutritionistRouting()
            foodRouting()
            mealPlanRouting()
            patientMealPlanRouting()
            anamnesisRoutes()
            documentRouting()
            walletRouting()
            couponRoutes()
            growthRoutes()
            settingsRoutes()
            supportRoutes()
            authRoutes()
            
            // Personal Trainer Routes
            personalDashboardRoutes()
            personalWalletRoutes()
            workoutRoutes()
            studentRoutes()
            appointmentRoutes()
            exerciseRoutes()

            authenticate("auth-jwt") {
                professionalProfileRouting()
                nutritionPlanRouting()
                get("/api/protected") {
                    call.respondText("Esta rota é protegida! Apenas com um JWT válido.", status = HttpStatusCode.OK)
                }
            }
        }
    }.start(wait = true)
}