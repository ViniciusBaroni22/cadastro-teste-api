package com.katsufit.models.personal.exercise

import com.katsufit.models.personal.Professionals
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

// ==========================================
// TABELA: Exercícios Padrão (Pré-carregados)
// ==========================================
object DefaultExercises : IntIdTable("default_exercises") {
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val muscleGroup = varchar("muscle_group", 50)
    val category = varchar("category", 50).nullable()
    val difficulty = varchar("difficulty", 20).nullable() // Iniciante, Intermediário, Avançado
    val videoUrl = varchar("video_url", 500).nullable()
    val thumbnailUrl = varchar("thumbnail_url", 500).nullable()
    val instructions = text("instructions").nullable()
    val equipment = varchar("equipment", 200).nullable()
}

// ==========================================
// TABELA: Exercícios Custom (Criados pelo Personal)
// ==========================================
object CustomExercises : IntIdTable("custom_exercises") {
    val professionalId = reference("professional_id", Professionals, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val muscleGroup = varchar("muscle_group", 50)
    val category = varchar("category", 50).nullable()
    val difficulty = varchar("difficulty", 20).nullable()
    val videoUrl = varchar("video_url", 500).nullable()
    val thumbnailUrl = varchar("thumbnail_url", 500).nullable()
    val instructions = text("instructions").nullable()
    val equipment = varchar("equipment", 200).nullable()
    val mediaType = varchar("media_type", 20).nullable() // VIDEO_UPLOAD, VIDEO_LINK
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

// ==========================================
// DAOs
// ==========================================

class DefaultExercise(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DefaultExercise>(DefaultExercises)
    var name by DefaultExercises.name
    var description by DefaultExercises.description
    var muscleGroup by DefaultExercises.muscleGroup
    var category by DefaultExercises.category
    var difficulty by DefaultExercises.difficulty
    var videoUrl by DefaultExercises.videoUrl
    var thumbnailUrl by DefaultExercises.thumbnailUrl
    var instructions by DefaultExercises.instructions
    var equipment by DefaultExercises.equipment
}

class CustomExercise(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CustomExercise>(CustomExercises)
    var professionalId by CustomExercises.professionalId
    var name by CustomExercises.name
    var description by CustomExercises.description
    var muscleGroup by CustomExercises.muscleGroup
    var category by CustomExercises.category
    var difficulty by CustomExercises.difficulty
    var videoUrl by CustomExercises.videoUrl
    var thumbnailUrl by CustomExercises.thumbnailUrl
    var instructions by CustomExercises.instructions
    var equipment by CustomExercises.equipment
    var mediaType by CustomExercises.mediaType
    var createdAt by CustomExercises.createdAt
    var updatedAt by CustomExercises.updatedAt
}
