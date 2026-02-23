package com.viktor.englishapp.ui // 1. ПРОВЕРИ ПАКЕТА!

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.viktor.englishapp.data.RetrofitClient // 2. ПРОВЕРИ ПАКЕТА!
import com.viktor.englishapp.data.TokenManager // 3. ПРОВЕРИ ПАКЕТА!
import kotlinx.coroutines.launch

// ЕТО ГО ЛИПСВАЩИЯ ПАРАМЕТЪР:
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
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
            value = username,
            onValueChange = { username = it },
            label = { Text("Имейл") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Парола") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                isLoading = true
                coroutineScope.launch {
                    try {
                        // Пращаме заявка към Python сървъра
                        val response = RetrofitClient.instance.login(username, password)
                        // Запазваме токена в портфейла
                        tokenManager.saveToken(response.access_token)
                        // КАЗВАМЕ НА НАВИГАТОРА ДА СМЕНИ ЕКРАНА!
                        onLoginSuccess()
                    } catch (e: Exception) {
                        message = "Грешка: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Зареждане..." else "Вход")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (message.isNotEmpty()) {
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }
    }
}