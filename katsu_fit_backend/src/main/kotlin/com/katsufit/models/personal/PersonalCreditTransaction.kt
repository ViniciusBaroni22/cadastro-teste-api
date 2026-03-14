package com.katsufit.models.personal

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp

object PersonalCreditTransactions : IntIdTable("personal_credit_transactions") {
    val professionalId = reference("professional_id", Professionals)
    val type = enumerationByName("type", 20, TransactionType::class)
    val amount = integer("amount")
    val studentId = integer("student_id").nullable()
    val description = varchar("description", 255)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

enum class TransactionType { PURCHASE, DEBIT, FREEZE, UNFREEZE, EXPIRE }

class PersonalCreditTransaction(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PersonalCreditTransaction>(PersonalCreditTransactions)
    
    var professional by Professional referencedOn PersonalCreditTransactions.professionalId
    var type by PersonalCreditTransactions.type
    var amount by PersonalCreditTransactions.amount
    var studentId by PersonalCreditTransactions.studentId
    var description by PersonalCreditTransactions.description
    var createdAt by PersonalCreditTransactions.createdAt
}
