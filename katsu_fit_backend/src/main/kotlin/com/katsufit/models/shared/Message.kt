package com.katsufit.models.shared

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Messages : UUIDTable("messages") {
    val senderId = uuid("sender_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val receiverId = uuid("receiver_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val content = text("content")
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)
    val read = bool("read").default(false)
    val attachmentUrl = varchar("attachment_url", 1024).nullable()
}

@Serializable
data class MessageRequest(
    val receiverId: String,
    val content: String,
    val attachmentUrl: String? = null
)

@Serializable
data class MessageResponse(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: String,
    val read: Boolean,
    val attachmentUrl: String? = null
)
