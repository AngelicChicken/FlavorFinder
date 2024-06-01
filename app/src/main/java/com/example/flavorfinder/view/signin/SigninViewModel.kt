package com.example.flavorfinder.view.signin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flavorfinder.network.ResultState
import com.example.flavorfinder.network.repository.UserRepository
import com.example.flavorfinder.network.response.LoginResponse
import kotlinx.coroutines.launch

class SigninViewModel(private val repository: UserRepository) : ViewModel() {

    private val _loginResult = MutableLiveData<ResultState<LoginResponse>>()
    val loginResult: LiveData<ResultState<LoginResponse>> get() = _loginResult

    fun login(email: String, password: String) {
        _loginResult.postValue(ResultState.Loading)
        viewModelScope.launch {
            try {
                val response = repository.login(email, password)
                _loginResult.postValue(ResultState.Success(response))
            } catch (e: Exception) {
                _loginResult.postValue(ResultState.Error(e.message))
            }
        }
    }
}
