package com.viktor.englishapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope() // Дефинирано като 'scope'
    val scrollState = rememberScrollState()

    var selectedModule by remember { mutableStateOf("Grammar") }
    var selectedLevel by remember { mutableStateOf("A2") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") } // Дефинирано като 'statusMessage'

    val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
    val modules = listOf("Grammar", "Vocabulary", "Reading")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Генератор") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Настройки на упражнението", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(24.dp))

            // ИЗБОР НА МОДУЛ
            Text("Изберете Модул:", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                modules.forEach { mod ->
                    FilterChip(
                        selected = selectedModule == mod,
                        onClick = { selectedModule = mod },
                        label = { Text(mod) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ИЗБОР НА НИВО
            Text("Изберете Ниво:", style = MaterialTheme.typography.titleMedium)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    levels.take(3).forEach { lv ->
                        FilterChip(selected = selectedLevel == lv, onClick = { selectedLevel = lv }, label = { Text(lv) })
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    levels.drop(3).forEach { lv ->
                        FilterChip(selected = selectedLevel == lv, onClick = { selectedLevel = lv }, label = { Text(lv) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("AI генерира въпроси... (може да отнеме 10-15 сек)")
            } else {
                Button(
                    onClick = {
                        isLoading = true
                        statusMessage = ""
                        scope.launch { // КОРИГИРАНО: Използваме 'scope'
                            try {
                                val token = tokenManager.getToken()

                                val response = RetrofitClient.instance.generateExercise(
                                    moduleId = 1,
                                    levelId = 1,
                                    token = "Bearer $token"
                                )

                                statusMessage = response.message // КОРИГИРАНО: Използваме 'statusMessage'
                            } catch (e: Exception) {
                                statusMessage = "Грешка: ${e.message}" // КОРИГИРАНО: Използваме 'statusMessage'
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("ГЕНЕРИРАЙ С OPENAI")
                }
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(statusMessage, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}