package com.viktor.englishapp.ui

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var errorMessage by mutableStateOf("")
    var isLoading by mutableStateOf(false)

    fun login(tokenManager: TokenManager, context: Context, onSuccess: () -> Unit) {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Моля, въведете потребителско име и парола."
            return
        }

        isLoading = true
        errorMessage = ""
        Log.d("LOGIN_DEBUG", "Login started for: $username")

        viewModelScope.launch {
            try {
                Log.d("LOGIN_DEBUG", "Sending login request...")
                val response = RetrofitClient.instance.login(username, password)
                Log.d("LOGIN_DEBUG", "Login SUCCESS — token received")

                tokenManager.saveToken(response.access_token)
                // Save role_id so ClassroomDetailScreen knows if user is teacher
                val profile = RetrofitClient.instance.getMyProfile("Bearer ${response.access_token}")
                context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    .edit().putInt("role_id", profile.role_id).apply()
                Log.d("LOGIN_DEBUG", "Token saved. Now calling FcmTokenManager...")

                FcmTokenManager.registerAfterLogin(context, response.access_token)
                Log.d("LOGIN_DEBUG", "FcmTokenManager.registerAfterLogin called")

                onSuccess()
            } catch (e: Exception) {
                Log.e("LOGIN_DEBUG", "Login FAILED: ${e.javaClass.simpleName}: ${e.message}")
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
