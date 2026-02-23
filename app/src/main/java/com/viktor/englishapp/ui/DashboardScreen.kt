package com.viktor.englishapp.ui // ПРОВЕРИ ПАКЕТА!

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.viktor.englishapp.data.RetrofitClient // ПРОВЕРИ ПАКЕТА!
import com.viktor.englishapp.data.TokenManager // ПРОВЕРИ ПАКЕТА!
import com.viktor.englishapp.data.UserProfile // ПРОВЕРИ ПАКЕТА!

@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    // Тук пазим данните, след като ги изтеглим от сървъра
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // ТОВА Е МАГИЯТА: Изпълнява се автоматично при отваряне на екрана
    LaunchedEffect(Unit) {
        val token = tokenManager.getToken()
        if (token != null) {
            try {
                // ВАЖНО: Сървърът очаква токенът да започва с думата "Bearer "
                val profile = RetrofitClient.instance.getMyProfile("Bearer $token")
                userProfile = profile
            } catch (e: Exception) {
                errorMessage = "Грешка при връзката: ${e.message}"
            } finally {
                isLoading = false
            }
        } else {
            errorMessage = "Липсва токен за достъп."
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Логика за показване на екрана в зависимост от състоянието:
        if (isLoading) {
            CircularProgressIndicator() // Въртящо се кръгче
            Spacer(modifier = Modifier.height(16.dp))
            Text("Зареждане на профила...")

        } else if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onLogout) { Text("Обратно към Вход") }

        } else {
            // УСПЕХ! Имаме данните и показваме истинското име!
            Text(
                text = "Здравей, ${userProfile?.full_name}! 👋",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Имейл: ${userProfile?.email}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Роля: ${userProfile?.role}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = onLogout) {
                Text("Изход")
            }
        }
    }
}