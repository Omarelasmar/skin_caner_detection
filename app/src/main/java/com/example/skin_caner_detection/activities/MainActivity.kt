package com.example.skin_caner_detection.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.skin_caner_detection.R
import com.example.skin_caner_detection.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var imageActivityResultLauncher: ActivityResultLauncher<Intent>

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

       // val image = findViewById<ImageView>(R.id.image)

        imageActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                imageUri = it.data?.data
                Glide.with(this).load(imageUri).into(binding.image)
            }


        binding.appCompatButton.setOnClickListener{
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            imageActivityResultLauncher.launch(intent)
        }

        binding.chipCancer.text = "cancer"
        binding.chipCancer.visibility = View.INVISIBLE

    }
}