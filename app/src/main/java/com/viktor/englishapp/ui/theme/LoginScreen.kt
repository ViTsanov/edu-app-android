package com.viktor.englishapp.ui // ПРОВЕРИ ДАЛИ ТОВА Е ТВОЯТ ПАКЕТ!

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.viktor.englishapp.data.RetrofitClient // Тук също провери пакета
import kotlinx.coroutines.launch

@Composable
fun LoginScreen() {
    // Тук пазим състоянието на полетата (какво е написал потребителят)
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") } // За съобщения (Успех/Грешка)
    var isLoading by remember { mutableStateOf(false) } // Върти ли се индикаторът?

    // Нужно ни е, за да извикаме сървъра във фонов режим (без да забива екранът)
    val coroutineScope = rememberCoroutineScope()

    // Вертикална колона, центрирана в средата на екрана
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Вход в системата", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        // Поле за Имейл
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Имейл") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Поле за Парола
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Парола") },
            visualTransformation = PasswordVisualTransformation(), // Скрива паролата със звездички
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Бутонът за Вход
        Button(
            onClick = {
                isLoading = true
                coroutineScope.launch {
                    try {
                        // ТУК СЕ СЛУЧВА МАГИЯТА: Пращаме данните към Python!
                        val response = RetrofitClient.instance.login(username, password)
                        message = "Успешен вход! Токен: ${response.access_token.take(10)}..."
                    } catch (e: Exception) {
                        message = "Грешка: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Деактивираме бутона, докато чакаме сървъра
        ) {
            Text(if (isLoading) "Зареждане..." else "Вход")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Показваме съобщението от сървъра (или грешката)
        if (message.isNotEmpty()) {
            Text(text = message, color = MaterialTheme.colorScheme.primary)
        }
    }
}

