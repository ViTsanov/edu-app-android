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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viktor.englishapp.data.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertScreen(
    onBack: () -> Unit,
    onNavigateToPending: () -> Unit,
    viewModel: ExpertViewModel = viewModel() // ИНЖЕКТИРАМЕ МОЗЪКА
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scrollState = rememberScrollState()

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

            // --- ИЗБОР НА МОДУЛ ---
            Text("Изберете Модул:", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                viewModel.modulesList.forEach { mod ->
                    FilterChip(
                        selected = viewModel.selectedModule == mod, // Сравняваме целия обект
                        onClick = { viewModel.selectedModule = mod },
                        label = { Text(mod.name) } // Показваме само името
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ИЗБОР НА НИВО ---
            Text("Изберете Ниво:", style = MaterialTheme.typography.titleMedium)
            Column(modifier = Modifier.fillMaxWidth()) {
                // Разделяме ги на два реда по 3
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    viewModel.levelsList.take(3).forEach { lv ->
                        FilterChip(
                            selected = viewModel.selectedLevel == lv,
                            onClick = { viewModel.selectedLevel = lv },
                            label = { Text(lv.name) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    viewModel.levelsList.drop(3).forEach { lv ->
                        FilterChip(
                            selected = viewModel.selectedLevel == lv,
                            onClick = { viewModel.selectedLevel = lv },
                            label = { Text(lv.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- БУТОН ЗА ГЕНЕРИРАНЕ ---
            if (viewModel.isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("AI генерира въпроси... (може да отнеме 10-15 сек)")
            } else {
                Button(
                    onClick = {
                        val token = tokenManager.getToken()
                        if (token != null) {
                            viewModel.generateExercise(token)
                        } else {
                            viewModel.statusMessage = "Грешка: Липсва токен за достъп."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("ГЕНЕРИРАЙ С OPENAI")
                }
            }

            // --- СЪОБЩЕНИЕ ЗА СТАТУС ---
            if (viewModel.statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(viewModel.statusMessage, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider() // Слагаме една разделителна линия за красота
            Spacer(modifier = Modifier.height(16.dp))

            // 🟢 НОВИЯТ БУТОН ЗА ПРЕГЛЕД НА ЧАКАЩИ
            Button(
                onClick = onNavigateToPending,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("ПРЕГЛЕД НА ЧАКАЩИ ЗА ОДОБРЕНИЕ")
            }
        }
    }
}