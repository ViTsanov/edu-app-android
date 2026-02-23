package com.viktor.englishapp.data

import com.viktor.englishapp.domain.LoginResponse
import com.viktor.englishapp.domain.UserResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    // 1. Маршрутът за ВХОД (Login)
    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("username") username: String, // FastAPI очаква 'username', дори да пращаме имейл
        @Field("password") password: String
    ): LoginResponse

    // 2. Маршрутът за ПРОФИЛ (Вземане на данните на логнатия потребител)
    @GET("users/me")
    suspend fun getMyProfile(
        @Header("Authorization") token: String // Тук слагаме пропуска (токенът)
    ): UserResponse
}