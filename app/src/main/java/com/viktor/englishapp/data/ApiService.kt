package com.viktor.englishapp.data

import com.viktor.englishapp.domain.*
import okhttp3.MultipartBody
import retrofit2.http.*
import com.viktor.englishapp.domain.ClassroomItem
import com.viktor.englishapp.domain.ClassroomStudent

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

    @GET("exercises/my-path")
    suspend fun getMyPath(
        @Header("Authorization") token: String
    ): List<StudentPathItem>

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

    @POST("teacher/classrooms")
    suspend fun createClassroom(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Map<String, Any>

    @GET("teacher/classrooms")
    suspend fun getTeacherClassrooms(
        @Header("Authorization") token: String
    ): List<ClassroomItem>    // from ClassroomManagementScreen.kt

    @GET("teacher/classrooms/{classroom_id}/students")
    suspend fun getClassroomStudents(
        @Path("classroom_id") classroomId: Int,
        @Header("Authorization") token: String
    ): List<ClassroomStudent>  // from ClassroomManagementScreen.kt

    @DELETE("teacher/classrooms/{classroom_id}/students/{student_id}")
    suspend fun removeStudentFromClassroom(
        @Path("classroom_id") classroomId: Int,
        @Path("student_id") studentId: Int,
        @Header("Authorization") token: String
    ): Map<String, String>

    @POST("student/join-classroom")
    suspend fun joinClassroom(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>   // {"access_code": "XYZ123"}
    ): Map<String, String>

    @GET("student/my-classrooms")
    suspend fun getMyClassrooms(
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

// ── Test endpoints ───────────────────────────────────────────────

    @POST("teacher/tests")
    suspend fun createTest(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Map<String, Any>

    @GET("teacher/tests")
    suspend fun getTeacherTests(
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

    @POST("teacher/tests/{test_id}/exercises")
    suspend fun addExerciseToTest(
        @Path("test_id") testId: Int,
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Map<String, String>

    @PUT("teacher/tests/{test_id}/activate")
    suspend fun toggleTestActive(
        @Path("test_id") testId: Int,
        @Header("Authorization") token: String
    ): Map<String, Any>

    @GET("teacher/tests/{test_id}/results")
    suspend fun getTestResults(
        @Path("test_id") testId: Int,
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

    @GET("student/active-tests")
    suspend fun getActiveTests(
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

    @GET("student/tests/{test_id}")
    suspend fun getTestForStudent(
        @Path("test_id") testId: Int,
        @Header("Authorization") token: String
    ): Map<String, Any>

    @POST("student/tests/{test_id}/submit")
    suspend fun submitTest(
        @Path("test_id") testId: Int,
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Map<String, Any>

// ── Monitoring ───────────────────────────────────────────────────

    @GET("teacher/classrooms/{classroom_id}/monitoring")
    suspend fun getClassroomMonitoring(
        @Path("classroom_id") classroomId: Int,
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

// ── Session tracking ─────────────────────────────────────────────

    @POST("sessions/start")
    suspend fun startSession(
        @Header("Authorization") token: String
    ): Map<String, Int>    // {"session_id": 5}

    @PUT("sessions/{session_id}/end")
    suspend fun endSession(
        @Path("session_id") sessionId: Int,
        @Header("Authorization") token: String
    ): Map<String, Any>

// ── Teacher exercise library ─────────────────────────────────────

    @POST("teacher/exercises")
    suspend fun saveTeacherExercise(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Map<String, Any>

    @GET("teacher/exercises")
    suspend fun getTeacherExercises(
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

    @DELETE("teacher/exercises/{exercise_id}")
    suspend fun deleteTeacherExercise(
        @Path("exercise_id") exerciseId: Int,
        @Header("Authorization") token: String
    ): Map<String, String>

// ── AI improvement suggestions ───────────────────────────────────

    @GET("student/improvement-suggestions")
    suspend fun getImprovementSuggestions(
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

    @PUT("student/improvement-suggestions/{id}/read")
    suspend fun markSuggestionRead(
        @Path("id") suggestionId: Int,
        @Header("Authorization") token: String
    ): Map<String, String>

    // Student classrooms
    @GET("student/my-classrooms")
    suspend fun getStudentClassrooms(
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

    // Student homework
    @GET("student/homework")
    suspend fun getStudentHomework(
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

    // Exercise result review
    @GET("exercises/{exercise_id}/my-result")
    suspend fun getMyExerciseResult(
        @Path("exercise_id") exerciseId: Int,
        @Header("Authorization") token: String
    ): Map<String, Any>

    // Teacher homework
    @POST("teacher/homework")
    suspend fun createHomework(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any>

    @GET("teacher/homework")
    suspend fun getTeacherHomework(
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

    @DELETE("teacher/homework/{homework_id}")
    suspend fun deleteHomework(
        @Path("homework_id") homeworkId: Int,
        @Header("Authorization") token: String
    ): Map<String, String>

    // Test activation with opening time
    @PUT("teacher/tests/{test_id}/activate")
    suspend fun activateTestWithTime(
        @Path("test_id") testId: Int,
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any>

    @PUT("teacher/tests/{test_id}/deactivate")
    suspend fun deactivateTest(
        @Path("test_id") testId: Int,
        @Header("Authorization") token: String
    ): Map<String, Any>

    // Classroom detail (student opens a specific classroom)
    @GET("classrooms/{classroom_id}/detail")
    suspend fun getClassroomDetail(
        @Path("classroom_id") classroomId: Int,
        @Header("Authorization") token: String
    ): Map<String, Any>

    // Homework submission endpoints (student submits homework)
    @POST("homework/{homework_id}/submit-text")
    suspend fun submitHomeworkText(
        @Path("homework_id") homeworkId: Int,
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Map<String, Any>

    @Multipart
    @POST("homework/{homework_id}/submit-audio")
    suspend fun submitHomeworkAudio(
        @Path("homework_id") homeworkId: Int,
        @Header("Authorization") token: String,
        @Part file: okhttp3.MultipartBody.Part
    ): Map<String, Any>

    // Teacher: see all students' answers for a homework
    @GET("teacher/homework/{homework_id}/submissions")
    suspend fun getHomeworkSubmissions(
        @Path("homework_id") homeworkId: Int,
        @Header("Authorization") token: String
    ): List<Map<String, Any>>
}