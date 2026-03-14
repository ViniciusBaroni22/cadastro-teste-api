package com.katsufit.models.shared

import kotlinx.serialization.Serializable

@Serializable
data class ProfessionalProfileRequest(
    val documentId: String,
    val name: String,
    val profession: String,
    val bio: String?,
    val specialty: String?
)
