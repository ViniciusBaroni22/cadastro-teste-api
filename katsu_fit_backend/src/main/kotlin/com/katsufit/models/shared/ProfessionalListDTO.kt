package com.katsufit.models.shared

import kotlinx.serialization.Serializable

@Serializable
data class ProfessionalListDTO(
    val id: String,
    val name: String,
    val email: String
)
