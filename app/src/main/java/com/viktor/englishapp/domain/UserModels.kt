package com.viktor.englishapp.domain // Внимавай тук да е твоят точен пакет!

// Това е отговорът, който сървърът връща при успешен Login
data class LoginResponse(
    val access_token: String,
    val token_type: String
)

// Това е профилът на потребителя (от маршрута /users/me)
data class UserResponse(
    val id: Int,
    val email: String,
    val full_name: String,
    val role: String
)