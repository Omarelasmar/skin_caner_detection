package com.example.skin_caner_detection.ml

data class DiagnosisResult(
    val case: Case,
    val confidence: Float,
    val inferenceTime: Long
)
