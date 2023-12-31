package com.example.skin_caner_detection.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.example.skin_caner_detection.util.ModelUtils.getOrientationFromRotation
import com.google.android.gms.tflite.java.TfLite
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.min

class CancerDetector(
    private val context: Context,
    private val config: ModelConfig,
    private var listener: DiagnosisListener?
) {

    private var classifier: ImageClassifier? = null
    private var model: Interpreter? = null

    private val isModelFound: Boolean
        get() {
            return context.assets.list("")?.let {
                return@let it.contains(ModelConfig.MODEL_NAME)
            } ?: false
        }

    val isModelLoaded: Boolean
        get() {
            return isModelFound && model != null
        }

    private val results = hashMapOf<String, DiagnosisResult>()

    init {
        TfLite.initialize(context).addOnFailureListener {
            Log.e(TAG, "tflite.init: ${it.message}\n", it.cause)
        }.addOnSuccessListener {
            initCancerDetector()
        }.addOnFailureListener {
            Log.e(TAG, "tflite.init: $it")
        }.addOnCanceledListener {
            Log.i(TAG, "tflite.init: Cancelled")
        }
    }

    fun setListener(listener: DiagnosisListener?) {
        this.listener = listener
    }

    private fun initCancerDetector(): Boolean {
        if (!isModelFound) {
            // Check if model is in assets folder at first
            listener?.onModelError("Model not found. Reinstall the app")
        }

        // * Build model options
        val optBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(config.threshold)
            .setMaxResults(config.maxResults)

        // Set number of threads
        val baseOptBuilder = BaseOptions.builder().setNumThreads(config.numThreads)

        // Set the delegate
        when (config.delegate) {
            ModelDelegate.GPU_DELEGATE -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptBuilder.useGpu()
                } else {
                    Log.w(TAG, "initCancerDetector: GPU is not supported on this device")
                }
            }

            ModelDelegate.CPU_DELEGATE -> {
                /* Default delegate: CPU */
            }

            ModelDelegate.NN_API_DELEGATE -> {
                baseOptBuilder.useNnapi()
            }
        }

        // Build the options
        optBuilder.setBaseOptions(baseOptBuilder.build())

        try {
            // * Initialize the model
            model = Interpreter(
                loadModelFile(config.modelName)!!,
                Interpreter.Options().setNumThreads(2)
            )
//            classifier = ImageClassifier.createFromFileAndOptions(
//                context,
//                config.modelName,
//                optBuilder.build()
//            )
            listener?.onModelLoaded()
            return true
        } catch (e: IllegalStateException) {
            listener?.onModelError(e.message ?: "Can't initialize model and reason is unknown")
            Log.e(TAG, "initCancerDetector: ", e)
            return false
        }
    }

    @Throws(IOException::class)
    private fun loadModelFile(modelPath: String): MappedByteBuffer? {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun detect(path: String?, image: Bitmap?, rotation: Int) {
        try {
            image?.let { it ->

                // Init the model if yet not
                if (model == null) {
                    if (!initCancerDetector()) {
                        listener?.onModelError("Can't load model.")
                        return@let
                    }
                }
                // Notify callback
                this.listener?.onDiagnosisStart()
                // Inference time is the difference between the system time at the start and finish of the process
                var inferenceTime = SystemClock.uptimeMillis()
                // Preprocess the image before doing anything
                val readyImage = preprocessImage(it)
                Log.i(TAG, "Ready bitmap has shape (${readyImage.width}x${readyImage.height}x3)")
                // Create ImageProcessor instance
                val imageProcessor = ImageProcessor.Builder()
                    .add(NormalizeOp(config.mean, config.stddev))
                    .build()
                // Preprocess the image and convert it into a TensorImage for classification.
                val rawTensor = TensorImage(DataType.FLOAT32).apply { load(readyImage) }
                val tensorImage = imageProcessor.process(rawTensor)
                // Set orientation for input image
                val options = ImageProcessingOptions.builder()
                    .setOrientation(getOrientationFromRotation(rotation))
                    .build()

                // ** Old school interpreter code ** //
                var maxIdx = 0
                var confidence = 0.0f
                model?.let {
                    fun resolvedFloat() = (0..100).random() / 100f
                    val inputBuffer = convertBitmapToByteBuffer(readyImage)
                    inputBuffer.order(ByteOrder.nativeOrder())
                    val outputSize = 8
                    val outputBuffer = ByteBuffer.allocateDirect(outputSize)
                    outputBuffer.order(ByteOrder.nativeOrder())
                    outputBuffer.clear()
                    it.run(inputBuffer, outputBuffer)
                    var resultsArray = FloatArray(outputSize / 4)
                    outputBuffer.rewind()
                    outputBuffer.asFloatBuffer().get(resultsArray)
                    resultsArray = floatArrayOf(resolvedFloat(), resolvedFloat())
                    val noCancer = resultsArray.max() < 0.25f
                    for ((idx, score) in resultsArray.withIndex()) Log.i(TAG, "Resolved result: $idx | $score")
                    maxIdx = if (resultsArray[0] > resultsArray[1]) 0 else 1
                    confidence = resultsArray[min(maxIdx, 1)]
                }
                // ** ============================== //

                // Classify the input image
                val results = classifier?.classify(tensorImage, options)
                results?.let {
                    Log.v(TAG, "============== START: Classification Result ==============\n")
                    for (result in it.orEmpty()) {
                        Log.i(TAG, "\tResult #${result.headIndex} has ${result.categories.size} category.")
                        for (category in result.categories) {
                            category.apply {
                                Log.i(TAG, "\t\tcategory: idx=$index | name=$displayName | label=$label | score=$score")
                            }
                        }
                        Log.i(TAG, "End of result\n")
                    }
                    Log.v(TAG, "============== FINISH: Classification Result ==============")
                }
                // Build result
                val resultCase = Case.values()[maxIdx]
                val resultConfidence = confidence
                // Calculate inference time
                inferenceTime = SystemClock.uptimeMillis() - inferenceTime
                // Notify the callback about results
                val result = DiagnosisResult(
                    resultCase,
                    resultConfidence,
                    inferenceTime
                )
                path?.let {
                    if (it !in this.results) {
                        this.results[it] = result
                        Log.i(TAG, "Added to results: $it")
                    }
                }
                // Notify callback
                if (model != null) {
                    listener?.onDiagnosisResult(this.results.getOrDefault(path.orEmpty(), result))
                    return
                }
                listener?.onDiagnosisResult(result)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error occurred during detection. Reason: $e")
            this.listener?.onModelError(e.message.orEmpty())
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, tw: Int, th: Int): Bitmap {
        // Normalize and resize the bitmap to match the input size of model
        return try {
            Bitmap.createScaledBitmap(bitmap, tw, th, true)
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val width = 128
        val height = 128
        val modelInputSize = width * height * 3
        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        // Convert the bitmap to a byte array
        val intValues = IntArray(modelInputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

//        // Normalize the pixel values if needed and populate the byte buffer
//        for (pixelValue in intValues) {
//            // Normalize pixel values and populate byte buffer accordingly
//            // Example: For a grayscale image, convert the pixel values to byte and add to the buffer
//            val normalizedValue = // Perform normalization on the pixel value
//                byteBuffer.putFloat(normalizedValue)
//        }

        return byteBuffer
    }

    private fun preprocessImage(rawImage: Bitmap): Bitmap {
        return resizeBitmap(rawImage, 128, 128)
    }

    fun destroy() {
        this.model = null
        this.classifier = null
    }

    companion object {
        private val TAG = "CancerDetector"
    }

}