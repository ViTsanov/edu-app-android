package com.viktor.englishapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // ВАЖНО: Този импорт ни трябва за viewModel()
import com.viktor.englishapp.data.TokenManager

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel() // ИНЖЕКТИРАМЕ МОЗЪКА ТУК!
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Вход в системата", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = viewModel.username, // Вече четем от ViewModel-а
            onValueChange = { viewModel.username = it }, // Вече записваме във ViewModel-а
            label = { Text("Имейл") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text("Парола") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // ПРОСТО КАЗВАМЕ НА МОЗЪКА ДА СИ СВЪРШИ РАБОТАТА
                viewModel.login(
                    onSuccess = { token ->
                        // Запазваме токена и сменяме екрана
                        tokenManager.saveToken(token)
                        onLoginSuccess()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading
        ) {
            Text(if (viewModel.isLoading) "Зареждане..." else "Вход")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Показване на грешки от ViewModel-а
        if (viewModel.message.isNotEmpty()) {
            Text(text = viewModel.message, color = MaterialTheme.colorScheme.error)
        }
    }
}