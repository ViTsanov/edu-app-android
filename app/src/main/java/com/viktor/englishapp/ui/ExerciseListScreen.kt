package com.viktor.englishapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
// ВАЖНА ПРОМЯНА: Вече импортираме от domain пакета!
import com.viktor.englishapp.domain.ExerciseResponse
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import kotlinx.coroutines.launch // Нужно за корутините

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(onBack: () -> Unit, onExerciseClick: (ExerciseResponse) -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var exercises by remember { mutableStateOf<List<ExerciseResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope() // За извикване на suspend функции

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val token = tokenManager.getToken()
                exercises = RetrofitClient.instance.getExercises("Bearer $token")
            } catch (e: Exception) {
                // Обработка на грешка
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Налични уроци") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(exercises) { exercise ->
                    ExerciseCard(exercise = exercise) { onExerciseClick(exercise) }
                }
            }
        }
    }
}

@Composable
fun ExerciseCard(exercise: ExerciseResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = exercise.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    // ВАЖНА ПРОМЯНА: В базата вече пазим статуса, а не нивото (сменихме cefr_level със status)
                    text = "Статус: ${exercise.status}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}