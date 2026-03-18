package com.viktor.englishapp.domain

// --- AUTH & USER ---
data class LoginResponse(
    val access_token: String,
    val token_type: String
)

data class UserProfile(
    val id: Int,
    val username: String, // FastAPI схемата ни ползва username и email
    val email: String,
    val role_id: Int,
    val total_xp: Int // Вече имаме точки!
)

// --- EXERCISES ---
data class ExerciseResponse(
    val id: Int,
    val title: String,
    val content_prompt: String, // Променено от 'content', за да съвпада с FastAPI
    val status: String // Променено от 'cefr_level' (в базата ползваме PENDING/APPROVED)
)

data class ExerciseGenerationResponse(
    val message: String,
    val id: Int
)

// --- AI AUDIO EVALUATION ---
// Тези класове "хващат" JSON-а, който връща новият ни аудио ендпойнт
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
    val title: String,
    val instructions: String,
    val is_speaking: Boolean = false,
    val content: List<String>, // 🟢 НОВО: Вече очакваме просто списък с текстове (изреченията)
    val correct_answers: List<String>
)

data class ApproveResponse(
    val status: String,
    val message: String
)

data class CategoryItem(
    val id: Int,
    val name: String
)