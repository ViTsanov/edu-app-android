package com.viktor.englishapp.data 

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // private const val BASE_URL = "http://10.0.2.2:8000/"        // Emulator
    //private const val BASE_URL = "http://192.168.0.103:8000/"        // WiFi
    private const val BASE_URL = "http://172.20.10.3:8000" // uvicorn main:app --reload --host 0.0.0.0 --port 8000

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}
