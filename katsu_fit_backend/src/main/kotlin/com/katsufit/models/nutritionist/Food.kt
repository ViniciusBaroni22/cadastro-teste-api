package com.katsufit.models.nutritionist

import com.katsufit.models.shared.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table

// ============================
// TABELA FOODS
// ============================
object Foods : Table("foods") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val portion = varchar("portion", 50) // ex: "100g", "1 unidade", "200ml"
    val foodSource = varchar("food_source", 50) // TACO, TBCA, USDA, CUSTOM (mudei de 'source' pra evitar conflito)
    
    // Macronutrientes principais
    val calories = double("calories").nullable()
    val protein = double("protein").nullable() // em gramas
    val carbs = double("carbs").nullable() // em gramas
    val fat = double("fat").nullable() // em gramas
    val fiber = double("fiber").nullable() // em gramas
    
    // Macronutrientes detalhados
    val saturatedFat = double("saturated_fat").nullable() // gordura saturada
    val transFat = double("trans_fat").nullable() // gordura trans
    val monounsaturatedFat = double("monounsaturated_fat").nullable() // gordura monoinsaturada
    val polyunsaturatedFat = double("polyunsaturated_fat").nullable() // gordura poli-insaturada
    val sugar = double("sugar").nullable() // açúcar
    val addedSugar = double("added_sugar").nullable() // açúcar adicionado
    
    // Minerais importantes
    val sodium = double("sodium").nullable() // sódio (mg)
    val calcium = double("calcium").nullable() // cálcio (mg)
    val iron = double("iron").nullable() // ferro (mg)
    val magnesium = double("magnesium").nullable() // magnésio (mg)
    val phosphorus = double("phosphorus").nullable() // fósforo (mg)
    val potassium = double("potassium").nullable() // potássio (mg)
    val zinc = double("zinc").nullable() // zinco (mg)
    
    // Vitaminas importantes
    val vitaminA = double("vitamin_a").nullable() // vitamina A (mcg)
    val vitaminC = double("vitamin_c").nullable() // vitamina C (mg)
    val vitaminD = double("vitamin_d").nullable() // vitamina D (mcg)
    val vitaminE = double("vitamin_e").nullable() // vitamina E (mg)
    val vitaminK = double("vitamin_k").nullable() // vitamina K (mcg)
    val vitaminB1 = double("vitamin_b1").nullable() // tiamina (mg)
    val vitaminB2 = double("vitamin_b2").nullable() // riboflavina (mg)
    val vitaminB3 = double("vitamin_b3").nullable() // niacina (mg)
    val vitaminB6 = double("vitamin_b6").nullable() // piridoxina (mg)
    val vitaminB12 = double("vitamin_b12").nullable() // cobalamina (mcg)
    val folate = double("folate").nullable() // ácido fólico (mcg)
    
    // Outros nutrientes
    val cholesterol = double("cholesterol").nullable() // colesterol (mg)
    val water = double("water").nullable() // água (g)
    val caffeine = double("caffeine").nullable() // cafeína (mg)
    val alcohol = double("alcohol").nullable() // álcool (g)
    
    // Controle
    val isFavorite = bool("is_favorite").default(false)
    val nutritionistId = varchar("nutritionist_id", 255).nullable()
    val externalId = varchar("external_id", 100).nullable() // ID da API externa
    val createdAt = varchar("created_at", 50)
    val updatedAt = varchar("updated_at", 50)
    
    override val primaryKey = PrimaryKey(id)
}

// ============================
// DTO - FOOD
// ============================
@Serializable
data class FoodDTO(
    val id: Int? = null,
    val name: String,
    val portion: String,
    val source: String, // Aqui pode ficar 'source' porque é DTO, não conflita
    
    // Macronutrientes principais
    val calories: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val fiber: Double? = null,
    
    // Macronutrientes detalhados
    val saturatedFat: Double? = null,
    val transFat: Double? = null,
    val monounsaturatedFat: Double? = null,
    val polyunsaturatedFat: Double? = null,
    val sugar: Double? = null,
    val addedSugar: Double? = null,
    
    // Minerais
    val sodium: Double? = null,
    val calcium: Double? = null,
    val iron: Double? = null,
    val magnesium: Double? = null,
    val phosphorus: Double? = null,
    val potassium: Double? = null,
    val zinc: Double? = null,
    
    // Vitaminas
    val vitaminA: Double? = null,
    val vitaminC: Double? = null,
    val vitaminD: Double? = null,
    val vitaminE: Double? = null,
    val vitaminK: Double? = null,
    val vitaminB1: Double? = null,
    val vitaminB2: Double? = null,
    val vitaminB3: Double? = null,
    val vitaminB6: Double? = null,
    val vitaminB12: Double? = null,
    val folate: Double? = null,
    
    // Outros
    val cholesterol: Double? = null,
    val water: Double? = null,
    val caffeine: Double? = null,
    val alcohol: Double? = null,
    
    // Controle
    val isFavorite: Boolean = false,
    val nutritionistId: String? = null,
    val externalId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

// ============================
// DTO - BUSCA DE ALIMENTOS
// ============================
@Serializable
data class FoodSearchRequest(
    val query: String,
    val source: String? = null, // null = busca em todas
    val limit: Int = 20
)

@Serializable
data class FoodSearchResponse(
    val foods: List<FoodDTO>,
    val total: Int
)

@Serializable
data class SaveFoodResponse(
    val id: Int,
    val message: String
)
