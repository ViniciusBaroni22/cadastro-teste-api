package com.katsufit.models.nutritionist

import com.katsufit.models.shared.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// ============================================
// MODELOS DE ANAMNESE (Templates)
// ============================================

object AnamnesisTemplates : IntIdTable("anamnesis_templates") {
    val professionalId = uuid("professional_id") // UUID do usuário
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val isDefault = bool("is_default").default(false)
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

object AnamnesisTemplateSections : IntIdTable("anamnesis_template_sections") {
    val templateId = integer("template_id").references(AnamnesisTemplates.id)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val orderIndex = integer("order_index").default(0)
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

object AnamnesisTemplateQuestions : IntIdTable("anamnesis_template_questions") {
    val sectionId = integer("section_id").references(AnamnesisTemplateSections.id)
    val question = text("question")
    val questionType = varchar("question_type", 50)
    val isRequired = bool("is_required").default(false)
    val orderIndex = integer("order_index").default(0)
    val placeholder = varchar("placeholder", 255).nullable()
    val helpText = text("help_text").nullable()
    val minValue = integer("min_value").nullable()
    val maxValue = integer("max_value").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

object AnamnesisQuestionOptions : IntIdTable("anamnesis_question_options") {
    val questionId = integer("question_id").references(AnamnesisTemplateQuestions.id)
    val optionText = varchar("option_text", 255)
    val orderIndex = integer("order_index").default(0)
    val isActive = bool("is_active").default(true)
}

// ============================================
// ANAMNESE DO PACIENTE (Preenchida)
// ============================================

object PatientAnamnesis : IntIdTable("patient_anamnesis") {
    val patientId = uuid("patient_id") // UUID do paciente
    val templateId = integer("template_id").references(AnamnesisTemplates.id).nullable()
    val professionalId = uuid("professional_id") // UUID do profissional
    val templateName = varchar("template_name", 255)
    val status = varchar("status", 50).default("PENDING")
    val notes = text("notes").nullable()
    val completedAt = timestamp("completed_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

object PatientAnamnesisAnswers : IntIdTable("patient_anamnesis_answers") {
    val anamnesisId = integer("anamnesis_id").references(PatientAnamnesis.id)
    val questionId = integer("question_id")
    val questionText = text("question_text")
    val questionType = varchar("question_type", 50)
    val answerText = text("answer_text").nullable()
    val answerNumber = double("answer_number").nullable()
    val answerBoolean = bool("answer_boolean").nullable()
    val answerDate = timestamp("answer_date").nullable()
    val answerOptions = text("answer_options").nullable()
    val fileUrl = varchar("file_url", 500).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}
