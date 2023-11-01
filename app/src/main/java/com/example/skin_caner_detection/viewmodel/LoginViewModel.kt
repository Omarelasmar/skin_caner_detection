package com.example.skin_caner_detection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skin_caner_detection.util.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
):ViewModel() {

    private val _login = MutableSharedFlow<NetworkResult<String>>()
    val login = _login.asSharedFlow()

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

}