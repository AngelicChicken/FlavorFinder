package com.example.flavorfinder.di

import android.content.Context
import com.example.flavorfinder.network.repository.MealRepository
import com.example.flavorfinder.network.repository.UserRepository
import com.example.flavorfinder.network.retrofit.ApiConfig
import com.example.flavorfinder.network.retrofit.user.UserApiConfig
import com.example.flavorfinder.view.ui.home.HomeFragment

object Injection {
    fun provideRepository(context: HomeFragment): MealRepository {
        val apiService = ApiConfig.getMealsApiService()
        return MealRepository.getInstance(apiService)
    }

    fun provideUserRepository(context: Context): UserRepository {
        val user = UserApiConfig.getUserApiService()
        return UserRepository.getInstance(user)
    }
}