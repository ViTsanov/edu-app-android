package com.viktor.englishapp.data

import com.viktor.englishapp.domain.*
import okhttp3.MultipartBody
import retrofit2.http.*
import com.viktor.englishapp.domain.ClassroomItem
import com.viktor.englishapp.domain.ClassroomStudent

interface ApiService {

    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): LoginResponse

    @GET("users/me")
    suspend fun getMyProfile(
        @Header("Authorization") token: String
    ): UserProfile

    @POST("expert/generate-exercise")
    suspend fun generateExercise(
        @Query("module_id") moduleId: Int,
        @Query("level_id") levelId: Int,
        @Header("Authorization") token: String
    ): ExerciseGenerationResponse

    @GET("exercises")
    suspend fun getExercises(
        @Header("Authorization") token: String
    ): List<ExerciseResponse>

    @GET("exercises/my-path")
    suspend fun getMyPath(
        @Header("Authorization") token: String
    ): List<StudentPathItem>

    @GET("expert/pending")
    suspend fun getPendingExercises(
        @Header("Authorization") token: String
    ): List<ExerciseResponse>

    @PUT("expert/approve/{exercise_id}")
    suspend fun approveExercise(
        @Path("exercise_id") exerciseId: Int,
        @Header("Authorization") token: String
    ): ApproveResponse

    @Multipart
    @POST("exercises/{exercise_id}/submit-audio")
    suspend fun submitAudioExercise(
        @Path("exercise_id") exerciseId: Int,
        @Part file: MultipartBody.Part,
        @Header("Authorization") token: String
    ): AudioSubmitResponse

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
    ): ApproveResponse

    @POST("users/")
    suspend fun register(@Body request: UserCreate): UserProfile

    @POST("exercises/{exercise_id}/submit-text")
    suspend fun submitTextExercise(
        @Path("exercise_id") exerciseId: Int,
        @Body request: TextSubmissionRequest,
        @Header("Authorization") token: String
    ): EvaluationResponse

    @POST("teacher/classrooms")
    suspend fun createClassroom(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Map<String, Any>

    @GET("teacher/classrooms")
    suspend fun getTeacherClassrooms(
        @Header("Authorization") token: String
    ): List<ClassroomItem>

    @GET("teacher/classrooms/{classroom_id}/students")
    suspend fun getClassroomStudents(
        @Path("classroom_id") classroomId: Int,
        @Header("Authorization") token: String
    ): List<ClassroomStudent>

    @DELETE("teacher/classrooms/{classroom_id}/students/{student_id}")
    suspend fun removeStudentFromClassroom(
        @Path("classroom_id") classroomId: Int,
        @Path("student_id") studentId: Int,
        @Header("Authorization") token: String
    ): Map<String, String>

    @POST("student/join-classroom")
    suspend fun joinClassroom(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Map<String, String>

    @GET("student/my-classrooms")
    suspend fun getStudentClassrooms(
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

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

    @GET("teacher/tests/{test_id}/results")
    suspend fun getTestResults(
        @Path("test_id") testId: Int,
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

    @GET("teacher/classrooms/{classroom_id}/monitoring")
    suspend fun getClassroomMonitoring(
        @Path("classroom_id") classroomId: Int,
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

    @POST("sessions/start")
    suspend fun startSession(
        @Header("Authorization") token: String
    ): Map<String, Int>

    @PUT("sessions/{session_id}/end")
    suspend fun endSession(
        @Path("session_id") sessionId: Int,
        @Header("Authorization") token: String
    ): Map<String, Any>

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

    @GET("student/homework")
    suspend fun getStudentHomework(
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

    @GET("exercises/{exercise_id}/my-result")
    suspend fun getMyExerciseResult(
        @Path("exercise_id") exerciseId: Int,
        @Header("Authorization") token: String
    ): Map<String, Any>

    @POST("teacher/homework")
    suspend fun createHomework(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any>

    @GET("teacher/homework")
    suspend fun getTeacherHomework(
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

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

    @PUT("teacher/tests/{test_id}/classroom")
    suspend fun updateTestClassroom(
        @Path("test_id") testId: Int,
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Map<String, Any>

    @GET("classrooms/{classroom_id}/detail")
    suspend fun getClassroomDetail(
        @Path("classroom_id") classroomId: Int,
        @Header("Authorization") token: String
    ): Map<String, Any>

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

    @GET("teacher/homework/{homework_id}/submissions")
    suspend fun getHomeworkSubmissions(
        @Path("homework_id") homeworkId: Int,
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

    @PUT("users/me/fcm-token")
    suspend fun updateFcmToken(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Map<String, String>

    @POST("expert/create-exercise-manual")
    suspend fun createExerciseAsExpert(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any>

    @PUT("teacher/exercises/{exercise_id}")
    suspend fun updateTeacherExercise(
        @Path("exercise_id") exerciseId: Int,
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, String>
}
