package com.katsufit.models.shared

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object BlacklistedTokens : IntIdTable("blacklisted_tokens") {
    val token = varchar("token", 500).uniqueIndex()
    val userId = uuid("user_id")
    val expiresAt = datetime("expires_at")
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}
