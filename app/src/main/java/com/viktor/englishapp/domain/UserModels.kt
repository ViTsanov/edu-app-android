package com.viktor.englishapp.domain

// --- AUTH & USER ---
data class LoginResponse(
    val access_token: String,
    val token_type: String
)

data class UserProfile(
    val id: Int,
    val username: String,
    val email: String,
    val role_id: Int,
    val total_xp: Int,
    val english_level: String? = "A1",
    val profile_picture: String? = null,
    val teacher_verification_status: String? = "none"
)

// --- EXERCISES ---
data class ExerciseResponse(
    val id: Int,
    val title: String,
    val content_prompt: String,
    val status: String
)

data class ExerciseGenerationResponse(
    val message: String,
    val id: Int
)

// --- AI AUDIO EVALUATION ---
data class AudioEvaluationData(
    val grammar_score: Int,
    val fluency_score: Int,
    val strengths: String,
    val weaknesses: String,
    val explanation: String,
    val transcribed_text: String,
    val pronunciation_tips: String
)

data class AudioSubmitResponse(
    val status: String,
    val message: String,
    val data: AudioEvaluationData
)

data class ExerciseContent(
    val title: String?,
    val instructions: String?,
    val is_speaking: Boolean = false,
    val content: List<String>?,
    val correct_answers: List<String>?
)

data class ApproveResponse(
    val status: String,
    val message: String
)

data class CategoryItem(
    val id: Int,
    val name: String
)

data class ExerciseUpdateRequest(
    val content_prompt: String
)

data class UserCreate(
    val email: String,
    val username: String,
    val password: String,
    val role_id: Int = 1
)

data class TextSubmissionRequest(
    val questions: List<String>,
    val expected_answers: List<String>,
    val user_answers: List<String>
)

data class EvaluationResult(
    val grammar_score: Int,
    val strengths: String?,
    val weaknesses: String?,
    val explanation: String?,
    val is_correct_array: List<Boolean>?,
    val xp_earned: Int?
)

data class EvaluationResponse(
    val status: String,
    val data: EvaluationResult
)

data class StudentPathItem(
    val id: Int,
    val title: String,
    val content_prompt: String,
    val status: String,
    val best_score: Int?
)

data class ClassroomItem(
    val id: Int,
    val name: String,
    val access_code: String,
    val level_id: Int,
    val student_count: Int
)

data class ClassroomStudent(
    val id: Int,
    val username: String,
    val email: String,
    val english_level: String,
    val total_xp: Int
)