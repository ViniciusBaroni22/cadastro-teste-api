package com.katsufit.models.nutritionist

import com.katsufit.models.shared.*
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

// ============================================
// TIPOS DE PERGUNTA
// ============================================

enum class QuestionType {
    TEXT_SHORT,      // Texto curto (uma linha)
    TEXT_LONG,       // Texto longo (várias linhas)
    NUMBER,          // Número
    YES_NO,          // Sim/Não
    SINGLE_CHOICE,   // Múltipla escolha (uma resposta)
    MULTIPLE_CHOICE, // Múltipla escolha (várias respostas)
    SCALE,           // Escala (1-10, por exemplo)
    DATE,            // Data
    FILE_UPLOAD      // Upload de arquivo
}

enum class AnamnesisStatus {
    PENDING,      // Aguardando preenchimento
    IN_PROGRESS,  // Em andamento
    COMPLETED     // Finalizada
}

// ============================================
// DTOs PARA MODELOS DE ANAMNESE
// ============================================

@Serializable
data class AnamnesisTemplateDTO(
    val id: Int? = null,
    val professionalId: String? = null, // MUDOU: Int -> String (UUID)
    val name: String,
    val description: String? = null,
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val sections: List<AnamnesisSectionDTO>? = null,
    val totalSections: Int? = null,
    val totalQuestions: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class AnamnesisSectionDTO(
    val id: Int? = null,
    val templateId: Int? = null,
    val name: String,
    val description: String? = null,
    val orderIndex: Int = 0,
    val isActive: Boolean = true,
    val questions: List<AnamnesisQuestionDTO>? = null,
    val totalQuestions: Int? = null
)

@Serializable
data class AnamnesisQuestionDTO(
    val id: Int? = null,
    val sectionId: Int? = null,
    val question: String,
    val questionType: String, // TEXT_SHORT, TEXT_LONG, NUMBER, YES_NO, SINGLE_CHOICE, MULTIPLE_CHOICE, SCALE, DATE, FILE_UPLOAD
    val isRequired: Boolean = false,
    val orderIndex: Int = 0,
    val placeholder: String? = null,
    val helpText: String? = null,
    val minValue: Int? = null,
    val maxValue: Int? = null,
    val isActive: Boolean = true,
    val options: List<QuestionOptionDTO>? = null
)

@Serializable
data class QuestionOptionDTO(
    val id: Int? = null,
    val questionId: Int? = null,
    val optionText: String,
    val orderIndex: Int = 0,
    val isActive: Boolean = true
)

// ============================================
// DTOs PARA CRIAR/ATUALIZAR
// ============================================

@Serializable
data class CreateAnamnesisTemplateRequest(
    val name: String,
    val description: String? = null
)

@Serializable
data class UpdateAnamnesisTemplateRequest(
    val name: String? = null,
    val description: String? = null,
    val isActive: Boolean? = null
)

@Serializable
data class CreateAnamnesisSectionRequest(
    val name: String,
    val description: String? = null,
    val orderIndex: Int = 0
)

@Serializable
data class UpdateAnamnesisSectionRequest(
    val name: String? = null,
    val description: String? = null,
    val orderIndex: Int? = null,
    val isActive: Boolean? = null
)

@Serializable
data class CreateAnamnesisQuestionRequest(
    val question: String,
    val questionType: String,
    val isRequired: Boolean = false,
    val orderIndex: Int = 0,
    val placeholder: String? = null,
    val helpText: String? = null,
    val minValue: Int? = null,
    val maxValue: Int? = null,
    val options: List<String>? = null // Lista de textos das opções (para SINGLE_CHOICE e MULTIPLE_CHOICE)
)

@Serializable
data class UpdateAnamnesisQuestionRequest(
    val question: String? = null,
    val questionType: String? = null,
    val isRequired: Boolean? = null,
    val orderIndex: Int? = null,
    val placeholder: String? = null,
    val helpText: String? = null,
    val minValue: Int? = null,
    val maxValue: Int? = null,
    val isActive: Boolean? = null
)

@Serializable
data class CreateQuestionOptionRequest(
    val optionText: String,
    val orderIndex: Int = 0
)

@Serializable
data class UpdateQuestionOptionRequest(
    val optionText: String? = null,
    val orderIndex: Int? = null,
    val isActive: Boolean? = null
)

// ============================================
// DTOs PARA ANAMNESE DO PACIENTE
// ============================================

@Serializable
data class PatientAnamnesisDTO(
    val id: Int? = null,
    val patientId: String, // MUDOU: Int -> String (UUID)
    val patientName: String? = null,
    val templateId: Int? = null,
    val templateName: String,
    val professionalId: String? = null, // MUDOU: Int -> String (UUID)
    val status: String = "PENDING",
    val notes: String? = null,
    val answers: List<AnamnesisAnswerDTO>? = null,
    val totalQuestions: Int? = null,
    val answeredQuestions: Int? = null,
    val progressPercent: Int? = null,
    val completedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class AnamnesisAnswerDTO(
    val id: Int? = null,
    val anamnesisId: Int? = null,
    val questionId: Int? = null,
    val questionText: String,
    val questionType: String,
    val answerText: String? = null,
    val answerNumber: Double? = null,
    val answerBoolean: Boolean? = null,
    val answerDate: String? = null,
    val answerOptions: List<Int>? = null, // IDs das opções selecionadas
    val answerOptionsText: List<String>? = null, // Texto das opções selecionadas
    val fileUrl: String? = null
)

@Serializable
data class ApplyAnamnesisRequest(
    val patientId: String, // MUDOU: Int -> String (UUID) - ISSO RESOLVE O ERRO
    val templateId: Int,
    val notes: String? = null
)

@Serializable
data class SaveAnamnesisAnswerRequest(
    val questionId: Int,
    val answerText: String? = null,
    val answerNumber: Double? = null,
    val answerBoolean: Boolean? = null,
    val answerDate: String? = null,
    val answerOptions: List<Int>? = null,
    val fileUrl: String? = null
)

@Serializable
data class CompleteAnamnesisRequest(
    val notes: String? = null
)

// ============================================
// DTOs PARA LISTAGEM
// ============================================

@Serializable
data class AnamnesisTemplateListResponse(
    val templates: List<AnamnesisTemplateDTO>,
    val total: Int
)

@Serializable
data class PatientAnamnesisListResponse(
    val anamneses: List<PatientAnamnesisDTO>,
    val total: Int
)
