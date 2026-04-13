package com.viktor.englishapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import com.viktor.englishapp.domain.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // 🟢 Екранът сам си изтегля данните при отваряне
    LaunchedEffect(Unit) {
        val token = tokenManager.getToken()
        if (token != null) {
            try {
                userProfile = RetrofitClient.instance.getMyProfile("Bearer $token")
            } catch (e: Exception) {
                // Грешка при зареждане
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Моят Профил") },
                navigationIcon = {
                    IconButton(onClick = onBack) { // 🟢 ТУК ИЗПОЛЗВАМЕ onBack
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
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // 🟢 ТУК ИЗПОЛЗВАМЕ СКРОЛА
            ) {
                // Хедър със снимка
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(20.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(userProfile?.username ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileInfoSection("Лична информация", listOf(
                        "Имейл" to (userProfile?.email ?: ""),
                        "Роля" to if (userProfile?.role_id == 3) "Учител" else "Ученик"
                    ))

                    Spacer(modifier = Modifier.height(16.dp))

                    ProfileInfoSection("Обучение", listOf(
                        "Текущо ниво" to (userProfile?.english_level ?: "A1"),
                        "Общо точки" to "${userProfile?.total_xp} XP"
                    ))

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = { /* Смяна парола */ }, modifier = Modifier.fillMaxWidth()) {
                        Text("Смяна на парола")
                    }

                    if (userProfile?.role_id == 1) {
                        OutlinedButton(onClick = { /* Верификация */ }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text("Кандидатствай за Учител")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoSection(title: String, items: List<Pair<String, String>>) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            items.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}