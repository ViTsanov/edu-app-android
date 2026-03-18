package com.viktor.englishapp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viktor.englishapp.data.RetrofitClient
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    // 1. Тук пазим състоянието на екрана
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var message by mutableStateOf("")
    var isLoading by mutableStateOf(false)

    // 2. Тук е бизнес логиката
    fun login(onSuccess: (String) -> Unit) {
        // Малка защита: Проверяваме дали полетата не са празни
        if (username.isBlank() || password.isBlank()) {
            message = "Моля, въведете имейл и парола."
            return
        }

        isLoading = true
        message = ""

        // viewModelScope автоматично управлява корутината и я спира, ако екранът бъде затворен
        viewModelScope.launch {
            try {
                // Викаме мрежата
                val response = RetrofitClient.instance.login(username, password)

                // Връщаме токена обратно на UI-а, за да го запази
                onSuccess(response.access_token)
            } catch (e: Exception) {
                // Обработка на грешки (например грешна парола)
                message = if (e.message?.contains("401") == true) {
                    "Грешен имейл или парола"
                } else {
                    "Грешка: ${e.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }
}

