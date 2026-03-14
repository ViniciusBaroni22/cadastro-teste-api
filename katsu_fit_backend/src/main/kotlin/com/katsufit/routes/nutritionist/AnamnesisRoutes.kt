package com.katsufit.routes.nutritionist

import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.anamnesisRoutes() {

    authenticate("auth-jwt") {

        route("/api/anamnesis") {

            // ============================================
            // MODELOS DE ANAMNESE
            // ============================================

            // Listar todos os modelos do profissional
            get("/templates") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@get
                }

                val professionalId = UUID.fromString(professionalIdStr)

                val templates = transaction {
                    AnamnesisTemplates
                        .select { (AnamnesisTemplates.professionalId eq professionalId) or (AnamnesisTemplates.isDefault eq true) }
                        .andWhere { AnamnesisTemplates.isActive eq true }
                        .orderBy(AnamnesisTemplates.createdAt, SortOrder.DESC)
                        .map { row ->
                            val templateId = row[AnamnesisTemplates.id].value

                            val sectionsCount = AnamnesisTemplateSections
                                .select { (AnamnesisTemplateSections.templateId eq templateId) and (AnamnesisTemplateSections.isActive eq true) }
                                .count()

                            val questionsCount = AnamnesisTemplateSections
                                .innerJoin(AnamnesisTemplateQuestions, { AnamnesisTemplateSections.id }, { AnamnesisTemplateQuestions.sectionId })
                                .select { (AnamnesisTemplateSections.templateId eq templateId) and (AnamnesisTemplateQuestions.isActive eq true) }
                                .count()

                            AnamnesisTemplateDTO(
                                id = templateId,
                                professionalId = null,
                                name = row[AnamnesisTemplates.name],
                                description = row[AnamnesisTemplates.description],
                                isDefault = row[AnamnesisTemplates.isDefault],
                                isActive = row[AnamnesisTemplates.isActive],
                                totalSections = sectionsCount.toInt(),
                                totalQuestions = questionsCount.toInt(),
                                createdAt = row[AnamnesisTemplates.createdAt].toString(),
                                updatedAt = row[AnamnesisTemplates.updatedAt].toString()
                            )
                        }
                }

                call.respond(AnamnesisTemplateListResponse(templates = templates, total = templates.size))
            }

            // Buscar modelo por ID (com seções e perguntas)
            get("/templates/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()
                val templateId = call.parameters["id"]?.toIntOrNull()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@get
                }

                if (templateId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@get
                }

                val professionalId = UUID.fromString(professionalIdStr)

                val template = transaction {
                    val templateRow = AnamnesisTemplates
                        .select { AnamnesisTemplates.id eq templateId }
                        .andWhere { (AnamnesisTemplates.professionalId eq professionalId) or (AnamnesisTemplates.isDefault eq true) }
                        .singleOrNull() ?: return@transaction null

                    val sections = AnamnesisTemplateSections
                        .select { (AnamnesisTemplateSections.templateId eq templateId) and (AnamnesisTemplateSections.isActive eq true) }
                        .orderBy(AnamnesisTemplateSections.orderIndex, SortOrder.ASC)
                        .map { sectionRow ->
                            val sectionId = sectionRow[AnamnesisTemplateSections.id].value

                            val questions = AnamnesisTemplateQuestions
                                .select { (AnamnesisTemplateQuestions.sectionId eq sectionId) and (AnamnesisTemplateQuestions.isActive eq true) }
                                .orderBy(AnamnesisTemplateQuestions.orderIndex, SortOrder.ASC)
                                .map { questionRow ->
                                    val questionId = questionRow[AnamnesisTemplateQuestions.id].value

                                    val options = AnamnesisQuestionOptions
                                        .select { (AnamnesisQuestionOptions.questionId eq questionId) and (AnamnesisQuestionOptions.isActive eq true) }
                                        .orderBy(AnamnesisQuestionOptions.orderIndex, SortOrder.ASC)
                                        .map { optionRow ->
                                            QuestionOptionDTO(
                                                id = optionRow[AnamnesisQuestionOptions.id].value,
                                                questionId = questionId,
                                                optionText = optionRow[AnamnesisQuestionOptions.optionText],
                                                orderIndex = optionRow[AnamnesisQuestionOptions.orderIndex],
                                                isActive = optionRow[AnamnesisQuestionOptions.isActive]
                                            )
                                        }

                                    AnamnesisQuestionDTO(
                                        id = questionId,
                                        sectionId = sectionId,
                                        question = questionRow[AnamnesisTemplateQuestions.question],
                                        questionType = questionRow[AnamnesisTemplateQuestions.questionType],
                                        isRequired = questionRow[AnamnesisTemplateQuestions.isRequired],
                                        orderIndex = questionRow[AnamnesisTemplateQuestions.orderIndex],
                                        placeholder = questionRow[AnamnesisTemplateQuestions.placeholder],
                                        helpText = questionRow[AnamnesisTemplateQuestions.helpText],
                                        minValue = questionRow[AnamnesisTemplateQuestions.minValue],
                                        maxValue = questionRow[AnamnesisTemplateQuestions.maxValue],
                                        isActive = questionRow[AnamnesisTemplateQuestions.isActive],
                                        options = options
                                    )
                                }

                            AnamnesisSectionDTO(
                                id = sectionId,
                                templateId = templateId,
                                name = sectionRow[AnamnesisTemplateSections.name],
                                description = sectionRow[AnamnesisTemplateSections.description],
                                orderIndex = sectionRow[AnamnesisTemplateSections.orderIndex],
                                isActive = sectionRow[AnamnesisTemplateSections.isActive],
                                questions = questions,
                                totalQuestions = questions.size
                            )
                        }

                    AnamnesisTemplateDTO(
                        id = templateId,
                        professionalId = null,
                        name = templateRow[AnamnesisTemplates.name],
                        description = templateRow[AnamnesisTemplates.description],
                        isDefault = templateRow[AnamnesisTemplates.isDefault],
                        isActive = templateRow[AnamnesisTemplates.isActive],
                        sections = sections,
                        totalSections = sections.size,
                        totalQuestions = sections.sumOf { it.questions?.size ?: 0 },
                        createdAt = templateRow[AnamnesisTemplates.createdAt].toString(),
                        updatedAt = templateRow[AnamnesisTemplates.updatedAt].toString()
                    )
                }

                if (template == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Modelo não encontrado"))
                    return@get
                }

                call.respond(template)
            }

            // Criar novo modelo
            post("/templates") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@post
                }

                val professionalId = UUID.fromString(professionalIdStr)
                val request = call.receive<CreateAnamnesisTemplateRequest>()

                val templateId = transaction {
                    AnamnesisTemplates.insertAndGetId {
                        it[AnamnesisTemplates.professionalId] = professionalId
                        it[name] = request.name
                        it[description] = request.description
                        it[isDefault] = false
                        it[isActive] = true
                    }.value
                }

                // CORRIGIDO: id.toString()
                call.respond(HttpStatusCode.Created, mapOf("id" to templateId.toString(), "message" to "Modelo criado com sucesso"))
            }

            // Atualizar modelo
            put("/templates/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()
                val templateId = call.parameters["id"]?.toIntOrNull()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@put
                }

                if (templateId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@put
                }

                val professionalId = UUID.fromString(professionalIdStr)
                val request = call.receive<UpdateAnamnesisTemplateRequest>()

                val updated = transaction {
                    AnamnesisTemplates.update({ (AnamnesisTemplates.id eq templateId) and (AnamnesisTemplates.professionalId eq professionalId) }) {
                        if (request.name != null) it[name] = request.name
                        if (request.description != null) it[description] = request.description
                        if (request.isActive != null) it[isActive] = request.isActive
                        it[updatedAt] = Clock.System.now()
                    }
                }

                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Modelo não encontrado"))
                    return@put
                }

                call.respond(mapOf("message" to "Modelo atualizado com sucesso"))
            }

            // Deletar modelo (soft delete)
            delete("/templates/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()
                val templateId = call.parameters["id"]?.toIntOrNull()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@delete
                }

                if (templateId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@delete
                }

                val professionalId = UUID.fromString(professionalIdStr)

                val deleted = transaction {
                    AnamnesisTemplates.update({ (AnamnesisTemplates.id eq templateId) and (AnamnesisTemplates.professionalId eq professionalId) and (AnamnesisTemplates.isDefault eq false) }) {
                        it[isActive] = false
                        it[updatedAt] = Clock.System.now()
                    }
                }

                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Modelo não encontrado ou é padrão do sistema"))
                    return@delete
                }

                call.respond(mapOf("message" to "Modelo excluído com sucesso"))
            }































            // Duplicar modelo
            post("/templates/{id}/duplicate") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()
                val templateId = call.parameters["id"]?.toIntOrNull()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@post
                }

                if (templateId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@post
                }

                val professionalId = UUID.fromString(professionalIdStr)

                val newTemplateId = transaction {
                    val original = AnamnesisTemplates
                        .select { AnamnesisTemplates.id eq templateId }
                        .singleOrNull() ?: return@transaction null

                    val newId = AnamnesisTemplates.insertAndGetId {
                        it[AnamnesisTemplates.professionalId] = professionalId
                        it[name] = "${original[AnamnesisTemplates.name]} (Cópia)"
                        it[description] = original[AnamnesisTemplates.description]
                        it[isDefault] = false
                        it[isActive] = true
                    }.value

                    val sections = AnamnesisTemplateSections
                        .select { AnamnesisTemplateSections.templateId eq templateId }
                        .toList()

                    sections.forEach { sectionRow ->
                        val oldSectionId = sectionRow[AnamnesisTemplateSections.id].value

                        val newSectionId = AnamnesisTemplateSections.insertAndGetId {
                            it[AnamnesisTemplateSections.templateId] = newId
                            it[name] = sectionRow[AnamnesisTemplateSections.name]
                            it[description] = sectionRow[AnamnesisTemplateSections.description]
                            it[orderIndex] = sectionRow[AnamnesisTemplateSections.orderIndex]
                            it[isActive] = true
                        }.value

                        val questions = AnamnesisTemplateQuestions
                            .select { AnamnesisTemplateQuestions.sectionId eq oldSectionId }
                            .toList()

                        questions.forEach { questionRow ->
                            val oldQuestionId = questionRow[AnamnesisTemplateQuestions.id].value

                            val newQuestionId = AnamnesisTemplateQuestions.insertAndGetId {
                                it[sectionId] = newSectionId
                                it[question] = questionRow[AnamnesisTemplateQuestions.question]
                                it[questionType] = questionRow[AnamnesisTemplateQuestions.questionType]
                                it[isRequired] = questionRow[AnamnesisTemplateQuestions.isRequired]
                                it[orderIndex] = questionRow[AnamnesisTemplateQuestions.orderIndex]
                                it[placeholder] = questionRow[AnamnesisTemplateQuestions.placeholder]
                                it[helpText] = questionRow[AnamnesisTemplateQuestions.helpText]
                                it[minValue] = questionRow[AnamnesisTemplateQuestions.minValue]
                                it[maxValue] = questionRow[AnamnesisTemplateQuestions.maxValue]
                                it[isActive] = true
                            }.value

                            val options = AnamnesisQuestionOptions
                                .select { AnamnesisQuestionOptions.questionId eq oldQuestionId }
                                .toList()

                            options.forEach { optionRow ->
                                AnamnesisQuestionOptions.insert {
                                    it[questionId] = newQuestionId
                                    it[optionText] = optionRow[AnamnesisQuestionOptions.optionText]
                                    it[orderIndex] = optionRow[AnamnesisQuestionOptions.orderIndex]
                                    it[isActive] = true
                                }
                            }
                        }
                    }

                    newId
                }

                if (newTemplateId == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Modelo não encontrado"))
                    return@post
                }

                // CORRIGIDO: id.toString()
                call.respond(HttpStatusCode.Created, mapOf("id" to newTemplateId.toString(), "message" to "Modelo duplicado com sucesso"))
            }





















            // ============================================
            // SEÇÕES
            // ============================================

         // ============================================
            // SEÇÕES
            // ============================================

            post("/templates/{templateId}/sections") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()
                val templateId = call.parameters["templateId"]?.toIntOrNull()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@post
                }

                if (templateId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@post
                }

                val professionalId = UUID.fromString(professionalIdStr)
                val request = call.receive<CreateAnamnesisSectionRequest>()

                val sectionId = transaction {
                    AnamnesisTemplates
                        .select { (AnamnesisTemplates.id eq templateId) and (AnamnesisTemplates.professionalId eq professionalId) }
                        .singleOrNull() ?: return@transaction null

                    // --- CORREÇÃO AQUI (A VACINA PARA O ERRO) ---
                    val maxOrderExpr = AnamnesisTemplateSections.orderIndex.max()

                    val maxOrder = AnamnesisTemplateSections
                        .slice(maxOrderExpr)
                        .select { AnamnesisTemplateSections.templateId eq templateId }
                        .singleOrNull()
                        ?.getOrNull(maxOrderExpr) ?: -1
                    // ---------------------------------------------

                    AnamnesisTemplateSections.insertAndGetId {
                        it[AnamnesisTemplateSections.templateId] = templateId
                        it[name] = request.name
                        it[description] = request.description
                        it[orderIndex] = if (request.orderIndex > 0) request.orderIndex else (maxOrder + 1)
                        it[isActive] = true
                    }.value
                }

                if (sectionId == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Modelo não encontrado"))
                    return@post
                }

                // CORRIGIDO: id.toString()
                call.respond(HttpStatusCode.Created, mapOf("id" to sectionId.toString(), "message" to "Seção criada com sucesso"))
            }

            put("/sections/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()
                val sectionId = call.parameters["id"]?.toIntOrNull()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@put
                }

                if (sectionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@put
                }

                val professionalId = UUID.fromString(professionalIdStr)
                val request = call.receive<UpdateAnamnesisSectionRequest>()

                val updated = transaction {
                    AnamnesisTemplateSections
                        .innerJoin(AnamnesisTemplates, { AnamnesisTemplateSections.templateId }, { AnamnesisTemplates.id })
                        .select { (AnamnesisTemplateSections.id eq sectionId) and (AnamnesisTemplates.professionalId eq professionalId) }
                        .singleOrNull() ?: return@transaction 0

                    AnamnesisTemplateSections.update({ AnamnesisTemplateSections.id eq sectionId }) {
                        if (request.name != null) it[name] = request.name
                        if (request.description != null) it[description] = request.description
                        if (request.orderIndex != null) it[orderIndex] = request.orderIndex
                        if (request.isActive != null) it[isActive] = request.isActive
                    }
                }

                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Seção não encontrada"))
                    return@put
                }

                call.respond(mapOf("message" to "Seção atualizada com sucesso"))
            }

            delete("/sections/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()
                val sectionId = call.parameters["id"]?.toIntOrNull()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@delete
                }

                if (sectionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@delete
                }

                val professionalId = UUID.fromString(professionalIdStr)

                val deleted = transaction {
                    AnamnesisTemplateSections
                        .innerJoin(AnamnesisTemplates, { AnamnesisTemplateSections.templateId }, { AnamnesisTemplates.id })
                        .select { (AnamnesisTemplateSections.id eq sectionId) and (AnamnesisTemplates.professionalId eq professionalId) }
                        .singleOrNull() ?: return@transaction 0

                    AnamnesisTemplateSections.update({ AnamnesisTemplateSections.id eq sectionId }) {
                        it[isActive] = false
                    }
                }

                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Seção não encontrada"))
                    return@delete
                }

                call.respond(mapOf("message" to "Seção excluída com sucesso"))
            }



















            
            // ============================================
            // PERGUNTAS
            // ============================================

// ============================================
            // PERGUNTAS (CORREÇÃO FINAL: PERMISSÃO + ORDEM)
            // ============================================

            post("/sections/{sectionId}/questions") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()
                val sectionId = call.parameters["sectionId"]?.toIntOrNull()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@post
                }

                if (sectionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@post
                }

                val professionalId = UUID.fromString(professionalIdStr)
                val request = call.receive<CreateAnamnesisQuestionRequest>()

                val questionId = transaction {
                    // 1. VERIFICAÇÃO DE SEGURANÇA CORRIGIDA
                    // Permite se: A seção existe E (pertence ao profissional OU o template é Padrão)
                    val sectionExists = AnamnesisTemplateSections
                        .innerJoin(AnamnesisTemplates, { AnamnesisTemplateSections.templateId }, { AnamnesisTemplates.id })
                        .select { 
                            (AnamnesisTemplateSections.id eq sectionId) and 
                            ((AnamnesisTemplates.professionalId eq professionalId) or (AnamnesisTemplates.isDefault eq true)) 
                        }
                        .singleOrNull() != null

                    if (!sectionExists) return@transaction null

                    // 2. CÁLCULO DA ORDEM CORRIGIDO (Evita o erro ClassCastException)
                    // Define a expressão SQL separada para o Exposed entender o tipo
                    val maxOrderExpr = AnamnesisTemplateQuestions.orderIndex.max()

                    val maxOrder = AnamnesisTemplateQuestions
                        .slice(maxOrderExpr)
                        .select { AnamnesisTemplateQuestions.sectionId eq sectionId }
                        .singleOrNull()
                        ?.getOrNull(maxOrderExpr) ?: -1
                    // -----------------------------------------------------------

                    // 3. INSERÇÃO DA PERGUNTA NO BANCO
                    val newQuestionId = AnamnesisTemplateQuestions.insertAndGetId {
                        it[AnamnesisTemplateQuestions.sectionId] = sectionId
                        it[question] = request.question
                        it[questionType] = request.questionType
                        it[isRequired] = request.isRequired
                        it[orderIndex] = if (request.orderIndex > 0) request.orderIndex else (maxOrder + 1)
                        it[placeholder] = request.placeholder
                        it[helpText] = request.helpText
                        it[minValue] = request.minValue
                        it[maxValue] = request.maxValue
                        it[isActive] = true
                    }.value

                    // 4. INSERÇÃO DAS OPÇÕES (Se for múltipla escolha)
                    if (request.questionType in listOf("SINGLE_CHOICE", "MULTIPLE_CHOICE") && !request.options.isNullOrEmpty()) {
                        request.options.forEachIndexed { index, optText ->
                            AnamnesisQuestionOptions.insert {
                                it[questionId] = newQuestionId
                                it[optionText] = optText
                                it[AnamnesisQuestionOptions.orderIndex] = index
                                it[isActive] = true
                            }
                        }
                    }

                    newQuestionId
                }

                if (questionId == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Seção não encontrada ou sem permissão"))
                    return@post
                }

                call.respond(HttpStatusCode.Created, mapOf("id" to questionId.toString(), "message" to "Pergunta criada com sucesso"))
            }




































































            put("/questions/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()
                val questionId = call.parameters["id"]?.toIntOrNull()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@put
                }

                if (questionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@put
                }

                val request = call.receive<UpdateAnamnesisQuestionRequest>()

                val updated = transaction {
                    AnamnesisTemplateQuestions.update({ AnamnesisTemplateQuestions.id eq questionId }) {
                        if (request.question != null) it[question] = request.question
                        if (request.questionType != null) it[questionType] = request.questionType
                        if (request.isRequired != null) it[isRequired] = request.isRequired
                        if (request.orderIndex != null) it[orderIndex] = request.orderIndex
                        if (request.placeholder != null) it[placeholder] = request.placeholder
                        if (request.helpText != null) it[helpText] = request.helpText
                        if (request.minValue != null) it[minValue] = request.minValue
                        if (request.maxValue != null) it[maxValue] = request.maxValue
                        if (request.isActive != null) it[isActive] = request.isActive
                    }
                }

                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Pergunta não encontrada"))
                    return@put
                }

                call.respond(mapOf("message" to "Pergunta atualizada com sucesso"))
            }

            delete("/questions/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()
                val questionId = call.parameters["id"]?.toIntOrNull()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@delete
                }

                if (questionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@delete
                }

                val deleted = transaction {
                    AnamnesisTemplateQuestions.update({ AnamnesisTemplateQuestions.id eq questionId }) {
                        it[isActive] = false
                    }
                }

                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Pergunta não encontrada"))
                    return@delete
                }

                call.respond(mapOf("message" to "Pergunta excluída com sucesso"))
            }

            // ============================================
            // OPÇÕES
            // ============================================

            post("/questions/{questionId}/options") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()
                val questionId = call.parameters["questionId"]?.toIntOrNull()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@post
                }

                if (questionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@post
                }

                val request = call.receive<CreateQuestionOptionRequest>()

                val optionId = transaction {
                    val maxOrder = AnamnesisQuestionOptions
                        .slice(AnamnesisQuestionOptions.orderIndex.max())
                        .select { AnamnesisQuestionOptions.questionId eq questionId }
                        .singleOrNull()?.getOrNull(AnamnesisQuestionOptions.orderIndex.max()) ?: -1

                    AnamnesisQuestionOptions.insertAndGetId {
                        it[AnamnesisQuestionOptions.questionId] = questionId
                        it[optionText] = request.optionText
                        it[orderIndex] = if (request.orderIndex > 0) request.orderIndex else (maxOrder + 1)
                        it[isActive] = true
                    }.value
                }

                // CORRIGIDO: id.toString()
                call.respond(HttpStatusCode.Created, mapOf("id" to optionId.toString(), "message" to "Opção criada com sucesso"))
            }

            put("/options/{id}") {
                val optionId = call.parameters["id"]?.toIntOrNull()

                if (optionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@put
                }

                val request = call.receive<UpdateQuestionOptionRequest>()

                val updated = transaction {
                    AnamnesisQuestionOptions.update({ AnamnesisQuestionOptions.id eq optionId }) {
                        if (request.optionText != null) it[optionText] = request.optionText
                        if (request.orderIndex != null) it[orderIndex] = request.orderIndex
                        if (request.isActive != null) it[isActive] = request.isActive
                    }
                }

                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Opção não encontrada"))
                    return@put
                }

                call.respond(mapOf("message" to "Opção atualizada com sucesso"))
            }

            delete("/options/{id}") {
                val optionId = call.parameters["id"]?.toIntOrNull()

                if (optionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@delete
                }

                val deleted = transaction {
                    AnamnesisQuestionOptions.update({ AnamnesisQuestionOptions.id eq optionId }) {
                        it[isActive] = false
                    }
                }

                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Opção não encontrada"))
                    return@delete
                }

                call.respond(mapOf("message" to "Opção excluída com sucesso"))
            }

            // ============================================
            // ANAMNESE DO PACIENTE
            // ============================================

            post("/apply") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@post
                }

                val professionalId = UUID.fromString(professionalIdStr)
                val request = call.receive<ApplyAnamnesisRequest>()

                val anamnesisId = transaction {
                    val template = AnamnesisTemplates
                        .select { AnamnesisTemplates.id eq request.templateId }
                        .singleOrNull() ?: return@transaction null

                    PatientAnamnesis.insertAndGetId {
                        it[patientId] = UUID.fromString(request.patientId)
                        it[templateId] = request.templateId
                        it[PatientAnamnesis.professionalId] = professionalId
                        it[templateName] = template[AnamnesisTemplates.name]
                        it[status] = "PENDING"
                        it[notes] = request.notes
                    }.value
                }

                if (anamnesisId == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Modelo não encontrado"))
                    return@post
                }

                // CORRIGIDO: id.toString() para evitar erro de Serialization (Map<String, Any>)
                call.respond(HttpStatusCode.Created, mapOf("id" to anamnesisId.toString(), "message" to "Anamnese aplicada com sucesso"))
            }

            get("/patient/{patientId}") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()
                val patientIdStr = call.parameters["patientId"]

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@get
                }

                if (patientIdStr == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@get
                }

                val patientId = try { UUID.fromString(patientIdStr) } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID do paciente inválido"))
                    return@get
                }

                val anamneses = transaction {
                    PatientAnamnesis
                        .select { PatientAnamnesis.patientId eq patientId }
                        .orderBy(PatientAnamnesis.createdAt, SortOrder.DESC)
                        .map { row ->
                            val anamnesisId = row[PatientAnamnesis.id].value

                            val answeredCount = PatientAnamnesisAnswers
                                .select { PatientAnamnesisAnswers.anamnesisId eq anamnesisId }
                                .count()

                            val totalQuestions = row[PatientAnamnesis.templateId]?.let { tId ->
                                AnamnesisTemplateSections
                                    .innerJoin(AnamnesisTemplateQuestions, { AnamnesisTemplateSections.id }, { AnamnesisTemplateQuestions.sectionId })
                                    .select { (AnamnesisTemplateSections.templateId eq tId) and (AnamnesisTemplateQuestions.isActive eq true) }
                                    .count()
                            } ?: 0

                            val progress = if (totalQuestions > 0) ((answeredCount.toDouble() / totalQuestions) * 100).toInt() else 0

                            PatientAnamnesisDTO(
                                id = anamnesisId,
                                patientId = patientId.toString(),
                                templateId = row[PatientAnamnesis.templateId],
                                templateName = row[PatientAnamnesis.templateName],
                                professionalId = null,
                                status = row[PatientAnamnesis.status],
                                notes = row[PatientAnamnesis.notes],
                                totalQuestions = totalQuestions.toInt(),
                                answeredQuestions = answeredCount.toInt(),
                                progressPercent = progress,
                                completedAt = row[PatientAnamnesis.completedAt]?.toString(),
                                createdAt = row[PatientAnamnesis.createdAt].toString(),
                                updatedAt = row[PatientAnamnesis.updatedAt].toString()
                            )
                        }
                }

                call.respond(PatientAnamnesisListResponse(anamneses = anamneses, total = anamneses.size))
            }

            get("/patient-anamnesis/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()
                val anamnesisId = call.parameters["id"]?.toIntOrNull()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@get
                }

                if (anamnesisId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@get
                }

                val anamnesis = transaction {
                    val row = PatientAnamnesis
                        .select { PatientAnamnesis.id eq anamnesisId }
                        .singleOrNull() ?: return@transaction null

                    val answers = PatientAnamnesisAnswers
                        .select { PatientAnamnesisAnswers.anamnesisId eq anamnesisId }
                        .map { answerRow ->
                            AnamnesisAnswerDTO(
                                id = answerRow[PatientAnamnesisAnswers.id].value,
                                anamnesisId = anamnesisId,
                                questionId = answerRow[PatientAnamnesisAnswers.questionId],
                                questionText = answerRow[PatientAnamnesisAnswers.questionText],
                                questionType = answerRow[PatientAnamnesisAnswers.questionType],
                                answerText = answerRow[PatientAnamnesisAnswers.answerText],
                                answerNumber = answerRow[PatientAnamnesisAnswers.answerNumber],
                                answerBoolean = answerRow[PatientAnamnesisAnswers.answerBoolean],
                                answerDate = answerRow[PatientAnamnesisAnswers.answerDate]?.toString(),
                                fileUrl = answerRow[PatientAnamnesisAnswers.fileUrl]
                            )
                        }

                    PatientAnamnesisDTO(
                        id = anamnesisId,
                        patientId = row[PatientAnamnesis.patientId].toString(),
                        templateId = row[PatientAnamnesis.templateId],
                        templateName = row[PatientAnamnesis.templateName],
                        professionalId = null,
                        status = row[PatientAnamnesis.status],
                        notes = row[PatientAnamnesis.notes],
                        answers = answers,
                        answeredQuestions = answers.size,
                        completedAt = row[PatientAnamnesis.completedAt]?.toString(),
                        createdAt = row[PatientAnamnesis.createdAt].toString(),
                        updatedAt = row[PatientAnamnesis.updatedAt].toString()
                    )
                }

                if (anamnesis == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Anamnese não encontrada"))
                    return@get
                }

                call.respond(anamnesis)
            }

            post("/patient-anamnesis/{id}/answers") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()
                val anamnesisId = call.parameters["id"]?.toIntOrNull()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@post
                }

                if (anamnesisId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@post
                }

                val request = call.receive<SaveAnamnesisAnswerRequest>()

                val answerId = transaction {
                    val question = AnamnesisTemplateQuestions
                        .select { AnamnesisTemplateQuestions.id eq request.questionId }
                        .singleOrNull() ?: return@transaction null

                    val existingAnswer = PatientAnamnesisAnswers
                        .select { (PatientAnamnesisAnswers.anamnesisId eq anamnesisId) and (PatientAnamnesisAnswers.questionId eq request.questionId) }
                        .singleOrNull()

                    if (existingAnswer != null) {
                        PatientAnamnesisAnswers.update({ PatientAnamnesisAnswers.id eq existingAnswer[PatientAnamnesisAnswers.id] }) {
                            it[answerText] = request.answerText
                            it[answerNumber] = request.answerNumber
                            it[answerBoolean] = request.answerBoolean
                            if (request.answerDate != null) {
                                it[answerDate] = Instant.parse(request.answerDate)
                            }
                            it[answerOptions] = request.answerOptions?.joinToString(",")
                            it[fileUrl] = request.fileUrl
                            it[updatedAt] = Clock.System.now()
                        }
                        existingAnswer[PatientAnamnesisAnswers.id].value
                    } else {
                        PatientAnamnesisAnswers.insertAndGetId {
                            it[PatientAnamnesisAnswers.anamnesisId] = anamnesisId
                            it[PatientAnamnesisAnswers.questionId] = request.questionId
                            it[questionText] = question[AnamnesisTemplateQuestions.question]
                            it[questionType] = question[AnamnesisTemplateQuestions.questionType]
                            it[answerText] = request.answerText
                            it[answerNumber] = request.answerNumber
                            it[answerBoolean] = request.answerBoolean
                            if (request.answerDate != null) {
                                it[answerDate] = Instant.parse(request.answerDate)
                            }
                            it[answerOptions] = request.answerOptions?.joinToString(",")
                            it[fileUrl] = request.fileUrl
                        }.value
                    }
                }

                transaction {
                    PatientAnamnesis.update({ PatientAnamnesis.id eq anamnesisId }) {
                        it[status] = "IN_PROGRESS"
                        it[updatedAt] = Clock.System.now()
                    }
                }

                if (answerId == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Pergunta não encontrada"))
                    return@post
                }

                // CORRIGIDO: id.toString()
                call.respond(mapOf("id" to answerId.toString(), "message" to "Resposta salva com sucesso"))
            }

            post("/patient-anamnesis/{id}/complete") {
                val principal = call.principal<JWTPrincipal>()
                val professionalIdStr = principal?.payload?.getClaim("id")?.asString()
                val anamnesisId = call.parameters["id"]?.toIntOrNull()

                if (professionalIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Não autorizado"))
                    return@post
                }

                if (anamnesisId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@post
                }

                val request = call.receive<CompleteAnamnesisRequest>()

                val updated = transaction {
                    PatientAnamnesis.update({ PatientAnamnesis.id eq anamnesisId }) {
                        it[status] = "COMPLETED"
                        it[completedAt] = Clock.System.now()
                        it[updatedAt] = Clock.System.now()
                        if (request.notes != null) {
                            it[notes] = request.notes
                        }
                    }
                }

                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Anamnese não encontrada"))
                    return@post
                }

                call.respond(mapOf("message" to "Anamnese finalizada com sucesso"))
            }
        }
    }
}
