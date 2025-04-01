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
        val ip = getLocalIpAddress()
        ipTextView.text = "Indirizzo IP: $ip:8081"
        Log.d("NetworkReceiver", "Indirizzo IP aggiornato: $ip")
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }
}
