package com.katsufit.routes.nutritionist

import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*

// Limites
const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
const val MAX_FILES_PER_CATEGORY = 50
val ALLOWED_EXTENSIONS = listOf("pdf", "png", "jpg", "jpeg")

fun Route.documentRouting() {
    authenticate("auth-jwt") {
        route("/api/documents") {

            // GET - Listar todos os documentos do profissional
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                    return@get
                }

                val professionalId = UUID.fromString(userId)

                val documents = transaction {
                    Documents.selectAll()
                        .where { Documents.professionalId eq professionalId }
                        .orderBy(Documents.createdAt, SortOrder.DESC)
                        .map { row ->
                            DocumentResponse(
                                id = row[Documents.id].value,
                                professionalId = row[Documents.professionalId].toString(),
                                name = row[Documents.name],
                                category = row[Documents.category],
                                fileUrl = row[Documents.fileUrl],
                                fileType = row[Documents.fileType],
                                createdAt = row[Documents.createdAt].toString()
                            )
                        }
                }

                call.respond(HttpStatusCode.OK, documents)
            }

            // GET - Listar por categoria
            get("/category/{category}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                val category = call.parameters["category"]

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                    return@get
                }

                if (category == null) {
                    call.respond(HttpStatusCode.BadRequest, "Categoria não informada")
                    return@get
                }

                val professionalId = UUID.fromString(userId)

                val documents = transaction {
                    Documents.selectAll()
                        .where { (Documents.professionalId eq professionalId) and (Documents.category eq category) }
                        .orderBy(Documents.createdAt, SortOrder.DESC)
                        .map { row ->
                            DocumentResponse(
                                id = row[Documents.id].value,
                                professionalId = row[Documents.professionalId].toString(),
                                name = row[Documents.name],
                                category = row[Documents.category],
                                fileUrl = row[Documents.fileUrl],
                                fileType = row[Documents.fileType],
                                createdAt = row[Documents.createdAt].toString()
                            )
                        }
                }

                call.respond(HttpStatusCode.OK, documents)
            }

            // POST - Upload de documento
            post("/upload") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                    return@post
                }

                val professionalId = UUID.fromString(userId)

                val multipart = call.receiveMultipart()
                var fileName: String? = null
                var category: String? = null
                var fileBytes: ByteArray? = null
                var fileExtension: String? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            when (part.name) {
                                "name" -> fileName = part.value
                                "category" -> category = part.value
                            }
                        }
                        is PartData.FileItem -> {
                            val originalFileName = part.originalFileName ?: "arquivo"
                            fileExtension = originalFileName.substringAfterLast(".", "").lowercase()
                            fileBytes = part.streamProvider().readBytes()
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                // Validações
                if (fileName == null || category == null || fileBytes == null || fileExtension == null) {
                    call.respond(HttpStatusCode.BadRequest, "Dados incompletos. Envie: name, category e file")
                    return@post
                }

                if (fileExtension !in ALLOWED_EXTENSIONS) {
                    call.respond(HttpStatusCode.BadRequest, "Tipo de arquivo não permitido. Use: PDF, PNG, JPG")
                    return@post
                }

                if (fileBytes!!.size > MAX_FILE_SIZE) {
                    call.respond(HttpStatusCode.BadRequest, "Arquivo muito grande. Máximo: 5MB")
                    return@post
                }

                // Verificar limite de arquivos por categoria
                val filesInCategory = transaction {
                    Documents.selectAll()
                        .where { (Documents.professionalId eq professionalId) and (Documents.category eq category!!) }
                        .count()
                }

                if (filesInCategory >= MAX_FILES_PER_CATEGORY) {
                    call.respond(HttpStatusCode.BadRequest, "Limite de $MAX_FILES_PER_CATEGORY arquivos por categoria atingido")
                    return@post
                }

                // Criar pasta se não existir
                val uploadDir = File("uploads/documents/$userId")
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs()
                }

                // Salvar arquivo
                val uniqueFileName = "${UUID.randomUUID()}.$fileExtension"
                val file = File(uploadDir, uniqueFileName)
                file.writeBytes(fileBytes!!)

                val fileUrl = "/uploads/documents/$userId/$uniqueFileName"

                // Salvar no banco
                val documentId = transaction {
                    Documents.insertAndGetId {
                        it[Documents.professionalId] = professionalId
                        it[name] = fileName!!
                        it[Documents.category] = category!!
                        it[Documents.fileUrl] = fileUrl
                        it[fileType] = fileExtension!!
                    }.value
                }

                call.respond(HttpStatusCode.Created, UploadResponse(
                    id = documentId,
                    fileUrl = fileUrl,
                    message = "Documento enviado com sucesso"
                ))
            }

            // POST - Criar documento (sem upload, só referência)
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                    return@post
                }

                val request = call.receive<DocumentRequest>()
                val professionalId = UUID.fromString(userId)

                val documentId = transaction {
                    Documents.insertAndGetId {
                        it[Documents.professionalId] = professionalId
                        it[name] = request.name
                        it[category] = request.category
                        it[fileUrl] = request.fileUrl
                        it[fileType] = request.fileType
                    }.value
                }

                call.respond(HttpStatusCode.Created, UploadResponse(
                    id = documentId,
                    fileUrl = request.fileUrl,
                    message = "Documento criado com sucesso"
                ))
            }

            // PUT - Atualizar documento (nome ou categoria)
            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                val documentId = call.parameters["id"]?.toIntOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                    return@put
                }

                if (documentId == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID inválido")
                    return@put
                }

                val request = call.receive<DocumentUpdateRequest>()
                val professionalId = UUID.fromString(userId)

                val updated = transaction {
                    Documents.update({ (Documents.id eq documentId) and (Documents.professionalId eq professionalId) }) {
                        if (request.name != null) it[name] = request.name
                        if (request.category != null) it[category] = request.category
                    }
                }

                if (updated > 0) {
                    call.respond(HttpStatusCode.OK, UploadResponse(
                        id = documentId,
                        fileUrl = "",
                        message = "Documento atualizado com sucesso"
                    ))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Documento não encontrado")
                }
            }

            // DELETE - Deletar documento
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                val documentId = call.parameters["id"]?.toIntOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                    return@delete
                }

                if (documentId == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID inválido")
                    return@delete
                }

                val professionalId = UUID.fromString(userId)

                // Buscar arquivo para deletar
                val document = transaction {
                    Documents.selectAll()
                        .where { (Documents.id eq documentId) and (Documents.professionalId eq professionalId) }
                        .firstOrNull()
                }

                if (document == null) {
                    call.respond(HttpStatusCode.NotFound, "Documento não encontrado")
                    return@delete
                }

                // Deletar arquivo físico
                val fileUrl = document[Documents.fileUrl]
                val file = File(fileUrl.removePrefix("/"))
                if (file.exists()) {
                    file.delete()
                }

                // Deletar compartilhamentos
                transaction {
                    SharedDocuments.deleteWhere { SharedDocuments.documentId eq documentId }
                }

                // Deletar do banco
                transaction {
                    Documents.deleteWhere { (Documents.id eq documentId) and (Documents.professionalId eq professionalId) }
                }

                call.respond(HttpStatusCode.OK, UploadResponse(
                    id = documentId,
                    fileUrl = "",
                    message = "Documento deletado com sucesso"
                ))
            }

            // GET - Servir arquivo
            get("/file/{userId}/{fileName}") {
                val fileUserId = call.parameters["userId"]
                val fileName = call.parameters["fileName"]

                if (fileUserId == null || fileName == null) {
                    call.respond(HttpStatusCode.BadRequest, "Parâmetros inválidos")
                    return@get
                }

                val file = File("uploads/documents/$fileUserId/$fileName")

                if (!file.exists()) {
                    call.respond(HttpStatusCode.NotFound, "Arquivo não encontrado")
                    return@get
                }

                call.respondFile(file)
            }

            // ========================================
            // <<< COMPARTILHAMENTO DE DOCUMENTOS >>>
            // ========================================

            // POST - Compartilhar documento com pacientes
            post("/share") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                    return@post
                }

                val professionalId = UUID.fromString(userId)
                val request = call.receive<ShareDocumentRequest>()

                // Verificar se o documento pertence ao profissional
                val document = transaction {
                    Documents.selectAll()
                        .where { (Documents.id eq request.documentId) and (Documents.professionalId eq professionalId) }
                        .firstOrNull()
                }

                if (document == null) {
                    call.respond(HttpStatusCode.NotFound, "Documento não encontrado")
                    return@post
                }

                // Compartilhar com cada paciente
                var sharedCount = 0
                transaction {
                    for (patientIdStr in request.patientIds) {
                        val patientId = UUID.fromString(patientIdStr)

                        // Verificar se já não está compartilhado
                        val alreadyShared = SharedDocuments.selectAll()
                            .where { (SharedDocuments.documentId eq request.documentId) and (SharedDocuments.patientId eq patientId) }
                            .count() > 0

                        if (!alreadyShared) {
                            SharedDocuments.insert {
                                it[SharedDocuments.documentId] = org.jetbrains.exposed.dao.id.EntityID(request.documentId, Documents)
                                it[SharedDocuments.patientId] = patientId
                                it[SharedDocuments.professionalId] = professionalId
                            }
                            sharedCount++
                        }
                    }
                }

                call.respond(HttpStatusCode.OK, UploadResponse(
                    id = request.documentId,
                    fileUrl = "",
                    message = "Documento compartilhado com $sharedCount paciente(s)"
                ))
            }

            // GET - Listar pacientes com quem o documento foi compartilhado
            get("/{id}/shared") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                val documentId = call.parameters["id"]?.toIntOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                    return@get
                }

                if (documentId == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID inválido")
                    return@get
                }

                val professionalId = UUID.fromString(userId)

                val sharedWith = transaction {
                    (SharedDocuments innerJoin Users)
                        .selectAll()
                        .where { (SharedDocuments.documentId eq documentId) and (SharedDocuments.professionalId eq professionalId) }
                        .map { row ->
                            SharedDocumentResponse(
                                id = row[SharedDocuments.id].value,
                                documentId = row[SharedDocuments.documentId].value,
                                documentName = "",
                                patientId = row[SharedDocuments.patientId].value.toString(),
                                patientName = row[Users.name],
                                sharedAt = row[SharedDocuments.sharedAt].toString()
                            )
                        }
                }

                call.respond(HttpStatusCode.OK, sharedWith)
            }

            // DELETE - Remover compartilhamento
            delete("/{documentId}/shared/{patientId}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                val documentId = call.parameters["documentId"]?.toIntOrNull()
                val patientIdStr = call.parameters["patientId"]

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                    return@delete
                }

                if (documentId == null || patientIdStr == null) {
                    call.respond(HttpStatusCode.BadRequest, "Parâmetros inválidos")
                    return@delete
                }

                val professionalId = UUID.fromString(userId)
                val patientId = UUID.fromString(patientIdStr)

                val deleted = transaction {
                    SharedDocuments.deleteWhere {
                        (SharedDocuments.documentId eq documentId) and
                        (SharedDocuments.patientId eq patientId) and
                        (SharedDocuments.professionalId eq professionalId)
                    }
                }

                if (deleted > 0) {
                    call.respond(HttpStatusCode.OK, UploadResponse(
                        id = documentId,
                        fileUrl = "",
                        message = "Compartilhamento removido"
                    ))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Compartilhamento não encontrado")
                }
            }
        }

        // ========================================
        // <<< DOCUMENTOS DO PACIENTE >>>
        // ========================================
        route("/api/patient/documents") {

            // GET - Listar documentos compartilhados com o paciente
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Token inválido")
                    return@get
                }

                val patientId = UUID.fromString(userId)

                val documents = transaction {
                    (SharedDocuments innerJoin Documents)
                        .join(Users, JoinType.INNER, SharedDocuments.professionalId, Users.id)
                        .selectAll()
                        .where { SharedDocuments.patientId eq patientId }
                        .orderBy(SharedDocuments.sharedAt, SortOrder.DESC)
                        .map { row ->
                            PatientDocumentResponse(
                                id = row[SharedDocuments.id].value,
                                documentId = row[Documents.id].value,
                                name = row[Documents.name],
                                category = row[Documents.category],
                                fileUrl = row[Documents.fileUrl],
                                fileType = row[Documents.fileType],
                                professionalName = row[Users.name],
                                sharedAt = row[SharedDocuments.sharedAt].toString()
                            )
                        }
                }

                call.respond(HttpStatusCode.OK, documents)
            }
        }
    }
}
