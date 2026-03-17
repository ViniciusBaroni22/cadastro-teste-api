package com.katsufit.seeds

import com.katsufit.models.nutritionist.Foods
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.InputStreamReader
import java.io.BufferedReader
import java.time.LocalDateTime

object FoodSeeds {

    fun seedDefaultFoods() {
        transaction {
            // Só faz seed se a tabela estiver vazia
            if (Foods.selectAll().count() > 0) return@transaction

            try {
                // Lê o arquivo CSV dos resources
                val inputStream = this::class.java.classLoader.getResourceAsStream("data/taco_alimentos_597.csv")
                if (inputStream == null) {
                    println("ERROR: taco_alimentos_597.csv não encontrado nos resources.")
                    return@transaction
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                var isFirstLine = true

                reader.forEachLine { line ->
                    if (isFirstLine) {
                        isFirstLine = false
                        return@forEachLine // Pula o cabeçalho
                    }

                    // Regex para lidar com campos entre aspas que contêm vírgulas
                    val regex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()
                    val tokens = line.split(regex).map { it.replace("\"", "").trim() }

                    if (tokens.size >= 41) {
                        insertFood(tokens)
                    }
                }
                
                println("SUCCESS: Tabela TACO semeada com sucesso.")
            } catch (e: Exception) {
                println("ERROR seeding Foods: ${e.message}")
            }
        }
    }

    private fun insertFood(tokens: List<String>) {
        // Função auxiliar para parsear doubles com segurança (se vazio ou nulo, retorna null)
        fun parseDoubleOrNull(value: String): Double? {
            return value.toDoubleOrNull()
        }

        val nowStr = LocalDateTime.now().toString()

        Foods.insert {
            it[name] = tokens[0]
            it[portion] = tokens[1]
            it[foodSource] = tokens[2]
            it[calories] = parseDoubleOrNull(tokens[3])
            it[protein] = parseDoubleOrNull(tokens[4])
            it[carbs] = parseDoubleOrNull(tokens[5])
            it[fat] = parseDoubleOrNull(tokens[6])
            it[fiber] = parseDoubleOrNull(tokens[7])
            it[saturatedFat] = parseDoubleOrNull(tokens[8])
            it[transFat] = parseDoubleOrNull(tokens[9])
            it[monounsaturatedFat] = parseDoubleOrNull(tokens[10])
            it[polyunsaturatedFat] = parseDoubleOrNull(tokens[11])
            it[sugar] = parseDoubleOrNull(tokens[12])
            it[addedSugar] = parseDoubleOrNull(tokens[13])
            it[sodium] = parseDoubleOrNull(tokens[14])
            it[calcium] = parseDoubleOrNull(tokens[15])
            it[iron] = parseDoubleOrNull(tokens[16])
            it[magnesium] = parseDoubleOrNull(tokens[17])
            it[phosphorus] = parseDoubleOrNull(tokens[18])
            it[potassium] = parseDoubleOrNull(tokens[19])
            it[zinc] = parseDoubleOrNull(tokens[20])
            it[vitaminA] = parseDoubleOrNull(tokens[21])
            it[vitaminC] = parseDoubleOrNull(tokens[22])
            it[vitaminD] = parseDoubleOrNull(tokens[23])
            it[vitaminE] = parseDoubleOrNull(tokens[24])
            it[vitaminK] = parseDoubleOrNull(tokens[25])
            it[vitaminB1] = parseDoubleOrNull(tokens[26])
            it[vitaminB2] = parseDoubleOrNull(tokens[27])
            it[vitaminB3] = parseDoubleOrNull(tokens[28])
            it[vitaminB6] = parseDoubleOrNull(tokens[29])
            it[vitaminB12] = parseDoubleOrNull(tokens[30])
            it[folate] = parseDoubleOrNull(tokens[31])
            it[cholesterol] = parseDoubleOrNull(tokens[32])
            it[water] = parseDoubleOrNull(tokens[33])
            it[caffeine] = parseDoubleOrNull(tokens[34])
            it[alcohol] = parseDoubleOrNull(tokens[35])
            it[isFavorite] = tokens[36].toBoolean()
            it[nutritionistId] = tokens[37].takeIf { id -> id.isNotEmpty() }
            it[externalId] = tokens[38].takeIf { id -> id.isNotEmpty() }
            it[createdAt] = nowStr
            it[updatedAt] = nowStr
        }
    }
}
