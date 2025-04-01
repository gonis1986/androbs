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


external fun initGStreamer()

class MainActivity : AppCompatActivity() {
    private lateinit var wifiManager: WifiManager
    private lateinit var networkStatusReceiver: BroadcastReceiver
    private lateinit var ipTextView: TextView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var previewView: PreviewView
    private lateinit var webSocketServer: WebSocketServer

    init {
        System.loadLibrary("native-lib") // Assicurati che il nome della libreria sia corretto
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "onCreate chiamato")

        previewView = findViewById(R.id.previewView)
        ipTextView = findViewById(R.id.ipTextView)

        // Richiesta permessi
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            startCamera()
        }

        // Inizializza GStreamer tramite JNI
        initGStreamer()
        Log.d("GStreamerTest", "GStreamer inizializzato tramite JNI")

        // Avvia il WebSocket Server sulla porta 8081
        webSocketServer = WebSocketServer(8081)
        try {
            webSocketServer.start()
            Log.d("WebSocket", "WebSocket server avviato sulla porta 8081")
        } catch (e: IOException) {
            Log.e("WebSocket", "Errore nell'avvio del WebSocket server: ${e.message}")
        }

        // Trova il pulsante di avvio e imposta un listener per avviare il server
        val startButton = findViewById<Button>(R.id.startButton)
        startButton.setOnClickListener {
            if (!::webSocketServer.isInitialized || !webSocketServer.isRunning) {
                startWebSocketServer()  // Avvia il WebSocket server
                startButton.isEnabled = false // Disabilita il pulsante di avvio per evitare che l'utente lo clicchi più volte
                val stopButton = findViewById<Button>(R.id.stopButton)
                stopButton.isEnabled = true  // Abilita il pulsante di stop
            }
        }

        // Trova il pulsante di stop e imposta un listener per fermare il server
        val stopButton = findViewById<Button>(R.id.stopButton)
        stopButton.setOnClickListener {
            stopWebSocketServer()  // Ferma il WebSocket server
            stopButton.isEnabled = false // Disabilita il pulsante di stop per evitare che l'utente lo clicchi più volte
            startButton.isEnabled = true  // Riabilita il pulsante di avvio
        }

        // Inizialmente, disabilita il pulsante di stop
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
        super.onDestroy()
        webSocketServer.stop()
        unregisterReceiver(networkStatusReceiver)
    }

    private fun updateIPAddress() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo

        if (networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI) {
            val ip = getLocalIpAddress()
            ipTextView.text = "Indirizzo IP: $ip:8081"
            Log.d("NetworkReceiver", "Indirizzo IP aggiornato: $ip")
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

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (e: Exception) {
                Log.e("CameraX", "Errore nell'avvio della fotocamera", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startWebSocketServer() {
        webSocketServer = WebSocketServer(8081)
        try {
            webSocketServer.start()
            Log.d("WebSocket", "WebSocket server avviato sulla porta 8081")
        } catch (e: IOException) {
            Log.e("WebSocket", "Errore nell'avvio del WebSocket server: ${e.message}")
        }
    }

    private fun stopWebSocketServer() {
        webSocketServer.stop()
        Log.d("WebSocket", "WebSocket server fermato.")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }
}
