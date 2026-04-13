package com.viktor.englishapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import com.viktor.englishapp.domain.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onGoToProfile: () -> Unit,
    onGoToExpert: () -> Unit, // За упражнения чакащи одобрение
    onGoToExpertActive: () -> Unit, // 🟢 НОВО: За преглед на активните упражнения
    onGoToExercises: () -> Unit // За ученици (започни учене)
) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EnglishApp") },
                actions = {
                    // 🟢 Бутонът в горния десен ъгъл
                    IconButton(onClick = onGoToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Профил", modifier = Modifier.size(32.dp))
                    }
                }
            )
        }
    )

    { padding ->
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
                // ОБЩА ЧАСТ (Заглавие)
                Text(
                    text = "Здравей, ${userProfile?.username}! 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Имейл: ${userProfile?.email}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 🟢 РАЗДЕЛЯНЕ НА ЛОГИКАТА: ЕКСПЕРТ срещу УЧЕНИК
                if (userProfile?.role_id == 3) {
                    // --- ИЗГЛЕД ЗА ЕКСПЕРТ ---
                    Text(
                        "Експертно табло",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onGoToExpert,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Default.PendingActions, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("ЧАКАЩИ ОДОБРЕНИЕ")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onGoToExpertActive,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("АКТИВНИ УПРАЖНЕНИЯ")
                    }

                } else {
                    // --- ИЗГЛЕД ЗА УЧЕНИК ---
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Точки: ${userProfile?.total_xp} XP 🏆",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { onGoToExercises() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("ЗАПОЧНИ УЧЕНЕ")
                    }
                }

                // ОБЩА ЧАСТ (Изход)
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedButton(onClick = onLogout) {
                    Text("Изход от профила")
                }
            }
        }
    }
}