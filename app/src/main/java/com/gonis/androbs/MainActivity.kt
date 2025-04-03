package com.gonis.androbs

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.common.util.concurrent.ListenableFuture
import java.io.IOException
import com.gonis.androbs.WebSocketServer
import androidx.camera.core.ImageAnalysis
import android.util.Size
import android.widget.Toast
import androidx.camera.core.ImageProxy



external fun initGStreamer()

class MainActivity : AppCompatActivity() {
    private val CAMERA_PERMISSION_REQUEST_CODE = 1
    private lateinit var wifiManager: WifiManager
    private lateinit var networkStatusReceiver: BroadcastReceiver
    private lateinit var ipTextView: TextView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var previewView: PreviewView
    private var webSocketServer: WebSocketServer? = null

    init {
        System.loadLibrary("native-lib")

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "onCreate chiamato")

        previewView = findViewById(R.id.previewView)
        ipTextView = findViewById(R.id.ipTextView)

        // Richiesta permessi
        // Verifica se il permesso per la fotocamera è stato concesso
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Se non è stato concesso, richiedi il permesso
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            // Il permesso è già stato concesso, avvia la fotocamera
            startCamera()
        }


        // Inizializza GStreamer tramite JNI
        initGStreamer()
        Log.d("GStreamerTest", "GStreamer inizializzato tramite JNI")


        // Trova i pulsanti
        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)

        // Imposta i listener dei pulsanti
        startButton.setOnClickListener {
            startWebSocketServer()
            startButton.isEnabled = false
            stopButton.isEnabled = true
        }

        stopButton.setOnClickListener {
            stopWebSocketServer()
            stopButton.isEnabled = false
            startButton.isEnabled = true
        }

        // Inizialmente disabilita il pulsante di stop
        stopButton.isEnabled = false

        // Inizializza WiFi Manager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Mostra l'indirizzo IP iniziale
        updateIPAddress()

        // Imposta il BroadcastReceiver per i cambiamenti di rete
        networkStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updateIPAddress()
            }
        }

        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkStatusReceiver, filter)
    }

    override fun onDestroy() {
        stopWebSocketServer()
        unregisterReceiver(networkStatusReceiver)
        super.onDestroy()
    }

    private fun updateIPAddress() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo

        if (networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI) {
            val ip = getLocalIpAddress()
            ipTextView.text = "Indirizzo IP: $ip:8081"
            Log.d("NetworkReceiver", "Indirizzo IP aggiornato: $ip")
            Log.d("NetworkInfo", "Indirizzo IP server: $ip")
        } else {
            ipTextView.text = "No Wi-Fi connection"
            Log.d("NetworkReceiver", "Nessuna connessione Wi-Fi attiva.")
        }
    }


    private fun getLocalIpAddress(): String {
        val ip = wifiManager.connectionInfo.ipAddress
        return android.text.format.Formatter.formatIpAddress(ip)
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Imposta il preview
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            // Imposta ImageAnalysis per catturare i fotogrammi
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720)) // Risoluzione del flusso video
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), { imageProxy ->
                // Qui puoi elaborare ogni fotogramma catturato dalla fotocamera
                // Ad esempio, convertirlo in JPEG e inviarlo tramite WebSocket
                val buffer = imageProxy.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                // Invia il fotogramma come JPEG attraverso WebSocket
                Log.d("WebSocket", "Invio frame di ${bytes.size} bytes")
                webSocketServer?.sendFrame(bytes)

                // Chiedi a ImageAnalysis di chiudere l'immagine quando hai finito
                imageProxy.close()
            })
            Log.d("CameraX", "Tentativo di accesso alle risorse della fotocamera...")

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            try {
                // Avvio della fotocamera con CameraX
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                Log.e("CameraX", "Errore nell'accesso alla fotocamera: ${e.message}")
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private fun startWebSocketServer() {
        if (webSocketServer != null && webSocketServer!!.isRunning) {
            Log.d("WebSocket", "Server già in esecuzione")
            return
        }

        try {
            webSocketServer = WebSocketServer(8081).apply {
                start()
                Log.d("WebSocket", "Server avviato sulla porta 8081")
            }
        } catch (e: IOException) {
            Log.e("WebSocket", "Errore nell'avvio del server: ${e.message}")
            webSocketServer = null
        } catch (e: Exception) {
            Log.e("WebSocket", "Errore generico: ${e.message}")
            webSocketServer = null
        }
    }

    private fun stopWebSocketServer() {
        try {
            webSocketServer?.let {
                if (it.isRunning) {
                    it.stop(1000) // Timeout di 1 secondo
                    Log.d("WebSocket", "Server fermato con ${it.connections.size} connessioni chiuse")
                }
            }
        } catch (e: Exception) {
            Log.e("WebSocket", "Errore arresto server", e)
        } finally {
            webSocketServer = null
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permesso concesso, avvia la fotocamera
                startCamera()
            } else {
                // Permesso negato, mostra un messaggio all'utente
                Toast.makeText(this, "Permesso fotocamera negato", Toast.LENGTH_SHORT).show()
            }
        }
    }
}