package com.example.skin_caner_detection.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.skin_caner_detection.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginRegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_register)
    }
}