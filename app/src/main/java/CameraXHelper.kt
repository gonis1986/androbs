package com.gonis.androbs

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.gonis.androbs.CameraXHelper.Companion.sendToGStreamer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraXHelper(
    private val context: Context,
    private val onFrameAvailable: (ImageProxy) -> Unit
) {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    companion object {
        @JvmStatic external fun sendToGStreamer(data: ByteArray) // Dichiarazione della funzione nativa
    }

    fun startCamera() {

        Log.d("CameraXHelper", "startCamera: Avvio fotocamera...")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { image ->
                    Log.d("CameraXHelper", "startCamera: Analizzando frame...")
                    onFrameAvailable(image) // Passa l'immagine per l'elaborazione
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )

                Log.d("CameraXHelper", "startCamera: Fotocamera avviata correttamente.")
            } catch (exc: Exception) {
                Log.e("CameraXHelper", "startCamera: Errore nell'avvio della fotocamera", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        Log.d("CameraXHelper", "stopCamera: Arresto fotocamera...")
        cameraExecutor.shutdown()
    }
}

fun analyzeImage(image: ImageProxy) {
    Log.d("CameraXHelper", "analyzeImage: Inizio analisi immagine...")

    // Converte ImageProxy in byte[]
    val byteBuffer = image.planes[0].buffer
    val byteArray = ByteArray(byteBuffer.remaining())
    byteBuffer.get(byteArray)

    // Invia i byte a GStreamer per l'elaborazione
    Log.d("CameraXHelper", "analyzeImage: Inviando frame a GStreamer...")
    sendToGStreamer(byteArray)

    image.close()  // Non dimenticare di chiudere ImageProxy
    Log.d("CameraXHelper", "analyzeImage: Immagine chiusa.")
}
