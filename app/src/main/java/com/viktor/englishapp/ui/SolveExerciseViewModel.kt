package com.viktor.englishapp.ui

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.domain.AudioEvaluationData
import com.viktor.englishapp.domain.EvaluationResult
import com.viktor.englishapp.domain.TextSubmissionRequest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class SolveExerciseViewModel : ViewModel() {

    // Състояния за UI-а
    var isRecording by mutableStateOf(false)
    var isUploading by mutableStateOf(false)
    var isEvaluatingText by mutableStateOf(false)

    // 🟢 РАЗДЕЛЯМЕ РЕЗУЛТАТИТЕ, ЗА ДА НЯМА КОНФЛИКТ
    var audioEvaluationResult by mutableStateOf<AudioEvaluationData?>(null)
    var textEvaluationResult by mutableStateOf<EvaluationResult?>(null)

    var errorMessage by mutableStateOf("")

    var homeworkId: Int = 0

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    // === АУДИО ЛОГИКА ===
    fun startRecording(context: Context) {
        errorMessage = ""
        audioEvaluationResult = null
        try {
            audioFile = File(context.cacheDir, "exercise_audio.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
        } catch (e: Exception) {
            errorMessage = "Грешка при запис: ${e.message}"
            isRecording = false
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
        } catch (e: Exception) {
            errorMessage = "Грешка при спиране: ${e.message}"
        }
    }

    fun submitAudio(exerciseId: Int, token: String) {
        val file = audioFile
        if (file == null || !file.exists()) {
            errorMessage = "Няма записан файл!"
            return
        }

        isUploading = true
        errorMessage = ""

        val requestFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        viewModelScope.launch {
            try {
                if (homeworkId > 0) {
                    // ── Homework audio path ───────────────────────
                    RetrofitClient.instance.submitHomeworkAudio(
                        homeworkId = homeworkId,
                        token = "Bearer $token",
                        file = body
                    )
                    // For homework audio we show a simple success — no live result needed
                    errorMessage = "✓ Аудиото е предадено успешно!"
                } else {
                    // ── Regular audio path ────────────────────────
                    val response = RetrofitClient.instance.submitAudioExercise(
                        exerciseId = exerciseId,
                        file = body,
                        token = "Bearer $token"
                    )
                    audioEvaluationResult = response.data
                }
            } catch (e: Exception) {
                errorMessage = "Грешка при оценяване: ${e.message}"
            } finally {
                isUploading = false
            }
        }
    }

    // === ТЕКСТОВА ЛОГИКА ===
    fun submitTextExercise(
        exerciseId: Int,
        questions: List<String>,
        expectedAnswers: List<String>,
        userAnswers: List<String>,
        token: String
    ) {
        viewModelScope.launch {
            isEvaluatingText = true
            try {
                if (homeworkId > 0) {
                    // ── Homework submission path ──────────────────
                    val response = RetrofitClient.instance.submitHomeworkText(
                        homeworkId = homeworkId,
                        token = "Bearer $token",
                        body = mapOf(
                            "questions" to questions,
                            "expected_answers" to expectedAnswers,
                            "user_answers" to userAnswers
                        )
                    )
                    // Backend returns the same shape under "data"
                    @Suppress("UNCHECKED_CAST")
                    val data = response["data"] as? Map<String, Any>
                    if (data != null) {
                        textEvaluationResult = EvaluationResult(
                            grammar_score = (data["grammar_score"] as? Double)?.toInt() ?: 0,
                            strengths = data["strengths"] as? String,
                            weaknesses = data["weaknesses"] as? String,
                            explanation = data["explanation"] as? String,
                            is_correct_array = null,
                            xp_earned = (data["xp_earned"] as? Double)?.toInt()
                        )
                    }
                } else {
                    // ── Regular exercise submission path ──────────
                    val request = TextSubmissionRequest(
                        questions = questions,
                        expected_answers = expectedAnswers,
                        user_answers = userAnswers
                    )
                    val response = RetrofitClient.instance.submitTextExercise(
                        exerciseId = exerciseId,
                        request = request,
                        token = "Bearer $token"
                    )
                    if (response.status == "success") {
                        textEvaluationResult = response.data
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Грешка: ${e.message}"
            } finally {
                isEvaluatingText = false
            }
        }
    }
}