package com.example.skin_caner_detection.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.example.skin_caner_detection.R
import com.example.skin_caner_detection.databinding.ActivityMainBinding
import com.example.skin_caner_detection.ml.CancerDetector
import com.example.skin_caner_detection.ml.DiagnosisListener
import com.example.skin_caner_detection.ml.DiagnosisResult
import com.example.skin_caner_detection.ml.ModelConfig
import com.example.skin_caner_detection.util.ModelUtils
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : AppCompatActivity(), DiagnosisListener {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    private var imageUri: Uri? = null
    private var latestResult: DiagnosisResult? = null
    private lateinit var detector: CancerDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize an CancerDetector instance
        detector = CancerDetector(this, ModelConfig.DEFAULT, this)

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            imageUri = it.data?.data
            Glide.with(this).load(imageUri).into(binding.image)
            changeState(if (imageUri != null) State.UPLOADED else State.NO_IMAGE)
        }

        binding.btnUploadImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
        }

        binding.btnDiagnosis.setOnClickListener {
            if (imageUri != null) {
                contentResolver.openInputStream(imageUri!!)?.let {
                    val rawImage = BitmapFactory.decodeStream(it)
                    val rotation = ModelUtils.getScreenOrientation(this)
                    detector?.detect(imageUri!!.path, rawImage, rotation)
                    it.close()
                }
            } else Toast.makeText(this, "Upload an image first", Toast.LENGTH_SHORT).show()
        }

        // Reset UI
        changeState(State.MODEL_LOADING)
    }

    private fun changeState(state: State) {
        when (state) {
            State.MODEL_LOADING -> {
                binding.btnDiagnosis.visibility = View.INVISIBLE
                binding.btnUploadImage.visibility = View.INVISIBLE
                binding.tvStatus.text = getString(R.string.loading_model)
                binding.tvStatus.setTextColor(R.color.adaptive_normal_text.toColor())
            }

            State.NO_IMAGE -> {
                binding.btnDiagnosis.visibility = View.INVISIBLE
                binding.btnUploadImage.visibility = View.VISIBLE
                binding.tvStatus.text = getString(R.string.upload_image_using_btn_below)
                binding.tvStatus.setTextColor(R.color.adaptive_normal_text.toColor())
            }

            State.UPLOADED -> {
                binding.btnDiagnosis.visibility = View.VISIBLE
                binding.tvStatus.text = getString(R.string.diagnosis_uploaded_image)
                binding.tvStatus.setTextColor(R.color.adaptive_normal_text.toColor())
            }

            State.DIAGNOSING -> {
                binding.tvStatus.text = getString(R.string.performing_diagnosis)
                binding.tvStatus.setTextColor(com.google.android.material.R.color.material_dynamic_secondary60.toColor())
                binding.btnUploadImage.isEnabled = false
                binding.btnDiagnosis.isEnabled = false
            }

            State.BENIGN -> {
                binding.tvStatus.text = getString(R.string.benign_cancer)
                binding.tvStatus.setTextColor(R.color.g_orange_yellow.toColor())
                binding.btnUploadImage.isEnabled = true
                binding.btnDiagnosis.isEnabled = true
            }

            State.MALIGNANT -> {
                binding.tvStatus.text = getString(R.string.malignant_cancer)
                binding.tvStatus.setTextColor(R.color.g_red.toColor())
                binding.btnUploadImage.isEnabled = true
                binding.btnDiagnosis.isEnabled = true
            }

            State.NO_CANCER -> {
                binding.tvStatus.text = getString(R.string.no_cancer)
                binding.tvStatus.setTextColor(R.color.green.toColor())
                binding.btnUploadImage.isEnabled = true
                binding.btnDiagnosis.isEnabled = true
            }

            State.ERROR -> {
                binding.tvStatus.text = getString(R.string.can_not_analyze_image)
                binding.tvStatus.setTextColor(R.color.g_light_red.toColor())
                binding.btnUploadImage.isEnabled = true
                binding.btnDiagnosis.isEnabled = true
            }
        }
    }

    private enum class State {
        MODEL_LOADING,
        NO_IMAGE,
        UPLOADED,
        DIAGNOSING,
        BENIGN,
        MALIGNANT,
        NO_CANCER,
        ERROR
    }

    private fun Int.toColor(): Int {
        return ResourcesCompat.getColor(resources, this, null)
    }

    override fun onModelLoaded() {
        changeState(if (imageUri != null) State.UPLOADED else State.NO_IMAGE)
    }

    override fun onDiagnosisStart() {
        changeState(State.DIAGNOSING)
    }

    override fun onDiagnosisResult(result: DiagnosisResult) {
        changeState(listOf(State.BENIGN,State.MALIGNANT,State.NO_CANCER)[result.case.ordinal])
        Log.i(TAG, "[onDiagnosisResult]: Case[${result.case.name}] | Confidence[${result.confidence}] | InfTime[${result.inferenceTime.milliseconds.toIsoString()}]")
    }

    override fun onDiagnosisError(reason: String) {
        changeState(State.ERROR)
        Log.e(TAG, "[onDiagnosisError]: $reason")
    }

    override fun onModelError(reason: String) {
        changeState(State.ERROR)
        binding.btnDiagnosis.isEnabled = false
        binding.btnUploadImage.isEnabled = false
        binding.tvStatus.text = getString(R.string.restart_the_app_to_reload_the_model)
        Log.e(TAG, "[onModelError]: $reason")
    }

    override fun onPause() {
        detector.setListener(null)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        this.detector.setListener(this)
        if (detector.isModelLoaded) {
            if (imageUri != null) changeState(State.UPLOADED)
            else changeState(State.NO_IMAGE)
        }
    }

    override fun onDestroy() {
        detector.setListener(null)
        detector.destroy()
        super.onDestroy()
    }

    companion object {
        private val TAG = "Runner"
    }

}