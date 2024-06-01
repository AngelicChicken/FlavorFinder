package com.example.flavorfinder.network.repository

import com.example.flavorfinder.network.response.LoginResponse
import com.example.flavorfinder.network.response.RegisterResponse
import com.example.flavorfinder.network.retrofit.user.UsersApiService

class UserRepository private constructor(
    private val apiService: UsersApiService,
){
    suspend fun register(email: String, password: String): RegisterResponse {
        return apiService.register(email, password)
    }

    suspend fun login(email: String, password: String): LoginResponse {
        return apiService.login(email, password)
    }

    companion object {
        fun getInstance(
            apiService: UsersApiService,
        ) = UserRepository(apiService)
    }
}
