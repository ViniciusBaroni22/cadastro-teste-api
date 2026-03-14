package com.katsufit.models.nutritionist

import com.katsufit.models.shared.*
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// Tabela que liga o Nutricionista ao Paciente no Banco de Dados
object NutritionistPatientLinks : UUIDTable("nutritionist_patient_links") {
    // Coluna do ID do Nutricionista (Quem convidou)
    val nutritionist = reference("nutritionist_id", Users, onDelete = ReferenceOption.CASCADE)
    
    // Coluna do ID do Paciente (Quem foi convidado)
    val patient = reference("patient_id", Users, onDelete = ReferenceOption.CASCADE)
    
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    // Regra: Não pode ter o mesmo vínculo duplicado
    init {
        uniqueIndex(nutritionist, patient)
    }
}
