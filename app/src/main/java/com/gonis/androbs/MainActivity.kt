package com.gonis.androbs

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.webrtc.EglBase
import java.io.IOException

external fun initGStreamer()

class MainActivity : AppCompatActivity() {
    private lateinit var cameraXHelper: CameraXHelper
    private lateinit var server: WebRTCServer
    private lateinit var rootEglBase: EglBase

    init {
        System.loadLibrary("native-lib") // Carica la libreria nativa
        System.loadLibrary("gstreamer_android") // Carica GStreamer se necessario
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "onCreate chiamato")

        // 1. Inizializza GStreamer
        initGStreamer()
        Log.d("GStreamerTest", "GStreamer inizializzato tramite JNI")

        // 2. Inizializza EGL per WebRTC
        rootEglBase = EglBase.create()

        // 3. Configura il server WebRTC
        server = WebRTCServer(
            port = 8080,
            context = this,
            rootEglBase = rootEglBase
        )

        try {
            server.start()
            Log.d("WebRTCServer", "Server avviato sulla porta 8080")
        } catch (e: IOException) {
            Log.e("WebRTCServer", "Failed to start server: ${e.message}")
        }

        // 4. Avvia la fotocamera con CameraX
        cameraXHelper = CameraXHelper(this) { image ->
            analyzeImage(image)
            // Qui puoi inviare i frame a GStreamer/WebRTC
            // Esempio:
            // NativeGStreamerApp.sendFrameToGStreamer(image)
        }
        cameraXHelper.startCamera()
    }

    private fun analyzeImage(image: android.media.Image) {
        // Implementa l'analisi dell'immagine se necessario
    }

    override fun onDestroy() {
        // Pulisci le risorse in ordine inverso
        cameraXHelper.stopCamera()
        server.stop()
        rootEglBase.release()
        super.onDestroy()
    }
}