package com.example.flavorfinder.helper

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.flavorfinder.di.Injection
import com.example.flavorfinder.network.repository.UserRepository
import com.example.flavorfinder.view.signin.SigninViewModel

class UserViewModelFactory(private val repository: UserRepository): ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SigninViewModel::class.java) -> {
                SigninViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
        }
    }

    companion object {
        fun getAuthInstance(context: Context): UserViewModelFactory {
            val userRepository = Injection.provideUserRepository(context)
            return UserViewModelFactory(userRepository)
        }
    }
}
