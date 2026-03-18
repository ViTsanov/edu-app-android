package com.viktor.englishapp.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.domain.ExerciseResponse
import kotlinx.coroutines.launch

class PendingExercisesViewModel : ViewModel() {
    // Използваме mutableStateListOf, за да може Compose автоматично
    // да обновява екрана, когато премахнем одобрено упражнение
    var pendingExercises = mutableStateListOf<ExerciseResponse>()
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf("")

    fun loadPendingExercises(token: String) {
        isLoading = true
        errorMessage = ""
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getPendingExercises("Bearer $token")
                pendingExercises.clear()
                pendingExercises.addAll(response)
            } catch (e: Exception) {
                errorMessage = "Грешка при зареждане: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun approveExercise(token: String, exerciseId: Int) {
        viewModelScope.launch {
            try {
                // Изпращаме заявка към бекенда за одобрение
                RetrofitClient.instance.approveExercise(exerciseId, "Bearer $token")

                // Премахваме упражнението от списъка визуално
                pendingExercises.removeAll { it.id == exerciseId }
            } catch (e: Exception) {
                errorMessage = "Грешка при одобряване: ${e.message}"
            }
        }
    }
}