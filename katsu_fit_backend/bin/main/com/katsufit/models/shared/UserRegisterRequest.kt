package com.katsufit.models.shared

import kotlinx.serialization.Serializable

@Serializable
data class UserRegisterRequest(
    val email: String,
    val password: String,
    val userType: String,
    val name: String // <-- CAMPO ADICIONADO AQUI
)
