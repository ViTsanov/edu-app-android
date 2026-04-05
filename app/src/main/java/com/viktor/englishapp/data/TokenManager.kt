package com.viktor.englishapp.data // ПРОВЕРИ ИМЕТО НА ПАКЕТА!

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class TokenManager(context: Context) {
    // Създаваме скрит файл в телефона, наречен "app_prefs", до който само нашето приложение има достъп
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Функция за ПРИБИРАНЕ на токена в портфейла
    fun saveToken(token: String) {
        prefs.edit().putString("jwt_token", token).apply()
    }

    // Функция за ВАДЕНЕ на токена (връща null, ако портфейлът е празен)
    fun getToken(): String? {
        return prefs.getString("jwt_token", null)
    }

    // Функция за ИЗХВЪРЛЯНЕ на токена (когато потребителят натисне "Изход/Logout")
    fun clearToken() {
        prefs.edit { remove("jwt_token") }
    }
}