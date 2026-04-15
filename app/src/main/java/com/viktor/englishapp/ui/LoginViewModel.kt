package com.viktor.englishapp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────
// ViewModel  (replaces your existing LoginViewModel.kt entirely)
// ─────────────────────────────────────────────────────────────────

class LoginViewModel : ViewModel() {

    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var errorMessage by mutableStateOf("")
    var isLoading by mutableStateOf(false)

    /**
     * Attempts login. On success saves the JWT via [tokenManager]
     * and calls [onSuccess]. On failure sets [errorMessage].
     */
    fun login(tokenManager: TokenManager, onSuccess: () -> Unit) {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Моля, въведете потребителско име и парола."
            return
        }

        isLoading = true
        errorMessage = ""

        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.login(username, password)
                // Save token through the proper manager — not raw SharedPreferences
                tokenManager.saveToken(response.access_token)
                onSuccess()
            } catch (e: Exception) {
                errorMessage = if (e.message?.contains("401") == true) {
                    "Грешни данни за вход."
                } else {
                    "Грешка при връзката: ${e.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }
}