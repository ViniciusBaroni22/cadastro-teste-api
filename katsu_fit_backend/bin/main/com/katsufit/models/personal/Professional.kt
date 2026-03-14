package com.katsufit.models.personal

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp

object Professionals : IntIdTable("professionals") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val fullName = varchar("full_name", 255)
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val cref = varchar("cref", 20).uniqueIndex()
    val planType = enumerationByName("plan_type", 20, PlanType::class).default(PlanType.STARTER)
    val creditsAvailable = integer("credits_available").default(0)
    val creditsFrozen = integer("credits_frozen").default(0)
    val proRenewsAt = datetime("pro_renews_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

enum class PlanType { STARTER, PRO }

class Professional(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Professional>(Professionals)
    
    var email by Professionals.email
    var passwordHash by Professionals.passwordHash
    var fullName by Professionals.fullName
    var avatarUrl by Professionals.avatarUrl
    var cref by Professionals.cref
    var planType by Professionals.planType
    var creditsAvailable by Professionals.creditsAvailable
    var creditsFrozen by Professionals.creditsFrozen
    var proRenewsAt by Professionals.proRenewsAt
    var createdAt by Professionals.createdAt
    var updatedAt by Professionals.updatedAt
    
    val students by ProfessionalStudent referrersOn ProfessionalStudents.professionalId
    val creditTransactions by PersonalCreditTransaction referrersOn PersonalCreditTransactions.professionalId
}
