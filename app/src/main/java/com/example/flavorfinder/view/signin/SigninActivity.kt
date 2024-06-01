package com.example.flavorfinder.view.signin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.example.flavorfinder.R
import com.example.flavorfinder.databinding.ActivitySigninBinding
import com.example.flavorfinder.helper.UserViewModelFactory
import com.example.flavorfinder.network.ResultState
import com.example.flavorfinder.view.MainActivity
import com.example.flavorfinder.view.RegisterActivity

class SigninActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySigninBinding
    private val viewModel by viewModels<SigninViewModel> {
        UserViewModelFactory.getAuthInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setUpTextViewButton()
        setUpButton()
        observeViewModel()
    }

    private fun setUpTextViewButton() {
        binding.GoToRegisterPageTextButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setUpButton() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            Log.d("Login", "Email: $email, Password: $password")

            viewModel.login(email, password)
        }
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(this, Observer { result ->
            when (result) {
                is ResultState.Success -> {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                }
                is ResultState.Error -> {
                    Toast.makeText(this, "Login failed: ${result.error}", Toast.LENGTH_SHORT).show()
                }
                is ResultState.Loading -> {
                    // Show loading indicator if needed
                }
            }
        })
    }
}
