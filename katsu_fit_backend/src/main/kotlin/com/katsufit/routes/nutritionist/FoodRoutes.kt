package com.katsufit.routes.nutritionist

import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

fun Route.foodRouting() {
    
    authenticate("auth-jwt") {
        
        // ============================
        // BUSCAR ALIMENTOS (Apenas Banco Local - TACO)
        // ============================
        post("/api/foods/search") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asString()
            
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                return@post
            }
            
            val request = call.receive<FoodSearchRequest>()
            
            // Busca APENAS no banco local (tabela TACO)
            val foods = transaction {
                Foods.select { 
                    Foods.name.lowerCase() like "%${request.query.lowercase()}%"
                }
                .limit(request.limit)
                .map { row ->
                    FoodDTO(
                        id = row[Foods.id],
                        name = row[Foods.name],
                        portion = row[Foods.portion],
                        source = row[Foods.foodSource],
                        calories = row[Foods.calories],
                        protein = row[Foods.protein],
                        carbs = row[Foods.carbs],
                        fat = row[Foods.fat],
                        fiber = row[Foods.fiber],
                        saturatedFat = row[Foods.saturatedFat],
                        transFat = row[Foods.transFat],
                        monounsaturatedFat = row[Foods.monounsaturatedFat],
                        polyunsaturatedFat = row[Foods.polyunsaturatedFat],
                        sugar = row[Foods.sugar],
                        addedSugar = row[Foods.addedSugar],
                        sodium = row[Foods.sodium],
                        calcium = row[Foods.calcium],
                        iron = row[Foods.iron],
                        magnesium = row[Foods.magnesium],
                        phosphorus = row[Foods.phosphorus],
                        potassium = row[Foods.potassium],
                        zinc = row[Foods.zinc],
                        vitaminA = row[Foods.vitaminA],
                        vitaminC = row[Foods.vitaminC],
                        vitaminD = row[Foods.vitaminD],
                        vitaminE = row[Foods.vitaminE],
                        vitaminK = row[Foods.vitaminK],
                        vitaminB1 = row[Foods.vitaminB1],
                        vitaminB2 = row[Foods.vitaminB2],
                        vitaminB3 = row[Foods.vitaminB3],
                        vitaminB6 = row[Foods.vitaminB6],
                        vitaminB12 = row[Foods.vitaminB12],
                        folate = row[Foods.folate],
                        cholesterol = row[Foods.cholesterol],
                        water = row[Foods.water],
                        caffeine = row[Foods.caffeine],
                        alcohol = row[Foods.alcohol],
                        isFavorite = row[Foods.isFavorite],
                        nutritionistId = row[Foods.nutritionistId],
                        externalId = row[Foods.externalId],
                        createdAt = row[Foods.createdAt],
                        updatedAt = row[Foods.updatedAt]
                    )
                }
            }
            
            call.respond(
                FoodSearchResponse(
                    foods = foods,
                    total = foods.size
                )
            )
        }
        
        // ============================
        // SALVAR FAVORITO
        // ============================
        post("/api/foods/favorite") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asString()
            
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                return@post
            }
            
            val food = call.receive<FoodDTO>()
            
            val foodId = transaction {
                Foods.insert {
                    it[name] = food.name
                    it[portion] = food.portion
                    it[foodSource] = food.source
                    it[calories] = food.calories
                    it[protein] = food.protein
                    it[carbs] = food.carbs
                    it[fat] = food.fat
                    it[fiber] = food.fiber
                    it[saturatedFat] = food.saturatedFat
                    it[transFat] = food.transFat
                    it[monounsaturatedFat] = food.monounsaturatedFat
                    it[polyunsaturatedFat] = food.polyunsaturatedFat
                    it[sugar] = food.sugar
                    it[addedSugar] = food.addedSugar
                    it[sodium] = food.sodium
                    it[calcium] = food.calcium
                    it[iron] = food.iron
                    it[magnesium] = food.magnesium
                    it[phosphorus] = food.phosphorus
                    it[potassium] = food.potassium
                    it[zinc] = food.zinc
                    it[vitaminA] = food.vitaminA
                    it[vitaminC] = food.vitaminC
                    it[vitaminD] = food.vitaminD
                    it[vitaminE] = food.vitaminE
                    it[vitaminK] = food.vitaminK
                    it[vitaminB1] = food.vitaminB1
                    it[vitaminB2] = food.vitaminB2
                    it[vitaminB3] = food.vitaminB3
                    it[vitaminB6] = food.vitaminB6
                    it[vitaminB12] = food.vitaminB12
                    it[folate] = food.folate
                    it[cholesterol] = food.cholesterol
                    it[water] = food.water
                    it[caffeine] = food.caffeine
                    it[alcohol] = food.alcohol
                    it[isFavorite] = true
                    it[nutritionistId] = userId
                    it[externalId] = food.externalId
                    it[createdAt] = LocalDateTime.now().toString()
                    it[updatedAt] = LocalDateTime.now().toString()
                }[Foods.id]
            }
            
            call.respond(HttpStatusCode.Created, SaveFoodResponse(id = foodId, message = "Alimento salvo nos favoritos"))
        }
        
        // ============================
        // LISTAR FAVORITOS
        // ============================
        get("/api/foods/favorites") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asString()
            
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                return@get
            }
            
            val favorites = transaction {
                Foods.select { (Foods.nutritionistId eq userId) and (Foods.isFavorite eq true) }
                    .map { row ->
                        FoodDTO(
                            id = row[Foods.id],
                            name = row[Foods.name],
                            portion = row[Foods.portion],
                            source = row[Foods.foodSource],
                            calories = row[Foods.calories],
                            protein = row[Foods.protein],
                            carbs = row[Foods.carbs],
                            fat = row[Foods.fat],
                            fiber = row[Foods.fiber],
                            saturatedFat = row[Foods.saturatedFat],
                            transFat = row[Foods.transFat],
                            monounsaturatedFat = row[Foods.monounsaturatedFat],
                            polyunsaturatedFat = row[Foods.polyunsaturatedFat],
                            sugar = row[Foods.sugar],
                            addedSugar = row[Foods.addedSugar],
                            sodium = row[Foods.sodium],
                            calcium = row[Foods.calcium],
                            iron = row[Foods.iron],
                            magnesium = row[Foods.magnesium],
                            phosphorus = row[Foods.phosphorus],
                            potassium = row[Foods.potassium],
                            zinc = row[Foods.zinc],
                            vitaminA = row[Foods.vitaminA],
                            vitaminC = row[Foods.vitaminC],
                            vitaminD = row[Foods.vitaminD],
                            vitaminE = row[Foods.vitaminE],
                            vitaminK = row[Foods.vitaminK],
                            vitaminB1 = row[Foods.vitaminB1],
                            vitaminB2 = row[Foods.vitaminB2],
                            vitaminB3 = row[Foods.vitaminB3],
                            vitaminB6 = row[Foods.vitaminB6],
                            vitaminB12 = row[Foods.vitaminB12],
                            folate = row[Foods.folate],
                            cholesterol = row[Foods.cholesterol],
                            water = row[Foods.water],
                            caffeine = row[Foods.caffeine],
                            alcohol = row[Foods.alcohol],
                            isFavorite = row[Foods.isFavorite],
                            nutritionistId = row[Foods.nutritionistId],
                            externalId = row[Foods.externalId],
                            createdAt = row[Foods.createdAt],
                            updatedAt = row[Foods.updatedAt]
                        )
                    }
            }
            
            call.respond(favorites)
        }
        
        // ============================
        // REMOVER FAVORITO
        // ============================
        delete("/api/foods/favorite/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asString()
            
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                return@delete
            }
            
            val foodId = call.parameters["id"]?.toIntOrNull()
            
            if (foodId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                return@delete
            }
            
            val deleted = transaction {
                Foods.deleteWhere { 
                    (Foods.id eq foodId) and (Foods.nutritionistId eq userId)
                }
            }
            
            if (deleted > 0) {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Favorito removido"))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Alimento não encontrado"))
            }
        }
    }
}
