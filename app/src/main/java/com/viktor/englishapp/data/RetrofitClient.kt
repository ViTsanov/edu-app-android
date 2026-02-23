package com.viktor.englishapp.data // Провери името на пакета!

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Ето го нашия магически адрес към FastAPI сървъра!
    // (Ако тестваш на реален телефон с кабел, тук ще трябва IP адреса на компютъра ти, напр. 192.168.1.5)
    private const val BASE_URL = "http://10.0.2.2:8000/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            // Добавяме "Преводача" (Gson), за да превръща JSON в Kotlin обекти
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}