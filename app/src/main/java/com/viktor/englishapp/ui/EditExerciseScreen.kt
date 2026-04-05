package com.viktor.englishapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import com.viktor.englishapp.domain.ExerciseContent
import com.viktor.englishapp.domain.ExerciseUpdateRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExerciseScreen(
    exerciseId: Int,
    exerciseTitle: String,
    initialContent: String,
    onBack: () -> Unit,
    onApproved: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val token = tokenManager.getToken()

    val coroutineScope = rememberCoroutineScope()
    val gson = remember { Gson() }

    var title by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var isSpeaking by remember { mutableStateOf(false) }
    val questions = remember { mutableStateListOf<String>() }
    val answers = remember { mutableStateListOf<String>() }

    var parseError by remember { mutableStateOf(false) }
    var rawFallbackContent by remember { mutableStateOf(initialContent) }

    var isSaving by remember { mutableStateOf(false) }
    var isApproving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    // 🟢 РАЗОПАКОВАНЕ И ДЕКОДИРАНЕ НА JSON ПРИ ОТВАРЯНЕ
    LaunchedEffect(initialContent) {
        try {
            var cleanJson = initialContent.trim()

            // 1. АВТОМАТИЧНО ДЕКОДИРАНЕ ОТ BASE64 (Ако навигацията е забравила да го направи)
            if (!cleanJson.startsWith("{") && !cleanJson.startsWith("`")) {
                try {
                    val decodedBytes = android.util.Base64.decode(cleanJson, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                    cleanJson = String(decodedBytes, Charsets.UTF_8).trim()
                } catch (e: Exception) {
                    // Ако не е Base64, просто продължаваме
                }
            }

            // 2. ИЗЧИСТВАНЕ НА МАРКДАУН (Ако AI го е сложил)
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substringAfter("```json").substringBeforeLast("```").trim()
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substringAfter("```").substringBeforeLast("```").trim()
            }

            // Запазваме разкодирания и изчистен текст в случай на авария
            rawFallbackContent = cleanJson

            // 3. ПРЕВРЪЩАНЕ В ОБЕКТ И РАЗДЕЛЯНЕ ПО КУТИЙКИ
            val parsed = gson.fromJson(cleanJson, ExerciseContent::class.java)
            if (parsed != null) {
                title = parsed.title ?: ""
                instructions = parsed.instructions ?: ""
                isSpeaking = parsed.is_speaking

                parsed.content?.let { questions.addAll(it) }
                parsed.correct_answers?.let { answers.addAll(it) }
                parseError = false
            } else {
                parseError = true
            }
        } catch (e: Exception) {
            parseError = true
        }
    }

    fun saveExercise(andApprove: Boolean) {
        if (token == null) return

        if (andApprove) isApproving = true else isSaving = true
        statusMessage = ""

        coroutineScope.launch {
            try {
                val updatedJsonString = if (parseError) {
                    rawFallbackContent
                } else {
                    val updatedObj = ExerciseContent(
                        title = title,
                        instructions = instructions,
                        is_speaking = isSpeaking,
                        content = questions.toList(),
                        correct_answers = answers.toList()
                    )
                    gson.toJson(updatedObj)
                }

                val request = ExerciseUpdateRequest(content_prompt = updatedJsonString)

                RetrofitClient.instance.editExercise(
                    exerciseId = exerciseId,
                    request = request,
                    token = "Bearer $token"
                )

                if (andApprove) {
                    RetrofitClient.instance.approveExercise(
                        exerciseId = exerciseId,
                        token = "Bearer $token"
                    )
                    statusMessage = "Успешно одобрено!"
                    onApproved()
                } else {
                    statusMessage = "Промените са запазени!"
                    isError = false
                }
            } catch (e: Exception) {
                statusMessage = "Грешка: ${e.message}"
                isError = true
            } finally {
                isSaving = false
                isApproving = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редакция: $exerciseTitle") },
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
                .verticalScroll(rememberScrollState())
        ) {
            if (parseError) {
                Text("AI върна неформатиран текст. Моля, редактирайте суровия код:", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rawFallbackContent,
                    onValueChange = { rawFallbackContent = it },
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                )
            } else {
                // --- КРАСИВ ИЗГЛЕД ЗА РЕДАКТИРАНЕ С ОТДЕЛНИ ПОЛЕТА ---
                Text("Основна информация", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Заглавие на упражнението") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Инструкции за ученика") },
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(if (isSpeaking) "Текстове за четене (Speaking)" else "Въпроси и Отговори", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                questions.forEachIndexed { index, qText ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Въпрос ${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = questions[index],
                                onValueChange = { questions[index] = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(if (isSpeaking) "Изречение за произнасяне" else "Въпрос / Изречение") }
                            )

                            if (!isSpeaking && index < answers.size) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = answers[index],
                                    onValueChange = { answers[index] = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Правилен отговор") }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(
                    onClick = { saveExercise(andApprove = false) },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving && !isApproving
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Text("Запази промените")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { saveExercise(andApprove = true) },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving && !isApproving,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isApproving) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else Text("Запази и Одобри")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}