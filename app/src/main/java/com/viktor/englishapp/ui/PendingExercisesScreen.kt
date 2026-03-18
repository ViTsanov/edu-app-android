package com.viktor.englishapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viktor.englishapp.data.TokenManager
import com.viktor.englishapp.domain.ExerciseResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingExercisesScreen(
    onBack: () -> Unit,
    viewModel: PendingExercisesViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val token = tokenManager.getToken()

    // Автоматично зареждаме данните при отваряне на екрана
    LaunchedEffect(Unit) {
        if (token != null) {
            viewModel.loadPendingExercises(token)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("За одобрение") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.errorMessage.isNotEmpty()) {
                Text(
                    text = viewModel.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else if (viewModel.pendingExercises.isEmpty()) {
                Text(
                    text = "Няма упражнения, чакащи одобрение.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Използваме LazyColumn за оптимизирано скролиране на списък
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(viewModel.pendingExercises) { exercise ->
                        PendingExerciseCard(
                            exercise = exercise,
                            onApprove = {
                                if (token != null) viewModel.approveExercise(token, exercise.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

// Отделен компонент (Card) за едно упражнение
@Composable
fun PendingExerciseCard(exercise: ExerciseResponse, onApprove: () -> Unit) {
    // 🟢 НОВО: Пазим състояние дали картата е разгъната или не
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = exercise.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Статус: ${exercise.status}", color = MaterialTheme.colorScheme.primary)

            // 🟢 НОВО: Ако е разгънато, показваме съдържанието на упражнението
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Опитваме се да разопаковаме JSON-а
                val parsedExercise = try {
                    var cleanJson = exercise.content_prompt.trim()
                    if (cleanJson.startsWith("```json")) {
                        cleanJson = cleanJson.substringAfter("```json").substringBeforeLast("```").trim()
                    } else if (cleanJson.startsWith("```")) {
                        cleanJson = cleanJson.substringAfter("```").substringBeforeLast("```").trim()
                    }
                    com.google.gson.Gson().fromJson(cleanJson, com.viktor.englishapp.domain.ExerciseContent::class.java)
                } catch (e: Exception) { null }

                if (parsedExercise != null) {
                    Text(text = "Инструкции:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Text(text = parsedExercise.instructions, style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Въпроси:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    parsedExercise.content.forEachIndexed { idx, qText ->
                        Text(text = "${idx + 1}. $qText", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    // В случай, че AI е върнал много странен формат, показваме го суров, за да видим къде е грешката
                    Text(text = exercise.content_prompt, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🟢 НОВО: Слагаме два бутона един до друг (Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Бутон за преглед
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (expanded) "Скрий съдържанието" else "Преглед")
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Бутон за одобряване
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Одобри")
                }
            }
        }
    }
}

