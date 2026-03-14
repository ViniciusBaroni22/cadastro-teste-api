package com.katsufit.seeds

import com.katsufit.models.personal.PlanType
import com.katsufit.models.personal.Professional
import com.katsufit.models.personal.Professionals
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

object ProfessionalSeeds {
    fun seedDefaultProfessional() {
        transaction {
            // Check if professional with ID 1 already exists
            val existing = Professional.findById(1)
            
            if (existing == null) {
                // Check if email already exists to avoid unique constraint violation
                val emailExists = Professionals.select { Professionals.email eq "vini@gmail.com" }.count() > 0
                
                if (!emailExists) {
                    println("🌱 Criando Professional de teste (ID 1)...")
                    val passwordHash = BCrypt.hashpw("123456", BCrypt.gensalt())
                    
                    Professional.new(1) {
                        this.email = "vini@gmail.com"
                        this.passwordHash = passwordHash
                        this.fullName = "Vini (Teste)"
                        this.cref = "123456-G/SP"
                        this.planType = PlanType.STARTER
                        this.creditsAvailable = 50
                    }
                    println("✅ Professional de teste criado com sucesso!")
                } else {
                    println("⚠️ O email vini@gmail.com já está em uso por outro ID na tabela Professionals.")
                }
            } else {
                println("✨ Professional de teste (ID 1) já existe. Pulando seed...")
            }
        }
    }
}
