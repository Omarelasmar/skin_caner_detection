package com.example.skin_caner_detection.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skin_caner_detection.data.User
import com.example.skin_caner_detection.util.NetworkResult
import com.example.skin_caner_detection.util.RegisterValidation
import com.example.skin_caner_detection.util.validateEmail
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val sharedPref: SharedPreferences
):ViewModel() {

    private val _login = MutableSharedFlow<NetworkResult<String>>()
    val login = _login.asSharedFlow()
    private val _resetPassword = MutableStateFlow<NetworkResult<User>>(NetworkResult.UnSpecified())
    val resetPassword = _resetPassword.asStateFlow()

    private val _navigateState = MutableStateFlow(0)
    val navigateState = _navigateState.asStateFlow()

    companion object {
        const val MAIN_ACTIVITY = 23
    }

    init {
        val user = firebaseAuth.currentUser
        if(user != null){
            viewModelScope.launch {
                _navigateState.emit(MAIN_ACTIVITY)
            }
        }else{ Unit }
    }
    fun login(email: String, password: String) {
        // Check for empty fields
        if (email.isEmpty() || password.isEmpty()) {
            viewModelScope.launch {
                _login.emit(NetworkResult.Error("Email or password cannot be empty"))
            }
            return
        }

        viewModelScope.launch {
            _login.emit(NetworkResult.Loading())
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                viewModelScope.launch {
                    _login.emit(NetworkResult.Success("Login Success"))
                }
            }
            .addOnFailureListener {
                viewModelScope.launch {
                    _login.emit(NetworkResult.Error("Invalid Email Or Password"))
                }
            }

    }


    fun resetPassword(email: String) {
        viewModelScope.launch {
            _resetPassword.emit(NetworkResult.Loading())
        }

        if (validateEmail(email) is RegisterValidation.Failed) {
            viewModelScope.launch {
                _resetPassword.emit(NetworkResult.Error("Invalid email"))
            }
            return
        }

        firebaseAuth.sendPasswordResetEmail(email).addOnSuccessListener {
            viewModelScope.launch {
                _resetPassword.emit(NetworkResult.Success(User()))
            }
        }.addOnFailureListener {
            viewModelScope.launch {
                _resetPassword.emit(NetworkResult.Error(it.message.toString()))
            }
        }
    }

}