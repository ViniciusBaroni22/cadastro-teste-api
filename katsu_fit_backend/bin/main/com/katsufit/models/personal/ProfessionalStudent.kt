package com.katsufit.models.personal

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object ProfessionalStudents : IntIdTable("professional_students") {
    val professionalId = reference("professional_id", Professionals)
    val studentId = integer("student_id") 
    val studentName = varchar("student_name", 255)
    val studentAvatar = varchar("student_avatar", 500).nullable()
    val isActive = bool("is_active").default(true)
    val creditActivatedAt = datetime("credit_activated_at").nullable()
    val creditExpiresAt = datetime("credit_expires_at").nullable()
    val objective = varchar("objective", 100).nullable()
    val age = integer("age").nullable()
    val email = varchar("email", 255).nullable()
    val phone = varchar("phone", 50).nullable()
    val birthDate = varchar("birth_date", 20).nullable()
    val gender = varchar("gender", 20).nullable()
    val weight = double("weight").nullable()
    val height = double("height").nullable()
    val bodyFatPercentage = double("body_fat_percentage").nullable()
    val healthConditions = text("health_conditions").nullable()
    val injuries = text("injuries").nullable()
    val experience = varchar("experience", 50).nullable()
    val trainingFrequency = integer("training_frequency").nullable()
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

class ProfessionalStudent(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ProfessionalStudent>(ProfessionalStudents)
    
    var professional by Professional referencedOn ProfessionalStudents.professionalId
    var studentId by ProfessionalStudents.studentId
    var studentName by ProfessionalStudents.studentName
    var studentAvatar by ProfessionalStudents.studentAvatar
    var isActive by ProfessionalStudents.isActive
    var creditActivatedAt by ProfessionalStudents.creditActivatedAt
    var creditExpiresAt by ProfessionalStudents.creditExpiresAt
    var objective by ProfessionalStudents.objective
    var age by ProfessionalStudents.age
    var email by ProfessionalStudents.email
    var phone by ProfessionalStudents.phone
    var birthDate by ProfessionalStudents.birthDate
    var gender by ProfessionalStudents.gender
    var weight by ProfessionalStudents.weight
    var height by ProfessionalStudents.height
    var bodyFatPercentage by ProfessionalStudents.bodyFatPercentage
    var healthConditions by ProfessionalStudents.healthConditions
    var injuries by ProfessionalStudents.injuries
    var experience by ProfessionalStudents.experience
    var trainingFrequency by ProfessionalStudents.trainingFrequency
    var notes by ProfessionalStudents.notes
    var createdAt by ProfessionalStudents.createdAt
    
    fun hasValidCredit(): Boolean {
        val expiresAt = creditExpiresAt ?: return false
        val now = Clock.System.now().toLocalDateTime(TimeZone.of("America/Sao_Paulo"))
        return expiresAt > now
    }
    
    fun daysUntilExpiry(): Int {
        val expiresAt = creditExpiresAt ?: return 0
        val now = Clock.System.now().toLocalDateTime(TimeZone.of("America/Sao_Paulo"))
        if (expiresAt <= now) return 0
        val nowDate = kotlinx.datetime.LocalDate(now.year, now.monthNumber, now.dayOfMonth)
        val expDate = kotlinx.datetime.LocalDate(expiresAt.year, expiresAt.monthNumber, expiresAt.dayOfMonth)
        return (expDate.toEpochDays() - nowDate.toEpochDays())
    }
}
