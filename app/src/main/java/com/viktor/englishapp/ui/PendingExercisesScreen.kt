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
    onEditExercise: (Int, String, String) -> Unit,
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
                            },
                            onEdit = {
                                onEditExercise(exercise.id, exercise.title, exercise.content_prompt)
                            },
                            // 🔴 НОВО: Добавена е логиката за натискане на бутона "Откажи"
                            onReject = {
                                if (token != null) viewModel.rejectExercise(token, exercise.id)
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
fun PendingExerciseCard(
    exercise: ExerciseResponse,
    onApprove: () -> Unit,
    onEdit: () -> Unit,
    onReject: () -> Unit // 🔴 НОВО: Приемаме функция за изтриване/отказ
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = exercise.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Статус: ${exercise.status}", color = MaterialTheme.colorScheme.primary)

            Spacer(modifier = Modifier.height(16.dp))

            // 🔴 НОВО: Вече имаме 3 бутона, разделени поравно
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Бутон за отказ (червен)
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Откажи")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 2. Бутон за редакция
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Редакция")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 3. Бутон за одобряване
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