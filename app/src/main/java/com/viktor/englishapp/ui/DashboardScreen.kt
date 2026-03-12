package com.viktor.englishapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
// 1. ВАЖНА ПРОМЯНА: Вече импортираме UserProfile от новия пакет 'domain'
import com.viktor.englishapp.domain.UserProfile

@Composable
fun DashboardScreen(onLogout: () -> Unit, onGoToExpert: () -> Unit, onGoToExercises: () -> Unit) {
    val context = LocalContext.current

    val tokenManager = remember { TokenManager(context) }

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val token = tokenManager.getToken()
        if (token != null) {
            try {
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
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Зареждане на профила...")

        } else if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onLogout) { Text("Обратно към Вход") }

        } else {
            // 2. ВАЖНА ПРОМЯНА: Сменихме 'full_name' с 'username' според новия модел
            Text(
                text = "Здравей, ${userProfile?.username}! 👋",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Имейл: ${userProfile?.email}",
                style = MaterialTheme.typography.bodyLarge
            )

            // 3. НОВО: Красива визуализация на точките (XP)!
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Точки: ${userProfile?.total_xp} XP 🏆",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onGoToExercises() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("ЗАПОЧНИ УЧЕНЕ")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onLogout) {
                Text("Изход")
            }

        }

        // 4. ВАЖНА ПРОМЯНА: Вече проверяваме ролята по ID, а не по текст "expert".
        // ВНИМАНИЕ: Сложил съм числото 3 като пример. Ако в твоята база данни Експертът
        // има ID = 2 или 4, просто промени цифрата тук!
        if (userProfile?.role_id == 3) {
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onGoToExpert() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("⭐ Експертен панел (AI)")
            }
        }
    }
}