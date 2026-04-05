package com.viktor.englishapp.data

import com.viktor.englishapp.domain.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    // 1. ВХОД (Login)
    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): LoginResponse

    // 2. ПРОФИЛ
    @GET("users/me")
    suspend fun getMyProfile(
        @Header("Authorization") token: String
    ): UserProfile

    // 3. ГЕНЕРИРАНЕ НА УПРАЖНЕНИЕ ОТ ЕКСПЕРТ
    // Променихме параметрите на Int, защото в базата вече ползваме ID-та за нива и модули
    @POST("expert/generate-exercise")
    suspend fun generateExercise(
        @Query("module_id") moduleId: Int,
        @Query("level_id") levelId: Int,
        @Header("Authorization") token: String
    ): ExerciseGenerationResponse

    // 4. СПИСЪК С УПРАЖНЕНИЯ
    @GET("exercises")
    suspend fun getExercises(
        @Header("Authorization") token: String
    ): List<ExerciseResponse>

    // 6. СПИСЪК С ЧАКАЩИ УПРАЖНЕНИЯ (Само за експерти)
    @GET("expert/pending")
    suspend fun getPendingExercises(
        @Header("Authorization") token: String
    ): List<ExerciseResponse>

    // 7. ОДОБРЯВАНЕ НА УПРАЖНЕНИЕ
    @PUT("expert/approve/{exercise_id}")
    suspend fun approveExercise(
        @Path("exercise_id") exerciseId: Int,
        @Header("Authorization") token: String
    ): ApproveResponse

    // 5. НОВО: ИЗПРАЩАНЕ НА АУДИО ЗА AI ОЦЕНКА 🎙️🤖
    // @Multipart казва на Android, че няма да пращаме прост текст, а същински файл (chunks of binary data)
    @Multipart
    @POST("exercises/{exercise_id}/submit-audio")
    suspend fun submitAudioExercise(
        @Path("exercise_id") exerciseId: Int, // Замества {exercise_id} в URL-то
        @Part file: MultipartBody.Part,       // Самият аудио файл (.wav / .m4a)
        @Header("Authorization") token: String
    ): AudioSubmitResponse

    // 8. ДИНАМИЧНИ МОДУЛИ И НИВА
    @GET("modules")
    suspend fun getModules(): List<CategoryItem>

    @GET("levels")
    suspend fun getLevels(): List<CategoryItem>

    @PUT("expert/edit/{exercise_id}")
    suspend fun editExercise(
        @Path("exercise_id") exerciseId: Int,
        @Body request: ExerciseUpdateRequest,
        @Header("Authorization") token: String
    ): ApproveResponse

    @DELETE("expert/reject/{exercise_id}")
    suspend fun rejectExercise(
        @Path("exercise_id") exerciseId: Int,
        @Header("Authorization") token: String
    ): ApproveResponse // Можем да преизползваме този модел, тъй като сървърът връща същия формат {"status": "...", "message": "..."}

    // Регистрация на нов потребител
    @POST("users/")
    suspend fun register(@Body request: UserCreate): UserProfile

    @POST("exercises/{exercise_id}/submit-text")
    suspend fun submitTextExercise(
        @Path("exercise_id") exerciseId: Int,
        @Body request: TextSubmissionRequest,
        @Header("Authorization") token: String
    ): EvaluationResponse // (Използвай същия Response модел като при аудиото)
}