package com.viktor.englishapp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.domain.CategoryItem
import kotlinx.coroutines.launch

class ExpertViewModel : ViewModel() {

    // Списъци, които ще се напълнят автоматично от базата данни
    var modulesList = mutableStateListOf<CategoryItem>()
    var levelsList = mutableStateListOf<CategoryItem>()

    // Избраните в момента обекти (вече пазим целия обект с ID и ИМЕ, а не само текст)
    var selectedModule by mutableStateOf<CategoryItem?>(null)
    var selectedLevel by mutableStateOf<CategoryItem?>(null)

    var isLoading by mutableStateOf(false)
    var statusMessage by mutableStateOf("")

    // init блокът се изпълнява автоматично в момента, в който екранът се отвори!
    init {
        loadMetadata()
    }

    private fun loadMetadata() {
        viewModelScope.launch {
            try {
                // Дърпаме данните от сървъра паралелно
                val modules = RetrofitClient.instance.getModules()
                val levels = RetrofitClient.instance.getLevels()

                // Пълним списъците за UI-а
                modulesList.clear()
                modulesList.addAll(modules)

                levelsList.clear()
                levelsList.addAll(levels)

                // Избираме първите елементи по подразбиране (за да не е празно)
                if (modules.isNotEmpty()) selectedModule = modules.first()
                if (levels.isNotEmpty()) selectedLevel = levels.first()

            } catch (e: Exception) {
                statusMessage = "Грешка при зареждане на модулите: ${e.message}"
            }
        }
    }

    fun generateExercise(token: String) {
        // Вземаме директно ID-тата от избраните обекти!
        val moduleId = selectedModule?.id ?: return
        val levelId = selectedLevel?.id ?: return

        isLoading = true
        statusMessage = ""

        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.generateExercise(
                    moduleId = moduleId,
                    levelId = levelId,
                    token = "Bearer $token"
                )
                statusMessage = response.message
            } catch (e: Exception) {
                statusMessage = "Грешка: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}