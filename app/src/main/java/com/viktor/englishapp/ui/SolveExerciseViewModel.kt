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
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class SolveExerciseViewModel : ViewModel() {

    // Състояния за UI-а
    var isRecording by mutableStateOf(false)
    var isUploading by mutableStateOf(false)
    var evaluationResult by mutableStateOf<AudioEvaluationData?>(null)
    var errorMessage by mutableStateOf("")

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    // 1. СТАРТИРАНЕ НА ЗАПИСА
    fun startRecording(context: Context) {
        errorMessage = ""
        evaluationResult = null
        try {
            // Създаваме временен файл в кеша на телефона (.m4a формат е много добър за глас)
            audioFile = File(context.cacheDir, "exercise_audio.m4a")

            // Android 12+ изисква context при създаване на MediaRecorder
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

    // 2. СПИРАНЕ НА ЗАПИСА
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

    // 3. ИЗПРАЩАНЕ КЪМ AI БЕКЕНДА
    fun submitAudio(exerciseId: Int, token: String) {
        val file = audioFile
        if (file == null || !file.exists()) {
            errorMessage = "Няма записан файл!"
            return
        }

        isUploading = true
        errorMessage = ""

        // Подготвяме файла за изпращане по интернет (Multipart)
        val requestFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.submitAudioExercise(
                    exerciseId = exerciseId,
                    file = body,
                    token = "Bearer $token"
                )
                // Запазваме резултата от AI-то, за да го покажем на екрана!
                evaluationResult = response.data
            } catch (e: Exception) {
                errorMessage = "Грешка при оценяване: ${e.message}"
            } finally {
                isUploading = false
            }
        }
    }
}

