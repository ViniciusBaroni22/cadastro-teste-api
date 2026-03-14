package com.katsufit.models.nutritionist

import com.katsufit.models.shared.Users
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object SharedDocuments : IntIdTable("shared_documents") {
    val documentId = reference("document_id", Documents, onDelete = ReferenceOption.CASCADE)
    val patientId = reference("patient_id", Users, onDelete = ReferenceOption.CASCADE)
    val professionalId = reference("professional_id", Users, onDelete = ReferenceOption.CASCADE)
    val sharedAt = timestamp("shared_at").defaultExpression(CurrentTimestamp)
}

@Serializable
data class ShareDocumentRequest(
    val documentId: Int,
    val patientIds: List<String>
)

@Serializable
data class SharedDocumentResponse(
    val id: Int,
    val documentId: Int,
    val documentName: String,
    val patientId: String,
    val patientName: String,
    val sharedAt: String
)

@Serializable
data class PatientDocumentResponse(
    val id: Int,
    val documentId: Int,
    val name: String,
    val category: String,
    val fileUrl: String,
    val fileType: String,
    val professionalName: String,
    val sharedAt: String
)
