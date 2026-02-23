package com.viktor.englishapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.viktor.englishapp.data.ExerciseContent

@Composable
fun SolveExerciseScreen(exerciseJson: String, onBack: () -> Unit) {
    // Разопаковаме JSON текста в истински обекти
    val exercise = remember { Gson().fromJson(exerciseJson, ExerciseContent::class.java) }
    val userAnswers = remember { mutableStateListOf(*Array(exercise.content.size) { "" }) }
    var showResults by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(text = exercise.title, style = MaterialTheme.typography.headlineSmall)
            Text(text = exercise.instructions, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(24.dp))

            exercise.content.forEachIndexed { index, question ->
                Text(text = "${index + 1}. ${question.original}", style = MaterialTheme.typography.titleMedium)
                Text(text = "Задача: ${question.task}", style = MaterialTheme.typography.bodySmall)

                OutlinedTextField(
                    value = userAnswers[index],
                    onValueChange = { userAnswers[index] = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Твоят отговор") },
                    enabled = !showResults
                )

                if (showResults) {
                    Text(
                        text = "Правилен отговор: ${exercise.correct_answers[index]}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = { if (showResults) onBack() else showResults = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showResults) "ЗАТВОРИ" else "ПРОВЕРИ ОТГОВОРИТЕ")
            }
        }
    }
}