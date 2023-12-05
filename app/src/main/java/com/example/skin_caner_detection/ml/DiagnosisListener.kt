package com.example.skin_caner_detection.ml

interface DiagnosisListener {

    fun onModelLoaded()

    fun onDiagnosisStart()

    fun onDiagnosisResult(result: DiagnosisResult)

    fun onDiagnosisError(reason: String)

    fun onModelError(reason: String)
}