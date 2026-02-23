package com.viktor.englishapp.data

import com.viktor.englishapp.domain.LoginResponse
import com.viktor.englishapp.domain.UserResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

data class UserProfile(
    val id: Int,
    val email: String,
    val full_name: String,
    val role: String,
    val is_active: Boolean
)

data class ExerciseResponse(
    val id: Int,
    val title: String,
    val content: String,
    val cefr_level: String
)

data class Question(
    val original: String,
    val task: String
)

data class ExerciseContent(
    val title: String,
    val instructions: String,
    val content: List<Question>,
    val correct_answers: List<String>
)

interface ApiService {

    // 1. Маршрутът за ВХОД (Login)
    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("username") username: String, // FastAPI очаква 'username', дори да пращаме имейл
        @Field("password") password: String
    ): LoginResponse

    // 2. Маршрутът за ПРОФИЛ (Вземане на данните на логнатия потребител)
    @GET("users/me")
    suspend fun getMyProfile(
        // Тук слагаме ключа: "Bearer <token>"
        @Header("Authorization") token: String
    ): UserProfile

    @POST("expert/generate-exercise")
    suspend fun generateExercise(
        @Query("module") module: String,
        @Query("level") level: String,
        @Header("Authorization") token: String
    ): Any

    @GET("exercises")
    suspend fun getExercises(@Header("Authorization") token: String): List<ExerciseResponse>
}
