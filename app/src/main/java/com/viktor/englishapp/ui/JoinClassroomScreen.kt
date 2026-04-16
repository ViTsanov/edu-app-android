package com.viktor.englishapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.alpha

// ─────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────

class JoinClassroomViewModel : ViewModel() {

    var accessCode by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf("")
    var successMessage by mutableStateOf("")

    fun onCodeChange(new: String) {
        // Only uppercase letters and digits, max 6 chars
        accessCode = new.uppercase().filter { it.isLetterOrDigit() }.take(6)
        errorMessage = ""
    }

    fun joinClassroom(tokenManager: TokenManager, onSuccess: (String) -> Unit) {
        if (accessCode.length < 4) {
            errorMessage = "Въведи поне 4 символа."
            return
        }
        isLoading = true
        errorMessage = ""
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken() ?: throw Exception("Липсва токен.")
                val response = RetrofitClient.instance.joinClassroom(
                    token = "Bearer $token",
                    body = mapOf("access_code" to accessCode)
                )
                val message = response["message"] as? String ?: "Успешно!"
                successMessage = message
                onSuccess(message)
            } catch (e: Exception) {
                val msg = e.message ?: ""
                errorMessage = when {
                    msg.contains("404") -> "Невалиден код. Провери отново."
                    msg.contains("400") -> "Вече си в този клас."
                    else -> "Грешка: $msg"
                }
            } finally {
                isLoading = false
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Screen — full-screen version (for when navigated to directly)
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinClassroomScreen(
    onBack: () -> Unit,
    onJoined: () -> Unit,
    viewModel: JoinClassroomViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Присъедини се към клас") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Icon
            Surface(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🏫", fontSize = 36.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Въведи кода на класа",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Поискай 6-символния код от своя учител",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Code input — large and centred
            OutlinedTextField(
                value = viewModel.accessCode,
                onValueChange = { viewModel.onCodeChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center,
                    letterSpacing = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                placeholder = {
                    Text(
                        "------",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            letterSpacing = 10.sp
                        )
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.Characters
                ),
                singleLine = true,
                isError = viewModel.errorMessage.isNotEmpty(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Character count dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    val filled = index < viewModel.accessCode.length
                    val animSize by animateDpAsState(
                        targetValue = if (filled) 8.dp else 6.dp,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy),
                        label = "dot_$index"
                    )
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(animSize),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = if (filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    ) {}
                }
            }

            // Error
            val errorAlpha by animateFloatAsState(
                targetValue = if (viewModel.errorMessage.isNotEmpty()) 1f else 0f,
                animationSpec = tween(200),
                label = "error_alpha"
            )
            if (viewModel.errorMessage.isNotEmpty() || errorAlpha > 0f) {
                Text(
                    viewModel.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .alpha(errorAlpha),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            // Join button
            Button(
                onClick = {
                    viewModel.joinClassroom(tokenManager) { _ ->
                        onJoined()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = viewModel.accessCode.length >= 4 && !viewModel.isLoading
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("ВЛЕЗ В КЛАСА", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}