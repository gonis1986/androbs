package com.gonis.androbs

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress

class WebSocketServer(port: Int) : WebSocketServer(InetSocketAddress(port)) {
    var isRunning: Boolean = false
    override fun onOpen(conn: WebSocket, handshake: ClientHandshake?) {
        Log.d("WebSocketServer", "Nuova connessione da ${conn.remoteSocketAddress}")
        conn.send("{\"type\": \"connection\", \"message\": \"Connessione stabilita\"}")
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
        Log.d("WebSocketServer", "Connessione chiusa: ${conn.remoteSocketAddress} - Motivo: $reason")
    }

    override fun onMessage(conn: WebSocket, message: String) {
        Log.d("WebSocketServer", "Messaggio ricevuto: $message")
        conn.send("{\"type\": \"echo\", \"message\": \"$message\"}")  // Risponde con lo stesso messaggio ricevuto
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        Log.e("WebSocketServer", "Errore nel WebSocket", ex)
    }

    override fun onStart() {
        Log.d("WebSocketServer", "WebSocket Server avviato sulla porta ${address.port}")
    }
}
