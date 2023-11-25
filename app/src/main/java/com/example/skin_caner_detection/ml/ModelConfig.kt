package com.example.skin_caner_detection.ml

data class ModelConfig(
    val numThreads: Int = 2,
    val maxResults: Int = 2,
    val threshold: Float = 0.5f,
    val delegate: ModelDelegate,
    val modelName: String = MODEL_NAME,
    val mean: FloatArray = floatArrayOf(),
    val stddev: FloatArray = floatArrayOf()
) {

    companion object {
        val DEFAULT: ModelConfig
            get() {
                return ModelConfig(
                    numThreads = 2,
                    maxResults = 2,
                    threshold = 0.5f,
                    delegate = ModelDelegate.GPU_DELEGATE,
                    modelName = MODEL_NAME,
                    mean = floatArrayOf(0.485f, 0.456f, 0.406f),
                    stddev = floatArrayOf(0.229f, 0.224f, 0.225f)
                )
            }
        val MODEL_NAME = "model_skin.tflite"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModelConfig

        if (numThreads != other.numThreads) return false
        if (maxResults != other.maxResults) return false
        if (threshold != other.threshold) return false
        if (delegate != other.delegate) return false
        if (modelName != other.modelName) return false
        if (!mean.contentEquals(other.mean)) return false
        if (!stddev.contentEquals(other.stddev)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = numThreads
        result = 31 * result + maxResults
        result = 31 * result + threshold.hashCode()
        result = 31 * result + delegate.hashCode()
        result = 31 * result + modelName.hashCode()
        result = 31 * result + mean.contentHashCode()
        result = 31 * result + stddev.contentHashCode()
        return result
    }

}
