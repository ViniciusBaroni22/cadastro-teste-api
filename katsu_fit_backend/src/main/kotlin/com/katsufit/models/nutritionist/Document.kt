package com.katsufit.models.nutritionist

import com.katsufit.models.shared.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Documents : IntIdTable("documents") {
    val professionalId = uuid("professional_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 255)
    val category = varchar("category", 50)
    val fileUrl = varchar("file_url", 1024)
    val fileType = varchar("file_type", 50)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

@Serializable
data class DocumentRequest(
    val name: String,
    val category: String,
    val fileUrl: String,
    val fileType: String
)

@Serializable
data class DocumentResponse(
    val id: Int,
    val professionalId: String,
    val name: String,
    val category: String,
    val fileUrl: String,
    val fileType: String,
    val createdAt: String
)

@Serializable
data class DocumentUpdateRequest(
    val name: String? = null,
    val category: String? = null
)

@Serializable
data class UploadResponse(
    val id: Int,
    val fileUrl: String,
    val message: String
)
