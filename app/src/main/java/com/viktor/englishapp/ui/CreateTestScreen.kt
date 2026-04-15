package com.viktor.englishapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import com.viktor.englishapp.domain.ExerciseContent
import com.viktor.englishapp.domain.ExerciseResponse
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────
// Data model for an exercise with selectable individual tasks
// ─────────────────────────────────────────────────────────────────

data class SelectableExercise(
    val id: Int,
    val title: String,
    val contentPrompt: String,
    val source: String,           // "approved" or "teacher"
    val parsed: ExerciseContent?,
    // Which question indices the teacher has selected (empty = none)
    val selectedQuestions: MutableSet<Int> = mutableSetOf()
)

// ─────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────

class CreateTestViewModel : ViewModel() {

    // Step 1: Test details
    var testTitle by mutableStateOf("")
    var timeLimitMinutes by mutableStateOf(30)
    var selectedClassroomId by mutableStateOf<Int?>(null)
    var selectedClassroomName by mutableStateOf("Изберете клас")

    // Step 2: Exercise browsing
    var approvedExercises = mutableStateListOf<SelectableExercise>()
    var teacherExercises = mutableStateListOf<SelectableExercise>()
    var isLoadingExercises by mutableStateOf(false)

    // Added exercises (exercises selected for this test)
    var addedExercises = mutableStateListOf<SelectableExercise>()

    // Classroom list
    var classrooms = mutableStateListOf<Map<String, Any>>()

    // UI state
    var currentStep by mutableStateOf(1)   // 1 = details, 2 = pick exercises, 3 = review
    var isSaving by mutableStateOf(false)
    var errorMessage by mutableStateOf("")
    var successMessage by mutableStateOf("")

    private val gson = Gson()

    fun loadData(tokenManager: TokenManager) {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            isLoadingExercises = true
            try {
                // Load classrooms
                val classroomResponse = RetrofitClient.instance.getTeacherClassrooms("Bearer $token")
                classrooms.clear()
                classroomResponse.forEach { classroom ->
                    classrooms.add(mapOf(
                        "id" to (classroom.id),
                        "name" to classroom.name
                    ))
                }

                // Load approved (AI) exercises
                val approved = RetrofitClient.instance.getExercises("Bearer $token")
                approvedExercises.clear()
                approved.forEach { ex ->
                    approvedExercises.add(
                        SelectableExercise(
                            id = ex.id,
                            title = ex.title,
                            contentPrompt = ex.content_prompt,
                            source = "approved",
                            parsed = parseContent(ex.content_prompt)
                        )
                    )
                }

                // Load teacher's own exercises
                val myExercises = RetrofitClient.instance.getTeacherExercises("Bearer $token")
                teacherExercises.clear()
                myExercises.forEach { map ->
                    val id = (map["id"] as? Double)?.toInt() ?: 0
                    val title = map["title"] as? String ?: ""
                    // Fetch content_prompt separately if needed; for now use title
                    teacherExercises.add(
                        SelectableExercise(
                            id = id,
                            title = title,
                            contentPrompt = "",
                            source = "teacher",
                            parsed = null
                        )
                    )
                }
            } catch (e: Exception) {
                errorMessage = "Грешка при зареждане: ${e.message}"
            } finally {
                isLoadingExercises = false
            }
        }
    }

    private fun parseContent(json: String): ExerciseContent? {
        return try {
            var clean = json.trim()
            if (clean.startsWith("```json")) clean = clean.substringAfter("```json").substringBeforeLast("```").trim()
            gson.fromJson(clean, ExerciseContent::class.java)
        } catch (_: Exception) { null }
    }

    fun toggleQuestion(exercise: SelectableExercise, questionIndex: Int) {
        if (exercise.selectedQuestions.contains(questionIndex)) {
            exercise.selectedQuestions.remove(questionIndex)
        } else {
            exercise.selectedQuestions.add(questionIndex)
        }
    }

    fun addExerciseToTest(exercise: SelectableExercise) {
        if (exercise.selectedQuestions.isEmpty()) {
            // Select all questions by default
            val count = exercise.parsed?.content?.size ?: 1
            exercise.selectedQuestions.addAll((0 until count))
        }
        if (addedExercises.none { it.id == exercise.id && it.source == exercise.source }) {
            addedExercises.add(exercise)
        }
    }

    fun removeFromTest(exercise: SelectableExercise) {
        addedExercises.removeAll { it.id == exercise.id && it.source == exercise.source }
    }

    fun saveTest(tokenManager: TokenManager, onSuccess: () -> Unit) {
        if (testTitle.isBlank()) { errorMessage = "Въведете заглавие на теста."; return }
        if (selectedClassroomId == null) { errorMessage = "Изберете клас."; return }
        if (addedExercises.isEmpty()) { errorMessage = "Добавете поне едно упражнение."; return }

        isSaving = true
        errorMessage = ""

        viewModelScope.launch {
            try {
                val token = tokenManager.getToken() ?: throw Exception("Липсва токен.")

                // 1. Create the test
                val testResponse = RetrofitClient.instance.createTest(
                    token = "Bearer $token",
                    body = mapOf(
                        "title" to testTitle,
                        "classroom_id" to (selectedClassroomId ?: 0),
                        "time_limit_minutes" to timeLimitMinutes
                    )
                )
                val testId = (testResponse["id"] as? Double)?.toInt()
                    ?: throw Exception("Не можа да се създаде тест.")

                // 2. Add each selected exercise to the test
                addedExercises.forEachIndexed { orderIndex, ex ->
                    if (ex.selectedQuestions.isNotEmpty() &&
                        ex.selectedQuestions.size < (ex.parsed?.content?.size ?: 1)
                    ) {
                        // Partial selection — save selected questions as a new TeacherExercise
                        val selectedContent = ex.parsed?.content
                            ?.filterIndexed { i, _ -> ex.selectedQuestions.contains(i) }
                            ?: emptyList()
                        val selectedAnswers = ex.parsed?.correct_answers
                            ?.filterIndexed { i, _ -> ex.selectedQuestions.contains(i) }
                            ?: emptyList()

                        val partialContent = ExerciseContent(
                            title = ex.title,
                            instructions = ex.parsed?.instructions ?: "",
                            is_speaking = ex.parsed?.is_speaking ?: false,
                            content = selectedContent,
                            correct_answers = selectedAnswers
                        )
                        val savedEx = RetrofitClient.instance.saveTeacherExercise(
                            token = "Bearer $token",
                            body = mapOf(
                                "title" to "${ex.title} (избрани)",
                                "content_prompt" to gson.toJson(partialContent)
                            )
                        )
                        val savedId = (savedEx["id"] as? Double)?.toInt() ?: return@forEachIndexed
                        RetrofitClient.instance.addExerciseToTest(
                            testId = testId,
                            token = "Bearer $token",
                            body = mapOf(
                                "teacher_exercise_id" to savedId,
                                "order_index" to orderIndex
                            )
                        )
                    } else {
                        // Full exercise
                        val body: Map<String, Any> = if (ex.source == "approved") {
                            mapOf("exercise_id" to ex.id, "order_index" to orderIndex)
                        } else {
                            mapOf("teacher_exercise_id" to ex.id, "order_index" to orderIndex)
                        }
                        RetrofitClient.instance.addExerciseToTest(
                            testId = testId,
                            token = "Bearer $token",
                            body = body
                        )
                    }
                }

                successMessage = "Тестът е създаден успешно!"
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Грешка: ${e.message}"
            } finally {
                isSaving = false
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Main Screen — 3-step wizard
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTestScreen(
    onBack: () -> Unit,
    viewModel: CreateTestViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(Unit) { viewModel.loadData(tokenManager) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Нов тест — Стъпка ${viewModel.currentStep}/3") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.currentStep > 1) viewModel.currentStep--
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Step progress indicator
            LinearProgressIndicator(
                progress = { viewModel.currentStep / 3f },
                modifier = Modifier.fillMaxWidth()
            )

            when (viewModel.currentStep) {
                1 -> StepOneDetails(
                    viewModel = viewModel,
                    onNext = { viewModel.currentStep = 2 }
                )
                2 -> StepTwoPickExercises(
                    viewModel = viewModel,
                    onNext = { viewModel.currentStep = 3 }
                )
                3 -> StepThreeReview(
                    viewModel = viewModel,
                    tokenManager = tokenManager,
                    onSuccess = onBack
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Step 1: Test title, time limit, classroom
// ─────────────────────────────────────────────────────────────────

@Composable
private fun StepOneDetails(
    viewModel: CreateTestViewModel,
    onNext: () -> Unit
) {
    var showClassroomMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Детайли на теста", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.testTitle,
            onValueChange = { viewModel.testTitle = it },
            label = { Text("Заглавие на теста") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        // Time limit slider
        Text(
            "Времево ограничение: ${viewModel.timeLimitMinutes} минути (0 = без ограничение)",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = viewModel.timeLimitMinutes.toFloat(),
            onValueChange = { viewModel.timeLimitMinutes = it.toInt() },
            valueRange = 0f..120f,
            steps = 23,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        // Classroom picker
        Text("Клас:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Box {
            OutlinedButton(
                onClick = { showClassroomMenu = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.School, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(viewModel.selectedClassroomName)
            }
            DropdownMenu(
                expanded = showClassroomMenu,
                onDismissRequest = { showClassroomMenu = false }
            ) {
                viewModel.classrooms.forEach { classroom ->
                    val id = (classroom["id"] as? Int) ?: 0
                    val name = classroom["name"] as? String ?: ""
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            viewModel.selectedClassroomId = id
                            viewModel.selectedClassroomName = name
                            showClassroomMenu = false
                        }
                    )
                }
                if (viewModel.classrooms.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Нямате класове. Създайте клас първо.") },
                        onClick = { showClassroomMenu = false }
                    )
                }
            }
        }

        if (viewModel.errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(viewModel.errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (viewModel.testTitle.isBlank()) {
                    viewModel.errorMessage = "Въведете заглавие."
                } else if (viewModel.selectedClassroomId == null) {
                    viewModel.errorMessage = "Изберете клас."
                } else {
                    viewModel.errorMessage = ""
                    onNext()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("НАПРЕД — Избери упражнения →")
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Step 2: Browse and pick exercises with individual task selection
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepTwoPickExercises(
    viewModel: CreateTestViewModel,
    onNext: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("AI Упражнения", "Мои упражнения")

    Column(modifier = Modifier.fillMaxSize()) {

        // Added count chip
        if (viewModel.addedExercises.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Добавени: ${viewModel.addedExercises.size} упражнения",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        if (viewModel.isLoadingExercises) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val exercises = if (selectedTab == 0) viewModel.approvedExercises
            else viewModel.teacherExercises

            if (exercises.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (selectedTab == 0) "Няма AI упражнения."
                        else "Нямате собствени упражнения. Създайте от таблото.",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(exercises) { exercise ->
                        SelectableExerciseCard(
                            exercise = exercise,
                            isAdded = viewModel.addedExercises.any {
                                it.id == exercise.id && it.source == exercise.source
                            },
                            onToggleQuestion = { index ->
                                viewModel.toggleQuestion(exercise, index)
                            },
                            onAdd = { viewModel.addExerciseToTest(exercise) },
                            onRemove = { viewModel.removeFromTest(exercise) }
                        )
                    }
                }
            }
        }

        // Next button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            enabled = viewModel.addedExercises.isNotEmpty()
        ) {
            Text("НАПРЕД — Преглед на теста →")
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Selectable exercise card with individual question checkboxes
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SelectableExerciseCard(
    exercise: SelectableExercise,
    isAdded: Boolean,
    onToggleQuestion: (Int) -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Track selections locally to trigger recomposition
    var selectionVersion by remember { mutableStateOf(0) }

    val borderColor = if (isAdded) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isAdded) 2.dp else 0.5.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            ),
        elevation = CardDefaults.cardElevation(if (isAdded) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(exercise.title, fontWeight = FontWeight.Bold)
                    if (exercise.source == "approved") {
                        Text(
                            "AI генерирано · ${exercise.parsed?.content?.size ?: 0} задачи",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                if (isAdded) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Expand to show tasks (only for AI exercises with parsed content)
            if (exercise.parsed?.content != null && exercise.parsed.content.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (expanded) "▲ Скрий задачите" else "▼ Покажи задачите (${exercise.parsed.content.size})")
                }

                if (expanded) {
                    Spacer(Modifier.height(4.dp))
                    // Show each task with a checkbox
                    exercise.parsed.content.forEachIndexed { index, question ->
                        val isSelected = exercise.selectedQuestions.contains(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onToggleQuestion(index)
                                    selectionVersion++ // force recompose
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    onToggleQuestion(index)
                                    selectionVersion++
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "${index + 1}. $question",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (!exercise.parsed.is_speaking &&
                                    index < (exercise.parsed.correct_answers?.size ?: 0)
                                ) {
                                    Text(
                                        "→ ${exercise.parsed.correct_answers?.get(index)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Add / Remove button
            if (isAdded) {
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Премахни от теста")
                }
            } else {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    val selected = exercise.selectedQuestions.size
                    val total = exercise.parsed?.content?.size ?: 0
                    Text(
                        if (selected in 1 until total) "Добави ($selected/$total задачи)"
                        else "Добави в теста"
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Step 3: Review and save
// ─────────────────────────────────────────────────────────────────

@Composable
private fun StepThreeReview(
    viewModel: CreateTestViewModel,
    tokenManager: TokenManager,
    onSuccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Преглед на теста",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        // Summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ReviewRow("Заглавие", viewModel.testTitle)
                ReviewRow("Клас", viewModel.selectedClassroomName)
                ReviewRow(
                    "Времево ограничение",
                    if (viewModel.timeLimitMinutes == 0) "Без ограничение"
                    else "${viewModel.timeLimitMinutes} мин."
                )
                ReviewRow("Упражнения", "${viewModel.addedExercises.size} броя")
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Упражнения в теста:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        viewModel.addedExercises.forEachIndexed { index, ex ->
            val selectedCount = ex.selectedQuestions.size
            val totalCount = ex.parsed?.content?.size ?: 1
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${index + 1}.",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ex.title, fontWeight = FontWeight.Medium)
                        Text(
                            if (selectedCount in 1 until totalCount)
                                "$selectedCount от $totalCount задачи"
                            else "$totalCount задачи",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = { viewModel.removeFromTest(ex) }) {
                        Icon(
                            Icons.Default.Delete, "Премахни",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (viewModel.errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(viewModel.errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.saveTest(tokenManager = tokenManager, onSuccess = onSuccess)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !viewModel.isSaving
        ) {
            if (viewModel.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("ЗАПАЗИ ТЕСТА", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Text(
            value,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}