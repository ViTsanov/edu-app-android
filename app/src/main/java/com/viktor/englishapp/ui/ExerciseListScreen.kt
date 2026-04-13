package com.viktor.englishapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import com.viktor.englishapp.domain.StudentPathItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(onBack: () -> Unit, onExerciseClick: (StudentPathItem) -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var exercises by remember { mutableStateOf<List<StudentPathItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val token = tokenManager.getToken()
                if (token != null) {
                    exercises = RetrofitClient.instance.getMyPath("Bearer $token")
                }
            } catch (e: Exception) {
                // Грешка
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Моят учебен път") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(exercises) { exercise ->
                    StudentPathCard(exercise = exercise) {
                        // 🟢 Позволяваме кликане само ако НЕ Е завършено
                        if (exercise.status != "COMPLETED") {
                            onExerciseClick(exercise)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentPathCard(exercise: StudentPathItem, onClick: () -> Unit) {
    // 🟢 Определяме цветовете според статуса
    val containerColor = when (exercise.status) {
        "COMPLETED" -> Color(0xFFF0FDF4) // Много светло зелено
        "RETRY" -> Color(0xFFFFFBEB) // Много светло оранжево
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val statusColor = when (exercise.status) {
        "COMPLETED" -> Color(0xFF16A34A) // Тъмно зелено
        "RETRY" -> Color(0xFFD97706) // Тъмно оранжево
        else -> MaterialTheme.colorScheme.primary
    }

    val icon = when (exercise.status) {
        "COMPLETED" -> Icons.Default.CheckCircle
        "RETRY" -> Icons.Default.Refresh
        else -> Icons.Default.PlayArrow
    }

    val statusText = when (exercise.status) {
        "COMPLETED" -> "Завършено успешно (${exercise.best_score}/100)"
        "RETRY" -> "Опитай пак (Най-добър резултат: ${exercise.best_score}/100)"
        else -> "Ново упражнение"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = exercise.status != "COMPLETED") { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = exercise.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = statusText, color = statusColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (exercise.status == "COMPLETED") statusColor else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}